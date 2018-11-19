/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.navigation.safe.args.generator

import androidx.navigation.safe.args.generator.ext.BEGIN_STMT
import androidx.navigation.safe.args.generator.ext.END_STMT
import androidx.navigation.safe.args.generator.ext.L
import androidx.navigation.safe.args.generator.ext.N
import androidx.navigation.safe.args.generator.ext.S
import androidx.navigation.safe.args.generator.ext.T
import androidx.navigation.safe.args.generator.ext.toCamelCase
import androidx.navigation.safe.args.generator.ext.toCamelCaseAsVar
import androidx.navigation.safe.args.generator.models.Action
import androidx.navigation.safe.args.generator.models.Argument
import androidx.navigation.safe.args.generator.models.Destination
import androidx.navigation.safe.args.generator.models.accessor
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

private const val NAVIGATION_PACKAGE = "androidx.navigation"
private val NAV_DIRECTION_CLASSNAME: ClassName = ClassName.get(NAVIGATION_PACKAGE, "NavDirections")
private val BUNDLE_CLASSNAME: ClassName = ClassName.get("android.os", "Bundle")

internal abstract class Annotations {
    abstract val NULLABLE_CLASSNAME: ClassName
    abstract val NONNULL_CLASSNAME: ClassName

    private object AndroidAnnotations : Annotations() {
        override val NULLABLE_CLASSNAME = ClassName.get("android.support.annotation", "Nullable")
        override val NONNULL_CLASSNAME = ClassName.get("android.support.annotation", "NonNull")
    }

    private object AndroidXAnnotations : Annotations() {
        override val NULLABLE_CLASSNAME = ClassName.get("androidx.annotation", "Nullable")
        override val NONNULL_CLASSNAME = ClassName.get("androidx.annotation", "NonNull")
    }

    companion object {
        fun getInstance(useAndroidX: Boolean): Annotations {
            if (useAndroidX) {
                return AndroidXAnnotations
            } else {
                return AndroidAnnotations
            }
        }
    }
}

