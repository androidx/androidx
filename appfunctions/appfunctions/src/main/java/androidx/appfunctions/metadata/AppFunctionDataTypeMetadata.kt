/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appfunctions.metadata

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.Bundle
import android.os.PatternMatcher
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.appfunctions.internal.PatternMatchers
import androidx.appfunctions.internal.getNonNullStringList
import androidx.appfunctions.internal.requireBundle
import androidx.appfunctions.internal.requireBundleList
import androidx.appfunctions.internal.requireNonNullStringList
import androidx.appfunctions.internal.requireString
import androidx.appsearch.annotation.Document
import java.util.Objects

@IntDef(
    AppFunctionDataTypeMetadata.TYPE_UNIT,
    AppFunctionDataTypeMetadata.TYPE_BOOLEAN,
    AppFunctionDataTypeMetadata.TYPE_BYTES,
    AppFunctionDataTypeMetadata.TYPE_OBJECT,
    AppFunctionDataTypeMetadata.TYPE_DOUBLE,
    AppFunctionDataTypeMetadata.TYPE_FLOAT,
    AppFunctionDataTypeMetadata.TYPE_LONG,
    AppFunctionDataTypeMetadata.TYPE_INT,
    AppFunctionDataTypeMetadata.TYPE_STRING,
    AppFunctionDataTypeMetadata.TYPE_ARRAY,
    AppFunctionDataTypeMetadata.TYPE_REFERENCE,
    AppFunctionDataTypeMetadata.TYPE_ALL_OF,
    AppFunctionDataTypeMetadata.TYPE_ONE_OF,
    AppFunctionDataTypeMetadata.TYPE_PARCELABLE,
)
@Retention(AnnotationRetention.SOURCE)
internal annotation class AppFunctionDataType

/** Base class for defining the schema of an input or output type. */
public abstract class AppFunctionDataTypeMetadata
internal constructor(
    /** Whether the data type is nullable. */
    public val isNullable: Boolean,
    /** A description of the data type and its intended use. */
    public val description: String,
) {

    /**
     * Converts this [AppFunctionDataTypeMetadata] to a [Bundle].
     *
     * @throws IllegalArgumentException if this metadata is nested deeper than [MAX_BUNDLE_DEPTH].
     */
    internal fun toBundle(): Bundle = toBundle(depth = 0)

    internal fun toBundle(depth: Int): Bundle {
        require(depth < MAX_BUNDLE_DEPTH) {
            "Data type metadata is nested deeper than $MAX_BUNDLE_DEPTH"
        }
        return Bundle().apply {
            putBoolean(KEY_IS_NULLABLE, isNullable)
            putString(KEY_DESCRIPTION, description)
            writeToBundle(this, depth)
        }
    }

    /** Writes [KEY_TYPE] and subclass-specific properties into [bundle]. */
    internal abstract fun writeToBundle(bundle: Bundle, depth: Int)

    /**
     * Checks if this metadata is semantically equivalent to [other].
     *
     * @throws IllegalArgumentException If they are not matching.
     */
    internal fun requireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>> = mutableSetOf(),
    ) {
        if (this === other && thisComponent === otherComponent) {
            return
        }
        require(this.isNullable == other.isNullable) {
            "Nullable mismatch. Expected: ${this.isNullable}, actual: ${other.isNullable}"
        }
        internalRequireSemanticallyEquivalentTo(other, thisComponent, otherComponent, visitingPairs)
    }

    internal abstract fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>> = mutableSetOf(),
    )

    internal fun tryResolveDataType(
        dataType: AppFunctionDataTypeMetadata,
        component: AppFunctionComponentsMetadata,
    ): AppFunctionDataTypeMetadata {
        return if (dataType is AppFunctionReferenceTypeMetadata) {
            val resolvedType =
                component.dataTypes[dataType.referenceDataType]
                    ?: throw IllegalArgumentException(
                        "Unable to resolve ${dataType.referenceDataType}"
                    )
            if (resolvedType is AppFunctionReferenceTypeMetadata) {
                tryResolveDataType(resolvedType, component)
            } else {
                resolvedType
            }
        } else {
            dataType
        }
    }

    public companion object {
        /**
         * Creates an [AppFunctionDataTypeMetadata] from a [Bundle] created by [toBundle].
         *
         * @throws IllegalArgumentException if [bundle] is malformed or nested deeper than
         *   [MAX_BUNDLE_DEPTH].
         */
        internal fun fromBundle(bundle: Bundle): AppFunctionDataTypeMetadata =
            try {
                fromBundle(bundle, depth = 0)
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: Exception) {
                throw IllegalArgumentException("Malformed data type metadata bundle", e)
            }

        internal fun fromBundle(bundle: Bundle, depth: Int): AppFunctionDataTypeMetadata {
            require(depth < MAX_BUNDLE_DEPTH) {
                "Data type metadata is nested deeper than $MAX_BUNDLE_DEPTH"
            }
            require(bundle.containsKey(KEY_TYPE)) { "Missing data type" }
            val isNullable = bundle.getBoolean(KEY_IS_NULLABLE, false)
            val description = bundle.getString(KEY_DESCRIPTION) ?: ""
            return when (val type = bundle.getInt(KEY_TYPE, -1)) {
                TYPE_UNIT -> AppFunctionUnitTypeMetadata.fromBundle(isNullable, description)
                TYPE_BOOLEAN -> AppFunctionBooleanTypeMetadata.fromBundle(isNullable, description)
                TYPE_BYTES -> AppFunctionBytesTypeMetadata.fromBundle(isNullable, description)
                TYPE_DOUBLE -> AppFunctionDoubleTypeMetadata.fromBundle(isNullable, description)
                TYPE_FLOAT -> AppFunctionFloatTypeMetadata.fromBundle(isNullable, description)
                TYPE_LONG -> AppFunctionLongTypeMetadata.fromBundle(isNullable, description)
                TYPE_INT -> AppFunctionIntTypeMetadata.fromBundle(bundle, isNullable, description)
                TYPE_STRING ->
                    AppFunctionStringTypeMetadata.fromBundle(bundle, isNullable, description)
                TYPE_PARCELABLE ->
                    AppFunctionParcelableTypeMetadata.fromBundle(bundle, isNullable, description)
                TYPE_REFERENCE ->
                    AppFunctionReferenceTypeMetadata.fromBundle(bundle, isNullable, description)
                TYPE_ARRAY ->
                    AppFunctionArrayTypeMetadata.fromBundle(bundle, isNullable, description, depth)
                TYPE_OBJECT ->
                    AppFunctionObjectTypeMetadata.fromBundle(bundle, isNullable, description, depth)
                TYPE_ALL_OF ->
                    AppFunctionAllOfTypeMetadata.fromBundle(bundle, isNullable, description, depth)
                TYPE_ONE_OF ->
                    AppFunctionOneOfTypeMetadata.fromBundle(bundle, isNullable, description, depth)
                else -> throw IllegalArgumentException("Unknown data type: $type")
            }
        }

        private const val MAX_BUNDLE_DEPTH = 64

        internal const val KEY_TYPE = "type"
        private const val KEY_IS_NULLABLE = "isNullable"
        private const val KEY_DESCRIPTION = "description"

        /** Void type. */
        internal const val TYPE_UNIT: Int = 0
        /** Boolean type. */
        internal const val TYPE_BOOLEAN: Int = 1
        /** Byte array type. */
        internal const val TYPE_BYTES: Int = 2
        /**
         * Object type. The schema of the object is defined in a [AppFunctionObjectTypeMetadata].
         */
        internal const val TYPE_OBJECT: Int = 3
        /** Double type. */
        internal const val TYPE_DOUBLE: Int = 4
        /** Float type. */
        internal const val TYPE_FLOAT: Int = 5
        /** Long type. */
        internal const val TYPE_LONG: Int = 6
        /** Integer type. */
        internal const val TYPE_INT: Int = 7
        /** String type. */
        internal const val TYPE_STRING: Int = 8
        /** Array type. The schema of the array is defined in a [AppFunctionArrayTypeMetadata] */
        internal const val TYPE_ARRAY: Int = 10
        /**
         * Reference type. The schema of the reference is defined in a
         * [AppFunctionReferenceTypeMetadata]
         */
        internal const val TYPE_REFERENCE: Int = 11
        /**
         * All of type. The schema of the all of type is defined in a [AppFunctionAllOfTypeMetadata]
         */
        internal const val TYPE_ALL_OF: Int = 12

        /** Parcelable type. */
        internal const val TYPE_PARCELABLE: Int = 13

        /**
         * One of type. The schema of the one of type is defined in a [AppFunctionOneOfTypeMetadata]
         */
        internal const val TYPE_ONE_OF: Int = 14
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionDataTypeMetadata

        if (description != other.description) return false
        return isNullable == other.isNullable
    }

    override fun hashCode(): Int {
        return 31 * description.hashCode() + isNullable.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionDataTypeMetadata(isNullable=$isNullable, description=$description)"
    }
}

