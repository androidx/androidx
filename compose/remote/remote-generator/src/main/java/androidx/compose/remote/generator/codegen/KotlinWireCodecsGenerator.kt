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

package androidx.compose.remote.generator.codegen

import androidx.compose.remote.generator.OperationSchema
import androidx.compose.remote.generator.SchemaAnimationSpec
import androidx.compose.remote.generator.SchemaOperation
import androidx.compose.remote.generator.SchemaPathCommand
import androidx.compose.remote.generator.SchemaPathEffects
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FLOAT_ARRAY
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import java.util.Objects

/**
 * Generates schema-driven Kotlin protocol constants, bit-packers, and sub-wire codecs
 * (`FloatAnimationSpec`, `RemotePathEffect`, `RemotePathCommand`) directly from `operations.json`.
 */
internal class KotlinWireCodecsGenerator(private val schema: OperationSchema) {

    private val suppressNoByteOrShort =
        AnnotationSpec.builder(Suppress::class).addMember("%S", "NoByteOrShort").build()

    private val floatAnimationSpecClass =
        ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "FloatAnimationSpec")
    private val remotePathEffectClass =
        ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "RemotePathEffect")
    private val remotePathCommandClass =
        ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "RemotePathCommand")
    private val nanEncodingClass = ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "NanEncoding")

    /** Appends all schema-driven protocol constant objects and animation spec helpers to a file. */
    fun appendProtocolConstantsTo(fileBuilder: FileSpec.Builder) {
        buildIdEncodingObject()?.let { fileBuilder.addType(it) }
        buildDocumentHeaderTagsObject()?.let { fileBuilder.addType(it) }
        buildEnumObjectFromOp(
                objectName = "DimensionType",
                opName = "WidthModifierOperation",
                fieldName = "type",
                kdoc =
                    "Dimension sizing modes for width/height modifiers (`DimensionModifierOperation.Type`).",
            )
            ?.let { fileBuilder.addType(it) }
        buildEnumObjectFromOp(
                objectName = "DimensionConstraintAxis",
                opName = "DimensionConstraintsModifierOperation",
                fieldName = "type",
                kdoc = "Axis selector for `DimensionConstraintsModifierOperation`.",
                constType = BYTE,
            )
            ?.let { fileBuilder.addType(it) }
        buildLayoutPositioningObject()?.let { fileBuilder.addType(it) }
        buildClipRegionOpObject()?.let { fileBuilder.addType(it) }
        buildPathWindingObject()?.let { fileBuilder.addType(it) }
        buildBitmapFormatObject()?.let { fileBuilder.addType(it) }
        buildEnumObjectFromOp(
                objectName = "NamedVariableType",
                opName = "NamedVariable",
                fieldName = "varType",
                kdoc = "Variable type tags for `NamedVariable` (opcode 137).",
                nameTransform = { "${it}_TYPE" },
            )
            ?.let { fileBuilder.addType(it) }
        buildEnumObjectFromOp(
                objectName = "HostNamedActionType",
                opName = "HostNamedAction",
                fieldName = "type",
                kdoc = "Payload value type tags for `HostNamedActionOperation` (opcode 210).",
            )
            ?.let { fileBuilder.addType(it) }
        buildEnumObjectFromOp(
                objectName = "TextFromFloatFlags",
                opName = "TextFromFloat",
                fieldName = "flags",
                kdoc = "Formatting flags for `TextFromFloat` (opcode 135).",
            )
            ?.let { fileBuilder.addType(it) }
        buildEnumObjectFromOp(
                objectName = "DrawTextAnchoredFlags",
                opName = "DrawTextAnchored",
                fieldName = "flags",
                kdoc = "Alignment and measurement flags for `DrawTextAnchored` (opcode 133).",
            )
            ?.let { fileBuilder.addType(it) }
        buildTextLayoutFlagsObject()?.let { fileBuilder.addType(it) }
        buildEnumObjectFromOp(
                objectName = "ImageScaleType",
                opName = "ImageLayout",
                fieldName = "scaleType",
                kdoc =
                    "Image scaling modes matching `ImageScaling` for `ImageLayout` and `DrawBitmapScaled`.",
            )
            ?.let { fileBuilder.addType(it) }
        buildRootContentFlagsObject()?.let { fileBuilder.addType(it) }
        buildSemanticsConstantsObject()?.let { fileBuilder.addType(it) }
        buildEnumObjectFromOp(
                objectName = "SkipCondition",
                opName = "Skip",
                fieldName = "conditionType",
                kdoc = "Skip condition selectors for `Skip` (opcode 241).",
            )
            ?.let { fileBuilder.addType(it) }
        buildEnumObjectFromOp(
                objectName = "ThemeMode",
                opName = "Theme",
                fieldName = "themeId",
                kdoc = "Theme mode identifiers matching `Theme` (opcode 63).",
            )
            ?.let { fileBuilder.addType(it) }
        buildEnumObjectFromOp(
                objectName = "ConditionType",
                opName = "ConditionalOperations",
                fieldName = "type",
                kdoc = "Comparison condition types for `ConditionalOperations` (opcode 178).",
                constType = BYTE,
            )
            ?.let { fileBuilder.addType(it) }
        buildEnumObjectFromOp(
                objectName = "ColorAttributeType",
                opName = "ColorAttribute",
                fieldName = "type",
                kdoc = "Component attribute tags for `ColorAttribute` (opcode 180).",
                nameTransform = { it.removePrefix("COLOR_") },
            )
            ?.let { fileBuilder.addType(it) }
        buildEnumObjectFromOp(
                objectName = "TextTransformOperation",
                opName = "TextTransform",
                fieldName = "operation",
                kdoc = "Transformation tags for `TextTransform` (opcode 199).",
                nameTransform = { it.removePrefix("TEXT_") },
            )
            ?.let { fileBuilder.addType(it) }
        buildColorExpressionModesObject()?.let { fileBuilder.addType(it) }
        buildCustomPropertyTypeObject()?.let { fileBuilder.addType(it) }

        val animSpec = schema.animationSpec
        if (animSpec != null) {
            fileBuilder.addType(buildAnimationEasingObject(animSpec))
            fileBuilder.addType(buildFloatAnimationSpecClass(animSpec))
            fileBuilder.addFunction(buildIsSpringAnimationSpecFun())
            fileBuilder.addFunction(buildUnpackAnimationSpecFun())
            fileBuilder.addFunction(buildPackAnimationSpecFun())
            fileBuilder.addFunction(buildRawFloatArrayEqualsFun())
            fileBuilder.addFunction(buildRawFloatArrayHashFun())
        }
    }

    /**
     * Appends generated `RemotePathEffect` and `RemotePathCommand` codecs to `RemoteOperations.kt`.
     */
    fun appendDocumentSubWireCodecsTo(fileBuilder: FileSpec.Builder) {
        val pathEffects = schema.pathEffects
        if (pathEffects != null) {
            fileBuilder.addType(buildRemotePathEffectClass(pathEffects))
        }
        if (schema.pathCommands.isNotEmpty()) {
            fileBuilder.addType(buildRemotePathCommandClass())
            fileBuilder.addFunction(buildDecodePathCommandsFun())
            fileBuilder.addFunction(buildForEachPathCoordinateIndexFun())
            fileBuilder.addFunction(buildRemapPathCoordinatesFun())
        }
    }

    private fun buildIdEncodingObject(): TypeSpec? {
        val enc = schema.idEncoding ?: return null
        val builder =
            TypeSpec.objectBuilder("IdEncoding")
                .addKdoc(
                    "%L",
                    "Wire NaN-boxing, Long-ID, and pointer-dereference encoding constants generated from `operations.json`.",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)

        val intConsts =
            listOf(
                "NAN_PREFIX" to enc.nanPrefix,
                "NAN_EXPONENT" to enc.nanExponent,
                "PAYLOAD_MASK" to enc.payloadMask,
                "NO_ID" to enc.noId,
                "START_ID" to enc.startId,
                "REGION_MASK" to enc.regionMask,
                "REGION_SUB_ID_MASK" to enc.regionSubIdMask,
                "REGION_SYSTEM" to enc.regionSystem,
                "REGION_VARIABLE" to enc.regionVariable,
                "REGION_ARRAY" to enc.regionArray,
                "REGION_OPERATION" to enc.regionOperation,
                "START_ARRAY_ID" to (enc.regionArray + enc.startId),
                "PTR_DEREFERENCE" to enc.ptrDereference,
                "VALUE_MASK" to enc.valueMask,
                "MACRO_LOCAL_START_ID" to enc.macroLocalStartId,
                "MACRO_LOCAL_END_ID" to enc.macroLocalEndId,
            )
        for ((name, value) in intConsts) {
            builder.addProperty(
                PropertySpec.builder(name, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", value)
                    .build()
            )
        }
        builder.addProperty(
            PropertySpec.builder("LONG_ID_OFFSET", LONG, KModifier.PUBLIC, KModifier.CONST)
                .initializer("%LL", enc.longIdOffset)
                .build()
        )
        return builder.build()
    }

    private fun buildDocumentHeaderTagsObject(): TypeSpec? {
        val hpStruct = schema.structs["HeaderProperty"] ?: return null
        val keyEnum =
            hpStruct.fields
                .firstOrNull { it.name == "tag" }
                ?.bits
                ?.firstOrNull { it.name == "key" }
                ?.enumValues ?: return null

        val builder =
            TypeSpec.objectBuilder("DocumentHeaderTags")
                .addKdoc(
                    "%L",
                    "Property tag keys and flag values for `DocumentHeader` generated from `operations.json`.",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)

        for ((name, value) in keyEnum.entries.sortedBy { it.value }) {
            builder.addProperty(
                PropertySpec.builder(name, SHORT, KModifier.PUBLIC, KModifier.CONST)
                    .addAnnotation(suppressNoByteOrShort)
                    .initializer("%L", value)
                    .build()
            )
        }
        for (groupName in listOf("densityBehavior", "scroll", "compression")) {
            val group = schema.headerConstants[groupName] ?: continue
            for ((name, value) in group.entries.sortedBy { it.value }) {
                builder.addProperty(
                    PropertySpec.builder(name, INT, KModifier.PUBLIC, KModifier.CONST)
                        .initializer("%L", value)
                        .build()
                )
            }
        }
        builder.addFunction(
            FunSpec.builder("versionForApiLevel")
                .addKdoc(
                    "%L",
                    "Maps a RemoteCompose API level (`6`, `7`, `8`) to `(majorVersion, minorVersion)`.",
                )
                .addAnnotation(JvmStatic::class)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("apiLevel", INT)
                .returns(ClassName("kotlin", "Pair").parameterizedBy(INT, INT))
                .beginControlFlow("return when")
                .addStatement("apiLevel <= 6 -> 1 to 0")
                .addStatement("apiLevel == 7 -> 1 to 1")
                .addStatement("else -> 1 to 2")
                .endControlFlow()
                .build()
        )
        return builder.build()
    }

    private fun buildEnumObjectFromOp(
        objectName: String,
        opName: String,
        fieldName: String,
        kdoc: String,
        constType: TypeName = INT,
        nameTransform: (String) -> String = { it },
    ): TypeSpec? {
        val op = schema.operationsByName[opName] ?: return null
        val enumMap = op.fields.firstOrNull { it.name == fieldName }?.enumValues ?: return null
        val builder =
            TypeSpec.objectBuilder(objectName)
                .addKdoc("%L", kdoc)
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
        for ((k, v) in enumMap.entries.sortedWith(compareBy({ it.value }, { it.key }))) {
            val prop =
                PropertySpec.builder(nameTransform(k), constType, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", v)
            if (constType == BYTE || constType == SHORT) {
                prop.addAnnotation(suppressNoByteOrShort)
            }
            builder.addProperty(prop.build())
        }
        return builder.build()
    }

    private fun buildLayoutPositioningObject(): TypeSpec? {
        val combined = linkedMapOf<String, Int>()
        for (opName in listOf("RowLayout", "ColumnLayout")) {
            val op = schema.operationsByName[opName] ?: continue
            for (fn in listOf("horizontalPositioning", "verticalPositioning")) {
                val em = op.fields.firstOrNull { it.name == fn }?.enumValues ?: continue
                for ((k, v) in em) {
                    combined.putIfAbsent(k, v)
                }
            }
        }
        if (combined.isEmpty()) return null
        val builder =
            TypeSpec.objectBuilder("LayoutPositioning")
                .addKdoc(
                    "%L",
                    "Alignment and arrangement positioning constants for layout containers generated from `operations.json`.",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
        for ((k, v) in combined.entries.sortedBy { it.value }) {
            builder.addProperty(
                PropertySpec.builder(k, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", v)
                    .build()
            )
        }
        return builder.build()
    }

    private fun buildClipRegionOpObject(): TypeSpec? {
        val op = schema.operationsByName["ClipPath"] ?: return null
        val field = op.fields.firstOrNull { it.name == "idAndRegionOp" } ?: return null
        val pathIdBit = field.bits?.firstOrNull { it.name == "pathId" } ?: return null
        val regionBit = field.bits?.firstOrNull { it.name == "regionOp" } ?: return null
        val regionEnum = regionBit.enumValues ?: return null

        val builder =
            TypeSpec.objectBuilder("ClipRegionOp")
                .addKdoc(
                    "%L",
                    "Clip region operations and bit-packing helper for `ClipPath` (opcode 38).",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
        for ((k, v) in regionEnum.entries.sortedBy { it.value }) {
            builder.addProperty(
                PropertySpec.builder(k, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", v)
                    .build()
            )
        }
        builder.addProperty(
            PropertySpec.builder("PATH_ID_MASK", INT, KModifier.PUBLIC, KModifier.CONST)
                .initializer("%L", hexInt(pathIdBit.bitMask.toInt()))
                .build()
        )
        builder.addFunction(
            FunSpec.builder("pack")
                .addKdoc("%L", "Packs [pathId] and [regionOp] into `ClipPathNode.idAndRegionOp`.")
                .addAnnotation(JvmStatic::class)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("pathId", INT)
                .addParameter(
                    ParameterSpec.builder("regionOp", INT).defaultValue("INTERSECT").build()
                )
                .returns(INT)
                .addStatement(
                    "return (pathId and PATH_ID_MASK) or ((regionOp and %L) shl %L)",
                    hexInt(regionBit.bitMask.toInt()),
                    regionBit.lowBit,
                )
                .build()
        )
        return builder.build()
    }

    private fun hexInt(value: Int): String = "0x${value.toUInt().toString(16).uppercase()}"

    private fun buildPathWindingObject(): TypeSpec? {
        val op = schema.operationsByName["PathData"] ?: return null
        val field = op.fields.firstOrNull { it.name == "idAndWinding" } ?: return null
        val pathIdBit = field.bits?.firstOrNull { it.name == "pathId" } ?: return null
        val windingBit = field.bits?.firstOrNull { it.name == "winding" } ?: return null
        val windingEnum = windingBit.enumValues ?: return null

        val builder =
            TypeSpec.objectBuilder("PathWinding")
                .addKdoc(
                    "%L",
                    "Path fill winding rules and bit-packing helper for `PathData` (opcode 123).",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
        for ((k, v) in windingEnum.entries.sortedBy { it.value }) {
            builder.addProperty(
                PropertySpec.builder(k, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", v)
                    .build()
            )
        }
        builder.addProperty(
            PropertySpec.builder("PATH_ID_MASK", INT, KModifier.PUBLIC, KModifier.CONST)
                .initializer("%L", hexInt(pathIdBit.bitMask.toInt()))
                .build()
        )
        builder.addFunction(
            FunSpec.builder("pack")
                .addKdoc("%L", "Packs [pathId] and [winding] into `PathDataNode.idAndWinding`.")
                .addAnnotation(JvmStatic::class)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("pathId", INT)
                .addParameter(ParameterSpec.builder("winding", INT).defaultValue("WINDING").build())
                .returns(INT)
                .addStatement(
                    "return (pathId and PATH_ID_MASK) or ((winding and %L) shl %L)",
                    hexInt(windingBit.bitMask.toInt()),
                    windingBit.lowBit,
                )
                .build()
        )
        return builder.build()
    }

    private fun buildBitmapFormatObject(): TypeSpec? {
        val op = schema.operationsByName["BitmapData"] ?: return null
        val widthField = op.fields.firstOrNull { it.name == "widthAndType" } ?: return null
        val heightField = op.fields.firstOrNull { it.name == "heightAndEncoding" } ?: return null
        val widthBit = widthField.bits?.firstOrNull { it.name == "width" } ?: return null
        val typeBit = widthField.bits?.firstOrNull { it.name == "imageType" } ?: return null
        val encodingBit =
            heightField.bits?.firstOrNull { it.name == "bitmapEncoding" } ?: return null
        val typeEnum = typeBit.enumValues ?: return null
        val encodingEnum = encodingBit.enumValues ?: return null

        val builder =
            TypeSpec.objectBuilder("BitmapFormat")
                .addKdoc(
                    "%L",
                    "Image pixel types, encodings, and bit-packing helpers for `BitmapData` (opcode 101). " +
                        "Unpacking is provided directly by generated `BitmapDataNode.width`, `imageType`, `height`, and `bitmapEncoding`.",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
        for ((k, v) in typeEnum.entries.sortedBy { it.value }) {
            builder.addProperty(
                PropertySpec.builder(k, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", v)
                    .build()
            )
        }
        for ((k, v) in encodingEnum.entries.sortedBy { it.value }) {
            builder.addProperty(
                PropertySpec.builder(k, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", v)
                    .build()
            )
        }
        builder.addProperty(
            PropertySpec.builder("DIMENSION_MASK", INT, KModifier.PUBLIC, KModifier.CONST)
                .initializer("%L", hexInt(widthBit.bitMask.toInt()))
                .build()
        )
        builder.addProperty(
            PropertySpec.builder("TYPE_AND_ENCODING_MASK", INT, KModifier.PUBLIC, KModifier.CONST)
                .initializer("%L", hexInt(typeBit.bitMask.toInt()))
                .build()
        )
        builder.addFunction(
            FunSpec.builder("packWidthAndType")
                .addKdoc("%L", "Packs [width] and [imageType] into `BitmapDataNode.widthAndType`.")
                .addAnnotation(JvmStatic::class)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("width", INT)
                .addParameter(
                    ParameterSpec.builder("imageType", INT).defaultValue("TYPE_PNG_8888").build()
                )
                .returns(INT)
                .addStatement(
                    "return (width and DIMENSION_MASK) or ((imageType and TYPE_AND_ENCODING_MASK) shl %L)",
                    typeBit.lowBit,
                )
                .build()
        )
        builder.addFunction(
            FunSpec.builder("packHeightAndEncoding")
                .addKdoc(
                    "%L",
                    "Packs [height] and [encoding] into `BitmapDataNode.heightAndEncoding`.",
                )
                .addAnnotation(JvmStatic::class)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("height", INT)
                .addParameter(
                    ParameterSpec.builder("encoding", INT).defaultValue("ENCODING_INLINE").build()
                )
                .returns(INT)
                .addStatement(
                    "return (height and DIMENSION_MASK) or ((encoding and TYPE_AND_ENCODING_MASK) shl %L)",
                    encodingBit.lowBit,
                )
                .build()
        )
        builder.addFunction(
            FunSpec.builder("bytesPerPixel")
                .addKdoc(
                    "%L",
                    "Returns uncompressed in-memory bytes per pixel for [imageType]: `1` for [TYPE_PNG_ALPHA_8] " +
                        "(`ALPHA_8`) and `4` for all other formats.",
                )
                .addAnnotation(JvmStatic::class)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("imageType", INT)
                .returns(LONG)
                .addStatement("return if (imageType == TYPE_PNG_ALPHA_8) 1L else 4L")
                .build()
        )
        return builder.build()
    }

    private fun buildTextLayoutFlagsObject(): TypeSpec? {
        val op = schema.operationsByName["TextLayout"] ?: return null
        val alignEnum =
            op.fields
                .firstOrNull { it.name == "textAlign" }
                ?.bits
                ?.firstOrNull { it.name == "alignment" }
                ?.enumValues ?: return null
        val overflowEnum =
            op.fields.firstOrNull { it.name == "overflow" }?.enumValues ?: return null

        val builder =
            TypeSpec.objectBuilder("TextLayoutFlags")
                .addKdoc(
                    "%L",
                    "Text alignment, overflow, and color flags for `TextLayout` / `CoreText` / `TextStyle`.",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
        for ((k, v) in alignEnum.entries.sortedBy { it.value }) {
            builder.addProperty(
                PropertySpec.builder("TEXT_ALIGN_$k", INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", v)
                    .build()
            )
        }
        for ((k, v) in overflowEnum.entries.sortedBy { it.value }) {
            builder.addProperty(
                PropertySpec.builder(k, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", v)
                    .build()
            )
        }
        builder.addProperty(
            PropertySpec.builder("FLAG_IS_DYNAMIC_COLOR", INT, KModifier.PUBLIC, KModifier.CONST)
                .initializer("1")
                .build()
        )
        return builder.build()
    }

    private fun buildRootContentFlagsObject(): TypeSpec? {
        val op = schema.operationsByName["RootContentBehavior"] ?: return null
        val seen = linkedMapOf<String, Int>()
        for (f in op.fields) {
            val em = f.enumValues ?: continue
            for ((k, v) in em.entries.sortedBy { it.value }) {
                seen.putIfAbsent(k, v)
            }
        }
        if (seen.isEmpty()) return null
        val builder =
            TypeSpec.objectBuilder("RootContentFlags")
                .addKdoc(
                    "%L",
                    "Scroll, alignment, sizing, and scale constants for `RootContentBehavior` (opcode 65).",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
        for ((k, v) in seen) {
            builder.addProperty(
                PropertySpec.builder(k, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", v)
                    .build()
            )
        }
        return builder.build()
    }

    private fun buildSemanticsConstantsObject(): TypeSpec? {
        val op = schema.operationsByName["CoreSemantics"] ?: return null
        val roleEnum = op.fields.firstOrNull { it.name == "role" }?.enumValues ?: return null
        val modeEnum = op.fields.firstOrNull { it.name == "mode" }?.enumValues ?: return null
        val builder =
            TypeSpec.objectBuilder("SemanticsConstants")
                .addKdoc(
                    "%L",
                    "Accessibility role and merge-mode constants matching `AccessibleComponent` / `CoreSemantics`.",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
        for ((k, v) in roleEnum.entries.sortedBy { it.value }) {
            builder.addProperty(
                PropertySpec.builder(k, BYTE, KModifier.PUBLIC, KModifier.CONST)
                    .addAnnotation(suppressNoByteOrShort)
                    .initializer("%L", v)
                    .build()
            )
        }
        for ((k, v) in modeEnum.entries.sortedBy { it.value }) {
            builder.addProperty(
                PropertySpec.builder(k, BYTE, KModifier.PUBLIC, KModifier.CONST)
                    .addAnnotation(suppressNoByteOrShort)
                    .initializer("%L", v)
                    .build()
            )
        }
        return builder.build()
    }

    private fun buildColorExpressionModesObject(): TypeSpec? {
        val op = schema.operationsByName["ColorExpression"] ?: return null
        val modeEnum =
            op.fields
                .firstOrNull { it.name == "flagsAndMode" }
                ?.bits
                ?.firstOrNull { it.name == "mode" }
                ?.enumValues ?: return null
        val builder =
            TypeSpec.objectBuilder("ColorExpressionModes")
                .addKdoc(
                    "%L",
                    "Color expression mode constants and flag helpers for `ColorExpression` (opcode 134).",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
        for ((k, v) in modeEnum.entries.sortedBy { it.value }) {
            val constName = if (k == "IDARGB_MODE") "ID_ARGB_MODE" else k
            builder.addProperty(
                PropertySpec.builder(constName, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", v)
                    .build()
            )
        }
        builder.addFunction(
            FunSpec.builder("mode")
                .addAnnotation(JvmStatic::class)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("flagsAndMode", INT)
                .returns(INT)
                .addStatement("return flagsAndMode and 0xFF")
                .build()
        )
        builder.addFunction(
            FunSpec.builder("alphaPayload")
                .addAnnotation(JvmStatic::class)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("flagsAndMode", INT)
                .returns(INT)
                .addStatement("return (flagsAndMode ushr 16) and 0xFF")
                .build()
        )
        return builder.build()
    }

    private fun buildCustomPropertyTypeObject(): TypeSpec? {
        val cpStruct = schema.structs["CustomProperty"] ?: return null
        val dtEnum =
            cpStruct.fields.firstOrNull { it.name == "dataType" }?.enumValues ?: return null
        val builder =
            TypeSpec.objectBuilder("CustomPropertyType")
                .addKdoc(
                    "%L",
                    "Property type tags for `CustomProperty` generated from `operations.json`.",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
        for ((k, v) in dtEnum.entries.sortedBy { it.value }) {
            builder.addProperty(
                PropertySpec.builder(k, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", v)
                    .build()
            )
        }
        return builder.build()
    }

    private fun buildAnimationEasingObject(animSpec: SchemaAnimationSpec): TypeSpec {
        val builder =
            TypeSpec.objectBuilder("AnimationEasing")
                .addKdoc(
                    "%L",
                    "Easing curve constants and CSS cubic-bezier presets for `FloatExpression.animationSpec` generated from `operations.json`.",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)

        for (easing in animSpec.easings.sortedBy { it.id }) {
            builder.addProperty(
                PropertySpec.builder(easing.name, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", easing.id)
                    .build()
            )
        }
        for (easing in animSpec.easings.sortedBy { it.id }) {
            for (alias in easing.aliases) {
                builder.addProperty(
                    PropertySpec.builder(alias, INT, KModifier.PUBLIC, KModifier.CONST)
                        .initializer("%L", easing.name)
                        .build()
                )
            }
        }

        val ctrlFun =
            FunSpec.builder("cubicControlPoints")
                .addKdoc(
                    "%L",
                    "Returns the 4 CSS cubic-bezier control points `(x1, y1, x2, y2)` for preset cubic easing " +
                        "[type] matching `CubicEasing` in `remote-core`, or `null` if [type] is not a built-in cubic preset.",
                )
                .addAnnotation(JvmStatic::class)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("type", INT)
                .returns(FLOAT_ARRAY.copy(nullable = true))
                .beginControlFlow("return when (type)")
        for (easing in animSpec.easings.sortedBy { it.id }) {
            val pts = easing.cubicControlPoints ?: continue
            ctrlFun.addStatement(
                "%L -> floatArrayOf(%Lf, %Lf, %Lf, %Lf)",
                easing.name,
                pts[0],
                pts[1],
                pts[2],
                pts[3],
            )
        }
        ctrlFun.addStatement("else -> null")
        ctrlFun.endControlFlow()
        builder.addFunction(ctrlFun.build())

        return builder.build()
    }

    private fun buildFloatAnimationSpecClass(animSpec: SchemaAnimationSpec): TypeSpec {
        val bitsByName = animSpec.flagsBits.associateBy { it.name }
        val typeBit = bitsByName.getValue("type")
        val hasWrapBit = bitsByName.getValue("hasWrap")
        val hasInitBit = bitsByName.getValue("hasInit")
        val snapBit = bitsByName.getValue("directionalSnap")
        val propBit = bitsByName.getValue("isPropagate")
        val paramLenBit = bitsByName.getValue("paramLen")

        val ctor =
            FunSpec.constructorBuilder()
                .addParameter(ParameterSpec.builder("duration", FLOAT).defaultValue("1f").build())
                .addParameter(
                    ParameterSpec.builder("type", INT)
                        .defaultValue("AnimationEasing.CUBIC_STANDARD")
                        .build()
                )
                .addParameter(
                    ParameterSpec.builder("params", FLOAT_ARRAY)
                        .defaultValue("floatArrayOf()")
                        .build()
                )
                .addParameter(
                    ParameterSpec.builder("initialValue", FLOAT).defaultValue("Float.NaN").build()
                )
                .addParameter(
                    ParameterSpec.builder("wrap", FLOAT).defaultValue("Float.NaN").build()
                )
                .addParameter(
                    ParameterSpec.builder("directionalSnap", INT).defaultValue("0").build()
                )
                .addParameter(
                    ParameterSpec.builder("isPropagate", BOOLEAN).defaultValue("false").build()
                )
                .build()

        val companion =
            TypeSpec.companionObjectBuilder()
                .addModifiers(KModifier.PUBLIC)
                .addFunction(
                    FunSpec.builder("isSpring")
                        .addKdoc(
                            "%L",
                            "Returns true if [data] uses the `SpringStopEngine` wire layout (`data.size > 4 && data[0] == 0f`).",
                        )
                        .addAnnotation(JvmStatic::class)
                        .addModifiers(KModifier.PUBLIC)
                        .addParameter("data", FLOAT_ARRAY)
                        .returns(BOOLEAN)
                        .addStatement("return data.size > 4 && data[0] == 0f")
                        .build()
                )
                .addFunction(
                    FunSpec.builder("unpack")
                        .addKdoc(
                            "%L",
                            "Unpacks a `FloatArray`-encoded duration-based animation specification matching `FloatAnimation` in `remote-core` bit-for-bit.",
                        )
                        .addAnnotation(JvmStatic::class)
                        .addModifiers(KModifier.PUBLIC)
                        .addParameter("data", FLOAT_ARRAY)
                        .returns(floatAnimationSpecClass)
                        .addCode(
                            CodeBlock.builder()
                                .beginControlFlow("require(!isSpring(data))")
                                .addStatement(
                                    "%S",
                                    "Spring animation specs cannot be unpacked with unpackAnimationSpec; check isSpringAnimationSpec first.",
                                )
                                .endControlFlow()
                                .addStatement(
                                    "if (data.isEmpty()) return %T()",
                                    floatAnimationSpecClass,
                                )
                                .addStatement("val duration = data[0]")
                                .addStatement(
                                    "if (data.size == 1) return %T(duration = duration)",
                                    floatAnimationSpecClass,
                                )
                                .addStatement("val bits = data[1].toRawBits()")
                                .addStatement(
                                    "val type = %L",
                                    CodegenExprUtils.extractBitExpr(
                                        "bits",
                                        typeBit,
                                        ExprLang.KOTLIN,
                                    ),
                                )
                                .addStatement(
                                    "val hasWrap = (%L) != 0",
                                    CodegenExprUtils.extractBitExpr(
                                        "bits",
                                        hasWrapBit,
                                        ExprLang.KOTLIN,
                                    ),
                                )
                                .addStatement(
                                    "val hasInit = (%L) != 0",
                                    CodegenExprUtils.extractBitExpr(
                                        "bits",
                                        hasInitBit,
                                        ExprLang.KOTLIN,
                                    ),
                                )
                                .addStatement(
                                    "val directionalSnap = %L",
                                    CodegenExprUtils.extractBitExpr(
                                        "bits",
                                        snapBit,
                                        ExprLang.KOTLIN,
                                    ),
                                )
                                .addStatement(
                                    "val isPropagate = (%L) != 0",
                                    CodegenExprUtils.extractBitExpr(
                                        "bits",
                                        propBit,
                                        ExprLang.KOTLIN,
                                    ),
                                )
                                .addStatement(
                                    "val paramLen = %L",
                                    CodegenExprUtils.extractBitExpr(
                                        "bits",
                                        paramLenBit,
                                        ExprLang.KOTLIN,
                                    ),
                                )
                                .addStatement("val paramsEnd = 2 + paramLen")
                                .beginControlFlow("require(paramsEnd <= data.size)")
                                .addStatement(
                                    "%P",
                                    "Truncated animation spec: expected at least \$paramsEnd floats, got \${data.size}",
                                )
                                .endControlFlow()
                                .addStatement("val params = FloatArray(paramLen) { data[2 + it] }")
                                .addStatement("var off = paramsEnd")
                                .beginControlFlow("val initialValue = if (hasInit)")
                                .beginControlFlow("require(off < data.size)")
                                .addStatement(
                                    "%P",
                                    "Truncated animation spec: missing initialValue at offset \$off",
                                )
                                .endControlFlow()
                                .addStatement("data[off++]")
                                .nextControlFlow("else")
                                .addStatement("Float.NaN")
                                .endControlFlow()
                                .beginControlFlow("val wrap = if (hasWrap)")
                                .beginControlFlow("require(off < data.size)")
                                .addStatement(
                                    "%P",
                                    "Truncated animation spec: missing wrap at offset \$off",
                                )
                                .endControlFlow()
                                .addStatement("data[off]")
                                .nextControlFlow("else")
                                .addStatement("Float.NaN")
                                .endControlFlow()
                                .addStatement(
                                    "return %T(duration = duration, type = type, params = params, initialValue = initialValue, wrap = wrap, directionalSnap = directionalSnap, isPropagate = isPropagate)",
                                    floatAnimationSpecClass,
                                )
                                .build()
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("pack")
                        .addKdoc(
                            "%L",
                            "Packs a float animation specification into a `FloatArray` matching `RemoteComposeBuffer.packAnimation` / `FloatAnimation.packToFloatArray` bit-for-bit.",
                        )
                        .addAnnotation(JvmStatic::class)
                        .addAnnotation(JvmOverloads::class)
                        .addModifiers(KModifier.PUBLIC)
                        .addParameter(
                            ParameterSpec.builder("duration", FLOAT).defaultValue("1f").build()
                        )
                        .addParameter(
                            ParameterSpec.builder("type", INT)
                                .defaultValue("AnimationEasing.CUBIC_STANDARD")
                                .build()
                        )
                        .addParameter(
                            ParameterSpec.builder("spec", FLOAT_ARRAY.copy(nullable = true))
                                .defaultValue("null")
                                .build()
                        )
                        .addParameter(
                            ParameterSpec.builder("initialValue", FLOAT)
                                .defaultValue("Float.NaN")
                                .build()
                        )
                        .addParameter(
                            ParameterSpec.builder("wrap", FLOAT).defaultValue("Float.NaN").build()
                        )
                        .returns(FLOAT_ARRAY)
                        .addCode(
                            """
                            var count = 0
                            if (!initialValue.isNaN()) {
                                count++
                            }
                            if (spec != null) {
                                count++
                            }
                            if (spec != null || type != AnimationEasing.CUBIC_STANDARD) {
                                count++
                                count += spec?.size ?: 0
                            }
                            if (!initialValue.isNaN()) {
                                count++
                            }
                            if (!wrap.isNaN()) {
                                count++
                            }
                            if (duration != 1f || count > 0) {
                                count++
                            }
                            if (!wrap.isNaN() || !initialValue.isNaN()) {
                                count++
                            }

                            val ret = FloatArray(count)
                            var pos = 0
                            val specLen = spec?.size ?: 0

                            if (ret.isNotEmpty()) {
                                ret[pos++] = duration
                            }
                            if (ret.size > 1) {
                                val wrapBit = if (wrap.isNaN()) 0 else 1
                                val initBit = if (initialValue.isNaN()) 0 else 2
                                val bits = type or ((wrapBit or initBit) shl 8)
                                ret[pos++] = Float.fromBits((specLen shl 16) or bits)
                            }
                            if (specLen > 0 && spec != null) {
                                spec.copyInto(ret, destinationOffset = pos)
                                pos += specLen
                            }
                            if (!initialValue.isNaN()) {
                                ret[pos++] = initialValue
                            }
                            if (!wrap.isNaN()) {
                                ret[pos] = wrap
                            }
                            return ret
                            """
                                .trimIndent()
                        )
                        .build()
                )
                .build()

        return TypeSpec.classBuilder("FloatAnimationSpec")
            .addKdoc(
                "%L",
                "Unpacked duration-based float animation specification for `FloatExpression.animationSpec`, " +
                    "generated from `operations.json` and matching `FloatAnimation` in `remote-core`.\n\n" +
                    "Spring specs (detected via [isSpringAnimationSpec]) use a distinct wire layout.",
            )
            .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
            .addModifiers(KModifier.PUBLIC)
            .primaryConstructor(ctor)
            .addProperty(
                PropertySpec.builder("duration", FLOAT, KModifier.PUBLIC)
                    .initializer("duration")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("type", INT, KModifier.PUBLIC).initializer("type").build()
            )
            .addProperty(
                PropertySpec.builder("backingParams", FLOAT_ARRAY, KModifier.PRIVATE)
                    .initializer("params.copyOf()")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("params", FLOAT_ARRAY, KModifier.PUBLIC)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return backingParams.copyOf()")
                            .build()
                    )
                    .build()
            )
            .addProperty(
                PropertySpec.builder("initialValue", FLOAT, KModifier.PUBLIC)
                    .initializer("initialValue")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("wrap", FLOAT, KModifier.PUBLIC).initializer("wrap").build()
            )
            .addProperty(
                PropertySpec.builder("directionalSnap", INT, KModifier.PUBLIC)
                    .initializer("directionalSnap")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("isPropagate", BOOLEAN, KModifier.PUBLIC)
                    .initializer("isPropagate")
                    .build()
            )
            .addFunction(
                FunSpec.builder("equals")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("other", ClassName("kotlin", "Any").copy(nullable = true))
                    .returns(BOOLEAN)
                    .addStatement(
                        "return other is %T && duration.toRawBits() == other.duration.toRawBits() && " +
                            "type == other.type && rawFloatArrayEquals(backingParams, other.backingParams) && " +
                            "initialValue.toRawBits() == other.initialValue.toRawBits() && " +
                            "wrap.toRawBits() == other.wrap.toRawBits() && " +
                            "directionalSnap == other.directionalSnap && isPropagate == other.isPropagate",
                        floatAnimationSpecClass,
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder("hashCode")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(INT)
                    .addStatement(
                        "return %T.hash(duration.toRawBits(), type, rawFloatArrayHash(backingParams), " +
                            "initialValue.toRawBits(), wrap.toRawBits(), directionalSnap, isPropagate)",
                        Objects::class,
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder("toString")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(STRING)
                    .addStatement(
                        "return %P",
                        "FloatAnimationSpec(duration=\$duration, type=\$type, params=\${backingParams.contentToString()}, initialValue=\$initialValue, wrap=\$wrap, directionalSnap=\$directionalSnap, isPropagate=\$isPropagate)",
                    )
                    .build()
            )
            .addType(companion)
            .build()
    }

    private fun buildIsSpringAnimationSpecFun(): FunSpec =
        FunSpec.builder("isSpringAnimationSpec")
            .addKdoc(
                "%L",
                "Returns true if [data] uses the `SpringStopEngine` wire layout (`data.size > 4 && data[0] == 0f`) " +
                    "rather than a duration-based `FloatAnimation` spec.",
            )
            .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
            .addModifiers(KModifier.PUBLIC)
            .addParameter("data", FLOAT_ARRAY)
            .returns(BOOLEAN)
            .addStatement("return %T.isSpring(data)", floatAnimationSpecClass)
            .build()

    private fun buildUnpackAnimationSpecFun(): FunSpec =
        FunSpec.builder("unpackAnimationSpec")
            .addKdoc(
                "%L",
                "Unpacks a `FloatArray`-encoded duration-based animation specification matching `FloatAnimation` in `remote-core` bit-for-bit.",
            )
            .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
            .addModifiers(KModifier.PUBLIC)
            .addParameter("data", FLOAT_ARRAY)
            .returns(floatAnimationSpecClass)
            .addStatement("return %T.unpack(data)", floatAnimationSpecClass)
            .build()

    private fun buildPackAnimationSpecFun(): FunSpec =
        FunSpec.builder("packAnimationSpec")
            .addKdoc(
                "%L",
                "Packs a float animation specification into a `FloatArray` matching `RemoteComposeBuffer.packAnimation` / `FloatAnimation.packToFloatArray` bit-for-bit.",
            )
            .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
            .addAnnotation(JvmOverloads::class)
            .addModifiers(KModifier.PUBLIC)
            .addParameter(ParameterSpec.builder("duration", FLOAT).defaultValue("1f").build())
            .addParameter(
                ParameterSpec.builder("type", INT)
                    .defaultValue("AnimationEasing.CUBIC_STANDARD")
                    .build()
            )
            .addParameter(
                ParameterSpec.builder("spec", FLOAT_ARRAY.copy(nullable = true))
                    .defaultValue("null")
                    .build()
            )
            .addParameter(
                ParameterSpec.builder("initialValue", FLOAT).defaultValue("Float.NaN").build()
            )
            .addParameter(ParameterSpec.builder("wrap", FLOAT).defaultValue("Float.NaN").build())
            .returns(FLOAT_ARRAY)
            .addStatement(
                "return %T.pack(duration, type, spec, initialValue, wrap)",
                floatAnimationSpecClass,
            )
            .build()

    private fun buildRawFloatArrayEqualsFun(): FunSpec =
        FunSpec.builder("rawFloatArrayEquals")
            .addModifiers(KModifier.INTERNAL)
            .addParameter("a", FLOAT_ARRAY.copy(nullable = true))
            .addParameter("b", FLOAT_ARRAY.copy(nullable = true))
            .returns(BOOLEAN)
            .addCode(
                """
                if (a === b) return true
                if (a == null || b == null || a.size != b.size) return false
                for (i in a.indices) {
                    if (a[i].toRawBits() != b[i].toRawBits()) return false
                }
                return true
                """
                    .trimIndent()
            )
            .build()

    private fun buildRawFloatArrayHashFun(): FunSpec =
        FunSpec.builder("rawFloatArrayHash")
            .addModifiers(KModifier.INTERNAL)
            .addParameter("a", FLOAT_ARRAY.copy(nullable = true))
            .returns(INT)
            .addCode(
                """
                if (a == null) return 0
                var result = 1
                for (v in a) {
                    result = 31 * result + v.toRawBits()
                }
                return result
                """
                    .trimIndent()
            )
            .build()

    private fun buildRemotePathEffectClass(pathEffects: SchemaPathEffects): TypeSpec {
        val mvf = KotlinGenerator.MAYBE_VARIABLE_FLOAT_CLASS
        val transformType = LambdaTypeName.get(parameters = arrayOf(mvf), returnType = mvf)

        val dashClass =
            TypeSpec.classBuilder("Dash")
                .addModifiers(KModifier.PUBLIC)
                .superclass(remotePathEffectClass)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("phase", mvf)
                        .addParameter("intervals", FLOAT_ARRAY)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("phase", mvf, KModifier.PUBLIC)
                        .initializer("phase")
                        .build()
                )
                .addInitializerBlock(
                    CodeBlock.builder()
                        .beginControlFlow("require(intervals.size <= MAX_DASH_INTERVALS)")
                        .addStatement(
                            "%P",
                            "Dash interval count (\${intervals.size}) exceeds MAX_DASH_INTERVALS (\$MAX_DASH_INTERVALS)",
                        )
                        .endControlFlow()
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("backingIntervals", FLOAT_ARRAY, KModifier.PRIVATE)
                        .initializer("intervals.copyOf()")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("intervals", FLOAT_ARRAY, KModifier.PUBLIC)
                        .getter(
                            FunSpec.getterBuilder()
                                .addStatement("return backingIntervals.copyOf()")
                                .build()
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("intervalCount", INT, KModifier.PUBLIC)
                        .getter(
                            FunSpec.getterBuilder()
                                .addStatement("return backingIntervals.size")
                                .build()
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("intervalAt")
                        .addModifiers(KModifier.PUBLIC)
                        .addParameter("index", INT)
                        .returns(mvf)
                        .addStatement(
                            "return %T.fromRawBits(backingIntervals[index].toRawBits())",
                            mvf,
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("encode")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(FLOAT_ARRAY)
                        .addCode(
                            """
                            val out = FloatArray(backingIntervals.size + 3)
                            out[0] = Float.fromBits(TYPE_DASH)
                            out[1] = phase.floatValue
                            out[2] = Float.fromBits(backingIntervals.size)
                            backingIntervals.copyInto(out, destinationOffset = 3)
                            return out
                            """
                                .trimIndent()
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("remapFloatValues")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("transform", transformType)
                        .returns(
                            ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "RemotePathEffect", "Dash")
                        )
                        .addCode(
                            CodeBlock.builder()
                                .addStatement(
                                    "val mappedIntervals = FloatArray(backingIntervals.size) { idx -> transform(%T.fromRawBits(backingIntervals[idx].toRawBits())).floatValue }",
                                    mvf,
                                )
                                .addStatement(
                                    "return Dash(phase = transform(phase), intervals = mappedIntervals)"
                                )
                                .build()
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("equals")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("other", ClassName("kotlin", "Any").copy(nullable = true))
                        .returns(BOOLEAN)
                        .addStatement(
                            "return other is Dash && phase == other.phase && floatArrayRawEquals(backingIntervals, other.backingIntervals)"
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("hashCode")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(INT)
                        .addStatement(
                            "return %T.hash(phase, floatArrayRawHashCode(backingIntervals))",
                            Objects::class,
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("toString")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(STRING)
                        .addStatement(
                            "return %P",
                            "RemotePathEffect.Dash(phase=\$phase, intervals=\${backingIntervals.contentToString()})",
                        )
                        .build()
                )
                .build()

        val discreteClass =
            TypeSpec.classBuilder("Discrete")
                .addModifiers(KModifier.PUBLIC)
                .superclass(remotePathEffectClass)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("segmentLength", mvf)
                        .addParameter("deviation", mvf)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("segmentLength", mvf, KModifier.PUBLIC)
                        .initializer("segmentLength")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("deviation", mvf, KModifier.PUBLIC)
                        .initializer("deviation")
                        .build()
                )
                .addFunction(
                    FunSpec.builder("encode")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(FLOAT_ARRAY)
                        .addStatement(
                            "return floatArrayOf(Float.fromBits(TYPE_DISCRETE), segmentLength.floatValue, deviation.floatValue)"
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("remapFloatValues")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("transform", transformType)
                        .returns(
                            ClassName(
                                KotlinGenerator.DOCUMENT_PACKAGE,
                                "RemotePathEffect",
                                "Discrete",
                            )
                        )
                        .addStatement(
                            "return Discrete(segmentLength = transform(segmentLength), deviation = transform(deviation))"
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("equals")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("other", ClassName("kotlin", "Any").copy(nullable = true))
                        .returns(BOOLEAN)
                        .addStatement(
                            "return other is Discrete && segmentLength == other.segmentLength && deviation == other.deviation"
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("hashCode")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(INT)
                        .addStatement("return %T.hash(segmentLength, deviation)", Objects::class)
                        .build()
                )
                .addFunction(
                    FunSpec.builder("toString")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(STRING)
                        .addStatement(
                            "return %P",
                            "RemotePathEffect.Discrete(segmentLength=\$segmentLength, deviation=\$deviation)",
                        )
                        .build()
                )
                .build()

        val pathDashClass =
            TypeSpec.classBuilder("PathDash")
                .addModifiers(KModifier.PUBLIC)
                .superclass(remotePathEffectClass)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("shapeId", INT)
                        .addParameter("advance", mvf)
                        .addParameter("phase", mvf)
                        .addParameter("style", INT)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("shapeId", INT, KModifier.PUBLIC)
                        .initializer("shapeId")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("advance", mvf, KModifier.PUBLIC)
                        .initializer("advance")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("phase", mvf, KModifier.PUBLIC)
                        .initializer("phase")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("style", INT, KModifier.PUBLIC)
                        .initializer("style")
                        .build()
                )
                .addFunction(
                    FunSpec.builder("encode")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(FLOAT_ARRAY)
                        .addStatement(
                            "return floatArrayOf(Float.fromBits(TYPE_PATH_DASH), Float.fromBits(shapeId), advance.floatValue, phase.floatValue, Float.fromBits(style))"
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("remapFloatValues")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("transform", transformType)
                        .returns(
                            ClassName(
                                KotlinGenerator.DOCUMENT_PACKAGE,
                                "RemotePathEffect",
                                "PathDash",
                            )
                        )
                        .addStatement(
                            "return PathDash(shapeId = shapeId, advance = transform(advance), phase = transform(phase), style = style)"
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("equals")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("other", ClassName("kotlin", "Any").copy(nullable = true))
                        .returns(BOOLEAN)
                        .addStatement(
                            "return other is PathDash && shapeId == other.shapeId && advance == other.advance && phase == other.phase && style == other.style"
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("hashCode")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(INT)
                        .addStatement(
                            "return %T.hash(shapeId, advance, phase, style)",
                            Objects::class,
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("toString")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(STRING)
                        .addStatement(
                            "return %P",
                            "RemotePathEffect.PathDash(shapeId=\$shapeId, advance=\$advance, phase=\$phase, style=\$style)",
                        )
                        .build()
                )
                .build()

        fun buildBinaryComposite(
            name: String,
            typeConst: String,
            firstName: String,
            secondName: String,
        ): TypeSpec =
            TypeSpec.classBuilder(name)
                .addModifiers(KModifier.PUBLIC)
                .superclass(remotePathEffectClass)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter(firstName, remotePathEffectClass)
                        .addParameter(secondName, remotePathEffectClass)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(firstName, remotePathEffectClass, KModifier.PUBLIC)
                        .initializer(firstName)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(secondName, remotePathEffectClass, KModifier.PUBLIC)
                        .initializer(secondName)
                        .build()
                )
                .addFunction(
                    FunSpec.builder("encode")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(FLOAT_ARRAY)
                        .addCode(
                            """
                            val a = $firstName.encode()
                            val b = $secondName.encode()
                            val out = FloatArray(1 + a.size + b.size)
                            out[0] = Float.fromBits($typeConst)
                            a.copyInto(out, destinationOffset = 1)
                            b.copyInto(out, destinationOffset = 1 + a.size)
                            return out
                            """
                                .trimIndent()
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("remapFloatValues")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("transform", transformType)
                        .returns(
                            ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "RemotePathEffect", name)
                        )
                        .addStatement(
                            "return $name($firstName.remapFloatValues(transform), $secondName.remapFloatValues(transform))"
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("equals")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("other", ClassName("kotlin", "Any").copy(nullable = true))
                        .returns(BOOLEAN)
                        .addStatement(
                            "return other is $name && $firstName == other.$firstName && $secondName == other.$secondName"
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("hashCode")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(INT)
                        .addStatement("return %T.hash($firstName, $secondName)", Objects::class)
                        .build()
                )
                .addFunction(
                    FunSpec.builder("toString")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(STRING)
                        .addStatement(
                            "return %P",
                            "RemotePathEffect.$name($firstName=\$$firstName, $secondName=\$$secondName)",
                        )
                        .build()
                )
                .build()

        val companion = TypeSpec.companionObjectBuilder().addModifiers(KModifier.PUBLIC)
        for (pe in pathEffects.types.sortedBy { it.id }) {
            companion.addProperty(
                PropertySpec.builder(pe.constName, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", pe.id)
                    .build()
            )
        }
        companion.addProperty(
            PropertySpec.builder("MAX_DASH_INTERVALS", INT, KModifier.PUBLIC, KModifier.CONST)
                .initializer("%L", pathEffects.maxDashIntervals)
                .build()
        )
        companion.addProperty(
            PropertySpec.builder("MAX_NESTING_DEPTH", INT, KModifier.PRIVATE, KModifier.CONST)
                .initializer("%L", pathEffects.maxNestingDepth)
                .build()
        )
        companion.addFunction(
            FunSpec.builder("decode")
                .addKdoc(
                    "%L",
                    "Decodes a wire-encoded `FloatArray` into a [RemotePathEffect], or returns `null` if [data] is empty, truncated, exceeds nesting bounds, or contains trailing garbage.",
                )
                .addAnnotation(JvmStatic::class)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("data", FLOAT_ARRAY)
                .returns(remotePathEffectClass.copy(nullable = true))
                .addCode(
                    """
                    if (data.isEmpty()) return null
                    val cursor = IntArray(1)
                    val effect = parseOne(data, cursor, 0) ?: return null
                    return if (cursor[0] == data.size) effect else null
                    """
                        .trimIndent()
                )
                .build()
        )
        companion.addFunction(
            FunSpec.builder("parseOne")
                .addModifiers(KModifier.PRIVATE)
                .addParameter("data", FLOAT_ARRAY)
                .addParameter("cursor", INT_ARRAY)
                .addParameter("depth", INT)
                .returns(remotePathEffectClass.copy(nullable = true))
                .addCode(
                    CodeBlock.builder()
                        .addStatement("if (depth > MAX_NESTING_DEPTH) return null")
                        .addStatement("val idx = cursor[0]")
                        .addStatement("if (idx >= data.size) return null")
                        .addStatement("val tag = data[idx].toRawBits()")
                        .beginControlFlow("return when (tag)")
                        .beginControlFlow("TYPE_DASH ->")
                        .addStatement("if (idx + 3 > data.size) return null")
                        .addStatement("val phase = %T.fromRawBits(data[idx + 1].toRawBits())", mvf)
                        .addStatement("val count = data[idx + 2].toRawBits()")
                        .addStatement(
                            "if (count < 0 || count > MAX_DASH_INTERVALS || count > data.size - (idx + 3)) return null"
                        )
                        .addStatement("val intervals = FloatArray(count) { data[idx + 3 + it] }")
                        .addStatement("cursor[0] = idx + 3 + count")
                        .addStatement("Dash(phase = phase, intervals = intervals)")
                        .endControlFlow()
                        .beginControlFlow("TYPE_DISCRETE ->")
                        .addStatement("if (idx + 3 > data.size) return null")
                        .addStatement("cursor[0] = idx + 3")
                        .addStatement(
                            "Discrete(segmentLength = %T.fromRawBits(data[idx + 1].toRawBits()), deviation = %T.fromRawBits(data[idx + 2].toRawBits()))",
                            mvf,
                            mvf,
                        )
                        .endControlFlow()
                        .beginControlFlow("TYPE_PATH_DASH ->")
                        .addStatement("if (idx + 5 > data.size) return null")
                        .addStatement("cursor[0] = idx + 5")
                        .addStatement(
                            "PathDash(shapeId = data[idx + 1].toRawBits(), advance = %T.fromRawBits(data[idx + 2].toRawBits()), phase = %T.fromRawBits(data[idx + 3].toRawBits()), style = data[idx + 4].toRawBits())",
                            mvf,
                            mvf,
                        )
                        .endControlFlow()
                        .beginControlFlow("TYPE_SUM ->")
                        .addStatement("cursor[0] = idx + 1")
                        .addStatement(
                            "val first = parseOne(data, cursor, depth + 1) ?: return null"
                        )
                        .addStatement(
                            "val second = parseOne(data, cursor, depth + 1) ?: return null"
                        )
                        .addStatement("Sum(first, second)")
                        .endControlFlow()
                        .beginControlFlow("TYPE_COMPOSE ->")
                        .addStatement("cursor[0] = idx + 1")
                        .addStatement(
                            "val outer = parseOne(data, cursor, depth + 1) ?: return null"
                        )
                        .addStatement(
                            "val inner = parseOne(data, cursor, depth + 1) ?: return null"
                        )
                        .addStatement("Compose(outer, inner)")
                        .endControlFlow()
                        .addStatement("else -> null")
                        .endControlFlow()
                        .build()
                )
                .build()
        )

        return TypeSpec.classBuilder("RemotePathEffect")
            .addKdoc(
                "%L",
                "Pure-Kotlin sealed representation and wire codec for `PaintBundle` path effects generated from `operations.json` matching `PaintPathEffects` bit-for-bit.",
            )
            .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
            .addModifiers(KModifier.PUBLIC, KModifier.SEALED)
            .addFunction(
                FunSpec.builder("encode")
                    .addModifiers(KModifier.PUBLIC, KModifier.ABSTRACT)
                    .returns(FLOAT_ARRAY)
                    .build()
            )
            .addFunction(
                FunSpec.builder("remapFloatValues")
                    .addModifiers(KModifier.PUBLIC, KModifier.ABSTRACT)
                    .addParameter("transform", transformType)
                    .returns(remotePathEffectClass)
                    .build()
            )
            .addType(dashClass)
            .addType(discreteClass)
            .addType(pathDashClass)
            .addType(buildBinaryComposite("Sum", "TYPE_SUM", "first", "second"))
            .addType(buildBinaryComposite("Compose", "TYPE_COMPOSE", "outer", "inner"))
            .addType(companion.build())
            .build()
    }

    private fun pathCmdKotlinClassName(cmd: SchemaPathCommand): String =
        when (cmd.name) {
            "Move" -> "MoveTo"
            "Line" -> "LineTo"
            "Quadratic" -> "QuadTo"
            "Conic" -> "ConicTo"
            "Cubic" -> "CubicTo"
            else -> cmd.name
        }

    private fun buildRemotePathCommandClass(): TypeSpec {
        val mvf = KotlinGenerator.MAYBE_VARIABLE_FLOAT_CLASS
        val builder =
            TypeSpec.classBuilder("RemotePathCommand")
                .addKdoc(
                    "%L",
                    "Decoded vector path command from a RemoteCompose `PathDataNode` or `PathAppendNode` `FloatArray` stream, generated from `operations.json`.",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC, KModifier.SEALED)

        for ((_, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
            val subName = pathCmdKotlinClassName(cmd)
            if (cmd.fields.isEmpty()) {
                builder.addType(
                    TypeSpec.objectBuilder(subName)
                        .addModifiers(KModifier.PUBLIC)
                        .superclass(remotePathCommandClass)
                        .addFunction(
                            FunSpec.builder("toString")
                                .addModifiers(KModifier.OVERRIDE)
                                .returns(STRING)
                                .addStatement("return %S", subName)
                                .build()
                        )
                        .build()
                )
            } else {
                val ctor = FunSpec.constructorBuilder()
                val sub =
                    TypeSpec.classBuilder(subName)
                        .addModifiers(KModifier.PUBLIC)
                        .superclass(remotePathCommandClass)
                for (fl in cmd.fields) {
                    ctor.addParameter(fl.name, mvf)
                    sub.addProperty(
                        PropertySpec.builder(fl.name, mvf, KModifier.PUBLIC)
                            .initializer(fl.name)
                            .build()
                    )
                }
                sub.primaryConstructor(ctor.build())
                val eqParts = cmd.fields.joinToString(" && ") { "${it.name} == other.${it.name}" }
                val hashArgs = cmd.fields.joinToString(", ") { it.name }
                val strArgs =
                    cmd.fields.joinToString(", ") {
                        if (it.name == "weight") "w=\$${it.name}" else "\$${it.name}"
                    }
                sub.addFunction(
                    FunSpec.builder("equals")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("other", ClassName("kotlin", "Any").copy(nullable = true))
                        .returns(BOOLEAN)
                        .addStatement("return other is $subName && $eqParts")
                        .build()
                )
                sub.addFunction(
                    FunSpec.builder("hashCode")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(INT)
                        .addStatement("return %T.hash($hashArgs)", Objects::class)
                        .build()
                )
                sub.addFunction(
                    FunSpec.builder("toString")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(STRING)
                        .addStatement("return %P", "$subName($strArgs)")
                        .build()
                )
                builder.addType(sub.build())
            }
        }

        builder.addType(
            TypeSpec.classBuilder("Unknown")
                .addModifiers(KModifier.PUBLIC)
                .superclass(remotePathCommandClass)
                .primaryConstructor(FunSpec.constructorBuilder().addParameter("verb", INT).build())
                .addProperty(
                    PropertySpec.builder("verb", INT, KModifier.PUBLIC).initializer("verb").build()
                )
                .addFunction(
                    FunSpec.builder("equals")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("other", ClassName("kotlin", "Any").copy(nullable = true))
                        .returns(BOOLEAN)
                        .addStatement("return other is Unknown && verb == other.verb")
                        .build()
                )
                .addFunction(
                    FunSpec.builder("hashCode")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(INT)
                        .addStatement("return verb.hashCode()")
                        .build()
                )
                .addFunction(
                    FunSpec.builder("toString")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(STRING)
                        .addStatement("return %P", "Unknown(\$verb)")
                        .build()
                )
                .build()
        )
        builder.addType(
            TypeSpec.companionObjectBuilder()
                .addModifiers(KModifier.PUBLIC)
                .addFunction(
                    FunSpec.builder("decode")
                        .addKdoc(
                            "%L",
                            "Decodes a wire-encoded [pathData] stream into an ordered list of [RemotePathCommand]s.",
                        )
                        .addAnnotation(JvmStatic::class)
                        .addModifiers(KModifier.PUBLIC)
                        .addParameter("pathData", FLOAT_ARRAY)
                        .returns(LIST.parameterizedBy(remotePathCommandClass))
                        .addStatement("return decodePathCommands(pathData)")
                        .build()
                )
                .build()
        )
        return builder.build()
    }

    private fun buildDecodePathCommandsFun(): FunSpec {
        val mvf = KotlinGenerator.MAYBE_VARIABLE_FLOAT_CLASS
        val code = CodeBlock.builder()
        code.addStatement("val result = ArrayList<%T>()", remotePathCommandClass)
        code.addStatement("var i = 0")
        code.addStatement("val len = pathData.size")
        code.beginControlFlow("while (i < len)")
        code.addStatement("val tag = %T.idFromNan(pathData[i])", nanEncodingClass)
        code.beginControlFlow("when (tag)")
        for ((_, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
            val tagConst = SchemaOperation.toScreamingSnakeCase(cmd.name)
            val subName = pathCmdKotlinClassName(cmd)
            val totalFloats = 1 + cmd.leadingPaddingFloats + cmd.fields.size
            code.beginControlFlow("PathCommandTags.%L ->", tagConst)
            if (cmd.fields.isEmpty()) {
                code.addStatement("result.add(%T.%L)", remotePathCommandClass, subName)
                if (cmd.name == "Done") {
                    code.addStatement("break")
                } else {
                    code.addStatement("i += 1")
                }
            } else {
                code.beginControlFlow("if (i + %L > len)", totalFloats)
                code.addStatement("result.add(%T.Unknown(tag))", remotePathCommandClass)
                code.addStatement("break")
                code.endControlFlow()
                val argsCode = CodeBlock.builder()
                for ((fIdx, fl) in cmd.fields.withIndex()) {
                    if (fIdx > 0) argsCode.add(", ")
                    val off = 1 + cmd.leadingPaddingFloats + fIdx
                    argsCode.add(
                        "%L = %T.fromRawBits(pathData[i + %L].toRawBits())",
                        fl.name,
                        mvf,
                        off,
                    )
                }
                code.addStatement(
                    "result.add(%T.%L(%L))",
                    remotePathCommandClass,
                    subName,
                    argsCode.build(),
                )
                code.addStatement("i += %L", totalFloats)
            }
            code.endControlFlow()
        }
        code.beginControlFlow("else ->")
        code.addStatement("result.add(%T.Unknown(tag))", remotePathCommandClass)
        code.addStatement("break")
        code.endControlFlow()
        code.endControlFlow()
        code.endControlFlow()
        code.addStatement("return result")

        return FunSpec.builder("decodePathCommands")
            .addKdoc(
                "%L",
                "Decodes a wire-encoded [pathData] stream into an ordered list of [RemotePathCommand]s, generated from `operations.json`.",
            )
            .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
            .addModifiers(KModifier.PUBLIC)
            .addParameter("pathData", FLOAT_ARRAY)
            .returns(LIST.parameterizedBy(remotePathCommandClass))
            .addCode(code.build())
            .build()
    }

    private fun buildForEachPathCoordinateIndexFun(): FunSpec {
        val mvf = KotlinGenerator.MAYBE_VARIABLE_FLOAT_CLASS
        val onCoordType = LambdaTypeName.get(parameters = arrayOf(INT, mvf), returnType = UNIT)
        val code = CodeBlock.builder()
        code.addStatement("var i = 0")
        code.addStatement("val len = pathData.size")
        code.beginControlFlow("while (i < len)")
        code.beginControlFlow("when (%T.idFromNan(pathData[i]))", nanEncodingClass)
        for ((_, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
            val tagConst = SchemaOperation.toScreamingSnakeCase(cmd.name)
            val totalFloats = 1 + cmd.leadingPaddingFloats + cmd.fields.size
            code.beginControlFlow("PathCommandTags.%L ->", tagConst)
            if (cmd.fields.isNotEmpty()) {
                code.beginControlFlow("if (i + %L <= len)", totalFloats)
                for (fIdx in cmd.fields.indices) {
                    val off = 1 + cmd.leadingPaddingFloats + fIdx
                    code.addStatement(
                        "onCoordinate(i + %L, %T.fromRawBits(pathData[i + %L].toRawBits()))",
                        off,
                        mvf,
                        off,
                    )
                }
                code.endControlFlow()
                code.addStatement("i += %L", totalFloats)
            } else if (cmd.name == "Done") {
                code.addStatement("return")
            } else {
                code.addStatement("i += 1")
            }
            code.endControlFlow()
        }
        code.addStatement("else -> return")
        code.endControlFlow()
        code.endControlFlow()

        return FunSpec.builder("forEachPathCoordinateIndex")
            .addKdoc(
                "%L",
                "Visits every coordinate and weight slot index in [pathData], skipping command tags and leading padding slots.",
            )
            .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
            .addModifiers(KModifier.PUBLIC, KModifier.INLINE)
            .addParameter("pathData", FLOAT_ARRAY)
            .addParameter("onCoordinate", onCoordType)
            .addCode(code.build())
            .build()
    }

    private fun buildRemapPathCoordinatesFun(): FunSpec {
        val mvf = KotlinGenerator.MAYBE_VARIABLE_FLOAT_CLASS
        val transformType = LambdaTypeName.get(parameters = arrayOf(mvf), returnType = mvf)
        return FunSpec.builder("remapPathCoordinates")
            .addKdoc(
                "%L",
                "Returns a copy of [pathData] with every coordinate and weight slot mapped through [transform], preserving command tags and padding slots.",
            )
            .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
            .addModifiers(KModifier.PUBLIC, KModifier.INLINE)
            .addParameter("pathData", FLOAT_ARRAY)
            .addParameter("transform", transformType)
            .returns(FLOAT_ARRAY)
            .addCode(
                """
                val out = pathData.copyOf()
                forEachPathCoordinateIndex(out) { index, value ->
                    out[index] = transform(value).floatValue
                }
                return out
                """
                    .trimIndent()
            )
            .build()
    }
}
