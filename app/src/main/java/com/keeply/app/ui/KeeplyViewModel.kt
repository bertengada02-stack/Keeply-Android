package com.keeply.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.keeply.app.data.ThingRepository
import com.keeply.app.data.ThingNotFoundException
import com.keeply.app.model.NewThingDraft
import com.keeply.app.model.Thing
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal sealed interface SaveThingEvent {
    data class Saved(val thing: Thing) : SaveThingEvent
    data object Failed : SaveThingEvent
}

internal sealed interface UpdateThingEvent {
    data class Updated(val thing: Thing) : UpdateThingEvent
    data object Unchanged : UpdateThingEvent
    data object Failed : UpdateThingEvent
    data object Missing : UpdateThingEvent
}

internal sealed interface LifecycleEvent {
    data class ReminderUpdated(val reminderAtEpochMillis: Long, val timeZoneId: String) : LifecycleEvent
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

class KeeplyViewModel(
    private val repository: ThingRepository
) : ViewModel() {
    val things: StateFlow<List<Thing>> = repository.things.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

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
                SaveThingEvent.Saved(repository.createThing(draft))
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
                if (result.changed) UpdateThingEvent.Updated(result.thing) else UpdateThingEvent.Unchanged
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

    internal fun remindAgain(id: String, reminderAtEpochMillis: Long, timeZoneId: String) =
        runLifecycleChange(LifecycleEvent.ReminderUpdated(reminderAtEpochMillis, timeZoneId)) {
            repository.remindAgain(id, reminderAtEpochMillis, timeZoneId)
        }

    internal fun markDone(id: String) = runLifecycleChange(LifecycleEvent.MarkedDone) {
        repository.markDone(id)
    }

    internal fun reopen(id: String) = runLifecycleChange(LifecycleEvent.Reopened) {
        repository.reopen(id)
    }

    internal fun deleteThing(id: String) = runLifecycleChange(LifecycleEvent.Deleted) {
        repository.deleteThing(id)
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
        fun factory(repository: ThingRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(KeeplyViewModel::class.java))
                    return KeeplyViewModel(repository) as T
                }
            }
    }
}
