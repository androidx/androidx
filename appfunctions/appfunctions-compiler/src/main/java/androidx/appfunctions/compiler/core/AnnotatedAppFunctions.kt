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

import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionContextClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSchemaDefinitionAnnotation
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.TypeName
import kotlin.reflect.KClass

/**
 * Represents a collection of functions within a specific class that are annotated as app functions.
 */
data class AnnotatedAppFunctions(
    /** The [KSClassDeclaration] of the class that contains the annotated app functions. */
    val classDeclaration: KSClassDeclaration,
    /** The list of [KSFunctionDeclaration] that are annotated as app function. */
    val appFunctionDeclarations: List<KSFunctionDeclaration>
) {
    fun validate(): AnnotatedAppFunctions {
        validateFirstParameter()
        validateParameterTypes()
        return this
    }

    private fun validateFirstParameter() {
        for (appFunctionDeclaration in appFunctionDeclarations) {
            val firstParam = appFunctionDeclaration.parameters.firstOrNull()
            if (firstParam == null) {
                throw ProcessingException(
                    "The first parameter of an app function must be " +
                        "${AppFunctionContextClass.CLASS_NAME}",
                    appFunctionDeclaration
                )
            }
            if (!firstParam.type.isOfType(AppFunctionContextClass.CLASS_NAME)) {
                throw ProcessingException(
                    "The first parameter of an app function must be " +
                        "${AppFunctionContextClass.CLASS_NAME}",
                    firstParam
                )
            }
        }
    }

    private fun validateParameterTypes() {
        for (appFunctionDeclaration in appFunctionDeclarations) {
            for ((paramIndex, ksValueParameter) in appFunctionDeclaration.parameters.withIndex()) {
                if (paramIndex == 0) {
                    // Skip the first parameter which is always the `AppFunctionContext`.
                    continue
                }

                if (!isSupportedType(ksValueParameter.type)) {
                    throw ProcessingException(
                        "App function parameters must be a supported type, or a type " +
                            "annotated as @AppFunctionSerializable. See list of supported types:\n" +
                            "${
                                SUPPORTED_TYPES.joinToString(
                                    ",\n"
                                )
                            }\n" +
                            "but found ${
                                ksValueParameter.resolveTypeReference().ensureQualifiedTypeName()
                                    .asString()
                            }",
                        ksValueParameter
                    )
                }
            }
        }
    }

    /**
     * Gets the identifier of an app functions.
     *
     * The format of the identifier is `packageName.className#methodName`.
     */
    fun getAppFunctionIdentifier(functionDeclaration: KSFunctionDeclaration): String {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val methodName = functionDeclaration.simpleName.asString()
        return "${packageName}.${className}#${methodName}"
    }

    /**
     * Returns the set of files that need to be processed to obtain the complete information about
     * the app functions defined in this class.
     *
     * This includes the class file containing the function declarations, the class file containing
     * the schema definitions, and the class files containing the AppFunctionSerializable classes
     * used in the function parameters.
     */
    fun getSourceFiles(): Set<KSFile> {
        val sourceFileSet: MutableSet<KSFile> = mutableSetOf()

        // Add the class file containing the function declarations
        classDeclaration.containingFile?.let { sourceFileSet.add(it) }

        for (functionDeclaration in appFunctionDeclarations) {
            // Add the class file containing the schema definitions
            val rootAppFunctionSchemaInterface =
                findRootAppFunctionSchemaInterface(functionDeclaration)
            rootAppFunctionSchemaInterface?.containingFile?.let { sourceFileSet.add(it) }

            // Traverse each functions parameter to obtain the relevant AppFunctionSerializable
            // class files
            for (ksValueParameter in functionDeclaration.parameters) {
                if (isAppFunctionSerializableType(ksValueParameter.type)) {
                    val appFunctionSerializableClassDeclaration =
                        ksValueParameter.type.resolve().declaration as KSClassDeclaration
                    val annotatedSerializable =
                        AnnotatedAppFunctionSerializable(appFunctionSerializableClassDeclaration)
                    sourceFileSet.addAll(annotatedSerializable.getSourceFiles())
                }
            }
        }

        // Todo(b/391342300): Consider return value source file in case of returning an
        // AppFunctionSerializable
        return sourceFileSet
    }

    /** Gets the [classDeclaration]'s [ClassName]. */
    fun getEnclosingClassName(): ClassName {
        return ClassName(
            classDeclaration.packageName.asString(),
            classDeclaration.simpleName.asString()
        )
    }

    /**
     * Creates a list of [AppFunctionMetadata] instances for each of the app functions defined in
     * this class.
     */
    fun createAppFunctionMetadataList(): List<AppFunctionMetadata> =
        appFunctionDeclarations.map { functionDeclaration ->
            val appFunctionAnnotationProperties =
                computeAppFunctionAnnotationProperties(functionDeclaration)
            val parameterObjectTypeMetadata = functionDeclaration.buildParameterTypeMetadata()
            val responseObjectTypeMetadata =
                checkNotNull(functionDeclaration.returnType).buildResponseObjectTypeMetadata()

            AppFunctionMetadata(
                id = getAppFunctionIdentifier(functionDeclaration),
                isEnabledByDefault = appFunctionAnnotationProperties.isEnabledByDefault,
                schema = appFunctionAnnotationProperties.toAppFunctionSchemaMetadata(),
                parameters = parameterObjectTypeMetadata,
                response = AppFunctionResponseMetadata(valueType = responseObjectTypeMetadata)
            )
        }

    /**
     * Builds an [AppFunctionDataTypeMetadata] instance for the return type of an app function.
     *
     * Currently, only primitive return types are supported.
     */
    private fun KSTypeReference.buildResponseObjectTypeMetadata(): AppFunctionDataTypeMetadata {
        return toAppFunctionDataTypeMetadata()
    }

    /**
     * Builds a list of [AppFunctionParameterMetadata] for the parameters of an app function.
     *
     * Currently, only primitive parameters are supported.
     */
    private fun KSFunctionDeclaration.buildParameterTypeMetadata():
        List<AppFunctionParameterMetadata> = buildList {
        for (ksValueParameter in parameters) {
            if (ksValueParameter.type.isOfType(AppFunctionContextClass.CLASS_NAME)) {
                // Skip the first parameter which is always the `AppFunctionContext`.
                continue
            }

            // TODO: Support serializable and their collections
            val parameterName = checkNotNull(ksValueParameter.name).asString()
            val dataTypeMetadata = ksValueParameter.type.toAppFunctionDataTypeMetadata()

            add(
                AppFunctionParameterMetadata(
                    name = parameterName,
                    // TODO(b/394553462): Parse required state from annotation.
                    isRequired = true,
                    dataType = dataTypeMetadata
                )
            )
        }
    }

    private fun KSTypeReference.toAppFunctionDataTypeMetadata(): AppFunctionDataTypeMetadata {
        val isNullable = resolve().isMarkedNullable
        val typeName = toTypeName()
        return when (typeName.ignoreNullable().toString()) {
            in SUPPORTED_SINGLE_PRIMITIVE_TYPES ->
                AppFunctionPrimitiveTypeMetadata(
                    type = typeName.toAppFunctionDataType(),
                    isNullable = isNullable
                )
            in SUPPORTED_ARRAY_PRIMITIVE_TYPES,
            in SUPPORTED_COLLECTION_TYPES, ->
                AppFunctionArrayTypeMetadata(
                    itemType =
                        AppFunctionPrimitiveTypeMetadata(
                            type = typeName.determineArrayItemType(),
                            // TODO: Support List with nullable items.
                            isNullable = false
                        ),
                    isNullable = isNullable
                )
            else ->
                // TODO: Properly construct this when @AppFunctionSerializable is supported.
                AppFunctionObjectTypeMetadata(
                    properties = emptyMap(),
                    required = emptyList(),
                    qualifiedName = null,
                    isNullable = isNullable
                )
        }
    }

    private fun TypeName.toAppFunctionDataType(): Int =
        when (this.ignoreNullable().toString()) {
            String::class.ensureQualifiedName() -> AppFunctionDataTypeMetadata.TYPE_STRING
            Int::class.ensureQualifiedName() -> AppFunctionDataTypeMetadata.TYPE_INT
            Long::class.ensureQualifiedName() -> AppFunctionDataTypeMetadata.TYPE_LONG
            Float::class.ensureQualifiedName() -> AppFunctionDataTypeMetadata.TYPE_FLOAT
            Double::class.ensureQualifiedName() -> AppFunctionDataTypeMetadata.TYPE_DOUBLE
            Boolean::class.ensureQualifiedName() -> AppFunctionDataTypeMetadata.TYPE_BOOLEAN
            Unit::class.ensureQualifiedName() -> AppFunctionDataTypeMetadata.TYPE_UNIT
            ByteArray::class.ensureQualifiedName() -> AppFunctionDataTypeMetadata.TYPE_BYTES
            in SUPPORTED_ARRAY_PRIMITIVE_TYPES,
            in SUPPORTED_COLLECTION_TYPES, -> AppFunctionDataTypeMetadata.TYPE_ARRAY
            else -> TODO("Support AppFunctionSerializable types")
        }

    private fun TypeName.determineArrayItemType(): Int =
        when (this.ignoreNullable().toString()) {
            IntArray::class.ensureQualifiedName() -> AppFunctionDataTypeMetadata.TYPE_INT
            LongArray::class.ensureQualifiedName() -> AppFunctionDataTypeMetadata.TYPE_LONG
            FloatArray::class.ensureQualifiedName() -> AppFunctionDataTypeMetadata.TYPE_FLOAT
            DoubleArray::class.ensureQualifiedName() -> AppFunctionDataTypeMetadata.TYPE_DOUBLE
            BooleanArray::class.ensureQualifiedName() -> AppFunctionDataTypeMetadata.TYPE_BOOLEAN
            STRING_LIST_TYPE -> AppFunctionDataTypeMetadata.TYPE_STRING
            BYTE_ARRAY_LIST_TYPE -> AppFunctionDataTypeMetadata.TYPE_BYTES
            else -> TODO("Support Lists AppFunctionSerializable types")
        }

    private fun computeAppFunctionAnnotationProperties(
        functionDeclaration: KSFunctionDeclaration
    ): AppFunctionAnnotationProperties {
        val appFunctionAnnotation =
            functionDeclaration.annotations.findAnnotation(AppFunctionAnnotation.CLASS_NAME)
                ?: throw ProcessingException(
                    "Function not annotated with @AppFunction.",
                    functionDeclaration
                )
        val enabled =
            appFunctionAnnotation.requirePropertyValueOfType(
                AppFunctionAnnotation.PROPERTY_IS_ENABLED,
                Boolean::class,
            )

        val rootInterfaceWithAppFunctionSchemaDefinition =
            findRootAppFunctionSchemaInterface(functionDeclaration)

        val schemaFunctionAnnotation =
            rootInterfaceWithAppFunctionSchemaDefinition
                ?.annotations
                ?.findAnnotation(AppFunctionSchemaDefinitionAnnotation.CLASS_NAME)
        val schemaCategory =
            schemaFunctionAnnotation?.requirePropertyValueOfType(
                AppFunctionSchemaDefinitionAnnotation.PROPERTY_CATEGORY,
                String::class,
            )
        val schemaName =
            schemaFunctionAnnotation?.requirePropertyValueOfType(
                AppFunctionSchemaDefinitionAnnotation.PROPERTY_NAME,
                String::class,
            )
        val schemaVersion =
            schemaFunctionAnnotation
                ?.requirePropertyValueOfType(
                    AppFunctionSchemaDefinitionAnnotation.PROPERTY_VERSION,
                    Int::class,
                )
                ?.toLong()

        return AppFunctionAnnotationProperties(enabled, schemaName, schemaVersion, schemaCategory)
    }

    private fun findRootAppFunctionSchemaInterface(
        function: KSFunctionDeclaration,
    ): KSClassDeclaration? {
        val parentDeclaration = function.parentDeclaration as? KSClassDeclaration ?: return null

        // Check if the enclosing class has the @AppFunctionSchemaDefinition
        val annotation =
            parentDeclaration.annotations.findAnnotation(
                AppFunctionSchemaDefinitionAnnotation.CLASS_NAME
            )
        if (annotation != null) {
            return parentDeclaration
        }

        val superClassFunction = (function.findOverridee() as? KSFunctionDeclaration) ?: return null
        return findRootAppFunctionSchemaInterface(superClassFunction)
    }

    /**
     * Resolves the type reference of a parameter.
     *
     * If the parameter type is a list, it will resolve the type reference of the list element.
     */
    private fun KSValueParameter.resolveTypeReference(): KSTypeReference {
        return if (type.isOfType(LIST)) {
            type.resolveListParameterizedType()
        } else {
            type
        }
    }

    private fun AppFunctionAnnotationProperties.toAppFunctionSchemaMetadata():
        AppFunctionSchemaMetadata? {
        return if (this.schemaName != null) {
            AppFunctionSchemaMetadata(
                category = checkNotNull(this.schemaCategory),
                name = this.schemaName,
                version = checkNotNull(this.schemaVersion)
            )
        } else {
            null
        }
    }

    private data class AppFunctionAnnotationProperties(
        val isEnabledByDefault: Boolean,
        val schemaName: String?,
        val schemaVersion: Long?,
        val schemaCategory: String?
    )

    companion object {
        /**
         * Checks if the type reference is a supported type.
         *
         * A supported type is a primitive type, a type annotated as @AppFunctionSerializable, or a
         * list of a supported type.
         */
        fun isSupportedType(typeReferenceArgument: KSTypeReference): Boolean {
            return SUPPORTED_TYPES.contains(
                typeReferenceArgument.toTypeName().ignoreNullable().toString()
            ) || isAppFunctionSerializableType(typeReferenceArgument)
        }

        /**
         * Checks if the type reference is annotated as @AppFunctionSerializable.
         *
         * If the type reference is a list, it will resolve the type reference of the list element.
         */
        fun isAppFunctionSerializableType(typeReferenceArgument: KSTypeReference): Boolean {
            var typeToCheck = typeReferenceArgument
            if (typeReferenceArgument.isOfType(LIST)) {
                typeToCheck = typeReferenceArgument.resolveListParameterizedType()
            }
            return typeToCheck
                .resolve()
                .declaration
                .annotations
                .findAnnotation(IntrospectionHelper.AppFunctionSerializableAnnotation.CLASS_NAME) !=
                null
        }

        private fun TypeName.ignoreNullable(): TypeName {
            return copy(nullable = false)
        }

        private fun KClass<*>.ensureQualifiedName(): String = checkNotNull(qualifiedName)

        internal val SUPPORTED_ARRAY_PRIMITIVE_TYPES =
            setOf(
                IntArray::class.ensureQualifiedName(),
                LongArray::class.ensureQualifiedName(),
                FloatArray::class.ensureQualifiedName(),
                DoubleArray::class.ensureQualifiedName(),
                BooleanArray::class.ensureQualifiedName(),
            )

        internal val SUPPORTED_SINGLE_PRIMITIVE_TYPES =
            setOf(
                Int::class.ensureQualifiedName(),
                Long::class.ensureQualifiedName(),
                Float::class.ensureQualifiedName(),
                Double::class.ensureQualifiedName(),
                Boolean::class.ensureQualifiedName(),
                String::class.ensureQualifiedName(),
                ByteArray::class.ensureQualifiedName(),
                Unit::class.ensureQualifiedName(),
            )

        private const val STRING_LIST_TYPE = "kotlin.collections.List<kotlin.String>"
        private const val BYTE_ARRAY_LIST_TYPE = "kotlin.collections.List<kotlin.ByteArray>"

        internal val SUPPORTED_COLLECTION_TYPES = setOf(STRING_LIST_TYPE, BYTE_ARRAY_LIST_TYPE)

        internal val SUPPORTED_TYPES =
            SUPPORTED_SINGLE_PRIMITIVE_TYPES +
                SUPPORTED_ARRAY_PRIMITIVE_TYPES +
                SUPPORTED_COLLECTION_TYPES
    }
}
