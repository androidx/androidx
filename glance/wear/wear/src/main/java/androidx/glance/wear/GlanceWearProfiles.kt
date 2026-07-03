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

package androidx.glance.wear

import androidx.annotation.RestrictTo
import androidx.collection.IntSet
import androidx.collection.buildIntSet
import androidx.collection.intSetOf
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations.ACCESSIBILITY_SEMANTICS
import androidx.compose.remote.core.Operations.ANIMATED_FLOAT
import androidx.compose.remote.core.Operations.ANIMATION_SPEC
import androidx.compose.remote.core.Operations.ATTRIBUTE_COLOR
import androidx.compose.remote.core.Operations.ATTRIBUTE_IMAGE
import androidx.compose.remote.core.Operations.ATTRIBUTE_TEXT
import androidx.compose.remote.core.Operations.ATTRIBUTE_TIME
import androidx.compose.remote.core.Operations.BITMAP_TEXT_MEASURE
import androidx.compose.remote.core.Operations.CANVAS_OPERATIONS
import androidx.compose.remote.core.Operations.CLICK_AREA
import androidx.compose.remote.core.Operations.CLIP_RECT
import androidx.compose.remote.core.Operations.COLOR_CONSTANT
import androidx.compose.remote.core.Operations.COLOR_EXPRESSIONS
import androidx.compose.remote.core.Operations.COMPONENT_START
import androidx.compose.remote.core.Operations.COMPONENT_VALUE
import androidx.compose.remote.core.Operations.CONDITIONAL_OPERATIONS
import androidx.compose.remote.core.Operations.CONTAINER_END
import androidx.compose.remote.core.Operations.DATA_BITMAP
import androidx.compose.remote.core.Operations.DATA_BOOLEAN
import androidx.compose.remote.core.Operations.DATA_FLOAT
import androidx.compose.remote.core.Operations.DATA_FONT
import androidx.compose.remote.core.Operations.DATA_INT
import androidx.compose.remote.core.Operations.DATA_LONG
import androidx.compose.remote.core.Operations.DATA_MAP_LOOKUP
import androidx.compose.remote.core.Operations.DATA_PATH
import androidx.compose.remote.core.Operations.DATA_TEXT
import androidx.compose.remote.core.Operations.DEBUG_MESSAGE
import androidx.compose.remote.core.Operations.DRAW_ARC
import androidx.compose.remote.core.Operations.DRAW_BITMAP
import androidx.compose.remote.core.Operations.DRAW_BITMAP_INT
import androidx.compose.remote.core.Operations.DRAW_BITMAP_SCALED
import androidx.compose.remote.core.Operations.DRAW_BITMAP_TEXT_ANCHORED
import androidx.compose.remote.core.Operations.DRAW_CIRCLE
import androidx.compose.remote.core.Operations.DRAW_CONTENT
import androidx.compose.remote.core.Operations.DRAW_LINE
import androidx.compose.remote.core.Operations.DRAW_OVAL
import androidx.compose.remote.core.Operations.DRAW_PATH
import androidx.compose.remote.core.Operations.DRAW_RECT
import androidx.compose.remote.core.Operations.DRAW_ROUND_RECT
import androidx.compose.remote.core.Operations.DRAW_SECTOR
import androidx.compose.remote.core.Operations.DRAW_TEXT_ANCHOR
import androidx.compose.remote.core.Operations.DRAW_TEXT_ON_CIRCLE
import androidx.compose.remote.core.Operations.DRAW_TEXT_RUN
import androidx.compose.remote.core.Operations.DRAW_TWEEN_PATH
import androidx.compose.remote.core.Operations.DYNAMIC_FLOAT_LIST
import androidx.compose.remote.core.Operations.FLOAT_LIST
import androidx.compose.remote.core.Operations.FUNCTION_CALL
import androidx.compose.remote.core.Operations.FUNCTION_DEFINE
import androidx.compose.remote.core.Operations.HAPTIC_FEEDBACK
import androidx.compose.remote.core.Operations.HEADER
import androidx.compose.remote.core.Operations.HOST_ACTION
import androidx.compose.remote.core.Operations.HOST_METADATA_ACTION
import androidx.compose.remote.core.Operations.HOST_NAMED_ACTION
import androidx.compose.remote.core.Operations.ID_LIST
import androidx.compose.remote.core.Operations.ID_LOOKUP
import androidx.compose.remote.core.Operations.ID_MAP
import androidx.compose.remote.core.Operations.IMPULSE_PROCESS
import androidx.compose.remote.core.Operations.IMPULSE_START
import androidx.compose.remote.core.Operations.INTEGER_EXPRESSION
import androidx.compose.remote.core.Operations.LAYOUT_BOX
import androidx.compose.remote.core.Operations.LAYOUT_CANVAS
import androidx.compose.remote.core.Operations.LAYOUT_CANVAS_CONTENT
import androidx.compose.remote.core.Operations.LAYOUT_COLUMN
import androidx.compose.remote.core.Operations.LAYOUT_CONTENT
import androidx.compose.remote.core.Operations.LAYOUT_FIT_BOX
import androidx.compose.remote.core.Operations.LAYOUT_IMAGE
import androidx.compose.remote.core.Operations.LAYOUT_ROOT
import androidx.compose.remote.core.Operations.LAYOUT_ROW
import androidx.compose.remote.core.Operations.LAYOUT_STATE
import androidx.compose.remote.core.Operations.LAYOUT_TEXT
import androidx.compose.remote.core.Operations.LOOP_START
import androidx.compose.remote.core.Operations.MATRIX_CONSTANT
import androidx.compose.remote.core.Operations.MATRIX_EXPRESSION
import androidx.compose.remote.core.Operations.MATRIX_FROM_PATH
import androidx.compose.remote.core.Operations.MATRIX_RESTORE
import androidx.compose.remote.core.Operations.MATRIX_ROTATE
import androidx.compose.remote.core.Operations.MATRIX_SAVE
import androidx.compose.remote.core.Operations.MATRIX_SCALE
import androidx.compose.remote.core.Operations.MATRIX_SKEW
import androidx.compose.remote.core.Operations.MATRIX_TRANSLATE
import androidx.compose.remote.core.Operations.MATRIX_VECTOR_MATH
import androidx.compose.remote.core.Operations.MODIFIER_ALIGN_BY
import androidx.compose.remote.core.Operations.MODIFIER_BACKGROUND
import androidx.compose.remote.core.Operations.MODIFIER_BORDER
import androidx.compose.remote.core.Operations.MODIFIER_CLICK
import androidx.compose.remote.core.Operations.MODIFIER_CLIP_RECT
import androidx.compose.remote.core.Operations.MODIFIER_DRAW_CONTENT
import androidx.compose.remote.core.Operations.MODIFIER_GRAPHICS_LAYER
import androidx.compose.remote.core.Operations.MODIFIER_HEIGHT
import androidx.compose.remote.core.Operations.MODIFIER_HEIGHT_IN
import androidx.compose.remote.core.Operations.MODIFIER_MARQUEE
import androidx.compose.remote.core.Operations.MODIFIER_OFFSET
import androidx.compose.remote.core.Operations.MODIFIER_PADDING
import androidx.compose.remote.core.Operations.MODIFIER_RIPPLE
import androidx.compose.remote.core.Operations.MODIFIER_ROUNDED_CLIP_RECT
import androidx.compose.remote.core.Operations.MODIFIER_VISIBILITY
import androidx.compose.remote.core.Operations.MODIFIER_WIDTH
import androidx.compose.remote.core.Operations.MODIFIER_WIDTH_IN
import androidx.compose.remote.core.Operations.MODIFIER_ZINDEX
import androidx.compose.remote.core.Operations.NAMED_VARIABLE
import androidx.compose.remote.core.Operations.PAINT_VALUES
import androidx.compose.remote.core.Operations.PATH_ADD
import androidx.compose.remote.core.Operations.PATH_COMBINE
import androidx.compose.remote.core.Operations.PATH_CREATE
import androidx.compose.remote.core.Operations.PATH_EXPRESSION
import androidx.compose.remote.core.Operations.PATH_TWEEN
import androidx.compose.remote.core.Operations.REM
import androidx.compose.remote.core.Operations.RUN_ACTION
import androidx.compose.remote.core.Operations.TEXT_FROM_FLOAT
import androidx.compose.remote.core.Operations.TEXT_LENGTH
import androidx.compose.remote.core.Operations.TEXT_LOOKUP
import androidx.compose.remote.core.Operations.TEXT_LOOKUP_INT
import androidx.compose.remote.core.Operations.TEXT_MEASURE
import androidx.compose.remote.core.Operations.TEXT_MERGE
import androidx.compose.remote.core.Operations.TEXT_SUBTEXT
import androidx.compose.remote.core.Operations.TEXT_TRANSFORM
import androidx.compose.remote.core.Operations.UPDATE_DYNAMIC_FLOAT_LIST
import androidx.compose.remote.core.Operations.VALUE_FLOAT_CHANGE_ACTION
import androidx.compose.remote.core.Operations.VALUE_FLOAT_EXPRESSION_CHANGE_ACTION
import androidx.compose.remote.core.Operations.VALUE_INTEGER_CHANGE_ACTION
import androidx.compose.remote.core.Operations.VALUE_INTEGER_EXPRESSION_CHANGE_ACTION
import androidx.compose.remote.core.Operations.VALUE_STRING_CHANGE_ACTION
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile

