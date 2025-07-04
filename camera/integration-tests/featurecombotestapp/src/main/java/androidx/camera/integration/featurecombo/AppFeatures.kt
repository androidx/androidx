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

package androidx.camera.integration.featurecombo

data class AppFeatures(
    val dynamicRange: DynamicRange = DynamicRange.SDR,
    val fps: Fps = Fps.FPS_30,
    val stabilizationMode: StabilizationMode = StabilizationMode.OFF,
    val imageFormat: ImageFormat = ImageFormat.JPEG,
    val unsupportedDynamicRanges: List<DynamicRange> = emptyList(),
    val unsupportedFps: List<Fps> = emptyList(),
    val unsupportedStabilizationModes: List<StabilizationMode> = emptyList(),
    val unsupportedImageFormats: List<ImageFormat> = emptyList(),
)

enum class DynamicRange(val text: String) {
    HLG_10("HLG10"),
    SDR("SDR"),
}

enum class StabilizationMode(val text: String) {
    PREVIEW("Preview"),
    OFF("Off"),
}

enum class Fps(val text: String) {
    FPS_60("60"),
    FPS_30("30"),
}

enum class ImageFormat(val text: String) {
    JPEG_R("JPEG_R"),
    JPEG("JPEG"),
}
