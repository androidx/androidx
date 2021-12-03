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

package androidx.activity

import android.content.ComponentCallbacks2
import androidx.core.util.Consumer
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityCallbacksTest {
    @Test
    fun onTrimMemory() {
        with(ActivityScenario.launch(ComponentActivity::class.java)) {
            var receivedLevel = -1

            val listener = Consumer<Int> { level ->
                receivedLevel = level
            }
            withActivity {
                addOnTrimMemoryListener(listener)
                onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_MODERATE)
            }

            assertThat(receivedLevel).isEqualTo(ComponentCallbacks2.TRIM_MEMORY_MODERATE)
        }
    }

    @Test
    fun onTrimMemoryRemove() {
        with(ActivityScenario.launch(ComponentActivity::class.java)) {
            var receivedLevel = -1

            val listener = Consumer<Int> { level ->
                receivedLevel = level
            }
            withActivity {
                addOnTrimMemoryListener(listener)
                onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_MODERATE)
            }

            assertThat(receivedLevel).isEqualTo(ComponentCallbacks2.TRIM_MEMORY_MODERATE)

            withActivity {
                removeOnTrimMemoryListener(listener)
                onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
            }

            // Should still be MODERATE and not TRIM_MEMORY_COMPLETE
            assertThat(receivedLevel).isEqualTo(ComponentCallbacks2.TRIM_MEMORY_MODERATE)
        }
    }

    @Test
    fun onTrimMemoryRemoveReentrant() {
        with(ActivityScenario.launch(ComponentActivity::class.java)) {
            val activity = withActivity { this }
            var receivedLevel = -1

            val listener = object : Consumer<Int> {
                override fun accept(level: Int) {
                    receivedLevel = level
                    activity.removeOnTrimMemoryListener(this)
                }
            }
            withActivity {
                addOnTrimMemoryListener(listener)
                // Add a second listener to force a ConcurrentModificationException
                // if not properly handled by ComponentActivity
                addOnTrimMemoryListener { }
                onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_MODERATE)
                onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
            }

            // Only the first trim level should be received
            assertThat(receivedLevel).isEqualTo(ComponentCallbacks2.TRIM_MEMORY_MODERATE)
        }
    }
}
