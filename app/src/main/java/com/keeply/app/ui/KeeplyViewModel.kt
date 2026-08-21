package com.keeply.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.keeply.app.data.ThingRepository
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
    data object Saved : SaveThingEvent
    data object Failed : SaveThingEvent
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

    fun createThing(draft: NewThingDraft) {
        if (!_isSaving.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            val event = try {
                repository.createThing(draft)
                SaveThingEvent.Saved
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
