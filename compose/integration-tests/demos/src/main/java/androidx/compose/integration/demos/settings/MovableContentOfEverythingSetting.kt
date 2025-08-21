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

package androidx.compose.integration.demos.settings

import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import kotlinx.coroutines.delay

internal enum class MovableContentOfEverythingMode(val summary: String) {
    Disabled(summary = "The content will not be moved around."),
    Sibling(
        summary = "The content will be switched back and forth between siblings every 3 seconds."
    ),
    BetweenSubcomposition(
        summary = "The content will be moved in and out of subcomposition every 3 seconds."
    ),
}

/** Setting that stresses movableContentOf, by moving the entire content of the demo app around. */
internal object MovableContentOfEverythingSetting : DemoSetting<Boolean> {

    private const val MovableContentOfEverythingKey = "MovableContentOfEverything"

    override fun createPreference(context: Context) =
        DropDownPreference(context).apply {
            title = "movableContentOf everything"
            key = MovableContentOfEverythingKey
            MovableContentOfEverythingMode.values()
                .map { it.name }
                .toTypedArray()
                .also {
                    entries = it
                    entryValues = it
                }
            summaryProvider =
                Preference.SummaryProvider<DropDownPreference> {
                    val mode = MovableContentOfEverythingMode.valueOf(value)
                    """
                ${mode.name}
                ${mode.summary}
            """
                        .trimIndent()
                }
            setDefaultValue(MovableContentOfEverythingMode.Disabled.name)
        }

    @Composable
    fun asState() =
        preferenceAsState(MovableContentOfEverythingKey) {
            val value =
                getString(
                    MovableContentOfEverythingKey,
                    MovableContentOfEverythingMode.Disabled.name,
                ) ?: MovableContentOfEverythingMode.Disabled.name
            MovableContentOfEverythingMode.valueOf(value)
        }
}

/** Moves the [content] around in accordance with the given [mode]. */
@Composable
internal fun MovableContentOfEverythingSetting(
    mode: MovableContentOfEverythingMode,
    content: @Composable () -> Unit,
) {
    val movableContent = remember { movableContentOf<@Composable () -> Unit> { it() } }
    when (mode) {
        MovableContentOfEverythingMode.Disabled -> {
            // If mode is disabled, just output the content statically
            movableContent(content)
        }
        MovableContentOfEverythingMode.Sibling,
        MovableContentOfEverythingMode.BetweenSubcomposition -> {
            // Otherwise, flip the flag every 3 seconds to stress test moving the
            // movableContentOf
            var flag by remember { mutableStateOf(true) }
            LaunchedEffect(flag) {
                delay(3000)
                flag = !flag
            }
            when (mode) {
                MovableContentOfEverythingMode.Sibling -> {
                    // For sibling, just flip the content back and forth
                    if (flag) {
                        movableContent(content)
                    } else {
                        movableContent(content)
                    }
                }
                MovableContentOfEverythingMode.BetweenSubcomposition -> {
                    // For between subcomposition, flip the content in and out of a
                    // BoxWithConstraints for subcomposition
                    if (flag) {
                        movableContent(content)
                    } else {
                        // Intentionally using subcomposition even when not needed for demo
                        // purposes
                        @Suppress("UnusedBoxWithConstraintsScope")
                        BoxWithConstraints { movableContent(content) }
                    }
                }
                MovableContentOfEverythingMode.Disabled -> error("not possible to reach")
            }
        }
    }
}
