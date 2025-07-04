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

package androidx.compose.foundation.text

import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraphIntrinsics
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.util.trace
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

/**
 * CompositionLocal that provides an Executor for background text processing to potentially get run
 * on.
 *
 * BasicText premeasure is the process of using a background thread to early start metrics
 * calculation for Text composables on Android to warm up the underlying text layout cache. This
 * becomes especially useful in LazyLists when precomposition may precede premeasurement by at least
 * a frame, which gives the background thread enough time to fully calculate text metrics. This
 * approximately reduces text layout duration on main thread from 50% to 90%.
 *
 * By default this CompositionLocal provides null, which means that any text prefetch behavior will
 * revert to the system default. You can provide an executor like
 * `Executors.newSingleThreadExecutor()` for BasicText to schedule background tasks.
 *
 * Please note that prefetch text does not guarantee a net performance increase. It may actually be
 * harmful in certain scenarios where there is not enough time between composition and measurement
 * for background thread to actually start warming the cache, or when the text is long enough that
 * it floods the cache and overflows it, at around 5000 words.
 *
 * Use benchmarking tools to check whether enabling this behavior works well for your use case.
 *
 * @sample androidx.compose.foundation.samples.BackgroundTextMeasurementSample
 */
val LocalBackgroundTextMeasurementExecutor = staticCompositionLocalOf<Executor?> { null }

@Composable
@NonRestartableComposable
internal actual fun BackgroundTextMeasurement(
    text: String,
    style: TextStyle,
    fontFamilyResolver: FontFamily.Resolver,
) {
    val executor = LocalBackgroundTextMeasurementExecutor.current
    if (executor != null && shouldPrefetch(text.length)) {
        val layoutDirection = LocalLayoutDirection.current
        val density = LocalDensity.current

        try {
            executor.execute {
                trace("BackgroundTextMeasurement") {
                    Snapshot.withMutableSnapshot {
                        val resolvedStyle = resolveDefaults(style, layoutDirection)
                        val intrinsics =
                            ParagraphIntrinsics(
                                text = text,
                                style = resolvedStyle,
                                density = density,
                                fontFamilyResolver = fontFamilyResolver,
                                annotations = emptyList(),
                            )
                        intrinsics.maxIntrinsicWidth
                    }
                }
            }
        } catch (_: RejectedExecutionException) {}
    }
}

@Composable
@NonRestartableComposable
internal actual fun BackgroundTextMeasurement(
    text: AnnotatedString,
    style: TextStyle,
    fontFamilyResolver: FontFamily.Resolver,
    placeholders: List<AnnotatedString.Range<Placeholder>>?,
) {
    val executor = LocalBackgroundTextMeasurementExecutor.current
    if (executor != null && shouldPrefetch(text.length)) {
        val layoutDirection = LocalLayoutDirection.current
        val density = LocalDensity.current

        try {
            executor.execute {
                trace("BackgroundTextMeasurement") {
                    Snapshot.withMutableSnapshot {
                        val resolvedStyle = resolveDefaults(style, layoutDirection)
                        val intrinsics =
                            MultiParagraphIntrinsics(
                                annotatedString = text,
                                style = resolvedStyle,
                                density = density,
                                placeholders = placeholders ?: emptyList(),
                                fontFamilyResolver = fontFamilyResolver,
                            )
                        intrinsics.maxIntrinsicWidth
                    }
                }
            }
        } catch (_: RejectedExecutionException) {}
    }
}

/**
 * The minimum number of CPU cores that should exist for us to consider attempting text prefetch.
 */
private const val PrefetchTextMinimumCoreCount = 4

/**
 * Defines the shortest text length that can be considered for prefetching. Texts that are shorter
 * than this number are usually not worth creating a threading overhead.
 */
private const val MinTextLengthThreshold = 8

/**
 * Defines the longest text length that can be considered for prefetching. Texts that are longer
 * than this number have a chance to flood the cache to cause overflow, essentially leading to
 * double measurement that causes performance regression.
 */
private const val MaxTextLengthThreshold = 1000

/** Reading the core count is expensive. Do it once and cache it globally. */
private var backingCoreCountSatisfactory: Boolean? = null

@VisibleForTesting
internal val coreCountSatisfactory: Boolean
    get() {
        if (backingCoreCountSatisfactory == null) {
            backingCoreCountSatisfactory =
                Runtime.getRuntime().availableProcessors() >= PrefetchTextMinimumCoreCount
        }
        return backingCoreCountSatisfactory!!
    }

internal fun shouldPrefetch(textLength: Int): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
        textLength >= MinTextLengthThreshold &&
        textLength < MaxTextLengthThreshold &&
        coreCountSatisfactory
}
