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
package androidx.compose.remote.creation.utils

import androidx.compose.remote.core.Operations

/**
 * Registry of RemoteCompose opcodes and their field specifications. Used for structured JSON
 * representation.
 */
object OpcodeRegistry {
    /** Get all OpSpecs. */
    val all: MutableMap<Int?, OpSpec> = HashMap<Int?, OpSpec>()

    init {
        // Protocol & Data
        reg(
            OpSpec(
                Operations.HEADER,
                "HEADER",
                false,
                true,
                FieldSpec("major", FieldType.INT),
                FieldSpec("minor", FieldType.INT),
                FieldSpec("patch", FieldType.INT),
                FieldSpec("body", FieldType.HEADER_BODY),
            )
        )
        reg(
            OpSpec(
                Operations.DATA_TEXT,
                "DATA_TEXT",
                false,
                true,
                FieldSpec("textId", FieldType.INT),
                FieldSpec("text", FieldType.UTF8),
            )
        )
        reg(
            OpSpec(
                Operations.ATTRIBUTE_TEXT,
                "ATTRIBUTE_TEXT",
                FieldSpec("id", FieldType.INT),
                FieldSpec("textId", FieldType.INT),
                FieldSpec("type", FieldType.SHORT),
                FieldSpec("unused", FieldType.SHORT),
            )
        )
        reg(
            OpSpec(
                Operations.ATTRIBUTE_IMAGE,
                "ATTRIBUTE_IMAGE",
                false,
                true,
                FieldSpec("id", FieldType.INT),
                FieldSpec("imageId", FieldType.INT),
                FieldSpec("type", FieldType.SHORT),
                FieldSpec("argsLength", FieldType.SHORT),
                FieldSpec("args", FieldType.INT_ARRAY),
            )
        )
        reg(
            OpSpec(
                Operations.ATTRIBUTE_TIME,
                "ATTRIBUTE_TIME",
                false,
                true,
                FieldSpec("id", FieldType.INT),
                FieldSpec("timeId", FieldType.INT),
                FieldSpec("type", FieldType.SHORT),
                FieldSpec("argsLength", FieldType.SHORT),
                FieldSpec("args", FieldType.INT_ARRAY),
            )
        )
        reg(
            OpSpec(
                Operations.ATTRIBUTE_COLOR,
                "ATTRIBUTE_COLOR",
                FieldSpec("id", FieldType.INT),
                FieldSpec("colorId", FieldType.INT),
                FieldSpec("type", FieldType.SHORT),
            )
        )
        reg(
            OpSpec(
                Operations.TEXT_MEASURE,
                "TEXT_MEASURE",
                FieldSpec("id", FieldType.INT),
                FieldSpec("textId", FieldType.INT),
                FieldSpec("type", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.DATA_FLOAT,
                "DATA_FLOAT",
                FieldSpec("id", FieldType.INT),
                FieldSpec("value", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.ANIMATED_FLOAT,
                "ANIMATED_FLOAT",
                false,
                true,
                FieldSpec("id", FieldType.INT),
                FieldSpec("packedLen", FieldType.INT),
                FieldSpec("expression", FieldType.FLOAT_RPN),
                FieldSpec("animation", FieldType.FLOAT_ARRAY_BASE64),
            )
        )
        reg(
            OpSpec(
                Operations.UPDATE_DYNAMIC_FLOAT_LIST,
                "UPDATE_DYNAMIC_FLOAT_LIST",
                FieldSpec("arrayId", FieldType.INT),
                FieldSpec("index", FieldType.FLOAT),
                FieldSpec("value", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.DATA_INT,
                "DATA_INT",
                FieldSpec("id", FieldType.INT),
                FieldSpec("value", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.DATA_LONG,
                "DATA_LONG",
                FieldSpec("id", FieldType.INT),
                FieldSpec("value", FieldType.LONG),
            )
        )
        reg(
            OpSpec(
                Operations.DATA_BOOLEAN,
                "DATA_BOOLEAN",
                FieldSpec("id", FieldType.INT),
                FieldSpec("value", FieldType.BOOLEAN),
            )
        )
        reg(
            OpSpec(
                Operations.DATA_BITMAP,
                "DATA_BITMAP",
                false,
                true,
                FieldSpec("imageId", FieldType.INT),
                FieldSpec("widthAndType", FieldType.INT),
                FieldSpec("heightAndEncoding", FieldType.INT),
                FieldSpec("bitmap", FieldType.BUFFER),
            )
        )
        reg(
            OpSpec(
                Operations.DATA_PATH,
                "DATA_PATH",
                false,
                true,
                FieldSpec("idAndWinding", FieldType.INT),
                FieldSpec("length", FieldType.INT),
                FieldSpec("pathData", FieldType.FLOAT_ARRAY),
            )
        )
        reg(
            OpSpec(
                Operations.PATH_EXPRESSION,
                "PATH_EXPRESSION",
                true,
                true,
                FieldSpec("id", FieldType.INT),
                FieldSpec("flags", FieldType.INT),
                FieldSpec("min", FieldType.FLOAT),
                FieldSpec("max", FieldType.FLOAT),
                FieldSpec("count", FieldType.FLOAT),
                FieldSpec("expressionX", FieldType.FLOAT_RPN),
                FieldSpec("expressionY", FieldType.FLOAT_RPN),
            )
        )
        reg(
            OpSpec(
                Operations.DATA_FONT,
                "DATA_FONT",
                false,
                true,
                FieldSpec("fontId", FieldType.INT),
                FieldSpec("type", FieldType.INT),
                FieldSpec("fontData", FieldType.BUFFER),
            )
        )
        reg(OpSpec(Operations.THEME, "THEME", FieldSpec("theme", FieldType.INT)))
        reg(
            OpSpec(
                Operations.CLICK_AREA,
                "CLICK_AREA",
                FieldSpec("id", FieldType.INT),
                FieldSpec("contentDescriptionId", FieldType.INT),
                FieldSpec("left", FieldType.FLOAT),
                FieldSpec("top", FieldType.FLOAT),
                FieldSpec("right", FieldType.FLOAT),
                FieldSpec("bottom", FieldType.FLOAT),
                FieldSpec("metadataId", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.ROOT_CONTENT_DESCRIPTION,
                "ROOT_CONTENT_DESCRIPTION",
                FieldSpec("contentDescriptionId", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.ROOT_CONTENT_BEHAVIOR,
                "ROOT_CONTENT_BEHAVIOR",
                FieldSpec("scroll", FieldType.INT),
                FieldSpec("alignment", FieldType.INT),
                FieldSpec("sizing", FieldType.INT),
                FieldSpec("mode", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.NAMED_VARIABLE,
                "NAMED_VARIABLE",
                false,
                true,
                FieldSpec("varId", FieldType.INT),
                FieldSpec("varType", FieldType.INT),
                FieldSpec("name", FieldType.UTF8),
            )
        )

        reg(OpSpec(Operations.DRAW_CONTENT, "DRAW_CONTENT"))

        // Basic Draw Commands
        reg(
            OpSpec(
                Operations.DRAW_RECT,
                "DRAW_RECT",
                FieldSpec("left", FieldType.FLOAT),
                FieldSpec("top", FieldType.FLOAT),
                FieldSpec("right", FieldType.FLOAT),
                FieldSpec("bottom", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_LINE,
                "DRAW_LINE",
                FieldSpec("x1", FieldType.FLOAT),
                FieldSpec("y1", FieldType.FLOAT),
                FieldSpec("x2", FieldType.FLOAT),
                FieldSpec("y2", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_CIRCLE,
                "DRAW_CIRCLE",
                FieldSpec("centerX", FieldType.FLOAT),
                FieldSpec("centerY", FieldType.FLOAT),
                FieldSpec("radius", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_OVAL,
                "DRAW_OVAL",
                FieldSpec("left", FieldType.FLOAT),
                FieldSpec("top", FieldType.FLOAT),
                FieldSpec("right", FieldType.FLOAT),
                FieldSpec("bottom", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_ROUND_RECT,
                "DRAW_ROUND_RECT",
                FieldSpec("left", FieldType.FLOAT),
                FieldSpec("top", FieldType.FLOAT),
                FieldSpec("right", FieldType.FLOAT),
                FieldSpec("bottom", FieldType.FLOAT),
                FieldSpec("radiusX", FieldType.FLOAT),
                FieldSpec("radiusY", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_ARC,
                "DRAW_ARC",
                FieldSpec("left", FieldType.FLOAT),
                FieldSpec("top", FieldType.FLOAT),
                FieldSpec("right", FieldType.FLOAT),
                FieldSpec("bottom", FieldType.FLOAT),
                FieldSpec("startAngle", FieldType.FLOAT),
                FieldSpec("sweepAngle", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_SECTOR,
                "DRAW_SECTOR",
                FieldSpec("left", FieldType.FLOAT),
                FieldSpec("top", FieldType.FLOAT),
                FieldSpec("right", FieldType.FLOAT),
                FieldSpec("bottom", FieldType.FLOAT),
                FieldSpec("startAngle", FieldType.FLOAT),
                FieldSpec("sweepAngle", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_BITMAP,
                "DRAW_BITMAP",
                FieldSpec("imageId", FieldType.INT),
                FieldSpec("left", FieldType.FLOAT),
                FieldSpec("top", FieldType.FLOAT),
                FieldSpec("right", FieldType.FLOAT),
                FieldSpec("bottom", FieldType.FLOAT),
                FieldSpec("descriptionId", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_BITMAP_INT,
                "DRAW_BITMAP_INT",
                FieldSpec("imageId", FieldType.INT),
                FieldSpec("srcLeft", FieldType.INT),
                FieldSpec("srcTop", FieldType.INT),
                FieldSpec("srcRight", FieldType.INT),
                FieldSpec("srcBottom", FieldType.INT),
                FieldSpec("dstLeft", FieldType.INT),
                FieldSpec("dstTop", FieldType.INT),
                FieldSpec("dstRight", FieldType.INT),
                FieldSpec("dstBottom", FieldType.INT),
                FieldSpec("contentDescriptionId", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_BITMAP_SCALED,
                "DRAW_BITMAP_SCALED",
                FieldSpec("imageId", FieldType.INT),
                FieldSpec("srcLeft", FieldType.FLOAT),
                FieldSpec("srcTop", FieldType.FLOAT),
                FieldSpec("srcRight", FieldType.FLOAT),
                FieldSpec("srcBottom", FieldType.FLOAT),
                FieldSpec("dstLeft", FieldType.FLOAT),
                FieldSpec("dstTop", FieldType.FLOAT),
                FieldSpec("dstRight", FieldType.FLOAT),
                FieldSpec("dstBottom", FieldType.FLOAT),
                FieldSpec("scaleType", FieldType.INT),
                FieldSpec("scaleFactor", FieldType.FLOAT),
                FieldSpec("descriptionId", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_TEXT_RUN,
                "DRAW_TEXT_RUN",
                FieldSpec("textId", FieldType.INT),
                FieldSpec("start", FieldType.INT),
                FieldSpec("end", FieldType.INT),
                FieldSpec("contextStart", FieldType.INT),
                FieldSpec("contextEnd", FieldType.INT),
                FieldSpec("x", FieldType.FLOAT),
                FieldSpec("y", FieldType.FLOAT),
                FieldSpec("rtl", FieldType.BOOLEAN),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_BITMAP_FONT_TEXT_RUN,
                "DRAW_BITMAP_FONT_TEXT_RUN",
                FieldSpec("textId", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_TEXT_ON_PATH,
                "DRAW_TEXT_ON_PATH",
                FieldSpec("textId", FieldType.INT),
                FieldSpec("pathId", FieldType.INT),
                FieldSpec("hOffset", FieldType.FLOAT),
                FieldSpec("vOffset", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_TEXT_ON_CIRCLE,
                "DRAW_TEXT_ON_CIRCLE",
                FieldSpec("textId", FieldType.INT),
                FieldSpec("centerX", FieldType.FLOAT),
                FieldSpec("centerY", FieldType.FLOAT),
                FieldSpec("radius", FieldType.FLOAT),
                FieldSpec("startAngle", FieldType.FLOAT),
                FieldSpec("warpRadiusOffset", FieldType.FLOAT),
                FieldSpec("alignment", FieldType.BYTE),
                FieldSpec("placement", FieldType.BYTE),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_TEXT_ANCHOR,
                "DRAW_TEXT_ANCHOR",
                FieldSpec("textId", FieldType.INT),
                FieldSpec("x", FieldType.FLOAT),
                FieldSpec("y", FieldType.FLOAT),
                FieldSpec("panX", FieldType.FLOAT),
                FieldSpec("panY", FieldType.FLOAT),
                FieldSpec("flags", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_BITMAP_TEXT_ANCHORED,
                "DRAW_BITMAP_TEXT_ANCHORED",
                true,
                true,
                FieldSpec("textId", FieldType.BITMAP_TEXT_ID),
                FieldSpec("glyphSpacing", FieldType.BITMAP_TEXT_GLYPH_SPACING),
                FieldSpec("bitmapFontId", FieldType.INT),
                FieldSpec("start", FieldType.FLOAT),
                FieldSpec("end", FieldType.FLOAT),
                FieldSpec("x", FieldType.FLOAT),
                FieldSpec("y", FieldType.FLOAT),
                FieldSpec("panX", FieldType.FLOAT),
                FieldSpec("panY", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.DRAW_TWEEN_PATH,
                "DRAW_TWEEN_PATH",
                FieldSpec("path1Id", FieldType.INT),
                FieldSpec("path2Id", FieldType.INT),
                FieldSpec("tween", FieldType.FLOAT),
                FieldSpec("start", FieldType.FLOAT),
                FieldSpec("stop", FieldType.FLOAT),
            )
        )
        reg(OpSpec(Operations.DRAW_PATH, "DRAW_PATH", FieldSpec("pathId", FieldType.INT)))
        reg(OpSpec(Operations.CLIP_PATH, "CLIP_PATH", FieldSpec("pathId", FieldType.INT)))
        reg(
            OpSpec(
                Operations.CLIP_RECT,
                "CLIP_RECT",
                FieldSpec("left", FieldType.FLOAT),
                FieldSpec("top", FieldType.FLOAT),
                FieldSpec("right", FieldType.FLOAT),
                FieldSpec("bottom", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.PAINT_VALUES,
                "PAINT_VALUES",
                false,
                true,
                FieldSpec("paintBundle", FieldType.INT_ARRAY),
            )
        )
        reg(
            OpSpec(
                Operations.ACCESSIBILITY_SEMANTICS,
                "ACCESSIBILITY_SEMANTICS",
                FieldSpec("contentDescriptionId", FieldType.INT),
                FieldSpec("role", FieldType.BYTE),
                FieldSpec("textId", FieldType.INT),
                FieldSpec("stateDescriptionId", FieldType.INT),
                FieldSpec("mode", FieldType.BYTE),
                FieldSpec("enabled", FieldType.BOOLEAN),
                FieldSpec("clickable", FieldType.BOOLEAN),
            )
        )
        reg(
            OpSpec(
                Operations.INTEGER_EXPRESSION,
                "INTEGER_EXPRESSION",
                false,
                true,
                FieldSpec("id", FieldType.INT),
                FieldSpec("mask", FieldType.INT),
                FieldSpec("length", FieldType.INT),
                FieldSpec("values", FieldType.INT_RPN),
            )
        )

        // Matrix
        reg(
            OpSpec(
                Operations.MATRIX_TRANSLATE,
                "MATRIX_TRANSLATE",
                FieldSpec("dx", FieldType.FLOAT),
                FieldSpec("dy", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.MATRIX_SCALE,
                "MATRIX_SCALE",
                FieldSpec("scaleX", FieldType.FLOAT),
                FieldSpec("scaleY", FieldType.FLOAT),
                FieldSpec("centerX", FieldType.FLOAT),
                FieldSpec("centerY", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.MATRIX_ROTATE,
                "MATRIX_ROTATE",
                FieldSpec("angle", FieldType.FLOAT),
                FieldSpec("centerX", FieldType.FLOAT),
                FieldSpec("centerY", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.MATRIX_SKEW,
                "MATRIX_SKEW",
                FieldSpec("skewX", FieldType.FLOAT),
                FieldSpec("skewY", FieldType.FLOAT),
            )
        )
        reg(OpSpec(Operations.MATRIX_SAVE, "MATRIX_SAVE"))
        reg(OpSpec(Operations.MATRIX_RESTORE, "MATRIX_RESTORE"))
        reg(
            OpSpec(
                Operations.MATRIX_CONSTANT,
                "MATRIX_CONSTANT",
                true,
                true,
                FieldSpec("id", FieldType.INT),
                FieldSpec("type", FieldType.INT),
                FieldSpec("values", FieldType.FLOAT_ARRAY),
            )
        )
        reg(
            OpSpec(
                Operations.MATRIX_EXPRESSION,
                "MATRIX_EXPRESSION",
                true,
                true,
                FieldSpec("id", FieldType.INT),
                FieldSpec("type", FieldType.INT),
                FieldSpec("expression", FieldType.FLOAT_RPN),
            )
        )

        // Path
        reg(
            OpSpec(
                Operations.PATH_TWEEN,
                "PATH_TWEEN",
                FieldSpec("outId", FieldType.INT),
                FieldSpec("pathId1", FieldType.INT),
                FieldSpec("pathId2", FieldType.INT),
                FieldSpec("tween", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.PATH_CREATE,
                "PATH_CREATE",
                FieldSpec("id", FieldType.INT),
                FieldSpec("startX", FieldType.FLOAT),
                FieldSpec("startY", FieldType.FLOAT),
            )
        )

        // Layout Components
        reg(OpSpec(Operations.LAYOUT_ROOT, "LAYOUT_ROOT", FieldSpec("componentId", FieldType.INT)))
        reg(
            OpSpec(
                Operations.LAYOUT_CONTENT,
                "LAYOUT_CONTENT",
                FieldSpec("componentId", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.COMPONENT_START,
                "COMPONENT_START",
                FieldSpec("type", FieldType.INT),
                FieldSpec("componentId", FieldType.INT),
                FieldSpec("x", FieldType.FLOAT),
                FieldSpec("y", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.LAYOUT_BOX,
                "LAYOUT_BOX",
                FieldSpec("componentId", FieldType.INT),
                FieldSpec("animationId", FieldType.INT),
                FieldSpec("horizontal", FieldType.INT),
                FieldSpec("vertical", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.LAYOUT_FIT_BOX,
                "LAYOUT_FIT_BOX",
                FieldSpec("componentId", FieldType.INT),
                FieldSpec("animationId", FieldType.INT),
                FieldSpec("horizontal", FieldType.INT),
                FieldSpec("vertical", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.LAYOUT_COLUMN,
                "LAYOUT_COLUMN",
                FieldSpec("componentId", FieldType.INT),
                FieldSpec("animationId", FieldType.INT),
                FieldSpec("horizontal", FieldType.INT),
                FieldSpec("vertical", FieldType.INT),
                FieldSpec("spacedBy", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.LAYOUT_COLLAPSIBLE_COLUMN,
                "LAYOUT_COLLAPSIBLE_COLUMN",
                FieldSpec("componentId", FieldType.INT),
                FieldSpec("animationId", FieldType.INT),
                FieldSpec("horizontal", FieldType.INT),
                FieldSpec("vertical", FieldType.INT),
                FieldSpec("spacedBy", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.LAYOUT_ROW,
                "LAYOUT_ROW",
                FieldSpec("componentId", FieldType.INT),
                FieldSpec("animationId", FieldType.INT),
                FieldSpec("horizontal", FieldType.INT),
                FieldSpec("vertical", FieldType.INT),
                FieldSpec("spacedBy", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.LAYOUT_COLLAPSIBLE_ROW,
                "LAYOUT_COLLAPSIBLE_ROW",
                FieldSpec("componentId", FieldType.INT),
                FieldSpec("animationId", FieldType.INT),
                FieldSpec("horizontal", FieldType.INT),
                FieldSpec("vertical", FieldType.INT),
                FieldSpec("spacedBy", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.LAYOUT_FLOW,
                "LAYOUT_FLOW",
                FieldSpec("componentId", FieldType.INT),
                FieldSpec("animationId", FieldType.INT),
                FieldSpec("horizontal", FieldType.INT),
                FieldSpec("vertical", FieldType.INT),
                FieldSpec("spacedBy", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.LAYOUT_TEXT,
                "LAYOUT_TEXT",
                FieldSpec("componentId", FieldType.INT),
                FieldSpec("animationId", FieldType.INT),
                FieldSpec("textId", FieldType.INT),
                FieldSpec("color", FieldType.INT),
                FieldSpec("fontSize", FieldType.FLOAT),
                FieldSpec("fontStyle", FieldType.INT),
                FieldSpec("fontWeight", FieldType.FLOAT),
                FieldSpec("fontFamilyId", FieldType.INT),
                FieldSpec("textAlign", FieldType.INT),
                FieldSpec("overflow", FieldType.INT),
                FieldSpec("maxLines", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.LAYOUT_IMAGE,
                "LAYOUT_IMAGE",
                FieldSpec("componentId", FieldType.INT),
                FieldSpec("animationId", FieldType.INT),
                FieldSpec("bitmapId", FieldType.INT),
                FieldSpec("scaleType", FieldType.INT),
                FieldSpec("alpha", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.LAYOUT_CANVAS,
                "LAYOUT_CANVAS",
                FieldSpec("componentId", FieldType.INT),
                FieldSpec("animationId", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.LAYOUT_CANVAS_CONTENT,
                "LAYOUT_CANVAS_CONTENT",
                FieldSpec("componentId", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.LAYOUT_STATE,
                "LAYOUT_STATE",
                FieldSpec("componentId", FieldType.INT),
                FieldSpec("animationId", FieldType.INT),
                FieldSpec("horizontal", FieldType.INT),
                FieldSpec("vertical", FieldType.INT),
                FieldSpec("indexId", FieldType.INT),
            )
        )
        reg(OpSpec(Operations.CONTAINER_END, "CONTAINER_END"))

        // Modifiers
        reg(
            OpSpec(
                Operations.MODIFIER_PADDING,
                "MODIFIER_PADDING",
                FieldSpec("left", FieldType.FLOAT),
                FieldSpec("top", FieldType.FLOAT),
                FieldSpec("right", FieldType.FLOAT),
                FieldSpec("bottom", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.MODIFIER_WIDTH,
                "MODIFIER_WIDTH",
                FieldSpec("type", FieldType.INT),
                FieldSpec("width", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.MODIFIER_HEIGHT,
                "MODIFIER_HEIGHT",
                FieldSpec("type", FieldType.INT),
                FieldSpec("height", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.MODIFIER_WIDTH_IN,
                "MODIFIER_WIDTH_IN",
                FieldSpec("min", FieldType.FLOAT),
                FieldSpec("max", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.MODIFIER_HEIGHT_IN,
                "MODIFIER_HEIGHT_IN",
                FieldSpec("min", FieldType.FLOAT),
                FieldSpec("max", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.MODIFIER_COLLAPSIBLE_PRIORITY,
                "MODIFIER_COLLAPSIBLE_PRIORITY",
                FieldSpec("orientation", FieldType.INT),
                FieldSpec("priority", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.MODIFIER_VISIBILITY,
                "MODIFIER_VISIBILITY",
                FieldSpec("visibilityId", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.MODIFIER_SCROLL,
                "MODIFIER_SCROLL",
                false,
                true,
                FieldSpec("direction", FieldType.INT),
                FieldSpec("position", FieldType.FLOAT),
                FieldSpec("max", FieldType.FLOAT),
                FieldSpec("notchMax", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.MODIFIER_MARQUEE,
                "MODIFIER_MARQUEE",
                FieldSpec("iterations", FieldType.INT),
                FieldSpec("animationMode", FieldType.INT),
                FieldSpec("repeatDelayMillis", FieldType.FLOAT),
                FieldSpec("initialDelayMillis", FieldType.FLOAT),
                FieldSpec("spacing", FieldType.FLOAT),
                FieldSpec("velocity", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.MODIFIER_OFFSET,
                "MODIFIER_OFFSET",
                FieldSpec("x", FieldType.FLOAT),
                FieldSpec("y", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.MODIFIER_ZINDEX,
                "MODIFIER_ZINDEX",
                FieldSpec("zIndex", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.MODIFIER_BACKGROUND,
                "MODIFIER_BACKGROUND",
                FieldSpec("flags", FieldType.INT),
                FieldSpec("colorId", FieldType.INT),
                FieldSpec("reserve1", FieldType.INT),
                FieldSpec("reserve2", FieldType.INT),
                FieldSpec("r", FieldType.FLOAT),
                FieldSpec("g", FieldType.FLOAT),
                FieldSpec("b", FieldType.FLOAT),
                FieldSpec("a", FieldType.FLOAT),
                FieldSpec("shapeType", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.MODIFIER_BORDER,
                "MODIFIER_BORDER",
                FieldSpec("flags", FieldType.INT),
                FieldSpec("colorId", FieldType.INT),
                FieldSpec("reserve1", FieldType.INT),
                FieldSpec("reserve2", FieldType.INT),
                FieldSpec("borderWidth", FieldType.FLOAT),
                FieldSpec("roundedCorner", FieldType.FLOAT),
                FieldSpec("r", FieldType.FLOAT),
                FieldSpec("g", FieldType.FLOAT),
                FieldSpec("b", FieldType.FLOAT),
                FieldSpec("a", FieldType.FLOAT),
                FieldSpec("shapeType", FieldType.INT),
            )
        )
        reg(OpSpec(Operations.MODIFIER_CLIP_RECT, "MODIFIER_CLIP_RECT"))
        reg(OpSpec(Operations.MODIFIER_DRAW_CONTENT, "MODIFIER_DRAW_CONTENT"))
        reg(
            OpSpec(
                Operations.MODIFIER_ROUNDED_CLIP_RECT,
                "MODIFIER_ROUNDED_CLIP_RECT",
                FieldSpec("topStart", FieldType.FLOAT),
                FieldSpec("topEnd", FieldType.FLOAT),
                FieldSpec("bottomStart", FieldType.FLOAT),
                FieldSpec("bottomEnd", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.MODIFIER_ALIGN_BY,
                "MODIFIER_ALIGN_BY",
                FieldSpec("line", FieldType.FLOAT),
                FieldSpec("flags", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.LAYOUT_COMPUTE,
                "LAYOUT_COMPUTE",
                false,
                true,
                FieldSpec("type", FieldType.INT),
                FieldSpec("boundsId", FieldType.INT),
                FieldSpec("animateChanges", FieldType.BOOLEAN),
            )
        )
        reg(OpSpec(Operations.MODIFIER_RIPPLE, "MODIFIER_RIPPLE", false, true))
        reg(OpSpec(Operations.MODIFIER_CLICK, "MODIFIER_CLICK", false, true))
        reg(OpSpec(Operations.MODIFIER_TOUCH_DOWN, "MODIFIER_TOUCH_DOWN", false, true))
        reg(OpSpec(Operations.MODIFIER_TOUCH_UP, "MODIFIER_TOUCH_UP", false, true))
        reg(OpSpec(Operations.MODIFIER_TOUCH_CANCEL, "MODIFIER_TOUCH_CANCEL", false, true))

        // Actions
        reg(OpSpec(Operations.RUN_ACTION, "RUN_ACTION"))
        reg(OpSpec(Operations.HOST_ACTION, "HOST_ACTION", FieldSpec("actionId", FieldType.INT)))
        reg(
            OpSpec(
                Operations.VALUE_FLOAT_CHANGE_ACTION,
                "VALUE_FLOAT_CHANGE_ACTION",
                FieldSpec("targetValueId", FieldType.INT),
                FieldSpec("value", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.VALUE_INTEGER_CHANGE_ACTION,
                "VALUE_INTEGER_CHANGE_ACTION",
                FieldSpec("targetValueId", FieldType.INT),
                FieldSpec("value", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.VALUE_STRING_CHANGE_ACTION,
                "VALUE_STRING_CHANGE_ACTION",
                false,
                true,
                FieldSpec("targetValueId", FieldType.INT),
                FieldSpec("value", FieldType.UTF8),
            )
        )
        reg(
            OpSpec(
                Operations.VALUE_INTEGER_EXPRESSION_CHANGE_ACTION,
                "VALUE_INTEGER_EXPRESSION_CHANGE_ACTION",
                FieldSpec("targetValueId", FieldType.LONG),
                FieldSpec("valueExpressionId", FieldType.LONG),
            )
        )
        reg(
            OpSpec(
                Operations.VALUE_FLOAT_EXPRESSION_CHANGE_ACTION,
                "VALUE_FLOAT_EXPRESSION_CHANGE_ACTION",
                FieldSpec("targetValueId", FieldType.INT),
                FieldSpec("valueExpressionId", FieldType.INT),
            )
        )

        // Colors
        reg(
            OpSpec(
                Operations.COLOR_CONSTANT,
                "COLOR_CONSTANT",
                FieldSpec("id", FieldType.INT),
                FieldSpec("color", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.COLOR_EXPRESSIONS,
                "COLOR_EXPRESSIONS",
                false,
                true,
                FieldSpec("id", FieldType.INT),
                FieldSpec("p1", FieldType.INT),
                FieldSpec("p2", FieldType.INT),
                FieldSpec("p3", FieldType.INT),
                FieldSpec("p4", FieldType.INT),
            )
        )

        // Other Utilities
        reg(
            OpSpec(
                Operations.ANIMATION_SPEC,
                "ANIMATION_SPEC",
                FieldSpec("animationId", FieldType.INT),
                FieldSpec("motionDuration", FieldType.FLOAT),
                FieldSpec("motionEasingType", FieldType.INT),
                FieldSpec("visibilityDuration", FieldType.FLOAT),
                FieldSpec("visibilityEasingType", FieldType.INT),
                FieldSpec("enterAnimation", FieldType.INT),
                FieldSpec("exitAnimation", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.UPDATE_DYNAMIC_FLOAT_LIST,
                "UPDATE_DYNAMIC_FLOAT_LIST",
                FieldSpec("id", FieldType.INT),
                FieldSpec("index", FieldType.FLOAT),
                FieldSpec("value", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.TEXT_LOOKUP,
                "TEXT_LOOKUP",
                FieldSpec("id", FieldType.INT),
                FieldSpec("dataSet", FieldType.INT),
                FieldSpec("index", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.TEXT_LOOKUP_INT,
                "TEXT_LOOKUP_INT",
                FieldSpec("id", FieldType.INT),
                FieldSpec("dataSet", FieldType.INT),
                FieldSpec("index", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.DATA_MAP_LOOKUP,
                "DATA_MAP_LOOKUP",
                FieldSpec("id", FieldType.INT),
                FieldSpec("dataMapId", FieldType.INT),
                FieldSpec("stringId", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.TEXT_MEASURE,
                "TEXT_MEASURE",
                FieldSpec("id", FieldType.INT),
                FieldSpec("textId", FieldType.INT),
                FieldSpec("mode", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.TEXT_LENGTH,
                "TEXT_LENGTH",
                FieldSpec("id", FieldType.INT),
                FieldSpec("textId", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.TEXT_SUBTEXT,
                "TEXT_SUBTEXT",
                FieldSpec("textId", FieldType.INT),
                FieldSpec("srcId1", FieldType.INT),
                FieldSpec("start", FieldType.FLOAT),
                FieldSpec("len", FieldType.FLOAT),
            )
        )
        reg(
            OpSpec(
                Operations.COMPONENT_VALUE,
                "COMPONENT_VALUE",
                FieldSpec("type", FieldType.INT),
                FieldSpec("componentId", FieldType.INT),
                FieldSpec("id", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.DEBUG_MESSAGE,
                "DEBUG_MESSAGE",
                FieldSpec("textId", FieldType.INT),
                FieldSpec("value", FieldType.FLOAT),
                FieldSpec("flags", FieldType.INT),
            )
        )
        reg(OpSpec(Operations.HAPTIC_FEEDBACK, "HAPTIC_FEEDBACK", FieldSpec("type", FieldType.INT)))
        reg(OpSpec(Operations.WAKE_IN, "WAKE_IN", FieldSpec("wake", FieldType.FLOAT)))
        reg(
            OpSpec(
                Operations.COLOR_THEME,
                "COLOR_THEME",
                FieldSpec("id", FieldType.INT),
                FieldSpec("groupId", FieldType.INT),
                FieldSpec("lightModeIndex", FieldType.SHORT),
                FieldSpec("darkModeIndex", FieldType.SHORT),
                FieldSpec("lightModeFallback", FieldType.INT),
                FieldSpec("darkModeFallback", FieldType.INT),
            )
        )
        reg(
            OpSpec(
                Operations.PATH_COMBINE,
                "PATH_COMBINE",
                FieldSpec("outId", FieldType.INT),
                FieldSpec("pathId1", FieldType.INT),
                FieldSpec("pathId2", FieldType.INT),
                FieldSpec("operation", FieldType.BYTE),
            )
        )
        reg(
            OpSpec(
                Operations.MATRIX_FROM_PATH,
                "MATRIX_FROM_PATH",
                FieldSpec("pathId", FieldType.INT),
                FieldSpec("percent", FieldType.FLOAT),
                FieldSpec("vOffset", FieldType.FLOAT),
                FieldSpec("flags", FieldType.INT),
            )
        )
    }

    private fun reg(spec: OpSpec) {
        all.put(spec.opcode, spec)
    }

    /** Get OpSpec for a given opcode. */
    @JvmStatic
    fun get(opcode: Int): OpSpec {
        return all.get(opcode)!!
    }

    enum class FieldType {
        BYTE,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        UTF8,
        BUFFER,
        BOOLEAN,
        FLOAT_ARRAY,
        FLOAT_ARRAY_BASE64,
        INT_ARRAY,
        HEADER_BODY,
        GLYPH_ARRAY,
        KERNING_TABLE,
        FLOAT_RPN,
        INT_RPN,
        BITMAP_TEXT_ID,
        BITMAP_TEXT_GLYPH_SPACING,
    }

    class FieldSpec(val name: String, val type: FieldType)

    public class OpSpec(
        val opcode: Int,
        val name: String,
        val isVariable: Boolean,
        public val forceReconstruct: Boolean,
        vararg val fields: FieldSpec,
    ) {
        public fun forceReconstruct(): Boolean {
            return forceReconstruct
        }

        constructor(
            opcode: Int,
            name: String,
            vararg fields: FieldSpec,
        ) : this(opcode, name, false, false, *fields)

        constructor(
            opcode: Int,
            name: String,
            isVariable: Boolean,
            vararg fields: FieldSpec,
        ) : this(opcode, name, isVariable, false, *fields)

        val isFixedLength: Boolean
            /**
             * is this entry a fixed-length entry?
             *
             * @return true if all fields are fixed-length
             */
            get() {
                if (isVariable) return false
                for (f in fields) {
                    if (
                        f.type == FieldType.UTF8 ||
                            f.type == FieldType.BUFFER ||
                            f.type == FieldType.FLOAT_ARRAY ||
                            f.type == FieldType.FLOAT_ARRAY_BASE64 ||
                            f.type == FieldType.INT_ARRAY ||
                            f.type == FieldType.HEADER_BODY ||
                            f.type == FieldType.GLYPH_ARRAY ||
                            f.type == FieldType.KERNING_TABLE ||
                            f.type == FieldType.FLOAT_RPN ||
                            f.type == FieldType.INT_RPN
                    ) {
                        return false
                    }
                }
                return true
            }
    }
}
