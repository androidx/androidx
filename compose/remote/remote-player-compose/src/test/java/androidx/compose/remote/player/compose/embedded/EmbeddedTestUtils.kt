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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.SystemClock
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/*
 * Shared helpers for embedded player ([RcPlayer]) tests. They only use public player APIs and
 * Compose UI testing APIs, so tests observe the player the way a host would.
 */

/**
 * Builds a [CoreDocument] from low-level [RemoteComposeWriter] calls, for documents the creation
 * DSL can't express (for example hand-written layout headers or raw operations).
 */
internal fun buildWriterDocument(
    width: Int = 300,
    height: Int = 300,
    name: String = "EmbeddedPlayerTest",
    apiLevel: Int = CoreDocument.DOCUMENT_API_LEVEL,
    profiles: Int = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
    clock: RemoteClock = RemoteClock.SYSTEM,
    block: RemoteComposeWriter.() -> Unit,
): CoreDocument {
    val writer =
        RemoteComposeWriterAndroid(
            width,
            height,
            name,
            apiLevel,
            profiles,
            AndroidxRcPlatformServices(),
        )
    writer.block()
    return CoreDocument(clock).apply {
        ByteArrayInputStream(writer.encodeToByteArray()).use {
            initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
        }
    }
}

/** Captures the whole composition as an Android [Bitmap]. */
internal fun SemanticsNodeInteractionsProvider.captureRootBitmap(): Bitmap =
    onRoot().captureToImage().asAndroidBitmap()

/** Captures the top-left [width] x [height] pixels of the whole composition. */
internal fun SemanticsNodeInteractionsProvider.captureRootPixels(
    width: Int,
    height: Int,
): IntArray {
    val pixels = IntArray(width * height)
    captureRootBitmap().getPixels(pixels, 0, width, 0, 0, width, height)
    return pixels
}

/** The [TextLayoutResult] of the text node that shows [text]. */
internal fun SemanticsNodeInteractionsProvider.textLayoutResultOf(text: String): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    onNodeWithText(text, useUnmergedTree = true)
        .fetchSemanticsNode()
        .config[SemanticsActions.GetTextLayoutResult]
        .action
        ?.invoke(results)
    return results.single()
}

/** The player's current document time, as reported by its inspectable state. */
internal val RcPlayerState.currentTimeMillis: Float
    get() = inspectableElements.first { it.name == "currentTimeMillis" }.value as Float

/** How often a player advances document time. */
internal enum class ClockCadence {
    /** Time stays at the value seeded at setup. */
    STATIC,
    /** Time advances on whole-second boundaries. */
    DISCRETE_SECOND,
    /** Time advances every frame. */
    CONTINUOUS_FRAME,
}

/** Plays a document holding [ops] and reports how often the player advances its time. */
internal fun ComposeContentTestRule.observeClockCadence(vararg ops: Operation): ClockCadence =
    observeClockCadence { clock ->
        CoreDocument(clock).apply { operations.addAll(ops) }
    }

/**
 * Plays the document from [createDocument] with a fixed clock and reports how often the player
 * advances its time: every frame, on whole seconds, or not at all.
 */
internal fun ComposeContentTestRule.observeClockCadence(
    createDocument: (RemoteClock) -> CoreDocument
): ClockCadence {
    val baseInstant = Instant.parse("2026-01-01T12:30:45.100Z")
    val clock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))
    val state = RcPlayerState(createDocument(clock))

    mainClock.autoAdvance = false
    setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(state = state) } }
    // Settle the initial frame at t = 0.
    mainClock.advanceTimeBy(32)
    val initialMillis = state.currentTimeMillis

    // Advance 200ms within the same second (.100Z -> .300Z).
    repeat(10) { mainClock.advanceTimeBy(20) }
    if (state.currentTimeMillis > initialMillis) {
        return ClockCadence.CONTINUOUS_FRAME
    }

    // Advance across the whole-second boundary (+900ms -> 1.2s elapsed).
    repeat(10) { mainClock.advanceTimeBy(100) }
    return if (state.currentTimeMillis > initialMillis) {
        ClockCadence.DISCRETE_SECOND
    } else {
        ClockCadence.STATIC
    }
}
