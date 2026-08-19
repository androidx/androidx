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

package androidx.appfunctions.integration.test.agent

import android.os.Build
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.Components.SHARED_COMPONENTS
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.ADDITIONAL_FUNCTION_ID
import androidx.appfunctions.integration.test.agent.AppFunctionMetadataHelper.FunctionIds.COMPONENT_CHANGED_FUNCTION_ID
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBytesTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.metadata.AppFunctionFloatTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionName
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionOneOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionParcelableTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata

internal object AppFunctionMetadataHelper {
    const val TARGET_APP_PACKAGE = "androidx.appfunctions.integration.testapp"

    object FunctionIds {
        const val ADD_FUNCTION_ID = "$TARGET_APP_PACKAGE.BaseTestAppFunctionService#add"
        const val DEPRECATED_FUNCTION_ID =
            "$TARGET_APP_PACKAGE.BaseTestAppFunctionService#deprecatedFunction"
        const val SENTINEL_FUNCTION_ID =
            "$TARGET_APP_PACKAGE.BaseTestAppFunctionService#voidFunction"
        const val DISABLED_BY_DEFAULT_FUNCTION_ID =
            "$TARGET_APP_PACKAGE.BaseTestAppFunctionService#functionDisabledByDefault"
        const val ENABLED_BY_DEFAULT_FUNCTION_ID =
            "$TARGET_APP_PACKAGE.BaseTestAppFunctionService#functionEnabledByDefault"
        const val CREATE_NOTE_FUNCTION_ID =
            "$TARGET_APP_PACKAGE.BaseTestAppFunctionService#createNote"
        const val CREATE_NOTE_DISABLED_BY_DEFAULT_FUNCTION_ID =
            "$TARGET_APP_PACKAGE.BaseTestAppFunctionService#createNoteDisabled"
        const val ADDITIONAL_FUNCTION_ID =
            "$TARGET_APP_PACKAGE.BaseTestAppFunctionService#additionalFunction"
        const val COMPONENT_CHANGED_FUNCTION_ID =
            "$TARGET_APP_PACKAGE.BaseTestAppFunctionService#componentChangeFunction"
    }

