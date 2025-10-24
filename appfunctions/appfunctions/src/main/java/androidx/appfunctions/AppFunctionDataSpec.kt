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

package androidx.appfunctions

import android.app.PendingIntent
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBytesTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.metadata.AppFunctionFloatTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionOneOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPendingIntentTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import java.util.Objects

/** Specification class defining the properties metadata for [AppFunctionData]. */
internal abstract class AppFunctionDataSpec {
    abstract val objectQualifiedName: String
    abstract val componentMetadata: AppFunctionComponentsMetadata

    internal abstract fun getDataType(key: String): AppFunctionDataTypeMetadata?

    /** Checks if [key] must be set with non-null value in [AppFunctionData]. */
    internal abstract fun isRequired(key: String): Boolean

    internal abstract fun getAllPropertyKeys(): Set<String>

    /** Checks if there is a metadata for [key]. */
    fun containsMetadata(key: String): Boolean {
        return getDataType(key) != null
    }

    /**
     * Gets the property object spec associated with [key].
     *
     * If the property associated with [key] is an Array, it would return the item object's
     * specification.
     *
     * @param key The property key.
     * @param qualifiedName The qualified name of the object, used in creating the corresponding
     *   [AppFunctionData].
     * @throws IllegalArgumentException If this is no child specification associated with [key].
     */
    fun getPropertyObjectSpec(key: String, qualifiedName: String): AppFunctionDataSpec {
        val childDataType =
            getDataType(key)
                ?: throw IllegalArgumentException("Value associated with $key is not an object")
        return getPropertyObjectSpec(childDataType, qualifiedName)
    }

    private fun getPropertyObjectSpec(
        type: AppFunctionDataTypeMetadata,
        qualifiedName: String,
    ): AppFunctionDataSpec {
        return when (type) {
            is AppFunctionArrayTypeMetadata -> {
                getPropertyObjectSpec(type.itemType, qualifiedName)
            }
            is AppFunctionObjectTypeMetadata -> {
                ObjectSpec(type, componentMetadata)
            }
            is AppFunctionReferenceTypeMetadata -> {
                val resolvedDataType =
                    componentMetadata.dataTypes[type.referenceDataType]
                        ?: throw IllegalStateException(
                            "Unable to resolve data type for ${type.referenceDataType}"
                        )
                getPropertyObjectSpec(resolvedDataType, qualifiedName)
            }
            is AppFunctionAllOfTypeMetadata -> {
                ObjectSpec(type.getPseudoObjectTypeMetadata(componentMetadata), componentMetadata)
            }
            is AppFunctionOneOfTypeMetadata -> {
                val oneOfType = type.getObjectMetadataForOneOfType(qualifiedName)
                getPropertyObjectSpec(oneOfType, qualifiedName)
            }
            else -> {
                throw IllegalStateException("Unexpected data type $type")
            }
        }
    }

    /**
     * Validates if [data] matches the current [AppFunctionDataSpec].
     *
     * @throws IllegalArgumentException If the [data] does not match the specification.
     */
    fun validateDataSpecMatches(data: AppFunctionData) {
        // TODO(b/447064745): Fix child object validation when spec is from schema inventory
        val unused = data.spec ?: return
        //        require(this == otherSpec) { "$data does not match the metadata specification of
        // $this" }
    }

    /**
     * Validates if a write request to set a value of type [targetClass] to [targetKey] is valid.
     *
     * @param isCollection Indicates if the write request is a collection of [targetClass].
     * @param targetValue The value to be validated against any constraints if specified by the
     *   metadata.
     * @throws IllegalArgumentException If the request is invalid.
     */
    fun validateWriteRequest(
        targetKey: String,
        targetClass: Class<*>,
        isCollection: Boolean,
        targetValue: Any,
    ) {
        val targetDataTypeMetadata = getDataType(targetKey)
        if (targetDataTypeMetadata == null) {
            throw IllegalArgumentException("No value should be set at $targetKey")
        }
        require(targetDataTypeMetadata.conform(targetClass, isCollection)) {
            if (isCollection) {
                "Invalid value for $targetKey: got collection of $targetClass, " +
                    "expecting a value matching $targetDataTypeMetadata"
            } else {
                "Invalid value for $targetKey: got $targetClass, " +
                    "expecting a value matching $targetDataTypeMetadata"
            }
        }

        targetDataTypeMetadata.requireConstraintsConformance(targetKey, targetValue)
    }

