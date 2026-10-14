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

import android.app.appfunctions.AppFunctionException as PlatformAppFunctionException
import android.app.appfunctions.ExecuteAppFunctionRequest as PlatformExecuteAppFunctionRequest
import android.app.appfunctions.ExecuteAppFunctionResponse as PlatformExecuteAppFunctionResponse
import android.content.Intent
import android.content.pm.SigningInfo
import android.net.Uri
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.appfunctions.internal.AppFunctionInventory
import androidx.appfunctions.internal.AppFunctionUriGrantTestInventory.Companion.URI_GRANT_COMPONENTS_METADATA
import androidx.appfunctions.internal.AppFunctionUriGrantTestInventory.Companion.URI_GRANT_OBJECT_TYPE_METADATA
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 36)
class AppFunctionServiceTest {
    private lateinit var service: TestAppFunctionService

    @Before
    fun setUp() {
        service = Robolectric.buildService(TestAppFunctionService::class.java).create().get()
    }

    @After
    fun tearDown() {
        service.onDestroy()
    }

    @Test
    @Config(maxSdk = 36)
    fun onExecuteFunction_belowApi37_grantsUriAccessViaContext() = runTest {
        val uriGrant =
            AppFunctionUriGrant(
                uri = Uri.parse("content://com.example/1"),
                modeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        service.nextResponse = createUriGrantResponse(uriGrant)

        service.executeFunction(
            PlatformExecuteAppFunctionRequest.Builder(service.packageName, TEST_FUNCTION_ID)
                .build(),
            callingPackage = "com.example.caller",
        )

        assertThat(service.grantedUris).containsExactly("com.example.caller" to uriGrant)
    }

    @Test
    @Config(minSdk = 37)
    fun onExecuteFunction_api37AndAbove_doesNotGrantUriAccessViaContext() = runTest {
        val uriGrant =
            AppFunctionUriGrant(
                uri = Uri.parse("content://com.example/1"),
                modeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        service.nextResponse = createUriGrantResponse(uriGrant)

        val platformResponse =
            service.executeFunction(
                PlatformExecuteAppFunctionRequest.Builder(service.packageName, TEST_FUNCTION_ID)
                    .build(),
                callingPackage = "com.example.caller",
            )

        assertThat(service.grantedUris).isEmpty()
        assertThat(platformResponse.uriGrants).containsExactly(uriGrant.toPlatformClass())
    }

    private fun createUriGrantResponse(
        uriGrant: AppFunctionUriGrant
    ): ExecuteAppFunctionResponse.Success {
        val returnValue =
            AppFunctionData.Builder(
                    AppFunctionResponseMetadata(valueType = URI_GRANT_OBJECT_TYPE_METADATA),
                    URI_GRANT_COMPONENTS_METADATA,
                )
                .setAppFunctionData(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    AppFunctionData.serialize(
                        URI_GRANT_OBJECT_TYPE_METADATA,
                        URI_GRANT_COMPONENTS_METADATA,
                        uriGrant,
                        AppFunctionUriGrant::class.java,
                    ),
                )
                .build()
        return ExecuteAppFunctionResponse.Success(returnValue)
    }

    private suspend fun AppFunctionService.executeFunction(
        request: PlatformExecuteAppFunctionRequest,
        callingPackage: String,
    ): PlatformExecuteAppFunctionResponse = suspendCancellableCoroutine { cont ->
        onExecuteFunction(
            request,
            callingPackage,
            SigningInfo(),
            CancellationSignal(),
            object :
                OutcomeReceiver<PlatformExecuteAppFunctionResponse, PlatformAppFunctionException> {
                override fun onResult(result: PlatformExecuteAppFunctionResponse) {
                    cont.resume(result)
                }

                override fun onError(error: PlatformAppFunctionException) {
                    cont.resumeWithException(RuntimeException(error.errorMessage))
                }
            },
        )
    }

    private class TestAppFunctionService : AppFunctionService() {
        val grantedUris = mutableListOf<Pair<String, AppFunctionUriGrant>>()
        lateinit var nextResponse: ExecuteAppFunctionResponse

        override fun getMainExecutor(): Executor = Executor { it.run() }

        override fun grantUriPermission(toPackage: String, uri: Uri, modeFlags: Int) {
            super.grantUriPermission(toPackage, uri, modeFlags)
            grantedUris.add(toPackage to AppFunctionUriGrant(uri, modeFlags))
        }

        override fun resolveInventory(): AppFunctionInventory =
            object : AppFunctionInventory {
                override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> =
                    mapOf(
                        TEST_FUNCTION_ID to
                            CompileTimeAppFunctionMetadata(
                                id = TEST_FUNCTION_ID,
                                isEnabledByDefault = true,
                                schema = null,
                                parameters = emptyList(),
                                response =
                                    AppFunctionResponseMetadata(
                                        valueType = URI_GRANT_OBJECT_TYPE_METADATA
                                    ),
                            )
                    )
                override val componentsMetadata: AppFunctionComponentsMetadata =
                    URI_GRANT_COMPONENTS_METADATA
            }

        override fun onExecuteFunction(
            request: ExecuteAppFunctionRequest,
            metadata: AppFunctionMetadata,
            cancellationSignal: CancellationSignal,
            callback: Consumer<ExecuteAppFunctionResponse>,
        ) {
            callback.accept(nextResponse)
        }
    }

    private companion object {
        const val TEST_FUNCTION_ID = "testUriGrantFunction"
    }
}
