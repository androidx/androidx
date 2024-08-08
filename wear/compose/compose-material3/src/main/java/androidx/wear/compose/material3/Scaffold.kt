/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import androidx.wear.compose.foundation.ScrollInfoProvider
import kotlinx.coroutines.delay

internal class ScaffoldState(private val appTimeText: (@Composable (() -> Unit))? = null) {
    fun removeScreen(key: Any) {
        screenContent.removeIf { it.key === key }
    }

    fun addScreen(
        key: Any,
        timeText: @Composable (() -> Unit)?,
        scrollInfoProvider: ScrollInfoProvider? = null
    ) {
        screenContent.add(ScreenContent(key, scrollInfoProvider, timeText))
    }

    val timeText: @Composable (() -> Unit)
        get() = {
            val (screenContent, timeText) = currentContent()
            Box(
                modifier =
                    screenContent?.scrollInfoProvider?.let {
                        Modifier.fillMaxSize().scrollAway(it) { screenStage.value }
                    } ?: Modifier
            ) {
                timeText()
            }
        }

    internal val screenStage: MutableState<ScreenStage> = mutableStateOf(ScreenStage.New)

    @Composable
    internal fun UpdateIdlingDetectorIfNeeded() {
        val scrollInfoProvider = currentContent().first?.scrollInfoProvider
        LaunchedEffect(scrollInfoProvider) { screenStage.value = ScreenStage.New }
        if (scrollInfoProvider?.isScrollInProgress == true) {
            screenStage.value = ScreenStage.Scrolling
        } else {
            LaunchedEffect(Unit) {
                // Entering the idle state will show the Time text (if it's hidden) AND hide the
                // scroll indicator.
                delay(IDLE_DELAY)
                screenStage.value = ScreenStage.Idle
            }
        }
    }

    internal data class ScreenContent(
        val key: Any,
        val scrollInfoProvider: ScrollInfoProvider? = null,
        val timeText: (@Composable () -> Unit)? = null,
    )

    private fun currentContent(): Pair<ScreenContent?, @Composable (() -> Unit)> {
        var resultTimeText: @Composable (() -> Unit)? = null
        var resultContent: ScreenContent? = null
        screenContent.fastForEach {
            if (it.timeText != null) {
                resultTimeText = it.timeText
            }
            if (it.scrollInfoProvider != null) {
                resultContent = it
            }
        }
        return resultContent to (resultTimeText ?: appTimeText ?: {})
    }

    private val screenContent = mutableStateListOf<ScreenContent>()
}

internal val LocalScaffoldState = compositionLocalOf { ScaffoldState() }

private const val IDLE_DELAY = 2500L
