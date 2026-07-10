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

package androidx.appfunctions.compiler.core

import androidx.appfunctions.compiler.AppFunctionCompiler
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.RESTRICT_API_TO_33_ANNOTATION
import androidx.appfunctions.compiler.processors.AppFunctionSerializableFactoryCodeBuilderHelper
import androidx.appfunctions.compiler.processors.AppFunctionSerializableFactoryCodeBuilderHelper.Companion.buildFromAppFunctionDataFunction
import androidx.appfunctions.compiler.processors.AppFunctionSerializableFactoryCodeBuilderHelper.Companion.buildToAppFunctionDataFunction
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec

/**
 * A class that represents a class annotated with @AppFunctionSerializableProxy.
 *
 * @param classDeclaration The class annotated with @AppFunctionSerializableProxy.
 */
data class AnnotatedAppFunctionSerializableProxy(
    override val classDeclaration: KSClassDeclaration
) : AppFunctionSerializableType {

    override val isDescribedByKDoc = false

    /** The type of the class that the proxy class is proxying. */
    val targetClassDeclaration: KSClassDeclaration by lazy {
        (classDeclaration.annotations.findAnnotation(
                IntrospectionHelper.AppFunctionSerializableProxyAnnotation.CLASS_NAME
            )
                ?: throw ProcessingException(
                    "Class Must have @AppFunctionSerializableProxy annotation",
                    classDeclaration,
                ))
            .requirePropertyValueOfType(
                IntrospectionHelper.AppFunctionSerializableProxyAnnotation.PROPERTY_TARGET_CLASS,
                KSType::class,
            )
            .declaration as KSClassDeclaration
    }

    /** The name of the method that returns an instance of the target class. */
    val toTargetClassMethodName: String by lazy {
        "to${targetClassDeclaration.simpleName.asString()}"
    }

    /** The name of the companion method that returns an instance of the proxy class. */
    val fromTargetClassMethodName: String by lazy {
        "from${targetClassDeclaration.simpleName.asString()}"
    }

    /** The type of the serializable reference. */
    // TODO(b/403199251): Clean up hack.
    val serializableReferenceType: KSTypeReference by lazy {
        checkNotNull(
            classDeclaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.isCompanionObject }
                .single()
                .getAllFunctions()
                .filter { it.simpleName.asString() == fromTargetClassMethodName }
                .first()
                .returnType
        )
    }

    /**
     * Validates the class annotated with @AppFunctionSerializableProxy.
     *
     * @return The validated class.
     */
    override fun validate(
        allowSerializableInterfaceTypes: Boolean
    ): AnnotatedAppFunctionSerializableProxy {
        val validateHelper = AppFunctionSerializableValidateHelper(this)
        validateHelper.validatePrimaryConstructor()
        validateHelper.validateParameters(allowSerializableInterfaceTypes)
        validateProxyHasToTargetClassMethod()
        validateProxyHasFromTargetClassMethod()
        return this
    }

    /** The generated factory ClassName. */
    override val factoryClassName: ClassName by lazy {
        ClassName(
            appFunctionSerializableTypeClassDeclaration.originalClassName.packageName,
            "$${targetClassDeclaration.getJvmClassName()}Factory",
        )
    }

    /** Validates that the proxy class has a method that returns an instance of the target class. */
    private fun validateProxyHasToTargetClassMethod() {
        val toTargetClassNameFunctionList: List<KSFunctionDeclaration> =
            classDeclaration
                .getAllFunctions()
                .filter { it.simpleName.asString() == toTargetClassMethodName }
                .toList()
        if (toTargetClassNameFunctionList.size != 1) {
            throw ProcessingException(
                "Class must have exactly one member function: $toTargetClassMethodName",
                classDeclaration,
            )
        }
        val toTargetClassNameFunction = toTargetClassNameFunctionList.first()
        if (
            checkNotNull(
                    checkNotNull(toTargetClassNameFunction.returnType)
                        .resolve()
                        .declaration
                        .qualifiedName
                )
                .asString() != checkNotNull(targetClassDeclaration.qualifiedName).asString()
        ) {
            throw ProcessingException(
                "Function $toTargetClassMethodName should return an instance of target class",
                classDeclaration,
            )
        }
    }

    /** Validates that the proxy class has a method that returns an instance of the target class. */
    private fun validateProxyHasFromTargetClassMethod() {
        val targetClassName = checkNotNull(targetClassDeclaration.simpleName).asString()
        val targetCompanionClass =
            classDeclaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.isCompanionObject }
                .single()

        val fromTargetClassNameFunctionList =
            targetCompanionClass
                .getAllFunctions()
                .filter { it.simpleName.asString() == fromTargetClassMethodName }
                .toList()
        if (fromTargetClassNameFunctionList.size != 1) {
            throw ProcessingException(
                "Companion Class must have exactly one member function: " +
                    fromTargetClassMethodName,
                classDeclaration,
            )
        }
        val fromTargetClassNameFunction = fromTargetClassNameFunctionList.first()
        if (
            fromTargetClassNameFunction.parameters.size != 1 ||
                fromTargetClassNameFunction.parameters.first().type.toTypeName().toString() !=
                    checkNotNull(targetClassDeclaration.qualifiedName).asString()
        ) {
            throw ProcessingException(
                "Function $fromTargetClassMethodName should have one parameter of type " +
                    targetClassName,
                classDeclaration,
            )
        }
        val returnTypeClassDeclaration =
            checkNotNull(fromTargetClassNameFunction.returnType).resolve().declaration
                as KSClassDeclaration
        if (
            checkNotNull(returnTypeClassDeclaration.qualifiedName).asString() !=
                checkNotNull(classDeclaration.qualifiedName).asString()
        ) {
            throw ProcessingException(
                "Function $fromTargetClassMethodName should return an instance of " +
                    "this serializable class (${checkNotNull(classDeclaration
                        .qualifiedName).asString()}). Instead, it returns ${checkNotNull(
                            returnTypeClassDeclaration.qualifiedName).asString()}",
                fromTargetClassNameFunction.returnType,
            )
        }
    }

    /**
     * A class that represents a list of resolved AnnotatedAppFunctionSerializableProxy.
     *
     * @param resolvedAnnotatedSerializableProxies The list of resolved
     *   AnnotatedAppFunctionSerializableProxy.
     */
    data class ResolvedAnnotatedSerializableProxies(
        val resolvedAnnotatedSerializableProxies: List<AnnotatedAppFunctionSerializableProxy>
    ) {
        private val proxyTargetToSerializableProxy:
            Map<ClassName, AnnotatedAppFunctionSerializableProxy> by lazy {
            resolvedAnnotatedSerializableProxies.associateBy {
                it.targetClassDeclaration.toClassName()
            }
        }

        /**
         * Returns the AnnotatedAppFunctionSerializableProxy for the given AppFunctionTypeReference.
         *
         * @param appFunctionTypeReference The AppFunctionTypeReference to get the
         *   AnnotatedAppFunctionSerializableProxy for.
         * @return The AnnotatedAppFunctionSerializableProxy for the given AppFunctionTypeReference.
         */
        fun getSerializableProxyForTypeReference(
            appFunctionTypeReference: AppFunctionTypeReference
        ): AnnotatedAppFunctionSerializableProxy {
            val targetClassName =
                (appFunctionTypeReference.selfOrItemTypeReference.resolve().declaration
                        as KSClassDeclaration)
                    .toClassName()
            return proxyTargetToSerializableProxy.getValue(targetClassName)
        }
    }

    override fun getFactoryCodeBuilder(
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies
    ): AppFunctionSerializableType.FactoryCodeBuilder {
        return AppFunctionSerializableProxyFactoryCodeBuilder(
            this,
            resolvedAnnotatedSerializableProxies,
        )
    }

    private class AppFunctionSerializableProxyFactoryCodeBuilder(
        val annotatedProxyClass: AnnotatedAppFunctionSerializableProxy,
        val resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
    ) : AppFunctionSerializableType.FactoryCodeBuilder {
        override fun buildAppFunctionSerializableFactoryClass(): FileSpec {

            val proxySuperInterfaceClass =
                AppFunctionSerializableFactoryClass.CLASS_NAME.parameterizedBy(
                    annotatedProxyClass.targetClassDeclaration.toClassName()
                )

            val serializableProxyClassBuilder =
                TypeSpec.classBuilder(annotatedProxyClass.factoryClassName)
            val factoryCodeBuilder =
                AppFunctionSerializableFactoryCodeBuilderHelper(
                    annotatedProxyClass,
                    resolvedAnnotatedSerializableProxies,
                )
            serializableProxyClassBuilder.addAnnotation(RESTRICT_API_TO_33_ANNOTATION)
            serializableProxyClassBuilder.addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
            serializableProxyClassBuilder.addSuperinterface(proxySuperInterfaceClass)
            annotatedProxyClass.targetClassDeclaration.toClassName()
            serializableProxyClassBuilder.addFunction(
                buildFromAppFunctionDataFunction(
                    factoryCodeBuilder.buildFromAppFunctionDataMethodBodyForProxy(),
                    returnType = annotatedProxyClass.targetClassDeclaration.toClassName(),
                )
            )
            serializableProxyClassBuilder.addFunction(
                buildToAppFunctionDataFunction(
                    factoryCodeBuilder.buildToAppFunctionDataMethodBodyForProxy(),
                    parameterType = annotatedProxyClass.targetClassDeclaration.toClassName(),
                )
            )
            return FileSpec.builder(
                    annotatedProxyClass.appFunctionSerializableTypeClassDeclaration
                        .originalClassName
                        .packageName,
                    annotatedProxyClass.factoryClassName.simpleName,
                )
                .addType(serializableProxyClassBuilder.build())
                .build()
        }
    }
}
