/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation

import androidx.savedstate.SavedState
import androidx.savedstate.savedState
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
internal class NavActionTest(private val params: Params) {

    data class Params(
        val name: String,
        val action1: NavAction,
        val action2: NavAction,
        val isEqual: Boolean,
    )

    @Test
    fun testEquals() {
        with(params) { assertThat(action1 == action2).isEqualTo(isEqual) }
    }

    @Test
    fun testHashCode() {
        with(params) { assertThat(action1.hashCode() == action2.hashCode()).isEqualTo(isEqual) }
    }

    @Test
    fun testToString() {
        with(params) {
            assumeTrue(name != "defaultArguments") // toString ignores defaultArguments.
            assertThat(action1.toString() == action2.toString()).isEqualTo(isEqual)
        }
    }

    companion object {

        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() =
            arrayOf(
                // destinationId
                Params(
                    name = "destinationId",
                    action1 = navActions(destinationId = 1),
                    action2 = navActions(destinationId = 1),
                    isEqual = true,
                ),
                Params(
                    name = "destinationId",
                    action1 = navActions(destinationId = 1),
                    action2 = navActions(destinationId = 2),
                    isEqual = false,
                ),

                // navOptions
                Params(
                    name = "navOptions",
                    action1 = navActions(navOptions = navOptions { setRestoreState(true) }),
                    action2 = navActions(navOptions = navOptions { setRestoreState(true) }),
                    isEqual = true,
                ),
                Params(
                    name = "navOptions",
                    action1 = navActions(navOptions = navOptions { setRestoreState(true) }),
                    action2 = navActions(navOptions = navOptions { setRestoreState(false) }),
                    isEqual = false,
                ),

                // defaultArguments
                Params(
                    name = "defaultArguments",
                    action1 = navActions(defaultArguments = defaultArguments()),
                    action2 = navActions(defaultArguments = defaultArguments()),
                    isEqual = true,
                ),
                Params(
                    name = "defaultArguments",
                    action1 = navActions(defaultArguments = defaultArguments()),
                    action2 = navActions(defaultArguments = savedState { putString("key", "val") }),
                    isEqual = false,
                ),
            )

        private fun navActions(
            destinationId: Int = 1,
            navOptions: NavOptions? = null,
            defaultArguments: SavedState = savedState(),
        ): NavAction = NavAction(destinationId, navOptions, defaultArguments)

        private fun navOptions(builderAction: NavOptions.Builder.() -> Unit): NavOptions {
            return NavOptions.Builder().apply(builderAction).build()
        }

        private fun defaultArguments(): SavedState {
            var key = 0
            val savedState = savedState {
                putBoolean(key = "KEY_${++key}", value = true)
                putChar(key = "KEY_${++key}", value = Char.MAX_VALUE)
                putDouble(key = "KEY_${++key}", value = Double.MAX_VALUE)
                putFloat(key = "KEY_${++key}", value = Float.MAX_VALUE)
                putInt(key = "KEY_${++key}", value = Int.MAX_VALUE)
                putLong(key = "KEY_${++key}", value = Long.MAX_VALUE)
                putNull(key = "KEY_${++key}")
                putString(key = "KEY_${++key}", value = "Text")
            }
            return savedState {
                putAll(savedState)
                putSavedState(key = "KEY_${++key}", savedState)
            }
        }
    }
}
