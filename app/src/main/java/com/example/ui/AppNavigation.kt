package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.ViceStudioHeader
import com.example.ui.screens.ClipMakerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.SeoStudioScreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ViceCardElevated
import com.example.ui.theme.ViceObsidian
import com.example.ui.theme.ViceSurface
import com.example.viewmodel.ClipMakerViewModel
import com.example.viewmodel.DashboardViewModel
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.SeoViewModel
import com.example.viewmodel.StudioContainer

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_CLIP_MAKER = "clip_maker"
const val ROUTE_EDITOR = "editor"
const val ROUTE_SEO_STUDIO = "seo_studio"

data class StudioTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val studioTabs = listOf(
    StudioTab(ROUTE_DASHBOARD, "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
    StudioTab(ROUTE_CLIP_MAKER, "Clip AI", Icons.Filled.MovieCreation, Icons.Outlined.MovieCreation, "nav_clip_maker"),
    StudioTab(ROUTE_EDITOR, "Editor", Icons.Filled.ContentCut, Icons.Outlined.ContentCut, "nav_editor"),
    StudioTab(ROUTE_SEO_STUDIO, "SEO Pro", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_seo_studio")
)

@Composable
fun GtaViClipAiApp(container: StudioContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DASHBOARD
    val appContext = LocalContext.current.applicationContext

    val dashboardVm: DashboardViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    projectDao = container.database.projectDao(),
                    clipDao = container.database.clipDao(),
                    videoEngine = container.videoEngine
                )
            }
        }
    )
    val clipMakerVm: ClipMakerViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ClipMakerViewModel(
                    appContext = appContext,
                    projectDao = container.database.projectDao(),
                    clipDao = container.database.clipDao(),
                    youtubeRepository = container.youtubeRepository,
                    aiRepository = container.aiRepository,
                    videoEngine = container.videoEngine
                )
            }
        }
    )
    val editorVm: EditorViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                EditorViewModel(
                    clipDao = container.database.clipDao(),
                    videoEngine = container.videoEngine
                )
            }
        }
    )
    val seoVm: SeoViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SeoViewModel(
                    aiRepository = container.aiRepository,
                    seoDao = container.database.seoDao()
                )
            }
        }
    )

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ViceObsidian)
    ) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ViceObsidian,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                ViceStudioHeader(modifier = Modifier.statusBarsPadding())
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = ViceSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        studioTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = ViceCardElevated,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(containerColor = ViceSurface) {
                        studioTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = NeonPink,
                                    selectedTextColor = NeonPink,
                                    indicatorColor = ViceCardElevated,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_DASHBOARD,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .weight(1f)
                    ) {
                        composable(ROUTE_DASHBOARD) {
                            DashboardScreen(
                                viewModel = dashboardVm,
                                onNavigateToClipMaker = { navigateToTab(ROUTE_CLIP_MAKER) },
                                onNavigateToEditor = { navigateToTab(ROUTE_EDITOR) }
                            )
                        }
                        composable(ROUTE_CLIP_MAKER) {
                            ClipMakerScreen(viewModel = clipMakerVm)
                        }
                        composable(ROUTE_EDITOR) {
                            EditorScreen(viewModel = editorVm)
                        }
                        composable(ROUTE_SEO_STUDIO) {
                            SeoStudioScreen(viewModel = seoVm)
                        }
                    }
                }
            }
        }
    }
}
