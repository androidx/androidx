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
import androidx.compose.frames.modelListOf
import androidx.compose.mutableStateOf
import androidx.compose.onCommit
import androidx.compose.remember
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

/**
 * Main [Activity] containing all Compose related demos.
 */
class DemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val composition = setContent {
            val navigator = remember {
                Navigator(
                    initialDemo = AllDemosCategory,
                    backDispatcher = onBackPressedDispatcher
                ) { activityDemo ->
                    startActivity(Intent(this, activityDemo.activityClass.java))
                }
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
                val filteringMode = remember { FilterMode(onBackPressedDispatcher) }
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
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                composition.dispose()
            }
        })
    }
}

@Composable
private fun DemoTheme(
    demoColors: DemoColorPalette,
    window: Window,
    children: @Composable() () -> Unit
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

private class Navigator(
    private val initialDemo: DemoCategory,
    private val backDispatcher: OnBackPressedDispatcher,
    val launchActivityDemo: (ActivityDemo<*>) -> Unit
) {
    private val backStack: MutableList<Demo> = modelListOf()

    private val onBackPressed = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            popBackStack()
        }
    }.apply {
        backDispatcher.addCallback(this)
    }

    private var _currentDemo by mutableStateOf<Demo>(initialDemo)
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
            currentDemo = initialDemo
        }
    }

    private fun popBackStack() {
        currentDemo = backStack.removeAt(backStack.lastIndex)
    }
}

private class FilterMode(backDispatcher: OnBackPressedDispatcher) {

    private var _isFiltering by mutableStateOf(false)

    private val onBackPressed = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            isFiltering = false
        }
    }.apply {
        backDispatcher.addCallback(this)
    }

    var isFiltering
        get() = _isFiltering
        set(value) {
            _isFiltering = value
            onBackPressed.isEnabled = value
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
        val function = if (isLightTheme) ::lightColorPalette else ::darkColorPalette
        val parametersToSet = function.parameters.mapNotNull { parameter ->
            val savedValue = sharedPreferences.getString(parameter.name + isLightTheme, "")
            if (savedValue.isNullOrBlank()) {
                null
            } else {
                val parsedColor = Color(savedValue.toLong(16))
                parameter to parsedColor
            }
        }.toMap()
        return function.callBy(parametersToSet)
    }

    lightColors = getColorsFromSharedPreferences(true)
    darkColors = getColorsFromSharedPreferences(false)
}
