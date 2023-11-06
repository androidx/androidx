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
 * [WearPreviewDevices] is a multi-preview annotation for composables with different Wear device
 * shapes and sizes. It supports [WearDevices.SMALL_ROUND], [WearDevices.LARGE_ROUND] and
 * [WearDevices.SQUARE].
 *
 * @sample androidx.wear.compose.material.samples.ToggleButtonWithIconPreview
 * @see WearDevices.SMALL_ROUND
 * @see WearDevices.LARGE_ROUND
 * @see WearDevices.SQUARE
 */
@Preview(
    device = WearDevices.SQUARE,
    backgroundColor = 0xff000000,
    showBackground = true,
    group = "Devices - Small Square",
    showSystemUi = true
)
@Preview(
    device = WearDevices.LARGE_ROUND,
    backgroundColor = 0xff000000,
    showBackground = true,
    group = "Devices - Large Round",
    showSystemUi = true
)
@Preview(
    device = WearDevices.SMALL_ROUND,
    backgroundColor = 0xff000000,
    showBackground = true,
    group = "Devices - Small Round",
    showSystemUi = true
)
public annotation class WearPreviewDevices
