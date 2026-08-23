package com.keeply.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.keeply.app.data.ThingRepository
import com.keeply.app.data.ThingNotFoundException
import com.keeply.app.model.NewThingDraft
import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
import com.keeply.app.notifications.ReminderSyncCoordinator
import com.keeply.app.notifications.ReminderSyncResult
import com.keeply.app.notifications.MissedReminderRecovery
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal sealed interface SaveThingEvent {
    data class Saved(val thing: Thing, val reminderSyncResult: ReminderSyncResult) : SaveThingEvent
    data object Failed : SaveThingEvent
}

internal sealed interface UpdateThingEvent {
    data class Updated(val thing: Thing, val reminderSyncResult: ReminderSyncResult) : UpdateThingEvent
    data object Unchanged : UpdateThingEvent
    data object Failed : UpdateThingEvent
    data object Missing : UpdateThingEvent
}

internal sealed interface LifecycleEvent {
    data class ReminderUpdated(
        val thing: Thing,
        val reminderAtEpochMillis: Long,
        val timeZoneId: String,
        val reminderSyncResult: ReminderSyncResult
    ) : LifecycleEvent
    data object MarkedDone : LifecycleEvent
    data object Reopened : LifecycleEvent
    data object Deleted : LifecycleEvent
    data object Failed : LifecycleEvent
    data object Missing : LifecycleEvent
}

internal sealed interface ItemDetailsState {
    data object NotSelected : ItemDetailsState
    data object Loading : ItemDetailsState
    data class Content(val thing: Thing) : ItemDetailsState
    data object NotFound : ItemDetailsState
    data object Error : ItemDetailsState
}

