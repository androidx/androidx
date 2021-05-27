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

package androidx.navigation.safe.args.generator.kotlin

import androidx.navigation.safe.args.generator.NavWriter
import androidx.navigation.safe.args.generator.ObjectArrayType
import androidx.navigation.safe.args.generator.ObjectType
import androidx.navigation.safe.args.generator.ext.toCamelCase
import androidx.navigation.safe.args.generator.ext.toCamelCaseAsVar
import androidx.navigation.safe.args.generator.models.Action
import androidx.navigation.safe.args.generator.models.Destination
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

class KotlinNavWriter(private val useAndroidX: Boolean = true) : NavWriter<KotlinCodeFile> {

    override fun generateDirectionsCodeFile(
        destination: Destination,
        parentDirectionsFileList: List<KotlinCodeFile>
    ): KotlinCodeFile {
        val destName = destination.name
            ?: throw IllegalStateException("Destination with actions must have name")
        val className = ClassName(destName.packageName(), "${destName.simpleName()}Directions")

        val actionTypes = destination.actions.map { action ->
            action to generateDirectionTypeSpec(action)
        }

        val actionsFunSpec = actionTypes.map { (action, actionTypeSpec) ->
            val typeName = ClassName("", actionTypeSpec.name!!)
            val parameters = action.args.map { arg ->
                ParameterSpec.builder(
                    name = arg.sanitizedName,
                    type = arg.type.typeName().copy(nullable = arg.isNullable)
                ).apply {
                    arg.defaultValue?.let {
                        defaultValue(it.write())
                    }
                }.build()
            }
            FunSpec.builder(action.id.javaIdentifier.toCamelCaseAsVar()).apply {
                returns(NAV_DIRECTION_CLASSNAME)
                addParameters(parameters)
                if (action.args.isEmpty()) {
                    addStatement(
                        "return %T(%L)",
                        ACTION_ONLY_NAV_DIRECTION_CLASSNAME, action.id.accessor()
                    )
                } else {
                    addStatement(
                        "return %T(${parameters.joinToString(", ") { it.name }})",
                        typeName
                    )
                }
            }.build()
        }

        // The parent destination list is ordered from the closest to the farthest parent of the
        // processing destination in the graph hierarchy.
        val parentActionsFunSpec = mutableListOf<FunSpec>()
        parentDirectionsFileList.forEach {
            val parentPackageName = it.wrapped.packageName
            val parentTypeSpec = it.wrapped.members.filterIsInstance(TypeSpec::class.java).first()
            val parentCompanionTypeSpec = parentTypeSpec.typeSpecs.first { it.isCompanion }
            parentCompanionTypeSpec.funSpecs.filter { function ->
                actionsFunSpec.none { it.name == function.name } && // de-dupe local actions
                    parentActionsFunSpec.none { it.name == function.name } // de-dupe parent actions
            }.forEach { functionSpec ->
                val params = functionSpec.parameters.joinToString(", ") { param -> param.name }
                val methodSpec = FunSpec.builder(functionSpec.name)
                    .addParameters(functionSpec.parameters)
                    .returns(NAV_DIRECTION_CLASSNAME)
                    .addStatement(
                        "return %T.%L($params)",
                        ClassName(parentPackageName, parentTypeSpec.name!!), functionSpec.name
                    )
                    .build()
                parentActionsFunSpec.add(methodSpec)
            }
        }

        val typeSpec = TypeSpec.classBuilder(className)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addTypes(
                actionTypes
                    .filter { (action, _) -> action.args.isNotEmpty() }
                    .map { (_, type) -> type }
            )
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addFunctions(actionsFunSpec + parentActionsFunSpec)
                    .build()
            )
            .build()

