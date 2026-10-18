package com.tvbrowser.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tvbrowser.core.design.NavDestination
import com.tvbrowser.core.design.KeyHint
import com.tvbrowser.core.design.RootShell
import com.tvbrowser.core.cursor.CursorState
import com.tvbrowser.feature.bookmarks.BookmarksScreen
import com.tvbrowser.feature.browser.BrowserScreen
import com.tvbrowser.feature.downloads.DownloadsScreen
import com.tvbrowser.feature.history.HistoryScreen
import com.tvbrowser.feature.home.HomeScreen
import com.tvbrowser.feature.settings.SettingsScreen
import com.tvbrowser.feature.tabs.TabsOverviewScreen

@Composable
fun AppNavHost(cursorState: CursorState, onToggleCursor: () -> Unit) {
    val nav = rememberNavController()
    val shellVm: ShellViewModel = hiltViewModel()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val navFocusRequesters = remember { NavDestination.entries.associateWith { FocusRequester() } }
    val homeNavFocus = navFocusRequesters.getValue(NavDestination.Home)
    var focusedNav by remember { mutableStateOf<NavDestination?>(null) }
    var browserChromeVisible by remember { mutableStateOf(true) }
    var focusTopBarWhenShown by remember { mutableStateOf(false) }

    val active = when (currentRoute) {
        Routes.HOME -> NavDestination.Home
        Routes.BROWSER -> NavDestination.Browser
        Routes.TABS -> NavDestination.Tabs
        Routes.BOOKMARKS -> NavDestination.Saved
        Routes.HISTORY -> NavDestination.Saved
        Routes.DOWNLOADS -> NavDestination.Files
        Routes.SETTINGS -> NavDestination.Settings
        else -> NavDestination.Home
    }

    LaunchedEffect(backStackEntry?.id) {
        browserChromeVisible = true
        focusTopBarWhenShown = true
    }
    LaunchedEffect(browserChromeVisible, focusTopBarWhenShown, active) {
        if (browserChromeVisible && focusTopBarWhenShown) {
            navFocusRequesters.getValue(active).requestFocus()
            focusTopBarWhenShown = false
        }
    }
    val handleBack: @Composable () -> Unit = {
        BackHandler(
            enabled = (currentRoute == Routes.BROWSER && !browserChromeVisible) ||
                cursorState.active || focusedNav != active,
        ) {
            if (cursorState.active) onToggleCursor()
            if (currentRoute == Routes.BROWSER && !browserChromeVisible) {
                browserChromeVisible = true
                focusTopBarWhenShown = true
            } else {
                navFocusRequesters.getValue(active).requestFocus()
            }
        }
    }

    RootShell(
        active = active,
        focusRequesters = navFocusRequesters,
        onFocusedDestinationChanged = { destination, focused ->
            if (focused) focusedNav = destination
            else if (focusedNav == destination) focusedNav = null
        },
        cursorModeActive = cursorState.active.takeIf { currentRoute == Routes.BROWSER },
        onToggleCursor = onToggleCursor,
        showRail = currentRoute != Routes.BROWSER || browserChromeVisible,
        showHints = currentRoute != Routes.BROWSER,
        hints = listOf(
            KeyHint("OK", "Select"),
            KeyHint("BACK", "Return / Exit"),
            KeyHint("D-PAD", "Navigate"),
        ),
        onSelect = { dest ->
            if (dest == NavDestination.Browser) {
                if (currentRoute != Routes.BROWSER) {
                    shellVm.openBrowser { tabId -> nav.navigate(Routes.browser(tabId)) }
                }
                return@RootShell
            }
            val route = when (dest) {
                NavDestination.Home -> Routes.HOME
                NavDestination.Browser -> Routes.HOME
                NavDestination.Tabs -> Routes.TABS
                NavDestination.Saved -> Routes.BOOKMARKS
                NavDestination.History -> Routes.HISTORY
                NavDestination.Files -> Routes.DOWNLOADS
                NavDestination.Settings -> Routes.SETTINGS
            }
            if (currentRoute != route) {
                nav.navigate(route) {
                    popUpTo(Routes.HOME) { inclusive = false }
                    launchSingleTop = true
                }
            }
        },
    ) {
        NavHost(navController = nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                handleBack()
                HomeScreen(
                    homeNavFocus = homeNavFocus,
                    onOpenUrl = { tabId -> nav.navigate(Routes.browser(tabId)) },
                    onOpenBrowse = { shellVm.openBrowser { tabId -> nav.navigate(Routes.browser(tabId)) } },
                    onOpenTabs = { nav.navigate(Routes.TABS) },
                    onOpenBookmarks = { nav.navigate(Routes.BOOKMARKS) },
                    onOpenHistory = { nav.navigate(Routes.HISTORY) },
                    onOpenDownloads = { nav.navigate(Routes.DOWNLOADS) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(
                route = Routes.BROWSER,
                arguments = listOf(navArgument("tabId") { type = NavType.StringType }),
            ) { backStack ->
                handleBack()
                val tabId = backStack.arguments?.getString("tabId").orEmpty()
                BrowserScreen(
                    tabId = tabId,
                    cursorState = cursorState,
                    chromeVisible = browserChromeVisible,
                    onWebNavigation = { browserChromeVisible = false },
                    onShowChrome = { browserChromeVisible = true },
                    onBackHome = { nav.popBackStack(Routes.HOME, inclusive = false) },
                )
            }
            composable(Routes.TABS) {
                handleBack()
                TabsOverviewScreen(
                    onOpenTab = { tabId -> nav.navigate(Routes.browser(tabId)) },
                    onNewTab = { tabId -> nav.navigate(Routes.browser(tabId)) },
                )
            }
            composable(Routes.BOOKMARKS) {
                handleBack()
                BookmarksScreen(
                    onOpenUrl = { tabId -> nav.navigate(Routes.browser(tabId)) },
                    onOpenHistory = { nav.navigate(Routes.HISTORY) },
                )
            }
            composable(Routes.HISTORY) {
                handleBack()
                HistoryScreen(onOpenUrl = { tabId -> nav.navigate(Routes.browser(tabId)) })
            }
            composable(Routes.DOWNLOADS) {
                handleBack()
                DownloadsScreen()
            }
            composable(Routes.SETTINGS) {
                handleBack()
                SettingsScreen()
            }
        }
    }
}