class KeeplyViewModel internal constructor(
    private val repository: ThingRepository,
    private val reminderSyncCoordinator: ReminderSyncCoordinator,
    private val missedReminderRecovery: MissedReminderRecovery
) : ViewModel() {
    val things: StateFlow<List<Thing>> = repository.things.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    private val _myThingsFilter = MutableStateFlow(MyThingsFilter.ALL)
    internal val myThingsFilter: StateFlow<MyThingsFilter> = _myThingsFilter.asStateFlow()
    private val _myThingsCategory = MutableStateFlow<ThingCategory?>(null)
    internal val myThingsCategory: StateFlow<ThingCategory?> = _myThingsCategory.asStateFlow()
    private val _myThingsSearchQuery = MutableStateFlow("")
    internal val myThingsSearchQuery: StateFlow<String> = _myThingsSearchQuery.asStateFlow()
    private val _myThingsSearchActive = MutableStateFlow(false)
    internal val myThingsSearchActive: StateFlow<Boolean> = _myThingsSearchActive.asStateFlow()

    internal fun selectMyThingsFilter(filter: MyThingsFilter) {
        _myThingsFilter.value = filter
    }

    internal fun selectMyThingsCategory(category: ThingCategory?) {
        _myThingsCategory.value = category
    }

    internal fun openMyThingsSearch() {
        _myThingsSearchActive.value = true
    }

    internal fun updateMyThingsSearchQuery(query: String) {
        _myThingsSearchQuery.value = query
    }

    internal fun closeAndClearMyThingsSearch() {
        _myThingsSearchActive.value = false
        _myThingsSearchQuery.value = ""
    }

    internal fun showAllMissedThings() {
        _myThingsFilter.value = MyThingsFilter.MISSED
        _myThingsCategory.value = null
        closeAndClearMyThingsSearch()
    }

    internal fun resetMyThingsAfterCreate() {
        val reset = myThingsStateAfterSuccessfulCreate()
        _myThingsFilter.value = reset.filter
        _myThingsCategory.value = reset.category
        _myThingsSearchActive.value = reset.searchActive
        _myThingsSearchQuery.value = reset.searchQuery
    }

    private val saveEventsChannel = Channel<SaveThingEvent>(Channel.BUFFERED)
    internal val saveEvents: Flow<SaveThingEvent> = saveEventsChannel.receiveAsFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _itemDetailsState = MutableStateFlow<ItemDetailsState>(ItemDetailsState.NotSelected)
    internal val itemDetailsState: StateFlow<ItemDetailsState> = _itemDetailsState.asStateFlow()
    private var itemDetailsJob: Job? = null

    private val updateEventsChannel = Channel<UpdateThingEvent>(Channel.BUFFERED)
    internal val updateEvents: Flow<UpdateThingEvent> = updateEventsChannel.receiveAsFlow()
    private val _isUpdating = MutableStateFlow(false)
    internal val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()
    private val lifecycleEventsChannel = Channel<LifecycleEvent>(Channel.BUFFERED)
    internal val lifecycleEvents: Flow<LifecycleEvent> = lifecycleEventsChannel.receiveAsFlow()
    private val _isChangingLifecycle = MutableStateFlow(false)
    internal val isChangingLifecycle: StateFlow<Boolean> = _isChangingLifecycle.asStateFlow()

    fun createThing(draft: NewThingDraft) {
        if (!_isSaving.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            val event = try {
                val thing = repository.createThing(draft)
                SaveThingEvent.Saved(thing, reminderSyncCoordinator.sync(thing))
            } catch (_: Exception) {
                SaveThingEvent.Failed
            } finally {
                _isSaving.value = false
            }
            saveEventsChannel.send(event)
        }
    }

    internal fun selectThing(id: String) {
        itemDetailsJob?.cancel()
        _itemDetailsState.value = ItemDetailsState.Loading
        itemDetailsJob = viewModelScope.launch {
            repository.observeThing(id)
                .catch {
                    _itemDetailsState.value = ItemDetailsState.Error
                }
                .collect { thing ->
                    _itemDetailsState.value = if (thing == null) {
                        ItemDetailsState.NotFound
                    } else {
                        ItemDetailsState.Content(thing)
                    }
                }
        }
    }

    internal fun clearSelectedThing() {
        itemDetailsJob?.cancel()
        itemDetailsJob = null
        _itemDetailsState.value = ItemDetailsState.NotSelected
    }

    internal fun updateThing(id: String, draft: NewThingDraft) {
        if (!_isUpdating.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            val event = try {
                val result = repository.updateThing(id, draft)
                if (result.changed) {
                    UpdateThingEvent.Updated(
                        result.thing,
                        reminderSyncCoordinator.sync(result.thing)
                    )
                } else UpdateThingEvent.Unchanged
            } catch (_: ThingNotFoundException) {
                _itemDetailsState.value = ItemDetailsState.NotFound
                UpdateThingEvent.Missing
            } catch (_: Exception) {
                UpdateThingEvent.Failed
            } finally {
                _isUpdating.value = false
            }
            updateEventsChannel.send(event)
        }
    }

    internal fun remindAgain(id: String, reminderAtEpochMillis: Long, timeZoneId: String) {
        if (!_isChangingLifecycle.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            val event = try {
                val thing = repository.remindAgain(id, reminderAtEpochMillis, timeZoneId)
                LifecycleEvent.ReminderUpdated(
                    thing,
                    reminderAtEpochMillis,
                    timeZoneId,
                    reminderSyncCoordinator.sync(thing)
                )
            } catch (_: ThingNotFoundException) {
                LifecycleEvent.Missing
            } catch (_: Exception) {
                LifecycleEvent.Failed
            } finally {
                _isChangingLifecycle.value = false
            }
            lifecycleEventsChannel.send(event)
        }
    }

    internal fun markDone(id: String) = runLifecycleChange(LifecycleEvent.MarkedDone) {
        reminderSyncCoordinator.sync(repository.markDone(id))
    }

    internal fun reopen(id: String) = runLifecycleChange(LifecycleEvent.Reopened) {
        reminderSyncCoordinator.sync(repository.reopen(id))
    }

    internal fun deleteThing(id: String) = runLifecycleChange(LifecycleEvent.Deleted) {
        repository.deleteThing(id)
        reminderSyncCoordinator.cancel(id)
    }

    internal fun reconcileReminders() {
        viewModelScope.launch { missedReminderRecovery.reconcile(repository.things.first()) }
    }

    private fun runLifecycleChange(success: LifecycleEvent, operation: suspend () -> Unit) {
        if (!_isChangingLifecycle.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            val event = try {
                operation()
                success
            } catch (_: ThingNotFoundException) {
                LifecycleEvent.Missing
            } catch (_: Exception) {
                LifecycleEvent.Failed
            } finally {
                _isChangingLifecycle.value = false
            }
            lifecycleEventsChannel.send(event)
        }
    }

    companion object {
        internal fun factory(
            repository: ThingRepository,
            reminderSyncCoordinator: ReminderSyncCoordinator,
            missedReminderRecovery: MissedReminderRecovery
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(KeeplyViewModel::class.java))
                    return KeeplyViewModel(repository, reminderSyncCoordinator, missedReminderRecovery) as T
                }
            }
    }
}
