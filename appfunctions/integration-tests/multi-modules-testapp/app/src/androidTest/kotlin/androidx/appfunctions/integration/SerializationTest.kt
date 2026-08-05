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

package androidx.appfunctions.integration

import android.content.Context
import androidx.appfunction.integration.test.sharedschema.IntEnumSerializable
import androidx.appfunction.integration.test.sharedschema.UriConstraintSerializable
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionName
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appsearch.app.GlobalSearchSession
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.platformstorage.PlatformStorage
import androidx.concurrent.futures.await
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.fail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class SerializationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var appFunctionManager: AppFunctionManager

    @Before
    fun setup() {
        val nullableAppFunctionManager = AppFunctionManager.getInstance(context)
        assumeNotNull(nullableAppFunctionManager)
        appFunctionManager = checkNotNull(nullableAppFunctionManager)
    }

    @Test
    fun serializeAppFunctionSerializable_failsForInvalidValues() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(context))
        val (dataType, components) =
            requireTargetParameterDataTypeMetadata(
                AppFunctionName(
                    context.packageName,
                    "androidx.appfunctions.integration.testapp.BaseTestAppFunctionService#enumValueFunction",
                ),
                "intEnumSerializable",
            )
        val resolvedObjectType =
            components.dataTypes[(dataType as AppFunctionReferenceTypeMetadata).referenceDataType]
                as AppFunctionObjectTypeMetadata

        assertFailsWith<IllegalArgumentException> {
            AppFunctionData.serialize(
                resolvedObjectType,
                components,
                IntEnumSerializable(value = -1),
                IntEnumSerializable::class.java,
            )
        }
    }

    @Test
    fun serializeAppFunctionSerializable_enumMatch_success() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(context))
        val (dataType, components) =
            requireTargetParameterDataTypeMetadata(
                AppFunctionName(
                    context.packageName,
                    "androidx.appfunctions.integration.testapp.BaseTestAppFunctionService#enumValueFunction",
                ),
                "intEnumSerializable",
            )
        val resolvedObjectType =
            components.dataTypes[(dataType as AppFunctionReferenceTypeMetadata).referenceDataType]
                as AppFunctionObjectTypeMetadata

        val afd =
            AppFunctionData.serialize(
                resolvedObjectType,
                components,
                IntEnumSerializable(value = 10),
                IntEnumSerializable::class.java,
            )

        assertThat(afd.getInt("value")).isEqualTo(10)
    }

    @Test
    @Ignore(
        "b/446606781: Re-enable once serialization no longer relies on aggregation mode to validate"
    )
    fun deserializeAppFunctionSerializable_failsForInvalidValues() {
        assertFailsWith<IllegalArgumentException> {
            AppFunctionData.Builder(
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "value",
                            isRequired = false,
                            dataType = AppFunctionIntTypeMetadata(isNullable = true),
                        )
                    ),
                    AppFunctionComponentsMetadata(),
                )
                .setInt("value", -1)
                .build()
                .deserialize(IntEnumSerializable::class.java)
        }
    }

    @Test
    fun deserializeAppFunctionSerializable_success() {
        val intEnumSerializable =
            AppFunctionData.Builder(
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "value",
                            isRequired = false,
                            dataType = AppFunctionIntTypeMetadata(isNullable = true),
                        )
                    ),
                    AppFunctionComponentsMetadata(),
                )
                .setInt("value", 10)
                .build()
                .deserialize(IntEnumSerializable::class.java)

        assertThat(intEnumSerializable.value).isEqualTo(10)
    }

    @Test
    fun serializeAppFunctionSerializable_uriConstraint_success() {
        val uriConstraintSerializable =
            UriConstraintSerializable(
                uri = android.net.Uri.parse("content://media/external/images/media/1"),
                numericString = "12345",
            )

        val afd =
            AppFunctionData.serialize(
                uriConstraintSerializable,
                UriConstraintSerializable::class.java,
            )

        assertThat(afd.getAppFunctionData("uri")?.getString("uri"))
            .isEqualTo("content://media/external/images/media/1")
        assertThat(afd.getString("numericString")).isEqualTo("12345")
    }

    @Test
    fun deserializeAppFunctionSerializable_uriConstraint_success() {
        val uri = android.net.Uri.parse("content://media/external/images/media/1")
        val afd =
            AppFunctionData.serialize(
                UriConstraintSerializable(uri = uri, numericString = "12345"),
                UriConstraintSerializable::class.java,
            )

        val deserialized = afd.deserialize(UriConstraintSerializable::class.java)

        assertThat(deserialized.uri).isEqualTo(uri)
        assertThat(deserialized.numericString).isEqualTo("12345")
    }

    private fun requireTargetParameterDataTypeMetadata(
        functionName: AppFunctionName,
        parameterName: String,
    ): Pair<AppFunctionDataTypeMetadata, AppFunctionComponentsMetadata> {
        val targetFunctionMetadata =
            runBlocking {
                appFunctionManager
                    .searchAppFunctions(AppFunctionSearchSpec(functionNames = setOf(functionName)))
                    .singleOrNull()
            } ?: fail("Unable to find $functionName")

        val targetParameterDataTypeMetadata =
            targetFunctionMetadata.parameters
                .singleOrNull { parameterMetadata -> parameterMetadata.name == parameterName }
                ?.dataType ?: fail("Unable to find $parameterName from $targetFunctionMetadata")

        return Pair(targetParameterDataTypeMetadata, targetFunctionMetadata.components)
    }

    private suspend fun isDynamicIndexerAvailable(
        context: Context,
        packageName: String = "androidx.appfunctions.integration.testapp",
    ): Boolean =
        createSearchSession(context).use { session ->
            val searchResults =
                session.search(
                    "",
                    SearchSpec.Builder()
                        .addFilterNamespaces("app_functions")
                        .addFilterPackageNames("android")
                        .addFilterSchemas("AppFunctionStaticMetadata")
                        .build(),
                )
            var nextPage = searchResults.nextPageAsync.await()
            while (nextPage.isNotEmpty()) {
                for (result in nextPage) {
                    val packageNameProperty =
                        result.genericDocument.getPropertyString("packageName")
                    if (packageNameProperty != packageName) {
                        continue
                    }
                    return result.genericDocument.getPropertyDocument("response") != null
                }
                nextPage = searchResults.nextPageAsync.await()
            }
            throw IllegalStateException("No functions found for package $packageName")
        }

    private suspend fun createSearchSession(context: Context): GlobalSearchSession {
        return PlatformStorage.createGlobalSearchSessionAsync(
                PlatformStorage.GlobalSearchContext.Builder(context).build()
            )
            .await()
    }

    private fun doBlocking(block: suspend CoroutineScope.() -> Unit) = runBlocking(block = block)
}
