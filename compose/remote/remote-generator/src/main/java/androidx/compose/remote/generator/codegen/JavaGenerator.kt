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
import androidx.compose.remote.generator.SchemaOperation
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

/**
 * Generator that transforms an [OperationSchema] into idiomatic, pure Java (Java 8/11/17) bindings
 * for `:compose:remote:remote-core`:
 * - Abstract base classes (`<OpName>Base.java`) containing mechanical boilerplate:
 *     - OpCode & class name constants
 *     - Wire serialization (`apply`, `write`)
 *     - Deserialization (`read`)
 *     - Metadata & documentation (`documentation`)
 *     - Map serialization (`serialize`)
 * - Pure data operation classes for operations with no custom layout or rendering logic
 * - Central `GeneratedOperations.java` with OpCode constants, profile bitmasks, and dispatch
 *   tables.
 */
internal class JavaGenerator(private val schema: OperationSchema) {

    /**
     * Generates an abstract base class for an operation containing all wire serialization,
     * documentation, and map serialization boilerplate.
     */
    fun generateBaseClass(
        op: SchemaOperation,
        packageName: String = "androidx.compose.remote.core.operations",
        superClassName: String = "Operation",
    ): String {
        val className = "${op.name}Base"
        val classDoc =
            if (op.description.isBlank()) {
                "Base class for ${op.name} operation."
            } else {
                "Base class for ${op.name} operation.\n${op.description.trim()}"
            }

        val classBuilder =
            TypeSpec.classBuilder(className)
                .addJavadoc("\$L\n", classDoc)
                .addAnnotation(RESTRICT_TO_LIBRARY_GROUP)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .superclass(resolveSuperClass(superClassName, packageName))
                .addSuperinterface(SERIALIZABLE_CLASS)
                .addField(
                    FieldSpec.builder(
                            TypeName.INT,
                            "OP_CODE",
                            Modifier.PUBLIC,
                            Modifier.STATIC,
                            Modifier.FINAL,
                        )
                        .initializer("\$L", op.id)
                        .build()
                )
                .addField(
                    FieldSpec.builder(
                            STRING_CLASS,
                            "CLASS_NAME",
                            Modifier.PUBLIC,
                            Modifier.STATIC,
                            Modifier.FINAL,
                        )
                        .initializer("\$S", op.name)
                        .build()
                )

        // Member fields
        for (f in op.fields) {
            classBuilder.addField(
                FieldSpec.builder(
                        mapToJavaTypeName(f.type),
                        memberFieldName(f.name),
                        Modifier.PROTECTED,
                        Modifier.FINAL,
                    )
                    .build()
            )
        }

        // Constructor
        val ctorBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PROTECTED)
        for (f in op.fields) {
            val paramName = sanitizeIdentifier(f.name)
            ctorBuilder.addParameter(mapToJavaTypeName(f.type), paramName)
            ctorBuilder.addStatement("this.\$N = \$N", memberFieldName(f.name), paramName)
        }
        classBuilder.addMethod(ctorBuilder.build())

        // Getters
        for (f in op.fields) {
            val getterName = "get${f.name.replaceFirstChar { it.uppercaseChar() }}"
            classBuilder.addMethod(
                MethodSpec.methodBuilder(getterName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(mapToJavaTypeName(f.type))
                    .addStatement("return \$N", memberFieldName(f.name))
                    .build()
            )
        }

        // Static id() & name()
        classBuilder.addMethod(
            MethodSpec.methodBuilder("id")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.INT)
                .addStatement("return OP_CODE")
                .build()
        )
        classBuilder.addMethod(
            MethodSpec.methodBuilder("name")
                .addAnnotation(NON_NULL_CLASS)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(STRING_CLASS)
                .addStatement("return CLASS_NAME")
                .build()
        )

        // Static apply() method
        val applyBuilder =
            MethodSpec.methodBuilder("apply")
                .addJavadoc("\$L\n", "Writes out the operation to the buffer.")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(
                    ParameterSpec.builder(WIRE_BUFFER_CLASS, "buffer")
                        .addAnnotation(NON_NULL_CLASS)
                        .build()
                )
                .addStatement("buffer.start(OP_CODE)")
        for (f in op.fields) {
            val paramName = sanitizeIdentifier(f.name)
            applyBuilder.addParameter(mapToJavaTypeName(f.type), paramName)
            applyBuilder.addStatement("buffer.\$L(\$N)", mapToJavaWriteMethod(f.type), paramName)
        }
        classBuilder.addMethod(applyBuilder.build())

