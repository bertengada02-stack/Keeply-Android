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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface SaveThingEvent {
    data object Saved : SaveThingEvent
    data object Failed : SaveThingEvent
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
