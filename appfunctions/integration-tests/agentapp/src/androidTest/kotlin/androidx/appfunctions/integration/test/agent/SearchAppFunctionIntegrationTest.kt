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

package androidx.appfunctions.integration.test.agent

import android.Manifest
import android.app.AppInteractionAttribution
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appfunction.integration.test.sharedschema.ASubclass
import androidx.appfunction.integration.test.sharedschema.Attachment
import androidx.appfunction.integration.test.sharedschema.BSubclass
import androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction
import androidx.appfunction.integration.test.sharedschema.CreateNoteParams
import androidx.appfunction.integration.test.sharedschema.Note
import androidx.appfunction.integration.test.sharedschema.OneOfSealedInterface
import androidx.appfunction.integration.test.sharedschema.OneOfSealedNestedSerializable
import androidx.appfunction.integration.test.sharedschema.OpenableNote
import androidx.appfunction.integration.test.sharedschema.Owner
import androidx.appfunction.integration.test.sharedschema.SetField
import androidx.appfunction.integration.test.sharedschema.UpdateNoteParams
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.AppFunctionTextResource
import androidx.appfunctions.AppFunctionUriGrant
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.ExecuteAppFunctionResponse.Success.Companion.PROPERTY_RETURN_VALUE
import androidx.appfunctions.integration.test.agent.AppSearchMetadataHelper.isDynamicIndexerAvailable
import androidx.appfunctions.integration.test.agent.TestUtil.doBlocking
import androidx.appfunctions.integration.test.agent.TestUtil.grantAppFunctionAccess
import androidx.appfunctions.integration.test.agent.TestUtil.retryAssert
import androidx.appfunctions.integration.test.agent.TestUtil.revokeAppFunctionAccess
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import kotlin.test.assertIs
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/** Integration tests for [AppFunctionManager.searchAppFunctions]. */
// TODO(b/508188326): These should be merged into IntegrationTest once the legacy
// observeAppFunctions is removed.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
@LargeTest
class SearchAppFunctionIntegrationTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var appFunctionManager: AppFunctionManager
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    private val targetAppApkFile =
        InstrumentationRegistry.getArguments().getString("TARGET_APP_APK")
            ?: throw IllegalStateException("TARGET_APP_APK argument not found")

    @Before
    fun setup() = doBlocking {
        uiAutomation.grantAppFunctionAccess(targetContext, TARGET_APP_PACKAGE)

        appFunctionManager = checkNotNull(AppFunctionManager.getInstance(targetContext))

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
    fun searchAllAppFunctions_returnsAllAppFunction_withDynamicIndexer() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))

        val appFunctions: List<AppFunctionMetadata> =
            appFunctionManager.searchAppFunctions(searchFunctionSpec)

        val aggregatedFunctionCount = 22
        val multiServiceFunctionCount = 6
        val dynamicFunctionsCount = 1
        if (Build.VERSION.SDK_INT >= 37) {
            assertThat(appFunctions)
                .hasSize(
                    aggregatedFunctionCount + multiServiceFunctionCount + dynamicFunctionsCount
                )
        } else {
            assertThat(appFunctions).hasSize(aggregatedFunctionCount)
        }
    }

    @Test
    fun searchAllAppFunctions_returnsAllSchemaAppFunction_withLegacyIndexer() = doBlocking {
        assumeFalse(isDynamicIndexerAvailable(targetContext))
        val searchFunctionSpec = AppFunctionSearchSpec(packageNames = setOf(TARGET_APP_PACKAGE))

        val appFunctions: List<AppFunctionMetadata> =
            appFunctionManager.searchAppFunctions(searchFunctionSpec)

        assertThat(appFunctions).hasSize(1)
    }

    @Test
    fun executeAppFunction_success() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val metadata =
            searchAppFunction("androidx.appfunctions.integration.testapp.TestFunctions#add")

        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        metadata.packageName,
                        metadata.id,
                        AppFunctionData.Builder(metadata.parameters, metadata.components)
                            .setLong("num1", 1)
                            .setLong("num2", 2)
                            .build(),
                    )
            )

        val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
        assertThat(successResponse.returnValue.getLong(PROPERTY_RETURN_VALUE)).isEqualTo(3)
    }

    @Test
    @SdkSuppress(minSdkVersion = 37)
    fun executeAppFunctionWithAttribution_success() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val metadata =
            searchAppFunction("androidx.appfunctions.integration.testapp.TestFunctions#add")

        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        metadata.packageName,
                        metadata.id,
                        AppFunctionData.Builder(metadata.parameters, metadata.components)
                            .setLong("num1", 1)
                            .setLong("num2", 2)
                            .build(),
                        AppInteractionAttribution.Builder(
                                AppInteractionAttribution.INTERACTION_TYPE_USER_QUERY
                            )
                            .build(),
                    )
            )

        val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
        assertThat(successResponse.returnValue.getLong(PROPERTY_RETURN_VALUE)).isEqualTo(3)
    }

    @Test
    fun executeAppFunction_voidReturnType_success() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val metadata =
            searchAppFunction(
                "androidx.appfunctions.integration.testapp.TestFunctions#voidFunction"
            )

        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        metadata.packageName,
                        metadata.id,
                        AppFunctionData.Builder(metadata.parameters, metadata.components).build(),
                    )
            )

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
    }

    @Test
    fun executeAppFunction_setFactory_success() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val metadata =
            searchAppFunction(
                "androidx.appfunctions.integration.testapp.TestFactory#isCreatedByFactory"
            )
        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        metadata.packageName,
                        metadata.id,
                        AppFunctionData.Builder(metadata.parameters, metadata.components).build(),
                    )
            )

        val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
        assertThat(successResponse.returnValue.getBoolean(PROPERTY_RETURN_VALUE)).isEqualTo(true)
    }

    @Test
    fun executeAppFunction_functionInLibraryModule_success() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val metadata =
            searchAppFunction(
                "androidx.appfunctions.integration.testapp.library.TestFunctions2#concat"
            )

        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        metadata.packageName,
                        metadata.id,
                        AppFunctionData.Builder(metadata.parameters, metadata.components)
                            .setString("str1", "log")
                            .setString("str2", "cat")
                            .build(),
                    )
            )

        val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
        assertThat(successResponse.returnValue.getString(PROPERTY_RETURN_VALUE)).isEqualTo("logcat")
    }

    @Test
    fun executeAppFunction_appThrows_fail() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val metadata =
            searchAppFunction("androidx.appfunctions.integration.testapp.TestFunctions#doThrow")

        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        metadata.packageName,
                        metadata.id,
                        AppFunctionData.Builder(metadata.parameters, metadata.components).build(),
                    )
            )

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        val errorResponse = response as ExecuteAppFunctionResponse.Error
        assertThat(errorResponse.error)
            .isInstanceOf(AppFunctionInvalidArgumentException::class.java)
    }

    @Test
    fun executeAppFunction_createNote() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val createNoteMetadata =
            searchAppFunction("androidx.appfunctions.integration.testapp.TestFunctions#createNote")

        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        createNoteMetadata.packageName,
                        createNoteMetadata.id,
                        AppFunctionData.Builder(
                                createNoteMetadata.parameters,
                                createNoteMetadata.components,
                            )
                            .setAppFunctionData(
                                "createNoteParams",
                                AppFunctionData.serialize(
                                    CreateNoteParams(
                                        title = "Test Title",
                                        content = listOf("1", "2"),
                                        owner = Owner("test"),
                                        attachments =
                                            listOf(Attachment("Uri1", Attachment("nested"))),
                                        folderId = null,
                                    ),
                                    CreateNoteParams::class.java,
                                ),
                            )
                            .build(),
                    )
            )

        val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
        val expectedNote =
            Note(
                title = "Test Title",
                content = listOf("1", "2"),
                owner = Owner("test"),
                attachments = listOf(Attachment("Uri1", Attachment("nested"))),
            )
        assertThat(
                successResponse.returnValue
                    .getAppFunctionData(PROPERTY_RETURN_VALUE)
                    ?.deserialize(Note::class.java)
            )
            .isEqualTo(expectedNote)
    }

    @Test
    fun executeAppFunction_createNote_withOpenableCapability_returnsNote() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val metadata =
            searchAppFunction(
                "androidx.appfunctions.integration.testapp.TestFunctions#getOpenableNote"
            )

        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        metadata.packageName,
                        metadata.id,
                        AppFunctionData.Builder(metadata.parameters, metadata.components)
                            .setAppFunctionData(
                                "createNoteParams",
                                AppFunctionData.serialize(
                                    CreateNoteParams(
                                        title = "Test Title",
                                        content = listOf("1", "2"),
                                        owner = Owner("test"),
                                        attachments =
                                            listOf(Attachment("Uri1", Attachment("nested"))),
                                        folderId = null,
                                    ),
                                    CreateNoteParams::class.java,
                                ),
                            )
                            .build(),
                    )
            )

        val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
        val expectedNote =
            Note(
                title = "Test Title",
                content = listOf("1", "2"),
                owner = Owner("test"),
                attachments = listOf(Attachment("Uri1", Attachment("nested"))),
            )
        assertThat(
                successResponse.returnValue
                    .getAppFunctionData(PROPERTY_RETURN_VALUE)
                    ?.deserialize(Note::class.java)
            )
            .isEqualTo(expectedNote)
    }

    @Test
    fun executeAppFunction_createNote_withOpenableCapability_returnsOpenableNote() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val metadata =
            searchAppFunction(
                "androidx.appfunctions.integration.testapp.TestFunctions#getOpenableNote"
            )

        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        metadata.packageName,
                        metadata.id,
                        AppFunctionData.Builder(metadata.parameters, metadata.components)
                            .setAppFunctionData(
                                "createNoteParams",
                                AppFunctionData.serialize(
                                    CreateNoteParams(
                                        title = "Test Title",
                                        content = listOf("1", "2"),
                                        owner = Owner("test"),
                                        attachments =
                                            listOf(Attachment("Uri1", Attachment("nested"))),
                                        folderId = null,
                                    ),
                                    CreateNoteParams::class.java,
                                ),
                            )
                            .build(),
                    )
            )

        val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
        val expectedNote =
            Note(
                title = "Test Title",
                content = listOf("1", "2"),
                owner = Owner("test"),
                attachments = listOf(Attachment("Uri1", Attachment("nested"))),
            )
        val openableNoteResult =
            assertIs<OpenableNote>(
                successResponse.returnValue
                    .getAppFunctionData(PROPERTY_RETURN_VALUE)
                    ?.deserialize(OpenableNote::class.java)
            )

        assertThat(openableNoteResult.title).isEqualTo(expectedNote.title)
        assertThat(openableNoteResult.content).isEqualTo(expectedNote.content)
        assertThat(openableNoteResult.owner).isEqualTo(expectedNote.owner)
        assertThat(openableNoteResult.attachments).isEqualTo(expectedNote.attachments)
        assertThat(openableNoteResult.intentToOpen).isNotNull()
    }

    @Test
    fun executeAppFunction_updateNote_success() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val metadata =
            searchAppFunction("androidx.appfunctions.integration.testapp.TestFunctions#updateNote")
        val attachment = Attachment(uri = "uri", nested = null)
        val dateTime = LocalDateTime.of(1, 1, 1, 1, 1)

        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        metadata.packageName,
                        metadata.id,
                        AppFunctionData.Builder(metadata.parameters, metadata.components)
                            .setAppFunctionData(
                                "updateNoteParams",
                                AppFunctionData.serialize(
                                    UpdateNoteParams(
                                        title = SetField("NewTitle1"),
                                        nullableTitle = SetField("NewTitle2"),
                                        content = SetField(listOf("NewContent1")),
                                        nullableContent = SetField(listOf("NewContent2")),
                                        attachments = SetField(listOf(attachment)),
                                        modifiedTime = SetField(dateTime),
                                    ),
                                    UpdateNoteParams::class.java,
                                ),
                            )
                            .build(),
                    )
            )

        val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
        val expectedNote =
            Note(
                title = "NewTitle1_NewTitle2",
                content = listOf("NewContent1", "NewContent2"),
                owner = Owner("test"),
                attachments = listOf(attachment),
                modifiedTime = dateTime,
            )
        assertThat(
                successResponse.returnValue
                    .getAppFunctionData(PROPERTY_RETURN_VALUE)
                    ?.deserialize(Note::class.java)
            )
            .isEqualTo(expectedNote)
    }

    @Test
    fun executeAppFunction_updateNoteSetFieldNullContent_success() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val metadata =
            searchAppFunction("androidx.appfunctions.integration.testapp.TestFunctions#updateNote")

        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        metadata.packageName,
                        metadata.id,
                        AppFunctionData.Builder(metadata.parameters, metadata.components)
                            .setAppFunctionData(
                                "updateNoteParams",
                                AppFunctionData.serialize(
                                    UpdateNoteParams(
                                        title = SetField("NewTitle1"),
                                        nullableTitle = SetField(null),
                                        content = SetField(listOf("NewContent1")),
                                        nullableContent = SetField(null),
                                    ),
                                    UpdateNoteParams::class.java,
                                ),
                            )
                            .build(),
                    )
            )

        val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
        val expectedNote =
            Note(
                title = "NewTitle1_DefaultTitle",
                content = listOf("NewContent1", "DefaultContent"),
                owner = Owner("test"),
                attachments = listOf(),
            )
        assertThat(
                successResponse.returnValue
                    .getAppFunctionData(PROPERTY_RETURN_VALUE)
                    ?.deserialize(Note::class.java)
            )
            .isEqualTo(expectedNote)
    }

    @Test
    fun executeAppFunction_updateNoteNullSetFields_success() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val metadata =
            searchAppFunction("androidx.appfunctions.integration.testapp.TestFunctions#updateNote")

        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        metadata.packageName,
                        metadata.id,
                        AppFunctionData.Builder(metadata.parameters, metadata.components)
                            .setAppFunctionData(
                                "updateNoteParams",
                                AppFunctionData.serialize(
                                    UpdateNoteParams(),
                                    UpdateNoteParams::class.java,
                                ),
                            )
                            .build(),
                    )
            )

        val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
        val expectedNote =
            Note(
                title = "DefaultTitle_DefaultTitle",
                content = listOf("DefaultContent", "DefaultContent"),
                owner = Owner("test"),
                attachments = listOf(),
            )
        assertThat(
                successResponse.returnValue
                    .getAppFunctionData(PROPERTY_RETURN_VALUE)
                    ?.deserialize(Note::class.java)
            )
            .isEqualTo(expectedNote)
    }

    @Test
    fun executeAppFunction_schemaCreateNote_success() = doBlocking {
        val createNoteMetadata =
            appFunctionManager
                .searchAppFunctions(
                    AppFunctionSearchSpec(
                        packageNames = setOf(TARGET_APP_PACKAGE),
                        schemaCategory = "myNotes",
                        schemaName = "createNote",
                        minSchemaVersion = 2,
                    )
                )
                .single()
        val request =
            ExecuteAppFunctionRequest(
                functionIdentifier = createNoteMetadata.id,
                targetPackageName = createNoteMetadata.packageName,
                functionParameters =
                    AppFunctionData.Builder(
                            createNoteMetadata.parameters,
                            createNoteMetadata.components,
                        )
                        .setAppFunctionData(
                            "parameters",
                            AppFunctionData.Builder(
                                    requireTargetObjectTypeMetadata(
                                        "parameters",
                                        createNoteMetadata.parameters,
                                        createNoteMetadata.components,
                                    ),
                                    createNoteMetadata.components,
                                )
                                .setString("title", "Test Title")
                                .setString("content", "Some valid content")
                                .setAppFunctionDataList("attachments", emptyList())
                                .setString("groupId", "testGroupId")
                                .setString("externalUuid", "testExternalUuid")
                                .build(),
                        )
                        .build(),
            )

        val response = appFunctionManager.executeAppFunction(request)

        assertIs<ExecuteAppFunctionResponse.Success>(response)
        val resultNote =
            response.returnValue
                .getAppFunctionData(PROPERTY_RETURN_VALUE)
                ?.getAppFunctionData("createdNote")
        assertThat(resultNote?.getString("id")).isEqualTo("testId")
        assertThat(resultNote?.getString("title")).isEqualTo("Test Title")
    }

    @Test
    fun executeAppFunction_schemaCreateNoteSerialization_success() = doBlocking {
        val createNoteMetadata =
            appFunctionManager
                .searchAppFunctions(
                    AppFunctionSearchSpec(
                        packageNames = setOf(TARGET_APP_PACKAGE),
                        schemaCategory = "myNotes",
                        schemaName = "createNote",
                        minSchemaVersion = 2,
                    )
                )
                .single()
        val parameters =
            CreateNoteAppFunction.Parameters(
                title = "Test Title",
                content = "Some valid content",
                attachments = emptyList(),
                groupId = "testGroupId",
                externalUuid = "testExternalUuid",
            )
        val request =
            ExecuteAppFunctionRequest(
                functionIdentifier = createNoteMetadata.id,
                targetPackageName = createNoteMetadata.packageName,
                functionParameters =
                    AppFunctionData.Builder(
                            createNoteMetadata.parameters,
                            createNoteMetadata.components,
                        )
                        .setAppFunctionData(
                            "parameters",
                            AppFunctionData.serialize(
                                parameters,
                                CreateNoteAppFunction.Parameters::class.java,
                            ),
                        )
                        .build(),
            )

        val response = appFunctionManager.executeAppFunction(request)

        assertIs<ExecuteAppFunctionResponse.Success>(response)
        val resultNote =
            response.returnValue
                .getAppFunctionData(PROPERTY_RETURN_VALUE)
                ?.getAppFunctionData("createdNote")
        assertThat(resultNote?.getString("id")).isEqualTo("testId")
        assertThat(resultNote?.getString("title")).isEqualTo("Test Title")
    }

    @Test
    fun executeAppFunction_schemaCreateNote_readInvalidFieldFail() = doBlocking {
        val createNoteMetadata =
            appFunctionManager
                .searchAppFunctions(
                    AppFunctionSearchSpec(
                        packageNames = setOf(TARGET_APP_PACKAGE),
                        schemaCategory = "myNotes",
                        schemaName = "createNote",
                        minSchemaVersion = 2,
                    )
                )
                .single()
        val request =
            ExecuteAppFunctionRequest(
                functionIdentifier = createNoteMetadata.id,
                targetPackageName = createNoteMetadata.packageName,
                functionParameters =
                    AppFunctionData.Builder(
                            createNoteMetadata.parameters,
                            createNoteMetadata.components,
                        )
                        .setAppFunctionData(
                            "parameters",
                            AppFunctionData.Builder(
                                    requireTargetObjectTypeMetadata(
                                        "parameters",
                                        createNoteMetadata.parameters,
                                        createNoteMetadata.components,
                                    ),
                                    createNoteMetadata.components,
                                )
                                .setString("title", "Test Title")
                                .setString("content", "Some valid content")
                                .setAppFunctionDataList("attachments", emptyList())
                                .setString("groupId", "testGroupId")
                                .setString("externalUuid", "testExternalUuid")
                                .build(),
                        )
                        .build(),
            )

        val response = appFunctionManager.executeAppFunction(request)

        assertIs<ExecuteAppFunctionResponse.Success>(response)
        val resultNote =
            response.returnValue
                .getAppFunctionData(PROPERTY_RETURN_VALUE)
                ?.getAppFunctionData("createdNote")
        assertThrows(IllegalArgumentException::class.java) { resultNote?.getInt(("title")) }
    }

    @Test
    fun executeAppFunction_oneOfSerializableFunction_success() = doBlocking {
        assumeTrue(isDynamicIndexerAvailable(targetContext))
        val oneOfFunctionMetadata = searchAppFunction(ONE_OF_FUNCTION_ID)
        val oneOfList =
            listOf(
                ASubclass(interfaceProperty = "interfacePropertyA", str = "strA"),
                BSubclass(
                    interfaceProperty = "interfacePropertyB",
                    integer = 10,
                    resources =
                        listOf(
                            AppFunctionTextResource(
                                mimeType = "text/plain",
                                content = "Hello World!",
                            )
                        ),
                ),
            )
        val request =
            ExecuteAppFunctionRequest(
                targetPackageName = oneOfFunctionMetadata.packageName,
                functionIdentifier = ONE_OF_FUNCTION_ID,
                functionParameters =
                    AppFunctionData.Builder(
                            oneOfFunctionMetadata.parameters,
                            oneOfFunctionMetadata.components,
                        )
                        .setAppFunctionDataList(
                            "oneOfList",
                            oneOfList.map {
                                AppFunctionData.serialize(it, OneOfSealedInterface::class.java)
                            },
                        )
                        .build(),
            )

        val response = appFunctionManager.executeAppFunction(request)

        assertIs<ExecuteAppFunctionResponse.Success>(response)
        assertThat(
                response.returnValue.getAppFunctionDataList(PROPERTY_RETURN_VALUE)?.map {
                    it.deserialize(OneOfSealedNestedSerializable::class.java)
                }
            )
            .containsExactlyElementsIn(oneOfList.map { OneOfSealedNestedSerializable(it) })
    }

    /**
     * Requires that [parameters] contains the [AppFunctionObjectTypeMetadata] under
     * [parameterName].
     *
     * @throws IllegalArgumentException If unable to find the target
     *   [AppFunctionObjectTypeMetadata].
     */
    private fun requireTargetObjectTypeMetadata(
        parameterName: String,
        parameters: List<AppFunctionParameterMetadata>,
        components: AppFunctionComponentsMetadata,
    ): AppFunctionObjectTypeMetadata {
        val targetParameterMetadata =
            parameters.find { it.name == parameterName }
                ?: throw IllegalArgumentException(
                    "Unable to find parameter metadata with name $parameterName"
                )
        val parameterDataTypeMetadata = targetParameterMetadata.dataType
        return when (parameterDataTypeMetadata) {
            is AppFunctionObjectTypeMetadata -> {
                parameterDataTypeMetadata
            }
            is AppFunctionReferenceTypeMetadata -> {
                components.dataTypes[parameterDataTypeMetadata.referenceDataType]
                    as? AppFunctionObjectTypeMetadata
                    ?: throw IllegalArgumentException(
                        "Unable to find object metadata with reference name ${parameterDataTypeMetadata.referenceDataType}"
                    )
            }
            else -> {
                throw IllegalArgumentException(
                    "The parameter metadata of $parameterName is not an object type."
                )
            }
        }
    }

    private suspend fun searchAppFunction(id: String): AppFunctionMetadata {
        return appFunctionManager.searchAppFunctions(AppFunctionSearchSpec()).single { it.id == id }
    }

    private fun AppFunctionUriGrant.getValidPersistableUriFlags(): Int {
        return modeFlags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
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

    private companion object {
        const val TARGET_APP_PACKAGE = "androidx.appfunctions.integration.testapp"
        const val ONE_OF_FUNCTION_ID =
            "androidx.appfunctions.integration.testapp.OneOfFunctions#oneOfFunction"
    }
}