        // Instance write() method
        val writeCall = CodeBlock.builder().add("apply(buffer")
        for (f in op.fields) {
            writeCall.add(", \$N", memberFieldName(f.name))
        }
        writeCall.add(")")
        classBuilder.addMethod(
            MethodSpec.methodBuilder("write")
                .addAnnotation(OVERRIDE_CLASS)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(
                    ParameterSpec.builder(WIRE_BUFFER_CLASS, "buffer")
                        .addAnnotation(NON_NULL_CLASS)
                        .build()
                )
                .addStatement("\$L", writeCall.build())
                .build()
        )

        // Documentation method
        val docChain =
            CodeBlock.builder()
                .add("doc.operation(\$S, OP_CODE, CLASS_NAME)\n", op.category)
                .indent()
                .indent()
                .add(".description(\$S)", op.description.replace("\n", " "))
        for (f in op.fields) {
            docChain.add(
                "\n.field(\$T.\$L, \$S, \$S)",
                DOCUMENTED_OPERATION_CLASS,
                mapToJavaDocTypeConst(f.type),
                f.name,
                f.description.replace("\n", " "),
            )
        }
        docChain.add(";\n").unindent().unindent()
        classBuilder.addMethod(
            MethodSpec.methodBuilder("documentation")
                .addJavadoc("\$L\n", "Populates documentation for this operation.")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(
                    ParameterSpec.builder(DOCUMENTATION_BUILDER_CLASS, "doc")
                        .addAnnotation(NON_NULL_CLASS)
                        .build()
                )
                .addCode(docChain.build())
                .build()
        )

        // MapSerializer serialize() method
        val serializeBuilder =
            MethodSpec.methodBuilder("serialize")
                .addAnnotation(OVERRIDE_CLASS)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(
                    ParameterSpec.builder(MAP_SERIALIZER_CLASS, "serializer")
                        .addAnnotation(NON_NULL_CLASS)
                        .build()
                )
                .addStatement("serializer.addType(CLASS_NAME)")
        for (f in op.fields) {
            serializeBuilder.addStatement(
                "serializer.add(\$S, \$N)",
                f.name,
                memberFieldName(f.name),
            )
        }
        classBuilder.addMethod(serializeBuilder.build())

        // deepToString & toString
        classBuilder.addMethod(
            MethodSpec.methodBuilder("deepToString")
                .addAnnotation(NON_NULL_CLASS)
                .addAnnotation(OVERRIDE_CLASS)
                .addModifiers(Modifier.PUBLIC)
                .returns(STRING_CLASS)
                .addParameter(
                    ParameterSpec.builder(STRING_CLASS, "indent")
                        .addAnnotation(NON_NULL_CLASS)
                        .build()
                )
                .addStatement("return indent + toString()")
                .build()
        )

        val toStringBuilder =
            MethodSpec.methodBuilder("toString")
                .addAnnotation(NON_NULL_CLASS)
                .addAnnotation(OVERRIDE_CLASS)
                .addModifiers(Modifier.PUBLIC)
                .returns(STRING_CLASS)
        if (op.fields.isEmpty()) {
            toStringBuilder.addStatement("return CLASS_NAME")
        } else {
            val expr = CodeBlock.builder().add("CLASS_NAME")
            op.fields.forEachIndexed { index, f ->
                val prefix = if (index == 0) "(${f.name}=" else ", ${f.name}="
                expr.add(" + \$S + \$N", prefix, memberFieldName(f.name))
            }
            expr.add(" + \$S", ")")
            toStringBuilder.addStatement("return \$L", expr.build())
        }
        classBuilder.addMethod(toStringBuilder.build())

