/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.safe.args.generator.java

import androidx.navigation.safe.args.generator.BoolArrayType
import androidx.navigation.safe.args.generator.BoolType
import androidx.navigation.safe.args.generator.FloatArrayType
import androidx.navigation.safe.args.generator.FloatType
import androidx.navigation.safe.args.generator.IntArrayType
import androidx.navigation.safe.args.generator.IntType
import androidx.navigation.safe.args.generator.LongArrayType
import androidx.navigation.safe.args.generator.LongType
import androidx.navigation.safe.args.generator.NavWriter
import androidx.navigation.safe.args.generator.ObjectArrayType
import androidx.navigation.safe.args.generator.ObjectType
import androidx.navigation.safe.args.generator.ReferenceArrayType
import androidx.navigation.safe.args.generator.ReferenceType
import androidx.navigation.safe.args.generator.StringArrayType
import androidx.navigation.safe.args.generator.StringType
import androidx.navigation.safe.args.generator.ext.capitalize
import androidx.navigation.safe.args.generator.ext.toCamelCase
import androidx.navigation.safe.args.generator.ext.toCamelCaseAsVar
import androidx.navigation.safe.args.generator.models.Action
import androidx.navigation.safe.args.generator.models.Argument
import androidx.navigation.safe.args.generator.models.Destination
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.util.Locale
import javax.lang.model.element.Modifier

const val L = "\$L"
const val N = "\$N"
const val T = "\$T"
const val S = "\$S"
const val BEGIN_STMT = "\$["
const val END_STMT = "\$]"

class JavaNavWriter(private val useAndroidX: Boolean = true) : NavWriter<JavaCodeFile> {

    override fun generateDirectionsCodeFile(
        destination: Destination,
        parentDirectionsFileList: List<JavaCodeFile>
    ): JavaCodeFile {
        val className = destination.toClassName()
        val typeSpec =
            generateDestinationDirectionsTypeSpec(className, destination, parentDirectionsFileList)
        return JavaFile.builder(className.packageName(), typeSpec).build().toCodeFile()
    }

    private fun generateDestinationDirectionsTypeSpec(
        className: ClassName,
        destination: Destination,
        parentDirectionsFileList: List<JavaCodeFile>
    ): TypeSpec {
        val constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build()

        val actionTypes = destination.actions.map { action ->
            action to generateDirectionsTypeSpec(action)
        }

        @Suppress("NAME_SHADOWING")
        val getters = actionTypes
            .map { (action, actionType) ->
                val annotations = Annotations.getInstance(useAndroidX)
                val methodName = action.id.javaIdentifier.toCamelCaseAsVar()
                if (action.args.isEmpty()) {
                    MethodSpec.methodBuilder(methodName)
                        .addAnnotation(annotations.NONNULL_CLASSNAME)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(NAV_DIRECTION_CLASSNAME)
                        .addStatement(
                            "return new $T($L)",
                            ACTION_ONLY_NAV_DIRECTION_CLASSNAME, action.id.accessor()
                        )
                        .build()
                } else {
                    val constructor = actionType.methodSpecs.find(MethodSpec::isConstructor)!!
                    val params = constructor.parameters.joinToString(", ") { param -> param.name }
                    val actionTypeName = ClassName.get(
                        className.packageName(),
                        className.simpleName(),
                        actionType.name
                    )
                    MethodSpec.methodBuilder(methodName)
                        .addAnnotation(annotations.NONNULL_CLASSNAME)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameters(constructor.parameters)
                        .returns(actionTypeName)
                        .addStatement("return new $T($params)", actionTypeName)
                        .build()
                }
            }

        // The parent destination list is ordered from the closest to the farthest parent of the
        // processing destination in the graph hierarchy.
        val parentGetters = mutableListOf<MethodSpec>()
        parentDirectionsFileList.forEach {
            val parentPackageName = it.wrapped.packageName
            val parentTypeSpec = it.wrapped.typeSpec
            parentTypeSpec.methodSpecs.filter { method ->
                method.hasModifier(Modifier.STATIC) &&
                    getters.none { it.name == method.name } && // de-dupe local actions
                    parentGetters.none { it.name == method.name } // de-dupe parent actions
            }.forEach { actionMethod ->
                val params = actionMethod.parameters.joinToString(", ") { param -> param.name }
                val methodSpec = MethodSpec.methodBuilder(actionMethod.name)
                    .addAnnotations(actionMethod.annotations)
                    .addModifiers(actionMethod.modifiers)
                    .addParameters(actionMethod.parameters)
                    .returns(actionMethod.returnType)
                    .addStatement(
                        "return $T.$L($params)",
                        ClassName.get(parentPackageName, parentTypeSpec.name), actionMethod.name
                    )
                    .build()
                parentGetters.add(methodSpec)
            }
        }

        return TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addTypes(
                actionTypes
                    .filter { (action, _) -> action.args.isNotEmpty() }
                    .map { (_, actionType) -> actionType }
            )
            .addMethod(constructor)
            .addMethods(getters + parentGetters)
            .build()
    }

