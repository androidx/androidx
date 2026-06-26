/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui


import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.hostOs

private var subscriberCount = 0
private var pollingJob: Job? = null
private val subscribeLock = Any()
private var currentSystemTheme = mutableStateOf(org.jetbrains.skiko.currentSystemTheme)

@OptIn(DelicateCoroutinesApi::class)
private fun onSubscriberAdded() {
    synchronized(subscribeLock) {
        if (subscriberCount == 0) {
            pollingJob = GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    pollCurrentSystemTheme()
                }
            }
        }
        subscriberCount += 1
    }
}

private fun onSubscriberRemoved() {
    synchronized(subscribeLock) {
        subscriberCount -= 1
        if (subscriberCount == 0) {
            pollingJob?.cancel()
            pollingJob = null
        }
    }
}

private suspend fun pollCurrentSystemTheme() {
    while (true) {
        currentSystemTheme.value = org.jetbrains.skiko.currentSystemTheme
        delay(1.seconds)
    }
}

@Composable
internal fun ProvideSystemTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSystemTheme provides currentSystemTheme.value,
        content = content
    )

    if (DesktopComposeUiFlags.pollSystemTheme) {
        DisposableEffect(Unit) {
            onSubscriberAdded()
            onDispose {
                onSubscriberRemoved()
            }
        }
    }
}

@VisibleForTesting
internal fun systemThemeSubscriberCount() = subscriberCount

@VisibleForTesting
internal fun systemThemePollingJob() = pollingJob