/** Defines the schema of an array data type. */
public class AppFunctionArrayTypeMetadata
@JvmOverloads
constructor(
    /** The type of items in the array. */
    public val itemType: AppFunctionDataTypeMetadata,
    /** Whether this data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {
    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        if (other !is AppFunctionArrayTypeMetadata) return false
        return itemType == other.itemType
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + itemType.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppFunctionArrayTypeMetadataDocument(" +
            "itemType=$itemType, " +
            "isNullable=$isNullable," +
            "description=$description" +
            ")"
    }

    override fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>>,
    ) {
        val otherResolved = tryResolveDataType(other, otherComponent)
        require(otherResolved is AppFunctionArrayTypeMetadata) {
            "Expect ${AppFunctionArrayTypeMetadata::class.java} but found ${otherResolved.javaClass}"
        }
        try {
            this.itemType.requireSemanticallyEquivalentTo(
                otherResolved.itemType,
                thisComponent,
                otherComponent,
                visitingPairs,
            )
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Type mismatch in Array items: ${e.message}", e)
        }
    }

    override fun writeToBundle(bundle: Bundle, depth: Int) {
        bundle.putInt(KEY_TYPE, TYPE_ARRAY)
        bundle.putBundle(KEY_ITEM_TYPE, itemType.toBundle(depth + 1))
    }

    public companion object {
        /** Array type. The schema of the array is defined in a [AppFunctionArrayTypeMetadata] */
        internal const val TYPE: Int = TYPE_ARRAY

        private const val KEY_ITEM_TYPE = "itemType"

        internal fun fromBundle(
            bundle: Bundle,
            isNullable: Boolean,
            description: String,
            depth: Int,
        ): AppFunctionArrayTypeMetadata =
            AppFunctionArrayTypeMetadata(
                itemType =
                    AppFunctionDataTypeMetadata.fromBundle(
                        bundle.requireBundle(KEY_ITEM_TYPE),
                        depth + 1,
                    ),
                isNullable = isNullable,
                description = description,
            )
    }
}

/**
 * Defines the schema of a single object data type that is a composition of all of the other types
 * in the [matchAll] list.
 *
 * An object of this type must match all of the [AppFunctionDataTypeMetadata] in the [matchAll]
 * list. [matchAll] takes an array of object definitions that are composed together to a single
 * object to form this type. Note that while this composition offers object type extensibility, it
 * does not imply a hierarchy between the objects matched in [matchAll] i.e. the resulting single
 * object is a flattened representation of all the other matched objects.
 *
 * For example, consider the following objects:
 * ```
 * package com.example.myapp
 *
 * open class Address (
 *     open val street: String,
 *     open val city: String,
 *     open val state: String,
 *     open val zipCode: String,
 * )
 *
 * class PersonWithAddress (
 *     override val street: String,
 *     override val city: String,
 *     override val state: String,
 *     override val zipCode: String,
 *     val name: String,
 *     val age: Int,
 * ) : Address(street, city, state, zipCode)
 * ```
 *
 * The following [AppFunctionAllOfTypeMetadata] can be used to define a data type that matches
 * PersonWithAddress.
 *
 * ```
 * val personWithAddressType = AppFunctionAllOfTypeMetadata(
 *     qualifiedName = "com.example.myapp.PersonWithAddress",
 *     matchAll = listOf(
 *         AppFunctionObjectTypeMetadata(
 *             properties = mapOf(
 *                 "street" to AppFunctionStringTypeMetadata(...),
 *                 "city" to AppFunctionStringTypeMetadata(...),
 *                 "state" to AppFunctionStringTypeMetadata(...),
 *                 "zipCode" to AppFunctionStringTypeMetadata(...),
 *             ),
 *             required = listOf("street", "city", "state", "zipCode"),
 *             qualifiedName = "com.example.myapp.Address",
 *             isNullable = false,
 *         ),
 *         AppFunctionObjectTypeMetadata(
 *             properties = mapOf(
 *                 "name" to AppFunctionStringTypeMetadata(...),
 *                 "age" to AppFunctionIntTypeMetadata(...),
 *             ),
 *             required = listOf("name", "age"),
 *             qualifiedName = "com.example.myapp.PersonWithAddress",
 *             isNullable = false,
 *         ),
 *     ),
 *     isNullable = false,
 * )
 * ```
 *
 * This data type can be used to define the schema of an input or output type.
 */
