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

import androidx.annotation.RestrictTo
import androidx.compose.remote.generator.AstProperty
import androidx.compose.remote.generator.DerivedExpr
import androidx.compose.remote.generator.OperationSchema
import androidx.compose.remote.generator.SchemaBitField
import androidx.compose.remote.generator.SchemaField
import androidx.compose.remote.generator.SchemaOperation
import androidx.compose.remote.generator.SchemaSparseParam
import androidx.compose.remote.generator.SchemaStruct
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.BYTE_ARRAY
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
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.SHORT_ARRAY
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.joinToCode
import java.util.Objects
import java.util.zip.Deflater

/**
 * Generator that transforms an [OperationSchema] into an AndroidX-compliant Kotlin AST:
 * - Sealed interface hierarchy with hidden private subclass (preventing binary compatibility
 *   breakage)
 * - Standard classes with explicit equals/hashCode/toString (avoiding data class ABI hazards)
 * - Declarative WireBuffer serialization and deserialization with zero dependency on `remote-core`
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class KotlinGenerator(private val schema: OperationSchema) {

    private val sharedSparseParams: List<SchemaSparseParam> = run {
        val sparseOps =
            schema.operations.filter {
                !it.sparseParameters.isNullOrEmpty() &&
                    (it.sparseCountType == null || it.sparseCountType == "ushort") &&
                    (it.sparseKeyType == null || it.sparseKeyType == "ubyte")
            }
        if (sparseOps.size < 2) {
            emptyList()
        } else {
            val candidates = sparseOps.first().sparseParameters.orEmpty()
            val common = candidates.filter { cand ->
                sparseOps.all { op ->
                    op.sparseParameters.orEmpty().any {
                        it.id == cand.id &&
                            it.name == cand.name &&
                            it.type == cand.type &&
                            (it.lengthType ?: "ushort") == (cand.lengthType ?: "ushort")
                    }
                }
            }
            if (common.size >= 4) common else emptyList()
        }
    }

    private val sharedSparseIds: Set<Int> = sharedSparseParams.map { it.id }.toSet()

    public fun generateAstNodes(): String {
        val fileBuilder = newFileBuilder("RemoteOperations")
        fileBuilder.addType(buildDocumentNodeSpec())
        fileBuilder.addType(buildOperationNodeSpec())

        for ((_, structDef) in schema.structs) {
            fileBuilder.addType(buildStructTypeSpec(structDef))
        }

        for (op in schema.operations) {
            fileBuilder.addType(buildOperationTypeSpec(op))
        }

        fileBuilder.addFunction(buildFloatArrayRawEqualsFunSpec())
        fileBuilder.addFunction(buildFloatArrayRawHashCodeFunSpec())

        if (sharedSparseParams.isNotEmpty()) {
            fileBuilder.addType(buildSharedSparseTypeSpec())
        }

        return renderFile(fileBuilder.build())
    }

    public fun generateProtocolConstants(): String =
        KotlinProtocolAndSinkGenerator(schema).generateProtocolConstants()

    public fun generateOperationSink(): String =
        KotlinProtocolAndSinkGenerator(schema).generateOperationSink()

    public fun generateWriterNodes(): String {
        val fileBuilder =
            newFileBuilder("RemoteOperationsWriter")
                .addAnnotation(
                    AnnotationSpec.builder(JvmName::class)
                        .addMember("%S", "RemoteOperationsWriter")
                        .build()
                )

        fileBuilder.addProperty(
            PropertySpec.builder(
                    "DEFAULT_DOCUMENT_API_LEVEL",
                    INT,
                    KModifier.INTERNAL,
                    KModifier.CONST,
                )
                .initializer("%L", 8)
                .build()
        )

        fileBuilder.addFunction(buildSaveToByteArrayFunSpec())
        fileBuilder.addFunction(buildSaveToBufferFunSpec())
        fileBuilder.addFunction(buildDocumentNodeWriteFunSpec())
        fileBuilder.addFunction(buildOperationNodeWriteFunSpec())
        fileBuilder.addType(buildRemoteDocumentSerializerSpec())

        for ((_, structDef) in schema.structs) {
            fileBuilder.addFunction(buildStructWriterFunSpec(structDef))
        }

        for (op in schema.operations) {
            fileBuilder.addFunction(buildOperationWriterFunSpec(op))
            fileBuilder.addFunction(buildWriteOperationHelperFunSpec(op))
        }

        return renderFile(fileBuilder.build())
    }

    private fun buildDocumentNodeSpec(): TypeSpec =
        TypeSpec.interfaceBuilder("DocumentNode")
            .addKdoc("%L", "Root AST node in a RemoteCompose document.")
            .addModifiers(KModifier.PUBLIC, KModifier.SEALED)
            .addType(
                TypeSpec.classBuilder("Hidden")
                    .addModifiers(KModifier.PRIVATE)
                    .addSuperinterface(DOCUMENT_NODE_CLASS)
                    .build()
            )
            .build()

    private fun buildOperationNodeSpec(): TypeSpec {
        val kdoc =
            """
            Base operation AST node in a RemoteCompose document.

            @property opcode unique wire protocol identifier for this operation
            @property operationName human-readable name of this operation
            @property profiles list of profile names this operation is supported in
            @property isContainer true if this operation opens a nested container scope
            @property isContainerEnd true if this operation closes an open container scope
            """
                .trimIndent()

        val hiddenSpec =
            TypeSpec.classBuilder("Hidden")
                .addModifiers(KModifier.PRIVATE)
                .addSuperinterface(OPERATION_NODE_CLASS)
                .addProperty(
                    PropertySpec.builder("opcode", INT, KModifier.OVERRIDE)
                        .getter(FunSpec.getterBuilder().addStatement("return -1").build())
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("operationName", STRING, KModifier.OVERRIDE)
                        .getter(FunSpec.getterBuilder().addStatement("return %S", "").build())
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                            "profiles",
                            LIST.parameterizedBy(STRING),
                            KModifier.OVERRIDE,
                        )
                        .getter(FunSpec.getterBuilder().addStatement("return emptyList()").build())
                        .build()
                )
                .build()

        val readFun =
            FunSpec.builder("read")
                .addKdoc(
                    "%L",
                    "Reads and parses an [OperationNode] from [buffer] matching [opcode].",
                )
                .addModifiers(KModifier.INTERNAL)
                .addParameter("opcode", INT)
                .addParameter("buffer", WIRE_BUFFER_CLASS)
                .addParameter(ParameterSpec.builder("apiLevel", INT).defaultValue("%L", 8).build())
                .returns(OPERATION_NODE_CLASS.copy(nullable = true))
                .apply {
                    beginControlFlow("return when (opcode)")
                    for (op in schema.operations) {
                        addStatement(
                            "%L -> %T.read(buffer, apiLevel)",
                            op.id,
                            ClassName(DOCUMENT_PACKAGE, "${op.name}Node"),
                        )
                    }
                    addStatement("else -> null")
                    endControlFlow()
                }
                .build()

        return TypeSpec.interfaceBuilder("OperationNode")
            .addKdoc("%L", kdoc)
            .addModifiers(KModifier.PUBLIC, KModifier.SEALED)
            .addSuperinterface(DOCUMENT_NODE_CLASS)
            .addProperty(PropertySpec.builder("opcode", INT, KModifier.PUBLIC).build())
            .addProperty(PropertySpec.builder("operationName", STRING, KModifier.PUBLIC).build())
            .addProperty(
                PropertySpec.builder("profiles", LIST.parameterizedBy(STRING), KModifier.PUBLIC)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("isContainer", BOOLEAN, KModifier.PUBLIC)
                    .getter(FunSpec.getterBuilder().addStatement("return false").build())
                    .build()
            )
            .addProperty(
                PropertySpec.builder("isContainerEnd", BOOLEAN, KModifier.PUBLIC)
                    .getter(FunSpec.getterBuilder().addStatement("return false").build())
                    .build()
            )
            .addType(hiddenSpec)
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addModifiers(KModifier.PUBLIC)
                    .addFunction(readFun)
                    .build()
            )
            .build()
    }

    private fun buildStructTypeSpec(structDef: SchemaStruct): TypeSpec {
        val className = structDef.name
        val classTypeName = ClassName(DOCUMENT_PACKAGE, className)
        val props = structDef.astProperties
        val enumByProp = structDef.fields.associate { it.name to it.enumValues }

        val builder =
            TypeSpec.classBuilder(className)
                .addKdoc(
                    "%L",
                    buildTypeKDoc(structDef.description, props, enumByProp, isStruct = true),
                )
                .addAnnotation(RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)

        configurePrimaryConstructorAndProperties(builder, props)
        addComputedBitProperties(builder, structDef.computedBitProperties, structDef.fields, props)
        builder.addFunction(buildEqualsFunSpec(classTypeName, props))
        builder.addFunction(buildHashCodeFunSpec(props, emptyFallback = "0"))
        builder.addFunction(buildToStringFunSpec(className, props))
        builder.addFunction(buildCopyFunSpec(classTypeName, props))
        builder.addType(
            TypeSpec.companionObjectBuilder()
                .addModifiers(KModifier.PUBLIC)
                .addFunction(
                    buildReadFunSpec(classTypeName, className, structDef.fields, null, props)
                )
                .build()
        )
        return builder.build()
    }

    private fun buildOperationTypeSpec(op: SchemaOperation): TypeSpec {
        val className = "${op.name}Node"
        val classTypeName = ClassName(DOCUMENT_PACKAGE, className)
        val props = op.astProperties
        val enumByProp = op.fields.associate { it.name to it.enumValues }

        val builder =
            TypeSpec.classBuilder(className)
                .addKdoc("%L", buildTypeKDoc(op.description, props, enumByProp, isStruct = false))
                .apply {
                    if (op.restrictToLibraryGroup) {
                        addAnnotation(RESTRICT_TO_ANNOTATION)
                    }
                }
                .addModifiers(KModifier.PUBLIC)
                .addSuperinterface(OPERATION_NODE_CLASS)

        configurePrimaryConstructorAndProperties(builder, props)

        builder.addProperty(
            PropertySpec.builder("opcode", INT, KModifier.OVERRIDE)
                .getter(FunSpec.getterBuilder().addStatement("return Companion.OPCODE").build())
                .build()
        )
        builder.addProperty(
            PropertySpec.builder("operationName", STRING, KModifier.OVERRIDE)
                .getter(FunSpec.getterBuilder().addStatement("return %S", op.name).build())
                .build()
        )
        builder.addProperty(
            PropertySpec.builder("profiles", LIST.parameterizedBy(STRING), KModifier.OVERRIDE)
                .getter(FunSpec.getterBuilder().addStatement("return Companion.profiles").build())
                .build()
        )
        if (op.isContainer) {
            builder.addProperty(
                PropertySpec.builder("isContainer", BOOLEAN, KModifier.OVERRIDE)
                    .getter(FunSpec.getterBuilder().addStatement("return true").build())
                    .build()
            )
        }
        if (op.isContainerEnd) {
            builder.addProperty(
                PropertySpec.builder("isContainerEnd", BOOLEAN, KModifier.OVERRIDE)
                    .getter(FunSpec.getterBuilder().addStatement("return true").build())
                    .build()
            )
        }

        addComputedBitProperties(builder, op.computedBitProperties, op.fields, props)
        builder.addFunction(buildEqualsFunSpec(classTypeName, props))
        builder.addFunction(buildHashCodeFunSpec(props, emptyFallback = "Companion.OPCODE"))
        builder.addFunction(buildToStringFunSpec(className, props))
        builder.addFunction(buildCopyFunSpec(classTypeName, props))

        val profileArgs = op.profiles.map { CodeBlock.of("%S", it) }.joinToCode(", ")
        val companion =
            TypeSpec.companionObjectBuilder()
                .addModifiers(KModifier.PUBLIC)
                .addProperty(
                    PropertySpec.builder("OPCODE", INT, KModifier.PUBLIC, KModifier.CONST)
                        .initializer("%L", op.id)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                            "profiles",
                            LIST.parameterizedBy(STRING),
                            KModifier.INTERNAL,
                        )
                        .initializer("listOf(%L)", profileArgs)
                        .build()
                )
                .addFunction(
                    buildReadFunSpec(
                        classTypeName,
                        className,
                        op.fields,
                        op.sparseParameters,
                        props,
                        op.sparseCountType,
                        op.sparseKeyType,
                    )
                )
                .build()

        builder.addType(companion)
        return builder.build()
    }

    private fun configurePrimaryConstructorAndProperties(
        builder: TypeSpec.Builder,
        props: List<AstProperty>,
    ) {
        if (props.isEmpty()) return

        val ctorBuilder = FunSpec.constructorBuilder()
        if (props.any { it.type == "byte" || it.type == "short" }) {
            ctorBuilder.addAnnotation(NO_BYTE_OR_SHORT_ANNOTATION)
        }

        for (p in props) {
            val cleanName = cleanIdentifier(p.name)
            val typeName = mapToKotlinTypeName(p.type, p.isNullable)
            val defaultVal = mapToKotlinDefault(p.type, p.isNullable, p.defaultValue)
            ctorBuilder.addParameter(
                ParameterSpec.builder(cleanName, typeName).defaultValue("%L", defaultVal).build()
            )
            builder.addProperty(
                PropertySpec.builder(cleanName, typeName, KModifier.PUBLIC)
                    .initializer("%N", cleanName)
                    .build()
            )
        }
        builder.primaryConstructor(ctorBuilder.build())
    }

    private fun addComputedBitProperties(
        builder: TypeSpec.Builder,
        computedBits: List<Pair<String, SchemaBitField>>,
        fields: List<SchemaField>,
        props: List<AstProperty>,
    ) {
        val byteNames = fields.filter { it.type == "byte" }.map { it.name }.toSet()
        val shortNames = fields.filter { it.type == "short" }.map { it.name }.toSet()
        val nullableNames = props.filter { it.isNullable }.map { it.name }.toSet()
        for ((parentName, bit) in computedBits) {
            val cleanBitName = cleanIdentifier(bit.name)
            val parentIdent = sanitizeIdentifier(parentName)
            val isNullable = parentName in nullableNames
            val baseIdent = if (isNullable) "it" else parentIdent
            val srcExpr =
                when (parentName) {
                    in byteNames -> "($baseIdent.toInt() and 0xFF)"
                    in shortNames -> "$baseIdent.toInt()"
                    else -> baseIdent
                }
            val expr = CodegenExprUtils.extractBitExpr(srcExpr, bit, ExprLang.KOTLIN)
            val unwrapped =
                if (expr.startsWith("(") && expr.endsWith(")")) {
                    expr.substring(1, expr.length - 1)
                } else {
                    expr
                }
            if (isNullable) {
                builder.addProperty(
                    PropertySpec.builder(cleanBitName, INT.copy(nullable = true), KModifier.PUBLIC)
                        .getter(
                            FunSpec.getterBuilder()
                                .addStatement("return %L?.let { %L }", parentIdent, unwrapped)
                                .build()
                        )
                        .build()
                )
            } else {
                builder.addProperty(
                    PropertySpec.builder(cleanBitName, INT, KModifier.PUBLIC)
                        .getter(
                            FunSpec.getterBuilder().addStatement("return %L", unwrapped).build()
                        )
                        .build()
                )
            }
        }
    }

    private fun buildEqualsFunSpec(classTypeName: ClassName, props: List<AstProperty>): FunSpec =
        FunSpec.builder("equals")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("other", ANY.copy(nullable = true))
            .returns(BOOLEAN)
            .apply {
                addStatement("if (this === other) return true")
                addStatement("if (other !is %T) return false", classTypeName)
                if (props.isEmpty()) {
                    addStatement("return true")
                } else {
                    val checks =
                        props.joinToString(" && ") { prop ->
                            val pName = sanitizeIdentifier(prop.name)
                            equalsCheckExpr(prop, pName, "other.$pName")
                        }
                    addStatement("return %L", checks)
                }
            }
            .build()

    private fun buildHashCodeFunSpec(props: List<AstProperty>, emptyFallback: String): FunSpec =
        FunSpec.builder("hashCode")
            .addModifiers(KModifier.OVERRIDE)
            .returns(INT)
            .apply {
                when {
                    props.isEmpty() -> addStatement("return %L", emptyFallback)
                    props.size == 1 -> {
                        val p = props[0]
                        val pName = sanitizeIdentifier(p.name)
                        val expr = hashArgExpr(p, pName)
                        if (expr != pName) {
                            addStatement("return %L", expr)
                        } else {
                            addStatement("return %T.hash(%L)", Objects::class, pName)
                        }
                    }
                    else -> {
                        val hashArgs =
                            props.joinToString(", ") {
                                hashArgExpr(it, sanitizeIdentifier(it.name))
                            }
                        addStatement("return %T.hash(%L)", Objects::class, hashArgs)
                    }
                }
            }
            .build()

    private fun buildToStringFunSpec(className: String, props: List<AstProperty>): FunSpec {
        val parts = props.map {
            val pName = sanitizeIdentifier(it.name)
            if (it.type in ARRAY_TO_STRING_TYPES) {
                "$pName=\${$pName.contentToString()}"
            } else {
                "$pName=\${$pName}"
            }
        }
        val fieldString = parts.joinToString(", ")
        val singleLiteral = "\"$className($fieldString)\""
        val expr =
            if (8 + singleLiteral.length <= 100) {
                singleLiteral
            } else {
                buildString {
                    append("\"$className(\" +\n")
                    for ((index, part) in parts.withIndex()) {
                        val suffix = if (index < parts.size - 1) ", \" +\n" else ")\""
                        append("\"$part$suffix")
                    }
                }
            }
        return FunSpec.builder("toString")
            .addModifiers(KModifier.OVERRIDE)
            .returns(STRING)
            .addStatement("return %L", expr)
            .build()
    }

    private fun buildCopyFunSpec(classTypeName: ClassName, props: List<AstProperty>): FunSpec {
        if (props.isEmpty()) {
            return FunSpec.builder("copy")
                .addKdoc("%L", "Returns a copy of this instance.")
                .addModifiers(KModifier.PUBLIC)
                .returns(classTypeName)
                .addStatement("return this")
                .build()
        }

        val builder =
            FunSpec.builder("copy")
                .addKdoc("%L", "Returns a copy with optional updated fields.")
                .addModifiers(KModifier.PUBLIC)
                .returns(classTypeName)

        if (props.any { it.type == "byte" || it.type == "short" }) {
            builder.addAnnotation(NO_BYTE_OR_SHORT_ANNOTATION)
        }

        for (p in props) {
            val cleanName = cleanIdentifier(p.name)
            val pName = sanitizeIdentifier(p.name)
            builder.addParameter(
                ParameterSpec.builder(cleanName, mapToKotlinTypeName(p.type, p.isNullable))
                    .defaultValue("this.%L", pName)
                    .build()
            )
        }

        val checks =
            props.joinToString(" && ") { prop ->
                val pName = sanitizeIdentifier(prop.name)
                equalsCheckExpr(prop, pName, "this.$pName")
            }
        builder.addStatement("if (%L) return this", checks)

        val ctorArgs =
            props.joinToString(", ") {
                val pName = sanitizeIdentifier(it.name)
                "$pName = $pName"
            }
        builder.addStatement("return %T(%L)", classTypeName, ctorArgs)
        return builder.build()
    }

    private fun buildReadFunSpec(
        classTypeName: ClassName,
        className: String,
        fields: List<SchemaField>,
        sparseParameters: List<SchemaSparseParam>?,
        props: List<AstProperty>,
        sparseCountType: String? = null,
        sparseKeyType: String? = null,
    ): FunSpec =
        FunSpec.builder("read")
            .addKdoc("%L", "Reads a [$className] from [buffer].")
            .addModifiers(KModifier.INTERNAL)
            .addParameter("buffer", WIRE_BUFFER_CLASS)
            .addParameter(ParameterSpec.builder("apiLevel", INT).defaultValue("%L", 8).build())
            .returns(classTypeName)
            .addCode(
                buildReadBodyCode(
                    classTypeName,
                    className,
                    fields,
                    sparseParameters,
                    props,
                    sparseCountType,
                    sparseKeyType,
                )
            )
            .build()

    private fun buildReadBodyCode(
        classTypeName: ClassName,
        className: String,
        fields: List<SchemaField>,
        sparseParameters: List<SchemaSparseParam>?,
        props: List<AstProperty>,
        sparseCountType: String? = null,
        sparseKeyType: String? = null,
    ): CodeBlock = buildCodeBlock {
        val byteNames = fields.filter { it.type == "byte" }.map { it.name }.toSet()
        val shortNames = fields.filter { it.type == "short" }.map { it.name }.toSet()

        for (f in fields) {
            val bits = f.bits
            if (bits != null && f.unpackBits) {
                val rawVar = "raw_${f.name}"
                addStatement("val %L = buffer.readInt()", rawVar)
                for (b in bits) {
                    val bName = sanitizeIdentifier(b.name)
                    val extract = CodegenExprUtils.extractBitExpr(rawVar, b, ExprLang.KOTLIN)
                    addStatement("val %L = %L", bName, extract)
                    if (b.strictEnum && !b.enumValues.isNullOrEmpty()) {
                        addStrictBitEnumCheck(this, className, b, bName)
                    }
                }
                continue
            }

            val fName = sanitizeIdentifier(f.name)
            val isNullable =
                f.derivedFrom == null &&
                    f.presentIf != null &&
                    !f.type.endsWith("[]") &&
                    f.defaultValue == null
            val fallback = mapToKotlinDefault(f.type, isNullable, f.defaultValue)

            if (f.presentIf != null) {
                val cond =
                    CodegenExprUtils.translateCondition(
                        f.presentIf,
                        ExprLang.KOTLIN,
                        needsKotlinIntCast = { it in shortNames },
                        isKotlinUnsignedByte = { it in byteNames },
                    ) {
                        sanitizeIdentifier(it)
                    }
                beginControlFlow("val %L = if (%L)", fName, cond)
                if (f.type.endsWith("[]")) {
                    addArrayReadLines(this, f)
                } else {
                    val readExpr = mapToKotlinScalarReadExpr(f.type, f.maxLength)
                    if (hasScalarReadValidations(f)) {
                        addStatement("val parsed = %L", readExpr)
                        addScalarReadValidations(this, className, f, "parsed")
                        addStatement("parsed")
                    } else {
                        addStatement("%L", readExpr)
                    }
                }
                nextControlFlow("else")
                addStatement("%L", fallback)
                endControlFlow()
            } else if (f.addedVersion != null) {
                val readExpr = mapToKotlinScalarReadExpr(f.type, f.maxLength)
                beginControlFlow(
                    "val %L = if (apiLevel >= %L && buffer.available())",
                    fName,
                    f.addedVersion,
                )
                if (hasScalarReadValidations(f)) {
                    addStatement("val parsed = %L", readExpr)
                    addScalarReadValidations(this, className, f, "parsed")
                    addStatement("parsed")
                } else {
                    addStatement("%L", readExpr)
                }
                nextControlFlow("else")
                addStatement("%L", fallback)
                endControlFlow()
            } else if (f.type.endsWith("[]")) {
                beginControlFlow("val %L = run", fName)
                addArrayReadLines(this, f)
                endControlFlow()
            } else {
                val readExpr = mapToKotlinScalarReadExpr(f.type, f.maxLength)
                addStatement("val %L = %L", fName, readExpr)
                addScalarReadValidations(this, className, f, fName)
            }
        }

        val usesSharedSparse =
            sparseParameters != null &&
                (sparseCountType == null || sparseCountType == "ushort") &&
                (sparseKeyType == null || sparseKeyType == "ubyte") &&
                sharedSparseParams.isNotEmpty() &&
                sharedSparseParams.all { s ->
                    sparseParameters.any {
                        it.id == s.id &&
                            it.name == s.name &&
                            it.type == s.type &&
                            (it.lengthType ?: "ushort") == (s.lengthType ?: "ushort")
                    }
                }

        if (sparseParameters != null && usesSharedSparse) {
            val uniqueSparse = sparseParameters.filter { it.id !in sharedSparseIds }
            for (sp in uniqueSparse) {
                addStatement(
                    "var %N: %T = null",
                    cleanIdentifier(sp.name),
                    mapToKotlinTypeName(sp.type, isNullable = true),
                )
            }
            addStatement("val sharedSparse = SharedSparseParams()")
            addStatement("val sparseCount = buffer.readShort() and 0xFFFF")
            beginControlFlow("require(sparseCount <= buffer.remaining() / 2)")
            addStatement(
                "%L",
                "\"Sparse count \$sparseCount exceeds buffer (\${buffer.remaining()})\"",
            )
            endControlFlow()
            beginControlFlow("repeat(sparseCount)")
            addStatement("val paramId = buffer.readByte() and 0xFF")
            beginControlFlow("if (!sharedSparse.read(paramId, buffer))")
            if (uniqueSparse.isEmpty()) {
                addStatement(
                    "throw %T(%L)",
                    IllegalArgumentException::class,
                    "\"Unknown sparse parameter ID: \$paramId\"",
                )
            } else {
                beginControlFlow("when (paramId)")
                for (sp in uniqueSparse) {
                    addSparseWhenBranch(this, sp)
                }
                addStatement(
                    "else -> throw %T(%L)",
                    IllegalArgumentException::class,
                    "\"Unknown sparse parameter ID: \$paramId\"",
                )
                endControlFlow()
            }
            endControlFlow()
            endControlFlow()

            val nonSharedNames = sharedSparseParams.map { sanitizeIdentifier(it.name) }.toSet()
            val nonSharedProps = props.filter { sanitizeIdentifier(it.name) !in nonSharedNames }
            val argsStr =
                nonSharedProps.joinToString(", ") {
                    val pName = sanitizeIdentifier(it.name)
                    "$pName = $pName"
                }
            addStatement("return sharedSparse.to%L(%L)", className, argsStr)
            return@buildCodeBlock
        }

        if (sparseParameters != null) {
            for (sp in sparseParameters) {
                addStatement(
                    "var %N: %T = null",
                    cleanIdentifier(sp.name),
                    mapToKotlinTypeName(sp.type, isNullable = true),
                )
            }
            val effectiveCountType = sparseCountType ?: "ushort"
            val effectiveKeyType = sparseKeyType ?: "ubyte"
            when (effectiveCountType) {
                "int" -> {
                    addStatement("val sparseCount = buffer.readInt()")
                    addStatement(
                        "require(sparseCount >= 0) { %L }",
                        "\"Negative sparse count: \$sparseCount\"",
                    )
                }
                "short" -> {
                    addStatement("val sparseCount = buffer.readShort().toShort().toInt()")
                    addStatement(
                        "require(sparseCount >= 0) { %L }",
                        "\"Negative sparse count: \$sparseCount\"",
                    )
                }
                "byte" -> {
                    addStatement("val sparseCount = buffer.readByte().toByte().toInt()")
                    addStatement(
                        "require(sparseCount >= 0) { %L }",
                        "\"Negative sparse count: \$sparseCount\"",
                    )
                }
                "ubyte" -> addStatement("val sparseCount = buffer.readByte() and 0xFF")
                else -> addStatement("val sparseCount = buffer.readShort() and 0xFFFF")
            }
            val minEntryBytes =
                when (effectiveKeyType) {
                    "int" -> 5
                    "short",
                    "ushort" -> 3
                    else -> 2
                }
            beginControlFlow("require(sparseCount <= buffer.remaining() / %L)", minEntryBytes)
            addStatement(
                "%L",
                "\"Sparse count \$sparseCount exceeds buffer (\${buffer.remaining()})\"",
            )
            endControlFlow()
            beginControlFlow("repeat(sparseCount)")
            val keyReadExpr =
                when (effectiveKeyType) {
                    "int" -> "buffer.readInt()"
                    "ushort" -> "buffer.readShort() and 0xFFFF"
                    "short" -> "buffer.readShort().toShort().toInt()"
                    "byte" -> "buffer.readByte().toByte().toInt()"
                    else -> "buffer.readByte() and 0xFF"
                }
            beginControlFlow("when (val paramId = %L)", keyReadExpr)
            for (sp in sparseParameters) {
                addSparseWhenBranch(this, sp)
            }
            addStatement(
                "else -> throw %T(%L)",
                IllegalArgumentException::class,
                "\"Unknown sparse parameter ID: \$paramId\"",
            )
            endControlFlow()
            endControlFlow()
        }

        val ctorArgs =
            props.joinToString(", ") {
                val pName = sanitizeIdentifier(it.name)
                "$pName = $pName"
            }
        addStatement("return %T(%L)", classTypeName, ctorArgs)
    }

    private fun hasScalarReadValidations(f: SchemaField): Boolean {
        if (f.strictEnum && !f.enumValues.isNullOrEmpty()) return true
        val bits = f.bits
        return bits != null &&
            !f.unpackBits &&
            bits.any { it.strictEnum && !it.enumValues.isNullOrEmpty() }
    }

    private fun addScalarReadValidations(
        code: CodeBlock.Builder,
        className: String,
        f: SchemaField,
        varName: String,
    ) {
        if (f.strictEnum && !f.enumValues.isNullOrEmpty()) {
            addStrictEnumCheck(code, className, f, varName)
        }
        val bits = f.bits
        if (bits != null && !f.unpackBits) {
            val srcExpr =
                when (f.type) {
                    "byte" -> "($varName.toInt() and 0xFF)"
                    "short" -> "$varName.toInt()"
                    else -> varName
                }
            for (b in bits) {
                if (b.strictEnum && !b.enumValues.isNullOrEmpty()) {
                    val bVar = "bit_${cleanIdentifier(b.name)}"
                    val extract = CodegenExprUtils.extractBitExpr(srcExpr, b, ExprLang.KOTLIN)
                    code.addStatement("val %L = %L", bVar, extract)
                    addStrictBitEnumCheck(code, className, b, bVar)
                }
            }
        }
    }

    private fun addSparseWhenBranch(code: CodeBlock.Builder, sp: SchemaSparseParam) {
        val spName = sanitizeIdentifier(sp.name)
        if (sp.type.endsWith("[]")) {
            code.beginControlFlow("%L ->", sp.id)
            val syntheticField =
                SchemaField(
                    name = sp.name,
                    type = sp.type,
                    description = sp.description,
                    enumValues = null,
                    lengthType = sp.lengthType ?: "ushort",
                )
            code.beginControlFlow("%L = run", spName)
            addArrayReadLines(code, syntheticField)
            code.endControlFlow()
            code.endControlFlow()
        } else {
            val readExpr = mapToKotlinScalarReadExpr(sp.type)
            code.addStatement("%L -> %L = %L", sp.id, spName, readExpr)
        }
    }

    private fun buildSharedSparseTypeSpec(): TypeSpec {
        val builder = TypeSpec.classBuilder("SharedSparseParams").addModifiers(KModifier.PRIVATE)
        for (sp in sharedSparseParams) {
            builder.addProperty(
                PropertySpec.builder(
                        cleanIdentifier(sp.name),
                        mapToKotlinTypeName(sp.type, isNullable = true),
                    )
                    .mutable(true)
                    .initializer("null")
                    .build()
            )
        }

        val readFun =
            FunSpec.builder("read")
                .addParameter("paramId", INT)
                .addParameter("buffer", WIRE_BUFFER_CLASS)
                .returns(BOOLEAN)
                .addCode(
                    buildCodeBlock {
                        beginControlFlow("when (paramId)")
                        for (sp in sharedSparseParams) {
                            addSparseWhenBranch(this, sp)
                        }
                        addStatement("else -> return false")
                        endControlFlow()
                        addStatement("return true")
                    }
                )
                .build()
        builder.addFunction(readFun)

        val sharedNames = sharedSparseParams.map { sanitizeIdentifier(it.name) }.toSet()
        for (op in schema.operations) {
            val spList = op.sparseParameters ?: continue
            val matchesAll =
                (op.sparseCountType == null || op.sparseCountType == "ushort") &&
                    (op.sparseKeyType == null || op.sparseKeyType == "ubyte") &&
                    sharedSparseParams.all { s ->
                        spList.any {
                            it.id == s.id &&
                                it.name == s.name &&
                                it.type == s.type &&
                                (it.lengthType ?: "ushort") == (s.lengthType ?: "ushort")
                        }
                    }
            if (!matchesAll) continue

            val className = "${op.name}Node"
            val classTypeName = ClassName(DOCUMENT_PACKAGE, className)
            val props = op.astProperties
            val nonSharedProps = props.filter { sanitizeIdentifier(it.name) !in sharedNames }

            val toNodeFun =
                FunSpec.builder("to$className")
                    .returns(classTypeName)
                    .apply {
                        for (p in nonSharedProps) {
                            addParameter(
                                cleanIdentifier(p.name),
                                mapToKotlinTypeName(p.type, p.isNullable),
                            )
                        }
                        val ctorArgs =
                            props.joinToString(", ") {
                                val pName = sanitizeIdentifier(it.name)
                                "$pName = $pName"
                            }
                        addStatement("return %T(%L)", classTypeName, ctorArgs)
                    }
                    .build()
            builder.addFunction(toNodeFun)
        }

        return builder.build()
    }

    private fun addStrictEnumCheck(
        code: CodeBlock.Builder,
        className: String,
        f: SchemaField,
        varName: String,
    ) {
        val enumValues = f.enumValues ?: return
        val checkExpr =
            when (f.type) {
                "byte" -> "($varName.toInt() and 0xFF)"
                "short" -> "$varName.toInt()"
                else -> varName
            }
        addStrictIntEnumCheck(code, className, f.name, enumValues, checkExpr, varName)
    }

    private fun addStrictBitEnumCheck(
        code: CodeBlock.Builder,
        className: String,
        b: SchemaBitField,
        varName: String,
    ) {
        val enumValues = b.enumValues ?: return
        addStrictIntEnumCheck(code, className, b.name, enumValues, varName, varName)
    }

    private fun addStrictIntEnumCheck(
        code: CodeBlock.Builder,
        className: String,
        propName: String,
        enumValues: Map<String, Int>,
        checkExpr: String,
        varName: String,
    ) {
        val sortedValues = enumValues.values.distinct().sorted()
        code.beginControlFlow("when (%L)", checkExpr)
        code.addStatement("%L -> Unit", sortedValues.joinToString(", "))
        code.addStatement(
            "else -> throw %T(%L)",
            IllegalArgumentException::class,
            "\"Invalid $className.$propName: \$$varName\"",
        )
        code.endControlFlow()
    }

    private fun addArrayReadLines(code: CodeBlock.Builder, f: SchemaField) {
        val lenExpr =
            if (f.countFrom != null) {
                sanitizeIdentifier(f.countFrom)
            } else {
                when (f.effectiveLengthType) {
                    "int" -> "buffer.readInt()"
                    "ushort" -> "buffer.readShort() and 0xFFFF"
                    "ubyte" -> "buffer.readByte() and 0xFF"
                    "byte" -> "buffer.readByte().toByte().toInt()"
                    else -> "buffer.readShort().toShort().toInt()"
                }
            }
        val elemType = f.type.removeSuffix("[]")
        val elemMinBytes = CodegenExprUtils.minWireElementBytes(elemType, schema)
        val remainingCap =
            if (elemMinBytes > 1) "buffer.remaining() / $elemMinBytes" else "buffer.remaining()"

        code.addStatement("val count = %L", lenExpr)
        code.addStatement("require(count >= 0) { %L }", "\"Negative array length: \$count\"")
        if (f.maxLength != null) {
            code.addStatement(
                "require(count <= %L) { %L }",
                f.maxLength,
                "\"Array length \$count exceeds maximum (${f.maxLength})\"",
            )
        }
        code.beginControlFlow("require(count <= %L)", remainingCap)
        code.addStatement(
            "%L",
            "\"Array length \$count exceeds buffer (\${buffer.remaining()})\"",
        )
        code.endControlFlow()

        val elemCreation =
            when (f.type) {
                "int[]" -> "IntArray(count) { buffer.readInt() }"
                "short[]" -> "ShortArray(count) { buffer.readShort().toShort() }"
                "float[]",
                "float_or_id[]" -> "FloatArray(count) { Float.fromBits(buffer.readInt()) }"
                else -> "List(count) { $elemType.read(buffer, apiLevel) }"
            }
        code.addStatement("%L", elemCreation)
    }

    private fun buildFloatArrayRawEqualsFunSpec(): FunSpec =
        FunSpec.builder("floatArrayRawEquals")
            .addModifiers(KModifier.PRIVATE)
            .addParameter("a", FLOAT_ARRAY.copy(nullable = true))
            .addParameter("b", FLOAT_ARRAY.copy(nullable = true))
            .returns(BOOLEAN)
            .addStatement("if (a === b) return true")
            .addStatement("if (a == null || b == null || a.size != b.size) return false")
            .beginControlFlow("for (i in a.indices)")
            .addStatement("if (a[i].toRawBits() != b[i].toRawBits()) return false")
            .endControlFlow()
            .addStatement("return true")
            .build()

    private fun buildFloatArrayRawHashCodeFunSpec(): FunSpec =
        FunSpec.builder("floatArrayRawHashCode")
            .addModifiers(KModifier.PRIVATE)
            .addParameter("a", FLOAT_ARRAY.copy(nullable = true))
            .returns(INT)
            .addStatement("if (a == null) return 0")
            .addStatement("var result = 1")
            .beginControlFlow("for (element in a)")
            .addStatement("result = 31 * result + element.toRawBits()")
            .endControlFlow()
            .addStatement("return result")
            .build()

    private fun buildSaveToByteArrayFunSpec(): FunSpec =
        FunSpec.builder("save")
            .addKdoc(
                "%L",
                "Serializes this [RemoteDocument] into a newly allocated byte array.",
            )
            .addAnnotation(JvmOverloads::class)
            .addModifiers(KModifier.PUBLIC)
            .receiver(REMOTE_DOCUMENT_CLASS)
            .addParameter(
                ParameterSpec.builder("targetApiLevel", INT)
                    .defaultValue("%L", "DEFAULT_DOCUMENT_API_LEVEL")
                    .build()
            )
            .addParameter(ParameterSpec.builder("compress", BOOLEAN).defaultValue("false").build())
            .returns(BYTE_ARRAY)
            .addStatement("val buffer = %T()", WIRE_BUFFER_CLASS)
            .addStatement("save(buffer, targetApiLevel, compress)")
            .addStatement("return buffer.cloneBytes()")
            .build()

    private fun buildSaveToBufferFunSpec(): FunSpec =
        FunSpec.builder("save")
            .addKdoc("%L", "Serializes this [RemoteDocument] into an existing [buffer].")
            .addModifiers(KModifier.INTERNAL)
            .receiver(REMOTE_DOCUMENT_CLASS)
            .addParameter("buffer", WIRE_BUFFER_CLASS)
            .addParameter(
                ParameterSpec.builder("targetApiLevel", INT)
                    .defaultValue("%L", "DEFAULT_DOCUMENT_API_LEVEL")
                    .build()
            )
            .addParameter(ParameterSpec.builder("compress", BOOLEAN).defaultValue("false").build())
            .beginControlFlow("if (compress)")
            .addStatement("val compressedProps = LinkedHashMap(header.properties)")
            .addStatement(
                "compressedProps[%T.COMPRESS] = %T.COMPRESSION_DEFLATE",
                DOCUMENT_HEADER_CLASS,
                DOCUMENT_HEADER_CLASS,
            )
            .addStatement("val compressedHeader = header.copy(properties = compressedProps)")
            .addStatement("val effectiveLevel = targetApiLevel.coerceAtLeast(7)")
            .addStatement("compressedHeader.write(buffer, effectiveLevel)")
            .addStatement("val nodesBuffer = %T()", WIRE_BUFFER_CLASS)
            .beginControlFlow("for (node in nodes)")
            .addStatement("node.write(nodesBuffer, effectiveLevel)")
            .endControlFlow()
            .addStatement("val uncompressedBytes = nodesBuffer.cloneBytes()")
            .addStatement("val deflater = %T()", Deflater::class)
            .beginControlFlow("try")
            .addStatement("deflater.setInput(uncompressedBytes)")
            .addStatement("deflater.finish()")
            .addStatement("val chunk = ByteArray(8192)")
            .beginControlFlow("while (!deflater.finished())")
            .addStatement("val count = deflater.deflate(chunk)")
            .beginControlFlow("if (count > 0)")
            .addStatement("val slice = if (count == chunk.size) chunk else chunk.copyOf(count)")
            .addStatement("buffer.write(slice)")
            .endControlFlow()
            .endControlFlow()
            .nextControlFlow("finally")
            .addStatement("deflater.end()")
            .endControlFlow()
            .nextControlFlow("else")
            .addStatement("header.write(buffer, targetApiLevel)")
            .beginControlFlow("for (node in nodes)")
            .addStatement("node.write(buffer, targetApiLevel)")
            .endControlFlow()
            .endControlFlow()
            .build()

    private fun buildDocumentNodeWriteFunSpec(): FunSpec =
        FunSpec.builder("write")
            .addKdoc("%L", "Serializes a [DocumentNode] into [buffer].")
            .addModifiers(KModifier.INTERNAL)
            .receiver(DOCUMENT_NODE_CLASS)
            .addParameter("buffer", WIRE_BUFFER_CLASS)
            .addParameter(
                ParameterSpec.builder("targetApiLevel", INT)
                    .defaultValue("%L", "DEFAULT_DOCUMENT_API_LEVEL")
                    .build()
            )
            .beginControlFlow("when (this)")
            .addStatement("is %T -> this.write(buffer, targetApiLevel)", OPERATION_NODE_CLASS)
            .addStatement(
                "else -> error(%L)",
                "\"Unsupported DocumentNode: \${this::class.java.name}\"",
            )
            .endControlFlow()
            .build()

    private fun buildOperationNodeWriteFunSpec(): FunSpec =
        FunSpec.builder("write")
            .addKdoc("%L", "Serializes an [OperationNode] into [buffer].")
            .addModifiers(KModifier.INTERNAL)
            .receiver(OPERATION_NODE_CLASS)
            .addParameter("buffer", WIRE_BUFFER_CLASS)
            .addParameter(
                ParameterSpec.builder("targetApiLevel", INT)
                    .defaultValue("%L", "DEFAULT_DOCUMENT_API_LEVEL")
                    .build()
            )
            .apply {
                beginControlFlow("when (this)")
                for (op in schema.operations) {
                    addStatement(
                        "is %T -> this.write(buffer, targetApiLevel)",
                        ClassName(DOCUMENT_PACKAGE, "${op.name}Node"),
                    )
                }
                addStatement(
                    "else -> error(%L)",
                    "\"Unsupported OperationNode: \${this::class.java.name}\"",
                )
                endControlFlow()
            }
            .build()

    private fun buildRemoteDocumentSerializerSpec(): TypeSpec =
        TypeSpec.objectBuilder("RemoteDocumentSerializer")
            .addKdoc("%L", "Serializes RemoteCompose documents and nodes into binary wire format.")
            .addModifiers(KModifier.INTERNAL)
            .addFunction(
                FunSpec.builder("write")
                    .addKdoc("%L", "Serializes [document] into a newly allocated byte array.")
                    .addAnnotation(JvmStatic::class)
                    .addAnnotation(JvmOverloads::class)
                    .addModifiers(KModifier.PUBLIC)
                    .addParameter("document", REMOTE_DOCUMENT_CLASS)
                    .addParameter(
                        ParameterSpec.builder("targetApiLevel", INT)
                            .defaultValue("%L", "DEFAULT_DOCUMENT_API_LEVEL")
                            .build()
                    )
                    .addParameter(
                        ParameterSpec.builder("compress", BOOLEAN).defaultValue("false").build()
                    )
                    .returns(BYTE_ARRAY)
                    .addStatement("return document.save(targetApiLevel, compress)")
                    .build()
            )
            .addFunction(
                FunSpec.builder("write")
                    .addKdoc("%L", "Serializes [document] into [buffer].")
                    .addModifiers(KModifier.INTERNAL)
                    .addParameter("document", REMOTE_DOCUMENT_CLASS)
                    .addParameter("buffer", WIRE_BUFFER_CLASS)
                    .addParameter(
                        ParameterSpec.builder("targetApiLevel", INT)
                            .defaultValue("%L", "DEFAULT_DOCUMENT_API_LEVEL")
                            .build()
                    )
                    .addParameter(
                        ParameterSpec.builder("compress", BOOLEAN).defaultValue("false").build()
                    )
                    .addStatement("document.save(buffer, targetApiLevel, compress)")
                    .build()
            )
            .addFunction(
                FunSpec.builder("write")
                    .addKdoc("%L", "Serializes [node] into [buffer].")
                    .addModifiers(KModifier.INTERNAL)
                    .addParameter("node", DOCUMENT_NODE_CLASS)
                    .addParameter("buffer", WIRE_BUFFER_CLASS)
                    .addParameter(
                        ParameterSpec.builder("targetApiLevel", INT)
                            .defaultValue("%L", "DEFAULT_DOCUMENT_API_LEVEL")
                            .build()
                    )
                    .addStatement("node.write(buffer, targetApiLevel)")
                    .build()
            )
            .build()

    private fun buildStructWriterFunSpec(structDef: SchemaStruct): FunSpec {
        val className = structDef.name
        val classTypeName = ClassName(DOCUMENT_PACKAGE, className)
        return FunSpec.builder("write")
            .addKdoc("%L", "Serializes this [$className] into [buffer].")
            .addModifiers(KModifier.INTERNAL)
            .receiver(classTypeName)
            .addParameter("buffer", WIRE_BUFFER_CLASS)
            .addParameter(
                ParameterSpec.builder("targetApiLevel", INT)
                    .defaultValue("%L", "DEFAULT_DOCUMENT_API_LEVEL")
                    .build()
            )
            .addCode(buildWriteFieldsCode(structDef.fields, null, structDef.astProperties))
            .build()
    }

    private fun buildOperationWriterFunSpec(op: SchemaOperation): FunSpec {
        val className = "${op.name}Node"
        val classTypeName = ClassName(DOCUMENT_PACKAGE, className)
        val props = op.astProperties
        val callArgs = buildList {
            add("buffer")
            add("targetApiLevel")
            for (p in props) {
                add(sanitizeIdentifier(p.name))
            }
        }
        return FunSpec.builder("write")
            .addKdoc("%L", "Serializes this [$className] into [buffer].")
            .addModifiers(KModifier.INTERNAL)
            .receiver(classTypeName)
            .addParameter("buffer", WIRE_BUFFER_CLASS)
            .addParameter(
                ParameterSpec.builder("targetApiLevel", INT)
                    .defaultValue("%L", "DEFAULT_DOCUMENT_API_LEVEL")
                    .build()
            )
            .addStatement("write%LOperation(%L)", op.name, callArgs.joinToString(", "))
            .build()
    }

    private fun buildWriteOperationHelperFunSpec(op: SchemaOperation): FunSpec {
        val className = "${op.name}Node"
        val classTypeName = ClassName(DOCUMENT_PACKAGE, className)
        val props = op.astProperties
        val builder =
            FunSpec.builder("write${op.name}Operation").addModifiers(KModifier.INTERNAL).apply {
                if (props.any { it.type == "byte" || it.type == "short" }) {
                    addAnnotation(NO_BYTE_OR_SHORT_ANNOTATION)
                }
                addParameter("buffer", WIRE_BUFFER_CLASS)
                addParameter("targetApiLevel", INT)
                for (p in props) {
                    addParameter(
                        cleanIdentifier(p.name),
                        mapToKotlinTypeName(p.type, p.isNullable),
                    )
                }
            }
        builder.addStatement("buffer.start(%T.OPCODE)", classTypeName)
        builder.addCode(
            buildWriteFieldsCode(
                op.fields,
                op.sparseParameters,
                props,
                op.sparseCountType,
                op.sparseKeyType,
            )
        )
        return builder.build()
    }

    private fun buildWriteFieldsCode(
        fields: List<SchemaField>,
        sparseParameters: List<SchemaSparseParam>?,
        props: List<AstProperty>,
        sparseCountType: String? = null,
        sparseKeyType: String? = null,
    ): CodeBlock = buildCodeBlock {
        val propByName = props.associateBy { it.name }
        val byteNames = fields.filter { it.type == "byte" }.map { it.name }.toSet()
        val shortNames = fields.filter { it.type == "short" }.map { it.name }.toSet()

        val omitZeroFields =
            fields.filter { it.omitIf != null && it.type == "float_or_id" }.map { it.name }.toSet()

        for (rawName in omitZeroFields) {
            val pName = sanitizeIdentifier(rawName)
            val effName = "effective_${cleanIdentifier(rawName)}"
            beginControlFlow(
                "val %L = if (%L != null && !%L.isVariable && %L.floatValue == 0f)",
                effName,
                pName,
                pName,
                pName,
            )
            addStatement("null")
            nextControlFlow("else")
            addStatement("%L", pName)
            endControlFlow()
        }

        fun effectiveVar(rawName: String): String =
            if (rawName in omitZeroFields) {
                "effective_${cleanIdentifier(rawName)}"
            } else {
                sanitizeIdentifier(rawName)
            }

        for (f in fields) {
            val bits = f.bits
            if (bits != null && f.unpackBits) {
                val terms = mutableListOf<Pair<String, SchemaBitField>>()
                for (b in bits) {
                    val bName = sanitizeIdentifier(b.name)
                    if (b.derivedFrom != null) {
                        val expr =
                            derivedExprToKotlin(
                                DerivedExpr.parse(b.derivedFrom),
                                propByName,
                                ::effectiveVar,
                            )
                        addStatement("val %L = %L", bName, expr)
                        terms.add(bName to b)
                    } else {
                        terms.add(bName to b)
                    }
                }
                val packed = CodegenExprUtils.packBitsExpr(terms, ExprLang.KOTLIN)
                addStatement("buffer.writeInt(%L)", packed)
                continue
            }

            if (f.derivedFrom != null) {
                val fName = sanitizeIdentifier(f.name)
                val derivedVal =
                    derivedExprToKotlin(
                        DerivedExpr.parse(f.derivedFrom),
                        propByName,
                        ::effectiveVar,
                    )
                if (f.presentIf != null) {
                    val cond =
                        CodegenExprUtils.translateCondition(
                            f.presentIf,
                            ExprLang.KOTLIN,
                            needsKotlinIntCast = { it in shortNames },
                            isKotlinUnsignedByte = { it in byteNames },
                        ) {
                            sanitizeIdentifier(it)
                        }
                    beginControlFlow("if (%L)", cond)
                    addStatement("%L", mapToKotlinScalarWriteExpr(f.type, derivedVal))
                    endControlFlow()
                } else {
                    addStatement("val %L = %L", fName, derivedVal)
                    addStatement("%L", mapToKotlinScalarWriteExpr(f.type, fName))
                }
                continue
            }

            val fName = effectiveVar(f.name)
            val prop = propByName[f.name]
            val isNullable = prop?.isNullable == true

            if (f.presentIf != null) {
                val cond =
                    CodegenExprUtils.translateCondition(
                        f.presentIf,
                        ExprLang.KOTLIN,
                        needsKotlinIntCast = { it in shortNames },
                        isKotlinUnsignedByte = { it in byteNames },
                    ) {
                        sanitizeIdentifier(it)
                    }
                beginControlFlow("if (%L)", cond)
                if (isNullable) {
                    val localVal = "local_${cleanIdentifier(f.name)}"
                    addStatement(
                        "val %L = requireNotNull(%L) { %S }",
                        localVal,
                        fName,
                        "Missing required field ${f.name}",
                    )
                    addFieldWriteBody(this, f, localVal)
                } else {
                    addFieldWriteBody(this, f, fName)
                }
                endControlFlow()
            } else if (f.addedVersion != null) {
                beginControlFlow("if (targetApiLevel >= %L)", f.addedVersion)
                addFieldWriteBody(this, f, fName)
                endControlFlow()
            } else {
                addFieldWriteBody(this, f, fName)
            }
        }

        if (sparseParameters != null) {
            addStatement("var sparseCount = 0")
            for (sp in sparseParameters) {
                val spName = sanitizeIdentifier(sp.name)
                addStatement("if (%L != null) sparseCount++", spName)
            }
            when (sparseCountType ?: "ushort") {
                "int" -> addStatement("buffer.writeInt(sparseCount)")
                "ubyte",
                "byte" -> addStatement("buffer.writeByte(sparseCount)")
                else -> addStatement("buffer.writeShort(sparseCount)")
            }
            val effectiveKeyType = sparseKeyType ?: "ubyte"
            for (sp in sparseParameters) {
                val spName = sanitizeIdentifier(sp.name)
                val localVar = "sp_${cleanIdentifier(sp.name)}"
                addStatement("val %L = %L", localVar, spName)
                beginControlFlow("if (%L != null)", localVar)
                when (effectiveKeyType) {
                    "int" -> addStatement("buffer.writeInt(%L)", sp.id)
                    "ushort",
                    "short" -> addStatement("buffer.writeShort(%L)", sp.id)
                    else -> addStatement("buffer.writeByte(%L)", sp.id)
                }
                val syntheticField =
                    SchemaField(
                        name = sp.name,
                        type = sp.type,
                        description = sp.description,
                        enumValues = null,
                        lengthType = sp.lengthType ?: "ushort",
                    )
                addFieldWriteBody(this, syntheticField, localVar)
                endControlFlow()
            }
        }
    }

    private fun addFieldWriteBody(code: CodeBlock.Builder, f: SchemaField, varExpr: String) {
        if (f.type.endsWith("[]")) {
            if (f.countFrom == null) {
                when (f.effectiveLengthType) {
                    "int" -> code.addStatement("buffer.writeInt(%L.size)", varExpr)
                    "ubyte",
                    "byte" -> code.addStatement("buffer.writeByte(%L.size)", varExpr)
                    else -> code.addStatement("buffer.writeShort(%L.size)", varExpr)
                }
            }
            code.beginControlFlow("for (item in %L)", varExpr)
            when (f.type) {
                "int[]" -> code.addStatement("buffer.writeInt(item)")
                "short[]" -> code.addStatement("buffer.writeShort(item.toInt())")
                "float[]",
                "float_or_id[]" -> code.addStatement("buffer.writeInt(item.toRawBits())")
                else -> code.addStatement("item.write(buffer, targetApiLevel)")
            }
            code.endControlFlow()
        } else {
            code.addStatement("%L", mapToKotlinScalarWriteExpr(f.type, varExpr))
        }
    }

    private fun derivedExprToKotlin(
        expr: DerivedExpr,
        propByName: Map<String, AstProperty>,
        resolveTarget: (String) -> String = { sanitizeIdentifier(it) },
    ): String {
        val target = resolveTarget(expr.targetName)
        return when (expr) {
            is DerivedExpr.LengthOf -> "$target.size"
            is DerivedExpr.PresentOf -> "if ($target != null) 1 else 0"
            is DerivedExpr.NonEmptyOf -> {
                val prop = propByName[expr.targetName]
                if (prop?.type?.startsWith("List<") == true || prop?.type?.endsWith("[]") == true) {
                    "if ($target.isNotEmpty()) 1 else 0"
                } else {
                    "if ($target.isNotEmpty()) 1 else 0"
                }
            }
        }
    }

    private fun equalsCheckExpr(prop: AstProperty, lhs: String, rhs: String): String =
        when (prop.type) {
            "bytes",
            "int[]",
            "short[]" -> "$lhs.contentEquals($rhs)"
            "float[]",
            "float_or_id[]" -> "floatArrayRawEquals($lhs, $rhs)"
            "float" ->
                if (prop.isNullable) "$lhs?.toRawBits() == $rhs?.toRawBits()"
                else "$lhs.toRawBits() == $rhs.toRawBits()"
            else -> "$lhs == $rhs"
        }

    private fun hashArgExpr(prop: AstProperty, name: String): String =
        when (prop.type) {
            "bytes",
            "int[]",
            "short[]" -> "$name.contentHashCode()"
            "float[]",
            "float_or_id[]" -> "floatArrayRawHashCode($name)"
            else -> name
        }

    private fun buildTypeKDoc(
        description: String,
        props: List<AstProperty>,
        enumByProp: Map<String, Map<String, Int>?>,
        isStruct: Boolean,
    ): String {
        val cleanDesc =
            if (isStruct) {
                val d = description.trim().replace(WHITESPACE_REGEX, " ")
                if (d.isEmpty()) {
                    "Wire payload element."
                } else {
                    val cap = d.replaceFirstChar { it.uppercaseChar() }
                    if (cap.endsWith('.')) cap else "$cap."
                }
            } else {
                cleanOperationDescription(description).replace(WHITESPACE_REGEX, " ")
            }

        return buildString {
            append("Represents an element of the RemoteCompose binary format.\n")
            if (cleanDesc.isNotEmpty()) {
                append("\n")
                append(cleanDesc).append("\n")
            }
            if (props.isNotEmpty()) {
                append("\n")
                for (p in props) {
                    val paramName = sanitizeIdentifier(p.name)
                    val paramDesc =
                        p.description.trim().replace(WHITESPACE_REGEX, " ").let {
                            if (it.isEmpty()) {
                                paramName
                            } else {
                                it.replaceFirstChar { ch -> ch.lowercaseChar() }.trimEnd('.')
                            }
                        }
                    val enumVals = enumByProp[p.name]
                    val enumSuffix =
                        if (!enumVals.isNullOrEmpty()) {
                            val opts =
                                enumVals.entries.joinToString(", ") { "${it.key}=${it.value}" }
                            " (values: $opts)"
                        } else {
                            ""
                        }
                    append("@param $paramName $paramDesc$enumSuffix\n")
                }
            }
        }
    }

    private fun cleanOperationDescription(description: String): String {
        var d = description.trim()
        if (d.isEmpty()) return "Operation AST node."

        val fillerPrefixes =
            listOf(
                "This operation represents a ",
                "This operation represents an ",
                "This operation represents ",
                "This operation contains a ",
                "This operation contains an ",
                "This operation contains ",
                "This operation runs ",
                "This operation will ",
                "This operation ",
                "This modifier will do a ",
                "This modifier will ",
                "A modifier that triggers ",
                "A modifier that ",
                "A class that ",
                "Used to ",
                "Allows the developer to ",
                "Is responsible for ",
            )
        for (filler in fillerPrefixes) {
            if (d.startsWith(filler, ignoreCase = true)) {
                d = d.substring(filler.length).trim()
                break
            }
        }

        when {
            d.startsWith("Padding modifier", ignoreCase = true) ->
                d = "Applies padding to a component."
            d.startsWith("Fixed width modifier", ignoreCase = true) ->
                d = "Sets a fixed width for a component."
            d.startsWith("Fixed height modifier", ignoreCase = true) ->
                d = "Sets a fixed height for a component."
            d.startsWith("Background color modifier", ignoreCase = true) ->
                d = "Applies a background color to a component."
            d.startsWith("Ripple modifier", ignoreCase = true) ->
                d = "Applies a ripple animation modifier on touch."
            d.startsWith("MultiClick modifier", ignoreCase = true) ->
                d = "Dispatches action operations on click."
            d.startsWith("Start of a layout component definition", ignoreCase = true) ->
                d = "Marks the start of a layout component definition."
            d.startsWith("Container end", ignoreCase = true) ->
                d = "Marks the end of a container layout."
            d.startsWith("Canvas implementation", ignoreCase = true) ->
                d = "Lays out a canvas component encapsulating drawing operations."
            d.startsWith("Text layout implementation", ignoreCase = true) ->
                d = "Lays out a text component."
            d.startsWith("Image layout implementation", ignoreCase = true) ->
                d = "Lays out an image component."
            d.startsWith("Root element for a document", ignoreCase = true) ->
                d = "Root layout component for a document."
            d.startsWith("FitBox layout implementation", ignoreCase = true) ->
                d = "Lays out components displaying the first child that fits."
            d.startsWith("Generate an haptic feedback", ignoreCase = true) ||
                d.startsWith("Generate a haptic feedback", ignoreCase = true) ->
                d = "Generates haptic feedback."
            d.startsWith("Draw an arcwhich will be scaled", ignoreCase = true) ||
                d.startsWith("Draw the specified arcwhich", ignoreCase = true) ->
                d = "Draws an arc scaled to fit inside the specified oval."
            d.startsWith("Core document metadata", ignoreCase = true) ->
                d = "Defines document metadata, dimensions, and capability profiles."
            d.startsWith("Multiply the local frame of a 2D mesh", ignoreCase = true) ->
                d = "Multiplies a 2D mesh local frame into canvas matrix for deformation."
            d.startsWith("Active color theme", ignoreCase = true) ->
                d = "Defines the active color theme."
            d.startsWith("Current matrix with the specified skew", ignoreCase = true) ->
                d = "Applies skew to the canvas matrix."
            d.startsWith("A collection of canvas operations", ignoreCase = true) ->
                d = "Encapsulates a collection of canvas drawing operations."
            d.startsWith("A list of IDs", ignoreCase = true) ->
                d = "Defines an indexed list of IDs."
            d.startsWith("A list of floats", ignoreCase = true) ->
                d = "Defines an indexed list of floats."
            d.startsWith("A long and its associated id", ignoreCase = true) ->
                d = "Defines a constant 64-bit integer variable."
            d.startsWith("A constant matrix and its associated id", ignoreCase = true) ->
                d = "Defines a constant transformation matrix."
            d.startsWith("A matrix defined by an expression", ignoreCase = true) ->
                d = "Defines a matrix using dynamic expressions."
            d.startsWith("A dynamic list of floats", ignoreCase = true) ->
                d = "Defines a dynamic list of float values."
            d.startsWith("Flow layout implementation", ignoreCase = true) ->
                d =
                    "Lays out components horizontally, wrapping to next line when space is exhausted."
            d.startsWith("Text style implementation", ignoreCase = true) ->
                d = "Defines styling attributes for text components."
            d.startsWith("Core text layout implementation", ignoreCase = true) ->
                d = "Lays out a core text component with styling."
            d.startsWith("Add additional priority", ignoreCase = true) ->
                d = "Sets layout priority for collapsible layout children."
            d.startsWith("Add additional constraints to the dimensions", ignoreCase = true) ->
                d = "Adds dimension constraints to a component."
            d.startsWith("Add additional constraints to the width", ignoreCase = true) ->
                d = "Adds width constraints to a component."
            d.startsWith("Add additional constraints to the height", ignoreCase = true) ->
                d = "Adds height constraints to a component."
            d.startsWith("Content description of root", ignoreCase = true) ->
                d = "Defines accessibility content description for the root document."
            d.startsWith("Embed a remark or comment", ignoreCase = true) ->
                d = "Embeds a remark or comment string in the document."
        }

        val verbMap =
            listOf(
                "Draw " to "Draws ",
                "Define " to "Defines ",
                "Scale " to "Scales ",
                "Clip " to "Clips ",
                "Multiply " to "Multiplies ",
                "Merge " to "Merges ",
                "Embed " to "Embeds ",
                "Apply " to "Applies ",
                "Preconcat " to "Preconcatenates ",
                "Save " to "Saves ",
                "Restore " to "Restores ",
                "Convert " to "Converts ",
                "Look up " to "Looks up ",
                "Measure " to "Measures ",
                "Create " to "Creates ",
                "Update " to "Updates ",
                "Extract " to "Extracts ",
                "Set " to "Sets ",
                "Start " to "Starts ",
                "Interpolate " to "Interpolates ",
                "Append " to "Appends ",
                "Combine " to "Combines ",
                "Generate " to "Generates ",
                "Print " to "Prints ",
                "Transform " to "Transforms ",
                "Compute " to "Computes ",
                "Add " to "Adds ",
                "Shift " to "Shifts ",
                "Execute " to "Executes ",
                "Encode " to "Encodes ",
                "Expose " to "Exposes ",
                "Get " to "Gets ",
                "Evaluate " to "Evaluates ",
                "Wake up " to "Wakes up ",
                "Compare " to "Compares ",
                "Mark " to "Marks ",
                "Dispatches " to "Dispatches ",
                "Triggers " to "Triggers ",
                "Animates " to "Animates ",
                "Runs " to "Runs ",
                "Run " to "Runs ",
                "Align " to "Aligns ",
            )
        for ((vFrom, vTo) in verbMap) {
            if (d.startsWith(vFrom, ignoreCase = true)) {
                d = vTo + d.substring(vFrom.length)
                break
            }
        }

        val capitalized = d.replaceFirstChar { it.uppercaseChar() }
        return if (capitalized.endsWith('.')) capitalized else "$capitalized."
    }

    internal companion object {
        const val DOCUMENT_PACKAGE: String = "androidx.compose.remote.document"

        val DOCUMENT_NODE_CLASS: ClassName = ClassName(DOCUMENT_PACKAGE, "DocumentNode")
        val OPERATION_NODE_CLASS: ClassName = ClassName(DOCUMENT_PACKAGE, "OperationNode")
        val WIRE_BUFFER_CLASS: ClassName = ClassName(DOCUMENT_PACKAGE, "WireBuffer")
        val REMOTE_DOCUMENT_CLASS: ClassName = ClassName(DOCUMENT_PACKAGE, "RemoteDocument")
        val DOCUMENT_HEADER_CLASS: ClassName = ClassName(DOCUMENT_PACKAGE, "DocumentHeader")
        val MAYBE_VARIABLE_FLOAT_CLASS: ClassName =
            ClassName(DOCUMENT_PACKAGE, "MaybeVariableFloat")

        val RESTRICT_TO_ANNOTATION: AnnotationSpec =
            AnnotationSpec.builder(RestrictTo::class)
                .addMember("%T.Scope.LIBRARY_GROUP", RestrictTo::class)
                .build()

        val NO_BYTE_OR_SHORT_ANNOTATION: AnnotationSpec =
            AnnotationSpec.builder(Suppress::class).addMember("%S", "NoByteOrShort").build()

        private val WHITESPACE_REGEX = Regex("\\s+")

        private val ARRAY_TO_STRING_TYPES =
            setOf("bytes", "int[]", "short[]", "float[]", "float_or_id[]")

        private val KOTLIN_KEYWORDS =
            setOf(
                "in",
                "is",
                "as",
                "fun",
                "val",
                "var",
                "class",
                "object",
                "interface",
                "package",
                "super",
                "this",
                "throw",
                "try",
                "catch",
                "finally",
                "null",
                "true",
                "false",
                "for",
                "while",
                "do",
                "when",
            )

        fun newFileBuilder(fileName: String): FileSpec.Builder =
            FileSpec.builder(DOCUMENT_PACKAGE, fileName)
                .indent("    ")
                .addKotlinDefaultImports(includeJvm = true, includeJs = false)
                .addDefaultPackageImport("kotlin.jvm")

        fun renderFile(fileSpec: FileSpec): String {
            val body =
                fileSpec
                    .toString()
                    .replace("import androidx.`annotation`.", "import androidx.annotation.")
            val fullSource =
                GeneratedSourceFormat.APACHE_2_HEADER +
                    "\n// Generated by RemoteCompose Schema Codegen. DO NOT EDIT.\n\n" +
                    body
            return GeneratedSourceFormat.formatKotlin(fullSource)
        }

        fun cleanIdentifier(name: String): String = CodegenExprUtils.cleanSchemaIdentifier(name)

        fun sanitizeIdentifier(name: String): String {
            val clean = CodegenExprUtils.cleanSchemaIdentifier(name)
            return if (clean in KOTLIN_KEYWORDS) "`$clean`" else clean
        }

        fun mapToKotlinTypeName(type: String, isNullable: Boolean = false): TypeName {
            val base: TypeName =
                when (type) {
                    "int",
                    "int_id",
                    "color" -> INT
                    "short" -> SHORT
                    "byte" -> BYTE
                    "long" -> LONG
                    "float" -> FLOAT
                    "float_or_id" -> MAYBE_VARIABLE_FLOAT_CLASS
                    "boolean" -> BOOLEAN
                    "string" -> STRING
                    "bytes" -> BYTE_ARRAY
                    "int[]" -> INT_ARRAY
                    "short[]" -> SHORT_ARRAY
                    "float[]",
                    "float_or_id[]" -> FLOAT_ARRAY
                    else ->
                        if (type.endsWith("[]")) {
                            LIST.parameterizedBy(
                                ClassName(DOCUMENT_PACKAGE, type.removeSuffix("[]"))
                            )
                        } else {
                            ClassName(DOCUMENT_PACKAGE, type)
                        }
                }
            return if (isNullable) base.copy(nullable = true) else base
        }

        fun mapToKotlinType(type: String, isNullable: Boolean = false): String {
            val base =
                when (type) {
                    "int",
                    "int_id",
                    "color" -> "Int"
                    "short" -> "Short"
                    "byte" -> "Byte"
                    "long" -> "Long"
                    "float" -> "Float"
                    "float_or_id" -> "MaybeVariableFloat"
                    "boolean" -> "Boolean"
                    "string" -> "String"
                    "bytes" -> "ByteArray"
                    "int[]" -> "IntArray"
                    "short[]" -> "ShortArray"
                    "float[]",
                    "float_or_id[]" -> "FloatArray"
                    else ->
                        if (type.endsWith("[]")) {
                            "List<${type.removeSuffix("[]")}>"
                        } else {
                            type
                        }
                }
            return if (isNullable) "$base?" else base
        }

        fun mapToKotlinDefault(
            type: String,
            isNullable: Boolean = false,
            defaultValue: String? = null,
        ): String {
            if (isNullable) return "null"
            if (defaultValue != null) {
                val dv = defaultValue.trim()
                return when (type) {
                    "float_or_id" -> {
                        val lit = if (dv.endsWith("f") || dv.endsWith("F")) dv else "${dv}f"
                        "MaybeVariableFloat.literal($lit)"
                    }
                    "float" -> if (dv.endsWith("f") || dv.endsWith("F")) dv else "${dv}f"
                    "long" -> if (dv.endsWith("L")) dv else "${dv}L"
                    "string" -> CodeBlock.of("%S", dv).toString()
                    else -> dv
                }
            }
            return when (type) {
                "int",
                "int_id",
                "color" -> "0"
                "short" -> "0"
                "byte" -> "0"
                "long" -> "0L"
                "float" -> "0f"
                "float_or_id" -> "MaybeVariableFloat.literal(0f)"
                "boolean" -> "false"
                "string" -> "\"\""
                "bytes" -> "byteArrayOf()"
                "int[]" -> "intArrayOf()"
                "short[]" -> "shortArrayOf()"
                "float[]",
                "float_or_id[]" -> "floatArrayOf()"
                else ->
                    if (type.endsWith("[]")) {
                        "emptyList()"
                    } else {
                        "$type()"
                    }
            }
        }

        fun mapToKotlinScalarReadExpr(type: String, maxLength: Int? = null): String =
            when (type) {
                "int",
                "int_id",
                "color" -> "buffer.readInt()"
                "short" -> "buffer.readShort().toShort()"
                "byte" -> "buffer.readByte().toByte()"
                "long" -> "buffer.readLong()"
                "float" -> "buffer.readFloat()"
                "float_or_id" -> "MaybeVariableFloat.fromRawBits(buffer.readInt())"
                "boolean" -> "buffer.readBoolean()"
                "string" ->
                    if (maxLength != null) "buffer.readUTF8($maxLength)" else "buffer.readUTF8()"
                "bytes" ->
                    if (maxLength != null) "buffer.readBuffer($maxLength)"
                    else "buffer.readBuffer()"
                else -> "$type.read(buffer, apiLevel)"
            }

        fun mapToKotlinScalarWriteExpr(type: String, varName: String): String =
            when (type) {
                "int",
                "int_id",
                "color" -> "buffer.writeInt($varName)"
                "short" -> "buffer.writeShort($varName.toInt())"
                "byte" -> "buffer.writeByte($varName.toInt())"
                "long" -> "buffer.writeLong($varName)"
                "float" -> "buffer.writeFloat($varName)"
                "float_or_id" -> "buffer.writeInt($varName.rawBits)"
                "boolean" -> "buffer.writeBoolean($varName)"
                "string" -> "buffer.writeUTF8($varName)"
                "bytes" -> "buffer.writeBuffer($varName)"
                else -> "$varName.write(buffer, targetApiLevel)"
            }
    }
}
