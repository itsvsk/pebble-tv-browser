package com.tvbrowser.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
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
import com.tvbrowser.core.design.PebbleMotion
import com.tvbrowser.core.design.RootShell
import com.tvbrowser.core.cursor.CursorMode
import com.tvbrowser.feature.bookmarks.BookmarksScreen
import com.tvbrowser.feature.browser.BrowserScreen
import com.tvbrowser.feature.downloads.DownloadsScreen
import com.tvbrowser.feature.history.HistoryScreen
import com.tvbrowser.feature.home.HomeScreen
import com.tvbrowser.feature.settings.SettingsScreen
import com.tvbrowser.feature.tabs.TabsOverviewScreen

@Composable
fun AppNavHost(
    cursorMode: CursorMode,
    onSetCursorActive: (Boolean) -> Unit,
    onSetCursorPreferred: (Boolean) -> Unit,
) {
    val nav = rememberNavController()
    val shellVm: ShellViewModel = hiltViewModel()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onToggleCursor = { onSetCursorPreferred(!cursorMode.preferredActive) }
    fun openBrowser(tabId: String) {
        onSetCursorPreferred(true)
        nav.navigate(Routes.browser(tabId))
    }
    val navFocusRequesters = remember { NavDestination.entries.associateWith { FocusRequester() } }
    val homeNavFocus = navFocusRequesters.getValue(NavDestination.Home)
    val tabsNewTabFocus = remember { FocusRequester() }
    var focusedNav by remember { mutableStateOf<NavDestination?>(null) }
    // Start every destination with its chrome visible in the first frame. Resetting in a
    // LaunchedEffect would briefly reuse the previous browser entry's hidden state.
    var browserChromeVisible by remember(backStackEntry?.id) { mutableStateOf(true) }
    var focusTopBarWhenShown by remember(backStackEntry?.id) { mutableStateOf(true) }

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

    // Each Browse entry starts in pointer mode; changing modes within that entry remains manual.
    LaunchedEffect(backStackEntry?.id) {
        if (currentRoute != null) onSetCursorPreferred(currentRoute == Routes.BROWSER)
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
                cursorMode.active || focusedNav != active,
        ) {
            if (cursorMode.active) onSetCursorActive(false)
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
        activeDownFocusRequester = tabsNewTabFocus.takeIf { currentRoute == Routes.TABS },
        onFocusedDestinationChanged = { destination, focused ->
            if (focused) focusedNav = destination
            else if (focusedNav == destination) focusedNav = null
        },
        cursorModeActive = cursorMode.preferredActive.takeIf { currentRoute == Routes.BROWSER },
        onToggleCursor = onToggleCursor,
        showRail = currentRoute != Routes.BROWSER || browserChromeVisible,
        onSelect = { dest ->
            if (dest == NavDestination.Browser) {
                if (currentRoute != Routes.BROWSER) {
                    shellVm.openBrowser(::openBrowser)
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
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            // Moving a whole destination repositions its WebView or lazy list every frame.
            // Let the persistent top rail provide directional motion; crossfade content.
            enterTransition = { fadeIn(tween(PebbleMotion.ScreenEnterMs, easing = PebbleMotion.Standard)) },
            exitTransition = { fadeOut(tween(PebbleMotion.ScreenExitMs)) },
            popEnterTransition = { fadeIn(tween(PebbleMotion.ScreenEnterMs, easing = PebbleMotion.Standard)) },
            popExitTransition = { fadeOut(tween(PebbleMotion.ScreenExitMs)) },
        ) {
            composable(Routes.HOME) {
                handleBack()
                HomeScreen(
                    homeNavFocus = homeNavFocus,
                    onOpenUrl = ::openBrowser,
                    onOpenBrowse = { shellVm.openBrowser(::openBrowser) },
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
                    cursorMode = cursorMode,
                    chromeVisible = browserChromeVisible,
                    topNavFocusRequester = navFocusRequesters.getValue(NavDestination.Browser),
                    onWebNavigation = { browserChromeVisible = false },
                    onResumePointer = {
                        onSetCursorActive(true)
                        browserChromeVisible = false
                    },
                    onShowChrome = { browserChromeVisible = true },
                    onBackHome = { nav.popBackStack(Routes.HOME, inclusive = false) },
                )
            }
            composable(Routes.TABS) {
                handleBack()
                TabsOverviewScreen(
                    topNavFocusRequester = navFocusRequesters.getValue(NavDestination.Tabs),
                    newTabFocusRequester = tabsNewTabFocus,
                    onOpenTab = ::openBrowser,
                    onNewTab = ::openBrowser,
                )
            }
            composable(Routes.BOOKMARKS) {
                handleBack()
                BookmarksScreen(
                    onOpenUrl = ::openBrowser,
                    onOpenBrowse = { shellVm.openBrowser(::openBrowser) },
                    onOpenHistory = { nav.navigate(Routes.HISTORY) },
                )
            }
            composable(Routes.HISTORY) {
                handleBack()
                HistoryScreen(onOpenUrl = ::openBrowser)
            }
            composable(Routes.DOWNLOADS) {
                handleBack()
                DownloadsScreen()
            }
            composable(Routes.SETTINGS) {
                handleBack()
                SettingsScreen(onOpenDownloads = {
                    nav.navigate(Routes.DOWNLOADS) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                })
            }
        }
    }
}
