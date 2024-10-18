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

package androidx.wear.compose.material3.macrobenchmark.common

import androidx.test.uiautomator.UiDevice

internal fun numberedContentDescription(n: Int) = "find-me-$n"

internal fun UiDevice.scrollDown() {
    swipe(
        displayWidth / 2,
        displayHeight - displayHeight / 10,
        displayWidth / 2,
        displayHeight / 10,
        10
    )
}

internal const val FIND_OBJECT_TIMEOUT_MS = 10_000L
