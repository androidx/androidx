/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.text.vertical.testapp

import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import java.io.File

/**
 * One tab in the sample app.
 *
 * @param title the label of the tab.
 * @param styleColorsInitiallyEnabled whether the style colors are on when the app starts.
 * @param content the demo that the tab shows. It gets whether the style colors are on.
 */
private class DemoTab(
    val title: String,
    val styleColorsInitiallyEnabled: Boolean = false,
    val content: @Composable (styleColorsEnabled: Boolean) -> Unit,
)

class VerticalTextSampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialTabIndex = intent.getIntExtra("tab", 0)
        setContent { VerticalTextSampleApp(initialTabIndex = initialTabIndex) }
    }
}

/**
 * Displays the vertical text sample tabs inside a [Scaffold].
 *
 * @param initialTabIndex the 0-based tab index selected when the composable first enters
 *   composition
 */
@Composable
internal fun VerticalTextSampleApp(initialTabIndex: Int = 0) {
    val tabs = remember {
        // Load the system CJK font as the primary typeface. Its top and bottom bounds are
        // much larger than its ascent and descent, unlike the Latin default font when it
        // falls back to CJK per glyph. Fall back to sans-serif when the file is absent.
        val cjkFontFamily =
            File("/system/fonts/NotoSansCJK-Regular.ttc")
                .takeIf { it.exists() }
                ?.let { FontFamily(Typeface.createFromFile(it)) } ?: FontFamily.SansSerif
        arrayOf(
            DemoTab("Vertical Text") { enabled ->
                ZoomableVerticalText(styleColorsEnabled = enabled) { LongText(it) }
            },
            DemoTab("Vertical Multi-style Text") { enabled ->
                ZoomableVerticalText(styleColorsEnabled = enabled) { ComplexText(it) }
            },
            DemoTab("Horizontal Text") { enabled ->
                ZoomableVerticalText(isVertical = false, styleColorsEnabled = enabled) {
                    LongHorizontalText(it)
                }
            },
            DemoTab("Horizontal Multi-style Text") { enabled ->
                ZoomableVerticalText(isVertical = false, styleColorsEnabled = enabled) {
                    ComplexHorizontalText(it)
                }
            },
            DemoTab("Horizontal CJK Font") { enabled ->
                ZoomableVerticalText(
                    isVertical = false,
                    styleColorsEnabled = enabled,
                    fontFamily = cjkFontFamily,
                ) {
                    ComplexHorizontalText(it)
                }
            },
            DemoTab("Style Colors", styleColorsInitiallyEnabled = true) { enabled ->
                ZoomableVerticalText(styleColorsEnabled = enabled) { StyleColorsText(it) }
            },
            DemoTab("Subtitle Cue") { enabled -> SubtitleCueDemo(styleColorsEnabled = enabled) },
        )
    }
    var selectedTabIndex by remember { mutableIntStateOf(initialTabIndex.coerceIn(tabs.indices)) }
    val styleColorsEnabled = remember {
        tabs.map { it.styleColorsInitiallyEnabled }.toMutableStateList()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            val enabled = styleColorsEnabled[selectedTabIndex]
            ExtendedFloatingActionButton(
                text = { Text("Style Colors") },
                icon = {
                    Icon(
                        painter =
                            painterResource(
                                if (enabled) R.drawable.checkbox
                                else R.drawable.checkbox_outline_blank
                            ),
                        contentDescription = null,
                    )
                },
                onClick = { styleColorsEnabled[selectedTabIndex] = !enabled },
                modifier = Modifier.semantics { stateDescription = if (enabled) "On" else "Off" },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(tab.title) },
                    )
                }
            }
            key(selectedTabIndex) {
                tabs[selectedTabIndex].content(styleColorsEnabled[selectedTabIndex])
            }
        }
    }
}
