/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.BitmapData;
import androidx.compose.remote.core.operations.BitmapFontData;
import androidx.compose.remote.core.operations.BitmapTextMeasure;
import androidx.compose.remote.core.operations.ClickArea;
import androidx.compose.remote.core.operations.ClipPath;
import androidx.compose.remote.core.operations.ClipRect;
import androidx.compose.remote.core.operations.ColorAttribute;
import androidx.compose.remote.core.operations.ColorConstant;
import androidx.compose.remote.core.operations.ColorExpression;
import androidx.compose.remote.core.operations.ComponentValue;
import androidx.compose.remote.core.operations.ConditionalOperations;
import androidx.compose.remote.core.operations.DataListFloat;
import androidx.compose.remote.core.operations.DataListIds;
import androidx.compose.remote.core.operations.DataMapIds;
import androidx.compose.remote.core.operations.DataMapLookup;
import androidx.compose.remote.core.operations.DebugMessage;
import androidx.compose.remote.core.operations.DrawArc;
import androidx.compose.remote.core.operations.DrawBitmap;
import androidx.compose.remote.core.operations.DrawBitmapFontText;
import androidx.compose.remote.core.operations.DrawBitmapFontTextOnPath;
import androidx.compose.remote.core.operations.DrawBitmapInt;
import androidx.compose.remote.core.operations.DrawBitmapScaled;
import androidx.compose.remote.core.operations.DrawBitmapTextAnchored;
import androidx.compose.remote.core.operations.DrawCircle;
import androidx.compose.remote.core.operations.DrawContent;
import androidx.compose.remote.core.operations.DrawLine;
import androidx.compose.remote.core.operations.DrawOval;
import androidx.compose.remote.core.operations.DrawPath;
import androidx.compose.remote.core.operations.DrawRect;
import androidx.compose.remote.core.operations.DrawRoundRect;
import androidx.compose.remote.core.operations.DrawSector;
import androidx.compose.remote.core.operations.DrawText;
import androidx.compose.remote.core.operations.DrawTextAnchored;
import androidx.compose.remote.core.operations.DrawTextOnPath;
import androidx.compose.remote.core.operations.DrawToBitmap;
import androidx.compose.remote.core.operations.DrawTweenPath;
import androidx.compose.remote.core.operations.FloatConstant;
import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.FloatFunctionCall;
import androidx.compose.remote.core.operations.FloatFunctionDefine;
import androidx.compose.remote.core.operations.FontData;
import androidx.compose.remote.core.operations.HapticFeedback;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.IdLookup;
import androidx.compose.remote.core.operations.ImageAttribute;
import androidx.compose.remote.core.operations.IntegerExpression;
import androidx.compose.remote.core.operations.MatrixFromPath;
import androidx.compose.remote.core.operations.MatrixRestore;
import androidx.compose.remote.core.operations.MatrixRotate;
import androidx.compose.remote.core.operations.MatrixSave;
import androidx.compose.remote.core.operations.MatrixScale;
import androidx.compose.remote.core.operations.MatrixSkew;
import androidx.compose.remote.core.operations.MatrixTranslate;
import androidx.compose.remote.core.operations.NamedVariable;
import androidx.compose.remote.core.operations.PaintData;
import androidx.compose.remote.core.operations.ParticlesCreate;
import androidx.compose.remote.core.operations.ParticlesLoop;
import androidx.compose.remote.core.operations.PathAppend;
import androidx.compose.remote.core.operations.PathCombine;
import androidx.compose.remote.core.operations.PathCreate;
import androidx.compose.remote.core.operations.PathData;
import androidx.compose.remote.core.operations.PathExpression;
import androidx.compose.remote.core.operations.PathTween;
import androidx.compose.remote.core.operations.Rem;
import androidx.compose.remote.core.operations.RootContentBehavior;
import androidx.compose.remote.core.operations.RootContentDescription;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.TextAttribute;
import androidx.compose.remote.core.operations.TextData;
import androidx.compose.remote.core.operations.TextFromFloat;
import androidx.compose.remote.core.operations.TextLength;
import androidx.compose.remote.core.operations.TextLookup;
import androidx.compose.remote.core.operations.TextLookupInt;
import androidx.compose.remote.core.operations.TextMeasure;
import androidx.compose.remote.core.operations.TextMerge;
import androidx.compose.remote.core.operations.TextSubtext;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.TimeAttribute;
import androidx.compose.remote.core.operations.TouchExpression;
import androidx.compose.remote.core.operations.WakeIn;
import androidx.compose.remote.core.operations.layout.CanvasContent;
import androidx.compose.remote.core.operations.layout.CanvasOperations;
import androidx.compose.remote.core.operations.layout.ClickModifierOperation;
import androidx.compose.remote.core.operations.layout.ComponentStart;
import androidx.compose.remote.core.operations.layout.ContainerEnd;
import androidx.compose.remote.core.operations.layout.ImpulseOperation;
import androidx.compose.remote.core.operations.layout.ImpulseProcess;
import androidx.compose.remote.core.operations.layout.LayoutComponentContent;
import androidx.compose.remote.core.operations.layout.LoopOperation;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.layout.TouchCancelModifierOperation;
import androidx.compose.remote.core.operations.layout.TouchDownModifierOperation;
import androidx.compose.remote.core.operations.layout.TouchUpModifierOperation;
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout;
import androidx.compose.remote.core.operations.layout.managers.CollapsibleColumnLayout;
import androidx.compose.remote.core.operations.layout.managers.CollapsibleRowLayout;
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout;
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout;
import androidx.compose.remote.core.operations.layout.managers.ImageLayout;
import androidx.compose.remote.core.operations.layout.managers.RowLayout;
import androidx.compose.remote.core.operations.layout.managers.StateLayout;
import androidx.compose.remote.core.operations.layout.managers.TextLayout;
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ClipRectModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation;
import androidx.compose.remote.core.operations.layout.modifiers.DrawContentOperation;
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HostActionMetadataOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HostActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.OffsetModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.RippleModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.RoundedClipRectModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.RunActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatExpressionChangeActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerChangeActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerExpressionChangeActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ValueStringChangeActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ZIndexModifierOperation;
import androidx.compose.remote.core.operations.matrix.MatrixConstant;
import androidx.compose.remote.core.operations.matrix.MatrixExpression;
import androidx.compose.remote.core.operations.matrix.MatrixVectorMath;
import androidx.compose.remote.core.operations.utilities.IntMap;
import androidx.compose.remote.core.semantics.CoreSemantics;
import androidx.compose.remote.core.types.BooleanConstant;
import androidx.compose.remote.core.types.IntegerConstant;
import androidx.compose.remote.core.types.LongConstant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

