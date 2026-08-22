package com.keeply.app.data

import com.keeply.app.data.local.ThingDao
import com.keeply.app.data.local.ThingEntity
import com.keeply.app.model.NewThingDraft
import com.keeply.app.model.ReminderType
import com.keeply.app.model.ReminderDeliveryState
import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
import com.keeply.app.model.ThingStatus
import com.keeply.app.model.toPersistedValues
import com.keeply.app.model.isValidNextReminder
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThingRepository(
    private val dao: ThingDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idFactory: () -> String = { UUID.randomUUID().toString() }
) {
    val things: Flow<List<Thing>> = dao.observeAll().map { entities ->
        entities.map(ThingEntity::toModel)
    }

    suspend fun createThing(draft: NewThingDraft): Thing {
        val timestamp = clock()
        val values = draft.toPersistedValues()
        val entity = ThingEntity(
            id = idFactory(),
            name = values.name,
            categoryCode = values.category.code,
            importantDate = values.importantDate,
            reminderTypeCode = values.reminderType?.code,
            reminderAtEpochMillis = values.reminderAtEpochMillis,
            reminderTimeZoneId = values.reminderTimeZoneId,
            notes = values.notes,
            statusCode = ThingStatus.ACTIVE.code,
            nextReminderAtEpochMillis = null,
            nextReminderTimeZoneId = null,
            originalReminderActionable = values.reminderType != null,
            reminderDeliveryStateCode = if (values.reminderAtEpochMillis != null) {
                ReminderDeliveryState.PENDING.code
            } else {
                ReminderDeliveryState.NONE.code
            },
            createdAtEpochMillis = timestamp,
            updatedAtEpochMillis = timestamp
        )
        dao.insert(entity)
        return entity.toModel()
    }

    suspend fun findById(id: String): Thing? = dao.findById(id)?.toModel()

    fun observeThing(id: String): Flow<Thing?> = dao.observeById(id).map { entity ->
        entity?.toModel()
    }

    suspend fun updateThing(id: String, draft: NewThingDraft): UpdateThingResult {
        val current = dao.findById(id) ?: throw ThingNotFoundException(id)
        val values = draft.toPersistedValues()
        val currentThing = current.toModel()
        val desiredActionability = when (currentThing.status) {
            ThingStatus.ACTIVE -> when {
                values.reminderType == null -> false
                draft.reminderExplicitlySelected -> true
                else -> currentThing.originalReminderActionable
            }
            ThingStatus.IN_PROGRESS, ThingStatus.DONE -> false
        }
        if (
            values == currentThing.toPersistedValues() &&
            desiredActionability == currentThing.originalReminderActionable
        ) {
            return UpdateThingResult(currentThing, changed = false)
        }

        val updatedWithoutDeliveryState = current.copy(
            name = values.name,
            categoryCode = values.category.code,
            importantDate = values.importantDate,
            reminderTypeCode = values.reminderType?.code,
            reminderAtEpochMillis = values.reminderAtEpochMillis,
            reminderTimeZoneId = values.reminderTimeZoneId,
            notes = values.notes,
            originalReminderActionable = desiredActionability,
            updatedAtEpochMillis = maxOf(clock(), current.updatedAtEpochMillis + 1L)
        )
        val currentActionable = currentThing.actionableReminderIdentity()
        val updatedActionable = updatedWithoutDeliveryState.toModel().actionableReminderIdentity()
        val updated = updatedWithoutDeliveryState.copy(
            reminderDeliveryStateCode = when {
                updatedActionable == null -> ReminderDeliveryState.NONE.code
                updatedActionable != currentActionable -> ReminderDeliveryState.PENDING.code
                else -> current.reminderDeliveryStateCode
            }
        )
        if (dao.update(updated) != 1) throw ThingNotFoundException(id)
        return UpdateThingResult(updated.toModel(), changed = true)
    }

    suspend fun remindAgain(id: String, reminderAtEpochMillis: Long, timeZoneId: String): Thing {
        if (!isValidNextReminder(reminderAtEpochMillis, clock())) throw InvalidNextReminderException()
        val current = dao.findById(id) ?: throw ThingNotFoundException(id)
        if (current.statusCode == ThingStatus.DONE.code) throw InvalidLifecycleTransitionException()
        val updatedAt = maxOf(clock(), current.updatedAtEpochMillis + 1L)
        if (dao.remindAgain(id, reminderAtEpochMillis, timeZoneId, updatedAt) != 1) {
            throw InvalidLifecycleTransitionException()
        }
        return dao.findById(id)?.toModel() ?: throw ThingNotFoundException(id)
    }

    suspend fun markDone(id: String): Thing {
        val current = dao.findById(id) ?: throw ThingNotFoundException(id)
        if (current.statusCode == ThingStatus.DONE.code) throw InvalidLifecycleTransitionException()
        val updatedAt = maxOf(clock(), current.updatedAtEpochMillis + 1L)
        if (dao.markDone(id, updatedAt) != 1) throw InvalidLifecycleTransitionException()
        return dao.findById(id)?.toModel() ?: throw ThingNotFoundException(id)
    }

    suspend fun reopen(id: String): Thing {
        val current = dao.findById(id) ?: throw ThingNotFoundException(id)
        if (current.statusCode != ThingStatus.DONE.code) throw InvalidLifecycleTransitionException()
        val updatedAt = maxOf(clock(), current.updatedAtEpochMillis + 1L)
        if (dao.reopen(id, updatedAt) != 1) throw InvalidLifecycleTransitionException()
        return dao.findById(id)?.toModel() ?: throw ThingNotFoundException(id)
    }

    suspend fun deleteThing(id: String) {
        if (dao.deleteById(id) != 1) throw ThingNotFoundException(id)
    }

    suspend fun markReminderDelivered(id: String, expectedEpoch: Long): Boolean =
        dao.markReminderDelivered(id, expectedEpoch) == 1

    suspend fun markReminderMissed(id: String, expectedEpoch: Long): Boolean =
        dao.markReminderMissed(id, expectedEpoch) == 1

    suspend fun unannouncedMissedThings(): List<Thing> =
        dao.findUnannouncedMissed().map(ThingEntity::toModel)

    suspend fun allMissedThings(): List<Thing> =
        dao.findAllMissed().map(ThingEntity::toModel)

    suspend fun markMissedAnnounced(ids: List<String>) {
        if (ids.isNotEmpty()) dao.markMissedAnnounced(ids)
    }
}