public class AppFunctionAllOfTypeMetadata
@JvmOverloads
constructor(
    /** The list of data types that are composed. */
    public val matchAll: List<AppFunctionDataTypeMetadata>,
    /**
     * The composed object's qualified name if available. For example,
     * "androidx.appfunctions.metadata.PersonWithAddress".
     *
     * Use this value to set [androidx.appfunctions.AppFunctionData.qualifiedName] when trying to
     * build the parameters for [androidx.appfunctions.ExecuteAppFunctionRequest].
     */
    public val qualifiedName: String?,
    /** Whether this data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        if (other !is AppFunctionAllOfTypeMetadata) return false
        if (qualifiedName != other.qualifiedName) return false
        return matchAll == other.matchAll
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + matchAll.hashCode()
        if (qualifiedName != null) {
            result = 31 * result + qualifiedName.hashCode()
        }
        return result
    }

    override fun toString(): String {
        return "AppFunctionAllOfTypeMetadata(matchAll=$matchAll, isNullable=$isNullable, description=$description)"
    }

    override fun writeToBundle(bundle: Bundle, depth: Int) {
        bundle.putInt(KEY_TYPE, TYPE_ALL_OF)
        bundle.putParcelableArrayList(
            KEY_ALL_OF,
            ArrayList(matchAll.map { it.toBundle(depth + 1) }),
        )
        if (qualifiedName != null) {
            bundle.putString(KEY_QUALIFIED_NAME, qualifiedName)
        }
    }

    /** Gets a pseudo [AppFunctionObjectTypeMetadata] by merging the [matchAll] data types. */
    // This cannot be internal since integration-test needs to use it to verify the AllOf type
    // serialization validation.
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getPseudoObjectTypeMetadata(
        componentsMetadata: AppFunctionComponentsMetadata
    ): AppFunctionObjectTypeMetadata {
        val allProperties = mutableMapOf<String, AppFunctionDataTypeMetadata>()
        val allRequired = mutableSetOf<String>()
        for (type in matchAll) {
            when (type) {
                is AppFunctionReferenceTypeMetadata -> {
                    val resolvedType = componentsMetadata.dataTypes[type.referenceDataType]
                    if (resolvedType == null) {
                        throw IllegalStateException(
                            "Unable to resolve the ${type.referenceDataType}"
                        )
                    }
                    if (resolvedType !is AppFunctionObjectTypeMetadata) {
                        continue
                    }
                    allProperties.putAll(resolvedType.properties)
                    allRequired.addAll(resolvedType.required)
                }
                is AppFunctionObjectTypeMetadata -> {
                    allProperties.putAll(type.properties)
                    allRequired.addAll(type.required)
                }
                is AppFunctionAllOfTypeMetadata -> {
                    val pseudoObjectType = type.getPseudoObjectTypeMetadata(componentsMetadata)
                    allProperties.putAll(pseudoObjectType.properties)
                    allRequired.addAll(pseudoObjectType.required)
                }
            }
        }
        return AppFunctionObjectTypeMetadata(
            properties = allProperties,
            required = allRequired.toList(),
            qualifiedName = qualifiedName,
            isNullable = false,
            description = "",
        )
    }

    override fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>>,
    ) {
        val otherResolved = tryResolveDataType(other, otherComponent)
        require(otherResolved is AppFunctionAllOfTypeMetadata) {
            "Expect ${AppFunctionAllOfTypeMetadata::class.java} but found ${otherResolved.javaClass}"
        }
        try {
            this.getPseudoObjectTypeMetadata(thisComponent)
                .requireSemanticallyEquivalentTo(
                    otherResolved.getPseudoObjectTypeMetadata(otherComponent),
                    thisComponent,
                    otherComponent,
                    visitingPairs,
                )
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Type mismatch in AllOf properties: ${e.message}", e)
        }
    }

    public companion object {
        /**
         * All Of type.
         *
         * The [AppFunctionAllOfTypeMetadata] is used to define a component only data type object
         * that is a composition of all of the types in the list.
         *
         * The [AppFunctionAllOfTypeMetadata] can contain either:
         * * Top level [AppFunctionObjectTypeMetadata]
         * * An [AppFunctionReferenceTypeMetadata] to an outer object metadata.
         */
        internal const val TYPE: Int = TYPE_ALL_OF

        private const val KEY_ALL_OF = "allOf"
        private const val KEY_QUALIFIED_NAME = "qualifiedName"

        internal fun fromBundle(
            bundle: Bundle,
            isNullable: Boolean,
            description: String,
            depth: Int,
        ): AppFunctionAllOfTypeMetadata =
            AppFunctionAllOfTypeMetadata(
                matchAll =
                    bundle.requireBundleList(KEY_ALL_OF).map {
                        AppFunctionDataTypeMetadata.fromBundle(it, depth + 1)
                    },
                qualifiedName = bundle.getString(KEY_QUALIFIED_NAME),
                isNullable = isNullable,
                description = description,
            )
    }
}

/**
 * Defines the schema for a data type that can be one of several possible types, representing a form
 * of polymorphism or a sealed hierarchy.
 *
 * An object of this type must match exactly one of the schemas defined in the [matchOneOf] list.
 * This is useful for modeling sealed classes or interfaces where an object can be one of a limited
 * set of subtypes. This implies a hierarchical relationship between the parent type (represented by
 * this `OneOfTypeMetadata`) and its possible concrete implementations in [matchOneOf].
 *
 * For example, consider the following sealed interface and its implementations:
 * ```
 * package com.example.myapp
 *
 * sealed interface Animal {
 *     val name: String
 * }
 *
 * data class Dog(
 *     override val name: String,
 *     val breed: String,
 * ) : Animal
 *
 * data class Cat(
 *     override val name: String,
 *     val livesLeft: Int,
 * ) : Animal
 *
 * ```
 *
 * The following [AppFunctionOneOfTypeMetadata] can be used to define a data type that matches any
 * object implementing the `Animal` interface (i.e., either a `Dog` or a `Cat`).
 *
 * ```
 * val animalType = AppFunctionOneOfTypeMetadata(
 *     qualifiedName = "com.example.myapp.Animal",
 *     matchOneOf = listOf(
 *         AppFunctionObjectTypeMetadata(
 *             qualifiedName = "com.example.myapp.Dog",
 *             properties = mapOf(
 *                 "name" to AppFunctionStringTypeMetadata(...),
 *                 "breed" to AppFunctionStringTypeMetadata(...),
 *             ),
 *             required = listOf("name", "breed"),
 *             isNullable = false,
 *         ),
 *         AppFunctionObjectTypeMetadata(
 *             qualifiedName = "com.example.myapp.Cat",
 *             properties = mapOf(
 *                 "name" to AppFunctionStringTypeMetadata(...),
 *                 "livesLeft" to AppFunctionIntTypeMetadata(...),
 *             ),
 *             required = listOf("name", "livesLeft"),
 *             isNullable = false,
 *         ),
 *     ),
 *     isNullable = false,
 * )
 * ```
 *
 * This data type can be used to define the schema of an input or output type.
 */
