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

package androidx.compose.remote.creation.compose.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource

/**
 * A JUnit test rule for testing Remote Compose document creation and painting.
 *
 * This rule provides:
 * - [androidContext]: The application [Context].
 * - [density]: The display density.
 * - [context]: An [AndroidRemoteContext] configured for testing.
 * - [creationState]: A [RemoteComposeCreationState] instance.
 *
 * To use this rule, include it in your test class:
 * ```kotlin
 * @get:Rule val rule = RemoteDocumentTestRule()
 * ```
 *
 * You can then use the [initialise] function to run code that populates the
 * [creationState.document] and automatically triggers the creation of a [CoreDocument] and paints
 * it using the provided [context].
 *
 * Example:
 * ```kotlin
 * @Test
 * fun myRemoteComposeTest() = rule.initialise { creationState ->
 *     // Use creationState to build your remote compose content.
 *     // For example, add elements to creationState.document.
 *     creationState.document.addNode(...)
 *     // The CoreDocument will be made and painted after this block.
 * }
 * ```
 */
class RemoteDocumentTestRule : ExternalResource() {
    val androidContext by lazy { ApplicationProvider.getApplicationContext<Context>() }

    val density by lazy { RemoteDensity(androidContext.resources.displayMetrics.density.rf, 1f.rf) }
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
            density = this@RemoteDocumentTestRule.density.density.constantValue
        }

    val creationState by lazy {
        RemoteComposeCreationState(AndroidxRcPlatformServices(), Size(1f, 1f)).apply {
            remoteDensity = density
        }
    }

    inline fun <T> initialise(crossinline body: (RemoteComposeCreationState) -> T): T {
        return body(creationState).also { makeAndPaintCoreDocument() }
    }

    fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}