    /**
     * Validates if a read request to get a value of type [targetClass] from [targetKey] is valid.
     *
     * @param isCollection Indicates if the write request is a collection of [targetClass].
     * @param targetValue The value to be validated against any constraints if specified by the
     *   metadata.
     * @throws IllegalArgumentException If the request is invalid.
     */
    fun validateReadRequest(
        targetKey: String,
        targetClass: Class<*>,
        isCollection: Boolean,
        targetValue: Any? = null,
    ) {
        val targetDataTypeMetadata = getDataType(targetKey)
        if (targetDataTypeMetadata == null) {
            throw IllegalArgumentException("No value should be set at $targetKey")
        }
        require(targetDataTypeMetadata.conform(targetClass, isCollection)) {
            if (isCollection) {
                "Unexpected read for $targetKey: expecting collection of $targetClass, " +
                    "the actual value should be $targetDataTypeMetadata"
            } else {
                "Unexpected read for $targetKey: expecting $targetClass, " +
                    "the actual value should be $targetDataTypeMetadata"
            }
        }

        targetDataTypeMetadata.requireConstraintsConformance(targetKey, targetValue)
    }

    private fun AppFunctionDataTypeMetadata.requireConstraintsConformance(
        targetKey: String,
        targetValue: Any?,
    ) {
        // targetValue == null is allowed when the data type is nullable or is marked optional in
        // either ObjectSpec or ParameterSpec.
        require(targetValue != null || !isRequired(targetKey)) {
            "\"$targetKey\" cannot be set to a null value."
        }

        // If null is allowed, no need to check for constraint conformance.
        if (targetValue == null) return

        when (this) {
            is AppFunctionIntTypeMetadata -> {
                require(enumValues == null || enumValues.contains(targetValue)) {
                    "Invalid value for \"$targetKey\" got \"$targetValue\", expecting one of $enumValues"
                }
            }
            is AppFunctionStringTypeMetadata -> {
                require(enumValues == null || enumValues.contains(targetValue)) {
                    "Invalid value for \"$targetKey\" got \"$targetValue\", expecting one of $enumValues"
                }
            }
            is AppFunctionArrayTypeMetadata -> {
                this.requireItemTypeConstraintsConformance(targetKey, targetValue)
            }

            else -> {}
        }
    }

    private fun AppFunctionArrayTypeMetadata.requireItemTypeConstraintsConformance(
        targetKey: String,
        targetValue: Any?,
    ) {
        when (itemType) {
            is AppFunctionIntTypeMetadata -> {
                val intArray = targetValue as? IntArray
                for (item in intArray ?: intArrayOf()) {
                    itemType.requireConstraintsConformance(targetKey, item)
                }
            }

            is AppFunctionStringTypeMetadata -> {
                @Suppress("UNCHECKED_CAST") val stringList = targetValue as? List<String>
                for (item in stringList ?: emptyList()) {
                    itemType.requireConstraintsConformance(targetKey, item)
                }
            }

            else -> {}
        }
    }