public class AppFunctionOneOfTypeMetadata
@JvmOverloads
constructor(
    /** The list of possible data types that an object can match. */
    public val matchOneOf: List<AppFunctionDataTypeMetadata>,
    /**
     * The parent object's qualified name if available. For example, "com.example.myapp.Animal".
     *
     * Use this value to set [androidx.appfunctions.AppFunctionData.qualifiedName] when trying to
     * build the parameters for [androidx.appfunctions.ExecuteAppFunctionRequest].
     */
    public val qualifiedName: String,
    /** Whether this data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {
    override fun writeToBundle(bundle: Bundle, depth: Int) {
        bundle.putInt(KEY_TYPE, TYPE_ONE_OF)
        bundle.putParcelableArrayList(
            KEY_ONE_OF,
            ArrayList(matchOneOf.map { it.toBundle(depth + 1) }),
        )
        bundle.putString(KEY_QUALIFIED_NAME, qualifiedName)
    }

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        if (other !is AppFunctionOneOfTypeMetadata) return false
        if (qualifiedName != other.qualifiedName) return false
        return matchOneOf == other.matchOneOf
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + matchOneOf.hashCode()
        result = 31 * result + qualifiedName.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppFunctionOneOfTypeMetadata(matchOneOf=$matchOneOf, isNullable=$isNullable, description=$description)"
    }

    internal fun getObjectMetadataForOneOfType(
        qualifiedName: String,
        componentsMetadata: AppFunctionComponentsMetadata,
    ): AppFunctionObjectTypeMetadata {
        fun resolveObjectType(
            dataTypeMetadata: AppFunctionDataTypeMetadata
        ): AppFunctionObjectTypeMetadata {
            return when (dataTypeMetadata) {
                is AppFunctionObjectTypeMetadata -> {
                    dataTypeMetadata
                }
                is AppFunctionReferenceTypeMetadata -> {
                    val resolved =
                        componentsMetadata.dataTypes[dataTypeMetadata.referenceDataType]
                            ?: throw IllegalArgumentException(
                                "Unable to resolve ${dataTypeMetadata.referenceDataType}"
                            )
                    resolveObjectType(resolved)
                }
                is AppFunctionAllOfTypeMetadata -> {
                    dataTypeMetadata.getPseudoObjectTypeMetadata(componentsMetadata)
                }
                else ->
                    throw IllegalArgumentException(
                        "Unable to resolve $dataTypeMetadata to object type"
                    )
            }
        }

        val target =
            matchOneOf.singleOrNull {
                when (it) {
                    is AppFunctionObjectTypeMetadata -> it.qualifiedName == qualifiedName
                    is AppFunctionReferenceTypeMetadata -> it.referenceDataType == qualifiedName
                    is AppFunctionAllOfTypeMetadata -> it.qualifiedName == qualifiedName
                    else ->
                        throw IllegalArgumentException("Unexpected data type $it for one of type")
                }
            }
                ?: throw IllegalArgumentException(
                    "$qualifiedName does not match any of the oneOf types"
                )
        return resolveObjectType(target)
    }

    override fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>>,
    ) {
        val otherResolved = tryResolveDataType(other, otherComponent)
        require(otherResolved is AppFunctionOneOfTypeMetadata) {
            "Expect ${AppFunctionOneOfTypeMetadata::class.java} but found ${otherResolved.javaClass}"
        }
        require(this.matchOneOf.size == otherResolved.matchOneOf.size) {
            "OneOf options size mismatch. Expected size: ${this.matchOneOf.size}, actual size: ${otherResolved.matchOneOf.size}"
        }
        for (t1 in this.matchOneOf) {
            val matchesAny =
                otherResolved.matchOneOf.any { t2 ->
                    try {
                        t1.requireSemanticallyEquivalentTo(
                            t2,
                            thisComponent,
                            otherComponent,
                            visitingPairs,
                        )
                        true
                    } catch (_: IllegalArgumentException) {
                        false
                    }
                }
            require(matchesAny) {
                "OneOf match mismatch. Cannot find equivalent type for $t1 in OneOf"
            }
        }
        for (t2 in otherResolved.matchOneOf) {
            val matchesAny =
                this.matchOneOf.any { t1 ->
                    try {
                        t1.requireSemanticallyEquivalentTo(
                            t2,
                            thisComponent,
                            otherComponent,
                            visitingPairs,
                        )
                        true
                    } catch (_: IllegalArgumentException) {
                        false
                    }
                }
            require(matchesAny) {
                "OneOf match mismatch. Cannot find equivalent type for $t2 in expected OneOf"
            }
        }
    }

    public companion object {
        internal const val TYPE: Int = TYPE_ONE_OF

        private const val KEY_ONE_OF = "oneOf"
        private const val KEY_QUALIFIED_NAME = "qualifiedName"

        internal fun fromBundle(
            bundle: Bundle,
            isNullable: Boolean,
            description: String,
            depth: Int,
        ): AppFunctionOneOfTypeMetadata =
            AppFunctionOneOfTypeMetadata(
                matchOneOf =
                    bundle.requireBundleList(KEY_ONE_OF).map {
                        AppFunctionDataTypeMetadata.fromBundle(it, depth + 1)
                    },
                qualifiedName = bundle.requireString(KEY_QUALIFIED_NAME),
                isNullable = isNullable,
                description = description,
            )
    }
}

/** Defines the schema of a object type. */
public class AppFunctionObjectTypeMetadata
@JvmOverloads
constructor(
    /** The schema of the properties of the object. */
    public val properties: Map<String, AppFunctionDataTypeMetadata>,
    /** A list of required properties' names. */
    public val required: List<String>,
    /**
     * The object's qualified name if available.
     *
     * Use this value to set [androidx.appfunctions.AppFunctionData.qualifiedName] when trying to
     * build the parameters for [androidx.appfunctions.ExecuteAppFunctionRequest].
     */
    public val qualifiedName: String?,
    /** Whether this data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {
    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        if (other !is AppFunctionObjectTypeMetadata) return false

        if (properties != other.properties) return false
        if (required != other.required) return false
        if (qualifiedName != other.qualifiedName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + properties.hashCode()
        result = 31 * result + required.hashCode()
        if (qualifiedName != null) {
            result = 31 * result + qualifiedName.hashCode()
        }
        return result
    }

    override fun toString(): String {
        return "AppFunctionObjectTypeMetadata(" +
            "properties=$properties, " +
            "required=$required, " +
            "qualifiedName=$qualifiedName, " +
            "isNullable=$isNullable," +
            "description=$description" +
            ")"
    }

    override fun writeToBundle(bundle: Bundle, depth: Int) {
        bundle.putInt(KEY_TYPE, TYPE_OBJECT)
        bundle.putStringArray(KEY_PROPERTY_NAMES, properties.keys.toTypedArray())
        val propertiesBundle = Bundle()
        for ((name, dataType) in properties) {
            propertiesBundle.putBundle(name, dataType.toBundle(depth + 1))
        }
        bundle.putBundle(KEY_PROPERTIES, propertiesBundle)
        bundle.putStringArray(KEY_REQUIRED, required.toTypedArray())
        if (qualifiedName != null) {
            bundle.putString(KEY_QUALIFIED_NAME, qualifiedName)
        }
    }

    override fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>>,
    ) {
        val otherResolved = tryResolveDataType(other, otherComponent)
        require(otherResolved is AppFunctionObjectTypeMetadata) {
            "Expect ${AppFunctionObjectTypeMetadata::class.java} but found ${otherResolved.javaClass}"
        }
        val thisKeys = this.properties.keys
        val otherKeys = otherResolved.properties.keys
        require(thisKeys == otherKeys) {
            "Property keys mismatch in Object type. Expected keys: $thisKeys, actual keys: $otherKeys"
        }
        val thisRequiredSet = this.required.toSet()
        val otherRequiredSet = otherResolved.required.toSet()
        require(thisRequiredSet == otherRequiredSet) {
            "Required properties mismatch in Object type. Expected: $thisRequiredSet, actual: $otherRequiredSet"
        }

        for ((key, value) in this.properties) {
            val otherValue = otherResolved.properties[key]
            requireNotNull(otherValue) { "Missing property \"$key\" in actual Object type" }
            try {
                value.requireSemanticallyEquivalentTo(
                    otherValue,
                    thisComponent,
                    otherComponent,
                    visitingPairs,
                )
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Type mismatch in Object property \"$key\": ${e.message}",
                    e,
                )
            }
        }
    }

    public companion object {
        /**
         * Object type. The schema of the object is defined in a [AppFunctionObjectTypeMetadata].
         */
        internal const val TYPE: Int = TYPE_OBJECT

        private const val KEY_PROPERTY_NAMES = "propertyNames"
        private const val KEY_PROPERTIES = "properties"
        private const val KEY_REQUIRED = "required"
        private const val KEY_QUALIFIED_NAME = "qualifiedName"

        internal fun fromBundle(
            bundle: Bundle,
            isNullable: Boolean,
            description: String,
            depth: Int,
        ): AppFunctionObjectTypeMetadata {
            val propertyNames = bundle.requireNonNullStringList(KEY_PROPERTY_NAMES)
            val propertiesBundle = bundle.requireBundle(KEY_PROPERTIES)
            val properties = propertyNames.associateWith { name ->
                AppFunctionDataTypeMetadata.fromBundle(
                    propertiesBundle.requireBundle(name),
                    depth + 1,
                )
            }
            val required = bundle.getNonNullStringList(KEY_REQUIRED) ?: emptyList()
            return AppFunctionObjectTypeMetadata(
                properties = properties,
                required = required,
                qualifiedName = bundle.getString(KEY_QUALIFIED_NAME),
                isNullable = isNullable,
                description = description,
            )
        }
    }
}

