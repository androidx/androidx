/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.glance.appwidget.testing.unit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.ImageProvider
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.testing.test.R
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import androidx.glance.text.Text
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import org.junit.Assert.assertThrows
import org.junit.Test

// In this test we aren't specifically testing anything bound to SDK, so we can run it without
// android unit test runners such as Robolectric.
class GlanceAppWidgetUnitTestEnvironmentTest {
    @Test
    fun runTest_localSizeRead() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(width = 120.dp, height = 200.dp))

        provideComposable {
            ComposableReadingLocalSize()
        }

        onNode(hasText("120.0 dp x 200.0 dp")).assertExists()
    }

    @Composable
    fun ComposableReadingLocalSize() {
        val size = LocalSize.current
        Column {
            Text(text = "${size.width.value} dp x ${size.height.value} dp")
            Spacer()
            Image(
                provider = ImageProvider(R.drawable.glance_test_android),
                contentDescription = "test-image",
            )
        }
    }

    @Test
    fun runTest_currentStateRead() = runGlanceAppWidgetUnitTest {
        setState(preferencesOf(toggleKey to true))

        provideComposable {
            ComposableReadingState()
        }

        onNode(hasText("isToggled")).assertExists()
    }

    @Composable
    fun ComposableReadingState() {
        Column {
            Text(text = "A text")
            Spacer()
            Text(text = getTitle(currentState<Preferences>()[toggleKey] == true))
            Spacer()
            Image(
                provider = ImageProvider(R.drawable.glance_test_android),
                contentDescription = "test-image",
                modifier = GlanceModifier.semantics { testTag = "img" }
            )
        }
    }

    @Test
    fun runTest_emptyComposable_throwsError() = runGlanceAppWidgetUnitTest {
        provideComposable {}

        val exception = assertThrows(IllegalStateException::class.java) {
            onNode(hasText("abc")).assertExists()
        }

        assertThat(exception).hasMessageThat().isEqualTo(
            "No nodes found to perform the assertions. Provide the composable to be " +
                "tested using `provideComposable` function before performing assertions."
        )
    }

    @Test
    fun runTest_composableNotProvided_throwsError() = runGlanceAppWidgetUnitTest {
        val exception = assertThrows(IllegalStateException::class.java) {
            onNode(hasText("abc")).assertExists()
        }

        assertThat(exception).hasMessageThat().isEqualTo(
            "No nodes found to perform the assertions. Provide the composable to be " +
                "tested using `provideComposable` function before performing assertions."
        )
    }

    @Test
    fun runTest_onNodeCalledMultipleTimes() = runGlanceAppWidgetUnitTest {
        provideComposable {
            Text(text = "abc")
            Spacer()
            Text(text = "xyz")
        }

        onNode(hasText("abc")).assertExists()
        // test context reset and new filter matched onNode
        onNode(hasText("xyz")).assertExists()
        onNode(hasText("def")).assertDoesNotExist()
    }

    @Test
    fun runTest_effect() = runGlanceAppWidgetUnitTest {
        provideComposable {
            var text by remember { mutableStateOf("initial") }

            Text(text = text, modifier = GlanceModifier.semantics { testTag = "mutable-test" })
            Spacer()
            Text(text = "xyz")

            LaunchedEffect(Unit) {
                text = "changed"
            }
        }

        onNode(hasTestTag("mutable-test")).assert(hasText("changed"))
    }

    @Test
    fun runTest_effectWithDelay() = runGlanceAppWidgetUnitTest {
        provideComposable {
            var text by remember { mutableStateOf("initial") }

            Text(text = text, modifier = GlanceModifier.semantics { testTag = "mutable-test" })
            Spacer()
            Text(text = "xyz")

            LaunchedEffect(Unit) {
                delay(100L)
                text = "changed"
            }
        }

        awaitIdle() // Since the launched effect has a delay.
        onNode(hasTestTag("mutable-test")).assert(hasText("changed"))
    }

    @Test
    fun runTest_effectWithDelayWithoutAdvancing() = runGlanceAppWidgetUnitTest {
        provideComposable {
            var text by remember { mutableStateOf("initial") }

            Text(text = text, modifier = GlanceModifier.semantics { testTag = "mutable-test" })
            Spacer()
            Text(text = "xyz")

            LaunchedEffect(Unit) {
                delay(100L)
                text = "changed"
            }
        }

        onNode(hasTestTag("mutable-test")).assert(hasText("initial"))
    }

    @Test
    fun runTest_onMultipleNodesMatchedAcrossHierarchy() = runGlanceAppWidgetUnitTest {
        provideComposable {
            Column {
                Row {
                    Text("text-row")
                }
                Spacer()
                Text("text-in-column")
            }
        }

        onAllNodes(hasText(text = "text-")).assertCountEquals(2)
    }

    @Test
    fun runTest_lazyColumnChildren() = runGlanceAppWidgetUnitTest {
        provideComposable {
            LazyColumn(modifier = GlanceModifier.semantics { testTag = "test-list" }) {
                item { Text("text-1") }
                item { Text("text-2") }
            }
        }

        onNode(hasTestTag("test-list"))
            .onChildren()
            .assertCountEquals(2)
            .filter(hasText("text-1"))
            .assertCountEquals(1)
    }

    @Test
    fun runTest_lazyGridChildren() = runGlanceAppWidgetUnitTest {
        provideComposable {
            LazyVerticalGrid(
                modifier = GlanceModifier.semantics { testTag = "test-list" },
                gridCells = GridCells.Fixed(2)
            ) {
                item { Text("text-1") }
                item { Text("text-2") }
                item { Text("text-3") }
                item { Text("text-4") }
            }
        }

        onNode(hasTestTag("test-list"))
            .onChildren()
            .assertCountEquals(4)
            .filter(hasText("text-1"))
            .assertCountEquals(1)
    }
}

private val toggleKey = booleanPreferencesKey("title_toggled_key")
private fun getTitle(toggled: Boolean) = if (toggled) "isToggled" else "notToggled"
