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

package androidx.compose.remote.creation.profile;

import static androidx.compose.remote.core.Operations.ACCESSIBILITY_SEMANTICS;
import static androidx.compose.remote.core.Operations.ANIMATED_FLOAT;
import static androidx.compose.remote.core.Operations.ANIMATION_SPEC;
import static androidx.compose.remote.core.Operations.ATTRIBUTE_COLOR;
import static androidx.compose.remote.core.Operations.ATTRIBUTE_IMAGE;
import static androidx.compose.remote.core.Operations.ATTRIBUTE_TEXT;
import static androidx.compose.remote.core.Operations.ATTRIBUTE_TIME;
import static androidx.compose.remote.core.Operations.BITMAP_TEXT_MEASURE;
import static androidx.compose.remote.core.Operations.CANVAS_OPERATIONS;
import static androidx.compose.remote.core.Operations.CLICK_AREA;
import static androidx.compose.remote.core.Operations.CLIP_RECT;
import static androidx.compose.remote.core.Operations.COLOR_CONSTANT;
import static androidx.compose.remote.core.Operations.COLOR_EXPRESSIONS;
import static androidx.compose.remote.core.Operations.COMPONENT_START;
import static androidx.compose.remote.core.Operations.COMPONENT_VALUE;
import static androidx.compose.remote.core.Operations.CONDITIONAL_OPERATIONS;
import static androidx.compose.remote.core.Operations.CONTAINER_END;
import static androidx.compose.remote.core.Operations.DATA_BITMAP;
import static androidx.compose.remote.core.Operations.DATA_BOOLEAN;
import static androidx.compose.remote.core.Operations.DATA_FLOAT;
import static androidx.compose.remote.core.Operations.DATA_FONT;
import static androidx.compose.remote.core.Operations.DATA_INT;
import static androidx.compose.remote.core.Operations.DATA_LONG;
import static androidx.compose.remote.core.Operations.DATA_MAP_LOOKUP;
import static androidx.compose.remote.core.Operations.DATA_PATH;
import static androidx.compose.remote.core.Operations.DATA_TEXT;
import static androidx.compose.remote.core.Operations.DEBUG_MESSAGE;
import static androidx.compose.remote.core.Operations.DRAW_ARC;
import static androidx.compose.remote.core.Operations.DRAW_BITMAP;
import static androidx.compose.remote.core.Operations.DRAW_BITMAP_INT;
import static androidx.compose.remote.core.Operations.DRAW_BITMAP_SCALED;
import static androidx.compose.remote.core.Operations.DRAW_BITMAP_TEXT_ANCHORED;
import static androidx.compose.remote.core.Operations.DRAW_CIRCLE;
import static androidx.compose.remote.core.Operations.DRAW_CONTENT;
import static androidx.compose.remote.core.Operations.DRAW_LINE;
import static androidx.compose.remote.core.Operations.DRAW_OVAL;
import static androidx.compose.remote.core.Operations.DRAW_PATH;
import static androidx.compose.remote.core.Operations.DRAW_RECT;
import static androidx.compose.remote.core.Operations.DRAW_ROUND_RECT;
import static androidx.compose.remote.core.Operations.DRAW_SECTOR;
import static androidx.compose.remote.core.Operations.DRAW_TEXT_ANCHOR;
import static androidx.compose.remote.core.Operations.DRAW_TEXT_ON_CIRCLE;
import static androidx.compose.remote.core.Operations.DRAW_TEXT_RUN;
import static androidx.compose.remote.core.Operations.DRAW_TWEEN_PATH;
import static androidx.compose.remote.core.Operations.DYNAMIC_FLOAT_LIST;
import static androidx.compose.remote.core.Operations.FLOAT_LIST;
import static androidx.compose.remote.core.Operations.FUNCTION_CALL;
import static androidx.compose.remote.core.Operations.FUNCTION_DEFINE;
import static androidx.compose.remote.core.Operations.HAPTIC_FEEDBACK;
import static androidx.compose.remote.core.Operations.HEADER;
import static androidx.compose.remote.core.Operations.HOST_ACTION;
import static androidx.compose.remote.core.Operations.HOST_METADATA_ACTION;
import static androidx.compose.remote.core.Operations.HOST_NAMED_ACTION;
import static androidx.compose.remote.core.Operations.ID_LIST;
import static androidx.compose.remote.core.Operations.ID_LOOKUP;
import static androidx.compose.remote.core.Operations.ID_MAP;
import static androidx.compose.remote.core.Operations.IMPULSE_PROCESS;
import static androidx.compose.remote.core.Operations.IMPULSE_START;
import static androidx.compose.remote.core.Operations.INTEGER_EXPRESSION;
import static androidx.compose.remote.core.Operations.LAYOUT_BOX;
import static androidx.compose.remote.core.Operations.LAYOUT_CANVAS;
import static androidx.compose.remote.core.Operations.LAYOUT_CANVAS_CONTENT;
import static androidx.compose.remote.core.Operations.LAYOUT_COLUMN;
import static androidx.compose.remote.core.Operations.LAYOUT_CONTENT;
import static androidx.compose.remote.core.Operations.LAYOUT_FIT_BOX;
import static androidx.compose.remote.core.Operations.LAYOUT_IMAGE;
import static androidx.compose.remote.core.Operations.LAYOUT_ROOT;
import static androidx.compose.remote.core.Operations.LAYOUT_ROW;
import static androidx.compose.remote.core.Operations.LAYOUT_STATE;
import static androidx.compose.remote.core.Operations.LAYOUT_TEXT;
import static androidx.compose.remote.core.Operations.LOOP_START;
import static androidx.compose.remote.core.Operations.MATRIX_CONSTANT;
import static androidx.compose.remote.core.Operations.MATRIX_EXPRESSION;
import static androidx.compose.remote.core.Operations.MATRIX_FROM_PATH;
import static androidx.compose.remote.core.Operations.MATRIX_RESTORE;
import static androidx.compose.remote.core.Operations.MATRIX_ROTATE;
import static androidx.compose.remote.core.Operations.MATRIX_SAVE;
import static androidx.compose.remote.core.Operations.MATRIX_SCALE;
import static androidx.compose.remote.core.Operations.MATRIX_SKEW;
import static androidx.compose.remote.core.Operations.MATRIX_TRANSLATE;
import static androidx.compose.remote.core.Operations.MATRIX_VECTOR_MATH;
import static androidx.compose.remote.core.Operations.MODIFIER_ALIGN_BY;
import static androidx.compose.remote.core.Operations.MODIFIER_BACKGROUND;
import static androidx.compose.remote.core.Operations.MODIFIER_BORDER;
import static androidx.compose.remote.core.Operations.MODIFIER_CLICK;
import static androidx.compose.remote.core.Operations.MODIFIER_CLIP_RECT;
import static androidx.compose.remote.core.Operations.MODIFIER_DRAW_CONTENT;
import static androidx.compose.remote.core.Operations.MODIFIER_GRAPHICS_LAYER;
import static androidx.compose.remote.core.Operations.MODIFIER_HEIGHT;
import static androidx.compose.remote.core.Operations.MODIFIER_HEIGHT_IN;
import static androidx.compose.remote.core.Operations.MODIFIER_MARQUEE;
import static androidx.compose.remote.core.Operations.MODIFIER_OFFSET;
import static androidx.compose.remote.core.Operations.MODIFIER_PADDING;
import static androidx.compose.remote.core.Operations.MODIFIER_RIPPLE;
import static androidx.compose.remote.core.Operations.MODIFIER_ROUNDED_CLIP_RECT;
import static androidx.compose.remote.core.Operations.MODIFIER_VISIBILITY;
import static androidx.compose.remote.core.Operations.MODIFIER_WIDTH;
import static androidx.compose.remote.core.Operations.MODIFIER_WIDTH_IN;
import static androidx.compose.remote.core.Operations.MODIFIER_ZINDEX;
import static androidx.compose.remote.core.Operations.NAMED_VARIABLE;
import static androidx.compose.remote.core.Operations.PAINT_VALUES;
import static androidx.compose.remote.core.Operations.PATH_ADD;
import static androidx.compose.remote.core.Operations.PATH_COMBINE;
import static androidx.compose.remote.core.Operations.PATH_CREATE;
import static androidx.compose.remote.core.Operations.PATH_EXPRESSION;
import static androidx.compose.remote.core.Operations.PATH_TWEEN;
import static androidx.compose.remote.core.Operations.REM;
import static androidx.compose.remote.core.Operations.RUN_ACTION;
import static androidx.compose.remote.core.Operations.TEXT_FROM_FLOAT;
import static androidx.compose.remote.core.Operations.TEXT_LENGTH;
import static androidx.compose.remote.core.Operations.TEXT_LOOKUP;
import static androidx.compose.remote.core.Operations.TEXT_LOOKUP_INT;
import static androidx.compose.remote.core.Operations.TEXT_MEASURE;
import static androidx.compose.remote.core.Operations.TEXT_MERGE;
import static androidx.compose.remote.core.Operations.TEXT_SUBTEXT;
import static androidx.compose.remote.core.Operations.TEXT_TRANSFORM;
import static androidx.compose.remote.core.Operations.UPDATE_DYNAMIC_FLOAT_LIST;
import static androidx.compose.remote.core.Operations.VALUE_FLOAT_CHANGE_ACTION;
import static androidx.compose.remote.core.Operations.VALUE_FLOAT_EXPRESSION_CHANGE_ACTION;
import static androidx.compose.remote.core.Operations.VALUE_INTEGER_CHANGE_ACTION;
import static androidx.compose.remote.core.Operations.VALUE_INTEGER_EXPRESSION_CHANGE_ACTION;
import static androidx.compose.remote.core.Operations.VALUE_STRING_CHANGE_ACTION;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RcProfiles;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RcPlatformProfiles {
    /**
     * Profile for Glance Widgets for Platform 16.
     * <p>
     * This will be moved to the glance module when creation APIs are public, before
     * stable APIs.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final @NonNull Profile WIDGETS_V6 =
            new Profile(6, 0, new AndroidxRcPlatformServices(),
                    (creationDisplayInfo, profile, callback) ->
                            new WidgetsProfileWriterV6(creationDisplayInfo, null, profile));

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final @NonNull Profile WIDGETS_V7 =
            new Profile(
                    7,
                    RcProfiles.PROFILE_WIDGETS,
                    new AndroidxRcPlatformServices(),
                    (creationDisplayInfo, profile, callback) ->
                            new RemoteComposeWriterAndroid(creationDisplayInfo, null, profile));


    /**
     * A profile for creating Remote Compose UIs for use with the embedded AndroidX Player.
     *
     * <p>It uses the {@link RemoteComposeWriterAndroid} to serialize the UI tree.
     */
    public static final @NonNull Profile ANDROIDX7 = new Profile(7,
            RcProfiles.PROFILE_ANDROIDX, new AndroidxRcPlatformServices(),
            (creationDisplayInfo, profile, callback) ->
                    new RemoteComposeWriterAndroid(
                            creationDisplayInfo, null, profile, callback));

    /**
     * A profile for creating Remote Compose UIs for use with the embedded AndroidX Player.
     *
     * <p>It uses the {@link RemoteComposeWriterAndroid} to serialize the UI tree.
     */
    public static final @NonNull Profile ANDROIDX8 = new Profile(8,
            RcProfiles.PROFILE_ANDROIDX, new AndroidxRcPlatformServices(),
            (creationDisplayInfo, profile, callback) ->
                    new RemoteComposeWriterAndroid(
                            creationDisplayInfo, null, profile, callback));

    /**
     * A profile for creating Remote Compose UIs for use with the embedded AndroidX Player.
     *
     * <p>It uses the {@link RemoteComposeWriterAndroid} to serialize the UI tree.
     */
    public static final @NonNull Profile ANDROIDX9 = new Profile(9,
            RcProfiles.PROFILE_ANDROIDX, new AndroidxRcPlatformServices(),
            (creationDisplayInfo, profile, callback) ->
                    new RemoteComposeWriterAndroid(
                            creationDisplayInfo, null, profile, callback));


    /**
     * A profile for creating Remote Compose UIs for use with the embedded AndroidX Player.
     *
     * <p>It uses the {@link RemoteComposeWriterAndroid} to serialize the UI tree.
     */
    public static final @NonNull Profile ANDROIDX = new Profile(CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX,
            new AndroidxRcPlatformServices(),
            (creationDisplayInfo, profile, callback) ->
                    new RemoteComposeWriterAndroid(
                            creationDisplayInfo, null, profile, callback));

    private static final @NonNull Set<Integer> WEAR_WIDGETS_SUPPORTED_OPERATIONS;

    static {
        List<Integer> tmp = Arrays.asList(
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
                // TODO(b/485509245) Temporarily out while implementation is worked on
//                CORE_TEXT,
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
                VALUE_STRING_CHANGE_ACTION
        );
        WEAR_WIDGETS_SUPPORTED_OPERATIONS = Collections.unmodifiableSet(new HashSet<>(tmp));
    }

    /**
     * Profile for Wear OS widgets.
     * <p>
     * This will be moved to the glance:wear:wear module when creation APIs are public, before
     * stable APIs.
     */
    public static final @NonNull Profile WEAR_WIDGETS = new Profile(CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_WEAR_WIDGETS, new AndroidxRcPlatformServices(),
            () -> WEAR_WIDGETS_SUPPORTED_OPERATIONS,
            (creationDisplayInfo, profile, callback) ->
                    new RemoteComposeWriterAndroid(
                            creationDisplayInfo, null, profile, callback));
}