/**
 * Represents a type that reference a data type that is defined in [AppFunctionComponentsMetadata].
 */
public class AppFunctionReferenceTypeMetadata
@JvmOverloads
constructor(
    /** The string referencing a data type defined in [AppFunctionComponentsMetadata]. */
    public val referenceDataType: String,
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {
    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        if (other !is AppFunctionReferenceTypeMetadata) return false
        return referenceDataType == other.referenceDataType
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + referenceDataType.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppFunctionReferenceTypeMetadata(" +
            "referenceDataType=$referenceDataType, " +
            "isNullable=$isNullable," +
            "description=$description" +
            ")"
    }

    override fun writeToBundle(bundle: Bundle, depth: Int) {
        bundle.putInt(KEY_TYPE, TYPE_REFERENCE)
        bundle.putString(KEY_DATA_TYPE_REFERENCE, referenceDataType)
    }

    override fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>>,
    ) {
        if (other is AppFunctionReferenceTypeMetadata) {
            val pair = this.referenceDataType to other.referenceDataType
            if (visitingPairs.contains(pair)) {
                return
            }
            visitingPairs.add(pair)
        }

        val thisResolved = tryResolveDataType(this, thisComponent)
        val otherResolved = tryResolveDataType(other, otherComponent)
        thisResolved.requireSemanticallyEquivalentTo(
            otherResolved,
            thisComponent,
            otherComponent,
            visitingPairs,
        )
    }

    public companion object {
        /**
         * Object type. The schema of the object is defined in a [AppFunctionObjectTypeMetadata].
         */
        internal const val TYPE: Int = TYPE_REFERENCE

        private const val KEY_DATA_TYPE_REFERENCE = "dataTypeReference"

        internal fun fromBundle(
            bundle: Bundle,
            isNullable: Boolean,
            description: String,
        ): AppFunctionReferenceTypeMetadata =
            AppFunctionReferenceTypeMetadata(
                referenceDataType = bundle.requireString(KEY_DATA_TYPE_REFERENCE),
                isNullable = isNullable,
                description = description,
            )
    }
}

/**
 * Defines the schema of a int data type.
 *
 * Corresponds to a [kotlin.Int].
 */
public class AppFunctionIntTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
    /**
     * Defines the complete set of allowed integer values accepted by this data type.
     *
     * If null, all values are allowed, otherwise it must be non-empty.
     *
     * If any of the values carry special meaning (e.g., `0` means "off", `1` means "on"), such
     * meanings should be documented clearly in the corresponding property, parameter, or function
     * return KDoc.
     */
    @get:Suppress(
        // Null value is used to specify that the value was not set by the caller.
        "NullableCollection"
    )
    public val enumValues: Set<Int>? = null,
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    init {
        require(enumValues == null || enumValues.isNotEmpty()) {
            "If specified, enumValues cannot be empty."
        }
    }

    override fun writeToBundle(bundle: Bundle, depth: Int) {
        bundle.putInt(KEY_TYPE, TYPE_INT)
        if (enumValues != null) {
            bundle.putIntArray(KEY_ENUM_VALUES, enumValues.toIntArray())
        }
    }

    override fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>>,
    ) {
        val otherResolved = tryResolveDataType(other, otherComponent)
        require(otherResolved is AppFunctionIntTypeMetadata) {
            "Expect ${AppFunctionIntTypeMetadata::class.java} but found ${otherResolved.javaClass}"
        }
        require(this.enumValues == otherResolved.enumValues) {
            "Enum values mismatch for Int type. " +
                "Expected: ${this.enumValues}, " +
                "actual: ${otherResolved.enumValues}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionIntTypeMetadata) return false
        return super.equals(other) && enumValues == other.enumValues
    }

    override fun hashCode(): Int = Objects.hash(isNullable, description, enumValues)

    override fun toString(): String {
        return "AppFunctionIntTypeMetadata(isNullable=$isNullable, description=$description, enumValues=$enumValues)"
    }

    internal companion object {
        private const val KEY_ENUM_VALUES = "enumValues"

        fun fromBundle(
            bundle: Bundle,
            isNullable: Boolean,
            description: String,
        ): AppFunctionIntTypeMetadata =
            AppFunctionIntTypeMetadata(
                isNullable = isNullable,
                description = description,
                enumValues = bundle.getIntArray(KEY_ENUM_VALUES)?.toSet(),
            )
    }
}

/**
 * Defines the schema of a long data type.
 *
 * Corresponds to a [kotlin.Long].
 */
public class AppFunctionLongTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    override fun writeToBundle(bundle: Bundle, depth: Int) {
        bundle.putInt(KEY_TYPE, TYPE_LONG)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionLongTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionLongTypeMetadata(isNullable=$isNullable, description=$description)"
    }

    override fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>>,
    ) {
        val otherResolved = tryResolveDataType(other, otherComponent)
        require(otherResolved is AppFunctionLongTypeMetadata) {
            "Expect ${AppFunctionLongTypeMetadata::class.java} but found ${otherResolved.javaClass}"
        }
    }

    internal companion object {
        fun fromBundle(isNullable: Boolean, description: String): AppFunctionLongTypeMetadata =
            AppFunctionLongTypeMetadata(isNullable, description)
    }
}

/**
 * Defines the schema of a float data type.
 *
 * Corresponds to a [kotlin.Float].
 */
