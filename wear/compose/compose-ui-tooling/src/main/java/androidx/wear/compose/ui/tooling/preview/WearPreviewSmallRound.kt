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
 * [WearPreviewSmallRound] is a custom preview annotation for displaying Wear composables on small
 * round Wear device ([Devices.WEAR_OS_SMALL_ROUND]).
 *
 * @sample androidx.wear.compose.material.samples.ButtonWithIconPreview
 * @see [Devices.WEAR_OS_SMALL_ROUND]
 */
@Preview(
    device = Devices.WEAR_OS_SMALL_ROUND,
    backgroundColor = 0xff000000,
    showBackground = true,
    group = "Devices - Small Round",
    showSystemUi = true
)
public annotation class WearPreviewSmallRound
