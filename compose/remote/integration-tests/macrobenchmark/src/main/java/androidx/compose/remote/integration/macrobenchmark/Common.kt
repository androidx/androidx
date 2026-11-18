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

package androidx.compose.remote.integration.macrobenchmark

import android.graphics.Bitmap
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.TraceSectionMetric.Mode
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.testutils.composeSectionMetrics
import androidx.testutils.defaultMemoryMetrics
import org.junit.Assert.assertTrue

internal const val LIST_CONTENT_DESCRIPTION = "ScrollableColumn"
internal const val PACKAGE_NAME = "androidx.compose.remote.integration.macrobenchmark.target"
internal const val SCROLL_ACTIVITY =
    "androidx.compose.remote.integration.macrobenchmark.target.SCROLL_ACTIVITY"
internal const val FIRST_FRAME_ACTIVITY =
    "androidx.compose.remote.integration.macrobenchmark.target.FIRST_FRAME_ACTIVITY"
internal const val DOCUMENT_TRACING_ACTIVITY =
    "androidx.compose.remote.integration.macrobenchmark.target.DOCUMENT_TRACING_ACTIVITY"
internal const val DOCUMENT_GENERATING_ACTIVITY =
    "androidx.compose.remote.integration.macrobenchmark.target.DOCUMENT_GENERATING_ACTIVITY"

const val BENCHMARK_MODE_ARG = "benchmark_mode"
const val DOCUMENT_READY = "READY"
const val MODE_RENDER_FROM_CACHE = "render_from_cache"
const val MODE_COMPOSE = "mode_compose"
const val MODE_REMOTE_COMPOSE = "mode_remote_compose"
const val MODE_WEB_VIEW = "mode_web_view"
const val MODE_REMOTE_VIEW = "mode_remote_view"
const val MODE_EMBEDDED_PLAYER = "mode_embedded_player"
const val MODE_JAVA_PLAYER = "mode_java_player"
const val MODE_VIEW_PLAYER = "mode_view_player"
const val MODE_DOCUMENT_PLAYER_EMBEDDED = "mode_document_player_embedded"
const val MODE_DOCUMENT_PLAYER_VIEW = "mode_document_player_view"
const val MODE_EMPTY = "mode_empty"

internal const val LOAD_TIME_ACTIVITY =
    "androidx.compose.remote.integration.macrobenchmark.target.LOAD_TIME_ACTIVITY"
internal const val ANIMATION_ACTIVITY =
    "androidx.compose.remote.integration.macrobenchmark.target.ANIMATION_ACTIVITY"
internal const val STATE_UPDATE_ACTIVITY =
    "androidx.compose.remote.integration.macrobenchmark.target.STATE_UPDATE_ACTIVITY"
internal const val STATE_LAYOUT_ACTIVITY =
    "androidx.compose.remote.integration.macrobenchmark.target.STATE_LAYOUT_ACTIVITY"
internal const val IMAGE_LOADING_ACTIVITY =
    "androidx.compose.remote.integration.macrobenchmark.target.IMAGE_LOADING_ACTIVITY"
internal const val FEED_ACTIVITY =
    "androidx.compose.remote.integration.macrobenchmark.target.FEED_ACTIVITY"
internal const val STATE_BENCH_ACTIVITY =
    "androidx.compose.remote.integration.macrobenchmark.target.STATE_BENCH_ACTIVITY"

internal const val LOAD_TIME_CONTENT_DESCRIPTION = "LoadTimeContainer"
internal const val SPINNER_CONTENT_DESCRIPTION = "IndefiniteSpinner"
internal const val STATE_UPDATE_CONTENT_DESCRIPTION = "StateUpdateText"
internal const val STATE_LAYOUT_CONTENT_DESCRIPTION = "StateLayoutContainer"
internal const val IMAGE_GRID_CONTENT_DESCRIPTION = "ImageGridContainer"
internal const val FEED_CONTENT_DESCRIPTION = "RemoteFeedList"
internal const val STATE_BENCH_CONTENT_DESCRIPTION = "StateBenchContainer"

public val recordingTraces: List<String> =
    listOf(
        "CaptureRemoteDocument:captureSingleRemoteDocument",
        "CaptureRemoteDocument:captureSingleRemoteDocument:setContent",
        "CaptureRemoteDocument:captureSingleRemoteDocument:settle",
        "CaptureRemoteDocument:captureSingleRemoteDocument:rootNodeRender",
        "CaptureRemoteDocument:captureSingleRemoteDocument:encodeToByteArray",
    )
