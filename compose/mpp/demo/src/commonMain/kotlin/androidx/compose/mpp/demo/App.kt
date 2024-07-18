package androidx.compose.mpp.demo

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController

class App(
    private val initialScreenName: String? = null,
    private val extraScreens: List<Screen> = listOf()
) {
    @Composable
    fun Content() {
        val navController = rememberNavController()
        val animationSpec = tween<IntOffset>(500)
        NavHost(
            navController = navController,
            startDestination = initialScreenName ?: MainScreen.title,

            // Custom animations
            enterTransition = { slideIntoContainer(SlideDirection.Left, animationSpec) },
            exitTransition = { slideOutOfContainer(SlideDirection.Left, animationSpec) },
            popEnterTransition = { slideIntoContainer(SlideDirection.Right, animationSpec) },
            popExitTransition = { slideOutOfContainer(SlideDirection.Right, animationSpec) }
        ) {
            buildScreen(MainScreen.mergedWith(extraScreens), navController)
        }
    }

    private fun NavGraphBuilder.buildScreen(screen: Screen, navController: NavController) {
        if (screen is Screen.Selection) {
            for (i in screen.screens) {
                buildScreen(i, navController)
            }
        }
        if (screen is Screen.Dialog) {
            dialog(screen.title) { ScreenContent(screen, navController) }
        } else {
            composable(screen.title) { ScreenContent(screen, navController) }
        }
    }

    @Composable
    private fun ScreenContent(screen: Screen, navController: NavController) {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val currentBackStack = remember(screen) { navController.currentBackStack.value }
        screen.Content(
            title = currentBackStack.drop(1)
                .joinToString("/") { it.destination.route ?: it.destination.displayName },
            navigate = { navController.navigate(it) },
            back = back@{
                // Filter multi-click by current lifecycle state: it's not [RESUMED] in case if
                // a navigation transaction is in progress or the window is not focused.
                if (lifecycle.currentState < Lifecycle.State.RESUMED) {
                    return@back
                }
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                }
            }
        )
    }
}
