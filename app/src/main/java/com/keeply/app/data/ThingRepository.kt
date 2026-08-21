package com.keeply.app.data

import com.keeply.app.data.local.ThingDao
import com.keeply.app.data.local.ThingEntity
import com.keeply.app.model.NewThingDraft
import com.keeply.app.model.ReminderType
import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
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
        val entity = ThingEntity(
            id = idFactory(),
            name = draft.name.trim(),
            categoryCode = draft.category.code,
            importantDate = draft.importantDate,
            reminderTypeCode = draft.reminderType?.code,
            reminderAtEpochMillis = draft.reminderAtEpochMillis,
            reminderTimeZoneId = draft.reminderTimeZoneId,
            notes = draft.notes.trim().ifBlank { null },
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
}

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