    internal fun generateDirectionsTypeSpec(action: Action): TypeSpec {
        val annotations = Annotations.getInstance(useAndroidX)
        val specs = ClassWithArgsSpecs(action.args, annotations, privateConstructor = true)
        val className = ClassName.get("", action.id.javaIdentifier.toCamelCase())

        val getDestIdMethod = MethodSpec.methodBuilder("getActionId")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .returns(Int::class.java)
            .addStatement("return $L", action.id.accessor())
            .build()

        val additionalEqualsBlock = CodeBlock.builder().apply {
            beginControlFlow("if ($N() != that.$N())", getDestIdMethod, getDestIdMethod).apply {
                addStatement("return false")
            }
            endControlFlow()
        }.build()

        val additionalHashCodeBlock = CodeBlock.builder().apply {
            addStatement("result = 31 * result + $N()", getDestIdMethod)
        }.build()

        val toStringHeaderBlock = CodeBlock.builder().apply {
            add("$S + $L() + $S", "${className.simpleName()}(actionId=", getDestIdMethod.name, "){")
        }.build()

        return TypeSpec.classBuilder(className)
            .addSuperinterface(NAV_DIRECTION_CLASSNAME)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addField(specs.hashMapFieldSpec)
            .addMethod(specs.constructor())
            .addMethods(specs.setters(className))
            .addMethod(specs.toBundleMethod("getArguments", true))
            .addMethod(getDestIdMethod)
            .addMethods(specs.getters())
            .addMethod(specs.equalsMethod(className, additionalEqualsBlock))
            .addMethod(specs.hashCodeMethod(additionalHashCodeBlock))
            .addMethod(specs.toStringMethod(className, toStringHeaderBlock))
            .build()
    }

