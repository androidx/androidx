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

import android.Manifest
import android.app.PendingIntent
import android.app.UiAutomation
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import androidx.appfunctions.core.AppFunctionMetadataTestHelper.Companion.TEST_APP_METADATA
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.InputStream
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class AppFunctionManagerCompatTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val metadataTestHelper: AppFunctionMetadataTestHelper =
        AppFunctionMetadataTestHelper(context)

    private lateinit var appFunctionManagerCompat: AppFunctionManagerCompat

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
        val appFunctionManagerCompatOrNull = AppFunctionManagerCompat.getInstance(context)
        assumeNotNull(appFunctionManagerCompatOrNull)
        appFunctionManagerCompat = checkNotNull(appFunctionManagerCompatOrNull)

        uiAutomation.adoptShellPermissionIdentity(
            Manifest.permission.INSTALL_PACKAGES,
            "android.permission.EXECUTE_APP_FUNCTIONS",
        )

        runBlocking {
            metadataTestHelper.awaitAppFunctionIndexed(resetFunctionIds)

            // Reset all test ids
            for (functionIds in resetFunctionIds) {
                appFunctionManagerCompat.setAppFunctionEnabled(
                    functionIds,
                    AppFunctionManagerCompat.Companion.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }
    }

    @After
    fun tearDown() {
        uiAutomation.dropShellPermissionIdentity()
        uiAutomation.executeShellCommand("pm uninstall $ADDITIONAL_APP_PACKAGE")
    }

    @Test
    fun testSelfIsAppFunctionEnabled_defaultEnabledState() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.isAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT
            )
        }

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun testSelfIsAppFunctionEnabled_defaultDisabledState() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.isAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT
            )
        }

        assertThat(isEnabled).isFalse()
    }

    @Test
    fun testIsAppFunctionEnabled_defaultEnabledState() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
            )
        }

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun testIsAppFunctionEnabled_defaultDisabledState() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
            )
        }

        assertThat(isEnabled).isFalse()
    }

    @Test
    fun testSetAppFunctionEnabled_overrideToDisable() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DISABLED,
            )
            appFunctionManagerCompat.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
            )
        }

        assertThat(isEnabled).isFalse()
    }

    @Test
    fun testSetAppFunctionEnabled_overrideToEnabled() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_ENABLED,
            )
            appFunctionManagerCompat.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
            )
        }

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun testSetAppFunctionEnabled_resetToEnabled() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DISABLED,
            )
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DEFAULT,
            )
            appFunctionManagerCompat.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
            )
        }

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun testSetAppFunctionEnabled_resetToDisabled() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_ENABLED,
            )
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DEFAULT,
            )
            appFunctionManagerCompat.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
            )
        }

        assertThat(isEnabled).isFalse()
    }

    @Test
    fun testExecuteAppFunction_functionNotExist() {
        val request =
            ExecuteAppFunctionRequest(
                targetPackageName = context.packageName,
                functionIdentifier = "fakeFunctionId",
                functionParameters = AppFunctionData.EMPTY,
            )

        val response = runBlocking { appFunctionManagerCompat.executeAppFunction(request) }

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        assertThat((response as ExecuteAppFunctionResponse.Error).error)
            .isInstanceOf(AppFunctionFunctionNotFoundException::class.java)
    }

    @Test
    fun testExecuteAppFunction_functionSucceed() {
        val request =
            ExecuteAppFunctionRequest(
                targetPackageName = context.packageName,
                functionIdentifier =
                    AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                functionParameters = AppFunctionData.EMPTY,
            )

        val response = runBlocking { appFunctionManagerCompat.executeAppFunction(request) }

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        assertThat(
                (response as ExecuteAppFunctionResponse.Success)
                    .returnValue
                    .getString(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
            )
            .isEqualTo("result")
    }

    @Test
    fun testExecuteAppFunction_functionFail() {
        val request =
            ExecuteAppFunctionRequest(
                targetPackageName = context.packageName,
                functionIdentifier =
                    AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_FAIL,
                functionParameters = AppFunctionData.EMPTY,
            )

        val response = runBlocking { appFunctionManagerCompat.executeAppFunction(request) }

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        assertThat((response as ExecuteAppFunctionResponse.Error).error)
            .isInstanceOf(AppFunctionInvalidArgumentException::class.java)
    }

    @Test
    fun observeAppFunctions_emptyPackagesListInSearchSpec_noResults() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec(packageNames = emptySet())

            assertThat(appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec).first())
                .isEmpty()
        }

    @Test
    fun observeAppFunctions_emptySchemaNameInSearchSpec_noResults() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec(schemaName = "")

            assertThat(appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec).first())
                .isEmpty()
        }

    @Test
    fun observeAppFunctions_emptySchemaCategoryInSearchSpec_noResults() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec(schemaCategory = "")

            assertThat(appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec).first())
                .isEmpty()
        }

    @Test
    fun observeAppFunction_queryFunctionsInMainPackage_returnsAllComponents() = runBlocking {
        // Component metadata is only available with dynamic indexer
        assumeTrue(metadataTestHelper.isDynamicIndexerAvailable())
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(context.packageName))
        val expectedComponentsInMainPackage =
            AppFunctionComponentsMetadata(
                dataTypes =
                    buildMap {
                        put(
                            "com.testdata.RecursiveSerializable",
                            AppFunctionObjectTypeMetadata(
                                properties =
                                    buildMap {
                                        put(
                                            "nested",
                                            AppFunctionReferenceTypeMetadata(
                                                referenceDataType =
                                                    "com.testdata.RecursiveSerializable",
                                                isNullable = true,
                                            ),
                                        )
                                    },
                                required = listOf("nested"),
                                qualifiedName = "com.testdata.RecursiveSerializable",
                                isNullable = true,
                                description = "Description of com.testdata.RecursiveSerializable",
                            ),
                        )
                        put(
                            "com.testdata.DerivedSerializable",
                            AppFunctionAllOfTypeMetadata(
                                matchAll =
                                    listOf(
                                        AppFunctionReferenceTypeMetadata(
                                            referenceDataType =
                                                "com.testdata.RecursiveSerializable",
                                            isNullable = true,
                                        )
                                    ),
                                qualifiedName = "com.testdata.DerivedSerializable",
                                isNullable = true,
                                description = "A child class of [RecursiveSerializable].",
                            ),
                        )
                    }
            )

        val appFunctions: List<AppFunctionMetadata> =
            appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec).first().flatMap {
                it.appFunctions
            }

        assertThat(appFunctions).isNotEmpty()
        assertThat(appFunctions.filter { it.components != expectedComponentsInMainPackage })
            .isEmpty()
    }

    @Test
    fun observeAppFunctions_packageListNotSetInSpec_returnsMetadataForAllApps_withDynamicIndexer() =
        runBlocking<Unit> {
            assumeTrue(metadataTestHelper.isDynamicIndexerAvailable())
            installApk(ADDITIONAL_APK_FILE)
            val searchFunctionSpec = AppFunctionSearchSpec()

            val appFunctionPackages: List<AppFunctionPackageMetadata> =
                appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec).first()

            // At least two apps should be indexed, can be more due to system apps implementing app
            // functions.
            assertThat(appFunctionPackages.size).isGreaterThan(1)
            val testAppPackage =
                appFunctionPackages.single { it.packageName == context.packageName }
            assertThat(testAppPackage.resolveAppFunctionAppMetadata(context))
                .isEqualTo(TEST_APP_METADATA)

            assertThat(testAppPackage.appFunctions)
                .containsExactly(
                    AppFunctionMetadataTestHelper.FunctionMetadata.NO_SCHEMA_ENABLED_BY_DEFAULT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NO_SCHEMA_DISABLED_BY_DEFAULT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA2_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NOTES_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NO_SCHEMA_EXECUTION_FAIL,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NO_SCHEMA_EXECUTION_SUCCEED,
                )
            val additionalAppPackage =
                appFunctionPackages.single { it.packageName == ADDITIONAL_APP_PACKAGE }
            // Additional app doesn't specify app metadata.
            assertThat(additionalAppPackage.resolveAppFunctionAppMetadata(context)).isNull()
            assertThat(additionalAppPackage.appFunctions)
                .containsExactly(
                    AppFunctionMetadataTestHelper.FunctionMetadata.ADDITIONAL_LEGACY_CREATE_NOTE
                )
        }

    @Test
    fun observeAppFunctions_multiplePackagesSetInSpec_returnsAppFunctionsFromBoth_withDynamicIndexer() =
        runBlocking<Unit> {
            assumeTrue(metadataTestHelper.isDynamicIndexerAvailable())
            installApk(ADDITIONAL_APK_FILE)
            val searchFunctionSpec =
                AppFunctionSearchSpec(
                    packageNames = setOf(context.packageName, ADDITIONAL_APP_PACKAGE)
                )

            val appFunctionPackages: List<AppFunctionPackageMetadata> =
                appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec).first()

            assertThat(appFunctionPackages.size).isEqualTo(2)
            val testAppPackage =
                appFunctionPackages.single { it.packageName == context.packageName }
            assertThat(testAppPackage.resolveAppFunctionAppMetadata(context))
                .isEqualTo(TEST_APP_METADATA)

            assertThat(testAppPackage.appFunctions)
                .containsExactly(
                    AppFunctionMetadataTestHelper.FunctionMetadata.NO_SCHEMA_ENABLED_BY_DEFAULT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NO_SCHEMA_DISABLED_BY_DEFAULT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA2_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NOTES_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NO_SCHEMA_EXECUTION_FAIL,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NO_SCHEMA_EXECUTION_SUCCEED,
                )
            val additionalAppPackage =
                appFunctionPackages.single { it.packageName == ADDITIONAL_APP_PACKAGE }
            // Additional app doesn't specify app metadata.
            assertThat(additionalAppPackage.resolveAppFunctionAppMetadata(context)).isNull()
            assertThat(additionalAppPackage.appFunctions)
                .containsExactly(
                    AppFunctionMetadataTestHelper.FunctionMetadata.ADDITIONAL_LEGACY_CREATE_NOTE
                )
        }

    // TODO: b/421388047 - Add more test cases, checking missing fields and translations.

    @Test
    fun observeAppFunctions_multiplePackagesSetInSpec_returnsSchemaAppFunctionsFromBoth_withLegacyIndexer() =
        runBlocking<Unit> {
            assumeFalse(metadataTestHelper.isDynamicIndexerAvailable())
            installApk(ADDITIONAL_APK_FILE)
            val searchFunctionSpec =
                AppFunctionSearchSpec(
                    packageNames = setOf(context.packageName, ADDITIONAL_APP_PACKAGE)
                )

            val appFunctions: List<AppFunctionMetadata> =
                appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec).first().flatMap {
                    it.appFunctions
                }

            assertThat(appFunctions)
                .containsExactly(
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA2_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NOTES_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.ADDITIONAL_LEGACY_CREATE_NOTE,
                )
        }

    @Test
    fun observeAppFunctions_packageListSetInSpec_returnsAppFunctionsInPackage_withDynamicIndexer() =
        runBlocking<Unit> {
            assumeTrue(metadataTestHelper.isDynamicIndexerAvailable())
            installApk(ADDITIONAL_APK_FILE)
            val searchFunctionSpec =
                AppFunctionSearchSpec(packageNames = setOf(context.packageName))

            val appFunctions: List<AppFunctionMetadata> =
                appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec).first().flatMap {
                    it.appFunctions
                }

            assertThat(appFunctions)
                .containsExactly(
                    AppFunctionMetadataTestHelper.FunctionMetadata.NO_SCHEMA_ENABLED_BY_DEFAULT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NO_SCHEMA_DISABLED_BY_DEFAULT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA2_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NOTES_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NO_SCHEMA_EXECUTION_FAIL,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NO_SCHEMA_EXECUTION_SUCCEED,
                )
        }

    @Test
    fun observeAppFunctions_packageListSetInSpec_returnsSchemaAppFunctionsInPackage_withLegacyIndexer() =
        runBlocking<Unit> {
            assumeFalse(metadataTestHelper.isDynamicIndexerAvailable())
            installApk(ADDITIONAL_APK_FILE)
            val searchFunctionSpec =
                AppFunctionSearchSpec(packageNames = setOf(context.packageName))

            val appFunctions: List<AppFunctionMetadata> =
                appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec).first().flatMap {
                    it.appFunctions
                }

            assertThat(appFunctions)
                .containsExactly(
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA2_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NOTES_SCHEMA_PRINT,
                )
        }

    @Test
    fun observeAppFunctions_schemaNameInSpec_returnsMatchingAppFunctions() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec(schemaName = "print")

            val appFunctions: List<AppFunctionMetadata> =
                appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec).first().flatMap {
                    it.appFunctions
                }

            assertThat(appFunctions)
                .containsAtLeast(
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.NOTES_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA2_PRINT,
                )
        }

    @Test
    fun observeAppFunctions_schemaCategoryInSpec_returnsMatchingAppFunctions() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec(schemaCategory = "media")

            val appFunctions: List<AppFunctionMetadata> =
                appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec).first().flatMap {
                    it.appFunctions
                }

            assertThat(appFunctions)
                .containsAtLeast(
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA2_PRINT,
                )
        }

    @Test
    fun observeAppFunctions_minSchemaVersionInSpec_returnsAppFunctionsWithSchemaVersionGreaterThanMin() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec(minSchemaVersion = 2)

            val appFunctions: List<AppFunctionMetadata> =
                appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec).first().flatMap {
                    it.appFunctions
                }

            assertThat(appFunctions)
                .contains(AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA2_PRINT)
        }

    @Test
    fun observeAppFunctions_isDisabledInRuntime_returnsIsEnabledFalse() =
        runBlocking<Unit> {
            val functionIdToTest = AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT
            val searchFunctionSpec = AppFunctionSearchSpec()
            appFunctionManagerCompat.setAppFunctionEnabled(
                functionIdToTest,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DISABLED,
            )

            val appFunctionMetadata =
                appFunctionManagerCompat
                    .observeAppFunctions(searchFunctionSpec)
                    .first()
                    .flatMap { it.appFunctions }
                    .single { it.id == functionIdToTest }

            assertThat(appFunctionMetadata.isEnabled).isFalse()
        }

    @Test
    fun observeAppFunctions_isEnabledInRuntime_returnsIsEnabledTrue() =
        runBlocking<Unit> {
            val functionIdToTest = AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA2_PRINT
            val searchFunctionSpec = AppFunctionSearchSpec()
            appFunctionManagerCompat.setAppFunctionEnabled(
                functionIdToTest,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_ENABLED,
            )

            val appFunctionMetadata =
                appFunctionManagerCompat
                    .observeAppFunctions(searchFunctionSpec)
                    .first()
                    .flatMap { it.appFunctions }
                    .single { it.id == functionIdToTest }

            assertThat(appFunctionMetadata.isEnabled).isTrue()
        }

    @Test
    fun observeAppFunctions_observeDocumentChanges_returnsListWithUpdatedValue() =
        runBlocking<Unit> {
            val functionIdToTest = AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT
            val searchFunctionSpec =
                AppFunctionSearchSpec(packageNames = setOf(context.packageName))
            val appFunctionSearchFlow =
                appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec)
            val emittedValues =
                appFunctionSearchFlow.shareIn(
                    scope = CoroutineScope(Dispatchers.Default),
                    started = SharingStarted.Eagerly,
                    replay = 10,
                )
            emittedValues.first() // Allow emitting initial value and registering callback.

            // Modify the runtime document.
            appFunctionManagerCompat.setAppFunctionEnabled(
                functionIdToTest,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DISABLED,
            )

            // Collect in a separate scope to avoid deadlock within the testcase.
            runBlocking(Dispatchers.Default) { emittedValues.take(2).collect {} }
            assertThat(emittedValues.replayCache).hasSize(2)
            // Assert first result to be default value.
            assertThat(
                    emittedValues.replayCache[0]
                        .flatMap { it.appFunctions }
                        .single {
                            it.id == AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT
                        }
                        .isEnabled
                )
                .isEqualTo(
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA_PRINT.isEnabled
                )
            // Assert next update has updated value.
            assertThat(
                    emittedValues.replayCache[1]
                        .flatMap { it.appFunctions }
                        .single {
                            it.id == AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT
                        }
                        .isEnabled
                )
                .isFalse()
        }

    @Test
    fun observeAppFunctions_multipleUpdates_returnsUpdatesAfterDebouncing() =
        runBlocking<Unit> {
            val functionIdToTest = AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT
            val searchFunctionSpec =
                AppFunctionSearchSpec(packageNames = setOf(context.packageName))
            val appFunctionSearchFlow =
                appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec)
            val emittedValues =
                appFunctionSearchFlow.shareIn(
                    scope = CoroutineScope(Dispatchers.Default),
                    started = SharingStarted.Eagerly,
                    replay = 10,
                )
            emittedValues.first() // Allow emitting initial value and registering callback.

            // Modify the runtime document twice.
            appFunctionManagerCompat.setAppFunctionEnabled(
                functionIdToTest,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DISABLED,
            )
            appFunctionManagerCompat.setAppFunctionEnabled(
                functionIdToTest,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_ENABLED,
            )

            // Collect in a separate scope to avoid deadlock within the testcase.
            runBlocking(Dispatchers.Default) { emittedValues.take(2).collect {} }
            // Only 2 updates are emitted.
            assertThat(emittedValues.replayCache).hasSize(2)
            assertThat(
                    emittedValues.replayCache[1]
                        .flatMap { it.appFunctions }
                        .single { it.id == functionIdToTest }
                        .isEnabled
                )
                .isTrue()
        }

    @Test
    fun observeAppFunctions_multiplePackageInstall_onlyObservesSpecifiedPackageUpdate() =
        runBlocking<Unit> {
            val searchFunctionSpec =
                AppFunctionSearchSpec(packageNames = setOf(context.packageName))
            val appFunctionSearchFlow =
                appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec)
            val emittedValues =
                appFunctionSearchFlow.shareIn(
                    scope = CoroutineScope(Dispatchers.Default),
                    started = SharingStarted.Eagerly,
                    replay = 10,
                )
            emittedValues.first() // Allow emitting initial value and registering callback.

            installApk(ADDITIONAL_APK_FILE)
            delay(1000) // Avoid debounce
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DISABLED,
            )

            // Collect in a separate scope to avoid deadlock within the testcase.
            runBlocking(Dispatchers.Default) { emittedValues.take(2).collect {} }
            // Only 2 updates are emitted and update from other app is ignored.
            assertThat(emittedValues.replayCache).hasSize(2)
            assertThat(
                    emittedValues.replayCache[1]
                        .flatMap { it.appFunctions }
                        .single {
                            it.id == AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT
                        }
                        .isEnabled
                )
                .isFalse()
        }

    @Test
    fun observeAppFunctions_multiplePackagesInSpec_updatesEmittedForAllChanges_withDynamicIndexer() =
        runBlocking<Unit> {
            assumeTrue(metadataTestHelper.isDynamicIndexerAvailable())
            val searchFunctionSpec =
                AppFunctionSearchSpec(
                    packageNames = setOf(context.packageName, ADDITIONAL_APP_PACKAGE)
                )
            val appFunctionSearchFlow =
                appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec)
            val emittedValues =
                appFunctionSearchFlow.shareIn(
                    scope = CoroutineScope(Dispatchers.Default),
                    started = SharingStarted.Eagerly,
                    replay = 10,
                )
            emittedValues.first() // Allow emitting initial value and registering callback.

            installApk(ADDITIONAL_APK_FILE)
            delay(1000) // Avoid debounce
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DISABLED,
            )

            // Collect in a separate scope to avoid deadlock within the testcase.
            runBlocking(Dispatchers.Default) { emittedValues.take(3).collect {} }
            assertThat(emittedValues.replayCache).hasSize(3)
            // First result only contains functions from first package.
            assertThat(emittedValues.replayCache[0].flatMap { it.appFunctions }.map { it.id })
                .containsExactly(
                    AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                    AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
                    AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA2_PRINT,
                    AppFunctionMetadataTestHelper.FunctionIds.NOTES_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_FAIL,
                    AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                )
            // Second result contains functionId from additional app install as well.
            assertThat(emittedValues.replayCache[1].flatMap { it.appFunctions }.map { it.id })
                .contains(AppFunctionMetadataTestHelper.FunctionIds.ADDITIONAL_LEGACY_CREATE_NOTE)
            // Third result has modified value of isEnabled from the original package.
            assertThat(
                    emittedValues.replayCache[2]
                        .flatMap { it.appFunctions }
                        .single {
                            it.id == AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT
                        }
                        .isEnabled
                )
                .isFalse()
        }

    @Test
    fun observeAppFunctions_multiplePackagesInSpec_updatesEmittedForAllChanges_withLegacyIndexer() =
        runBlocking<Unit> {
            assumeFalse(metadataTestHelper.isDynamicIndexerAvailable())
            val searchFunctionSpec =
                AppFunctionSearchSpec(
                    packageNames = setOf(context.packageName, ADDITIONAL_APP_PACKAGE)
                )
            val appFunctionSearchFlow =
                appFunctionManagerCompat.observeAppFunctions(searchFunctionSpec)
            val emittedValues =
                appFunctionSearchFlow.shareIn(
                    scope = CoroutineScope(Dispatchers.Default),
                    started = SharingStarted.Eagerly,
                    replay = 10,
                )
            emittedValues.first() // Allow emitting initial value and registering callback.

            installApk(ADDITIONAL_APK_FILE)
            delay(1000) // Avoid debounce
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DISABLED,
            )

            // Collect in a separate scope to avoid deadlock within the testcase.
            runBlocking(Dispatchers.Default) { emittedValues.take(3).collect {} }
            assertThat(emittedValues.replayCache).hasSize(3)
            // First result only contains schema functions from first package.
            assertThat(emittedValues.replayCache[0].flatMap { it.appFunctions }.map { it.id })
                .containsExactly(
                    AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
                    AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA2_PRINT,
                    AppFunctionMetadataTestHelper.FunctionIds.NOTES_SCHEMA_PRINT,
                )
            // Second result contains functionId from additional app install as well.
            assertThat(emittedValues.replayCache[1].flatMap { it.appFunctions }.map { it.id })
                .contains(AppFunctionMetadataTestHelper.FunctionIds.ADDITIONAL_LEGACY_CREATE_NOTE)
            // Third result has modified value of isEnabled from the original package.
            assertThat(
                    emittedValues.replayCache[2]
                        .flatMap { it.appFunctions }
                        .single {
                            it.id == AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT
                        }
                        .isEnabled
                )
                .isFalse()
        }

    private suspend fun installApk(apk: String) {
        val installer = context.packageManager.packageInstaller
        val sessionParams =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)

        val sessionId = installer.createSession(sessionParams)

        installer.openSession(sessionId).use { session ->
            session.openWrite("apk_install", 0, -1).use { outputStream ->
                getResourceAsStream(apk).transferTo(outputStream)
            }

            assertThat(session.commitSession()).isTrue()
        }

        metadataTestHelper.awaitAppFunctionIndexed(
            setOf(AppFunctionMetadataTestHelper.FunctionIds.ADDITIONAL_LEGACY_CREATE_NOTE)
        )
    }

    fun getResourceAsStream(name: String): InputStream {
        return checkNotNull(Thread.currentThread().contextClassLoader).getResourceAsStream(name)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun PackageInstaller.Session.commitSession(): Boolean {
        val action = "com.example.COMMIT_COMPLETE.${System.currentTimeMillis()}"

        return suspendCancellableCoroutine { continuation ->
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        context.unregisterReceiver(this)

                        val status =
                            intent.getIntExtra(
                                PackageInstaller.EXTRA_STATUS,
                                PackageInstaller.STATUS_FAILURE,
                            )
                        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

                        if (status == PackageInstaller.STATUS_SUCCESS) {
                            continuation.resume(true) {}
                        } else {
                            continuation.resumeWithException(
                                Exception("Installation failed: $message")
                            )
                        }
                    }
                }

            val filter = IntentFilter(action)
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)

            val intent = Intent(action).setPackage(context.packageName)
            val sender =
                PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )

            this.commit(sender.intentSender)

            continuation.invokeOnCancellation {
                // Unregister the receiver if the coroutine is cancelled
                context.unregisterReceiver(receiver)
            }
        }
    }

    private companion object {
        const val ADDITIONAL_APK_FILE = "notes.apk"
        const val ADDITIONAL_APP_PACKAGE = "com.google.android.app.notes"
    }
}
