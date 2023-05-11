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

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/**
 * [WearPreviewDevices] is a multi-preview annotation for composables with different Wear device
 * shapes and sizes. It supports [Devices.WEAR_OS_SMALL_ROUND], [Devices.WEAR_OS_LARGE_ROUND] and
 * [Devices.WEAR_OS_SQUARE].
 *
 * @sample androidx.wear.compose.material.samples.ToggleButtonWithIconPreview
 * @see Devices.WEAR_OS_SMALL_ROUND
 * @see Devices.WEAR_OS_LARGE_ROUND
 * @see Devices.WEAR_OS_SQUARE
 */
@Preview(
    device = Devices.WEAR_OS_SQUARE,
    backgroundColor = 0xff000000,
    showBackground = true,
    group = "Devices - Small Square",
    showSystemUi = true
)
@Preview(
    device = Devices.WEAR_OS_LARGE_ROUND,
    backgroundColor = 0xff000000,
    showBackground = true,
    group = "Devices - Large Round",
    showSystemUi = true
)
@Preview(
    device = Devices.WEAR_OS_SMALL_ROUND,
    backgroundColor = 0xff000000,
    showBackground = true,
    group = "Devices - Small Round",
    showSystemUi = true
)
public annotation class WearPreviewDevices
