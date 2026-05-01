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

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.appfunction.integration.test.sharedschema.MultiServiceCreateNoteParams
import androidx.appfunction.integration.test.sharedschema.MultiServiceFilesData
import androidx.appfunction.integration.test.sharedschema.MultiServiceNote
import androidx.appfunction.integration.test.sharedschema.MultiServiceProxyTypesWrapper
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.ExecuteAppFunctionResponse.Success.Companion.PROPERTY_RETURN_VALUE
import androidx.appfunctions.integration.test.agent.TestUtil.assertReadAccessible
import androidx.appfunctions.integration.test.agent.TestUtil.assertReadInaccessible
import androidx.appfunctions.integration.test.agent.TestUtil.assertWriteAccessible
import androidx.appfunctions.integration.test.agent.TestUtil.assertWriteInaccessible
import androidx.appfunctions.integration.test.agent.TestUtil.doBlocking
import androidx.appfunctions.integration.test.agent.TestUtil.grantAppFunctionAccess
import androidx.appfunctions.integration.test.agent.TestUtil.retryAssert
import androidx.appfunctions.integration.test.agent.TestUtil.revokeAppFunctionAccess
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = 37)
@LargeTest
class MultiServiceIntegrationTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var appFunctionCaller: AppFunctionCaller
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    private val targetAppApkFile =
        InstrumentationRegistry.getArguments().getString("TARGET_APP_APK")
            ?: throw IllegalStateException("TARGET_APP_APK argument not found")

    @Before
    fun setup() = doBlocking {
        uiAutomation.grantAppFunctionAccess(targetContext, TARGET_APP_PACKAGE)

        appFunctionCaller = AppFunctionCaller(targetContext)

        uiAutomation.apply {
            adoptShellPermissionIdentity(
                Manifest.permission.INSTALL_PACKAGES,
                Manifest.permission.EXECUTE_APP_FUNCTIONS,
            )
        }
        InstallHelper.install(targetAppApkFile)
        targetContext.awaitAppFunctionsIndexed(TARGET_APP_PACKAGE)
    }

    @After
    fun tearDown() {
        uiAutomation.revokeAppFunctionAccess()
        InstallHelper.uninstall(TARGET_APP_PACKAGE)
        uiAutomation.dropShellPermissionIdentity()
    }

    @Test
    fun searchAndInvokeCustomServiceFunction_success() = doBlocking {
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))

        val appFunctions: List<AppFunctionMetadata> =
            appFunctionCaller.observeAppFunctions(searchFunctionSpec).first().flatMap {
                it.appFunctions
            }
        val targetFunction =
            appFunctions.single {
                it.id == "androidx.appfunctions.integration.testapp.CustomAppFunctionService#add"
            }
        val response =
            appFunctionCaller.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetFunction.packageName,
                        targetFunction.id,
                        AppFunctionData.Builder(
                                targetFunction.parameters,
                                targetFunction.components,
                            )
                            .setInt("a", 1)
                            .setInt("b", 2)
                            .build(),
                    )
            )

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        val successResponse = response as ExecuteAppFunctionResponse.Success
        assertThat(successResponse.returnValue.getInt(PROPERTY_RETURN_VALUE)).isEqualTo(3)
    }

    @Test
    fun executeAppFunction_echoProxyTypes_succeed() = doBlocking {
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))
        val appFunctions: List<AppFunctionMetadata> =
            appFunctionCaller.observeAppFunctions(searchFunctionSpec).first().flatMap {
                it.appFunctions
            }
        val targetFunction =
            appFunctions.single {
                it.id ==
                    "androidx.appfunctions.integration.testapp.BaseSimpleAppFunctionService#echoProxyTypes"
            }
        val value =
            MultiServiceProxyTypesWrapper(
                localDateTime = LocalDateTime.of(2026, 4, 25, 22, 0),
                localDate = LocalDate.of(2026, 4, 25),
                localTime = LocalTime.of(22, 0),
                uri = Uri.parse("https://www.google.com/"),
                instant = Instant.ofEpochMilli(1000),
                zoneId = ZoneId.of("UTC"),
            )

        val response =
            appFunctionCaller.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetFunction.packageName,
                        targetFunction.id,
                        AppFunctionData.Builder(
                                targetFunction.parameters,
                                targetFunction.components,
                            )
                            .setAppFunctionData(
                                "value",
                                AppFunctionData.serialize(
                                    value,
                                    MultiServiceProxyTypesWrapper::class.java,
                                ),
                            )
                            .build(),
                    )
            )

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        val successResponse = response as ExecuteAppFunctionResponse.Success
        val result =
            successResponse.returnValue
                .getAppFunctionData(PROPERTY_RETURN_VALUE)
                ?.deserialize(MultiServiceProxyTypesWrapper::class.java)

        assertThat(result).isNotNull()
        assertThat(result!!.localDateTime).isEqualTo(value.localDateTime)
        assertThat(result.localDate).isEqualTo(value.localDate)
        assertThat(result.localTime).isEqualTo(value.localTime)
        assertThat(result.uri.toString()).isEqualTo(value.uri.toString())
        assertThat(result.instant).isEqualTo(value.instant)
        assertThat(result.zoneId).isEqualTo(value.zoneId)
    }

    @Test
    fun executeAppFunction_appThrows_fail() = doBlocking {
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))
        val appFunctions: List<AppFunctionMetadata> =
            appFunctionCaller.observeAppFunctions(searchFunctionSpec).first().flatMap {
                it.appFunctions
            }
        val targetFunction =
            appFunctions.single {
                it.id ==
                    "androidx.appfunctions.integration.testapp.BaseSimpleAppFunctionService#doThrow"
            }

        val response =
            appFunctionCaller.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetFunction.packageName,
                        targetFunction.id,
                        AppFunctionData.Builder(
                                targetFunction.parameters,
                                targetFunction.components,
                            )
                            .build(),
                    )
            )

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        val errorResponse = response as ExecuteAppFunctionResponse.Error
        assertThat(errorResponse.error)
            .isInstanceOf(AppFunctionInvalidArgumentException::class.java)
    }

    @Test
    fun executeAppFunction_enumValueFunction_validParam_executesService() = doBlocking {
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))
        val appFunctions: List<AppFunctionMetadata> =
            appFunctionCaller.observeAppFunctions(searchFunctionSpec).first().flatMap {
                it.appFunctions
            }
        val targetFunction =
            appFunctions.single {
                it.id ==
                    "androidx.appfunctions.integration.testapp.BaseSimpleAppFunctionService#enumValueFunction"
            }

        val response =
            appFunctionCaller.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetFunction.packageName,
                        targetFunction.id,
                        AppFunctionData.Builder(
                                targetFunction.parameters,
                                targetFunction.components,
                            )
                            .setInt("intEnum", 0)
                            .setString("stringEnum", "A")
                            .build(),
                    )
            )

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
    }

    @Test
    fun executeAppFunction_getFilesData_uriAccessGranted() = doBlocking {
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))
        val appFunctions: List<AppFunctionMetadata> =
            appFunctionCaller.observeAppFunctions(searchFunctionSpec).first().flatMap {
                it.appFunctions
            }
        val targetFunction =
            appFunctions.single {
                it.id ==
                    "androidx.appfunctions.integration.testapp.BaseSimpleAppFunctionService#getFilesData"
            }

        val response =
            appFunctionCaller.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetFunction.packageName,
                        targetFunction.id,
                        AppFunctionData.Builder(
                                targetFunction.parameters,
                                targetFunction.components,
                            )
                            .build(),
                    )
            )

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        val successResponse = response as ExecuteAppFunctionResponse.Success
        val filesData =
            successResponse.returnValue
                .getAppFunctionData(PROPERTY_RETURN_VALUE)
                ?.deserialize(MultiServiceFilesData::class.java)
        assertThat(filesData).isNotNull()
        targetContext.assertReadAccessible(checkNotNull(filesData).readOnlyUri.uri)
        targetContext.assertWriteInaccessible(filesData.readOnlyUri.uri)
        targetContext.assertReadInaccessible(filesData.writeOnlyUri.uri)
        targetContext.assertWriteAccessible(filesData.writeOnlyUri.uri)
        targetContext.assertReadAccessible(filesData.readWriteUri.uri)
        targetContext.assertWriteAccessible(filesData.readWriteUri.uri)
    }

    @Test
    fun executeAppFunction_createNote_success() = doBlocking {
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))
        val appFunctions: List<AppFunctionMetadata> =
            appFunctionCaller.observeAppFunctions(searchFunctionSpec).first().flatMap {
                it.appFunctions
            }
        val targetFunction =
            appFunctions.single {
                it.id ==
                    "androidx.appfunctions.integration.testapp.BaseSimpleAppFunctionService#createNote"
            }

        val response =
            appFunctionCaller.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetFunction.packageName,
                        targetFunction.id,
                        AppFunctionData.Builder(
                                targetFunction.parameters,
                                targetFunction.components,
                            )
                            .setAppFunctionData(
                                "createNoteParams",
                                AppFunctionData.serialize(
                                    MultiServiceCreateNoteParams(
                                        title = "Test Title",
                                        content = listOf("1", "2"),
                                    ),
                                    MultiServiceCreateNoteParams::class.java,
                                ),
                            )
                            .build(),
                    )
            )

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        val successResponse = response as ExecuteAppFunctionResponse.Success
        val expectedNote = MultiServiceNote(title = "Test Title", content = listOf("1", "2"))
        assertThat(
                successResponse.returnValue
                    .getAppFunctionData(PROPERTY_RETURN_VALUE)
                    ?.deserialize(MultiServiceNote::class.java)
            )
            .isEqualTo(expectedNote)
    }

    @Test
    fun observeAppFunctions_shouldGetCorrectDescription() = doBlocking {
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))
        val appFunctions: List<AppFunctionMetadata> =
            appFunctionCaller.observeAppFunctions(searchFunctionSpec).first().flatMap {
                it.appFunctions
            }

        val targetFunction =
            appFunctions.single {
                it.id ==
                    "androidx.appfunctions.integration.testapp.BaseSimpleAppFunctionService#createNote"
            }

        assertThat(targetFunction.description).isEqualTo("Multiservice to create note.")
        assertThat(targetFunction.response.description).isEqualTo("The multiservice node.")
        assertThat(targetFunction.parameters.single { it.name == "createNoteParams" }.description)
            .isEqualTo("Multi-service's createNoteParams.")
        val multiServiceCreateNoteParamDataType =
            targetFunction.components.dataTypes[
                    "androidx.appfunction.integration.test.sharedschema.MultiServiceCreateNoteParams"]
                as? AppFunctionObjectTypeMetadata
        assertThat(multiServiceCreateNoteParamDataType!!.description)
            .isEqualTo("The MultiServiceCreateNoteParams.")
        assertThat(multiServiceCreateNoteParamDataType.properties["title"]?.description)
            .isEqualTo("The multiservice note title.")
        assertThat(multiServiceCreateNoteParamDataType.properties["content"]?.description)
            .isEqualTo("The multiservice note content.")
        val multiServiceCreateNote =
            targetFunction.components.dataTypes[
                    "androidx.appfunction.integration.test.sharedschema.MultiServiceNote"]
                as? AppFunctionObjectTypeMetadata
        assertThat(multiServiceCreateNote!!.description).isEqualTo("The MultiServiceNote.")
        assertThat(multiServiceCreateNote.properties["title"]?.description)
            .isEqualTo("The multiservice note title.")
        assertThat(multiServiceCreateNote.properties["content"]?.description)
            .isEqualTo("The multiservice note content.")
    }

    private suspend fun Context.awaitAppFunctionsIndexed(targetPackage: String) {
        retryAssert {
            val functionIds =
                AppSearchMetadataHelper.collectFunctionIds(
                    this@awaitAppFunctionsIndexed,
                    targetPackage,
                )
            assertThat(functionIds).isNotEmpty()
        }
    }

    companion object {
        const val TARGET_APP_PACKAGE = "androidx.appfunctions.integration.testapp"
    }
}
