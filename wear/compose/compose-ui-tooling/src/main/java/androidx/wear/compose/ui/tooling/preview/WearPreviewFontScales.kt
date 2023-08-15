/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.ui.tooling.preview

import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices

/**
 * [WearPreviewFontScales] is a multi-preview annotation for the Wear devices of following font
 * scales
 * <ul>
 *     <li> Fonts - Small: 0.94f </li>
 *     <li> Fonts - Normal: 1f </li>
 *     <li> Fonts - Medium: 1.06f </li>
 *     <li> Fonts - Large: 1.12f </li>
 *     <li> Fonts - Larger: 1.18f </li>
 *     <li> Fonts - Largest: 1.24f </li>
 * </ul>
 * Font scales represent the scaling factor for fonts, relative to the base density scaling. Please
 * note, the above list is not exhaustive. It previews the composables on a small round Wear device.
 *
 * @sample androidx.wear.compose.material.samples.TitleCardWithImagePreview
 * @see [Preview.fontScale]
 */
@Preview(
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    group = "Fonts - Small",
    fontScale = 0.94f
)
@Preview(
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    group = "Fonts - Normal",
    fontScale = 1f
)
@Preview(
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    group = "Fonts - Medium",
    fontScale = 1.06f
)
@Preview(
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    group = "Fonts - Large",
    fontScale = 1.12f
)
@Preview(
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    group = "Fonts - Larger",
    fontScale = 1.18f
)
@Preview(
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    group = "Fonts - Largest",
    fontScale = 1.24f
)
public annotation class WearPreviewFontScales
