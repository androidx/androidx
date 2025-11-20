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

package androidx.appfunctions.internal

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.Attachment.Companion.ATTACHMENT_OBJECT_TYPE_METADATA
import androidx.appfunctions.Note.Companion.NOTE_OBJECT_TYPE_METADATA
import androidx.appfunctions.OpenableNote
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata

/** Test implementation for [androidx.appfunctions.AppFunctionDataTest] */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class `$AggregatedAppFunctionInventory_Impl` : AggregatedAppFunctionInventory() {
    override val inventories: List<AppFunctionInventory>
        get() = listOf(AppFunctionUriGrantTestInventory())
}

internal class AppFunctionUriGrantTestInventory : AppFunctionInventory {
    override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata>
        get() = mapOf()

    override val componentsMetadata: AppFunctionComponentsMetadata
        get() = TEST_COMPONENT_METADATA

    internal companion object {
        val URI_OBJECT_TYPE_METADATA =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "uri" to AppFunctionStringTypeMetadata(isNullable = false, description = "")
                    ),
                required = listOf("uri"),
                qualifiedName = "androidx.appfunctions.internal.serializableproxies.AppFunctionUri",
                isNullable = false,
                description = "",
            )
        val URI_GRANT_OBJECT_TYPE_METADATA =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "uri" to URI_OBJECT_TYPE_METADATA,
                        "modeFlags" to
                            AppFunctionIntTypeMetadata(isNullable = false, description = ""),
                    ),
                required = listOf("uri", "modeFlags"),
                qualifiedName = "androidx.appfunctions.AppFunctionUriGrant",
                isNullable = false,
                description = "",
            )

        val TEST_APP_FUNCTION_URI_GRANT_HOLDER_OBJECT_METADATA =
            AppFunctionObjectTypeMetadata(
                properties = mapOf("secondGrant" to URI_GRANT_OBJECT_TYPE_METADATA),
                required = listOf("secondGrant"),
                qualifiedName = "nest",
                isNullable = false,
            )

        val TEST_NESTED_APP_FUNCTION_URI_GRANT_OBJECT_METADATA =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "firstGrant" to URI_GRANT_OBJECT_TYPE_METADATA,
                        "nest" to TEST_APP_FUNCTION_URI_GRANT_HOLDER_OBJECT_METADATA,
                        "thirdGrants" to
                            AppFunctionArrayTypeMetadata(
                                itemType = URI_GRANT_OBJECT_TYPE_METADATA,
                                isNullable = false,
                            ),
                    ),
                required = listOf("firstGrant", "nest"),
                qualifiedName = "testObject",
                isNullable = false,
            )

        private val URI_GRANT_COMPONENT_METADATA_MAP =
            mapOf(
                "testObject" to TEST_NESTED_APP_FUNCTION_URI_GRANT_OBJECT_METADATA,
                "nest" to TEST_APP_FUNCTION_URI_GRANT_HOLDER_OBJECT_METADATA,
                "androidx.appfunctions.internal.serializableproxies.AppFunctionUri" to
                    URI_OBJECT_TYPE_METADATA,
                "androidx.appfunctions.AppFunctionUriGrant" to URI_GRANT_OBJECT_TYPE_METADATA,
            )
        private val NOTE_COMPONENT_METADATA_MAP =
            mapOf(
                "androidx.appfunctions.Attachment" to ATTACHMENT_OBJECT_TYPE_METADATA,
                "androidx.appfunctions.Note" to NOTE_OBJECT_TYPE_METADATA,
                "androidx.appfunctions.OpenableNote" to
                    OpenableNote.OPENABLE_NOTE_ALL_OF_TYPE_METADATA,
            )

        val TEST_COMPONENT_METADATA =
            AppFunctionComponentsMetadata(
                dataTypes =
                    URI_GRANT_COMPONENT_METADATA_MAP +
                        NOTE_COMPONENT_METADATA_MAP +
                        OpenableNote.COMPONENT_METADATA.dataTypes
            )
    }
}
