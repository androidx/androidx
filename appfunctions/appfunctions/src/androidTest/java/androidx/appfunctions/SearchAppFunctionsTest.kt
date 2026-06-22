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

package androidx.appfunctions

import android.Manifest
import android.app.UiAutomation
import android.content.Context
import android.os.Build
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import androidx.appfunctions.metadata.AppFunctionName
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.InputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class SearchAppFunctionsTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val metadataTestHelper: AppFunctionMetadataTestHelper =
        AppFunctionMetadataTestHelper(context)

    private lateinit var appFunctionManager: AppFunctionManager

    private val uiAutomation: UiAutomation =
        InstrumentationRegistry.getInstrumentation().uiAutomation

    private val resetFunctionIds =
        setOf(
            AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
            AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
            AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
            AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA2_PRINT,
        )

    @Before
    fun setup() {
        val appFunctionManagerOrNull = AppFunctionManager.getInstance(context)
        assumeNotNull(appFunctionManagerOrNull)
        appFunctionManager = checkNotNull(appFunctionManagerOrNull)

        uiAutomation.adoptShellPermissionIdentity(
            Manifest.permission.INSTALL_PACKAGES,
            "android.permission.EXECUTE_APP_FUNCTIONS",
        )

        runBlocking {
            metadataTestHelper.awaitAppFunctionIndexed(resetFunctionIds)

            // Reset all test ids
            for (functionIds in resetFunctionIds) {
                appFunctionManager.setAppFunctionEnabled(
                    functionIds,
                    AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }
    }

    @After
    fun tearDown() {
        uiAutomation.dropShellPermissionIdentity()
    }

    @Test
    fun testSearchAppFunctions_byPackageName() =
        runBlocking<Unit> {
            val searchSpec = AppFunctionSearchSpec(packageNames = setOf(context.packageName))
            val result = appFunctionManager.searchAppFunctions(searchSpec)
            assertThat(result).isNotEmpty()
            assertThat(result.all { it.packageName == context.packageName }).isTrue()
        }

    @Test
    fun testSearchAppFunctions_byFunctionName_schemalessFunction() =
        runBlocking<Unit> {
            assumeTrue(metadataTestHelper.isDynamicIndexerAvailable())
            val searchSpec =
                AppFunctionSearchSpec(
                    functionNames =
                        setOf(
                            AppFunctionMetadataTestHelper.FunctionMetadata
                                .NO_SCHEMA_ENABLED_BY_DEFAULT
                                .name
                        )
                )
            val result = appFunctionManager.searchAppFunctions(searchSpec)
            assertThat(result)
                .containsExactly(
                    AppFunctionMetadataTestHelper.FunctionMetadata.NO_SCHEMA_ENABLED_BY_DEFAULT
                )
        }

    @Test
    fun testSearchAppFunctions_byFunctionName() =
        runBlocking<Unit> {
            val searchSpec =
                AppFunctionSearchSpec(
                    functionNames =
                        setOf(
                            AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA_PRINT.name
                        )
                )
            val result = appFunctionManager.searchAppFunctions(searchSpec)
            assertThat(result)
                .containsExactly(AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA_PRINT)
        }

    @Test
    fun testSearchAppFunctions_bySchemaName() =
        runBlocking<Unit> {
            val searchSpec = AppFunctionSearchSpec(schemaName = "print")
            val result = appFunctionManager.searchAppFunctions(searchSpec)
            assertThat(result)
                .containsAtLeast(
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NOTES_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA2_PRINT,
                )
        }

    @Test
    fun testSearchAppFunctions_bySchemaCategory() =
        runBlocking<Unit> {
            val searchSpec = AppFunctionSearchSpec(schemaCategory = "media")
            val result = appFunctionManager.searchAppFunctions(searchSpec)
            assertThat(result)
                .containsAtLeast(
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA2_PRINT,
                )
        }

    @Test
    fun testSearchAppFunctions_byMinSchemaVersion() =
        runBlocking<Unit> {
            val searchSpec = AppFunctionSearchSpec(minSchemaVersion = 2)
            val result = appFunctionManager.searchAppFunctions(searchSpec)
            assertThat(result)
                .contains(AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA2_PRINT)
        }

    @Test
    fun testSearchAppFunctions_combinedFilters() =
        runBlocking<Unit> {
            val searchSpec =
                AppFunctionSearchSpec(
                    packageNames = setOf(context.packageName),
                    schemaName = "print",
                    schemaCategory = "media",
                )
            val result = appFunctionManager.searchAppFunctions(searchSpec)
            assertThat(result)
                .containsAtLeast(
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA2_PRINT,
                )
        }

    @Test
    fun testSearchAppFunctions_noMatchingSchemaName_returnsEmpty() =
        runBlocking<Unit> {
            val searchSpec = AppFunctionSearchSpec(schemaName = "nonExistentSchema")
            val result = appFunctionManager.searchAppFunctions(searchSpec)
            assertThat(result).isEmpty()
        }

    @Test
    fun testSearchAppFunctions_noMatchingPackage_returnsEmpty() =
        runBlocking<Unit> {
            val searchSpec = AppFunctionSearchSpec(packageNames = setOf("nonExistentPackage"))
            val result = appFunctionManager.searchAppFunctions(searchSpec)
            assertThat(result).isEmpty()
        }

    @Test
    fun testSearchAppFunctions_noMatchingFunctionName_returnsEmpty() =
        runBlocking<Unit> {
            val searchSpec =
                AppFunctionSearchSpec(
                    functionNames =
                        setOf(AppFunctionName(context.packageName, "nonExistentFunction"))
                )
            val result = appFunctionManager.searchAppFunctions(searchSpec)
            assertThat(result).isEmpty()
        }

    fun getResourceAsStream(name: String): InputStream {
        return checkNotNull(Thread.currentThread().contextClassLoader).getResourceAsStream(name)
    }
}