    object Components {
        private val MULTI_SERVICE_DATA_TYPES =
            mapOf(
                "androidx.appfunction.integration.test.sharedschema.MultiServiceCreateNoteParams" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "title" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "The multiservice note title.",
                                    ),
                                "content" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionStringTypeMetadata(
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "The multiservice note content.",
                                    ),
                            ),
                        required = listOf("title", "content"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.MultiServiceCreateNoteParams",
                        isNullable = true,
                        description = "The MultiServiceCreateNoteParams.",
                    ),
                "androidx.appfunction.integration.test.sharedschema.MultiServiceFilesData" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "readOnlyUri" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunctions.AppFunctionUriGrant",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "writeOnlyUri" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunctions.AppFunctionUriGrant",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "readWriteUri" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunctions.AppFunctionUriGrant",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "persistReadWriteUri" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunctions.AppFunctionUriGrant",
                                        isNullable = false,
                                        description = "",
                                    ),
                            ),
                        required =
                            listOf(
                                "readOnlyUri",
                                "writeOnlyUri",
                                "readWriteUri",
                                "persistReadWriteUri",
                            ),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.MultiServiceFilesData",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.MultiServiceNote" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "title" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "The multiservice note title.",
                                    ),
                                "content" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionStringTypeMetadata(
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "The multiservice note content.",
                                    ),
                            ),
                        required = listOf("title", "content"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.MultiServiceNote",
                        isNullable = true,
                        description = "The MultiServiceNote.",
                    ),
                "androidx.appfunction.integration.test.sharedschema.MultiServiceProxyTypesWrapper" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "localDateTime" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "java.time.LocalDateTime",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "localDate" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "java.time.LocalDate",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "localTime" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "java.time.LocalTime",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "uri" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "android.net.Uri",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "instant" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "java.time.Instant",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "zoneId" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "java.time.ZoneId",
                                        isNullable = false,
                                        description = "",
                                    ),
                            ),
                        required =
                            listOf(
                                "localDateTime",
                                "localDate",
                                "localTime",
                                "uri",
                                "instant",
                                "zoneId",
                            ),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.MultiServiceProxyTypesWrapper",
                        isNullable = true,
                        description = "",
                    ),
            )

        private val SCHEMA_DATA_TYPES =
            mapOf(
                "androidx.appfunction.integration.test.sharedschema.AppFunctionNote" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "attachments" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionReferenceTypeMetadata(
                                                referenceDataType =
                                                    "androidx.appfunction.integration.test.sharedschema.AppFunctionNote\$Attachment",
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "The attachments of the note.",
                                    ),
                                "content" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = true,
                                        description = "The content of the note.",
                                    ),
                                "id" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "The ID of the note.",
                                    ),
                                "title" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "The title of the note.",
                                    ),
                            ),
                        required = listOf("id", "title"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.AppFunctionNote",
                        isNullable = true,
                        description = "A note entity.",
                    ),
                "androidx.appfunction.integration.test.sharedschema.AppFunctionNote\$Attachment" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "displayName" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "The display name of the attached file.",
                                    ),
                                "mimeType" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = true,
                                        description =
                                            "The MIME type of the attached file. Format defined in RFC 6838.",
                                    ),
                                "uri" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "android.net.Uri",
                                        isNullable = false,
                                        description =
                                            "The URI of the attached file.\n\nWhen providing an [Uri] to another app, that app must be granted URI permission using\n[android.content.Context.grantUriPermission] to the receiving app.\n\nThe providing app should also consider revoking the URI permission by using\n[android.content.Context.revokeUriPermission] after a certain time period.",
                                    ),
                            ),
                        required = listOf("uri", "displayName"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.AppFunctionNote\$Attachment",
                        isNullable = true,
                        description = "An attached file.",
                    ),
                "androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction\$Parameters" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "attachments" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionReferenceTypeMetadata(
                                                referenceDataType =
                                                    "androidx.appfunction.integration.test.sharedschema.AppFunctionNote\$Attachment",
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "The attachments of the note.",
                                    ),
                                "content" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = true,
                                        description = "The text content of the note.",
                                    ),
                                "externalUuid" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = true,
                                        description =
                                            "An optional UUID for this note provided by the caller. If provided, the caller can use\nthis UUID as well as the returned [AppFunctionNote.id] to reference this specific note in\nsubsequent requests, such as a request to update the note that was just created.\n\nTo support [externalUuid], the application should maintain a mapping between the\n[externalUuid] and the internal id of this note. This allows the application to retrieve\nthe correct note when the caller references it using the provided `externalUuid` in\nsubsequent requests.\n\nIf the `externalUuid` is not provided by the caller in the creation request, the\napplication should expect subsequent requests from the caller to reference the note using\nthe application generated [AppFunctionNote.id].",
                                    ),
                                "groupId" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = true,
                                        description =
                                            "The ID of the group the note is in, if any else `null`.\n\n[androidx.appfunctions.AppFunctionElementNotFoundException] should be thrown when a group\nwith the specified groupId doesn't exist.",
                                    ),
                                "title" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "The title of the note.",
                                    ),
                            ),
                        required = listOf("title"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction\$Parameters",
                        isNullable = true,
                        description = "The parameters for creating a note.",
                    ),
                "androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction\$Response" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "createdNote" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunction.integration.test.sharedschema.AppFunctionNote",
                                        isNullable = false,
                                        description = "The created note.",
                                    ),
                                "tag" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = true,
                                        description = "Optional tag.",
                                    ),
                            ),
                        required = listOf("createdNote"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction\$Response",
                        isNullable = true,
                        description = "The response including the created note.",
                    ),
                "android.net.Uri" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "uri" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "",
                                    )
                            ),
                        required = listOf("uri"),
                        qualifiedName = "android.net.Uri",
                        isNullable = true,
                        description = "",
                    ),
            )

        val SCHEMA_DATA_TYPES_LEGACY_INDEXER =
            mapOf(
                "androidx.appfunction.integration.test.sharedschema.AppFunctionNote" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "attachments" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionReferenceTypeMetadata(
                                                referenceDataType =
                                                    "androidx.appfunction.integration.test.sharedschema.AppFunctionNote\$Attachment",
                                                isNullable = false,
                                            ),
                                        isNullable = false,
                                    ),
                                "content" to AppFunctionStringTypeMetadata(isNullable = true),
                                "id" to AppFunctionStringTypeMetadata(isNullable = false),
                                "title" to AppFunctionStringTypeMetadata(isNullable = false),
                            ),
                        required = listOf("id", "title"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.AppFunctionNote",
                        isNullable = true,
                    ),
                "androidx.appfunction.integration.test.sharedschema.AppFunctionNote\$Attachment" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "displayName" to AppFunctionStringTypeMetadata(isNullable = false),
                                "mimeType" to AppFunctionStringTypeMetadata(isNullable = true),
                                "uri" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "android.net.Uri",
                                        isNullable = false,
                                    ),
                            ),
                        required = listOf("uri", "displayName"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.AppFunctionNote\$Attachment",
                        isNullable = true,
                    ),
                "androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction\$Parameters" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "attachments" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionReferenceTypeMetadata(
                                                referenceDataType =
                                                    "androidx.appfunction.integration.test.sharedschema.AppFunctionNote\$Attachment",
                                                isNullable = false,
                                            ),
                                        isNullable = false,
                                    ),
                                "content" to AppFunctionStringTypeMetadata(isNullable = true),
                                "externalUuid" to AppFunctionStringTypeMetadata(isNullable = true),
                                "groupId" to AppFunctionStringTypeMetadata(isNullable = true),
                                "title" to AppFunctionStringTypeMetadata(isNullable = false),
                            ),
                        required = listOf("title"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction\$Parameters",
                        isNullable = true,
                    ),
                "androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction\$Response" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "createdNote" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunction.integration.test.sharedschema.AppFunctionNote",
                                        isNullable = false,
                                    ),
                                "tag" to AppFunctionStringTypeMetadata(isNullable = true),
                            ),
                        required = listOf("createdNote"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction\$Response",
                        isNullable = true,
                    ),
                "android.net.Uri" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf("uri" to AppFunctionStringTypeMetadata(isNullable = false)),
                        required = listOf("uri"),
                        qualifiedName = "android.net.Uri",
                        isNullable = true,
                    ),
            )
        private val COMMON_DATA_TYPES =
            mapOf(
                "android.net.Uri" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "uri" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "",
                                    )
                            ),
                        required = listOf("uri"),
                        qualifiedName = "android.net.Uri",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.ASubclass" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "interfaceProperty" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "",
                                    ),
                                "str" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "",
                                    ),
                            ),
                        required = listOf("interfaceProperty", "str"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.ASubclass",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.Attachment" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "nested" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunction.integration.test.sharedschema.Attachment",
                                        isNullable = true,
                                        description = "",
                                    ),
                                "uri" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "",
                                    ),
                            ),
                        required = listOf("uri"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.Attachment",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.BSubclass" to
                    AppFunctionAllOfTypeMetadata(
                        matchAll =
                            listOf(
                                AppFunctionObjectTypeMetadata(
                                    properties =
                                        mapOf(
                                            "resources" to
                                                AppFunctionArrayTypeMetadata(
                                                    itemType =
                                                        AppFunctionReferenceTypeMetadata(
                                                            referenceDataType =
                                                                "androidx.appfunctions.AppFunctionTextResource",
                                                            isNullable = false,
                                                            description = "",
                                                        ),
                                                    isNullable = false,
                                                    description = "",
                                                )
                                        ),
                                    required = listOf("resources"),
                                    qualifiedName =
                                        "androidx.appfunctions.AppFunctionResourceContainer",
                                    isNullable = true,
                                    description = "",
                                ),
                                AppFunctionObjectTypeMetadata(
                                    properties =
                                        mapOf(
                                            "interfaceProperty" to
                                                AppFunctionStringTypeMetadata(
                                                    isNullable = false,
                                                    description = "",
                                                ),
                                            "integer" to
                                                AppFunctionIntTypeMetadata(isNullable = false),
                                        ),
                                    required = listOf("interfaceProperty", "integer"),
                                    qualifiedName =
                                        "androidx.appfunction.integration.test.sharedschema.BSubclass",
                                    isNullable = true,
                                    description = "",
                                ),
                            ),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.BSubclass",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.ClassWithOptionalValues" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "optionalNonNullBoolean" to
                                    AppFunctionBooleanTypeMetadata(isNullable = false),
                                "optionalNonNullBooleanArray" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionBooleanTypeMetadata(isNullable = false),
                                        isNullable = false,
                                        description = "",
                                    ),
                                "optionalNonNullByteArray" to
                                    AppFunctionBytesTypeMetadata(isNullable = false),
                                "optionalNonNullDouble" to
                                    AppFunctionDoubleTypeMetadata(isNullable = false),
                                "optionalNonNullDoubleArray" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionDoubleTypeMetadata(isNullable = false),
                                        isNullable = false,
                                        description = "",
                                    ),
                                "optionalNonNullFloat" to
                                    AppFunctionFloatTypeMetadata(isNullable = false),
                                "optionalNonNullFloatArray" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType = AppFunctionFloatTypeMetadata(isNullable = false),
                                        isNullable = false,
                                        description = "",
                                    ),
                                "optionalNonNullInt" to
                                    AppFunctionIntTypeMetadata(isNullable = false),
                                "optionalNonNullIntArray" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType = AppFunctionIntTypeMetadata(isNullable = false),
                                        isNullable = false,
                                        description = "",
                                    ),
                                "optionalNonNullListString" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionStringTypeMetadata(
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "",
                                    ),
                                "optionalNonNullLong" to
                                    AppFunctionLongTypeMetadata(isNullable = false),
                                "optionalNonNullLongArray" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType = AppFunctionLongTypeMetadata(isNullable = false),
                                        isNullable = false,
                                        description = "",
                                    ),
                                "optionalNonNullProxySerializableList" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionReferenceTypeMetadata(
                                                referenceDataType = "java.time.LocalDateTime",
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "",
                                    ),
                                "optionalNonNullSerializableList" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionReferenceTypeMetadata(
                                                referenceDataType =
                                                    "androidx.appfunction.integration.test.sharedschema.Owner",
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "",
                                    ),
                                "optionalNullableBoolean" to
                                    AppFunctionBooleanTypeMetadata(isNullable = true),
                                "optionalNullableBooleanArray" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionBooleanTypeMetadata(isNullable = false),
                                        isNullable = true,
                                        description = "",
                                    ),
                                "optionalNullableByteArray" to
                                    AppFunctionBytesTypeMetadata(isNullable = true),
                                "optionalNullableDouble" to
                                    AppFunctionDoubleTypeMetadata(isNullable = true),
                                "optionalNullableDoubleArray" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionDoubleTypeMetadata(isNullable = false),
                                        isNullable = true,
                                        description = "",
                                    ),
                                "optionalNullableFloat" to
                                    AppFunctionFloatTypeMetadata(isNullable = true),
                                "optionalNullableFloatArray" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType = AppFunctionFloatTypeMetadata(isNullable = false),
                                        isNullable = true,
                                        description = "",
                                    ),
                                "optionalNullableInt" to
                                    AppFunctionIntTypeMetadata(isNullable = true),
                                "optionalNullableIntArray" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType = AppFunctionIntTypeMetadata(isNullable = false),
                                        isNullable = true,
                                        description = "",
                                    ),
                                "optionalNullableListString" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionStringTypeMetadata(
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = true,
                                        description = "",
                                    ),
                                "optionalNullableLong" to
                                    AppFunctionLongTypeMetadata(isNullable = true),
                                "optionalNullableLongArray" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType = AppFunctionLongTypeMetadata(isNullable = false),
                                        isNullable = true,
                                        description = "",
                                    ),
                                "optionalNullableProxySerializable" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "java.time.LocalDateTime",
                                        isNullable = true,
                                        description = "",
                                    ),
                                "optionalNullableProxySerializableList" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionReferenceTypeMetadata(
                                                referenceDataType = "java.time.LocalDateTime",
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = true,
                                        description = "",
                                    ),
                                "optionalNullableSerializable" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunction.integration.test.sharedschema.Owner",
                                        isNullable = true,
                                        description = "",
                                    ),
                                "optionalNullableSerializableList" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionReferenceTypeMetadata(
                                                referenceDataType =
                                                    "androidx.appfunction.integration.test.sharedschema.Owner",
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = true,
                                        description = "",
                                    ),
                                "optionalNullableString" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = true,
                                        description = "",
                                    ),
                            ),
                        required = emptyList(),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.ClassWithOptionalValues",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.CreateNoteParams" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "attachments" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionReferenceTypeMetadata(
                                                referenceDataType =
                                                    "androidx.appfunction.integration.test.sharedschema.Attachment",
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "",
                                    ),
                                "content" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionStringTypeMetadata(
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "",
                                    ),
                                "folderId" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = true,
                                        description = "",
                                    ),
                                "owner" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunction.integration.test.sharedschema.Owner",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "title" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "",
                                    ),
                            ),
                        required = listOf("title", "content", "owner", "attachments", "folderId"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.CreateNoteParams",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.FilesData" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "persistReadWriteUri" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunctions.AppFunctionUriGrant",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "readOnlyUri" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunctions.AppFunctionUriGrant",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "readWriteUri" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunctions.AppFunctionUriGrant",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "writeOnlyUri" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunctions.AppFunctionUriGrant",
                                        isNullable = false,
                                        description = "",
                                    ),
                            ),
                        required =
                            listOf(
                                "readOnlyUri",
                                "writeOnlyUri",
                                "readWriteUri",
                                "persistReadWriteUri",
                            ),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.FilesData",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.IntEnumSerializable" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "value" to
                                    AppFunctionIntTypeMetadata(
                                        isNullable = false,
                                        enumValues = setOf(10, 20),
                                    )
                            ),
                        required = listOf("value"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.IntEnumSerializable",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.Note" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "attachments" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionReferenceTypeMetadata(
                                                referenceDataType =
                                                    "androidx.appfunction.integration.test.sharedschema.Attachment",
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "The note's attachments.",
                                    ),
                                "content" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionStringTypeMetadata(
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "The note's content.",
                                    ),
                                "modifiedTime" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "java.time.LocalDateTime",
                                        isNullable = true,
                                        description = "The note's last modified time.",
                                    ),
                                "owner" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunction.integration.test.sharedschema.Owner",
                                        isNullable = false,
                                        description = "The note's [Owner].",
                                    ),
                                "title" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "The note's title.",
                                    ),
                            ),
                        required = listOf("title", "content", "owner", "attachments"),
                        qualifiedName = "androidx.appfunction.integration.test.sharedschema.Note",
                        isNullable = true,
                        description = "Represents a note in the notes app.",
                    ),
                "androidx.appfunction.integration.test.sharedschema.OneOfSealedInterface" to
                    AppFunctionOneOfTypeMetadata(
                        matchOneOf =
                            listOf(
                                AppFunctionReferenceTypeMetadata(
                                    referenceDataType =
                                        "androidx.appfunction.integration.test.sharedschema.ASubclass",
                                    isNullable = true,
                                    description = "",
                                ),
                                AppFunctionReferenceTypeMetadata(
                                    referenceDataType =
                                        "androidx.appfunction.integration.test.sharedschema.BSubclass",
                                    isNullable = true,
                                    description = "",
                                ),
                            ),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.OneOfSealedInterface",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.OneOfSealedNestedSerializable" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "sealedInterface" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunction.integration.test.sharedschema.OneOfSealedInterface",
                                        isNullable = false,
                                        description = "",
                                    )
                            ),
                        required = listOf("sealedInterface"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.OneOfSealedNestedSerializable",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.OpenableNote" to
                    AppFunctionAllOfTypeMetadata(
                        matchAll =
                            listOf(
                                AppFunctionObjectTypeMetadata(
                                    properties =
                                        mapOf(
                                            "intentToOpen" to
                                                AppFunctionParcelableTypeMetadata(
                                                    qualifiedName = "android.app.PendingIntent",
                                                    isNullable = false,
                                                    description = "",
                                                )
                                        ),
                                    required = listOf("intentToOpen"),
                                    qualifiedName =
                                        "androidx.appfunction.integration.test.sharedschema.AppFunctionOpenable",
                                    isNullable = true,
                                    description = "",
                                ),
                                AppFunctionObjectTypeMetadata(
                                    properties =
                                        mapOf(
                                            "attachments" to
                                                AppFunctionArrayTypeMetadata(
                                                    itemType =
                                                        AppFunctionReferenceTypeMetadata(
                                                            referenceDataType =
                                                                "androidx.appfunction.integration.test.sharedschema.Attachment",
                                                            isNullable = false,
                                                            description = "",
                                                        ),
                                                    isNullable = false,
                                                    description = "",
                                                ),
                                            "content" to
                                                AppFunctionArrayTypeMetadata(
                                                    itemType =
                                                        AppFunctionStringTypeMetadata(
                                                            isNullable = false,
                                                            description = "",
                                                        ),
                                                    isNullable = false,
                                                    description = "",
                                                ),
                                            "modifiedTime" to
                                                AppFunctionReferenceTypeMetadata(
                                                    referenceDataType = "java.time.LocalDateTime",
                                                    isNullable = true,
                                                    description = "",
                                                ),
                                            "owner" to
                                                AppFunctionReferenceTypeMetadata(
                                                    referenceDataType =
                                                        "androidx.appfunction.integration.test.sharedschema.Owner",
                                                    isNullable = false,
                                                    description = "",
                                                ),
                                            "title" to
                                                AppFunctionStringTypeMetadata(
                                                    isNullable = false,
                                                    description = "",
                                                ),
                                        ),
                                    required = listOf("title", "content", "owner", "attachments"),
                                    qualifiedName =
                                        "androidx.appfunction.integration.test.sharedschema.OpenableNote",
                                    isNullable = true,
                                    description = "",
                                ),
                            ),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.OpenableNote",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.Owner" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "name" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "",
                                    )
                            ),
                        required = listOf("name"),
                        qualifiedName = "androidx.appfunction.integration.test.sharedschema.Owner",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.ProxyTypesWrapper" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "instant" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "java.time.Instant",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "localDate" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "java.time.LocalDate",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "localDateTime" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "java.time.LocalDateTime",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "localTime" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "java.time.LocalTime",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "uri" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "android.net.Uri",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "zoneId" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "java.time.ZoneId",
                                        isNullable = false,
                                        description = "",
                                    ),
                            ),
                        required =
                            listOf(
                                "localDateTime",
                                "localDate",
                                "localTime",
                                "uri",
                                "instant",
                                "zoneId",
                            ),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.ProxyTypesWrapper",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.ResourceFunctionResponse" to
                    AppFunctionAllOfTypeMetadata(
                        matchAll =
                            listOf(
                                AppFunctionObjectTypeMetadata(
                                    properties =
                                        mapOf(
                                            "resources" to
                                                AppFunctionArrayTypeMetadata(
                                                    itemType =
                                                        AppFunctionReferenceTypeMetadata(
                                                            referenceDataType =
                                                                "androidx.appfunctions.AppFunctionTextResource",
                                                            isNullable = false,
                                                            description = "",
                                                        ),
                                                    isNullable = false,
                                                    description = "",
                                                )
                                        ),
                                    required = listOf("resources"),
                                    qualifiedName =
                                        "androidx.appfunctions.AppFunctionResourceContainer",
                                    isNullable = true,
                                    description = "",
                                ),
                                AppFunctionObjectTypeMetadata(
                                    properties =
                                        mapOf(
                                            "stringValue" to
                                                AppFunctionStringTypeMetadata(
                                                    isNullable = false,
                                                    description = "",
                                                )
                                        ),
                                    required = listOf("stringValue"),
                                    qualifiedName =
                                        "androidx.appfunction.integration.test.sharedschema.ResourceFunctionResponse",
                                    isNullable = true,
                                    description = "",
                                ),
                            ),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.ResourceFunctionResponse",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunction.integration.test.sharedschema.SetField<java.time.LocalDateTime>" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "value" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "java.time.LocalDateTime",
                                        isNullable = false,
                                        description = "Value property of SetField.",
                                    )
                            ),
                        required = listOf("value"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.SetField<java.time.LocalDateTime>",
                        isNullable = true,
                        description = "Example parameterized AppFunctionSerializable.",
                    ),
                "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.String>" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "value" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "Value property of SetField.",
                                    )
                            ),
                        required = listOf("value"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.String>",
                        isNullable = true,
                        description = "Example parameterized AppFunctionSerializable.",
                    ),
                "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.String?>" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "value" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = true,
                                        description = "Value property of SetField.",
                                    )
                            ),
                        required = listOf("value"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.String?>",
                        isNullable = true,
                        description = "Example parameterized AppFunctionSerializable.",
                    ),
                "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.collections.List<androidx.appfunction.integration.test.sharedschema.Attachment>>" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "value" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionReferenceTypeMetadata(
                                                referenceDataType =
                                                    "androidx.appfunction.integration.test.sharedschema.Attachment",
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "Value property of SetField.",
                                    )
                            ),
                        required = listOf("value"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.collections.List<androidx.appfunction.integration.test.sharedschema.Attachment>>",
                        isNullable = true,
                        description = "Example parameterized AppFunctionSerializable.",
                    ),
                "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.collections.List<kotlin.String>>" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "value" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionStringTypeMetadata(
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "Value property of SetField.",
                                    )
                            ),
                        required = listOf("value"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.collections.List<kotlin.String>>",
                        isNullable = true,
                        description = "Example parameterized AppFunctionSerializable.",
                    ),
                "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.collections.List<kotlin.String>?>" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "value" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionStringTypeMetadata(
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = true,
                                        description = "Value property of SetField.",
                                    )
                            ),
                        required = listOf("value"),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.collections.List<kotlin.String>?>",
                        isNullable = true,
                        description = "Example parameterized AppFunctionSerializable.",
                    ),
                "androidx.appfunction.integration.test.sharedschema.UpdateNoteParams" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "attachments" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.collections.List<androidx.appfunction.integration.test.sharedschema.Attachment>>",
                                        isNullable = true,
                                        description = "",
                                    ),
                                "content" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.collections.List<kotlin.String>>",
                                        isNullable = true,
                                        description = "",
                                    ),
                                "modifiedTime" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunction.integration.test.sharedschema.SetField<java.time.LocalDateTime>",
                                        isNullable = true,
                                        description = "",
                                    ),
                                "nullableContent" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.collections.List<kotlin.String>?>",
                                        isNullable = true,
                                        description = "",
                                    ),
                                "nullableTitle" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.String?>",
                                        isNullable = true,
                                        description = "",
                                    ),
                                "title" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunction.integration.test.sharedschema.SetField<kotlin.String>",
                                        isNullable = true,
                                        description = "",
                                    ),
                            ),
                        required = emptyList(),
                        qualifiedName =
                            "androidx.appfunction.integration.test.sharedschema.UpdateNoteParams",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunctions.AppFunctionTextResource" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "content" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "",
                                    ),
                                "mimeType" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "",
                                    ),
                            ),
                        required = listOf("mimeType", "content"),
                        qualifiedName = "androidx.appfunctions.AppFunctionTextResource",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunctions.AppFunctionUriGrant" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "modeFlags" to AppFunctionIntTypeMetadata(isNullable = false),
                                "uri" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType = "android.net.Uri",
                                        isNullable = false,
                                        description = "",
                                    ),
                            ),
                        required = listOf("uri", "modeFlags"),
                        qualifiedName = "androidx.appfunctions.AppFunctionUriGrant",
                        isNullable = true,
                        description = "",
                    ),
                "java.time.Instant" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "epochSecond" to AppFunctionLongTypeMetadata(isNullable = false),
                                "nanoAdjustment" to AppFunctionIntTypeMetadata(isNullable = false),
                            ),
                        required = listOf("epochSecond", "nanoAdjustment"),
                        qualifiedName = "java.time.Instant",
                        isNullable = true,
                        description = "",
                    ),
                "java.time.LocalDate" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "dayOfMonth" to AppFunctionIntTypeMetadata(isNullable = false),
                                "month" to AppFunctionIntTypeMetadata(isNullable = false),
                                "year" to AppFunctionIntTypeMetadata(isNullable = false),
                            ),
                        required = listOf("year", "month", "dayOfMonth"),
                        qualifiedName = "java.time.LocalDate",
                        isNullable = true,
                        description = "",
                    ),
                "java.time.LocalDateTime" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "dayOfMonth" to AppFunctionIntTypeMetadata(isNullable = false),
                                "hour" to AppFunctionIntTypeMetadata(isNullable = false),
                                "minute" to AppFunctionIntTypeMetadata(isNullable = false),
                                "month" to AppFunctionIntTypeMetadata(isNullable = false),
                                "nanoOfSecond" to AppFunctionIntTypeMetadata(isNullable = false),
                                "second" to AppFunctionIntTypeMetadata(isNullable = false),
                                "year" to AppFunctionIntTypeMetadata(isNullable = false),
                            ),
                        required =
                            listOf(
                                "year",
                                "month",
                                "dayOfMonth",
                                "hour",
                                "minute",
                                "second",
                                "nanoOfSecond",
                            ),
                        qualifiedName = "java.time.LocalDateTime",
                        isNullable = true,
                        description = "",
                    ),
                "java.time.LocalTime" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "hour" to AppFunctionIntTypeMetadata(isNullable = false),
                                "minute" to AppFunctionIntTypeMetadata(isNullable = false),
                                "nanoOfSecond" to AppFunctionIntTypeMetadata(isNullable = false),
                                "second" to AppFunctionIntTypeMetadata(isNullable = false),
                            ),
                        required = listOf("hour", "minute", "second", "nanoOfSecond"),
                        qualifiedName = "java.time.LocalTime",
                        isNullable = true,
                        description = "",
                    ),
                "java.time.ZoneId" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "zoneID" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "",
                                    )
                            ),
                        required = listOf("zoneID"),
                        qualifiedName = "java.time.ZoneId",
                        isNullable = true,
                        description = "",
                    ),
            )

        private val DYNAMIC_SIGNATURE_DATA_TYPES =
            mapOf(
                "androidx.appfunctions.integration.testapp.InnerComplexData" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "id" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "",
                                    ),
                                "scores" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionIntTypeMetadata(
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "",
                                    ),
                                "optionalTag" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = true,
                                        description = "",
                                    ),
                            ),
                        required = listOf("id", "scores"),
                        qualifiedName =
                            "androidx.appfunctions.integration.testapp.InnerComplexData",
                        isNullable = true,
                        description = "",
                    ),
                "androidx.appfunctions.integration.testapp.OuterComplexData" to
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "title" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "",
                                    ),
                                "primaryInner" to
                                    AppFunctionReferenceTypeMetadata(
                                        referenceDataType =
                                            "androidx.appfunctions.integration.testapp.InnerComplexData",
                                        isNullable = false,
                                        description = "",
                                    ),
                                "innerList" to
                                    AppFunctionArrayTypeMetadata(
                                        itemType =
                                            AppFunctionReferenceTypeMetadata(
                                                referenceDataType =
                                                    "androidx.appfunctions.integration.testapp.InnerComplexData",
                                                isNullable = false,
                                                description = "",
                                            ),
                                        isNullable = false,
                                        description = "",
                                    ),
                                "optionalMetadata" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = true,
                                        description = "",
                                    ),
                            ),
                        required = listOf("title", "primaryInner", "innerList"),
                        qualifiedName =
                            "androidx.appfunctions.integration.testapp.OuterComplexData",
                        isNullable = true,
                        description = "",
                    ),
            )

        val SHARED_COMPONENTS =
            AppFunctionComponentsMetadata(
                dataTypes =
                    if (Build.VERSION.SDK_INT >= 37) {
                        COMMON_DATA_TYPES +
                            MULTI_SERVICE_DATA_TYPES +
                            DYNAMIC_SIGNATURE_DATA_TYPES +
                            SCHEMA_DATA_TYPES
                    } else if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
                        // Using BAKLAVAL_1 as proxy to decide whether the dynamic indexer exist
                        // or not since this is a static variable. If we started seeing dynamic
                        // indexer being backported to SDK36. We should update the logic here to
                        // dynamically decide the shared components.
                        COMMON_DATA_TYPES + SCHEMA_DATA_TYPES
                    } else {
                        SCHEMA_DATA_TYPES_LEGACY_INDEXER
                    }
            )
    }

    object FunctionMetadata {
        val ADD =
            AppFunctionMetadata(
                name = AppFunctionName(TARGET_APP_PACKAGE, FunctionIds.ADD_FUNCTION_ID),
                schema = null,
                parameters =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "num1",
                            isRequired = true,
                            dataType = AppFunctionLongTypeMetadata(isNullable = false),
                            description = "The first number.",
                        ),
                        AppFunctionParameterMetadata(
                            name = "num2",
                            isRequired = true,
                            dataType = AppFunctionLongTypeMetadata(isNullable = false),
                            description = "The second number.",
                        ),
                    ),
                response =
                    AppFunctionResponseMetadata(
                        valueType = AppFunctionLongTypeMetadata(isNullable = false),
                        description = "The sum of the two numbers.",
                    ),
                packageMetadata =
                    AppFunctionPackageMetadata(
                        packageName = TARGET_APP_PACKAGE,
                        components = Components.SHARED_COMPONENTS,
                    ),
                description = "Returns the sum of the given two numbers.",
                deprecation = null,
            )

        val CREATE_NOTE =
            AppFunctionMetadata(
                name = AppFunctionName(TARGET_APP_PACKAGE, FunctionIds.CREATE_NOTE_FUNCTION_ID),
                schema =
                    AppFunctionSchemaMetadata(
                        category = "myNotes",
                        name = "createNote",
                        version = 2,
                    ),
                parameters =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "parameters",
                            isRequired = true,
                            dataType =
                                AppFunctionReferenceTypeMetadata(
                                    referenceDataType =
                                        "androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction\$Parameters",
                                    isNullable = false,
                                ),
                            description = "The parameters.",
                        ),
                        AppFunctionParameterMetadata(
                            name = "tag",
                            isRequired = false,
                            dataType = AppFunctionStringTypeMetadata(isNullable = true),
                            description = "The optional tag.",
                        ),
                    ),
                response =
                    AppFunctionResponseMetadata(
                        valueType =
                            AppFunctionReferenceTypeMetadata(
                                referenceDataType =
                                    "androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction\$Response",
                                isNullable = false,
                            ),
                        description =
                            "[androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction.Response] as response.",
                    ),
                packageMetadata =
                    AppFunctionPackageMetadata(
                        packageName = TARGET_APP_PACKAGE,
                        components = Components.SHARED_COMPONENTS,
                    ),
                description = "Create a note.",
                deprecation = null,
            )

        val CREATE_NOTE_LEGACY_INDEXER =
            AppFunctionMetadata(
                name = AppFunctionName(TARGET_APP_PACKAGE, FunctionIds.CREATE_NOTE_FUNCTION_ID),
                schema =
                    AppFunctionSchemaMetadata(
                        category = "myNotes",
                        name = "createNote",
                        version = 2,
                    ),
                parameters =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "parameters",
                            isRequired = true,
                            dataType =
                                AppFunctionReferenceTypeMetadata(
                                    referenceDataType =
                                        "androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction\$Parameters",
                                    isNullable = false,
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "tag",
                            isRequired = false,
                            dataType = AppFunctionStringTypeMetadata(isNullable = true),
                        ),
                    ),
                response =
                    AppFunctionResponseMetadata(
                        valueType =
                            AppFunctionReferenceTypeMetadata(
                                referenceDataType =
                                    "androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction\$Response",
                                isNullable = false,
                            )
                    ),
                packageMetadata =
                    AppFunctionPackageMetadata(
                        packageName = TARGET_APP_PACKAGE,
                        components = Components.SHARED_COMPONENTS,
                    ),
                deprecation = null,
            )
    }

    object FunctionMetadataV2 {

        val ADDITIONAL_FUNCTION =
            AppFunctionMetadata(
                name = AppFunctionName(TARGET_APP_PACKAGE, ADDITIONAL_FUNCTION_ID),
                schema = null,
                parameters = emptyList(),
                response =
                    AppFunctionResponseMetadata(
                        valueType = AppFunctionStringTypeMetadata(isNullable = false)
                    ),
                packageMetadata =
                    AppFunctionPackageMetadata(
                        packageName = TARGET_APP_PACKAGE,
                        components = SHARED_COMPONENTS,
                    ),
            )

        val ADDITIONAL_COMPONENT_FUNCTION_V2 =
            AppFunctionMetadata(
                name = AppFunctionName(TARGET_APP_PACKAGE, COMPONENT_CHANGED_FUNCTION_ID),
                schema = null,
                parameters =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "param",
                            isRequired = true,
                            dataType =
                                AppFunctionReferenceTypeMetadata(
                                    referenceDataType =
                                        "androidx.appfunctions.integration.testapp.ComponentChangeSerializable",
                                    isNullable = false,
                                ),
                        )
                    ),
                response =
                    AppFunctionResponseMetadata(
                        valueType = AppFunctionStringTypeMetadata(isNullable = false)
                    ),
                packageMetadata =
                    AppFunctionPackageMetadata(
                        packageName = TARGET_APP_PACKAGE,
                        components =
                            AppFunctionComponentsMetadata(
                                dataTypes =
                                    SHARED_COMPONENTS.dataTypes +
                                        mapOf(
                                            "androidx.appfunctions.integration.testapp.ComponentChangeSerializable" to
                                                AppFunctionObjectTypeMetadata(
                                                    properties =
                                                        mapOf(
                                                            "prop1" to
                                                                AppFunctionStringTypeMetadata(
                                                                    isNullable = false
                                                                )
                                                        ),
                                                    required = listOf("prop1"),
                                                    qualifiedName =
                                                        "androidx.appfunctions.integration.testapp.ComponentChangeSerializable",
                                                    isNullable = false,
                                                )
                                        )
                            ),
                    ),
            )
    }

    object FunctionMetadataV3 {
        val ADDITIONAL_COMPONENT_FUNCTION_V3 =
            AppFunctionMetadata(
                name = AppFunctionName(TARGET_APP_PACKAGE, COMPONENT_CHANGED_FUNCTION_ID),
                schema = null,
                parameters =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "param",
                            isRequired = true,
                            dataType =
                                AppFunctionReferenceTypeMetadata(
                                    referenceDataType =
                                        "androidx.appfunctions.integration.testapp.ComponentChangeSerializable",
                                    isNullable = false,
                                ),
                        )
                    ),
                response =
                    AppFunctionResponseMetadata(
                        valueType = AppFunctionStringTypeMetadata(isNullable = false)
                    ),
                packageMetadata =
                    AppFunctionPackageMetadata(
                        packageName = TARGET_APP_PACKAGE,
                        components =
                            AppFunctionComponentsMetadata(
                                dataTypes =
                                    SHARED_COMPONENTS.dataTypes +
                                        mapOf(
                                            "androidx.appfunctions.integration.testapp.ComponentChangeSerializable" to
                                                AppFunctionObjectTypeMetadata(
                                                    properties =
                                                        mapOf(
                                                            "prop1" to
                                                                AppFunctionStringTypeMetadata(
                                                                    isNullable = false
                                                                ),
                                                            "prop2" to
                                                                AppFunctionStringTypeMetadata(
                                                                    isNullable = false
                                                                ),
                                                        ),
                                                    required = listOf("prop1", "prop2"),
                                                    qualifiedName =
                                                        "androidx.appfunctions.integration.testapp.ComponentChangeSerializable",
                                                    isNullable = false,
                                                )
                                        )
                            ),
                    ),
            )
    }
}
