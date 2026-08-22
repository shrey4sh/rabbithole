package com.shrey4sh.rabbithole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.shrey4sh.rabbithole.core.ui.RabbitHoleTheme
import com.shrey4sh.rabbithole.ui.discovery.DiscoveryLoadingScreen
import com.shrey4sh.rabbithole.ui.graph.GraphScreen
import com.shrey4sh.rabbithole.ui.graph.GraphUiState
import com.shrey4sh.rabbithole.ui.graph.GraphViewModel
import com.shrey4sh.rabbithole.ui.home.HomeScreen
import com.shrey4sh.rabbithole.ui.placeholder.EmptyScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RabbitHoleTheme { RootApp() }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: @Composable () -> Unit)

@Composable
fun RootApp() {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab("home", "Home") { Icon(Icons.Default.Home, null) },
        Tab("explore", "Explore") { Icon(Icons.Default.Explore, null) },
        Tab("saved", "Saved") { Icon(Icons.Default.Star, null) },
        Tab("history", "History") { Icon(Icons.Default.History, null) },
        Tab("settings", "Settings") { Icon(Icons.Default.Settings, null) },
    )
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "home"
    val showBar = current in tabs.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) NavigationBar(containerColor = androidx.compose.ui.graphics.Color(0xFF111318)) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                launchSingleTop = true
                                popUpTo("home") { saveState = true }
                                restoreState = true
                            }
                        },
                        icon = tab.icon,
                        label = { Text(tab.label, fontSize = 11.sp) })
                }
            }
        }) { pad ->
        NavHost(navController = nav, startDestination = "home",
            modifier = Modifier.padding(pad)) {
            composable("home") {
                HomeScreen(
                    onSearch = { q -> nav.navigate("discovery/${java.net.URLEncoder.encode(q, "UTF-8")}") },
                    onSurpriseMe = { nav.navigate("discovery/@random") },
                    continueExploring = emptyList(),
                )
            }
            composable("discovery/{query}") { entry ->
                val query = java.net.URLDecoder.decode(entry.arguments?.getString("query") ?: "", "UTF-8")
                val vm: GraphViewModel = hiltViewModel()
                androidx.compose.runtime.LaunchedEffect(query) {
                    if (query == "@random") vm.surpriseMe() else vm.search(query)
                }
                GraphRoute(vm, onBack = { nav.popBackStack() }, loadingTopic = query)
            }
            composable("explore") { EmptyScreen("Explore", "Discover trending rabbit holes — coming soon.") }
            composable("saved") { EmptyScreen("Nothing saved yet.", "Follow something interesting and save it here.") }
            composable("history") { EmptyScreen("Your rabbit holes will appear here.", "") }
            composable("settings") { EmptyScreen("Settings", "Appearance, graph behavior, data & privacy.") }
        }
    }
}

@Composable
private fun GraphRoute(vm: GraphViewModel, onBack: () -> Unit, loadingTopic: String) {
    when (val s = vm.state.collectAsState().value) {
        is GraphUiState.Loading -> DiscoveryLoadingScreen(loadingTopic)
        is GraphUiState.Ready -> GraphScreen(hole = s.hole, onBack = onBack)
        is GraphUiState.Empty -> EmptyScreen(
            "We couldn't find much on that topic.", "Try something more specific.")
        is GraphUiState.Error -> EmptyScreen(
            "RabbitHole couldn't reach one of its sources.", "Reconnect and try again.")
    }
}
