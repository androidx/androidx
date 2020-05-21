/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.demos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.compose.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager
import androidx.ui.core.setContent
import androidx.ui.demos.common.ActivityDemo
import androidx.ui.demos.common.Demo
import androidx.ui.demos.common.DemoCategory
import androidx.ui.graphics.Color
import androidx.ui.graphics.toArgb
import androidx.ui.material.ColorPalette
import androidx.ui.material.MaterialTheme
import androidx.ui.material.darkColorPalette
import androidx.ui.material.lightColorPalette
import androidx.ui.savedinstancestate.Saver
import androidx.ui.savedinstancestate.listSaver
import androidx.ui.savedinstancestate.rememberSavedInstanceState

/**
 * Main [Activity] containing all Compose related demos.
 */
class DemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val activityStarter = fun(demo: ActivityDemo<*>) {
                startActivity(Intent(this, demo.activityClass.java))
            }
            val navigator = rememberSavedInstanceState(
                saver = Navigator.Saver(AllDemosCategory, onBackPressedDispatcher, activityStarter)
            ) {
                Navigator(AllDemosCategory, onBackPressedDispatcher, activityStarter)
            }
            val demoColors = remember {
                DemoColorPalette().also {
                    lifecycle.addObserver(LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            it.loadColorsFromSharedPreferences(this)
                        }
                    })
                }
            }
            DemoTheme(demoColors, window) {
                val filteringMode = rememberSavedInstanceState(
                    saver = FilterMode.Saver(onBackPressedDispatcher)
                ) {
                    FilterMode(onBackPressedDispatcher)
                }
                val onStartFiltering = { filteringMode.isFiltering = true }
                val onEndFiltering = { filteringMode.isFiltering = false }
                DemoApp(
                    currentDemo = navigator.currentDemo,
                    backStackTitle = navigator.backStackTitle,
                    isFiltering = filteringMode.isFiltering,
                    onStartFiltering = onStartFiltering,
                    onEndFiltering = onEndFiltering,
                    onNavigateToDemo = { demo ->
                        if (filteringMode.isFiltering) {
                            onEndFiltering()
                            navigator.popAll()
                        }
                        navigator.navigateTo(demo)
                    },
                    canNavigateUp = !navigator.isRoot,
                    onNavigateUp = {
                        onBackPressed()
                    },
                    launchSettings = {
                        startActivity(Intent(this, DemoSettingsActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
private fun DemoTheme(
    demoColors: DemoColorPalette,
    window: Window,
    children: @Composable () -> Unit
) {
    MaterialTheme(demoColors.colors) {
        val statusBarColor = with(MaterialTheme.colors) {
            if (isLight) darkenedPrimary else surface.toArgb()
        }
        onCommit(statusBarColor) {
            window.statusBarColor = statusBarColor
        }
        children()
    }
}

private val ColorPalette.darkenedPrimary: Int
    get() = with(primary) {
        copy(
            red = red * 0.75f,
            green = green * 0.75f,
            blue = blue * 0.75f
        )
    }.toArgb()

private class Navigator private constructor(
    private val backDispatcher: OnBackPressedDispatcher,
    private val launchActivityDemo: (ActivityDemo<*>) -> Unit,
    private val rootDemo: Demo,
    initialDemo: Demo,
    private val backStack: MutableList<Demo>
) {
    constructor(
        rootDemo: Demo,
        backDispatcher: OnBackPressedDispatcher,
        launchActivityDemo: (ActivityDemo<*>) -> Unit
    ) : this(backDispatcher, launchActivityDemo, rootDemo, rootDemo, mutableListOf<Demo>())

    private val onBackPressed = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            popBackStack()
        }
    }.apply {
        isEnabled = !isRoot
        backDispatcher.addCallback(this)
    }

    private var _currentDemo by mutableStateOf(initialDemo)
    var currentDemo: Demo
        get() = _currentDemo
        private set(value) {
            _currentDemo = value
            onBackPressed.isEnabled = !isRoot
        }

    val isRoot: Boolean get() = backStack.isEmpty()

    val backStackTitle: String
        get() =
            (backStack.drop(1) + currentDemo).joinToString(separator = " > ") { it.title }

    fun navigateTo(demo: Demo) {
        if (demo is ActivityDemo<*>) {
            launchActivityDemo(demo)
        } else {
            backStack.add(currentDemo)
            currentDemo = demo
        }
    }

    fun popAll() {
        if (!isRoot) {
            backStack.clear()
            currentDemo = rootDemo
        }
    }

    private fun popBackStack() {
        currentDemo = backStack.removeAt(backStack.lastIndex)
    }

    companion object {
        fun Saver(
            rootDemo: DemoCategory,
            backDispatcher: OnBackPressedDispatcher,
            launchActivityDemo: (ActivityDemo<*>) -> Unit
        ): Saver<Navigator, *> = listSaver<Navigator, String>(
            save = { navigator ->
                (navigator.backStack + navigator.currentDemo).map { it.title }
            },
            restore = { restored ->
                require(restored.isNotEmpty())
                val backStack = restored.mapTo(mutableListOf()) {
                    requireNotNull(findDemo(rootDemo, it))
                }
                val initial = backStack.removeAt(backStack.lastIndex)
                Navigator(backDispatcher, launchActivityDemo, rootDemo, initial, backStack)
            }
        )

        private fun findDemo(demo: Demo, title: String): Demo? {
            if (demo.title == title) {
                return demo
            }
            if (demo is DemoCategory) {
                demo.demos.forEach { child ->
                    findDemo(child, title)
                        ?.let { return it }
                }
            }
            return null
        }
    }
}

private class FilterMode(backDispatcher: OnBackPressedDispatcher, initialValue: Boolean = false) {

    private var _isFiltering by mutableStateOf(initialValue)

    private val onBackPressed = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            isFiltering = false
        }
    }.apply {
        isEnabled = initialValue
        backDispatcher.addCallback(this)
    }

    var isFiltering
        get() = _isFiltering
        set(value) {
            _isFiltering = value
            onBackPressed.isEnabled = value
        }

    companion object {
        fun Saver(backDispatcher: OnBackPressedDispatcher) = Saver<FilterMode, Boolean>(
            save = { it.isFiltering },
            restore = { FilterMode(backDispatcher, it) }
        )
    }
}

/**
 * Returns a [DemoColorPalette] from the values saved to [SharedPreferences]. If a given color is
 * not present in the [SharedPreferences], its default value as defined in [ColorPalette]
 * will be returned.
 */
fun DemoColorPalette.loadColorsFromSharedPreferences(context: Context) {
    val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    fun getColorsFromSharedPreferences(isLightTheme: Boolean): ColorPalette {
        val function = if (isLightTheme) ::reflectLightColorPalette else ::reflectDarkColorPalette
        val parametersToSet = function.parameters.mapNotNull { parameter ->
            val savedValue = sharedPreferences.getString(parameter.name + isLightTheme, "")
            if (savedValue.isNullOrBlank()) {
                null
            } else {
                // TODO: should be a Color(savedValue.toLong(16)) when b/154329050 is fixed
                val parsedColor = savedValue.toLong(16)
                parameter to parsedColor
            }
        }.toMap()
        return function.callBy(parametersToSet)
    }

    lightColors = getColorsFromSharedPreferences(true)
    darkColors = getColorsFromSharedPreferences(false)
}

/**
 * TODO: remove after b/154329050 is fixed
 * Inline classes don't play well with reflection, so we want boxed classes for our
 * call to [lightColorPalette].
 */
internal fun reflectLightColorPalette(
    primary: Long = 0xFF6200EE,
    primaryVariant: Long = 0xFF3700B3,
    secondary: Long = 0xFF03DAC6,
    secondaryVariant: Long = 0xFF018786,
    background: Long = 0xFFFFFFFF,
    surface: Long = 0xFFFFFFFF,
    error: Long = 0xFFB00020,
    onPrimary: Long = 0xFFFFFFFF,
    onSecondary: Long = 0xFF000000,
    onBackground: Long = 0xFF000000,
    onSurface: Long = 0xFF000000,
    onError: Long = 0xFFFFFFFF
) = lightColorPalette(
    primary = Color(primary),
    primaryVariant = Color(primaryVariant),
    secondary = Color(secondary),
    secondaryVariant = Color(secondaryVariant),
    background = Color(background),
    surface = Color(surface),
    error = Color(error),
    onPrimary = Color(onPrimary),
    onSecondary = Color(onSecondary),
    onBackground = Color(onBackground),
    onSurface = Color(onSurface),
    onError = Color(onError)
)

/**
 * TODO: remove after b/154329050 is fixed
 * Inline classes don't play well with reflection, so we want boxed classes for our
 * call to [darkColorPalette].
 */
internal fun reflectDarkColorPalette(
    primary: Long = 0xFFBB86FC,
    primaryVariant: Long = 0xFF3700B3,
    secondary: Long = 0xFF03DAC6,
    background: Long = 0xFF121212,
    surface: Long = 0xFF121212,
    error: Long = 0xFFCF6679,
    onPrimary: Long = 0xFF000000,
    onSecondary: Long = 0xFF000000,
    onBackground: Long = 0xFFFFFFFF,
    onSurface: Long = 0xFFFFFFFF,
    onError: Long = 0xFF000000
) = darkColorPalette(
    primary = Color(primary),
    primaryVariant = Color(primaryVariant),
    secondary = Color(secondary),
    background = Color(background),
    surface = Color(surface),
    error = Color(error),
    onPrimary = Color(onPrimary),
    onSecondary = Color(onSecondary),
    onBackground = Color(onBackground),
    onSurface = Color(onSurface),
    onError = Color(onError)
)