public class AppFunctionFloatTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    override fun writeToBundle(bundle: Bundle, depth: Int) {
        bundle.putInt(KEY_TYPE, TYPE_FLOAT)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionFloatTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionFloatTypeMetadata(isNullable=$isNullable, description=$description)"
    }

    override fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>>,
    ) {
        val otherResolved = tryResolveDataType(other, otherComponent)
        require(otherResolved is AppFunctionFloatTypeMetadata) {
            "Expect ${AppFunctionFloatTypeMetadata::class.java} but found ${otherResolved.javaClass}"
        }
    }

    internal companion object {
        fun fromBundle(isNullable: Boolean, description: String): AppFunctionFloatTypeMetadata =
            AppFunctionFloatTypeMetadata(isNullable, description)
    }
}

/**
 * Defines the schema of a unit data type.
 *
 * Corresponds to [kotlin.Unit].
 */
public class AppFunctionUnitTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    // Unit types are inherently not nullable in the same way other types are,
    // but the `isNullable` property is part of the base class.
    // Typically, for Unit, this would be false.
    isNullable: Boolean = false,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    override fun writeToBundle(bundle: Bundle, depth: Int) {
        bundle.putInt(KEY_TYPE, TYPE_UNIT)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionUnitTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionUnitTypeMetadata(isNullable=$isNullable, description=$description)"
    }

    override fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>>,
    ) {
        val otherResolved = tryResolveDataType(other, otherComponent)
        require(otherResolved is AppFunctionUnitTypeMetadata) {
            "Expect ${AppFunctionUnitTypeMetadata::class.java} but found ${otherResolved.javaClass}"
        }
    }

    internal companion object {
        fun fromBundle(isNullable: Boolean, description: String): AppFunctionUnitTypeMetadata =
            AppFunctionUnitTypeMetadata(isNullable, description)
    }
}

/**
 * Defines the schema of a boolean data type.
 *
 * Corresponds to [kotlin.Boolean].
 */
public class AppFunctionBooleanTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    override fun writeToBundle(bundle: Bundle, depth: Int) {
        bundle.putInt(KEY_TYPE, TYPE_BOOLEAN)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionBooleanTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionBooleanTypeMetadata(isNullable=$isNullable, description=$description)"
    }

    override fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>>,
    ) {
        val otherResolved = tryResolveDataType(other, otherComponent)
        require(otherResolved is AppFunctionBooleanTypeMetadata) {
            "Expect ${AppFunctionBooleanTypeMetadata::class.java} but found ${otherResolved.javaClass}"
        }
    }

    internal companion object {
        fun fromBundle(isNullable: Boolean, description: String): AppFunctionBooleanTypeMetadata =
            AppFunctionBooleanTypeMetadata(isNullable, description)
    }
}

/**
 * Defines the schema of a byte array data type.
 *
 * Corresponds to [kotlin.ByteArray].
 */
public class AppFunctionBytesTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    override fun writeToBundle(bundle: Bundle, depth: Int) {
        bundle.putInt(KEY_TYPE, TYPE_BYTES)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionBytesTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionBytesTypeMetadata(isNullable=$isNullable, description=$description)"
    }

    override fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>>,
    ) {
        val otherResolved = tryResolveDataType(other, otherComponent)
        require(otherResolved is AppFunctionBytesTypeMetadata) {
            "Expect ${AppFunctionBytesTypeMetadata::class.java} but found ${otherResolved.javaClass}"
        }
    }

    internal companion object {
        fun fromBundle(isNullable: Boolean, description: String): AppFunctionBytesTypeMetadata =
            AppFunctionBytesTypeMetadata(isNullable, description)
    }
}

/**
 * Defines the schema of a double data type.
 *
 * Corresponds to [kotlin.Double] or a 64-bit floating-point number.
 */
public class AppFunctionDoubleTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    override fun writeToBundle(bundle: Bundle, depth: Int) {
        bundle.putInt(KEY_TYPE, TYPE_DOUBLE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionDoubleTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionDoubleTypeMetadata(isNullable=$isNullable, description=$description)"
    }

    override fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>>,
    ) {
        val otherResolved = tryResolveDataType(other, otherComponent)
        require(otherResolved is AppFunctionDoubleTypeMetadata) {
            "Expect ${AppFunctionDoubleTypeMetadata::class.java} but found ${otherResolved.javaClass}"
        }
    }

    internal companion object {
        fun fromBundle(isNullable: Boolean, description: String): AppFunctionDoubleTypeMetadata =
            AppFunctionDoubleTypeMetadata(isNullable, description)
    }
}

/**
 * Defines the schema of a string data type.
 *
 * Corresponds to [kotlin.String].
 */
