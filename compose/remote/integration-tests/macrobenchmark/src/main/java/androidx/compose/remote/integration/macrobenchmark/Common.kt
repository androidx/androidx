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

public val recordingTraces: List<String> =
    listOf(
        "CaptureRemoteDocument:captureSingleRemoteDocument",
        "CaptureRemoteDocument:captureSingleRemoteDocument:compositionInitialization",
        "CaptureRemoteDocument:captureSingleRemoteDocument:rootNodeRender",
        "CaptureRemoteDocument:captureSingleRemoteDocument:toByteArray",
    )
public val decodingTraces: List<String> = listOf("CreateRemoteDocument:parsing")
public val allTraces: List<String> = recordingTraces + decodingTraces