/** List of operations supported in a RemoteCompose document */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Operations {

    private Operations() {}

    ////////////////////////////////////////
    // Protocol
    ////////////////////////////////////////
    public static final int HEADER = 0;
    public static final int LOAD_BITMAP = 4;
    public static final int THEME = 63;
    public static final int CLICK_AREA = 64;
    public static final int ROOT_CONTENT_BEHAVIOR = 65;
    public static final int ROOT_CONTENT_DESCRIPTION = 103;
    // TODO reorder before submitting
    public static final int ACCESSIBILITY_SEMANTICS = 250;

    // Reserve some opcodes for future extension
    public static final int EXTENDED_OPCODE = 255;
    public static final int EXTENSION_RANGE_RESERVED_1 = 254;
    public static final int EXTENSION_RANGE_RESERVED_2 = 253;
    public static final int EXTENSION_RANGE_RESERVED_3 = 252;
    public static final int EXTENSION_RANGE_RESERVED_4 = 251;

    ////////////////////////////////////////
    // Draw commands
    ////////////////////////////////////////
    public static final int DRAW_BITMAP = 44;
    public static final int DRAW_BITMAP_INT = 66;
    public static final int DATA_BITMAP = 101;
    public static final int DATA_SHADER = 45;
    public static final int DATA_TEXT = 102;
    public static final int DATA_BITMAP_FONT = 167;

    ///////////////////////////// =====================
    public static final int CLIP_PATH = 38;
    public static final int CLIP_RECT = 39;
    public static final int PAINT_VALUES = 40;
    public static final int DRAW_RECT = 42;
    public static final int DRAW_BITMAP_FONT_TEXT_RUN = 48;
    public static final int DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH = 49;
    public static final int DRAW_TEXT_RUN = 43;
    public static final int DRAW_CIRCLE = 46;
    public static final int DRAW_LINE = 47;
    public static final int DRAW_ROUND_RECT = 51;
    public static final int DRAW_SECTOR = 52;
    public static final int DRAW_TEXT_ON_PATH = 53;
    public static final int DRAW_OVAL = 56;
    public static final int DATA_PATH = 123;
    public static final int DRAW_PATH = 124;
    public static final int DRAW_TWEEN_PATH = 125;
    public static final int DRAW_CONTENT = 139;
    public static final int MATRIX_SCALE = 126;
    public static final int MATRIX_TRANSLATE = 127;
    public static final int MATRIX_SKEW = 128;
    public static final int MATRIX_ROTATE = 129;
    public static final int MATRIX_SAVE = 130;
    public static final int MATRIX_RESTORE = 131;
    public static final int MATRIX_SET = 132;
    public static final int DATA_FLOAT = 80;
    public static final int ANIMATED_FLOAT = 81;
    public static final int DRAW_TEXT_ANCHOR = 133;
    public static final int COLOR_EXPRESSIONS = 134;
    public static final int TEXT_FROM_FLOAT = 135;
    public static final int TEXT_MERGE = 136;
    public static final int NAMED_VARIABLE = 137;
    public static final int COLOR_CONSTANT = 138;
    public static final int DATA_INT = 140;
    public static final int DATA_BOOLEAN = 143;
    public static final int INTEGER_EXPRESSION = 144;
    public static final int ID_MAP = 145;
    public static final int ID_LIST = 146;
    public static final int FLOAT_LIST = 147;
    public static final int DATA_LONG = 148;
    public static final int DRAW_BITMAP_SCALED = 149;
    public static final int TEXT_LOOKUP = 151;
    public static final int DRAW_ARC = 152;
    public static final int TEXT_LOOKUP_INT = 153;
    public static final int DATA_MAP_LOOKUP = 154;
    public static final int TEXT_MEASURE = 155;
    public static final int TEXT_LENGTH = 156;
    public static final int TOUCH_EXPRESSION = 157;
    public static final int PATH_TWEEN = 158;
    public static final int PATH_CREATE = 159;
    public static final int PATH_ADD = 160;
    public static final int PARTICLE_DEFINE = 161;
    public static final int PARTICLE_PROCESS = 162;
    public static final int PARTICLE_LOOP = 163;
    public static final int IMPULSE_START = 164;
    public static final int IMPULSE_PROCESS = 165;
    public static final int FUNCTION_CALL = 166;
    public static final int FUNCTION_DEFINE = 168;
    public static final int ATTRIBUTE_TEXT = 170;
    public static final int ATTRIBUTE_IMAGE = 171;
    public static final int ATTRIBUTE_TIME = 172;
    public static final int CANVAS_OPERATIONS = 173;
    public static final int MODIFIER_DRAW_CONTENT = 174;
    public static final int PATH_COMBINE = 175;
    public static final int HAPTIC_FEEDBACK = 177;
    public static final int CONDITIONAL_OPERATIONS = 178;
    public static final int DEBUG_MESSAGE = 179;
    public static final int ATTRIBUTE_COLOR = 180;
    public static final int MATRIX_FROM_PATH = 181;
    public static final int TEXT_SUBTEXT = 182;
    public static final int BITMAP_TEXT_MEASURE = 183;
    public static final int DRAW_BITMAP_TEXT_ANCHORED = 184;
    public static final int REM = 185;
    public static final int MATRIX_CONSTANT = 186;
    public static final int MATRIX_EXPRESSION = 187;
    public static final int MATRIX_VECTOR_MATH = 188;
    public static final int DATA_FONT = 189;
    public static final int DRAW_TO_BITMAP = 190;
    public static final int WAKE_IN = 191;
    public static final int ID_LOOKUP = 192;
    public static final int PATH_EXPRESSION = 193;

    ///////////////////////////////////////// ======================

    ////////////////////////////////////////
    // Layout commands
    ////////////////////////////////////////

    public static final int LAYOUT_ROOT = 200;
    public static final int LAYOUT_CONTENT = 201;
    public static final int LAYOUT_BOX = 202;
    public static final int LAYOUT_FIT_BOX = 176;
    public static final int LAYOUT_ROW = 203;
    public static final int LAYOUT_COLLAPSIBLE_ROW = 230;
    public static final int LAYOUT_COLUMN = 204;
    public static final int LAYOUT_COLLAPSIBLE_COLUMN = 233;
    public static final int LAYOUT_CANVAS = 205;
    public static final int LAYOUT_CANVAS_CONTENT = 207;
    public static final int LAYOUT_TEXT = 208;
    public static final int LAYOUT_STATE = 217;
    public static final int LAYOUT_IMAGE = 234;

    public static final int COMPONENT_START = 2;

    public static final int MODIFIER_WIDTH = 16;
    public static final int MODIFIER_HEIGHT = 67;
    public static final int MODIFIER_WIDTH_IN = 231;
    public static final int MODIFIER_HEIGHT_IN = 232;
    public static final int MODIFIER_COLLAPSIBLE_PRIORITY = 235;
    public static final int MODIFIER_BACKGROUND = 55;
    public static final int MODIFIER_BORDER = 107;
    public static final int MODIFIER_PADDING = 58;
    public static final int MODIFIER_CLIP_RECT = 108;
    public static final int MODIFIER_ROUNDED_CLIP_RECT = 54;

    public static final int MODIFIER_CLICK = 59;
    public static final int MODIFIER_TOUCH_DOWN = 219;
    public static final int MODIFIER_TOUCH_UP = 220;
    public static final int MODIFIER_TOUCH_CANCEL = 225;

    public static final int CONTAINER_END = 214;

    public static final int MODIFIER_OFFSET = 221;
    public static final int MODIFIER_ZINDEX = 223;
    public static final int MODIFIER_GRAPHICS_LAYER = 224;
    public static final int MODIFIER_SCROLL = 226;
    public static final int MODIFIER_MARQUEE = 228;
    public static final int MODIFIER_RIPPLE = 229;

    public static final int LOOP_START = 215;

    public static final int MODIFIER_VISIBILITY = 211;
    public static final int HOST_ACTION = 209;
    public static final int HOST_METADATA_ACTION = 216;
    public static final int HOST_NAMED_ACTION = 210;
    public static final int RUN_ACTION = 236;

    public static final int VALUE_INTEGER_CHANGE_ACTION = 212;
    public static final int VALUE_STRING_CHANGE_ACTION = 213;
    public static final int VALUE_INTEGER_EXPRESSION_CHANGE_ACTION = 218;
    public static final int VALUE_FLOAT_CHANGE_ACTION = 222;
    public static final int VALUE_FLOAT_EXPRESSION_CHANGE_ACTION = 227;

    public static final int ANIMATION_SPEC = 14;

    public static final int COMPONENT_VALUE = 150;

    ////////////////////////////////////////
    // Profiles management
    ////////////////////////////////////////

    static UniqueIntMap<CompanionOperation> sMapV6;
    static HashMap<Integer, UniqueIntMap<CompanionOperation>> sMapV7;

    static UniqueIntMap<CompanionOperation> sMapV7AndroidX;
    static UniqueIntMap<CompanionOperation> sMapV7AndroidXExperimental;
    static UniqueIntMap<CompanionOperation> sMapV7AndroidXDeprecated;

    static UniqueIntMap<CompanionOperation> sMapV7Widgets;
    static UniqueIntMap<CompanionOperation> sMapV7WidgetsExperimental;
    static UniqueIntMap<CompanionOperation> sMapV7WidgetsDeprecated;

    ////////////////////////////////////////
    // Available profiles
    ////////////////////////////////////////

    public static final int PROFILE_BASELINE = 0x0;

    // Additive profiles
    public static final int PROFILE_EXPERIMENTAL = 0x1;
    public static final int PROFILE_DEPRECATED = 0x2;
    public static final int PROFILE_OEM = 0x4;
    public static final int PROFILE_LOW_POWER = 0x8;

    // Intersected profiles
    public static final int PROFILE_WIDGETS = 0x100;
    public static final int PROFILE_ANDROIDX = 0x200;
    public static final int PROFILE_ANDROID_NATIVE = 0x400;
    public static final int PROFILE_WEAR_WIDGETS = 0x800;

    /**
     * Returns true if the operation exists for the given api level
     *
     * @param opId
     * @param apiLevel
     * @param profiles
     * @return
     */
    public static boolean valid(int opId, int apiLevel, int profiles) {
        switch (apiLevel) {
            case 7:
                if (sMapV7 == null) {
                    sMapV7 = createMapV7(sMapV7, profiles);
                }
                UniqueIntMap<CompanionOperation> map = sMapV7.get(profiles);
                if (map == null) {
                    sMapV7 = createMapV7(sMapV7, profiles);
                    map = sMapV7.get(profiles);
                }
                return map.get(opId) != null;
            case 6:
                if (sMapV6 == null) {
                    sMapV6 = createMapV6();
                }
                return sMapV6.get(opId) != null;
        }
        return false;
    }

    /**
     * Returns a map of operations for the given api level
     *
     * @param apiLevel
     * @param profiles
     * @return
     */
    public static @Nullable UniqueIntMap<CompanionOperation> getOperations(
            int apiLevel, int profiles) {
        switch (apiLevel) {
            case 7:
                if (sMapV7 == null || !sMapV7.containsKey(profiles)) {
                    sMapV7 = createMapV7(sMapV7, profiles);
                }
                return sMapV7.get(profiles);
            case 6:
                if (sMapV6 == null) {
                    sMapV6 = createMapV6();
                }
                return sMapV6;
        }
        return null;
    }

    private static UniqueIntMap<CompanionOperation> createMapV6() {
        UniqueIntMap<CompanionOperation> map = new UniqueIntMap<>();
        fillDefaultVersionMap(map);
        map.put(DATA_SHADER, ShaderData::read);
        map.put(ROOT_CONTENT_BEHAVIOR, RootContentBehavior::read);
        return map;
    }

    private static UniqueIntMap<CompanionOperation> createMapV7_Androidx() {
        if (sMapV7AndroidX == null) {
            sMapV7AndroidX = new UniqueIntMap<>();
            sMapV7AndroidX.put(MATRIX_FROM_PATH, MatrixFromPath::read);
            sMapV7AndroidX.put(TEXT_SUBTEXT, TextSubtext::read);
            sMapV7AndroidX.put(BITMAP_TEXT_MEASURE, BitmapTextMeasure::read);
            sMapV7AndroidX.put(DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH, DrawBitmapFontTextOnPath::read);
            sMapV7AndroidX.put(DRAW_BITMAP_TEXT_ANCHORED, DrawBitmapTextAnchored::read);
            sMapV7AndroidX.put(DATA_SHADER, ShaderData::read);
            sMapV7AndroidX.put(DATA_FONT, FontData::read);
            sMapV7AndroidX.put(DRAW_TO_BITMAP, DrawToBitmap::read);
            sMapV7AndroidX.put(WAKE_IN, WakeIn::read);
            sMapV7AndroidX.put(ID_LOOKUP, IdLookup::read);
            sMapV7AndroidX.put(PATH_EXPRESSION, PathExpression::read);
        }
        return sMapV7AndroidX;
    }

    private static UniqueIntMap<CompanionOperation> createMapV7_Androidx_Experimental() {
        if (sMapV7AndroidXExperimental == null) {
            sMapV7AndroidXExperimental = new UniqueIntMap<>();
            // add experimental operations for this profile here
        }
        return sMapV7AndroidXExperimental;
    }

    private static UniqueIntMap<CompanionOperation> createMapV7_Androidx_Deprecated() {
        if (sMapV7AndroidXDeprecated == null) {
            sMapV7AndroidXDeprecated = new UniqueIntMap<>();
            sMapV7AndroidXDeprecated.put(ROOT_CONTENT_BEHAVIOR, RootContentBehavior::read);
        }
        return sMapV7AndroidXDeprecated;
    }

    private static UniqueIntMap<CompanionOperation> createMapV7_Widgets() {
        if (sMapV7Widgets == null) {
            sMapV7Widgets = new UniqueIntMap<>();
            sMapV7Widgets.put(MATRIX_FROM_PATH, MatrixFromPath::read);
            sMapV7Widgets.put(TEXT_SUBTEXT, TextSubtext::read);
            sMapV7Widgets.put(BITMAP_TEXT_MEASURE, BitmapTextMeasure::read);
            sMapV7Widgets.put(DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH, DrawBitmapFontTextOnPath::read);
            sMapV7Widgets.put(DRAW_BITMAP_TEXT_ANCHORED, DrawBitmapTextAnchored::read);
            sMapV7Widgets.put(DRAW_TO_BITMAP, DrawToBitmap::read);
            sMapV7Widgets.put(WAKE_IN, WakeIn::read);
            sMapV7Widgets.put(ID_LOOKUP, IdLookup::read);
            sMapV7Widgets.put(PATH_EXPRESSION, PathExpression::read);
        }
        return sMapV7Widgets;
    }

    private static UniqueIntMap<CompanionOperation> createMapV7_Widgets_Experimental() {
        if (sMapV7WidgetsExperimental == null) {
            sMapV7WidgetsExperimental = new UniqueIntMap<>();
            // add experimental operations for this profile here
        }
        return sMapV7WidgetsExperimental;
    }

    private static UniqueIntMap<CompanionOperation> createMapV7_Widgets_Deprecated() {
        if (sMapV7WidgetsDeprecated == null) {
            sMapV7WidgetsDeprecated = new UniqueIntMap<>();
            sMapV7WidgetsDeprecated.put(ROOT_CONTENT_BEHAVIOR, RootContentBehavior::read);
        }
        return sMapV7WidgetsDeprecated;
    }

    /**
     * Returns a list of operation for the v7 using the given profiles
     *
     * @param currentMapV7
     * @param profiles
     * @return
     */
    private static HashMap<Integer, UniqueIntMap<CompanionOperation>> createMapV7(
            HashMap<Integer, UniqueIntMap<CompanionOperation>> currentMapV7, int profiles) {
        UniqueIntMap<CompanionOperation> mapV7 = new UniqueIntMap<>();

        // Add the set of pre-v7 operations

        fillDefaultVersionMap(mapV7);

        if (profiles != 0) {
            // Add profiles operations

            ArrayList<UniqueIntMap<CompanionOperation>> listProfiles = new ArrayList<>();
            if ((profiles & PROFILE_ANDROIDX) != 0) {
                UniqueIntMap<CompanionOperation> androidx = new UniqueIntMap<>();
                androidx.putAll(createMapV7_Androidx());

                if ((profiles & PROFILE_EXPERIMENTAL) != 0) {
                    androidx.putAll(createMapV7_Androidx_Experimental());
                }
                if ((profiles & Operations.PROFILE_DEPRECATED) != 0) {
                    androidx.putAll(createMapV7_Androidx_Deprecated());
                }
                listProfiles.add(androidx);
            }
            if ((profiles & PROFILE_WIDGETS) != 0) {
                UniqueIntMap<CompanionOperation> widgets = new UniqueIntMap<>();
                widgets.putAll(createMapV7_Widgets());

                if ((profiles & PROFILE_EXPERIMENTAL) != 0) {
                    widgets.putAll(createMapV7_Widgets_Experimental());
                }
                if ((profiles & Operations.PROFILE_DEPRECATED) != 0) {
                    widgets.putAll(createMapV7_Widgets_Deprecated());
                }
                listProfiles.add(widgets);
            }
            if ((profiles & PROFILE_ANDROID_NATIVE) != 0) {
                throw new UnsupportedOperationException(
                        "Android native profiles are defined externally");
            }

            if (listProfiles.size() == 1) {
                mapV7.putAll(listProfiles.get(0));
            } else {
                // If multiple profiles are specified, we only want to support the intersection
                // (to ensure that the document will work in all the profiles).
                // Let's compute the intersection of the operations in the profiles.

                HashMap<Integer, Integer> intersection = new HashMap<>();
                for (UniqueIntMap<CompanionOperation> profile : listProfiles) {
                    for (int i = 0; i < 255; i++) { // one day we'll have an iterator in IntMap
                        CompanionOperation op = profile.get(i);
                        if (op != null) {
                            int count = 0;
                            if (intersection.containsKey(i)) {
                                count = intersection.get(i);
                            }
                            intersection.put(i, count + 1);
                        }
                    }
                }
                if (!intersection.isEmpty()) {
                    int max = listProfiles.size();
                    UniqueIntMap<CompanionOperation> profile = listProfiles.get(0);
                    for (Integer key : intersection.keySet()) {
                        int count = intersection.get(key);
                        if (count == max) {
                            mapV7.put(key, profile.get(key));
                        }
                    }
                }
            }
        }

        // Add baseline v7 operations

        mapV7.put(REM, Rem::read);
        mapV7.put(MATRIX_CONSTANT, MatrixConstant::read);
        mapV7.put(MATRIX_EXPRESSION, MatrixExpression::read);
        mapV7.put(MATRIX_VECTOR_MATH, MatrixVectorMath::read);

        // Add the computed map to the overall hashmap of versions...

        if (currentMapV7 != null) {
            currentMapV7.put(profiles, mapV7);
            return currentMapV7;
        }

        HashMap<Integer, UniqueIntMap<CompanionOperation>> result = new HashMap<>();
        result.put(profiles, mapV7);
        return result;
    }

    public static class UniqueIntMap<T> extends IntMap<T> {
        @Override
        public T put(int key, @NonNull T value) {
            assert null == get(key) : "Opcode " + key + " already used in Operations !";
            return super.put(key, value);
        }
    }

    private static void fillDefaultVersionMap(UniqueIntMap<CompanionOperation> map) {
        map.put(HEADER, Header::read);
        map.put(DRAW_BITMAP_INT, DrawBitmapInt::read);
        map.put(DATA_BITMAP, BitmapData::read);
        map.put(DATA_BITMAP_FONT, BitmapFontData::read);
        map.put(DATA_TEXT, TextData::read);
        map.put(THEME, Theme::read);
        map.put(CLICK_AREA, ClickArea::read);
        map.put(ROOT_CONTENT_DESCRIPTION, RootContentDescription::read);

        map.put(DRAW_SECTOR, DrawSector::read);
        map.put(DRAW_BITMAP, DrawBitmap::read);
        map.put(DRAW_CIRCLE, DrawCircle::read);
        map.put(DRAW_LINE, DrawLine::read);
        map.put(DRAW_OVAL, DrawOval::read);
        map.put(DRAW_PATH, DrawPath::read);
        map.put(DRAW_RECT, DrawRect::read);
        map.put(DRAW_ROUND_RECT, DrawRoundRect::read);
        map.put(DRAW_TEXT_ON_PATH, DrawTextOnPath::read);
        map.put(DRAW_TEXT_RUN, DrawText::read);
        map.put(DRAW_BITMAP_FONT_TEXT_RUN, DrawBitmapFontText::read);
        map.put(DRAW_TWEEN_PATH, DrawTweenPath::read);
        map.put(DATA_PATH, PathData::read);
        map.put(PAINT_VALUES, PaintData::read);
        map.put(MATRIX_RESTORE, MatrixRestore::read);
        map.put(MATRIX_ROTATE, MatrixRotate::read);
        map.put(MATRIX_SAVE, MatrixSave::read);
        map.put(MATRIX_SCALE, MatrixScale::read);
        map.put(MATRIX_SKEW, MatrixSkew::read);
        map.put(MATRIX_TRANSLATE, MatrixTranslate::read);
        map.put(CLIP_PATH, ClipPath::read);
        map.put(CLIP_RECT, ClipRect::read);
        map.put(DATA_FLOAT, FloatConstant::read);
        map.put(ANIMATED_FLOAT, FloatExpression::read);
        map.put(DRAW_TEXT_ANCHOR, DrawTextAnchored::read);
        map.put(COLOR_EXPRESSIONS, ColorExpression::read);
        map.put(TEXT_FROM_FLOAT, TextFromFloat::read);
        map.put(TEXT_MERGE, TextMerge::read);
        map.put(NAMED_VARIABLE, NamedVariable::read);
        map.put(COLOR_CONSTANT, ColorConstant::read);
        map.put(DATA_INT, IntegerConstant::read);
        map.put(INTEGER_EXPRESSION, IntegerExpression::read);
        map.put(DATA_BOOLEAN, BooleanConstant::read);
        map.put(ID_MAP, DataMapIds::read);
        map.put(ID_LIST, DataListIds::read);
        map.put(FLOAT_LIST, DataListFloat::read);
        map.put(DATA_LONG, LongConstant::read);
        map.put(DRAW_BITMAP_SCALED, DrawBitmapScaled::read);
        map.put(TEXT_LOOKUP, TextLookup::read);
        map.put(TEXT_LOOKUP_INT, TextLookupInt::read);

        map.put(LOOP_START, LoopOperation::read);

        // Layout

        map.put(COMPONENT_START, ComponentStart::read);
        map.put(ANIMATION_SPEC, AnimationSpec::read);

        map.put(MODIFIER_WIDTH, WidthModifierOperation::read);
        map.put(MODIFIER_HEIGHT, HeightModifierOperation::read);
        map.put(MODIFIER_WIDTH_IN, WidthInModifierOperation::read);
        map.put(MODIFIER_HEIGHT_IN, HeightInModifierOperation::read);
        map.put(MODIFIER_COLLAPSIBLE_PRIORITY, CollapsiblePriorityModifierOperation::read);
        map.put(MODIFIER_PADDING, PaddingModifierOperation::read);
        map.put(MODIFIER_BACKGROUND, BackgroundModifierOperation::read);
        map.put(MODIFIER_BORDER, BorderModifierOperation::read);
        map.put(MODIFIER_ROUNDED_CLIP_RECT, RoundedClipRectModifierOperation::read);
        map.put(MODIFIER_CLIP_RECT, ClipRectModifierOperation::read);
        map.put(MODIFIER_CLICK, ClickModifierOperation::read);
        map.put(MODIFIER_TOUCH_DOWN, TouchDownModifierOperation::read);
        map.put(MODIFIER_TOUCH_UP, TouchUpModifierOperation::read);
        map.put(MODIFIER_TOUCH_CANCEL, TouchCancelModifierOperation::read);
        map.put(MODIFIER_VISIBILITY, ComponentVisibilityOperation::read);
        map.put(MODIFIER_OFFSET, OffsetModifierOperation::read);
        map.put(MODIFIER_ZINDEX, ZIndexModifierOperation::read);
        map.put(MODIFIER_GRAPHICS_LAYER, GraphicsLayerModifierOperation::read);
        map.put(MODIFIER_SCROLL, ScrollModifierOperation::read);
        map.put(MODIFIER_MARQUEE, MarqueeModifierOperation::read);
        map.put(MODIFIER_RIPPLE, RippleModifierOperation::read);
        map.put(MODIFIER_DRAW_CONTENT, DrawContentOperation::read);

        map.put(CONTAINER_END, ContainerEnd::read);

        map.put(RUN_ACTION, RunActionOperation::read);
        map.put(HOST_ACTION, HostActionOperation::read);
        map.put(HOST_METADATA_ACTION, HostActionMetadataOperation::read);
        map.put(HOST_NAMED_ACTION, HostNamedActionOperation::read);
        map.put(VALUE_INTEGER_CHANGE_ACTION, ValueIntegerChangeActionOperation::read);
        map.put(
                VALUE_INTEGER_EXPRESSION_CHANGE_ACTION,
                ValueIntegerExpressionChangeActionOperation::read);
        map.put(VALUE_STRING_CHANGE_ACTION, ValueStringChangeActionOperation::read);
        map.put(VALUE_FLOAT_CHANGE_ACTION, ValueFloatChangeActionOperation::read);
        map.put(
                VALUE_FLOAT_EXPRESSION_CHANGE_ACTION,
                ValueFloatExpressionChangeActionOperation::read);

        map.put(LAYOUT_ROOT, RootLayoutComponent::read);
        map.put(LAYOUT_CONTENT, LayoutComponentContent::read);
        map.put(LAYOUT_BOX, BoxLayout::read);
        map.put(LAYOUT_FIT_BOX, FitBoxLayout::read);
        map.put(LAYOUT_COLUMN, ColumnLayout::read);
        map.put(LAYOUT_COLLAPSIBLE_COLUMN, CollapsibleColumnLayout::read);
        map.put(LAYOUT_ROW, RowLayout::read);
        map.put(LAYOUT_COLLAPSIBLE_ROW, CollapsibleRowLayout::read);
        map.put(LAYOUT_CANVAS, CanvasLayout::read);
        map.put(LAYOUT_CANVAS_CONTENT, CanvasContent::read);
        map.put(LAYOUT_TEXT, TextLayout::read);
        map.put(LAYOUT_IMAGE, ImageLayout::read);
        map.put(LAYOUT_STATE, StateLayout::read);
        map.put(DRAW_CONTENT, DrawContent::read);

        map.put(COMPONENT_VALUE, ComponentValue::read);
        map.put(DRAW_ARC, DrawArc::read);
        map.put(DATA_MAP_LOOKUP, DataMapLookup::read);
        map.put(TEXT_MEASURE, TextMeasure::read);
        map.put(TEXT_LENGTH, TextLength::read);
        map.put(TOUCH_EXPRESSION, TouchExpression::read);
        map.put(PATH_TWEEN, PathTween::read);
        map.put(PATH_CREATE, PathCreate::read);
        map.put(PATH_ADD, PathAppend::read);
        map.put(IMPULSE_START, ImpulseOperation::read);
        map.put(IMPULSE_PROCESS, ImpulseProcess::read);
        map.put(PARTICLE_DEFINE, ParticlesCreate::read);
        map.put(PARTICLE_LOOP, ParticlesLoop::read);
        map.put(FUNCTION_CALL, FloatFunctionCall::read);
        map.put(FUNCTION_DEFINE, FloatFunctionDefine::read);
        map.put(CANVAS_OPERATIONS, CanvasOperations::read);

        map.put(ACCESSIBILITY_SEMANTICS, CoreSemantics::read);
        map.put(ATTRIBUTE_IMAGE, ImageAttribute::read);
        map.put(ATTRIBUTE_TEXT, TextAttribute::read);
        map.put(ATTRIBUTE_TIME, TimeAttribute::read);
        map.put(PATH_COMBINE, PathCombine::read);
        map.put(HAPTIC_FEEDBACK, HapticFeedback::read);
        map.put(CONDITIONAL_OPERATIONS, ConditionalOperations::read);
        map.put(DEBUG_MESSAGE, DebugMessage::read);
        map.put(ATTRIBUTE_COLOR, ColorAttribute::read);
        // TODO ?? map.put(ACCESSIBILITY_CUSTOM_ACTION, CoreSemantics::read);

    }
}
