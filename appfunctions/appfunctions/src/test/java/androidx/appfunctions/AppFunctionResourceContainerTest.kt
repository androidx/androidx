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

import androidx.appfunctions.AppFunctionResourceContainer.Companion.asAppFunctionResourceContainer
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 33)
class AppFunctionResourceContainerTest {

    @Test
    fun asAppFunctionResourceContainer_success() {
        val textResource1 = AppFunctionTextResource(mimeType = "text/plain", "Hello World!")
        val textResource2 = AppFunctionTextResource(mimeType = "text/id", "123")
        val resources = listOf(textResource1, textResource2)

        val appFunctionData =
            AppFunctionData.Builder(
                    AppFunctionAllOfTypeMetadata(
                        matchAll =
                            listOf(
                                APP_FUNCTION_RESOURCE_CONTAINER_OBJECT_METADATA,
                                AppFunctionObjectTypeMetadata(
                                    properties =
                                        mapOf(
                                            "additionalProperty" to
                                                AppFunctionStringTypeMetadata(isNullable = true)
                                        ),
                                    required = emptyList(),
                                    qualifiedName = "androidx.appfunctions.tests.RandomAllOfClass",
                                    isNullable = true,
                                ),
                            ),
                        qualifiedName = "androidx.appfunctions.tests.RandomAllOfClass",
                        isNullable = false,
                    ),
                    AppFunctionComponentsMetadata(),
                )
                .setAppFunctionDataList(
                    "resources",
                    resources.map {
                        AppFunctionData.serialize(it, AppFunctionTextResource::class.java)
                    },
                )
                .build()

        val resourceContainer = appFunctionData.asAppFunctionResourceContainer()

        assertThat(resourceContainer?.resources).containsExactlyElementsIn(resources)
    }

    @Test
    fun asAppFunctionResourceContainer_noResources_throwsException() {
        assertThat(AppFunctionData.EMPTY.asAppFunctionResourceContainer()).isNull()
    }

    private companion object {
        val RESOURCES_ARRAY_TYPE_METADATA =
            AppFunctionArrayTypeMetadata(
                itemType =
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "mimeType" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "",
                                    ),
                                "content" to
                                    AppFunctionStringTypeMetadata(
                                        isNullable = false,
                                        description = "",
                                    ),
                            ),
                        required = listOf("mimeType", "content"),
                        qualifiedName = "androidx.appfunctions.AppFunctionTextResource",
                        isNullable = false,
                        description = "",
                    ),
                isNullable = false,
            )

        val APP_FUNCTION_RESOURCE_CONTAINER_OBJECT_METADATA =
            AppFunctionObjectTypeMetadata(
                properties = mapOf("resources" to RESOURCES_ARRAY_TYPE_METADATA),
                required = listOf("mimeType", "content"),
                qualifiedName = "androidx.appfunctions.AppFunctionResourceContainer",
                isNullable = false,
                description = "",
            )
    }
}
