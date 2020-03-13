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
import androidx.compose.frames.modelListOf
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
import androidx.ui.graphics.vector.VectorAsset
import androidx.ui.layout.Column
import androidx.ui.material.ColorPalette
import androidx.ui.material.IconButton
import androidx.ui.material.ListItem
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Scaffold
import androidx.ui.material.TopAppBar
import androidx.ui.material.darkColorPalette
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.lazyMaterialIcon
import androidx.ui.material.icons.materialPath
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
    private val backStack: MutableList<Demo> = modelListOf()

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

/**
 * Icons below are copied from [Icons.Filled] in ui-material-icons-extended to avoid recompiling the
 * module in demos. In the future when we release a stable artifact we could directly depend on
 * that, instead of a project dependency which causes recompilation.
 *
 * If the generated icons change, just build ui-material-icons-extended and copy the generated
 * file, which should appear in Studio sources by searching for the name of that icon.
 */

private val Icons.Filled.ArrowBack: VectorAsset by lazyMaterialIcon {
    materialPath {
        moveTo(20.0f, 11.0f)
        horizontalLineTo(7.83f)
        lineToRelative(5.59f, -5.59f)
        lineTo(12.0f, 4.0f)
        lineToRelative(-8.0f, 8.0f)
        lineToRelative(8.0f, 8.0f)
        lineToRelative(1.41f, -1.41f)
        lineTo(7.83f, 13.0f)
        horizontalLineTo(20.0f)
        verticalLineToRelative(-2.0f)
        close()
    }
}

private val Icons.Filled.Settings: VectorAsset by lazyMaterialIcon {
    materialPath {
        moveTo(19.14f, 12.94f)
        curveToRelative(0.04f, -0.3f, 0.06f, -0.61f, 0.06f, -0.94f)
        curveToRelative(0.0f, -0.32f, -0.02f, -0.64f, -0.07f, -0.94f)
        lineToRelative(2.03f, -1.58f)
        curveToRelative(0.18f, -0.14f, 0.23f, -0.41f, 0.12f, -0.61f)
        lineToRelative(-1.92f, -3.32f)
        curveToRelative(-0.12f, -0.22f, -0.37f, -0.29f, -0.59f, -0.22f)
        lineToRelative(-2.39f, 0.96f)
        curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
        lineTo(14.4f, 2.81f)
        curveToRelative(-0.04f, -0.24f, -0.24f, -0.41f, -0.48f, -0.41f)
        horizontalLineToRelative(-3.84f)
        curveToRelative(-0.24f, 0.0f, -0.43f, 0.17f, -0.47f, 0.41f)
        lineTo(9.25f, 5.35f)
        curveTo(8.66f, 5.59f, 8.12f, 5.92f, 7.63f, 6.29f)
        lineTo(5.24f, 5.33f)
        curveToRelative(-0.22f, -0.08f, -0.47f, 0.0f, -0.59f, 0.22f)
        lineTo(2.74f, 8.87f)
        curveTo(2.62f, 9.08f, 2.66f, 9.34f, 2.86f, 9.48f)
        lineToRelative(2.03f, 1.58f)
        curveTo(4.84f, 11.36f, 4.8f, 11.69f, 4.8f, 12.0f)
        reflectiveCurveToRelative(0.02f, 0.64f, 0.07f, 0.94f)
        lineToRelative(-2.03f, 1.58f)
        curveToRelative(-0.18f, 0.14f, -0.23f, 0.41f, -0.12f, 0.61f)
        lineToRelative(1.92f, 3.32f)
        curveToRelative(0.12f, 0.22f, 0.37f, 0.29f, 0.59f, 0.22f)
        lineToRelative(2.39f, -0.96f)
        curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
        lineToRelative(0.36f, 2.54f)
        curveToRelative(0.05f, 0.24f, 0.24f, 0.41f, 0.48f, 0.41f)
        horizontalLineToRelative(3.84f)
        curveToRelative(0.24f, 0.0f, 0.44f, -0.17f, 0.47f, -0.41f)
        lineToRelative(0.36f, -2.54f)
        curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
        lineToRelative(2.39f, 0.96f)
        curveToRelative(0.22f, 0.08f, 0.47f, 0.0f, 0.59f, -0.22f)
        lineToRelative(1.92f, -3.32f)
        curveToRelative(0.12f, -0.22f, 0.07f, -0.47f, -0.12f, -0.61f)
        lineTo(19.14f, 12.94f)
        close()
        moveTo(12.0f, 15.6f)
        curveToRelative(-1.98f, 0.0f, -3.6f, -1.62f, -3.6f, -3.6f)
        reflectiveCurveToRelative(1.62f, -3.6f, 3.6f, -3.6f)
        reflectiveCurveToRelative(3.6f, 1.62f, 3.6f, 3.6f)
        reflectiveCurveTo(13.98f, 15.6f, 12.0f, 15.6f)
        close()
    }
}
