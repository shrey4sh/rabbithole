package com.shrey4sh.rabbithole.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shrey4sh.rabbithole.data.local.RabbitHoleEntity
import com.shrey4sh.rabbithole.data.repository.LocalStorageRepository
import com.shrey4sh.rabbithole.domain.model.Edge
import com.shrey4sh.rabbithole.domain.model.Node
import com.shrey4sh.rabbithole.domain.model.NodeType
import com.shrey4sh.rabbithole.domain.model.RabbitHole
import com.shrey4sh.rabbithole.ui.placeholder.EmptyScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val storage: LocalStorageRepository,
) : ViewModel() {
    val holes = storage.allHoles()

    fun restore(id: String, onRestored: (RabbitHole) -> Unit) = viewModelScope.launch {
        storage.restoreHole(id)?.let(onRestored)
    }
}

private val json = Json { ignoreUnknownKeys = true }

@Composable
fun HistoryScreen(
    vm: HistoryViewModel = hiltViewModel(),
    onOpenHole: (RabbitHole) -> Unit,
) {
    val holes by vm.holes.collectAsState(initial = emptyList())

    if (holes.isEmpty()) {
        EmptyScreen("Your rabbit holes will appear here.", "")
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item {
            Text("HISTORY", style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp, color = MaterialTheme.colorScheme.outline),
                modifier = Modifier.padding(top = 24.dp, bottom = 14.dp))
        }
        items(holes.size) { i ->
            val h = holes[i]
            val whenTxt = java.text.SimpleDateFormat("d MMM, HH:mm",
                java.util.Locale.getDefault()).format(java.util.Date(h.updatedAt))
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .clickable {
                    vm.restore(h.id) { hole -> onOpenHole(hole) }
                }
                .padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(h.id.replace("-", " ").replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(whenTxt, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
                Spacer(Modifier.height(4.dp))
                Text("${h.nodeCount} nodes · ${h.edgeCount} connections",
                    fontSize = 11.5.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
