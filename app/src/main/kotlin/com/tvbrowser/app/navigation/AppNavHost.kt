package com.tvbrowser.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tvbrowser.core.design.NavDestination
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
fun AppNavHost(cursorState: CursorState) {
    val nav = rememberNavController()
    val shellVm: ShellViewModel = hiltViewModel()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val homeNavFocus = remember { FocusRequester() }

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

    RootShell(
        active = active,
        homeFocusRequester = homeNavFocus,
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
                HomeScreen(
                    homeNavFocus = homeNavFocus,
                    onOpenUrl = { tabId -> nav.navigate(Routes.browser(tabId)) },
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
                val tabId = backStack.arguments?.getString("tabId").orEmpty()
                BrowserScreen(
                    tabId = tabId,
                    cursorState = cursorState,
                    onBackHome = { nav.popBackStack(Routes.HOME, inclusive = false) },
                    onOpenTabs = { nav.navigate(Routes.TABS) },
                )
            }
            composable(Routes.TABS) {
                TabsOverviewScreen(
                    onOpenTab = { tabId -> nav.navigate(Routes.browser(tabId)) },
                    onNewTab = { tabId -> nav.navigate(Routes.browser(tabId)) },
                )
            }
            composable(Routes.BOOKMARKS) {
                BookmarksScreen(
                    onOpenUrl = { tabId -> nav.navigate(Routes.browser(tabId)) },
                    onOpenHistory = { nav.navigate(Routes.HISTORY) },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onOpenUrl = { tabId -> nav.navigate(Routes.browser(tabId)) })
            }
            composable(Routes.DOWNLOADS) {
                DownloadsScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}