data class UpdateThingResult(val thing: Thing, val changed: Boolean)

class ThingNotFoundException(id: String) : IllegalStateException("Thing not found: $id")
class InvalidLifecycleTransitionException : IllegalStateException("Lifecycle transition is not allowed")
class InvalidNextReminderException : IllegalArgumentException("Next reminder must be in the future")

internal fun ThingEntity.toModel(): Thing = Thing(
    id = id,
    name = name,
    category = ThingCategory.fromCode(categoryCode),
    importantDate = importantDate,
    reminderType = reminderTypeCode?.let(ReminderType::fromCode),
    reminderAtEpochMillis = reminderAtEpochMillis,
    reminderTimeZoneId = reminderTimeZoneId,
    notes = notes,
    status = ThingStatus.fromCode(statusCode),
    nextReminderAtEpochMillis = nextReminderAtEpochMillis,
    nextReminderTimeZoneId = nextReminderTimeZoneId,
    originalReminderActionable = originalReminderActionable,
    reminderDeliveryState = ReminderDeliveryState.fromCode(reminderDeliveryStateCode),
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)

private data class ActionableReminderIdentity(val source: String, val epochMillis: Long)

private fun Thing.actionableReminderIdentity(): ActionableReminderIdentity? = when (status) {
    ThingStatus.ACTIVE -> reminderAtEpochMillis
        ?.takeIf { originalReminderActionable }
        ?.let { ActionableReminderIdentity("ORIGINAL", it) }
    ThingStatus.IN_PROGRESS -> nextReminderAtEpochMillis
        ?.let { ActionableReminderIdentity("FOLLOW_UP", it) }
    ThingStatus.DONE -> null
}