public val decodingTraces: List<String> = listOf("CreateRemoteDocument:parsing")
public val allTraces: List<String> = recordingTraces + decodingTraces

@OptIn(ExperimentalMetricApi::class)
internal fun feedMetrics(): List<Metric> =
    listOf(FrameTimingMetric()) +
        composeSectionMetrics() +
        listOf(
            TraceSectionMetric("Compose:recompose", label = "recomposeCount", mode = Mode.Count),
            TraceSectionMetric("RcPlayer:preprocessDocument", mode = Mode.Count),
            TraceSectionMetric("RcPlayer:%", label = "rcPlayerTotal", mode = Mode.Sum),
            TraceSectionMetric("RcView:%", label = "rcViewTotal", mode = Mode.Sum),
            TraceSectionMetric("CreateRemoteDocument:parsing", mode = Mode.Count),
            TraceSectionMetric("RemoteBench:placeholder", mode = Mode.Count),
            TraceSectionMetric("RemoteBench:hostUpdate", mode = Mode.Count),
        ) +
        defaultMemoryMetrics()

@OptIn(ExperimentalMetricApi::class)
internal fun stateMetrics(): List<Metric> =
    listOf(FrameTimingMetric()) +
        composeSectionMetrics() +
        listOf(
            TraceSectionMetric("Compose:recompose", label = "recomposeCount", mode = Mode.Count),
            TraceSectionMetric("RcPlayer:hostValueChange", mode = Mode.Count),
            TraceSectionMetric("RcPlayer:%", label = "rcPlayerTotal", mode = Mode.Sum),
            TraceSectionMetric("RcView:%", label = "rcViewTotal", mode = Mode.Sum),
            TraceSectionMetric("RemoteBench:hostUpdate", mode = Mode.Count),
        )

/**
 * Verifies that the active benchmark lane actually rendered content:
 * 1. If [contentDescription] is provided, waits for a matching semantics node.
 * 2. Samples the screen to verify it is not a single flat colour.
 */
internal fun assertLaneDrew(device: UiDevice, contentDescription: String? = null) {
    if (contentDescription != null) {
        val found = device.wait(Until.hasObject(By.desc(contentDescription)), 10_000)
        assertTrue("Expected semantics node '$contentDescription' not found", found)
    }
    device.waitForIdle()
    var colors = sampleScreenColors()
    if (colors.distinct().size <= 1) {
        Thread.sleep(300L)
        colors = sampleScreenColors()
    }
    assertTrue(
        "Expected rendered content, but screen is a single flat color (${colors.distinct().size} distinct colors)",
        colors.distinct().size > 1,
    )
}

/** Verifies that rendered output changes within [waitMs] (for state-updating lanes). */
internal fun assertScreenChanged(waitMs: Long = 300L) {
    val before = sampleScreenColors(gridSize = 128)
    val budgetMs = waitMs.coerceAtLeast(500L)
    val deadline = System.currentTimeMillis() + budgetMs
    while (System.currentTimeMillis() < deadline) {
        Thread.sleep(120L)
        val after = sampleScreenColors(gridSize = 128)
        if (before != after) return
    }
    assertTrue("Expected rendered output to change within ${budgetMs}ms", false)
}

private fun sampleScreenColors(gridSize: Int = 16): List<Int> {
    val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    var bitmap: Bitmap? = null
    for (attempt in 0 until 5) {
        bitmap = uiAutomation.takeScreenshot()
        if (bitmap != null) break
        Thread.sleep(100L)
    }
    checkNotNull(bitmap) { "UiAutomation.takeScreenshot() returned null" }
    try {
        val startX = (bitmap.width * 0.1f).toInt()
        val endX = (bitmap.width * 0.9f).toInt().coerceAtLeast(startX + 1)
        val startY = (bitmap.height * 0.1f).toInt()
        val endY = (bitmap.height * 0.9f).toInt().coerceAtLeast(startY + 1)
        val stepX = ((endX - startX) / gridSize).coerceAtLeast(1)
        val stepY = ((endY - startY) / gridSize).coerceAtLeast(1)
        val colors = ArrayList<Int>(gridSize * gridSize)
        var y = startY
        while (y < endY) {
            var x = startX
            while (x < endX) {
                colors.add(bitmap.getPixel(x, y))
                x += stepX
            }
            y += stepY
        }
        return colors
    } finally {
        bitmap.recycle()
    }
}
