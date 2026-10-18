package com.fin.mf.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fin.mf.core.theme.VaultThemeManager
import com.fin.mf.tools.news.ui.NewsTerminalScreen
import com.fin.mf.tools.terminal.ui.MarketTerminalScreen
import com.fin.mf.ui.PortfolioViewModel
import com.fin.mf.ui.screens.PortfolioTrackerWorkspace
import com.fin.mf.ui.screens.StudioSettingsScreen
import com.fin.mf.ui.screens.VaultHubScreen

/**
 * Centralized route definitions for the FinBaba Financial Studio & Tool Vault.
 */
object FinBabaDestinations {
    const val VAULT_HUB_ROUTE = "vault_hub"
    const val PORTFOLIO_TOOL_BASE_ROUTE = "tool_portfolio_tracker"
    const val MARKET_TERMINAL_ROUTE = "market_terminal"
    const val NEWS_TERMINAL_ROUTE = "news_terminal"
    const val SETTINGS_ROUTE = "studio_settings"
    const val ARG_INITIAL_TAB = "initialTab"
    const val PORTFOLIO_TOOL_ROUTE_WITH_ARG =
        "$PORTFOLIO_TOOL_BASE_ROUTE?$ARG_INITIAL_TAB={$ARG_INITIAL_TAB}"

    fun portfolioToolRoute(initialTab: Int = 0): String =
        "$PORTFOLIO_TOOL_BASE_ROUTE?$ARG_INITIAL_TAB=$initialTab"
}

/**
 * Root Navigation Host orchestrating transitions between the Studio Launcher (VaultHubScreen)
 * and individual financial tools (Tool #1: PortfolioTrackerWorkspace).
 */
@Composable
fun FinBabaNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    portfolioViewModel: PortfolioViewModel = viewModel(),
    themeManager: VaultThemeManager = VaultThemeManager.getInstance(LocalContext.current)
) {
    NavHost(
        navController = navController,
        startDestination = FinBabaDestinations.VAULT_HUB_ROUTE,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(280)
            ) + fadeIn(animationSpec = tween(280))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(280)
            ) + fadeOut(animationSpec = tween(280))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(280)
            ) + fadeIn(animationSpec = tween(280))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(280)
            ) + fadeOut(animationSpec = tween(280))
        }
    ) {
        // Root Destination: FinBaba Financial Studio & Tool Vault Launcher
        composable(route = FinBabaDestinations.VAULT_HUB_ROUTE) {
            VaultHubScreen(
                portfolioViewModel = portfolioViewModel,
                themeManager = themeManager,
                onOpenPortfolioTracker = { initialTabIndex ->
                    navController.navigate(
                        FinBabaDestinations.portfolioToolRoute(initialTab = initialTabIndex)
                    ) {
                        launchSingleTop = true
                    }
                },
                onOpenMarketTerminal = {
                    navController.navigate(FinBabaDestinations.MARKET_TERMINAL_ROUTE) {
                        launchSingleTop = true
                    }
                },
                onOpenNewsTerminal = {
                    navController.navigate(FinBabaDestinations.NEWS_TERMINAL_ROUTE) {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = {
                    navController.navigate(FinBabaDestinations.SETTINGS_ROUTE) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Tool #1 Destination: Mutual Fund Portfolio & Look-Through Tracker Workspace
        composable(
            route = FinBabaDestinations.PORTFOLIO_TOOL_ROUTE_WITH_ARG,
            arguments = listOf(
                navArgument(FinBabaDestinations.ARG_INITIAL_TAB) {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getInt(FinBabaDestinations.ARG_INITIAL_TAB) ?: 0
            PortfolioTrackerWorkspace(
                portfolioViewModel = portfolioViewModel,
                themeManager = themeManager,
                initialTabIndex = initialTab,
                onNavigateBackToHub = {
                    if (!navController.popBackStack()) {
                        navController.navigate(FinBabaDestinations.VAULT_HUB_ROUTE) {
                            popUpTo(FinBabaDestinations.VAULT_HUB_ROUTE) { inclusive = true }
                        }
                    }
                },
                onOpenSettings = {
                    navController.navigate(FinBabaDestinations.SETTINGS_ROUTE) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Dedicated Full-Screen Destination: Studio Settings & Appearance Suite
        composable(route = FinBabaDestinations.SETTINGS_ROUTE) {
            StudioSettingsScreen(
                portfolioViewModel = portfolioViewModel,
                themeManager = themeManager,
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(FinBabaDestinations.VAULT_HUB_ROUTE) {
                            popUpTo(FinBabaDestinations.VAULT_HUB_ROUTE) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Tool #2 Destination: Global Market Terminal & Broker Studio
        composable(route = FinBabaDestinations.MARKET_TERMINAL_ROUTE) {
            MarketTerminalScreen(
                themeManager = themeManager,
                onNavigateBackToHub = {
                    if (!navController.popBackStack()) {
                        navController.navigate(FinBabaDestinations.VAULT_HUB_ROUTE) {
                            popUpTo(FinBabaDestinations.VAULT_HUB_ROUTE) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Tool #4 Destination: Institutional Market Intelligence Wire & Sentiment Radar
        composable(route = FinBabaDestinations.NEWS_TERMINAL_ROUTE) {
            NewsTerminalScreen(
                themeManager = themeManager,
                onNavigateBackToHub = {
                    if (!navController.popBackStack()) {
                        navController.navigate(FinBabaDestinations.VAULT_HUB_ROUTE) {
                            popUpTo(FinBabaDestinations.VAULT_HUB_ROUTE) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}