/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.integration.view.convert;

import android.annotation.SuppressLint;

import androidx.compose.remote.core.Operations;

import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of RemoteCompose opcodes and their field specifications.
 * Used for structured JSON representation.
 */
@SuppressLint("RestrictedApiAndroidX")
public class OpcodeRegistry {

    private OpcodeRegistry() {

    }

    @SuppressLint("RestrictedApiAndroidX")
    public enum FieldType {
        BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, UTF8, BUFFER, BOOLEAN,
        FLOAT_ARRAY, FLOAT_ARRAY_BASE64,
        INT_ARRAY, HEADER_BODY, GLYPH_ARRAY, KERNING_TABLE,
        FLOAT_RPN, INT_RPN
    }

    @SuppressLint("RestrictedApiAndroidX")
    public static class FieldSpec {
        public final String name;
        public final FieldType type;

        public FieldSpec(@NonNull String name, @NonNull FieldType type) {
            this.name = name;
            this.type = type;
        }
    }

    @SuppressLint("RestrictedApiAndroidX")
    public static class OpSpec {
        public final int opcode;
        public final String name;
        public final FieldSpec[] fields;
        public final boolean isVariable;
        public final boolean forceReconstruct;

        public OpSpec(int opcode, @NonNull String name, @NonNull FieldSpec... fields) {
            this(opcode, name, false, false, fields);
        }

        public OpSpec(int opcode, @NonNull String name, boolean isVariable,
                @NonNull FieldSpec... fields) {
            this(opcode, name, isVariable, false, fields);
        }

        public OpSpec(int opcode, @NonNull String name, boolean isVariable,
                boolean forceReconstruct,
                @NonNull FieldSpec... fields) {
            this.opcode = opcode;
            this.name = name;
            this.isVariable = isVariable;
            this.forceReconstruct = forceReconstruct;
            this.fields = fields;
        }

        /**
         * is this entry a fixed-length entry?
         *
         * @return true if all fields are fixed-length
         */
        @SuppressLint("RestrictedApiAndroidX")
        public boolean isFixedLength() {
            if (isVariable) return false;
            for (FieldSpec f : fields) {
                if (f.type == FieldType.UTF8 || f.type == FieldType.BUFFER
                        || f.type == FieldType.FLOAT_ARRAY
                        || f.type == FieldType.FLOAT_ARRAY_BASE64
                        || f.type == FieldType.INT_ARRAY
                        || f.type == FieldType.HEADER_BODY
                        || f.type == FieldType.GLYPH_ARRAY
                        || f.type == FieldType.KERNING_TABLE
                        || f.type == FieldType.FLOAT_RPN
                        || f.type == FieldType.INT_RPN) {
                    return false;
                }
            }
            return true;
        }
    }

    @SuppressLint("PrimitiveInCollection")
    private static final Map<Integer, OpSpec> sSpecs = new HashMap<>();

