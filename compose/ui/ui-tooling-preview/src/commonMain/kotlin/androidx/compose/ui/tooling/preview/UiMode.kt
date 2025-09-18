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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

internal expect fun validateUiModes()

/** List of ui modes available to be used in the preview. */
object UiModes {

    init {
        validateUiModes()
    }

    /** Bits that encode the mode type. */
    const val UI_MODE_TYPE_MASK: Int = 0x0f

    /** [UI_MODE_TYPE_MASK] value indicating that no mode type has been set. */
    const val UI_MODE_TYPE_UNDEFINED: Int = 0x00

    /**
     * [UI_MODE_TYPE_MASK] value that corresponds to
     * [no UI mode]({@docRoot}guide/topics/resources/providing-resources.html#UiModeQualifier)
     * resource qualifier specified.
     */
    const val UI_MODE_TYPE_NORMAL: Int = 0x01

    /**
     * [UI_MODE_TYPE_MASK] value that corresponds to the
     * [desk]({@docRoot}guide/topics/resources/providing-resources.html#UiModeQualifier) resource
     * qualifier.
     */
    const val UI_MODE_TYPE_DESK: Int = 0x02

    /**
     * [UI_MODE_TYPE_MASK] value that corresponds to the
     * [car]({@docRoot}guide/topics/resources/providing-resources.html#UiModeQualifier) resource
     * qualifier.
     */
    const val UI_MODE_TYPE_CAR: Int = 0x03

    /**
     * [UI_MODE_TYPE_MASK] value that corresponds to the
     * [television]({@docRoot}guide/topics/resources/providing-resources.html#UiModeQualifier)
     * resource qualifier.
     */
    const val UI_MODE_TYPE_TELEVISION: Int = 0x04

    /**
     * [UI_MODE_TYPE_MASK] value that corresponds to the
     * [appliance]({@docRoot}guide/topics/resources/providing-resources.html#UiModeQualifier)
     * resource qualifier.
     */
    const val UI_MODE_TYPE_APPLIANCE: Int = 0x05

    /**
     * [UI_MODE_TYPE_MASK] value that corresponds to the
     * [watch]({@docRoot}guide/topics/resources/providing-resources.html#UiModeQualifier) resource
     * qualifier.
     */
    const val UI_MODE_TYPE_WATCH: Int = 0x06

    /**
     * [UI_MODE_TYPE_MASK] value that corresponds to the
     * [vrheadset]({@docRoot}guide/topics/resources/providing-resources.html#UiModeQualifier)
     * resource qualifier.
     */
    const val UI_MODE_TYPE_VR_HEADSET: Int = 0x07

    /** Bits that encode the night mode. */
    const val UI_MODE_NIGHT_MASK: Int = 0x30

    /** [UI_MODE_NIGHT_MASK] value indicating that no mode type has been set. */
    const val UI_MODE_NIGHT_UNDEFINED: Int = 0x00

    /**
     * [UI_MODE_NIGHT_MASK] value that corresponds to the
     * [notnight]({@docRoot}guide/topics/resources/providing-resources.html#NightQualifier) resource
     * qualifier.
     */
    const val UI_MODE_NIGHT_NO: Int = 0x10

    /**
     * [UI_MODE_NIGHT_MASK] value that corresponds to the
     * [night]({@docRoot}guide/topics/resources/providing-resources.html#NightQualifier) resource
     * qualifier.
     */
    const val UI_MODE_NIGHT_YES: Int = 0x20
}

/** Annotation of setting uiMode in [Preview]. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("UniqueConstants") // UI_MODE_NIGHT_UNDEFINED == UI_MODE_TYPE_UNDEFINED
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    value =
        [
            UiModes.UI_MODE_TYPE_MASK,
            UiModes.UI_MODE_TYPE_UNDEFINED,
            UiModes.UI_MODE_TYPE_APPLIANCE,
            UiModes.UI_MODE_TYPE_CAR,
            UiModes.UI_MODE_TYPE_DESK,
            UiModes.UI_MODE_TYPE_NORMAL,
            UiModes.UI_MODE_TYPE_TELEVISION,
            UiModes.UI_MODE_TYPE_VR_HEADSET,
            UiModes.UI_MODE_TYPE_WATCH,
            UiModes.UI_MODE_NIGHT_MASK,
            UiModes.UI_MODE_NIGHT_UNDEFINED,
            UiModes.UI_MODE_NIGHT_NO,
            UiModes.UI_MODE_NIGHT_YES,
        ]
)
annotation class UiMode
