package androidx.compose.mpp.demo

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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
    initialScreenName: String? = null,
    extraScreens: List<Screen> = listOf()
) {
    private val navigationStack: SnapshotStateList<Screen> =
        mutableStateListOf(MainScreen.mergedWith(extraScreens))

    init {
        if (initialScreenName != null) {
            var currentScreen = navigationStack.first()
            initialScreenName.split("/").forEach { target ->
                val selectionScreen = currentScreen as Screen.Selection
                currentScreen = selectionScreen.screens.find { it.title == target }!!
                navigationStack.add(currentScreen)
            }
        }
    }

    @Composable
    fun Content() {
        when (val screen = navigationStack.last()) {
            is Screen.Example -> {
                ExampleScaffold(backgroundColor = screen.backgroundColor) {
                    screen.content()
                }
            }

            is Screen.Selection -> {
                SelectionScaffold {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(screen.screens) {
                            Text(it.title, Modifier.clickable {
                                navigationStack.add(it)
                            }.padding(16.dp).fillMaxWidth())
                        }
                    }
                }
            }

            is Screen.FullscreenExample -> {
                screen.content {
                    if (navigationStack.size > 1) {
                        navigationStack.removeLast()
                    }
                }
            }
        }
    }

    @Composable
    private fun ExampleScaffold(
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
                        val title = navigationStack.drop(1)
                            .joinToString("/") { it.title }
                        Text(title)
                    },
                    navigationIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.backButton()
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
                                if (navigationStack.size > 1) {
                                    Icon(
                                        Icons.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        modifier = Modifier.backButton()
                                    )
                                    Spacer(Modifier.width(16.dp))
                                }
                                ProvideTextStyle(value = MaterialTheme.typography.h6) {
                                    Text(navigationStack.first().title)
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

    private fun Modifier.backButton() = clickable {
        if (navigationStack.size > 1) {
            navigationStack.removeLast()
        }
    }
}
