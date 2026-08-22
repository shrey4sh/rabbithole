package com.shrey4sh.rabbithole.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EXAMPLES = listOf(
    "Cyberpunk 2077", "Artificial Intelligence", "Joji", "Black holes", "Delhi", "Formula 1",
)

private val QUICK_START = listOf(
    "🎬 Movies", "🎮 Games", "🎵 Music", "🌍 Places",
    "🧠 Science", "👤 People", "💻 Technology", "📚 Books",
)

@Composable
fun HomeScreen(
    onSearch: (String) -> Unit,
    onSurpriseMe: () -> Unit,
    continueExploring: List<Pair<String, String>>,
) {
    var query by remember { mutableStateOf("") }
    var exampleIndex by remember { mutableIntStateOf(0) }
    val focus = LocalFocusManager.current

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2500)
            exampleIndex = (exampleIndex + 1) % EXAMPLES.size
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(64.dp))

        // top row: title + surprise me
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("RabbitHole",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp))
            IconButton(onClick = onSurpriseMe,
                modifier = Modifier.size(42.dp).background(Color(0xFF111318), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1E222D), RoundedCornerShape(12.dp))) {
                Icon(Icons.Default.Shuffle, "Surprise me",
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }

        Text("Start anywhere. See where it takes you.",
            style = MaterialTheme.typography.bodyMedium, color = Color(0xFF9AA0AE),
            modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(34.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("What are you curious about?", color = Color(0xFF6A7080)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search",
                tint = Color(0xFF8B7CFF)) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Clear, "Clear", tint = Color(0xFF6A7080))
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (query.isNotBlank()) { focus.clearFocus(); onSearch(query.trim()) }
            }),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF8B7CFF),
                unfocusedBorderColor = Color(0xFF232838),
                focusedContainerColor = Color(0xFF111318),
                unfocusedContainerColor = Color(0xFF111318),
                cursorColor = Color(0xFF8B7CFF),
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))
        val exAlpha by animateFloatAsState(if (query.isEmpty()) 1f else 0f, tween(400), label = "ex")
        Box(modifier = Modifier.fillMaxWidth().clickable(enabled = query.isEmpty()) {
            onSearch(EXAMPLES[exampleIndex])
        }) {
            Text("Try “${EXAMPLES[exampleIndex]}”",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = Color(0xFF9AA0AE).copy(alpha = exAlpha),
                modifier = Modifier.padding(start = 6.dp))
        }

        Spacer(Modifier.height(30.dp))
        Text("QUICK START", style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 2.sp, color = Color(0xFF9AA0AE)))
        Spacer(Modifier.height(12.dp))

        QUICK_START.chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { chip ->
                    Box(modifier = Modifier.weight(1f).height(40.dp)
                        .background(Color(0xFF111318), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF1E222D), RoundedCornerShape(12.dp))
                        .clickable { onSearch(chip.dropWhile { !it.isLetter() }.trim()) },
                        contentAlignment = Alignment.Center) {
                        Text(chip, fontSize = 13.sp)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("CONTINUE EXPLORING", style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 2.sp, color = Color(0xFF9AA0AE)))
        Spacer(Modifier.height(12.dp))

        if (continueExploring.isEmpty()) {
            Text("Your rabbit holes will appear here.",
                style = MaterialTheme.typography.bodySmall, color = Color(0xFF6A7080))
        } else {
            continueExploring.forEach { (title, sub) ->
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    .background(Color(0xFF111318), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF1E222D), RoundedCornerShape(14.dp))
                    .clickable { onSearch(title) }
                    .padding(16.dp)) {
                    Column {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9AA0AE))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
