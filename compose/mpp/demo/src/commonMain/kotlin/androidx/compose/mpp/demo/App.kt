package androidx.compose.mpp.demo

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.mpp.demo.bug.BugReproducers
import androidx.compose.mpp.demo.components.Components
import androidx.compose.mpp.demo.textfield.android.AndroidTextFieldSamples
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

val MainScreen = Screen.Selection(
    "Demo",
    Components,
    BugReproducers,
    Screen.Example("Example1") { Example1() },
    Screen.Example("ImageViewer") { ImageViewer() },
    Screen.Example("TextDirection") { TextDirection() },
    Screen.Example("FontFamilies") { FontFamilies() },
    Screen.Example("LottieAnimation") { LottieAnimation() },
    Screen.FullscreenExample("ApplicationLayouts") { ApplicationLayouts(it) },
    Screen.Example("GraphicsLayerSettings") { GraphicsLayerSettings() },
    Screen.Example("Blending") { Blending() },
    Screen.Example("FontRasterization") { FontRasterization() },
    Screen.Example("InteropOrder") { InteropOrder() },
    AndroidTextFieldSamples,
)

sealed interface Screen {
    val title: String

    class Example(
        override val title: String,
        val backgroundColor: Color? = null,
        val content: @Composable () -> Unit
    ) : Screen

    class Selection(
        override val title: String,
        val screens: List<Screen>
    ) : Screen {
        constructor(title: String, vararg screens: Screen) : this(title, listOf(*screens))

        fun mergedWith(screens: List<Screen>): Selection {
            return Selection(title, screens + this.screens)
        }
    }

    class FullscreenExample(
        override val title: String,
        val content: @Composable (back: () -> Unit) -> Unit
    ) : Screen
}

class App(
    private val initialScreenName: String? = null,
    private val extraScreens: List<Screen> = listOf()
) {
    @Composable
    fun Content() {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = initialScreenName ?: MainScreen.title,

            // Custom animations
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(700)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(700)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(700)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(700)
                )
            }
        ) {
            buildScreen(MainScreen, navController)
            for (i in extraScreens) {
                buildScreen(i, navController)
            }
        }
    }

    private fun NavGraphBuilder.buildScreen(screen: Screen, navController: NavController) {
        if (screen is Screen.Selection) {
            for (i in screen.screens) {
                buildScreen(i, navController)
            }
        }
        composable(screen.title) { ScreenContent(screen, navController) }
    }

    @Composable
    private fun ScreenContent(screen: Screen, navController: NavController) {
        val currentBackStack = remember(screen) { navController.currentBackStack.value }
        val firstEntry = remember(screen) { navController.previousBackStackEntry == null }
        val title = currentBackStack.drop(1)
            .joinToString("/") { it.destination.route ?: it.destination.displayName }
        val back: (() -> Unit)? = if (!firstEntry) {
            { navController.popBackStack() }
        } else null
        when (screen) {
            is Screen.Example -> {
                ExampleScaffold(
                    title = title,
                    back = back!!,
                    backgroundColor = screen.backgroundColor,
                    content = screen.content
                )
            }

            is Screen.Selection -> {
                SelectionScaffold(
                    title = title,
                    back = back,
                ) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(screen.screens) {
                            Text(it.title, Modifier.clickable {
                                navController.navigate(it.title)
                            }.padding(16.dp).fillMaxWidth())
                        }
                    }
                }
            }

            is Screen.FullscreenExample -> {
                screen.content {
                    navController.popBackStack()
                }
            }
        }
    }

    @Composable
    private fun ExampleScaffold(
        title: String,
        back: () -> Unit,
        backgroundColor: Color?,
        content: @Composable () -> Unit
    ) {
        Scaffold(
            /*
            Without using TopAppBar, this is recommended approach to apply multiplatform window insets
            to Material2 Scaffold (otherwise there will be empty space above top app bar - as is here)
            */
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
            topBar = {
                TopAppBar(
                    title = {
                        Text(title)
                    },
                    navigationIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.clickable { back.invoke() }
                        )
                    }
                )
            },
            backgroundColor = backgroundColor ?: MaterialTheme.colors.background
        ) { innerPadding ->
            Box(
                Modifier.fillMaxSize().padding(innerPadding)
            ) {
                content()
            }
        }
    }

    @Composable
    private fun SelectionScaffold(
        title: String,
        back: (() -> Unit)? = null,
        content: @Composable () -> Unit
    ) {
        Scaffold(
            topBar = {
                /*
                This is recommended approach of applying multiplatform window insets to Material2 Scaffold with using top app bar.
                By that way, it is possible to fill area above top app bar with its background - as it works out of box in android development or with Material3 Scaffold
                */
                TopAppBar(
                    contentPadding = WindowInsets.systemBars
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                        .union(WindowInsets(left = 20.dp))
                        .asPaddingValues(),
                    content = {
                        CompositionLocalProvider(
                            LocalContentAlpha provides ContentAlpha.high
                        ) {
                            Row(
                                Modifier.fillMaxHeight().weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (back != null) {
                                    Icon(
                                        Icons.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        modifier = Modifier.clickable { back.invoke() }
                                    )
                                    Spacer(Modifier.width(16.dp))
                                }
                                ProvideTextStyle(value = MaterialTheme.typography.h6) {
                                    Text(title)
                                }
                            }
                        }
                    }
                )
            },
        ) { innerPadding ->
            /*
            In case of applying WindowInsets as content padding, it is strongly recommended to wrap
            content of scaffold into box with these modifiers to support proper layout when device rotated
            */
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                    .padding(innerPadding)
            ) {
                content()
            }
        }
    }
}
