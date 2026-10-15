/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.remote.material3.util

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile

object TestProfiles {
    private val wearWidgetsSupportedOperations =
        setOf(
            Operations.ACCESSIBILITY_SEMANTICS,
            Operations.ANIMATED_FLOAT,
            Operations.ANIMATION_SPEC,
            Operations.ATTRIBUTE_COLOR,
            Operations.ATTRIBUTE_IMAGE,
            Operations.ATTRIBUTE_TEXT,
            Operations.ATTRIBUTE_TIME,
            Operations.BITMAP_TEXT_MEASURE,
            Operations.CANVAS_OPERATIONS,
            Operations.CLICK_AREA,
            Operations.CLIP_RECT,
            Operations.COLOR_CONSTANT,
            Operations.COLOR_EXPRESSIONS,
            Operations.COMPONENT_START,
            Operations.COMPONENT_VALUE,
            Operations.CONDITIONAL_OPERATIONS,
            Operations.CONTAINER_END,
            Operations.DATA_BITMAP,
            Operations.DATA_BOOLEAN,
            Operations.DATA_FLOAT,
            Operations.DATA_FONT,
            Operations.DATA_INT,
            Operations.DATA_LONG,
            Operations.DATA_MAP_LOOKUP,
            Operations.DATA_PATH,
            Operations.DATA_TEXT,
            Operations.DEBUG_MESSAGE,
            Operations.DRAW_ARC,
            Operations.DRAW_BITMAP,
            Operations.DRAW_BITMAP_INT,
            Operations.DRAW_BITMAP_SCALED,
            Operations.DRAW_BITMAP_TEXT_ANCHORED,
            Operations.DRAW_CIRCLE,
            Operations.DRAW_CONTENT,
            Operations.DRAW_LINE,
            Operations.DRAW_OVAL,
            Operations.DRAW_PATH,
            Operations.DRAW_RECT,
            Operations.DRAW_ROUND_RECT,
            Operations.DRAW_SECTOR,
            Operations.DRAW_TEXT_ANCHOR,
            Operations.DRAW_TEXT_ON_CIRCLE,
            Operations.DRAW_TEXT_RUN,
            Operations.DRAW_TWEEN_PATH,
            Operations.DYNAMIC_FLOAT_LIST,
            Operations.FLOAT_LIST,
            Operations.FUNCTION_CALL,
            Operations.FUNCTION_DEFINE,
            Operations.HAPTIC_FEEDBACK,
            Operations.HEADER,
            Operations.HOST_ACTION,
            Operations.HOST_METADATA_ACTION,
            Operations.HOST_NAMED_ACTION,
            Operations.ID_LIST,
            Operations.ID_LOOKUP,
            Operations.ID_MAP,
            Operations.IMPULSE_PROCESS,
            Operations.IMPULSE_START,
            Operations.INTEGER_EXPRESSION,
            Operations.LAYOUT_BOX,
            Operations.LAYOUT_CANVAS,
            Operations.LAYOUT_CANVAS_CONTENT,
            Operations.LAYOUT_COLUMN,
            Operations.LAYOUT_CONTENT,
            Operations.LAYOUT_FIT_BOX,
            Operations.LAYOUT_IMAGE,
            Operations.LAYOUT_ROOT,
            Operations.LAYOUT_ROW,
            Operations.LAYOUT_STATE,
            Operations.LAYOUT_TEXT,
            Operations.LOOP_START,
            Operations.MATRIX_CONSTANT,
            Operations.MATRIX_EXPRESSION,
            Operations.MATRIX_FROM_PATH,
            Operations.MATRIX_RESTORE,
            Operations.MATRIX_ROTATE,
            Operations.MATRIX_SAVE,
            Operations.MATRIX_SCALE,
            Operations.MATRIX_SKEW,
            Operations.MATRIX_TRANSLATE,
            Operations.MATRIX_VECTOR_MATH,
            Operations.MODIFIER_ALIGN_BY,
            Operations.MODIFIER_BACKGROUND,
            Operations.MODIFIER_BORDER,
            Operations.MODIFIER_CLICK,
            Operations.MODIFIER_CLIP_RECT,
            Operations.MODIFIER_DRAW_CONTENT,
            Operations.MODIFIER_GRAPHICS_LAYER,
            Operations.MODIFIER_HEIGHT,
            Operations.MODIFIER_HEIGHT_IN,
            Operations.MODIFIER_MARQUEE,
            Operations.MODIFIER_OFFSET,
            Operations.MODIFIER_PADDING,
            Operations.MODIFIER_RIPPLE,
            Operations.MODIFIER_ROUNDED_CLIP_RECT,
            Operations.MODIFIER_VISIBILITY,
            Operations.MODIFIER_WIDTH,
            Operations.MODIFIER_WIDTH_IN,
            Operations.MODIFIER_ZINDEX,
            Operations.NAMED_VARIABLE,
            Operations.PAINT_VALUES,
            Operations.PATH_ADD,
            Operations.PATH_COMBINE,
            Operations.PATH_CREATE,
            Operations.PATH_EXPRESSION,
            Operations.PATH_TWEEN,
            Operations.REM,
            Operations.RUN_ACTION,
            Operations.TEXT_FROM_FLOAT,
            Operations.TEXT_LENGTH,
            Operations.TEXT_LOOKUP,
            Operations.TEXT_LOOKUP_INT,
            Operations.TEXT_MEASURE,
            Operations.TEXT_MERGE,
            Operations.TEXT_SUBTEXT,
            Operations.TEXT_TRANSFORM,
            Operations.UPDATE_DYNAMIC_FLOAT_LIST,
            Operations.VALUE_FLOAT_CHANGE_ACTION,
            Operations.VALUE_FLOAT_EXPRESSION_CHANGE_ACTION,
            Operations.VALUE_INTEGER_CHANGE_ACTION,
            Operations.VALUE_INTEGER_EXPRESSION_CHANGE_ACTION,
            Operations.VALUE_STRING_CHANGE_ACTION,
        )

    val wearWidgets =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_WEAR_WIDGETS,
            AndroidxRcPlatformServices(),
            { wearWidgetsSupportedOperations },
        ) { creationDisplayInfo, profile, callback ->
            RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
        }

    val androidNativeProfile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROID_NATIVE,
            AndroidxRcPlatformServices(),
            {
                Operations.getOperations(
                        CoreDocument.DOCUMENT_API_LEVEL,
                        RcProfiles.PROFILE_ANDROIDX,
                    )
                    ?.keySet()
                    .orEmpty() + setOf(Operations.DRAW_TEXT_ON_CIRCLE)
            },
        ) { creationDisplayInfo, profile, callback ->
            RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
        }

    val androidXWithCoreText =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            AndroidxRcPlatformServices(),
            {
                Operations.getOperations(
                        CoreDocument.DOCUMENT_API_LEVEL,
                        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                    )
                    ?.keySet()
                    .orEmpty() + setOf(Operations.CORE_TEXT, Operations.DRAW_TEXT_ON_CIRCLE)
            },
        ) { creationDisplayInfo, profile, callback ->
            RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
        }

    val wearWidgetsWithCoreText =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            AndroidxRcPlatformServices(),
            { wearWidgets.supportedOperations + setOf(Operations.CORE_TEXT) },
        ) { creationDisplayInfo, profile, callback ->
            RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
        }
}