        return renderJavaFile(packageName, classBuilder.build())
    }

    /**
     * Generates a concrete operation class inheriting from the base class for operations that
     * require no custom layout or rendering logic.
     */
    fun generateConcreteClass(
        op: SchemaOperation,
        packageName: String = "androidx.compose.remote.core.operations",
        superClassName: String = "${op.name}Base",
    ): String {
        val className = op.name
        val concreteClassName = ClassName.get(packageName, className)
        val classBuilder =
            TypeSpec.classBuilder(className)
                .addAnnotation(RESTRICT_TO_LIBRARY_GROUP)
                .addModifiers(Modifier.PUBLIC)
                .superclass(resolveSuperClass(superClassName, packageName))

        if (op.description.isNotBlank()) {
            classBuilder.addJavadoc("\$L\n", op.description.trim())
        }

        // Constructor
        val ctorBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
        val superCall = CodeBlock.builder().add("super(")
        op.fields.forEachIndexed { index, f ->
            val paramName = sanitizeIdentifier(f.name)
            ctorBuilder.addParameter(mapToJavaTypeName(f.type), paramName)
            if (index > 0) superCall.add(", ")
            superCall.add("\$N", paramName)
        }
        superCall.add(")")
        ctorBuilder.addStatement("\$L", superCall.build())
        classBuilder.addMethod(ctorBuilder.build())

        // Static read() method
        val listOfOperation = ParameterizedTypeName.get(LIST_CLASS, OPERATION_CLASS)
        val readBuilder =
            MethodSpec.methodBuilder("read")
                .addJavadoc(
                    "\$L\n",
                    "Reads a [$className] from [buffer] and appends it to [operations].",
                )
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(
                    ParameterSpec.builder(WIRE_BUFFER_CLASS, "buffer")
                        .addAnnotation(NON_NULL_CLASS)
                        .build()
                )
                .addParameter(
                    ParameterSpec.builder(listOfOperation, "operations")
                        .addAnnotation(NON_NULL_CLASS)
                        .build()
                )
        for (f in op.fields) {
            val paramName = sanitizeIdentifier(f.name)
            readBuilder.addStatement(
                "\$T \$N = buffer.\$L()",
                mapToJavaTypeName(f.type),
                paramName,
                mapToJavaReadMethod(f.type),
            )
        }
        val addCall = CodeBlock.builder().add("operations.add(new \$T(", concreteClassName)
        op.fields.forEachIndexed { index, f ->
            if (index > 0) addCall.add(", ")
            addCall.add("\$N", sanitizeIdentifier(f.name))
        }
        addCall.add("))")
        readBuilder.addStatement("\$L", addCall.build())
        classBuilder.addMethod(readBuilder.build())

        // Instance apply(RemoteContext context)
        classBuilder.addMethod(
            MethodSpec.methodBuilder("apply")
                .addAnnotation(OVERRIDE_CLASS)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(
                    ParameterSpec.builder(REMOTE_CONTEXT_CLASS, "context")
                        .addAnnotation(NON_NULL_CLASS)
                        .build()
                )
                .addComment("\$L", "Default no-op for non-rendering operations")
                .build()
        )

        return renderJavaFile(packageName, classBuilder.build())
    }

    /** Generates a central GeneratedOperations.java providing OpCode constants and profile maps. */
    fun generateOperationsTable(packageName: String = "androidx.compose.remote.core"): String {
        val classBuilder =
            TypeSpec.classBuilder("GeneratedOperations")
                .addJavadoc(
                    "\$L\n",
                    "Central table of opcodes and profile capability maps generated from schema.",
                )
                .addAnnotation(RESTRICT_TO_LIBRARY_GROUP)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())

        // OpCode constants
        for (op in schema.operations) {
            classBuilder.addField(
                FieldSpec.builder(
                        TypeName.INT,
                        op.resolvedOpCodeName,
                        Modifier.PUBLIC,
                        Modifier.STATIC,
                        Modifier.FINAL,
                    )
                    .initializer("\$L", op.id)
                    .build()
            )
        }

        // OpCode name helper
        val nameSwitch = CodeBlock.builder().beginControlFlow("switch (opCode)")
        for (op in schema.operations) {
            nameSwitch
                .add("case \$N:\n", op.resolvedOpCodeName)
                .indent()
                .addStatement("return \$S", op.name)
                .unindent()
        }
        nameSwitch
            .add("default:\n")
            .indent()
            .addStatement("return \$S + opCode", "UNKNOWN_")
            .unindent()
            .endControlFlow()

        classBuilder.addMethod(
            MethodSpec.methodBuilder("name")
                .addAnnotation(NON_NULL_CLASS)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(STRING_CLASS)
                .addParameter(TypeName.INT, "opCode")
                .addCode(nameSwitch.build())
                .build()
        )

        // Profile membership helper
        val profileSwitch = CodeBlock.builder().beginControlFlow("switch (opCode)")
        for (op in schema.operations) {
            val checks = CodeBlock.builder()
            if (op.profiles.isEmpty()) {
                checks.add("false")
            } else {
                op.profiles.forEachIndexed { index, prof ->
                    if (index > 0) checks.add(" || ")
                    checks.add("\$S.equalsIgnoreCase(profile)", prof)
                }
            }
            profileSwitch
                .add("case \$N:\n", op.resolvedOpCodeName)
                .indent()
                .addStatement("return \$L", checks.build())
                .unindent()
        }
        profileSwitch
            .add("default:\n")
            .indent()
            .addStatement("return false")
            .unindent()
            .endControlFlow()

        classBuilder.addMethod(
            MethodSpec.methodBuilder("isSupportedInProfile")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(TypeName.INT, "opCode")
                .addParameter(
                    ParameterSpec.builder(STRING_CLASS, "profile")
                        .addAnnotation(NON_NULL_CLASS)
                        .build()
                )
                .addCode(profileSwitch.build())
                .build()
        )

        return renderJavaFile(packageName, classBuilder.build())
    }

    private fun renderJavaFile(packageName: String, typeSpec: TypeSpec): String {
        val raw =
            JavaFile.builder(packageName, typeSpec)
                .addFileComment("\$L", "Generated by RemoteCompose JavaGenerator. DO NOT EDIT.")
                .indent("    ")
                .skipJavaLangImports(true)
                .build()
                .toString()
        return GeneratedSourceFormat.APACHE_2_HEADER + unwrapShortMethodHeaders(raw)
    }

    companion object {
        private val STRING_CLASS = ClassName.get("java.lang", "String")
        private val OVERRIDE_CLASS = ClassName.get("java.lang", "Override")
        private val LIST_CLASS = ClassName.get("java.util", "List")
        private val RESTRICT_TO_CLASS = ClassName.get("androidx.annotation", "RestrictTo")
        private val NON_NULL_CLASS = ClassName.get("org.jspecify.annotations", "NonNull")
        private val OPERATION_CLASS = ClassName.get("androidx.compose.remote.core", "Operation")
        private val REMOTE_CONTEXT_CLASS =
            ClassName.get("androidx.compose.remote.core", "RemoteContext")
        private val WIRE_BUFFER_CLASS = ClassName.get("androidx.compose.remote.core", "WireBuffer")
        private val DOCUMENTATION_BUILDER_CLASS =
            ClassName.get("androidx.compose.remote.core.documentation", "DocumentationBuilder")
        private val DOCUMENTED_OPERATION_CLASS =
            ClassName.get("androidx.compose.remote.core.documentation", "DocumentedOperation")
        private val MAP_SERIALIZER_CLASS =
            ClassName.get("androidx.compose.remote.core.serialize", "MapSerializer")
        private val SERIALIZABLE_CLASS =
            ClassName.get("androidx.compose.remote.core.serialize", "Serializable")

        private val RESTRICT_TO_LIBRARY_GROUP =
            AnnotationSpec.builder(RESTRICT_TO_CLASS)
                .addMember("value", "\$T.Scope.LIBRARY_GROUP", RESTRICT_TO_CLASS)
                .build()

        private fun resolveSuperClass(superClassName: String, defaultPackage: String): ClassName =
            when {
                '.' in superClassName ->
                    ClassName.get(
                        superClassName.substringBeforeLast('.'),
                        superClassName.substringAfterLast('.'),
                    )
                superClassName == "Operation" -> OPERATION_CLASS
                else -> ClassName.get(defaultPackage, superClassName)
            }

        private fun memberFieldName(name: String): String =
            "m${name.replaceFirstChar { it.uppercaseChar() }}"

        private fun mapToJavaTypeName(type: String): TypeName =
            when (type) {
                "int",
                "color" -> TypeName.INT
                "short" -> TypeName.SHORT
                "byte" -> TypeName.BYTE
                "long" -> TypeName.LONG
                "float",
                "float_or_id" -> TypeName.FLOAT
                "boolean" -> TypeName.BOOLEAN
                "string" -> STRING_CLASS
                "bytes" -> ArrayTypeName.of(TypeName.BYTE)
                else -> TypeName.INT
            }

        private fun mapToJavaDocTypeConst(type: String): String =
            when (type) {
                "int",
                "color" -> "INT"
                "short" -> "SHORT"
                "byte" -> "BYTE"
                "long" -> "LONG"
                "float",
                "float_or_id" -> "FLOAT"
                "boolean" -> "BOOLEAN"
                "string" -> "STRING"
                "bytes" -> "BUFFER"
                else -> "INT"
            }

        private fun mapToJavaWriteMethod(type: String): String =
            when (type) {
                "int",
                "color" -> "writeInt"
                "short" -> "writeShort"
                "byte" -> "writeByte"
                "long" -> "writeLong"
                "float",
                "float_or_id" -> "writeFloat"
                "boolean" -> "writeBoolean"
                "string" -> "writeUTF8"
                "bytes" -> "writeBuffer"
                else -> "writeInt"
            }

        private fun mapToJavaReadMethod(type: String): String =
            when (type) {
                "int",
                "color" -> "readInt"
                "short" -> "readShort"
                "byte" -> "readByte"
                "long" -> "readLong"
                "float",
                "float_or_id" -> "readFloat"
                "boolean" -> "readBoolean"
                "string" -> "readUTF8"
                "bytes" -> "readBuffer"
                else -> "readInt"
            }

        private fun unwrapShortMethodHeaders(source: String, maxColumns: Int = 120): String {
            val lines = source.split('\n')
            val result = ArrayList<String>(lines.size)
            var i = 0
            while (i < lines.size) {
                var current = lines[i]
                if (
                    (current.startsWith("    public ") ||
                        current.startsWith("    protected ") ||
                        current.startsWith("    private ")) && current.endsWith(",")
                ) {
                    var j = i
                    var candidate = current
                    while (
                        j + 1 < lines.size &&
                            lines[j + 1].startsWith("        ") &&
                            (lines[j + 1].endsWith(",") || lines[j + 1].endsWith(") {"))
                    ) {
                        j++
                        candidate = "$candidate ${lines[j].trimStart()}"
                        if (lines[j].endsWith(") {")) break
                    }
                    if (candidate.endsWith(") {") && candidate.length <= maxColumns) {
                        current = candidate
                        i = j
                    }
                }
                result.add(current)
                i++
            }
            return result.joinToString("\n")
        }

        fun mapToJavaType(type: String): String =
            when (type) {
                "int",
                "color" -> "int"
                "short" -> "short"
                "byte" -> "byte"
                "long" -> "long"
                "float",
                "float_or_id" -> "float"
                "boolean" -> "boolean"
                "string" -> "String"
                "bytes" -> "byte[]"
                else -> "int"
            }

        fun mapToJavaDocType(type: String): String =
            "DocumentedOperation.${mapToJavaDocTypeConst(type)}"

        fun mapToJavaWriteExpr(type: String, fieldVar: String): String =
            "buffer.${mapToJavaWriteMethod(type)}($fieldVar)"

        fun mapToJavaReadExpr(type: String): String = "buffer.${mapToJavaReadMethod(type)}()"

        private val JAVA_KEYWORDS =
            setOf(
                "abstract",
                "assert",
                "boolean",
                "break",
                "byte",
                "case",
                "catch",
                "char",
                "class",
                "const",
                "continue",
                "default",
                "do",
                "double",
                "else",
                "enum",
                "extends",
                "final",
                "finally",
                "float",
                "for",
                "goto",
                "if",
                "implements",
                "import",
                "instanceof",
                "int",
                "interface",
                "long",
                "native",
                "new",
                "package",
                "private",
                "protected",
                "public",
                "return",
                "short",
                "static",
                "strictfp",
                "super",
                "switch",
                "synchronized",
                "this",
                "throw",
                "throws",
                "transient",
                "try",
                "void",
                "volatile",
                "while",
            )

        fun sanitizeIdentifier(name: String): String {
            val clean = CodegenExprUtils.cleanSchemaIdentifier(name)
            return if (clean in JAVA_KEYWORDS) "${clean}Val" else clean
        }

        fun toScreamingSnakeCase(name: String): String = SchemaOperation.toScreamingSnakeCase(name)
    }
}
