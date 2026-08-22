package com.shrey4sh.rabbithole.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shrey4sh.rabbithole.data.local.SavedItemEntity
import com.shrey4sh.rabbithole.data.repository.LocalStorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val storage: LocalStorageRepository,
) : ViewModel() {
    val items: Flow<List<SavedItemEntity>> = storage.savedItems()

    fun delete(id: String) = viewModelScope.launch { storage.unsave(id) }
}
