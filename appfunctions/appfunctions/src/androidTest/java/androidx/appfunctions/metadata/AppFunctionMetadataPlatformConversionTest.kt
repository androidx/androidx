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

package androidx.appfunctions.metadata

import androidx.appfunctions.internal.SchemaAppFunctionInventory
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@SdkSuppress(minSdkVersion = 37)
class AppFunctionMetadataPlatformConversionTest {

    @Test
    fun testFromPlatformAppFunctionMetadata_withDynamicIndexer_convertsCorrectly() {
        val jetpackMetadata =
            AppFunctionMetadata.fromPlatformAppFunctionMetadata(
                PLATFORM_METADATA_WITH_DYNAMIC_INDEXER
            )

        assertThat(jetpackMetadata).isNotNull()
        assertThat(jetpackMetadata!!.id).isEqualTo(TEST_FUNCTION_ID)
        assertThat(jetpackMetadata.packageName).isEqualTo(TEST_PACKAGE_NAME)
        assertThat(jetpackMetadata.isEnabled).isTrue()
        assertThat(jetpackMetadata.description).isEqualTo("Function Description")
        assertThat(jetpackMetadata.deprecation).isNotNull()
        assertThat(jetpackMetadata.deprecation!!.message).isEqualTo("Deprecated")

        assertThat(jetpackMetadata.schema).isNotNull()
        assertThat(jetpackMetadata.schema!!.category).isEqualTo("testCategory")
        assertThat(jetpackMetadata.schema!!.name).isEqualTo("testSchema")
        assertThat(jetpackMetadata.schema!!.version).isEqualTo(2L)

        assertThat(jetpackMetadata.parameters).hasSize(1)
        assertThat(jetpackMetadata.parameters[0].name).isEqualTo("param1")
        assertThat(jetpackMetadata.parameters[0].isRequired).isTrue()
        assertThat(jetpackMetadata.parameters[0].dataType)
            .isInstanceOf(AppFunctionStringTypeMetadata::class.java)

        assertThat(jetpackMetadata.response.valueType)
            .isInstanceOf(AppFunctionStringTypeMetadata::class.java)
        assertThat(jetpackMetadata.response.valueType.isNullable).isTrue()
        assertThat(jetpackMetadata.response.description).isEqualTo("Response Description")

        assertThat(jetpackMetadata.packageMetadata.components.dataTypes).hasSize(2)
        assertThat(jetpackMetadata.packageMetadata.components.dataTypes["Type1"])
            .isInstanceOf(AppFunctionStringTypeMetadata::class.java)
        assertThat(jetpackMetadata.packageMetadata.components.dataTypes["Type1"]!!.isNullable)
            .isFalse()
        assertThat(jetpackMetadata.packageMetadata.components.dataTypes["Type2"])
            .isInstanceOf(AppFunctionIntTypeMetadata::class.java)
        assertThat(jetpackMetadata.packageMetadata.components.dataTypes["Type2"]!!.isNullable)
            .isTrue()
    }

    @Test
    fun testFromPlatformAppFunctionMetadata_withLegacyIndexer_convertsCorrectly() {
        val jetpackMetadata =
            AppFunctionMetadata.fromPlatformAppFunctionMetadata(
                platformMetadata = PLATFORM_METADATA_WITH_LEGACY_INDEXER,
                schemaAppFunctionInventory = FAKE_INVENTORY,
            )

        assertThat(jetpackMetadata).isNotNull()
        assertThat(jetpackMetadata!!.id).isEqualTo(TEST_FUNCTION_ID)
        assertThat(jetpackMetadata.packageName).isEqualTo(TEST_PACKAGE_NAME)
        assertThat(jetpackMetadata.isEnabled).isTrue()
        assertThat(jetpackMetadata.description).isEqualTo("Document Description")

        assertThat(jetpackMetadata.schema).isEqualTo(SCHEMA_METADATA)

        assertThat(jetpackMetadata.parameters).isEqualTo(EXPECTED_PARAMETERS)
        assertThat(jetpackMetadata.response).isEqualTo(EXPECTED_RESPONSE)
        assertThat(jetpackMetadata.packageMetadata.components).isEqualTo(EXPECTED_COMPONENTS)
    }

