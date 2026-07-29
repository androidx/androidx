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

package androidx.compose.ui.test.failure

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorageRegistry

internal interface ScreenshotHandler {
    fun export(fileName: String)
}

/**
 * Implementation of [ScreenshotHandler] that uses [android.app.UiAutomation] to capture the
 * physical device screen.
 *
 * The resulting bitmap is compressed as a PNG and written directly to the
 * [androidx.test.platform.io.PlatformTestStorageRegistry].
 */
internal class AndroidScreenshotHandler : ScreenshotHandler {
    @Suppress("UnsafeOptInUsageError")
    override fun export(fileName: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot =
            instrumentation.uiAutomation.takeScreenshot()
                ?: throw RuntimeException("UiAutomation.takeScreenshot() returned null")

        try {
            val storage = PlatformTestStorageRegistry.getInstance()
            storage.openOutputFile(fileName).use { stream ->
                screenshot.compress(Bitmap.CompressFormat.PNG, 0, stream)
            }
        } finally {
            screenshot.recycle()
        }
    }
}