    override fun generateArgsCodeFile(
        destination: Destination
    ): JavaCodeFile {
        val annotations = Annotations.getInstance(useAndroidX)
        val destName = destination.name
            ?: throw IllegalStateException("Destination with arguments must have name")
        val className = ClassName.get(destName.packageName(), "${destName.simpleName()}Args")
        val args = destination.args
        val specs = ClassWithArgsSpecs(args, annotations)

        val fromBundleMethod = MethodSpec.methodBuilder("fromBundle").apply {
            addAnnotation(annotations.NONNULL_CLASSNAME)
            addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            addAnnotation(specs.suppressAnnotationSpec)
            val bundle = "bundle"
            addParameter(
                ParameterSpec.builder(BUNDLE_CLASSNAME, bundle)
                    .addAnnotation(specs.androidAnnotations.NONNULL_CLASSNAME)
                    .build()
            )
            returns(className)
            val result = "__result"
            addStatement("$T $N = new $T()", className, result, className)
            addStatement("$N.setClassLoader($T.class.getClassLoader())", bundle, className)
            args.forEach { arg ->
                addReadSingleArgBlock("containsKey", bundle, result, arg, specs) {
                    arg.type.addBundleGetStatement(this, arg, arg.sanitizedName, bundle)
                }
            }
            addStatement("return $N", result)
        }.build()

        val fromSavedStateHandleMethod = MethodSpec.methodBuilder("fromSavedStateHandle").apply {
            addAnnotation(annotations.NONNULL_CLASSNAME)
            addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            addAnnotation(specs.suppressAnnotationSpec)
            val savedStateHandle = "savedStateHandle"
            addParameter(
                ParameterSpec.builder(SAVED_STATE_HANDLE_CLASSNAME, savedStateHandle)
                    .addAnnotation(specs.androidAnnotations.NONNULL_CLASSNAME)
                    .build()
            )
            returns(className)
            val result = "__result"
            addStatement("$T $N = new $T()", className, result, className)
            args.forEach { arg ->
                addReadSingleArgBlock("contains", savedStateHandle, result, arg, specs) {
                    addStatement("$N = $N.get($S)", arg.sanitizedName, savedStateHandle, arg.name)
                }
            }
            addStatement("return $N", result)
        }.build()

        val constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build()

        val copyConstructor = MethodSpec.constructorBuilder()
            .addAnnotation(specs.suppressAnnotationSpec)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(className, "original")
            .addCode(specs.copyMapContents("this", "original"))
            .build()

        val fromMapConstructor = MethodSpec.constructorBuilder()
            .addAnnotation(specs.suppressAnnotationSpec)
            .addModifiers(Modifier.PRIVATE)
            .addParameter(HASHMAP_CLASSNAME, "argumentsMap")
            .addStatement(
                "$N.$N.putAll($N)",
                "this",
                specs.hashMapFieldSpec.name,
                "argumentsMap"
            )
            .build()

        val buildMethod = MethodSpec.methodBuilder("build")
            .addAnnotation(annotations.NONNULL_CLASSNAME)
            .addModifiers(Modifier.PUBLIC)
            .returns(className)
            .addStatement(
                "$T result = new $T($N)",
                className,
                className,
                specs.hashMapFieldSpec.name
            )
            .addStatement("return result")
            .build()

        val builderClassName = ClassName.get("", "Builder")
        val builderTypeSpec = TypeSpec.classBuilder("Builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addField(specs.hashMapFieldSpec)
            .addMethod(copyConstructor)
            .addMethod(specs.constructor())
            .addMethod(buildMethod)
            .addMethods(specs.setters(builderClassName))
            .addMethods(specs.getters())
            .build()

        val typeSpec = TypeSpec.classBuilder(className)
            .addSuperinterface(NAV_ARGS_CLASSNAME)
            .addModifiers(Modifier.PUBLIC)
            .addField(specs.hashMapFieldSpec)
            .addMethod(constructor)
            .addMethod(fromMapConstructor)
            .addMethod(fromBundleMethod)
            .addMethod(fromSavedStateHandleMethod)
            .addMethods(specs.getters())
            .addMethod(specs.toBundleMethod("toBundle"))
            .addMethod(specs.equalsMethod(className))
            .addMethod(specs.hashCodeMethod())
            .addMethod(specs.toStringMethod(className))
            .addType(builderTypeSpec)
            .build()

        return JavaFile.builder(className.packageName(), typeSpec).build().toCodeFile()
    }

    private fun MethodSpec.Builder.addReadSingleArgBlock(
        containsMethodName: String,
        sourceVariableName: String,
        targetVariableName: String,
        arg: Argument,
        specs: ClassWithArgsSpecs,
        addGetStatement: MethodSpec.Builder.() -> Unit
    ) {
        beginControlFlow("if ($N.$containsMethodName($S))", sourceVariableName, arg.name)
        addStatement("$T $N", arg.type.typeName(), arg.sanitizedName)
        addGetStatement()
        addNullCheck(arg, arg.sanitizedName)
        addStatement(
            "$targetVariableName.$N.put($S, $N)",
            specs.hashMapFieldSpec,
            arg.name,
            arg.sanitizedName
        )
        nextControlFlow("else")
        if (arg.defaultValue == null) {
            addStatement(
                "throw new $T($S)", IllegalArgumentException::class.java,
                "Required argument \"${arg.name}\" is missing and does not have an " +
                    "android:defaultValue"
            )
        } else {
            addStatement(
                "$targetVariableName.$N.put($S, $L)",
                specs.hashMapFieldSpec,
                arg.name,
                arg.defaultValue.write()
            )
        }
        endControlFlow()
    }
}

private class ClassWithArgsSpecs(
    val args: List<Argument>,
    val androidAnnotations: Annotations,
    val privateConstructor: Boolean = false
) {

    val suppressAnnotationSpec = AnnotationSpec.builder(SuppressWarnings::class.java)
        .addMember("value", "$S", "unchecked")
        .build()

    val hashMapFieldSpec = FieldSpec.builder(
        HASHMAP_CLASSNAME,
        "arguments",
        Modifier.PRIVATE,
        Modifier.FINAL
    ).initializer("new $T()", HASHMAP_CLASSNAME).build()

    fun setters(thisClassName: ClassName) = args.map { arg ->
        val capitalizedName = arg.sanitizedName.capitalize(Locale.US)
        MethodSpec.methodBuilder("set$capitalizedName").apply {
            addAnnotation(androidAnnotations.NONNULL_CLASSNAME)
            addAnnotation(suppressAnnotationSpec)
            addModifiers(Modifier.PUBLIC)
            addParameter(generateParameterSpec(arg))
            addNullCheck(arg, arg.sanitizedName)
            addStatement(
                "this.$N.put($S, $N)",
                hashMapFieldSpec.name,
                arg.name,
                arg.sanitizedName
            )
            addStatement("return this")
            returns(thisClassName)
        }.build()
    }

    fun constructor() = MethodSpec.constructorBuilder().apply {
        if (args.filterNot(Argument::isOptional).isNotEmpty()) {
            addAnnotation(suppressAnnotationSpec)
        }
        addModifiers(if (privateConstructor) Modifier.PRIVATE else Modifier.PUBLIC)
        args.filterNot(Argument::isOptional).forEach { arg ->
            addParameter(generateParameterSpec(arg))
            addNullCheck(arg, arg.sanitizedName)
            addStatement(
                "this.$N.put($S, $N)",
                hashMapFieldSpec.name,
                arg.name,
                arg.sanitizedName
            )
        }
    }.build()

    fun toBundleMethod(
        name: String,
        addOverrideAnnotation: Boolean = false
    ) = MethodSpec.methodBuilder(name).apply {
        if (addOverrideAnnotation) {
            addAnnotation(Override::class.java)
        }
        addAnnotation(suppressAnnotationSpec)
        addAnnotation(androidAnnotations.NONNULL_CLASSNAME)
        addModifiers(Modifier.PUBLIC)
        returns(BUNDLE_CLASSNAME)
        val result = "__result"
        addStatement("$T $N = new $T()", BUNDLE_CLASSNAME, result, BUNDLE_CLASSNAME)
        args.forEach { arg ->
            beginControlFlow("if ($N.containsKey($S))", hashMapFieldSpec.name, arg.name).apply {
                addStatement(
                    "$T $N = ($T) $N.get($S)",
                    arg.type.typeName(),
                    arg.sanitizedName,
                    arg.type.typeName(),
                    hashMapFieldSpec.name,
                    arg.name
                )
                arg.type.addBundlePutStatement(this, arg, result, arg.sanitizedName)
            }
            if (arg.defaultValue != null) {
                nextControlFlow("else").apply {
                    arg.type.addBundlePutStatement(this, arg, result, arg.defaultValue.write())
                }
            }
            endControlFlow()
        }
        addStatement("return $N", result)
    }.build()

    fun copyMapContents(to: String, from: String) = CodeBlock.builder()
        .addStatement(
            "$N.$N.putAll($N.$N)",
            to,
            hashMapFieldSpec.name,
            from,
            hashMapFieldSpec.name
        ).build()

    fun getters() = args.map { arg ->
        MethodSpec.methodBuilder(getterFromArgName(arg.sanitizedName)).apply {
            addModifiers(Modifier.PUBLIC)
            addAnnotation(suppressAnnotationSpec)
            if (arg.type.allowsNullable()) {
                if (arg.isNullable) {
                    addAnnotation(androidAnnotations.NULLABLE_CLASSNAME)
                } else {
                    addAnnotation(androidAnnotations.NONNULL_CLASSNAME)
                }
            }
            addStatement(
                "return ($T) $N.get($S)",
                arg.type.typeName(),
                hashMapFieldSpec.name,
                arg.name
            )
            returns(arg.type.typeName())
        }.build()
    }

    fun equalsMethod(
        className: ClassName,
        additionalCode: CodeBlock? = null
    ) = MethodSpec.methodBuilder("equals").apply {
        addAnnotation(Override::class.java)
        addModifiers(Modifier.PUBLIC)
        addParameter(TypeName.OBJECT, "object")
        addCode(
            """
                if (this == object) {
                    return true;
                }
                if (object == null || getClass() != object.getClass()) {
                    return false;
                }

            """.trimIndent()
        )
        addStatement("$T that = ($T) object", className, className)
        args.forEach { (name, type, _, _, sanitizedName) ->
            beginControlFlow(
                "if ($N.containsKey($S) != that.$N.containsKey($S))",
                hashMapFieldSpec,
                name,
                hashMapFieldSpec,
                name
            ).apply {
                addStatement("return false")
            }.endControlFlow()
            val getterName = getterFromArgName(sanitizedName, "()")
            val compareExpression = when (type) {
                IntType,
                BoolType,
                ReferenceType,
                LongType -> "$getterName != that.$getterName"
                FloatType -> "Float.compare(that.$getterName, $getterName) != 0"
                StringType, IntArrayType, LongArrayType, FloatArrayType, StringArrayType,
                BoolArrayType, ReferenceArrayType, is ObjectArrayType, is ObjectType ->
                    "$getterName != null ? !$getterName.equals(that.$getterName) " +
                        ": that.$getterName != null"
                else -> throw IllegalStateException("unknown type: $type")
            }
            beginControlFlow("if ($N)", compareExpression).apply {
                addStatement("return false")
            }
            endControlFlow()
        }
        if (additionalCode != null) {
            addCode(additionalCode)
        }
        addStatement("return true")
        returns(TypeName.BOOLEAN)
    }.build()

    private fun getterFromArgName(sanitizedName: String, suffix: String = ""): String {
        val capitalizedName = sanitizedName.capitalize(Locale.US)
        return "get${capitalizedName}$suffix"
    }

    fun hashCodeMethod(
        additionalCode: CodeBlock? = null
    ) = MethodSpec.methodBuilder("hashCode").apply {
        addAnnotation(Override::class.java)
        addModifiers(Modifier.PUBLIC)
        addStatement("int result = 1")
        args.forEach { (_, type, _, _, sanitizedName) ->
            val getterName = getterFromArgName(sanitizedName, "()")
            val hashCodeExpression = when (type) {
                IntType, ReferenceType -> getterName
                FloatType -> "Float.floatToIntBits($getterName)"
                IntArrayType, LongArrayType, FloatArrayType, StringArrayType,
                BoolArrayType, ReferenceArrayType, is ObjectArrayType ->
                    "java.util.Arrays.hashCode($getterName)"
                StringType, is ObjectType ->
                    "($getterName != null ? $getterName.hashCode() : 0)"
                BoolType -> "($getterName ? 1 : 0)"
                LongType -> "(int)($getterName ^ ($getterName >>> 32))"
                else -> throw IllegalStateException("unknown type: $type")
            }
            addStatement("result = 31 * result + $N", hashCodeExpression)
        }
        if (additionalCode != null) {
            addCode(additionalCode)
        }
        addStatement("return result")
        returns(TypeName.INT)
    }.build()

    fun toStringMethod(
        className: ClassName,
        toStringHeaderBlock: CodeBlock? = null
    ) = MethodSpec.methodBuilder("toString").apply {
        addAnnotation(Override::class.java)
        addModifiers(Modifier.PUBLIC)
        addCode(
            CodeBlock.builder().apply {
                if (toStringHeaderBlock != null) {
                    add("${BEGIN_STMT}return $L", toStringHeaderBlock)
                } else {
                    add("${BEGIN_STMT}return $S", "${className.simpleName()}{")
                }
                args.forEachIndexed { index, (_, _, _, _, sanitizedName) ->
                    val getterName = getterFromArgName(sanitizedName, "()")
                    val prefix = if (index == 0) "" else ", "
                    add("\n+ $S + $L", "$prefix$sanitizedName=", getterName)
                }
                add("\n+ $S;\n$END_STMT", "}")
            }.build()
        )
        returns(ClassName.get(String::class.java))
    }.build()

    private fun generateParameterSpec(arg: Argument): ParameterSpec {
        return ParameterSpec.builder(arg.type.typeName(), arg.sanitizedName).apply {
            if (arg.type.allowsNullable()) {
                if (arg.isNullable) {
                    addAnnotation(androidAnnotations.NULLABLE_CLASSNAME)
                } else {
                    addAnnotation(androidAnnotations.NONNULL_CLASSNAME)
                }
            }
        }.build()
    }
}

internal fun MethodSpec.Builder.addNullCheck(
    arg: Argument,
    variableName: String
) {
    if (arg.type.allowsNullable() && !arg.isNullable) {
        beginControlFlow("if ($N == null)", variableName).apply {
            addStatement(
                "throw new $T($S)", IllegalArgumentException::class.java,
                "Argument \"${arg.name}\" is marked as non-null but was passed a null value."
            )
        }
        endControlFlow()
    }
}

internal fun Destination.toClassName(): ClassName {
    val destName = name ?: throw IllegalStateException("Destination with actions must have name")
    return ClassName.get(destName.packageName(), "${destName.simpleName()}Directions")
}