private class ClassWithArgsSpecs(
    val args: List<Argument>,
    val annotations: Annotations
) {

    fun fieldSpecs() = args.map { arg ->
        FieldSpec.builder(arg.type.typeName(), arg.sanitizedName)
                .apply {
                    addModifiers(Modifier.PRIVATE)
                    if (arg.type.allowsNullable()) {
                        if (arg.isNullable) {
                            addAnnotation(annotations.NULLABLE_CLASSNAME)
                        } else {
                            addAnnotation(annotations.NONNULL_CLASSNAME)
                        }
                    }
                    if (arg.isOptional()) {
                        initializer(arg.defaultValue!!.write())
                    }
                }
                .build()
    }

    fun setters(thisClassName: ClassName) = args.map { arg ->
        MethodSpec.methodBuilder("set${arg.sanitizedName.capitalize()}").apply {
            addAnnotation(annotations.NONNULL_CLASSNAME)
            addModifiers(Modifier.PUBLIC)
            addParameter(generateParameterSpec(arg))
            addNullCheck(arg, arg.sanitizedName)
            addStatement("this.$N = $N", arg.sanitizedName, arg.sanitizedName)
            addStatement("return this")
            returns(thisClassName)
        }.build()
    }

    fun constructor() = MethodSpec.constructorBuilder().apply {
        addModifiers(Modifier.PUBLIC)
        args.filterNot(Argument::isOptional).forEach { arg ->
            addParameter(generateParameterSpec(arg))
            addStatement("this.$N = $N", arg.sanitizedName, arg.sanitizedName)
            addNullCheck(arg, "this.${arg.sanitizedName}")
        }
    }.build()

    fun toBundleMethod(name: String) = MethodSpec.methodBuilder(name).apply {
        addAnnotation(annotations.NONNULL_CLASSNAME)
        addModifiers(Modifier.PUBLIC)
        returns(BUNDLE_CLASSNAME)
        val bundleName = "__outBundle"
        addStatement("$T $N = new $T()", BUNDLE_CLASSNAME, bundleName, BUNDLE_CLASSNAME)
        args.forEach { arg ->
            arg.type.addBundlePutStatement(this, arg, bundleName, "this.${arg.sanitizedName}")
        }
        addStatement("return $N", bundleName)
    }.build()

    fun copyProperties(to: String, from: String) = CodeBlock.builder()
            .apply {
                args.forEach { (_, _, _, _, sanitizedName) ->
                    addStatement("$to.$sanitizedName = $from.$sanitizedName")
                }
            }
            .build()

    fun getters() = args.map { arg ->
        MethodSpec.methodBuilder("get${arg.sanitizedName.capitalize()}").apply {
            addModifiers(Modifier.PUBLIC)
            if (arg.type.allowsNullable()) {
                if (arg.isNullable) {
                    addAnnotation(annotations.NULLABLE_CLASSNAME)
                } else {
                    addAnnotation(annotations.NONNULL_CLASSNAME)
                }
            }
            addStatement("return $N", arg.sanitizedName)
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
        addCode("""
                if (this == object) {
                    return true;
                }
                if (object == null || getClass() != object.getClass()) {
                    return false;
                }

                """.trimIndent())
        addStatement("$T that = ($T) object", className, className)
        args.forEach { (_, type, _, _, sanitizedName) ->
            val compareExpression = when (type) {
                IntType,
                BoolType,
                ReferenceType,
                LongType -> "$sanitizedName != that.$sanitizedName"
                FloatType -> "Float.compare(that.$sanitizedName, $sanitizedName) != 0"
                StringType, IntArrayType, LongArrayType, FloatArrayType, StringArrayType,
                BoolArrayType, ReferenceArrayType, is ObjectArrayType, is ObjectType ->
                    "$sanitizedName != null ? !$sanitizedName.equals(that.$sanitizedName) " +
                        ": that.$sanitizedName != null"
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

    fun hashCodeMethod(
        additionalCode: CodeBlock? = null
    ) = MethodSpec.methodBuilder("hashCode").apply {
        addAnnotation(Override::class.java)
        addModifiers(Modifier.PUBLIC)
        addStatement("int result = super.hashCode()")
        args.forEach { (_, type, _, _, sanitizedName) ->
            val hashCodeExpression = when (type) {
                IntType, ReferenceType -> sanitizedName
                FloatType -> "Float.floatToIntBits($sanitizedName)"
                IntArrayType, LongArrayType, FloatArrayType, StringArrayType,
                BoolArrayType, ReferenceArrayType, is ObjectArrayType ->
                    "java.util.Arrays.hashCode($sanitizedName)"
                StringType, is ObjectType ->
                    "($sanitizedName != null ? $sanitizedName.hashCode() : 0)"
                BoolType -> "($sanitizedName ? 1 : 0)"
                LongType -> "(int)($sanitizedName ^ ($sanitizedName >>> 32))"
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
        addCode(CodeBlock.builder().apply {
            if (toStringHeaderBlock != null) {
                add("${BEGIN_STMT}return $L", toStringHeaderBlock)
            } else {
                add("${BEGIN_STMT}return $S", "${className.simpleName()}{")
            }
            args.forEachIndexed { index, (_, _, _, _, sanitizedName) ->
                val prefix = if (index == 0) "" else ", "
                add("\n+ $S + $L", "$prefix$sanitizedName=", sanitizedName)
            }
            add("\n+ $S;\n$END_STMT", "}")
        }.build())
        returns(ClassName.get(String::class.java))
    }.build()

    private fun generateParameterSpec(arg: Argument): ParameterSpec {
        return ParameterSpec.builder(arg.type.typeName(), arg.sanitizedName).apply {
            if (arg.type.allowsNullable()) {
                if (arg.isNullable) {
                    addAnnotation(annotations.NULLABLE_CLASSNAME)
                } else {
                    addAnnotation(annotations.NONNULL_CLASSNAME)
                }
            }
        }.build()
    }
}

fun generateDestinationDirectionsTypeSpec(
    className: ClassName,
    superclassName: TypeName?,
    destination: Destination,
    useAndroidX: Boolean
): TypeSpec {
    val actionTypes = destination.actions.map { action ->
        action to generateDirectionsTypeSpec(action, useAndroidX)
    }

    val getters = actionTypes
            .map { (action, actionType) ->
                val annotations = Annotations.getInstance(useAndroidX)
                val constructor = actionType.methodSpecs.find(MethodSpec::isConstructor)!!
                val params = constructor.parameters.joinToString(", ") { param -> param.name }
                val actionTypeName = ClassName.get("", actionType.name)
                MethodSpec.methodBuilder(action.id.javaIdentifier.toCamelCaseAsVar())
                        .addAnnotation(annotations.NONNULL_CLASSNAME)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameters(constructor.parameters)
                        .returns(actionTypeName)
                        .addStatement("return new $T($params)", actionTypeName)
                        .build()
            }

    return TypeSpec.classBuilder(className)
            .superclass(superclassName ?: ClassName.OBJECT)
            .addModifiers(Modifier.PUBLIC)
            .addTypes(actionTypes.map { (_, actionType) -> actionType })
            .addMethods(getters)
            .build()
}

fun generateDirectionsTypeSpec(action: Action, useAndroidX: Boolean): TypeSpec {
    val annotations = Annotations.getInstance(useAndroidX)
    val specs = ClassWithArgsSpecs(action.args, annotations)
    val className = ClassName.get("", action.id.javaIdentifier.toCamelCase())

    val getDestIdMethod = MethodSpec.methodBuilder("getActionId")
            .addModifiers(Modifier.PUBLIC)
            .returns(Int::class.java)
            .addStatement("return $N", action.id.accessor())
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
            .addFields(specs.fieldSpecs())
            .addMethod(specs.constructor())
            .addMethods(specs.setters(className))
            .addMethod(specs.toBundleMethod("getArguments"))
            .addMethod(getDestIdMethod)
            .addMethod(specs.equalsMethod(className, additionalEqualsBlock))
            .addMethod(specs.hashCodeMethod(additionalHashCodeBlock))
            .addMethod(specs.toStringMethod(className, toStringHeaderBlock))
            .build()
}

internal fun generateArgsJavaFile(destination: Destination, useAndroidX: Boolean): JavaFile {
    val annotations = Annotations.getInstance(useAndroidX)
    val destName = destination.name
            ?: throw IllegalStateException("Destination with arguments must have name")
    val className = ClassName.get(destName.packageName(), "${destName.simpleName()}Args")
    val args = destination.args
    val specs = ClassWithArgsSpecs(args, annotations)

    val fromBundleMethod = MethodSpec.methodBuilder("fromBundle").apply {
        addAnnotation(annotations.NONNULL_CLASSNAME)
        addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        val bundle = "bundle"
        addParameter(BUNDLE_CLASSNAME, bundle)
        returns(className)
        val result = "result"
        addStatement("$T $N = new $T()", className, result, className)
        addStatement("$N.setClassLoader($T.class.getClassLoader())", bundle, className)
        args.forEach { arg ->
            beginControlFlow("if ($N.containsKey($S))", bundle, arg.name).apply {
                arg.type.addBundleGetStatement(this, arg, "$result.${arg.sanitizedName}", bundle)
                addNullCheck(arg, "$result.${arg.sanitizedName}")
            }
            if (!arg.isOptional()) {
                nextControlFlow("else")
                addStatement("throw new $T($S)", IllegalArgumentException::class.java,
                        "Required argument \"${arg.name}\" is missing and does " +
                                "not have an android:defaultValue")
            }
            endControlFlow()
        }
        addStatement("return $N", result)
    }.build()

    val constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build()

    val copyConstructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(className, "original")
            .addCode(specs.copyProperties("this", "original"))
            .build()

    val buildMethod = MethodSpec.methodBuilder("build")
            .addAnnotation(annotations.NONNULL_CLASSNAME)
            .addModifiers(Modifier.PUBLIC)
            .returns(className)
            .addStatement("$T result = new $T()", className, className)
            .addCode(specs.copyProperties("result", "this"))
            .addStatement("return result")
            .build()

    val builderClassName = ClassName.get("", "Builder")
    val builderTypeSpec = TypeSpec.classBuilder("Builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addFields(specs.fieldSpecs())
            .addMethod(copyConstructor)
            .addMethod(specs.constructor())
            .addMethod(buildMethod)
            .addMethods(specs.setters(builderClassName))
            .addMethods(specs.getters())
            .build()

    val typeSpec = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addFields(specs.fieldSpecs())
            .addMethod(constructor)
            .addMethod(fromBundleMethod)
            .addMethods(specs.getters())
            .addMethod(specs.toBundleMethod("toBundle"))
            .addMethod(specs.equalsMethod(className))
            .addMethod(specs.hashCodeMethod())
            .addMethod(specs.toStringMethod(className))
            .addType(builderTypeSpec)
            .build()

    return JavaFile.builder(className.packageName(), typeSpec).build()
}

private fun MethodSpec.Builder.addNullCheck(
    arg: Argument,
    variableName: String
) {
    if (arg.type.allowsNullable() && !arg.isNullable) {
        beginControlFlow("if ($N == null)", variableName).apply {
            addStatement("throw new $T($S)", IllegalArgumentException::class.java,
                    "Argument \"${arg.name}\" is marked as non-null " +
                            "but was passed a null value.")
        }
        endControlFlow()
    }
}

fun generateDirectionsJavaFile(
    destination: Destination,
    parentDirectionName: ClassName?,
    useAndroidX: Boolean
): JavaFile {
    val destName = destination.name
            ?: throw IllegalStateException("Destination with actions must have name")
    val className = ClassName.get(destName.packageName(), "${destName.simpleName()}Directions")
    val typeSpec = generateDestinationDirectionsTypeSpec(className, parentDirectionName,
            destination, useAndroidX)
    return JavaFile.builder(className.packageName(), typeSpec).build()
}
