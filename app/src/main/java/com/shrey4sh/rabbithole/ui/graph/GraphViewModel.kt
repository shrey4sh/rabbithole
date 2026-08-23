package com.shrey4sh.rabbithole.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shrey4sh.rabbithole.data.mock.DeeperLayers
import com.shrey4sh.rabbithole.data.mock.MockData
import com.shrey4sh.rabbithole.domain.model.Edge
import com.shrey4sh.rabbithole.domain.model.Node
import com.shrey4sh.rabbithole.domain.model.RabbitHole
import com.shrey4sh.rabbithole.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GraphUiState {
    data object Loading : GraphUiState
    data class Ready(val hole: RabbitHole) : GraphUiState
    data object Empty : GraphUiState
    data class Error(val message: String) : GraphUiState
    /** Query is ambiguous — show clean entity choices before building the graph. */
    data class Disambiguate(
        val query: String,
        val options: List<com.shrey4sh.rabbithole.domain.model.KnowledgeEntity>,
    ) : GraphUiState
}

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val repo: TopicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<GraphUiState>(GraphUiState.Loading)
    val state: StateFlow<GraphUiState> = _state

    // exploration path: sequence of node ids visited (root first)
    private val _path = MutableStateFlow<List<PathEntry>>(emptyList())
    val path: StateFlow<List<PathEntry>> = _path

    // rabbit hole depth counter
    private val _depth = MutableStateFlow(0)
    val depth: StateFlow<Int> = _depth

    // expanding state for "take me deeper" animation
    private val _expanding = MutableStateFlow(false)
    val expanding: StateFlow<Boolean> = _expanding

    fun search(query: String) {
        _state.value = GraphUiState.Loading
        _path.value = emptyList()
        _depth.value = 0
        viewModelScope.launch {
            repo.searchTopic(query).collect { hole ->
                if (hole != null) {
                    _state.value = GraphUiState.Ready(hole)
                    _path.value = listOf(PathEntry(hole.rootNodeId, titleOf(hole, hole.rootNodeId), null))
                } else {
                    _state.value = GraphUiState.Empty
                }
            }
        }
    }

    /** User picked a meaning from the disambiguation sheet — build the graph around it. */
    fun chooseEntity(entity: com.shrey4sh.rabbithole.domain.model.KnowledgeEntity) {
        _state.value = GraphUiState.Loading
        viewModelScope.launch {
            (repo as? com.shrey4sh.rabbithole.data.repository.WikipediaTopicRepository)
                ?.resolveChosen(entity)
                ?.collect { hole ->
                    if (hole != null) {
                        _state.value = GraphUiState.Ready(hole)
                        _path.value = listOf(PathEntry(hole.rootNodeId, titleOf(hole, hole.rootNodeId), null))
                    } else {
                        _state.value = GraphUiState.Empty
                    }
                }
        }
    }

    fun surpriseMe() {
        _depth.value = 0
        _path.value = emptyList()
        viewModelScope.launch {
            _state.value = GraphUiState.Ready(repo.randomTopic())
        }
    }

    private fun titleOf(hole: RabbitHole, nodeId: String) =
        hole.nodes.find { it.id == nodeId }?.title ?: nodeId

    /**
     * TAKE ME DEEPER: expand from a node — add new nodes/edges organically.
     */
    fun takeMeDeeper(hole: RabbitHole, fromNodeId: String) {
        if (_expanding.value) return
        viewModelScope.launch {
            _expanding.value = true
            delay(350) // organic feel

            val layer = DeeperLayers.get(fromNodeId, titleOf(hole, fromNodeId))
            val existingIds = hole.nodes.map { it.id }.toSet()

            val newNodes = DeeperLayers.nodesFor(layer.newNodeIds)
                .filter { it.id !in existingIds }
            val newEdges = layer.newEdges.map { (a, b, rel) ->
                Edge(id = "$a-$b-$rel-${System.currentTimeMillis()}",
                     sourceNodeId = a, targetNodeId = b, relationship = rel)
            }.filter { e -> e.sourceNodeId in existingIds + newNodes.map { it.id }
                          && e.targetNodeId in existingIds + newNodes.map { it.id } }

            val updated = hole.copy(
                nodes = hole.nodes + newNodes,
                edges = hole.edges + newEdges,
                updatedAt = System.currentTimeMillis(),
            )
            _state.value = GraphUiState.Ready(updated)
            _depth.value += 1

            // append to path
            val fromTitle = titleOf(hole, fromNodeId)
            val firstNew = newNodes.firstOrNull()
            if (firstNew != null) {
                _path.value = _path.value + PathEntry(firstNew.id, firstNew.title, fromTitle)
            }
            _expanding.value = false
        }
    }

    /** "How did I get here?" — return to a previous node: truncate path forward. */
    fun jumpBackTo(nodeId: String) {
        val p = _path.value
        val idx = p.indexOfFirst { it.nodeId == nodeId }
        if (idx >= 0) _path.value = p.take(idx + 1)
    }
}