        return FileSpec.builder(className.packageName, className.simpleName)
            .addType(typeSpec)
            .build()
            .toCodeFile()
    }

    internal fun generateDirectionTypeSpec(action: Action): TypeSpec {
        val className = ClassName("", action.id.javaIdentifier.toCamelCase())

        val actionIdPropSpec =
            PropertySpec.builder("actionId", Int::class, KModifier.OVERRIDE)
                .initializer("%L", action.id.accessor()).build()

        val argumentsPropSpec =
            PropertySpec.builder("arguments", BUNDLE_CLASSNAME, KModifier.OVERRIDE)
                .getter(
                    FunSpec.getterBuilder().apply {
                        if (action.args.any { it.type is ObjectType }) {
                            addAnnotation(CAST_NEVER_SUCCEEDS)
                        }
                        val resultVal = "result"
                        addStatement("val %L = %T()", resultVal, BUNDLE_CLASSNAME)
                        action.args.forEach { arg ->
                            arg.type.addBundlePutStatement(
                                this,
                                arg,
                                resultVal,
                                "this.${arg.sanitizedName}"
                            )
                        }
                        addStatement("return %L", resultVal)
                    }.build()
                ).build()

        val constructorFunSpec = FunSpec.constructorBuilder()
            .addParameters(
                action.args.map { arg ->
                    ParameterSpec.builder(
                        name = arg.sanitizedName,
                        type = arg.type.typeName().copy(nullable = arg.isNullable)
                    ).apply {
                        arg.defaultValue?.let {
                            defaultValue(it.write())
                        }
                    }.build()
                }
            )
            .build()

        return if (action.args.isEmpty()) {
            TypeSpec.objectBuilder(className)
        } else {
            TypeSpec.classBuilder(className)
                .addModifiers(KModifier.DATA)
                .primaryConstructor(constructorFunSpec)
                .addProperties(
                    action.args.map { arg ->
                        PropertySpec.builder(
                            arg.sanitizedName,
                            arg.type.typeName().copy(nullable = arg.isNullable)
                        ).initializer(arg.sanitizedName).build()
                    }
                )
        }.addSuperinterface(NAV_DIRECTION_CLASSNAME)
            .addModifiers(KModifier.PRIVATE)
            .addProperty(actionIdPropSpec)
            .addProperty(argumentsPropSpec)
            .build()
    }

    override fun generateArgsCodeFile(destination: Destination): KotlinCodeFile {
        val destName = destination.name
            ?: throw IllegalStateException("Destination with actions must have name")
        val className = ClassName(destName.packageName(), "${destName.simpleName()}Args")

        val constructorFunSpec = FunSpec.constructorBuilder()
            .addParameters(
                destination.args.map { arg ->
                    ParameterSpec.builder(
                        name = arg.sanitizedName,
                        type = arg.type.typeName().copy(nullable = arg.isNullable)
                    ).apply { arg.defaultValue?.let { defaultValue(it.write()) } }.build()
                }
            )
            .build()

        val toBundleFunSpec = FunSpec.builder("toBundle").apply {
            if (destination.args.any { it.type is ObjectType }) {
                addAnnotation(CAST_NEVER_SUCCEEDS)
            }
            returns(BUNDLE_CLASSNAME)
            val resultVal = "result"
            addStatement("val %L = %T()", resultVal, BUNDLE_CLASSNAME)
            destination.args.forEach { arg ->
                arg.type.addBundlePutStatement(this, arg, resultVal, "this.${arg.sanitizedName}")
            }
            addStatement("return %L", resultVal)
        }.build()

        val fromBundleFunSpec = FunSpec.builder("fromBundle").apply {
            addAnnotation(JvmStatic::class)
            if (destination.args.any { it.type is ObjectArrayType }) {
                addAnnotation(
                    AnnotationSpec.builder(Suppress::class)
                        .addMember("%S", "UNCHECKED_CAST")
                        .build()
                )
            }
            returns(className)
            val bundleParamName = "bundle"
            addParameter(bundleParamName, BUNDLE_CLASSNAME)
            addStatement(
                "%L.setClassLoader(%T::class.java.classLoader)",
                bundleParamName,
                className
            )
            val tempVariables = destination.args.map { arg ->
                val tempVal = "__${arg.sanitizedName}"
                addStatement(
                    "val %L : %T",
                    tempVal,
                    arg.type.typeName().copy(nullable = arg.type.allowsNullable())
                )
                beginControlFlow("if (%L.containsKey(%S))", bundleParamName, arg.name)
                arg.type.addBundleGetStatement(this, arg, tempVal, bundleParamName)
                if (arg.type.allowsNullable() && !arg.isNullable) {
                    beginControlFlow("if (%L == null)", tempVal).apply {
                        addStatement(
                            "throw·%T(%S)",
                            IllegalArgumentException::class.asTypeName(),
                            "Argument \"${arg.name}\" is marked as non-null but was passed a " +
                                "null value."
                        )
                    }
                    endControlFlow()
                }
                nextControlFlow("else")
                val defaultValue = arg.defaultValue
                if (defaultValue != null) {
                    addStatement("%L = %L", tempVal, arg.defaultValue.write())
                } else {
                    addStatement(
                        "throw·%T(%S)",
                        IllegalArgumentException::class.asTypeName(),
                        "Required argument \"${arg.name}\" is missing and does not have an " +
                            "android:defaultValue"
                    )
                }
                endControlFlow()
                return@map tempVal
            }
            addStatement("return·%T(${tempVariables.joinToString(", ") { it }})", className)
        }.build()

        val fromSavedStateHandleFunSpec = FunSpec.builder("fromSavedStateHandle").apply {
            addAnnotation(JvmStatic::class)
            returns(className)
            val savedStateParamName = "savedStateHandle"
            addParameter(savedStateParamName, SAVED_STATE_HANDLE_CLASSNAME)
            val tempVariables = destination.args.map { arg ->
                val tempVal = "__${arg.sanitizedName}"
                addStatement(
                    "val %L : %T",
                    tempVal,
                    arg.type.typeName().copy(nullable = true)
                )
                beginControlFlow("if (%L.contains(%S))", savedStateParamName, arg.name)
                addStatement("%L = %L[%S]", tempVal, savedStateParamName, arg.name)
                if (!arg.isNullable) {
                    beginControlFlow("if (%L == null)", tempVal)
                    val errorMessage = if (arg.type.allowsNullable()) {
                        "Argument \"${arg.name}\" is marked as non-null but was passed a null value"
                    } else {
                        "Argument \"${arg.name}\" of type ${arg.type} does not support null values"
                    }
                    addStatement(
                        "throw·%T(%S)",
                        IllegalArgumentException::class.asTypeName(),
                        errorMessage
                    )
                    endControlFlow()
                }
                nextControlFlow("else")
                val defaultValue = arg.defaultValue
                if (defaultValue != null) {
                    addStatement("%L = %L", tempVal, arg.defaultValue.write())
                } else {
                    addStatement(
                        "throw·%T(%S)",
                        IllegalArgumentException::class.asTypeName(),
                        "Required argument \"${arg.name}\" is missing and does not have an " +
                            "android:defaultValue"
                    )
                }
                endControlFlow()
                return@map tempVal
            }
            addStatement("return·%T(${tempVariables.joinToString(", ") { it }})", className)
        }.build()

        val typeSpec = TypeSpec.classBuilder(className)
            .addSuperinterface(NAV_ARGS_CLASSNAME)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(constructorFunSpec)
            .addProperties(
                destination.args.map { arg ->
                    PropertySpec.builder(
                        arg.sanitizedName,
                        arg.type.typeName().copy(nullable = arg.isNullable)
                    ).initializer(arg.sanitizedName).build()
                }
            )
            .addFunction(toBundleFunSpec)
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addFunction(fromBundleFunSpec)
                    .addFunction(fromSavedStateHandleFunSpec)
                    .build()
            )
            .build()

        return FileSpec.builder(className.packageName, className.simpleName)
            .addType(typeSpec)
            .build()
            .toCodeFile()
    }

    companion object {
        /**
         * Annotation to suppress casts that never succeed. This is necessary since the generated
         * code will contain branches that contain a cast that will never occur and succeed. The
         * reason being that Safe Args is not an annotation processor and cannot inspect the class
         * hierarchy to generate the correct cast branch only.
         */
        val CAST_NEVER_SUCCEEDS = AnnotationSpec.builder(Suppress::class)
            .addMember("%S", "CAST_NEVER_SUCCEEDS")
            .build()
    }
}