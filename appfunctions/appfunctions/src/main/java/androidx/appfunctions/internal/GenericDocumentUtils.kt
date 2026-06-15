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
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
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
                        Log.w(
                            APP_FUNCTIONS_TAG,
                            "Property \"$propertyName\" has unsupported array element type ${property.javaClass.name}",
                        )
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
                    Log.w(
                        APP_FUNCTIONS_TAG,
                        "Property \"$propertyName\" has unsupported value type ${property?.javaClass?.name}",
                    )
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
            Log.w(
                APP_FUNCTIONS_TAG,
                "Failed to convert search result ${genericDocument.id} " +
                    "to ${T::class.simpleName}",
                ex,
            )
            null
        }
}
