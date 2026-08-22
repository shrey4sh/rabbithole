package com.shrey4sh.rabbithole.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shrey4sh.rabbithole.domain.model.RabbitHole
import com.shrey4sh.rabbithole.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GraphUiState {
    data object Loading : GraphUiState
    data class Ready(val hole: RabbitHole) : GraphUiState
    data object Empty : GraphUiState
    data class Error(val message: String) : GraphUiState
}

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val repo: TopicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<GraphUiState>(GraphUiState.Loading)
    val state: StateFlow<GraphUiState> = _state

    fun search(query: String) {
        _state.value = GraphUiState.Loading
        viewModelScope.launch {
            repo.searchTopic(query).collect { hole ->
                _state.value = if (hole != null) GraphUiState.Ready(hole) else GraphUiState.Empty
            }
        }
    }

    fun surpriseMe() {
        viewModelScope.launch {
            val hole = repo.randomTopic()
            _state.value = GraphUiState.Ready(hole)
        }
    }
}
