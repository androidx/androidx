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

package androidx.compose.foundation.text

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import java.util.concurrent.Executor
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicTextPrefetchTest {

    @get:Rule val rule = createComposeRule()

    @Before
    fun coreCountMustBeAtLeast() {
        assumeTrue(coreCountSatisfactory)
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.O_MR1)
    fun prefetchDoesNotHappen_whenAndroidVersionIsOlderThanP() {
        val executor = MockExecutor()
        var assertions = 0
        rule.setContent {
            ProvidePrefetchScheduler(executor) {
                Truth.assertThat(executor.commands.size).isEqualTo(0)
                BasicText("Hello World")
                Truth.assertThat(executor.commands.size).isEqualTo(0)
                assertions++
            }
        }

        rule.waitForIdle()
        Truth.assertThat(assertions).isEqualTo(1)
        Truth.assertThat(executor.commands.size).isEqualTo(0)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun basicText_simpleString_prefetchRequestComesDuringComposition() {
        val executor = MockExecutor()
        var assertions = 0
        rule.setContent {
            ProvidePrefetchScheduler(executor) {
                Truth.assertThat(executor.commands.size).isEqualTo(0)
                BasicText("Hello World")
                Truth.assertThat(executor.commands.size).isEqualTo(1)
                assertions++
            }
        }

        rule.waitForIdle()
        Truth.assertThat(assertions).isEqualTo(1)
        Truth.assertThat(executor.commands.size).isEqualTo(1)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun basicText_simpleString_prefetchHappens_whenTextChanges() {
        val executor = MockExecutor()
        var text by mutableStateOf("Hello World")
        rule.setContent { ProvidePrefetchScheduler(executor) { BasicText(text) } }

        rule.waitForIdle()

        text += "A"

        rule.waitForIdle()

        Truth.assertThat(executor.commands.size).isEqualTo(2)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun basicText_simpleString_prefetchHappens_whenStyleChanges() {
        val executor = MockExecutor()
        var style by mutableStateOf(TextStyle(fontSize = 10.sp))
        rule.setContent {
            ProvidePrefetchScheduler(executor) { BasicText("Hello World", style = style) }
        }

        rule.waitForIdle()

        style = style.copy(fontSize = 12.sp)

        rule.waitForIdle()

        Truth.assertThat(executor.commands.size).isEqualTo(2)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun basicText_simpleString_doesNotPrefetch_whenTextIsShort() {
        val executor = MockExecutor()
        var text by mutableStateOf("a")
        rule.setContent { ProvidePrefetchScheduler(executor) { BasicText(text) } }

        while (text.length < 8) {
            rule.waitForIdle()
            Truth.assertThat(executor.commands.size).isEqualTo(0)
            text += "a"
        }
        // only prefetch when text becomes at least 8 characters long
        rule.waitForIdle()
        Truth.assertThat(executor.commands.size).isEqualTo(1)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun basicText_simpleString_doesNotPrefetch_whenTextIsLong() {
        val executor = MockExecutor()
        var text by mutableStateOf("a".repeat(1005))
        rule.setContent { ProvidePrefetchScheduler(executor) { BasicText(text) } }

        while (text.length > 999) {
            rule.waitForIdle()
            Truth.assertThat(executor.commands.size).isEqualTo(0)
            text = text.drop(1)
        }
        // only prefetch when text is at most 999 characters long
        rule.waitForIdle()
        Truth.assertThat(executor.commands.size).isEqualTo(1)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun basicText_annotatedString_prefetchRequestComesDuringComposition() {
        val executor = MockExecutor()
        var assertions = 0
        rule.setContent {
            ProvidePrefetchScheduler(executor) {
                Truth.assertThat(executor.commands.size).isEqualTo(0)
                BasicText(AnnotatedString("Hello World"))
                Truth.assertThat(executor.commands.size).isEqualTo(1)
                assertions++
            }
        }

        rule.waitForIdle()
        Truth.assertThat(assertions).isEqualTo(1)
        Truth.assertThat(executor.commands.size).isEqualTo(1)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun basicText_annotatedString_prefetchHappens_whenTextChanges() {
        val executor = MockExecutor()
        var text by mutableStateOf(AnnotatedString("Hello World"))
        rule.setContent { ProvidePrefetchScheduler(executor) { BasicText(text) } }

        rule.waitForIdle()

        text += AnnotatedString("A")

        rule.waitForIdle()

        Truth.assertThat(executor.commands.size).isEqualTo(2)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun basicText_annotatedString_prefetchHappens_whenStyleChanges() {
        val executor = MockExecutor()
        var style by mutableStateOf(TextStyle(fontSize = 10.sp))
        rule.setContent {
            ProvidePrefetchScheduler(executor) {
                BasicText(AnnotatedString("Hello World"), style = style)
            }
        }

        rule.waitForIdle()

        style = style.copy(fontSize = 12.sp)

        rule.waitForIdle()

        Truth.assertThat(executor.commands.size).isEqualTo(2)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun basicText_annotatedString_doesNotPrefetch_whenTextIsShort() {
        val executor = MockExecutor()
        var text by mutableStateOf(AnnotatedString("a"))
        rule.setContent { ProvidePrefetchScheduler(executor) { BasicText(text) } }

        while (text.length < 8) {
            rule.waitForIdle()
            Truth.assertThat(executor.commands.size).isEqualTo(0)
            text += AnnotatedString("a")
        }
        // only prefetch when text becomes at least 8 characters long
        rule.waitForIdle()
        Truth.assertThat(executor.commands.size).isEqualTo(1)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun basicText_annotatedString_doesNotPrefetch_whenTextIsLong() {
        val executor = MockExecutor()
        var text by mutableStateOf(AnnotatedString("a".repeat(1005)))
        rule.setContent { ProvidePrefetchScheduler(executor) { BasicText(text) } }

        while (text.length > 999) {
            rule.waitForIdle()
            Truth.assertThat(executor.commands.size).isEqualTo(0)
            text = AnnotatedString(text.drop(1).toString())
        }
        // only prefetch when text is at most 999 characters long
        rule.waitForIdle()
        Truth.assertThat(executor.commands.size).isEqualTo(1)
    }

    companion object {
        @Composable
        private fun ProvidePrefetchScheduler(executor: Executor, content: @Composable () -> Unit) {
            CompositionLocalProvider(
                LocalBackgroundTextMeasurementExecutor provides executor,
                content,
            )
        }
    }
}

private class MockExecutor : Executor {
    var commands = mutableListOf<Runnable>()

    override fun execute(command: Runnable?) {
        commands.add(command!!)
    }
}