public class AppFunctionStringTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
    /**
     * Defines the complete set of allowed string values accepted by this data type.
     *
     * If null, all values are allowed, otherwise it must be non-empty.
     *
     * If any of the values carry special meaning (e.g., `"AUTO"` means automatic mode), such
     * meanings should be documented clearly in the corresponding property, parameter, or function
     * return KDoc.
     */
    @get:Suppress(
        // Null value is used to specify that the value was not set by the caller.
        "NullableCollection"
    )
    public val enumValues: Set<String>? = null,
    /**
     * The patternMatchers that string values must match.
     *
     * If null, no pattern constraint is applied, otherwise it must be non-empty and string values
     * accepted by this data type must match at least one of these [PatternMatcher]s.
     */
    @get:Suppress(
        // Null value is used to specify that the value was not set by the caller.
        "NullableCollection"
    )
    public val patternMatchers: List<PatternMatcher>? = null,
    /**
     * The semantic format description for string values (e.g., `"uri"`).
     *
     * Provides a hint describing the expected format or semantic representation of the string
     * values. A `null` value indicates that no format description is set.
     */
    public val format: String? = null,
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    init {
        require(enumValues == null || enumValues.isNotEmpty()) {
            "If specified, enumValues cannot be empty."
        }
        require(patternMatchers == null || patternMatchers.isNotEmpty()) {
            "If specified, patternMatchers cannot be empty."
        }
    }

    /**
     * [PatternMatcher] does not implement equals, so compare by (path, type) regardless of order.
     */
    private val patternKeys: Set<Pair<String, Int>>
        get() = patternMatchers.orEmpty().mapTo(mutableSetOf()) { it.path to it.type }

    /**
     * A regular expression equivalent to [patternMatchers], or `null` if [patternMatchers] is null
     * or contains a type that this library version cannot convert (for example, a type added in a
     * newer SDK).
     *
     * Combines all [patternMatchers] into a single expression joined as alternatives (logical OR),
     * so it matches a string value if and only if at least one of [patternMatchers] matches it. The
     * expression uses ECMA-262 regular expression syntax and is derived from [patternMatchers]
     * rather than stored.
     *
     * This is intended for describing the constraint to non-Android consumers. Do not use it to
     * validate values: evaluating a regular expression derived from untrusted metadata risks
     * catastrophic backtracking (ReDoS) and regex dialect mismatches. An
     * [androidx.appfunctions.AppFunctionData] built with this metadata already validates string
     * values against [patternMatchers].
     */
    public val regexPattern: String? by lazy { patternMatchers?.toRegexPatternOrNull() }

    /**
     * Legacy name of [regexPattern].
     *
     * Kept temporarily so that existing callers compiled against both older and newer library
     * versions keep working during the migration. Use [regexPattern] instead.
     *
     * This returns the regular expression derived from [patternMatchers] (for example, a
     * [PatternMatcher.PATTERN_PREFIX] matcher for `"content:"` yields `"^content:"`), not the
     * original string a caller passed to older library versions.
     */
    // TODO: b/487929963 - Remove before the first beta release.
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val pattern: String?
        get() = regexPattern

    /**
     * Legacy constructor that accepts a regex [pattern] for source compatibility only.
     *
     * Kept temporarily so that callers compiled against both older and newer library versions keep
     * compiling during the migration. [pattern] is **ignored**: the resulting metadata has no
     * pattern constraint ([patternMatchers] is `null`). Use the primary constructor with
     * [patternMatchers] instead.
     */
    // TODO: b/487929963 - Remove before the first beta release.
    @Suppress("UNUSED_PARAMETER")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        isNullable: Boolean,
        description: String = "",
        enumValues: Set<String>? = null,
        pattern: String,
        format: String? = null,
    ) : this(
        isNullable = isNullable,
        description = description,
        enumValues = enumValues,
        patternMatchers = null,
        format = format,
    )

    override fun writeToBundle(bundle: Bundle, depth: Int) {
        bundle.putInt(KEY_TYPE, TYPE_STRING)
        if (enumValues != null) {
            bundle.putStringArray(KEY_ENUM_VALUES, enumValues.toTypedArray())
        }
        if (format != null) {
            bundle.putString(KEY_FORMAT, format)
        }
        if (patternMatchers != null) {
            bundle.putStringArray(
                KEY_PATTERN_PATHS,
                Array(patternMatchers.size) { patternMatchers[it].path },
            )
            bundle.putIntArray(
                KEY_PATTERN_TYPES,
                IntArray(patternMatchers.size) { patternMatchers[it].type },
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionStringTypeMetadata) return false
        return super.equals(other) &&
            patternKeys == other.patternKeys &&
            format == other.format &&
            enumValues == other.enumValues
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + patternKeys.hashCode()
        result = 31 * result + (format?.hashCode() ?: 0)
        result = 31 * result + (enumValues?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "AppFunctionStringTypeMetadata(isNullable=$isNullable, description=$description, patternMatchers=$patternMatchers, format=$format, enumValues=$enumValues)"
    }

    override fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>>,
    ) {
        val otherResolved = tryResolveDataType(other, otherComponent)
        require(otherResolved is AppFunctionStringTypeMetadata) {
            "Expect ${AppFunctionStringTypeMetadata::class.java} but found ${otherResolved.javaClass}"
        }
        require(this.patternKeys == otherResolved.patternKeys) {
            "Patterns mismatch for String type. Expected: ${this.patternMatchers}, actual: ${otherResolved.patternMatchers}"
        }
        require(this.format == otherResolved.format) {
            "Format mismatch for String type. Expected: ${this.format}, actual: ${otherResolved.format}"
        }
        require(this.enumValues == otherResolved.enumValues) {
            "Enum values mismatch for String type. " +
                "Expected: ${this.enumValues}, " +
                "actual: ${otherResolved.enumValues}"
        }
    }

    public companion object {
        /** The format string representing a URI value. */
        public const val FORMAT_URI: String = "uri"

        private const val KEY_ENUM_VALUES = "enumValues"
        private const val KEY_FORMAT = "format"
        private const val KEY_PATTERN_PATHS = "patternPaths"
        private const val KEY_PATTERN_TYPES = "patternTypes"

        internal fun fromBundle(
            bundle: Bundle,
            isNullable: Boolean,
            description: String,
        ): AppFunctionStringTypeMetadata {
            val patternPaths = bundle.getNonNullStringList(KEY_PATTERN_PATHS)
            val patternTypes = bundle.getIntArray(KEY_PATTERN_TYPES)
            val patternMatchers =
                if (patternPaths == null && patternTypes == null) {
                    null
                } else {
                    require(patternPaths != null && patternTypes != null) {
                        "Pattern paths and types must both be present"
                    }
                    require(patternPaths.size == patternTypes.size) {
                        "Pattern paths and types must have the same length"
                    }
                    List(patternPaths.size) { index ->
                        PatternMatchers.create(patternPaths[index], patternTypes[index])
                    }
                }
            return AppFunctionStringTypeMetadata(
                isNullable = isNullable,
                description = description,
                enumValues = bundle.getNonNullStringList(KEY_ENUM_VALUES)?.toSet(),
                patternMatchers = patternMatchers,
                format = bundle.getString(KEY_FORMAT),
            )
        }
    }
}

/**
 * Defines the schema of a Parcelable data type.
 *
 * Corresponds to [android.os.Parcelable].
 */
public class AppFunctionParcelableTypeMetadata
@JvmOverloads
constructor(
    /** The qualified name of the [android.os.Parcelable] represented by this metadata. */
    public val qualifiedName: String,
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {
    override fun writeToBundle(bundle: Bundle, depth: Int) {
        bundle.putInt(KEY_TYPE, TYPE_PARCELABLE)
        bundle.putString(KEY_QUALIFIED_NAME, qualifiedName)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionParcelableTypeMetadata) return false
        if (!super.equals(other)) return false

        return qualifiedName == other.qualifiedName
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + qualifiedName.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppFunctionParcelableTypeMetadata(qualifiedName=$qualifiedName, isNullable=$isNullable, description=$description)"
    }

    override fun internalRequireSemanticallyEquivalentTo(
        other: AppFunctionDataTypeMetadata,
        thisComponent: AppFunctionComponentsMetadata,
        otherComponent: AppFunctionComponentsMetadata,
        visitingPairs: MutableSet<Pair<String, String>>,
    ) {
        val otherResolved = tryResolveDataType(other, otherComponent)
        require(otherResolved is AppFunctionParcelableTypeMetadata) {
            "Expect ${AppFunctionParcelableTypeMetadata::class.java} but found ${otherResolved.javaClass}"
        }
        require(this.qualifiedName == otherResolved.qualifiedName) {
            "Parcelable qualified name mismatch. " +
                "Expected: ${this.qualifiedName}, " +
                "actual: ${otherResolved.qualifiedName}"
        }
    }

    internal companion object {
        private const val KEY_QUALIFIED_NAME = "qualifiedName"

        fun fromBundle(
            bundle: Bundle,
            isNullable: Boolean,
            description: String,
        ): AppFunctionParcelableTypeMetadata =
            AppFunctionParcelableTypeMetadata(
                qualifiedName = bundle.requireString(KEY_QUALIFIED_NAME),
                isNullable = isNullable,
                description = description,
            )
    }
}

/** Represents the persistent storage format of the schema of a data type and its name. */
@Document
internal data class AppFunctionNamedDataTypeMetadataDocument(
    @Document.Namespace val namespace: String = APP_FUNCTION_NAMESPACE,
    /** The id of the data type. */
    @Document.Id val id: String = APP_FUNCTION_ID_EMPTY,
    /** The name of the data type. */
    @Document.StringProperty val name: String,
    /** The data type metadata. */
    @Document.DocumentProperty val dataTypeMetadata: AppFunctionDataTypeMetadataDocument,
)

