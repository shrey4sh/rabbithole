package com.shrey4sh.rabbithole.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    onClearHistory: () -> Unit,
    onClearCache: () -> Unit,
) {
    var animations by remember { mutableStateOf(true) }
    var connectionLabels by remember { mutableStateOf(false) }
    var analytics by remember { mutableStateOf(false) }
    var density by remember { mutableStateOf("Normal") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {

        Text("SETTINGS", style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 2.sp, color = MaterialTheme.colorScheme.outline),
            modifier = Modifier.padding(top = 24.dp, bottom = 18.dp))

        SettingHeader("Appearance")
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Theme", fontSize = 14.sp)
                Text("Dark-first design; light coming soon",
                    fontSize = 11.5.sp, color = MaterialTheme.colorScheme.outline)
            }
            Text("Dark", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }

        SettingHeader("Graph behavior")
        ToggleRow("Animations", "Node appearance & transitions", animations) { animations = it }
        ToggleRow("Connection labels", "Show relationship text on edges", connectionLabels) { connectionLabels = it }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column { Text("Node density", fontSize = 14.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Sparse", "Normal", "Dense").forEach { opt ->
                    Text(opt, fontSize = 12.sp,
                        color = if (density == opt) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(4.dp).clickable { density = opt })
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1E222D), modifier = Modifier.padding(vertical = 8.dp))

        SettingHeader("Data")
        ActionRow("Clear history", "Remove all explored rabbit holes", onClearHistory)
        ActionRow("Clear cache", "Free stored images and data", onClearCache)

        HorizontalDivider(color = Color(0xFF1E222D), modifier = Modifier.padding(vertical = 8.dp))

        SettingHeader("Privacy")
        ToggleRow("Analytics", "Share anonymous usage data", analytics) { analytics = it }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = Color(0xFF1E222D))

        SettingHeader("About")
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Version", fontSize = 14.sp)
            Text("0.4.0-phase6", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        }
        Text("Open source licenses", fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp))
        Text("Data from Launch Library 2 & Wikipedia (CC BY-SA)",
            fontSize = 11.5.sp, color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 30.dp))
    }
}

@Composable
private fun SettingHeader(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall.copy(
        letterSpacing = 2.sp, color = MaterialTheme.colorScheme.outline),
        modifier = Modifier.padding(vertical = 10.dp))
}

@Composable
private fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp)
            Text(sub, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.outline)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionRow(title: String, sub: String, action: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
        .clickable { action() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
            Text(sub, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