    private data class ObjectSpec(
        private val objectTypeMetadata: AppFunctionObjectTypeMetadata,
        override val componentMetadata: AppFunctionComponentsMetadata,
    ) : AppFunctionDataSpec() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ObjectSpec) return false

            // TODO(b/446606781): Comparing component metadata
            return this.objectTypeMetadata == other.objectTypeMetadata
        }

        override fun hashCode(): Int {
            return Objects.hash(objectTypeMetadata)
        }

        override val objectQualifiedName: String
            get() = objectTypeMetadata.qualifiedName ?: ""

        override fun getDataType(key: String): AppFunctionDataTypeMetadata? {
            return objectTypeMetadata.properties[key]
        }

        override fun isRequired(key: String): Boolean {
            val isRequired = objectTypeMetadata.required.contains(key)
            val isNullable = objectTypeMetadata.properties[key]?.isNullable ?: true
            // A field is only required when it is in required list AND being non-null. A nullable
            // required field is considered as optional from data validation's perspective.
            return isRequired && !isNullable
        }

        override fun getAllPropertyKeys(): Set<String> = objectTypeMetadata.properties.keys
    }

    private data class ParametersSpec(
        private val parameterMetadataList: List<AppFunctionParameterMetadata>,
        override val componentMetadata: AppFunctionComponentsMetadata,
    ) : AppFunctionDataSpec() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ParametersSpec) return false

            // TODO(b/446606781): Comparing component metadata
            return this.parameterMetadataList == other.parameterMetadataList
        }

        override fun hashCode(): Int {
            return Objects.hash(parameterMetadataList)
        }

        override val objectQualifiedName: String
            get() = ""

        override fun getDataType(key: String): AppFunctionDataTypeMetadata? {
            return parameterMetadataList.firstOrNull { it.name == key }?.dataType
        }

        override fun isRequired(key: String): Boolean {
            val isRequired =
                parameterMetadataList.firstOrNull { it.name == key }?.isRequired ?: false
            val isNullable =
                parameterMetadataList.firstOrNull { it.name == key }?.dataType?.isNullable ?: true
            // A field is only required when it is in required list AND being non-null. A nullable
            // required field is considered as optional from data validation's perspective.
            return isRequired && !isNullable
        }

        override fun getAllPropertyKeys(): Set<String> =
            parameterMetadataList.map { it.name }.toSet()
    }

    fun AppFunctionDataTypeMetadata.conform(typeClazz: Class<*>, isCollection: Boolean): Boolean {
        return when (this) {
            is AppFunctionIntTypeMetadata -> {
                !isCollection && typeClazz == Int::class.java
            }
            is AppFunctionLongTypeMetadata -> {
                !isCollection && typeClazz == Long::class.java
            }
            is AppFunctionFloatTypeMetadata -> {
                !isCollection && typeClazz == Float::class.java
            }
            is AppFunctionDoubleTypeMetadata -> {
                !isCollection && typeClazz == Double::class.java
            }
            is AppFunctionBooleanTypeMetadata -> {
                !isCollection && typeClazz == Boolean::class.java
            }
            is AppFunctionStringTypeMetadata -> {
                !isCollection && typeClazz == String::class.java
            }
            is AppFunctionBytesTypeMetadata -> {
                // Unlike other primitive array types, AppFunction does not support Byte itself.
                // Instead, ByteArray is considered as a primitive type. Therefore, when validating
                // against AppFunctionBytesTypeMetadata, it must always be a collection.
                isCollection && typeClazz == Byte::class.java
            }
            is AppFunctionPendingIntentTypeMetadata -> {
                !isCollection && typeClazz == PendingIntent::class.java
            }
            is AppFunctionArrayTypeMetadata -> {
                isCollection && this.conform(typeClazz)
            }
            is AppFunctionObjectTypeMetadata -> {
                !isCollection && this.conform(typeClazz)
            }
            is AppFunctionAllOfTypeMetadata -> {
                !isCollection && this.conform(typeClazz)
            }
            is AppFunctionReferenceTypeMetadata -> {
                !isCollection && this.conform(typeClazz)
            }
            is AppFunctionOneOfTypeMetadata -> {
                !isCollection && this.conform(typeClazz)
            }
            else -> {
                throw IllegalStateException("Unexpected data type ${this.javaClass}")
            }
        }
    }

    private fun AppFunctionArrayTypeMetadata.conform(itemTypeClass: Class<*>): Boolean {
        return this.itemType.conform(itemTypeClass, isCollection = false)
    }

    private fun AppFunctionObjectTypeMetadata.conform(typeClass: Class<*>): Boolean {
        return typeClass == AppFunctionData::class.java
    }

    private fun AppFunctionAllOfTypeMetadata.conform(typeClass: Class<*>): Boolean {
        return typeClass == AppFunctionData::class.java
    }

    private fun AppFunctionOneOfTypeMetadata.conform(typeClass: Class<*>): Boolean {
        return typeClass == AppFunctionData::class.java
    }

    private fun AppFunctionReferenceTypeMetadata.conform(typeClass: Class<*>): Boolean {
        // Reference Type is always an object type
        return typeClass == AppFunctionData::class.java
    }

    companion object {
        fun create(
            objectType: AppFunctionObjectTypeMetadata,
            componentMetadata: AppFunctionComponentsMetadata,
        ): AppFunctionDataSpec {
            return ObjectSpec(objectType, componentMetadata)
        }

        fun create(
            parameterMetadataList: List<AppFunctionParameterMetadata>,
            componentMetadata: AppFunctionComponentsMetadata,
        ): AppFunctionDataSpec {
            return ParametersSpec(parameterMetadataList, componentMetadata)
        }

        fun create(
            responseMetadata: AppFunctionResponseMetadata,
            componentMetadata: AppFunctionComponentsMetadata,
        ): AppFunctionDataSpec {
            return ObjectSpec(
                AppFunctionObjectTypeMetadata(
                    properties =
                        mapOf(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE to
                                responseMetadata.valueType
                        ),
                    required = listOf(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE),
                    qualifiedName = null,
                    isNullable = false,
                ),
                componentMetadata,
            )
        }
    }
}
