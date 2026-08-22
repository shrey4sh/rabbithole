package com.shrey4sh.rabbithole.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shrey4sh.rabbithole.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: TopicRepository,
) : ViewModel() {
    private val _pendingQuery = MutableStateFlow<String?>(null)
    val pendingQuery: StateFlow<String?> = _pendingQuery

    fun search(query: String) { _pendingQuery.value = query }
    fun consumeQuery() { _pendingQuery.value = null }

    fun surpriseMe() {
        viewModelScope.launch { _pendingQuery.value = "@random:" + repo.randomTopic().id }
    }
}
