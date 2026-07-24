/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.tooling.preview

import androidx.annotation.StringDef

/** List with the pre-defined devices available to be used in the preview. */
public object Devices {
    public const val DEFAULT: String = ""

    public const val NEXUS_7: String = "id:Nexus 7"
    public const val NEXUS_7_2013: String = "id:Nexus 7 2013"
    public const val NEXUS_5: String = "id:Nexus 5"
    public const val NEXUS_6: String = "id:Nexus 6"
    public const val NEXUS_9: String = "id:Nexus 9"
    public const val NEXUS_10: String = "name:Nexus 10"
    public const val NEXUS_5X: String = "id:Nexus 5X"
    public const val NEXUS_6P: String = "id:Nexus 6P"
    public const val PIXEL_C: String = "id:pixel_c"
    public const val PIXEL: String = "id:pixel"
    public const val PIXEL_XL: String = "id:pixel_xl"
    public const val PIXEL_2: String = "id:pixel_2"
    public const val PIXEL_2_XL: String = "id:pixel_2_xl"
    public const val PIXEL_3: String = "id:pixel_3"
    public const val PIXEL_3_XL: String = "id:pixel_3_xl"
    public const val PIXEL_3A: String = "id:pixel_3a"
    public const val PIXEL_3A_XL: String = "id:pixel_3a_xl"
    public const val PIXEL_4: String = "id:pixel_4"
    public const val PIXEL_4_XL: String = "id:pixel_4_xl"
    public const val PIXEL_4A: String = "id:pixel_4a"
    public const val PIXEL_5: String = "id:pixel_5"
    public const val PIXEL_6: String = "id:pixel_6"
    public const val PIXEL_6_PRO: String = "id:pixel_6_pro"
    public const val PIXEL_6A: String = "id:pixel_6a"
    public const val PIXEL_7: String = "id:pixel_7"
    public const val PIXEL_7_PRO: String = "id:pixel_7_pro"
    public const val PIXEL_7A: String = "id:pixel_7a"
    public const val PIXEL_8: String = "id:pixel_8"
    public const val PIXEL_8_PRO: String = "id:pixel_8_pro"
    public const val PIXEL_8A: String = "id:pixel_8a"
    public const val PIXEL_9: String = "id:pixel_9"
    public const val PIXEL_9_PRO: String = "id:pixel_9_pro"
    public const val PIXEL_9_PRO_FOLD: String = "id:pixel_9_pro_fold"
    public const val PIXEL_9_PRO_XL: String = "id:pixel_9_pro_xl"
    public const val PIXEL_FOLD: String = "id:pixel_fold"
    public const val PIXEL_TABLET: String = "id:pixel_tablet"

    public const val AUTOMOTIVE_1024p: String = "id:automotive_1024p_landscape"

    @Deprecated(
        "Use [androidx.wear.tooling.preview.devices.WearDevices.LARGE_ROUND] from the " +
            "wear:wear-tooling-preview library instead"
    )
    public const val WEAR_OS_LARGE_ROUND: String = "id:wearos_large_round"
    @Deprecated(
        "Use [androidx.wear.tooling.preview.devices.WearDevices.SMALL_ROUND] from the " +
            "wear:wear-tooling-preview library instead"
    )
    public const val WEAR_OS_SMALL_ROUND: String = "id:wearos_small_round"
    @Deprecated(
        "Use [androidx.wear.tooling.preview.devices.WearDevices.SQUARE] from the " +
            "wear:wear-tooling-preview library instead"
    )
    public const val WEAR_OS_SQUARE: String = "id:wearos_square"
    @Deprecated(
        "Use [androidx.wear.tooling.preview.devices.WearDevices.RECT] from the " +
            "wear:wear-tooling-preview library instead"
    )
    public const val WEAR_OS_RECT: String = "id:wearos_rect"

    // Reference devices
    public const val PHONE: String = "spec:width=411dp,height=891dp"
    public const val FOLDABLE: String = "spec:width=673dp,height=841dp"
    public const val TABLET: String = "spec:width=1280dp,height=800dp,dpi=240"
    public const val DESKTOP: String = "spec:width=1920dp,height=1080dp,dpi=160"

    // TV devices (not adding 4K since it will be very heavy for preview)
    public const val TV_720p: String = "spec:width=1280dp,height=720dp"
    public const val TV_1080p: String = "spec:width=1920dp,height=1080dp"
}

/** Annotation for defining the [Preview] device to use. */
@Retention(AnnotationRetention.SOURCE)
@Suppress("DEPRECATION")
@StringDef(
    open = true,
    value =
        [
            Devices.DEFAULT,
            Devices.NEXUS_7,
            Devices.NEXUS_7_2013,
            Devices.NEXUS_5,
            Devices.NEXUS_6,
            Devices.NEXUS_9,
            Devices.NEXUS_10,
            Devices.NEXUS_5X,
            Devices.NEXUS_6P,
            Devices.PIXEL_C,
            Devices.PIXEL,
            Devices.PIXEL_XL,
            Devices.PIXEL_2,
            Devices.PIXEL_2_XL,
            Devices.PIXEL_3,
            Devices.PIXEL_3_XL,
            Devices.PIXEL_3A,
            Devices.PIXEL_3A_XL,
            Devices.PIXEL_4,
            Devices.PIXEL_4_XL,
            Devices.PIXEL_4A,
            Devices.PIXEL_5,
            Devices.PIXEL_6,
            Devices.PIXEL_6_PRO,
            Devices.PIXEL_6A,
            Devices.PIXEL_7,
            Devices.PIXEL_7_PRO,
            Devices.PIXEL_7A,
            Devices.PIXEL_8,
            Devices.PIXEL_8_PRO,
            Devices.PIXEL_8A,
            Devices.PIXEL_9,
            Devices.PIXEL_9_PRO,
            Devices.PIXEL_9_PRO_XL,
            Devices.PIXEL_9_PRO_FOLD,
            Devices.PIXEL_FOLD,
            Devices.PIXEL_TABLET,
            Devices.AUTOMOTIVE_1024p,
            Devices.WEAR_OS_LARGE_ROUND,
            Devices.WEAR_OS_SMALL_ROUND,
            Devices.WEAR_OS_SQUARE,
            Devices.WEAR_OS_RECT,
            Devices.PHONE,
            Devices.FOLDABLE,
            Devices.TABLET,
            Devices.DESKTOP,
            Devices.TV_720p,
            Devices.TV_1080p,
        ],
)
internal annotation class Device
