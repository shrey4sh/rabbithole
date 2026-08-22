package com.shrey4sh.rabbithole.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.shrey4sh.rabbithole.core.ui.nodeColor
import com.shrey4sh.rabbithole.data.local.SavedItemEntity
import com.shrey4sh.rabbithole.ui.saved.SavedViewModel
import com.shrey4sh.rabbithole.ui.placeholder.EmptyScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    storage: LocalStorageRepository,
) : ViewModel() {
    val items = storage.savedItems()
}

@Composable
fun SavedScreen(
    onOpenHole: (String) -> Unit = {},
) {
    val vm: SavedViewModel = hiltViewModel()
    val items by vm.items.collectAsState(initial = emptyList())

    if (items.isEmpty()) {
        EmptyScreen("Nothing saved yet.", "Follow something interesting and save it here.")
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item {
            Text("SAVED", style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp, color = MaterialTheme.colorScheme.outline),
                modifier = Modifier.padding(top = 24.dp, bottom = 14.dp))
        }
        items(items.size) { i ->
            val item = items[i]
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .clickable {
                    if (item.kind == "HOLE") onOpenHole(item.id.removePrefix("hole:"))
                }
                .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape)
                    .background(nodeColor(item.type).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Star, null, tint = nodeColor(item.type), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (item.subtitle.isNotEmpty()) {
                        Text(item.subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.outline,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = { vm.delete(item.id) }) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
