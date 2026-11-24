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

package androidx.appfunctions.internal

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.internal.AppFunctionLibraryConfiguration.isLoggingEnabled
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appsearch.app.GenericDocument

/** Utility methods for working with [GenericDocument]s. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object GenericDocumentUtils {

    /**
     * Translates a platform [android.app.appsearch.GenericDocument] into a Jetpack
     * [androidx.appsearch.app.GenericDocument].
     *
     * @param platformDocument The platform generic document to translate.
     * @return The corresponding Jetpack generic document.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @Suppress("deprecation")
    @NonNull
    public fun fromPlatformToJetpackGenericDocument(
        platformDocument: android.app.appsearch.GenericDocument
    ): GenericDocument {
        val jetpackBuilder =
            GenericDocument.Builder<GenericDocument.Builder<*>>(
                platformDocument.namespace,
                platformDocument.id,
                platformDocument.schemaType,
            )
        jetpackBuilder
            .setScore(platformDocument.score)
            .setTtlMillis(platformDocument.ttlMillis)
            .setCreationTimestampMillis(platformDocument.creationTimestampMillis)
        for (propertyName in platformDocument.propertyNames) {
            when (val property = platformDocument.getProperty(propertyName)) {
                is Array<*> -> {
                    if (property.isArrayOf<String>()) {
                        @Suppress("UNCHECKED_CAST")
                        jetpackBuilder.setPropertyString(propertyName, *(property as Array<String>))
                    } else if (property.isArrayOf<ByteArray>()) {
                        @Suppress("UNCHECKED_CAST")
                        jetpackBuilder.setPropertyBytes(
                            propertyName,
                            *(property as Array<ByteArray>),
                        )
                    } else if (property.isArrayOf<android.app.appsearch.GenericDocument>()) {
                        @Suppress("UNCHECKED_CAST")
                        val documentValues =
                            property as Array<android.app.appsearch.GenericDocument>
                        val jetpackSubDocuments =
                            Array(documentValues.size) { j ->
                                fromPlatformToJetpackGenericDocument(documentValues[j])
                            }
                        jetpackBuilder.setPropertyDocument(propertyName, *jetpackSubDocuments)
                    } else {
                        if (isLoggingEnabled) {
                            Log.w(
                                APP_FUNCTIONS_TAG,
                                "Property \"$propertyName\" has unsupported array element type ${property.javaClass.name}",
                            )
                        }
                    }
                }

                is LongArray -> {
                    jetpackBuilder.setPropertyLong(propertyName, *property)
                }

                is DoubleArray -> {
                    jetpackBuilder.setPropertyDouble(propertyName, *property)
                }

                is BooleanArray -> {
                    jetpackBuilder.setPropertyBoolean(propertyName, *property)
                }

                is ByteArray -> {
                    jetpackBuilder.setPropertyBytes(propertyName, property)
                }

                else -> {
                    if (isLoggingEnabled) {
                        Log.w(
                            APP_FUNCTIONS_TAG,
                            "Property \"$propertyName\" has unsupported value type ${property?.javaClass?.name}",
                        )
                    }
                }
            }
        }
        return jetpackBuilder.build()
    }

    /**
     * Safely casts a [GenericDocument] to an instance of the specified document class [T].
     *
     * If casting fails, it logs a warning message and returns null.
     *
     * @param genericDocument The generic document to cast.
     * @return The casted document class instance of type [T], or null if the conversion fails.
     */
    public inline fun <reified T : Any> safeCastToDocumentClass(
        genericDocument: GenericDocument
    ): T? =
        try {
            genericDocument.toDocumentClass(T::class.java)
        } catch (ex: Exception) {
            if (isLoggingEnabled) {
                Log.w(
                    APP_FUNCTIONS_TAG,
                    "Failed to convert search result ${genericDocument.id} " +
                        "to ${T::class.simpleName}",
                    ex,
                )
            }
            null
        }

    /**
     * Converts a platform [android.app.appsearch.GenericDocument] to a [Bundle].
     *
     * Preserves namespace, id, schema type, and properties (`String[]`, `long[]`, `double[]`,
     * `boolean[]`, `byte[][]`, and nested [android.app.appsearch.GenericDocument]s).
     *
     * @throws IllegalArgumentException if [platformDocument] contains an unsupported property type
     *   or is nested deeper than [MAX_BUNDLE_DEPTH].
     */
    @RequiresApi(Build.VERSION_CODES.S)
    internal fun toBundle(platformDocument: android.app.appsearch.GenericDocument): Bundle =
        toBundle(platformDocument, depth = 0)

    @RequiresApi(Build.VERSION_CODES.S)
    private fun toBundle(
        platformDocument: android.app.appsearch.GenericDocument,
        depth: Int,
    ): Bundle {
        require(depth < MAX_BUNDLE_DEPTH) {
            "GenericDocument is nested deeper than $MAX_BUNDLE_DEPTH"
        }
        val properties = Bundle()
        for (propertyName in platformDocument.propertyNames) {
            @Suppress("DEPRECATION") val property = platformDocument.getProperty(propertyName)
            properties.putBundle(propertyName, propertyToBundle(property, depth))
        }
        return Bundle().apply {
            putString(KEY_NAMESPACE, platformDocument.namespace)
            putString(KEY_ID, platformDocument.id)
            putString(KEY_SCHEMA_TYPE, platformDocument.schemaType)
            putBundle(KEY_PROPERTIES, properties)
        }
    }

    /**
     * Converts a [Bundle] created by [toBundle] back to a platform
     * [android.app.appsearch.GenericDocument].
     *
     * @throws IllegalArgumentException if [bundle] is malformed or nested deeper than
     *   [MAX_BUNDLE_DEPTH].
     */
    @RequiresApi(Build.VERSION_CODES.S)
    internal fun fromBundle(bundle: Bundle): android.app.appsearch.GenericDocument =
        try {
            fromBundle(bundle, depth = 0)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Malformed GenericDocument bundle", e)
        }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun fromBundle(bundle: Bundle, depth: Int): android.app.appsearch.GenericDocument {
        require(depth < MAX_BUNDLE_DEPTH) {
            "GenericDocument is nested deeper than $MAX_BUNDLE_DEPTH"
        }
        val builder =
            android.app.appsearch.GenericDocument.Builder<
                android.app.appsearch.GenericDocument.Builder<*>
            >(
                bundle.requireString(KEY_NAMESPACE),
                bundle.requireString(KEY_ID),
                bundle.requireString(KEY_SCHEMA_TYPE),
            )
        val properties =
            if (bundle.containsKey(KEY_PROPERTIES)) {
                bundle.requireBundle(KEY_PROPERTIES)
            } else {
                Bundle.EMPTY
            }
        for (propertyName in properties.keySet()) {
            require(propertyName != null) { "Malformed null property name" }
            val property = properties.requireBundle(propertyName)
            builder.setPropertyFromBundle(propertyName, property, depth)
        }
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun propertyToBundle(property: Any?, depth: Int): Bundle =
        Bundle().apply {
            when {
                property is Array<*> && property.isArrayOf<String>() -> {
                    putInt(KEY_TYPE, TYPE_STRING)
                    @Suppress("UNCHECKED_CAST") putStringArray(KEY_VALUE, property as Array<String>)
                }
                property is LongArray -> {
                    putInt(KEY_TYPE, TYPE_LONG)
                    putLongArray(KEY_VALUE, property)
                }
                property is DoubleArray -> {
                    putInt(KEY_TYPE, TYPE_DOUBLE)
                    putDoubleArray(KEY_VALUE, property)
                }
                property is BooleanArray -> {
                    putInt(KEY_TYPE, TYPE_BOOLEAN)
                    putBooleanArray(KEY_VALUE, property)
                }
                property is Array<*> && property.isArrayOf<ByteArray>() -> {
                    putInt(KEY_TYPE, TYPE_BYTES)
                    @Suppress("UNCHECKED_CAST") val bytesArray = property as Array<ByteArray>
                    putParcelableArrayList(
                        KEY_VALUE,
                        bytesArray.mapTo(ArrayList(bytesArray.size)) { bytes ->
                            Bundle().apply { putByteArray(KEY_VALUE, bytes) }
                        },
                    )
                }
                property is Array<*> &&
                    property.isArrayOf<android.app.appsearch.GenericDocument>() -> {
                    putInt(KEY_TYPE, TYPE_DOCUMENT)
                    @Suppress("UNCHECKED_CAST")
                    val documents = property as Array<android.app.appsearch.GenericDocument>
                    putParcelableArrayList(
                        KEY_VALUE,
                        documents.mapTo(ArrayList(documents.size)) { doc ->
                            toBundle(doc, depth + 1)
                        },
                    )
                }
                else ->
                    throw IllegalArgumentException(
                        "Unsupported property type ${property?.javaClass?.name}"
                    )
            }
        }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun android.app.appsearch.GenericDocument.Builder<*>.setPropertyFromBundle(
        name: String,
        property: Bundle,
        depth: Int,
    ) {
        when (val type = property.getInt(KEY_TYPE, -1)) {
            TYPE_STRING ->
                setPropertyString(
                    name,
                    *property.requireNonNullStringList(KEY_VALUE).toTypedArray(),
                )
            TYPE_LONG ->
                setPropertyLong(
                    name,
                    *requireNotNull(property.getLongArray(KEY_VALUE)) { "Missing $KEY_VALUE" },
                )
            TYPE_DOUBLE ->
                setPropertyDouble(
                    name,
                    *requireNotNull(property.getDoubleArray(KEY_VALUE)) { "Missing $KEY_VALUE" },
                )
            TYPE_BOOLEAN ->
                setPropertyBoolean(
                    name,
                    *requireNotNull(property.getBooleanArray(KEY_VALUE)) { "Missing $KEY_VALUE" },
                )
            TYPE_BYTES -> {
                val byteBundles = property.requireBundleList(KEY_VALUE)
                val bytes =
                    Array(byteBundles.size) { index ->
                        requireNotNull(byteBundles[index].getByteArray(KEY_VALUE)) {
                            "Missing byte array at index $index"
                        }
                    }
                setPropertyBytes(name, *bytes)
            }
            TYPE_DOCUMENT -> {
                val docBundles = property.requireBundleList(KEY_VALUE)
                val documents =
                    Array(docBundles.size) { index -> fromBundle(docBundles[index], depth + 1) }
                setPropertyDocument(name, *documents)
            }
            else -> throw IllegalArgumentException("Unsupported property type $type")
        }
    }

    private const val MAX_BUNDLE_DEPTH = 64

    private const val KEY_NAMESPACE = "namespace"
    private const val KEY_ID = "id"
    private const val KEY_SCHEMA_TYPE = "schemaType"
    private const val KEY_PROPERTIES = "properties"
    private const val KEY_TYPE = "type"
    private const val KEY_VALUE = "value"

    private const val TYPE_STRING = 1
    private const val TYPE_LONG = 2
    private const val TYPE_DOUBLE = 3
    private const val TYPE_BOOLEAN = 4
    private const val TYPE_BYTES = 5
    private const val TYPE_DOCUMENT = 6
}