/** Defines profiles for Glance Wear. */
// TODO: b/526711189 - Make this public API once RC is public, currently this code is forked from
// RcPlatformProfiles.WEAR_WIDGETS.
@Suppress("RestrictedApiAndroidX")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object GlanceWearProfiles {
    /**
     * The set of operations that are allowed to be used on Wear Widgets. The actual support is
     * Host-dependent.
     */
    private val WEAR_WIDGETS_ALLOWED_OPERATIONS: IntSet =
        intSetOf(
            ACCESSIBILITY_SEMANTICS,
            ANIMATED_FLOAT,
            ANIMATION_SPEC,
            ATTRIBUTE_COLOR,
            ATTRIBUTE_IMAGE,
            ATTRIBUTE_TEXT,
            ATTRIBUTE_TIME,
            BITMAP_TEXT_MEASURE,
            CANVAS_OPERATIONS,
            CLICK_AREA,
            CLIP_RECT,
            COLOR_CONSTANT,
            COLOR_EXPRESSIONS,
            COMPONENT_START,
            COMPONENT_VALUE,
            CONDITIONAL_OPERATIONS,
            CONTAINER_END,
            DATA_BITMAP,
            DATA_BOOLEAN,
            DATA_FLOAT,
            DATA_FONT,
            DATA_INT,
            DATA_LONG,
            DATA_MAP_LOOKUP,
            DATA_PATH,
            DATA_TEXT,
            DEBUG_MESSAGE,
            DRAW_ARC,
            DRAW_BITMAP,
            DRAW_BITMAP_INT,
            DRAW_BITMAP_SCALED,
            DRAW_BITMAP_TEXT_ANCHORED,
            DRAW_CIRCLE,
            DRAW_CONTENT,
            DRAW_LINE,
            DRAW_OVAL,
            DRAW_PATH,
            DRAW_RECT,
            DRAW_ROUND_RECT,
            DRAW_SECTOR,
            DRAW_TEXT_ANCHOR,
            DRAW_TEXT_ON_CIRCLE,
            DRAW_TEXT_RUN,
            DRAW_TWEEN_PATH,
            DYNAMIC_FLOAT_LIST,
            FLOAT_LIST,
            FUNCTION_CALL,
            FUNCTION_DEFINE,
            HAPTIC_FEEDBACK,
            HEADER,
            HOST_ACTION,
            HOST_METADATA_ACTION,
            HOST_NAMED_ACTION,
            ID_LIST,
            ID_LOOKUP,
            ID_MAP,
            IMPULSE_PROCESS,
            IMPULSE_START,
            INTEGER_EXPRESSION,
            LAYOUT_BOX,
            LAYOUT_CANVAS,
            LAYOUT_CANVAS_CONTENT,
            LAYOUT_COLUMN,
            LAYOUT_CONTENT,
            LAYOUT_FIT_BOX,
            LAYOUT_IMAGE,
            LAYOUT_ROOT,
            LAYOUT_ROW,
            LAYOUT_STATE,
            LAYOUT_TEXT,
            LOOP_START,
            MATRIX_CONSTANT,
            MATRIX_EXPRESSION,
            MATRIX_FROM_PATH,
            MATRIX_RESTORE,
            MATRIX_ROTATE,
            MATRIX_SAVE,
            MATRIX_SCALE,
            MATRIX_SKEW,
            MATRIX_TRANSLATE,
            MATRIX_VECTOR_MATH,
            MODIFIER_ALIGN_BY,
            MODIFIER_BACKGROUND,
            MODIFIER_BORDER,
            MODIFIER_CLICK,
            MODIFIER_CLIP_RECT,
            MODIFIER_DRAW_CONTENT,
            MODIFIER_GRAPHICS_LAYER,
            MODIFIER_HEIGHT,
            MODIFIER_HEIGHT_IN,
            MODIFIER_MARQUEE,
            MODIFIER_OFFSET,
            MODIFIER_PADDING,
            MODIFIER_RIPPLE,
            MODIFIER_ROUNDED_CLIP_RECT,
            MODIFIER_VISIBILITY,
            MODIFIER_WIDTH,
            MODIFIER_WIDTH_IN,
            MODIFIER_ZINDEX,
            NAMED_VARIABLE,
            PAINT_VALUES,
            PATH_ADD,
            PATH_COMBINE,
            PATH_CREATE,
            PATH_EXPRESSION,
            PATH_TWEEN,
            REM,
            RUN_ACTION,
            TEXT_FROM_FLOAT,
            TEXT_LENGTH,
            TEXT_LOOKUP,
            TEXT_LOOKUP_INT,
            TEXT_MEASURE,
            TEXT_MERGE,
            TEXT_SUBTEXT,
            TEXT_TRANSFORM,
            UPDATE_DYNAMIC_FLOAT_LIST,
            VALUE_FLOAT_CHANGE_ACTION,
            VALUE_FLOAT_EXPRESSION_CHANGE_ACTION,
            VALUE_INTEGER_CHANGE_ACTION,
            VALUE_INTEGER_EXPRESSION_CHANGE_ACTION,
            VALUE_STRING_CHANGE_ACTION,
        )

    /**
     * Creates a Profile for Wear Widgets, based on the allowed operations for widgets using only
     * the supported Host operations from the list.
     *
     * If `supportedOperations` is not specified or null, all allowed operations are used.
     *
     * @param supportedOperations The set of operations that are supported by the host. If null, all
     *   allowed operations can be used.
     */
    @Suppress("PrimitiveInCollection")
    public fun wearWidgets(supportedOperations: IntSet? = null): Profile {
        val operationsToUse =
            supportedOperations?.let {
                buildIntSet {
                    supportedOperations.forEach {
                        if (WEAR_WIDGETS_ALLOWED_OPERATIONS.contains(it)) {
                            add(it)
                        }
                    }
                }
            } ?: WEAR_WIDGETS_ALLOWED_OPERATIONS
        return Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_WEAR_WIDGETS,
            AndroidxRcPlatformServices(),
            Profile.SupportedOperationsProvider {
                val set = mutableSetOf<Int>()
                operationsToUse.forEach { set.add(it) }
                set
            },
            { creationDisplayInfo, profile, callback ->
                RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
            },
        )
    }
}
