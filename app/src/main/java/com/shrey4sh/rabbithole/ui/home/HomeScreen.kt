package com.shrey4sh.rabbithole.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import com.shrey4sh.rabbithole.core.ui.Surface1
import com.shrey4sh.rabbithole.core.ui.TextSecondary
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

private data class Category(val label: String, val icon: ImageVector)

private val CATEGORIES = listOf(
    Category("Movies", Icons.Default.Movie),
    Category("Games", Icons.Default.SportsEsports),
    Category("Music", Icons.Default.MusicNote),
    Category("Places", Icons.Default.Public),
    Category("Science", Icons.Default.Science),
    Category("People", Icons.Default.Person),
    Category("Books", Icons.Default.MenuBook),
    Category("Technology", Icons.Default.Memory),
)

private val EXAMPLES = listOf(
    "Artificial Intelligence",
    "Why are black holes black?",
    "The history of video games",
    "How did the internet begin?",
    "Cyberpunk 2077",
    "The Roman Empire",
    "Why do we dream?",
)

private val SUGGESTIONS = listOf(
    "Why do black holes exist?",
    "The story of the Internet",
    "How did video games evolve?",
)

@Composable
fun HomeScreen(
    onSearch: (String) -> Unit,
    onSurpriseMe: () -> Unit = {},
    continueExploring: List<com.shrey4sh.rabbithole.data.local.RabbitHoleEntity> = emptyList(),
) {
    var query by remember { mutableStateOf("") }
    var exampleIndex by remember { mutableStateOf(0) }
    val focusRequester = FocusRequester()

    // open keyboard immediately when Home appears
    LaunchedEffect(Unit) {
        delay(300)
        runCatching { focusRequester.requestFocus() }
    }

    // rotate examples every 4s with fade
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            exampleIndex = (exampleIndex + 1) % EXAMPLES.size
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {

        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("RabbitHole", fontSize = 30.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text("Start anywhere. See where it takes you.",
                    fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
            }
            IconButton(onClick = onSurpriseMe, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Surprise me",
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.height(26.dp))

        // ---- search field (opens keyboard immediately via focusRequester below) ----
        var focused by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(1.dp, if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(16.dp))) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Icon(Icons.Default.Search, contentDescription = null,
                    tint = if (focused) MaterialTheme.colorScheme.primary else TextSecondary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.size(12.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { if (query.isNotBlank()) onSearch(query.trim()) }),
                    modifier = Modifier.weight(1f)
                        .focusRequester(focusRequester)
                        .onKeyEvent {
                            if (it.type == KeyEventType.KeyUp && it.key == Key.Enter &&
                                query.isNotBlank()) { onSearch(query.trim()); true } else false
                        },
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("What are you curious about?",
                            fontSize = 17.sp, color = TextSecondary, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        inner()
                    })
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Clear", tint = TextSecondary,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // rotating "Try …" with subtle fade
        AnimatedContent(targetState = exampleIndex, transitionSpec = {
            fadeIn(tween(500)) togetherWith fadeOut(tween(500))
        }, label = "example") { idx ->
            Text("Try \"${EXAMPLES[idx]}\"", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp).clickable {
                    onSearch(EXAMPLES[idx])
                })
        }

        Spacer(Modifier.height(26.dp))

        // ---- QUICK START: 4×2 grid of vector-icon cards, no clipping ----
        Text("QUICK START", fontSize = 11.sp, letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CATEGORIES.chunked(4).forEach { rowCats ->
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowCats.forEach { cat ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                                .aspectRatio(0.82f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Surface1)
                                .clickable { onSearch(cat.label) }
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.Center) {
                            Icon(cat.icon, contentDescription = cat.label,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(cat.label, fontSize = 11.sp, maxLines = 1,
                                overflow = TextOverflow.Visible,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    // pad short rows to keep equal widths
                    repeat(4 - rowCats.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ---- CONTINUE EXPLORING / empty state with suggestions ----
        Text("CONTINUE EXPLORING", fontSize = 11.sp, letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))

        if (continueExploring.isEmpty()) {
            Text("Your rabbit holes will appear here.",
                fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(16.dp))
            Text("NEED AN IDEA?", fontSize = 11.sp, letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(10.dp))
            SUGGESTIONS.forEach { suggestion ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Surface1)
                    .clickable { onSearch(suggestion) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null,
                        tint = TextSecondary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(suggestion, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        } else {
            continueExploring.take(5).forEach { hole ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Surface1)
                    .clickable { onSearch(hole.id.replace("-", " ")) }
                    .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(hole.id.replace("-", " ").replaceFirstChar { it.uppercase() },
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${hole.nodeCount} nodes · explored ${formatWhen(hole.updatedAt)}",
                            fontSize = 11.5.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Text("Continue →", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun formatWhen(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    val mins = diff / 60000
    return when {
        mins < 60 -> "${mins}m ago"
        mins < 1440 -> "${mins / 60}h ago"
        else -> "${mins / 1440}d ago"
    }
}