/** Represents the persistent storage format of a single [PatternMatcher]. */
@Document
internal data class AppFunctionStringPatternDocument(
    @Document.Namespace val namespace: String = APP_FUNCTION_NAMESPACE,
    /** The id of the pattern. */
    @Document.Id val id: String = APP_FUNCTION_ID_EMPTY,
    /** The pattern string, i.e. [PatternMatcher.getPath]. */
    @Document.StringProperty val value: String,
    /** The pattern type, i.e. [PatternMatcher.getType]. */
    @Document.LongProperty val type: Long,
)

/**
 * Converts indexed patterns to [PatternMatcher]s, or null if there are none.
 *
 * @throws androidx.appfunctions.internal.InvalidPatternMatcherException if [PatternMatcher] rejects
 *   any of the patterns.
 */
private fun List<AppFunctionStringPatternDocument>.toPatternMatchersOrNull():
    List<PatternMatcher>? {
    val matchers = map { PatternMatchers.create(it.value, it.type.toInt()) }
    return matchers.ifEmpty { null }
}

/** Represents the persistent storage format of [AppFunctionDataTypeMetadata]. */
@Document
internal data class AppFunctionDataTypeMetadataDocument(
    @Document.Namespace val namespace: String = APP_FUNCTION_NAMESPACE,
    /** The id of the data type. */
    @Document.Id val id: String = APP_FUNCTION_ID_EMPTY,
    /** The data type. */
    @Document.LongProperty @AppFunctionDataType val type: Int,

    /**
     * If the [type] is [AppFunctionDataTypeMetadata.TYPE_ARRAY], this specifies the array content
     * data type.
     */
    @Document.DocumentProperty val itemType: AppFunctionDataTypeMetadataDocument? = null,
    /**
     * If the [type] is [AppFunctionDataTypeMetadata.TYPE_OBJECT], this specified the object's
     * properties.
     */
    @Document.DocumentProperty
    val properties: List<AppFunctionNamedDataTypeMetadataDocument> = emptyList(),

    /**
     * If the [type] is [AppFunctionDataTypeMetadata.TYPE_ALL_OF], this specified the object's
     * properties.
     */
    @Document.DocumentProperty val allOf: List<AppFunctionDataTypeMetadataDocument> = emptyList(),

    /**
     * If the [type] is [AppFunctionDataTypeMetadata.TYPE_ONE_OF], this specifies the types
     * supported by this one of.
     */
    @Document.DocumentProperty val oneOf: List<AppFunctionDataTypeMetadataDocument> = emptyList(),

    /**
     * If the [type] is [AppFunctionDataTypeMetadata.TYPE_OBJECT], this specified the object's
     * required properties' names.
     */
    @Document.StringProperty val required: List<String> = emptyList(),
    /**
     * If the [type] is [AppFunctionDataTypeMetadata.TYPE_REFERENCE], this specified the reference.
     */
    @Document.StringProperty val dataTypeReference: String? = null,
    /** Whether the type is nullable. */
    @Document.BooleanProperty val isNullable: Boolean = false,
    /**
     * If the [type] is [AppFunctionDataTypeMetadata.TYPE_OBJECT], this specified the object's
     * qualified name if available.
     */
    @Document.StringProperty val objectQualifiedName: String? = null,
    /** A description of the data type and its intended use. */
    @Document.StringProperty val description: String? = null,
    /** Enum values, that this data type is restricted to use. */
    @Document.StringProperty val enumValues: List<String> = emptyList(),
    /** Pattern restrictions for String data type, matched with [PatternMatcher]. */
    @Document.DocumentProperty val patterns: List<AppFunctionStringPatternDocument> = emptyList(),
    /** Format restriction for String data type. */
    @Document.StringProperty val format: String? = null,
) {
    @SuppressLint(
        // When doesn't handle @IntDef correctly.
        "WrongConstant"
    )
    fun toAppFunctionDataTypeMetadata(): AppFunctionDataTypeMetadata =
        when (type) {
            AppFunctionDataTypeMetadata.TYPE_ARRAY -> {
                val itemType = checkNotNull(itemType) { "Item type must be present for array type" }
                AppFunctionArrayTypeMetadata(
                    itemType = itemType.toAppFunctionDataTypeMetadata(),
                    isNullable = isNullable,
                    description = description ?: "",
                )
            }
            AppFunctionDataTypeMetadata.TYPE_OBJECT -> {
                check(properties.isNotEmpty()) {
                    "Properties must be present for object type can't be empty"
                }
                val propertiesMap = properties.associate {
                    it.name to it.dataTypeMetadata.toAppFunctionDataTypeMetadata()
                }
                AppFunctionObjectTypeMetadata(
                    properties = propertiesMap,
                    required = required,
                    qualifiedName = objectQualifiedName,
                    isNullable = isNullable,
                    description = description ?: "",
                )
            }
            AppFunctionDataTypeMetadata.TYPE_REFERENCE ->
                AppFunctionReferenceTypeMetadata(
                    referenceDataType =
                        checkNotNull(dataTypeReference) {
                            "Data type reference must be present for reference type"
                        },
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_ALL_OF ->
                AppFunctionAllOfTypeMetadata(
                    matchAll = allOf.map { it.toAppFunctionDataTypeMetadata() },
                    qualifiedName = objectQualifiedName,
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_ONE_OF ->
                AppFunctionOneOfTypeMetadata(
                    matchOneOf = oneOf.map { it.toAppFunctionDataTypeMetadata() },
                    qualifiedName = checkNotNull(objectQualifiedName),
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_INT ->
                AppFunctionIntTypeMetadata(
                    isNullable = isNullable,
                    description = description ?: "",
                    enumValues = enumValues.map { it.toInt() }.toSet().ifEmpty { null },
                )
            AppFunctionDataTypeMetadata.TYPE_LONG ->
                AppFunctionLongTypeMetadata(
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_FLOAT ->
                AppFunctionFloatTypeMetadata(
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_UNIT ->
                AppFunctionUnitTypeMetadata(
                    isNullable = isNullable, // Or false if you want to enforce non-null for Unit
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_BOOLEAN ->
                AppFunctionBooleanTypeMetadata(
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_BYTES ->
                AppFunctionBytesTypeMetadata(
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_DOUBLE ->
                AppFunctionDoubleTypeMetadata(
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_STRING ->
                AppFunctionStringTypeMetadata(
                    patternMatchers = patterns.toPatternMatchersOrNull(),
                    format = format,
                    enumValues = enumValues.toSet().ifEmpty { null },
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_PARCELABLE ->
                AppFunctionParcelableTypeMetadata(
                    // In library versions alpha01 through alpha07, PendingIntent was represented
                    // by AppFunctionPendingIntentTypeMetadata. To maintain runtime backward
                    // compatibility, we use the same type constant for all parcelables and
                    // default to "android.app.PendingIntent" if the indexed AppFunctionMetadata
                    // does not contain a qualified name.
                    qualifiedName = objectQualifiedName ?: PendingIntent::class.java.name,
                    isNullable = isNullable,
                    description = description ?: "",
                )
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
}