    private class FakeSchemaAppFunctionInventory(
        override val componentsMetadata: AppFunctionComponentsMetadata,
        override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> =
            emptyMap(),
    ) : SchemaAppFunctionInventory()

    companion object {
        private const val TEST_PACKAGE_NAME = "test.package"
        private const val TEST_FUNCTION_ID = "testFunction"

        private val SCHEMA_METADATA = AppFunctionSchemaMetadata("testCategory", "testSchema", 2L)

        private val EXPECTED_PARAMETERS =
            listOf(
                AppFunctionParameterMetadata(
                    name = "param1",
                    isRequired = true,
                    dataType = AppFunctionStringTypeMetadata(isNullable = false),
                    description = "Inventory Param Description",
                )
            )

        private val EXPECTED_RESPONSE =
            AppFunctionResponseMetadata(
                valueType = AppFunctionStringTypeMetadata(isNullable = true),
                description = "Inventory Response Description",
            )

        private val EXPECTED_COMPONENTS =
            AppFunctionComponentsMetadata(
                dataTypes =
                    mapOf(
                        "InventoryType" to
                            AppFunctionStringTypeMetadata(
                                isNullable = false,
                                description = "Inventory Component Description",
                            ),
                        "InventoryType2" to
                            AppFunctionIntTypeMetadata(
                                isNullable = true,
                                description = "Inventory Component Description 2",
                            ),
                    )
            )

        private val FAKE_INVENTORY =
            FakeSchemaAppFunctionInventory(
                componentsMetadata = EXPECTED_COMPONENTS,
                functionIdToMetadataMap =
                    mapOf(
                        TEST_FUNCTION_ID to
                            CompileTimeAppFunctionMetadata(
                                id = TEST_FUNCTION_ID,
                                isEnabledByDefault = true,
                                schema = SCHEMA_METADATA,
                                parameters = EXPECTED_PARAMETERS,
                                response = EXPECTED_RESPONSE,
                                description = "Inventory Function Description",
                            )
                    ),
            )

        private val PLATFORM_METADATA_WITH_DYNAMIC_INDEXER:
            android.app.appfunctions.AppFunctionMetadata =
            buildPlatformMetadataWithDynamicIndexer()

        private val PLATFORM_METADATA_WITH_LEGACY_INDEXER:
            android.app.appfunctions.AppFunctionMetadata =
            buildPlatformMetadataWithLegacyIndexer()

        private fun buildPlatformMetadataWithDynamicIndexer():
            android.app.appfunctions.AppFunctionMetadata {
            val platformMetadataGd =
                android.app.appsearch.GenericDocument.Builder<
                        android.app.appsearch.GenericDocument.Builder<*>
                    >(
                        "appfunctions",
                        "$TEST_PACKAGE_NAME/$TEST_FUNCTION_ID",
                        "AppFunctionStaticMetadata",
                    )
                    .setPropertyBoolean("enabledByDefault", true)
                    .setPropertyString("schemaCategory", "testCategory")
                    .setPropertyString("schemaName", "testSchema")
                    .setPropertyLong("schemaVersion", 2L)
                    .setPropertyString("description", "Function Description")
                    .setPropertyDocument(
                        "parameters",
                        android.app.appsearch.GenericDocument.Builder<
                                android.app.appsearch.GenericDocument.Builder<*>
                            >(
                                "appfunctions",
                                "unused",
                                "AppFunctionParameterMetadata",
                            )
                            .setPropertyString("name", "param1")
                            .setPropertyBoolean("isRequired", true)
                            .setPropertyString("description", "Param Description")
                            .setPropertyDocument(
                                "dataTypeMetadata",
                                android.app.appsearch.GenericDocument.Builder<
                                        android.app.appsearch.GenericDocument.Builder<*>
                                    >(
                                        "appfunctions",
                                        "unused",
                                        "AppFunctionDataTypeMetadata",
                                    )
                                    .setPropertyLong("type", 8L) // TYPE_STRING
                                    .setPropertyBoolean("isNullable", false)
                                    .build(),
                            )
                            .build(),
                    )
                    .setPropertyDocument(
                        "response",
                        android.app.appsearch.GenericDocument.Builder<
                                android.app.appsearch.GenericDocument.Builder<*>
                            >(
                                "appfunctions",
                                "unused",
                                "AppFunctionResponseMetadata",
                            )
                            .setPropertyString("description", "Response Description")
                            .setPropertyDocument(
                                "valueType",
                                android.app.appsearch.GenericDocument.Builder<
                                        android.app.appsearch.GenericDocument.Builder<*>
                                    >(
                                        "appfunctions",
                                        "unused",
                                        "AppFunctionDataTypeMetadata",
                                    )
                                    .setPropertyLong("type", 8L) // TYPE_STRING
                                    .setPropertyBoolean("isNullable", true)
                                    .build(),
                            )
                            .build(),
                    )
                    .setPropertyDocument(
                        "deprecation",
                        android.app.appsearch.GenericDocument.Builder<
                                android.app.appsearch.GenericDocument.Builder<*>
                            >(
                                "appfunctions",
                                "unused",
                                "AppFunctionDeprecationMetadata",
                            )
                            .setPropertyString("message", "Deprecated")
                            .build(),
                    )
                    .build()

            val platformPackageMetadataGd =
                android.app.appsearch.GenericDocument.Builder<
                        android.app.appsearch.GenericDocument.Builder<*>
                    >(
                        "appfunctions",
                        "$TEST_PACKAGE_NAME/package",
                        "AppFunctionPackageMetadata",
                    )
                    .setPropertyDocument(
                        "topLevelMetadataDocuments",
                        android.app.appsearch.GenericDocument.Builder<
                                android.app.appsearch.GenericDocument.Builder<*>
                            >(
                                "appfunctions",
                                "component1",
                                AppFunctionComponentsMetadataDocument.SCHEMA_TYPE,
                            )
                            .setPropertyDocument(
                                "dataTypes",
                                android.app.appsearch.GenericDocument.Builder<
                                        android.app.appsearch.GenericDocument.Builder<*>
                                    >(
                                        "appfunctions",
                                        "unused",
                                        "AppFunctionNamedDataTypeMetadata",
                                    )
                                    .setPropertyString("name", "Type1")
                                    .setPropertyDocument(
                                        "dataTypeMetadata",
                                        android.app.appsearch.GenericDocument.Builder<
                                                android.app.appsearch.GenericDocument.Builder<*>
                                            >(
                                                "appfunctions",
                                                "unused",
                                                "AppFunctionDataTypeMetadata",
                                            )
                                            .setPropertyLong("type", 8L) // TYPE_STRING
                                            .setPropertyBoolean("isNullable", false)
                                            .build(),
                                    )
                                    .build(),
                            )
                            .build(),
                        android.app.appsearch.GenericDocument.Builder<
                                android.app.appsearch.GenericDocument.Builder<*>
                            >(
                                "appfunctions",
                                "component2",
                                AppFunctionComponentsMetadataDocument.SCHEMA_TYPE,
                            )
                            .setPropertyDocument(
                                "dataTypes",
                                android.app.appsearch.GenericDocument.Builder<
                                        android.app.appsearch.GenericDocument.Builder<*>
                                    >(
                                        "appfunctions",
                                        "unused",
                                        "AppFunctionNamedDataTypeMetadata",
                                    )
                                    .setPropertyString("name", "Type2")
                                    .setPropertyDocument(
                                        "dataTypeMetadata",
                                        android.app.appsearch.GenericDocument.Builder<
                                                android.app.appsearch.GenericDocument.Builder<*>
                                            >(
                                                "appfunctions",
                                                "unused",
                                                "AppFunctionDataTypeMetadata",
                                            )
                                            .setPropertyLong("type", 7L) // TYPE_INT
                                            .setPropertyBoolean("isNullable", true)
                                            .build(),
                                    )
                                    .build(),
                            )
                            .build(),
                    )
                    .build()

            val platformPackageMetadata =
                createPlatformPackageMetadata(TEST_PACKAGE_NAME, platformPackageMetadataGd)
            val platformName =
                android.app.appfunctions.AppFunctionName(TEST_PACKAGE_NAME, TEST_FUNCTION_ID)
            val platformSchema =
                android.app.appfunctions.AppFunctionSchemaMetadata("testCategory", "testSchema", 2L)

            return createPlatformMetadata(
                platformName,
                platformSchema,
                platformPackageMetadata,
                platformMetadataGd,
            )
        }

        private fun buildPlatformMetadataWithLegacyIndexer():
            android.app.appfunctions.AppFunctionMetadata {
            val platformMetadataGdForInventory =
                android.app.appsearch.GenericDocument.Builder<
                        android.app.appsearch.GenericDocument.Builder<*>
                    >(
                        "appfunctions",
                        "$TEST_PACKAGE_NAME/$TEST_FUNCTION_ID",
                        "AppFunctionStaticMetadata",
                    )
                    .setPropertyBoolean("enabledByDefault", true)
                    .setPropertyString("schemaCategory", "testCategory")
                    .setPropertyString("schemaName", "testSchema")
                    .setPropertyLong("schemaVersion", 2L)
                    .setPropertyString("description", "Document Description")
                    .build()

            val emptyPlatformPackageMetadataGd =
                android.app.appsearch.GenericDocument.Builder<
                        android.app.appsearch.GenericDocument.Builder<*>
                    >(
                        "appfunctions",
                        "$TEST_PACKAGE_NAME/package",
                        "AppFunctionPackageMetadata",
                    )
                    .build()

            val emptyPlatformPackageMetadata =
                createPlatformPackageMetadata(TEST_PACKAGE_NAME, emptyPlatformPackageMetadataGd)
            val platformName =
                android.app.appfunctions.AppFunctionName(TEST_PACKAGE_NAME, TEST_FUNCTION_ID)
            val platformSchema =
                android.app.appfunctions.AppFunctionSchemaMetadata("testCategory", "testSchema", 2L)

            return createPlatformMetadata(
                platformName,
                platformSchema,
                emptyPlatformPackageMetadata,
                platformMetadataGdForInventory,
            )
        }

        private fun createPlatformPackageMetadata(
            packageName: String,
            metadataDocument: android.app.appsearch.GenericDocument,
        ): android.app.appfunctions.AppFunctionPackageMetadata {
            val constructor =
                android.app.appfunctions.AppFunctionPackageMetadata::class
                    .java
                    .getDeclaredConstructor(
                        String::class.java,
                        android.app.appsearch.GenericDocument::class.java,
                    )
            constructor.isAccessible = true
            return constructor.newInstance(packageName, metadataDocument)
        }

        private fun createPlatformMetadata(
            name: android.app.appfunctions.AppFunctionName,
            schemaMetadata: android.app.appfunctions.AppFunctionSchemaMetadata?,
            packageMetadata: android.app.appfunctions.AppFunctionPackageMetadata,
            metadataDocument: android.app.appsearch.GenericDocument,
        ): android.app.appfunctions.AppFunctionMetadata {
            val constructor =
                android.app.appfunctions.AppFunctionMetadata::class
                    .java
                    .getDeclaredConstructor(
                        android.app.appfunctions.AppFunctionName::class.java,
                        android.app.appfunctions.AppFunctionSchemaMetadata::class.java,
                        android.app.appfunctions.AppFunctionPackageMetadata::class.java,
                        android.app.appsearch.GenericDocument::class.java,
                    )
            constructor.isAccessible = true
            return constructor.newInstance(name, schemaMetadata, packageMetadata, metadataDocument)
        }
    }
}
