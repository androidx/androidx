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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier

/**
 * One tab in the sample app.
 *
 * @param title the label of the tab.
 * @param content the demo that the tab shows.
 */
private class DemoTab(val title: String, val content: @Composable () -> Unit)

private val TABS =
    listOf(
        DemoTab("Vertical Text") { LongVerticalTextDemo() },
        DemoTab("Vertical Multi-style Text") { MultiStyleVerticalTextDemo() },
        DemoTab("Horizontal Text") { LongHorizontalTextDemo() },
        DemoTab("Horizontal Multi-style Text") { MultiStyleHorizontalTextDemo() },
        DemoTab("Horizontal CJK Font") { CjkFontHorizontalTextDemo() },
        DemoTab("Style Colors") { StyleColorsDemo() },
        DemoTab("Subtitle Cue") { SubtitleCueDemo() },
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
    var selectedTabIndex by remember { mutableIntStateOf(initialTabIndex.coerceIn(TABS.indices)) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TABS.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(tab.title) },
                    )
                }
            }
            key(selectedTabIndex) { TABS[selectedTabIndex].content() }
        }
    }
}
