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
import androidx.compose.Composable
import androidx.compose.Composition
import androidx.compose.mutableStateOf
import androidx.compose.onCommit
import androidx.preference.PreferenceManager
import androidx.ui.animation.Crossfade
import androidx.ui.core.Text
import androidx.ui.core.setContent
import androidx.ui.demos.common.ActivityDemo
import androidx.ui.demos.common.ComposableDemo
import androidx.ui.demos.common.Demo
import androidx.ui.demos.common.DemoCategory
import androidx.ui.foundation.Icon
import androidx.ui.foundation.VerticalScroller
import androidx.ui.graphics.Color
import androidx.ui.graphics.toArgb
import androidx.ui.layout.Column
import androidx.ui.material.ColorPalette
import androidx.ui.material.IconButton
import androidx.ui.material.ListItem
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Scaffold
import androidx.ui.material.TopAppBar
import androidx.ui.material.darkColorPalette
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.ArrowBack
import androidx.ui.material.icons.filled.Settings
import androidx.ui.material.lightColorPalette

/**
 * Main [Activity] containing all Compose related demos.
 */
class DemoActivity : Activity() {

    private lateinit var composition: Composition

    private val navigator = Navigator(initialDemo = AllDemosCategory) { activityDemo ->
        startActivity(Intent(this, activityDemo.activityClass.java))
    }

    private var demoColors = DemoColorPalette()

    override fun onResume() {
        super.onResume()
        demoColors.loadColorsFromSharedPreferences(this)
    }

    override fun onBackPressed() {
        if (!navigator.popBackStack()) {
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        composition = setContent {
            DemoTheme {
                DemoScaffold {
                    Crossfade(navigator.currentDemo) { demo ->
                        DisplayDemo(demo)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        composition.dispose()
    }

    @Composable
    private fun DemoTheme(children: @Composable() () -> Unit) {
        MaterialTheme(demoColors.colors) {
            val statusBarColor = with(MaterialTheme.colors()) {
                if (isLight) darkenedPrimary else surface.toArgb()
            }
            onCommit(statusBarColor) {
                window.statusBarColor = statusBarColor
            }
            children()
        }
    }

    @Composable
    private fun DemoScaffold(children: @Composable() () -> Unit) {
        val navigationIcon = (@Composable {
            IconButton(onClick = { onBackPressed() }) {
                Icon(Icons.Filled.ArrowBack)
            }
        }).takeUnless { navigator.isRoot }

        val topAppBar = @Composable {
            TopAppBar(
                title = { Text(navigator.backStackTitle) },
                navigationIcon = navigationIcon,
                actions = { SettingsIcon() }
            )
        }

        Scaffold(topAppBar = topAppBar) {
            children()
        }
    }

    @Composable
    private fun DisplayDemo(demo: Demo) {
        when (demo) {
            is ActivityDemo<*> -> {
                /* should never get here as activity demos are not added to the backstack*/
            }
            is ComposableDemo -> demo.content()
            is DemoCategory -> DisplayDemoCategory(demo)
        }
    }

    @Composable
    private fun DisplayDemoCategory(category: DemoCategory) {
        VerticalScroller {
            Column {
                category.demos.forEach { demo ->
                    ListItem(
                        text = { Text(text = demo.title) },
                        onClick = {
                            navigator.navigateTo(demo)
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingsIcon() {
        IconButton(onClick = {
            startActivity(Intent(this, DemoSettingsActivity::class.java))
        }) {
            Icon(Icons.Filled.Settings)
        }
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
    initialDemo: DemoCategory,
    val launchActivityDemo: (ActivityDemo<*>) -> Unit
) {
    private val backStack: MutableList<Demo> = mutableListOf()

    var currentDemo by mutableStateOf<Demo>(initialDemo)
        private set

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

    fun popBackStack(): Boolean {
        if (isRoot) return false
        currentDemo = backStack.removeAt(backStack.lastIndex)
        return true
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