    static {
        // Protocol & Data
        reg(new OpSpec(Operations.HEADER, "HEADER", false, true,
                new FieldSpec("major", FieldType.INT),
                new FieldSpec("minor", FieldType.INT),
                new FieldSpec("patch", FieldType.INT),
                new FieldSpec("body", FieldType.HEADER_BODY)));
        reg(new OpSpec(Operations.DATA_TEXT, "DATA_TEXT", false, true,
                new FieldSpec("textId", FieldType.INT),
                new FieldSpec("text", FieldType.UTF8)));
        reg(new OpSpec(Operations.DATA_FLOAT, "DATA_FLOAT",
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("value", FieldType.FLOAT)));
        reg(new OpSpec(Operations.ANIMATED_FLOAT, "ANIMATED_FLOAT", false, true,
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("packedLen", FieldType.INT),
                new FieldSpec("expression", FieldType.FLOAT_RPN),
                new FieldSpec("animation", FieldType.FLOAT_ARRAY_BASE64)));
        reg(new OpSpec(Operations.DATA_INT, "DATA_INT",
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("value", FieldType.INT)));
        reg(new OpSpec(Operations.DATA_LONG, "DATA_LONG",
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("value", FieldType.LONG)));
        reg(new OpSpec(Operations.DATA_BOOLEAN, "DATA_BOOLEAN",
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("value", FieldType.BOOLEAN)));
        reg(new OpSpec(Operations.DATA_BITMAP, "DATA_BITMAP", false, true,
                new FieldSpec("imageId", FieldType.INT),
                new FieldSpec("widthAndType", FieldType.INT),
                new FieldSpec("heightAndEncoding", FieldType.INT),
                new FieldSpec("bitmap", FieldType.BUFFER)));
        reg(new OpSpec(Operations.THEME, "THEME",
                new FieldSpec("theme", FieldType.INT)));
        reg(new OpSpec(Operations.CLICK_AREA, "CLICK_AREA",
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("contentDescriptionId", FieldType.INT),
                new FieldSpec("left", FieldType.FLOAT),
                new FieldSpec("top", FieldType.FLOAT),
                new FieldSpec("right", FieldType.FLOAT),
                new FieldSpec("bottom", FieldType.FLOAT),
                new FieldSpec("metadataId", FieldType.INT)));
        reg(new OpSpec(Operations.ROOT_CONTENT_DESCRIPTION, "ROOT_CONTENT_DESCRIPTION",
                new FieldSpec("contentDescriptionId", FieldType.INT)));
        reg(new OpSpec(Operations.ROOT_CONTENT_BEHAVIOR, "ROOT_CONTENT_BEHAVIOR",
                new FieldSpec("scroll", FieldType.INT),
                new FieldSpec("alignment", FieldType.INT),
                new FieldSpec("sizing", FieldType.INT),
                new FieldSpec("mode", FieldType.INT)));
        reg(new OpSpec(Operations.NAMED_VARIABLE, "NAMED_VARIABLE", false, true,
                new FieldSpec("varId", FieldType.INT),
                new FieldSpec("varType", FieldType.INT),
                new FieldSpec("name", FieldType.UTF8)));

        // Basic Draw Commands
        reg(new OpSpec(Operations.DRAW_RECT, "DRAW_RECT",
                new FieldSpec("left", FieldType.FLOAT),
                new FieldSpec("top", FieldType.FLOAT),
                new FieldSpec("right", FieldType.FLOAT),
                new FieldSpec("bottom", FieldType.FLOAT)));
        reg(new OpSpec(Operations.DRAW_LINE, "DRAW_LINE",
                new FieldSpec("x1", FieldType.FLOAT),
                new FieldSpec("y1", FieldType.FLOAT),
                new FieldSpec("x2", FieldType.FLOAT),
                new FieldSpec("y2", FieldType.FLOAT)));
        reg(new OpSpec(Operations.DRAW_CIRCLE, "DRAW_CIRCLE",
                new FieldSpec("centerX", FieldType.FLOAT),
                new FieldSpec("centerY", FieldType.FLOAT),
                new FieldSpec("radius", FieldType.FLOAT)));
        reg(new OpSpec(Operations.DRAW_OVAL, "DRAW_OVAL",
                new FieldSpec("left", FieldType.FLOAT),
                new FieldSpec("top", FieldType.FLOAT),
                new FieldSpec("right", FieldType.FLOAT),
                new FieldSpec("bottom", FieldType.FLOAT)));
        reg(new OpSpec(Operations.DRAW_ROUND_RECT, "DRAW_ROUND_RECT",
                new FieldSpec("left", FieldType.FLOAT),
                new FieldSpec("top", FieldType.FLOAT),
                new FieldSpec("right", FieldType.FLOAT),
                new FieldSpec("bottom", FieldType.FLOAT),
                new FieldSpec("radiusX", FieldType.FLOAT),
                new FieldSpec("radiusY", FieldType.FLOAT)));
        reg(new OpSpec(Operations.DRAW_ARC, "DRAW_ARC",
                new FieldSpec("left", FieldType.FLOAT),
                new FieldSpec("top", FieldType.FLOAT),
                new FieldSpec("right", FieldType.FLOAT),
                new FieldSpec("bottom", FieldType.FLOAT),
                new FieldSpec("startAngle", FieldType.FLOAT),
                new FieldSpec("sweepAngle", FieldType.FLOAT)));
        reg(new OpSpec(Operations.DRAW_SECTOR, "DRAW_SECTOR",
                new FieldSpec("left", FieldType.FLOAT),
                new FieldSpec("top", FieldType.FLOAT),
                new FieldSpec("right", FieldType.FLOAT),
                new FieldSpec("bottom", FieldType.FLOAT),
                new FieldSpec("startAngle", FieldType.FLOAT),
                new FieldSpec("sweepAngle", FieldType.FLOAT)));
        reg(new OpSpec(Operations.DRAW_BITMAP, "DRAW_BITMAP",
                new FieldSpec("imageId", FieldType.INT),
                new FieldSpec("left", FieldType.FLOAT),
                new FieldSpec("top", FieldType.FLOAT),
                new FieldSpec("right", FieldType.FLOAT),
                new FieldSpec("bottom", FieldType.FLOAT),
                new FieldSpec("descriptionId", FieldType.INT)));
        reg(new OpSpec(Operations.DRAW_BITMAP_INT, "DRAW_BITMAP_INT",
                new FieldSpec("imageId", FieldType.INT),
                new FieldSpec("srcLeft", FieldType.INT),
                new FieldSpec("srcTop", FieldType.INT),
                new FieldSpec("srcRight", FieldType.INT),
                new FieldSpec("srcBottom", FieldType.INT),
                new FieldSpec("dstLeft", FieldType.INT),
                new FieldSpec("dstTop", FieldType.INT),
                new FieldSpec("dstRight", FieldType.INT),
                new FieldSpec("dstBottom", FieldType.INT),
                new FieldSpec("contentDescriptionId", FieldType.INT)));
        reg(new OpSpec(Operations.DRAW_BITMAP_SCALED, "DRAW_BITMAP_SCALED",
                new FieldSpec("imageId", FieldType.INT),
                new FieldSpec("srcLeft", FieldType.FLOAT),
                new FieldSpec("srcTop", FieldType.FLOAT),
                new FieldSpec("srcRight", FieldType.FLOAT),
                new FieldSpec("srcBottom", FieldType.FLOAT),
                new FieldSpec("dstLeft", FieldType.FLOAT),
                new FieldSpec("dstTop", FieldType.FLOAT),
                new FieldSpec("dstRight", FieldType.FLOAT),
                new FieldSpec("dstBottom", FieldType.FLOAT),
                new FieldSpec("scaleType", FieldType.INT),
                new FieldSpec("scaleFactor", FieldType.FLOAT),
                new FieldSpec("descriptionId", FieldType.INT)));
        reg(new OpSpec(Operations.DRAW_TEXT_RUN, "DRAW_TEXT_RUN",
                new FieldSpec("textId", FieldType.INT),
                new FieldSpec("start", FieldType.INT),
                new FieldSpec("end", FieldType.INT),
                new FieldSpec("contextStart", FieldType.INT),
                new FieldSpec("contextEnd", FieldType.INT),
                new FieldSpec("x", FieldType.FLOAT),
                new FieldSpec("y", FieldType.FLOAT),
                new FieldSpec("rtl", FieldType.BOOLEAN)));
        reg(new OpSpec(Operations.DRAW_BITMAP_FONT_TEXT_RUN, "DRAW_BITMAP_FONT_TEXT_RUN",
                new FieldSpec("textId", FieldType.INT)));
        reg(new OpSpec(Operations.DRAW_TEXT_ON_PATH, "DRAW_TEXT_ON_PATH",
                new FieldSpec("textId", FieldType.INT),
                new FieldSpec("pathId", FieldType.INT),
                new FieldSpec("hOffset", FieldType.FLOAT),
                new FieldSpec("vOffset", FieldType.FLOAT)));
        reg(new OpSpec(Operations.DRAW_TEXT_ON_CIRCLE, "DRAW_TEXT_ON_CIRCLE",
                new FieldSpec("textId", FieldType.INT),
                new FieldSpec("centerX", FieldType.FLOAT),
                new FieldSpec("centerY", FieldType.FLOAT),
                new FieldSpec("radius", FieldType.FLOAT),
                new FieldSpec("startAngle", FieldType.FLOAT),
                new FieldSpec("warpRadiusOffset", FieldType.FLOAT),
                new FieldSpec("alignment", FieldType.BYTE),
                new FieldSpec("placement", FieldType.BYTE)));
        reg(new OpSpec(Operations.DRAW_TEXT_ANCHOR, "DRAW_TEXT_ANCHOR",
                new FieldSpec("textId", FieldType.INT),
                new FieldSpec("x", FieldType.FLOAT),
                new FieldSpec("y", FieldType.FLOAT),
                new FieldSpec("panX", FieldType.FLOAT),
                new FieldSpec("panY", FieldType.FLOAT),
                new FieldSpec("flags", FieldType.INT)));
        reg(new OpSpec(Operations.DRAW_TWEEN_PATH, "DRAW_TWEEN_PATH",
                new FieldSpec("path1Id", FieldType.INT),
                new FieldSpec("path2Id", FieldType.INT),
                new FieldSpec("tween", FieldType.FLOAT),
                new FieldSpec("start", FieldType.FLOAT),
                new FieldSpec("stop", FieldType.FLOAT)));
        reg(new OpSpec(Operations.DRAW_PATH, "DRAW_PATH",
                new FieldSpec("pathId", FieldType.INT)));
        reg(new OpSpec(Operations.CLIP_PATH, "CLIP_PATH",
                new FieldSpec("pathId", FieldType.INT)));
        reg(new OpSpec(Operations.CLIP_RECT, "CLIP_RECT",
                new FieldSpec("left", FieldType.FLOAT),
                new FieldSpec("top", FieldType.FLOAT),
                new FieldSpec("right", FieldType.FLOAT),
                new FieldSpec("bottom", FieldType.FLOAT)));

        // Matrix
        reg(new OpSpec(Operations.MATRIX_TRANSLATE, "MATRIX_TRANSLATE",
                new FieldSpec("dx", FieldType.FLOAT),
                new FieldSpec("dy", FieldType.FLOAT)));
        reg(new OpSpec(Operations.MATRIX_SCALE, "MATRIX_SCALE",
                new FieldSpec("scaleX", FieldType.FLOAT),
                new FieldSpec("scaleY", FieldType.FLOAT),
                new FieldSpec("centerX", FieldType.FLOAT),
                new FieldSpec("centerY", FieldType.FLOAT)));
        reg(new OpSpec(Operations.MATRIX_ROTATE, "MATRIX_ROTATE",
                new FieldSpec("angle", FieldType.FLOAT),
                new FieldSpec("centerX", FieldType.FLOAT),
                new FieldSpec("centerY", FieldType.FLOAT)));
        reg(new OpSpec(Operations.MATRIX_SKEW, "MATRIX_SKEW",
                new FieldSpec("skewX", FieldType.FLOAT),
                new FieldSpec("skewY", FieldType.FLOAT)));
        reg(new OpSpec(Operations.MATRIX_SAVE, "MATRIX_SAVE"));
        reg(new OpSpec(Operations.MATRIX_RESTORE, "MATRIX_RESTORE"));
        reg(new OpSpec(Operations.MATRIX_CONSTANT, "MATRIX_CONSTANT", true,
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("type", FieldType.INT)));

        // Path
        reg(new OpSpec(Operations.PATH_TWEEN, "PATH_TWEEN",
                new FieldSpec("outId", FieldType.INT),
                new FieldSpec("pathId1", FieldType.INT),
                new FieldSpec("pathId2", FieldType.INT),
                new FieldSpec("tween", FieldType.FLOAT)));
        reg(new OpSpec(Operations.PATH_CREATE, "PATH_CREATE",
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("startX", FieldType.FLOAT),
                new FieldSpec("startY", FieldType.FLOAT)));

        // Layout Components
        reg(new OpSpec(Operations.LAYOUT_ROOT, "LAYOUT_ROOT",
                new FieldSpec("componentId", FieldType.INT)));
        reg(new OpSpec(Operations.COMPONENT_START, "COMPONENT_START",
                new FieldSpec("type", FieldType.INT),
                new FieldSpec("componentId", FieldType.INT),
                new FieldSpec("x", FieldType.FLOAT),
                new FieldSpec("y", FieldType.FLOAT)));
        reg(new OpSpec(Operations.LAYOUT_BOX, "LAYOUT_BOX",
                new FieldSpec("componentId", FieldType.INT),
                new FieldSpec("animationId", FieldType.INT),
                new FieldSpec("horizontal", FieldType.INT),
                new FieldSpec("vertical", FieldType.INT)));
        reg(new OpSpec(Operations.LAYOUT_COLUMN, "LAYOUT_COLUMN",
                new FieldSpec("componentId", FieldType.INT),
                new FieldSpec("animationId", FieldType.INT),
                new FieldSpec("horizontal", FieldType.INT),
                new FieldSpec("vertical", FieldType.INT),
                new FieldSpec("spacedBy", FieldType.FLOAT)));
        reg(new OpSpec(Operations.LAYOUT_ROW, "LAYOUT_ROW",
                new FieldSpec("componentId", FieldType.INT),
                new FieldSpec("animationId", FieldType.INT),
                new FieldSpec("horizontal", FieldType.INT),
                new FieldSpec("vertical", FieldType.INT),
                new FieldSpec("spacedBy", FieldType.FLOAT)));
        reg(new OpSpec(Operations.LAYOUT_FLOW, "LAYOUT_FLOW",
                new FieldSpec("componentId", FieldType.INT),
                new FieldSpec("animationId", FieldType.INT),
                new FieldSpec("horizontal", FieldType.INT),
                new FieldSpec("vertical", FieldType.INT),
                new FieldSpec("spacedBy", FieldType.FLOAT)));
        reg(new OpSpec(Operations.LAYOUT_TEXT, "LAYOUT_TEXT",
                new FieldSpec("componentId", FieldType.INT),
                new FieldSpec("animationId", FieldType.INT),
                new FieldSpec("textId", FieldType.INT),
                new FieldSpec("color", FieldType.INT),
                new FieldSpec("fontSize", FieldType.FLOAT),
                new FieldSpec("fontStyle", FieldType.INT),
                new FieldSpec("fontWeight", FieldType.FLOAT),
                new FieldSpec("fontFamilyId", FieldType.INT),
                new FieldSpec("textAlign", FieldType.INT),
                new FieldSpec("overflow", FieldType.INT),
                new FieldSpec("maxLines", FieldType.INT)));
        reg(new OpSpec(Operations.LAYOUT_IMAGE, "LAYOUT_IMAGE",
                new FieldSpec("componentId", FieldType.INT),
                new FieldSpec("animationId", FieldType.INT),
                new FieldSpec("bitmapId", FieldType.INT),
                new FieldSpec("scaleType", FieldType.INT),
                new FieldSpec("alpha", FieldType.FLOAT)));
        reg(new OpSpec(Operations.LAYOUT_CANVAS, "LAYOUT_CANVAS",
                new FieldSpec("componentId", FieldType.INT),
                new FieldSpec("animationId", FieldType.INT)));
        reg(new OpSpec(Operations.CONTAINER_END, "CONTAINER_END"));

        // Modifiers
        reg(new OpSpec(Operations.MODIFIER_PADDING, "MODIFIER_PADDING",
                new FieldSpec("left", FieldType.FLOAT),
                new FieldSpec("top", FieldType.FLOAT),
                new FieldSpec("right", FieldType.FLOAT),
                new FieldSpec("bottom", FieldType.FLOAT)));
        reg(new OpSpec(Operations.MODIFIER_WIDTH, "MODIFIER_WIDTH",
                new FieldSpec("type", FieldType.INT),
                new FieldSpec("width", FieldType.FLOAT)));
        reg(new OpSpec(Operations.MODIFIER_HEIGHT, "MODIFIER_HEIGHT",
                new FieldSpec("type", FieldType.INT),
                new FieldSpec("height", FieldType.FLOAT)));
        reg(new OpSpec(Operations.MODIFIER_WIDTH_IN, "MODIFIER_WIDTH_IN",
                new FieldSpec("min", FieldType.FLOAT),
                new FieldSpec("max", FieldType.FLOAT)));
        reg(new OpSpec(Operations.MODIFIER_HEIGHT_IN, "MODIFIER_HEIGHT_IN",
                new FieldSpec("min", FieldType.FLOAT),
                new FieldSpec("max", FieldType.FLOAT)));
        reg(new OpSpec(Operations.MODIFIER_COLLAPSIBLE_PRIORITY, "MODIFIER_COLLAPSIBLE_PRIORITY",
                new FieldSpec("orientation", FieldType.INT),
                new FieldSpec("priority", FieldType.FLOAT)));
        reg(new OpSpec(Operations.MODIFIER_VISIBILITY, "MODIFIER_VISIBILITY",
                new FieldSpec("visibilityId", FieldType.INT)));
        reg(new OpSpec(Operations.MODIFIER_OFFSET, "MODIFIER_OFFSET",
                new FieldSpec("x", FieldType.FLOAT),
                new FieldSpec("y", FieldType.FLOAT)));
        reg(new OpSpec(Operations.MODIFIER_ZINDEX, "MODIFIER_ZINDEX",
                new FieldSpec("zIndex", FieldType.FLOAT)));
        reg(new OpSpec(Operations.MODIFIER_BACKGROUND, "MODIFIER_BACKGROUND",
                new FieldSpec("flags", FieldType.INT),
                new FieldSpec("colorId", FieldType.INT),
                new FieldSpec("reserve1", FieldType.INT),
                new FieldSpec("reserve2", FieldType.INT),
                new FieldSpec("r", FieldType.FLOAT),
                new FieldSpec("g", FieldType.FLOAT),
                new FieldSpec("b", FieldType.FLOAT),
                new FieldSpec("a", FieldType.FLOAT),
                new FieldSpec("shapeType", FieldType.INT)));
        reg(new OpSpec(Operations.MODIFIER_BORDER, "MODIFIER_BORDER",
                new FieldSpec("flags", FieldType.INT),
                new FieldSpec("colorId", FieldType.INT),
                new FieldSpec("reserve1", FieldType.INT),
                new FieldSpec("reserve2", FieldType.INT),
                new FieldSpec("borderWidth", FieldType.FLOAT),
                new FieldSpec("roundedCorner", FieldType.FLOAT),
                new FieldSpec("r", FieldType.FLOAT),
                new FieldSpec("g", FieldType.FLOAT),
                new FieldSpec("b", FieldType.FLOAT),
                new FieldSpec("a", FieldType.FLOAT),
                new FieldSpec("shapeType", FieldType.INT)));
        reg(new OpSpec(Operations.MODIFIER_ALIGN_BY, "MODIFIER_ALIGN_BY",
                new FieldSpec("line", FieldType.FLOAT),
                new FieldSpec("flags", FieldType.INT)));
        reg(new OpSpec(Operations.LAYOUT_COMPUTE, "LAYOUT_COMPUTE",
                new FieldSpec("type", FieldType.INT),
                new FieldSpec("boundsId", FieldType.INT),
                new FieldSpec("animateChanges", FieldType.BOOLEAN)));
        reg(new OpSpec(Operations.MODIFIER_RIPPLE, "MODIFIER_RIPPLE"));
        reg(new OpSpec(Operations.MODIFIER_CLICK, "MODIFIER_CLICK"));
        reg(new OpSpec(Operations.MODIFIER_TOUCH_DOWN, "MODIFIER_TOUCH_DOWN"));
        reg(new OpSpec(Operations.MODIFIER_TOUCH_UP, "MODIFIER_TOUCH_UP"));
        reg(new OpSpec(Operations.MODIFIER_TOUCH_CANCEL, "MODIFIER_TOUCH_CANCEL"));

        // Actions
        reg(new OpSpec(Operations.RUN_ACTION, "RUN_ACTION"));
        reg(new OpSpec(Operations.HOST_ACTION, "HOST_ACTION",
                new FieldSpec("actionId", FieldType.INT)));
        reg(new OpSpec(Operations.VALUE_FLOAT_CHANGE_ACTION, "VALUE_FLOAT_CHANGE_ACTION",
                new FieldSpec("targetValueId", FieldType.INT),
                new FieldSpec("value", FieldType.FLOAT)));
        reg(new OpSpec(Operations.VALUE_INTEGER_CHANGE_ACTION, "VALUE_INTEGER_CHANGE_ACTION",
                new FieldSpec("targetValueId", FieldType.INT),
                new FieldSpec("value", FieldType.INT)));
        reg(new OpSpec(Operations.VALUE_STRING_CHANGE_ACTION,
                "VALUE_STRING_CHANGE_ACTION", false, true,
                new FieldSpec("targetValueId", FieldType.INT),
                new FieldSpec("value", FieldType.UTF8)));

        // Colors
        reg(new OpSpec(Operations.COLOR_CONSTANT, "COLOR_CONSTANT",
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("color", FieldType.INT)));
        reg(new OpSpec(Operations.COLOR_EXPRESSIONS, "COLOR_EXPRESSIONS", false, true,
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("p1", FieldType.INT),
                new FieldSpec("p2", FieldType.INT),
                new FieldSpec("p3", FieldType.INT),
                new FieldSpec("p4", FieldType.INT)));

        // Other Utilities
        reg(new OpSpec(Operations.ANIMATION_SPEC, "ANIMATION_SPEC",
                new FieldSpec("animationId", FieldType.INT),
                new FieldSpec("motionDuration", FieldType.FLOAT),
                new FieldSpec("motionEasingType", FieldType.INT),
                new FieldSpec("visibilityDuration", FieldType.FLOAT),
                new FieldSpec("visibilityEasingType", FieldType.INT),
                new FieldSpec("enterAnimation", FieldType.INT),
                new FieldSpec("exitAnimation", FieldType.INT)));
        reg(new OpSpec(Operations.UPDATE_DYNAMIC_FLOAT_LIST, "UPDATE_DYNAMIC_FLOAT_LIST",
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("index", FieldType.FLOAT),
                new FieldSpec("value", FieldType.FLOAT)));
        reg(new OpSpec(Operations.TEXT_LOOKUP, "TEXT_LOOKUP",
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("dataSet", FieldType.INT),
                new FieldSpec("index", FieldType.FLOAT)));
        reg(new OpSpec(Operations.TEXT_LOOKUP_INT, "TEXT_LOOKUP_INT",
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("dataSet", FieldType.INT),
                new FieldSpec("index", FieldType.INT)));
        reg(new OpSpec(Operations.DATA_MAP_LOOKUP, "DATA_MAP_LOOKUP",
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("dataMapId", FieldType.INT),
                new FieldSpec("stringId", FieldType.INT)));
        reg(new OpSpec(Operations.TEXT_MEASURE, "TEXT_MEASURE",
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("textId", FieldType.INT),
                new FieldSpec("mode", FieldType.INT)));
        reg(new OpSpec(Operations.TEXT_LENGTH, "TEXT_LENGTH",
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("textId", FieldType.INT)));
        reg(new OpSpec(Operations.TEXT_SUBTEXT, "TEXT_SUBTEXT",
                new FieldSpec("textId", FieldType.INT),
                new FieldSpec("srcId1", FieldType.INT),
                new FieldSpec("start", FieldType.FLOAT),
                new FieldSpec("len", FieldType.FLOAT)));
        reg(new OpSpec(Operations.COMPONENT_VALUE, "COMPONENT_VALUE",
                new FieldSpec("type", FieldType.INT),
                new FieldSpec("componentId", FieldType.INT),
                new FieldSpec("id", FieldType.INT)));
        reg(new OpSpec(Operations.DEBUG_MESSAGE, "DEBUG_MESSAGE",
                new FieldSpec("textId", FieldType.INT),
                new FieldSpec("value", FieldType.FLOAT),
                new FieldSpec("flags", FieldType.INT)));
        reg(new OpSpec(Operations.HAPTIC_FEEDBACK, "HAPTIC_FEEDBACK",
                new FieldSpec("type", FieldType.INT)));
        reg(new OpSpec(Operations.WAKE_IN, "WAKE_IN",
                new FieldSpec("wake", FieldType.FLOAT)));
        reg(new OpSpec(Operations.COLOR_THEME, "COLOR_THEME",
                new FieldSpec("id", FieldType.INT),
                new FieldSpec("groupId", FieldType.INT),
                new FieldSpec("lightModeIndex", FieldType.SHORT),
                new FieldSpec("darkModeIndex", FieldType.SHORT),
                new FieldSpec("lightModeFallback", FieldType.INT),
                new FieldSpec("darkModeFallback", FieldType.INT)));
        reg(new OpSpec(Operations.PATH_COMBINE, "PATH_COMBINE",
                new FieldSpec("outId", FieldType.INT),
                new FieldSpec("pathId1", FieldType.INT),
                new FieldSpec("pathId2", FieldType.INT),
                new FieldSpec("operation", FieldType.BYTE)));
        reg(new OpSpec(Operations.MATRIX_FROM_PATH, "MATRIX_FROM_PATH",
                new FieldSpec("pathId", FieldType.INT),
                new FieldSpec("percent", FieldType.FLOAT),
                new FieldSpec("vOffset", FieldType.FLOAT),
                new FieldSpec("flags", FieldType.INT)));
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static void reg(OpSpec spec) {
        sSpecs.put(spec.opcode, spec);
    }

    /**
     * Get OpSpec for a given opcode.
     */
    @SuppressLint("RestrictedApiAndroidX")
    public static @NonNull OpSpec get(int opcode) {
        return sSpecs.get(opcode);
    }

    /**
     * Get all OpSpecs.
     */
    @SuppressLint({"RestrictedApiAndroidX", "PrimitiveInCollection"})
    public static @NonNull Map<Integer, OpSpec> getAll() {
        return sSpecs;
    }
}
