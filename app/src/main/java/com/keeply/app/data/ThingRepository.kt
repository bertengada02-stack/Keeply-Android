package com.keeply.app.data

import com.keeply.app.data.local.ThingDao
import com.keeply.app.data.local.ThingEntity
import com.keeply.app.model.NewThingDraft
import com.keeply.app.model.ReminderType
import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
import com.keeply.app.model.toPersistedValues
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
        if (values == currentThing.toPersistedValues()) {
            return UpdateThingResult(currentThing, changed = false)
        }

        val updated = current.copy(
            name = values.name,
            categoryCode = values.category.code,
            importantDate = values.importantDate,
            reminderTypeCode = values.reminderType?.code,
            reminderAtEpochMillis = values.reminderAtEpochMillis,
            reminderTimeZoneId = values.reminderTimeZoneId,
            notes = values.notes,
            updatedAtEpochMillis = maxOf(clock(), current.updatedAtEpochMillis + 1L)
        )
        if (dao.update(updated) != 1) throw ThingNotFoundException(id)
        return UpdateThingResult(updated.toModel(), changed = true)
    }
}

data class UpdateThingResult(val thing: Thing, val changed: Boolean)

class ThingNotFoundException(id: String) : IllegalStateException("Thing not found: $id")

internal fun ThingEntity.toModel(): Thing = Thing(
    id = id,
    name = name,
    category = ThingCategory.fromCode(categoryCode),
    importantDate = importantDate,
    reminderType = reminderTypeCode?.let(ReminderType::fromCode),
    reminderAtEpochMillis = reminderAtEpochMillis,
    reminderTimeZoneId = reminderTimeZoneId,
    notes = notes,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)
