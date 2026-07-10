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

import android.content.Context
import android.os.UserManager
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionName
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata
import androidx.appfunctions.testing.FakeAppFunctionManagerApi
import androidx.appfunctions.testing.FakeAppFunctionReader
import androidx.appfunctions.testing.FakeTranslator
import androidx.appfunctions.testing.FakeTranslatorSelector
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowUserManager

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 33)
class AppFunctionManagerUnitTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fakeAppFunctionReader = FakeAppFunctionReader()
    private val fakeTranslateSelector = FakeTranslatorSelector()
    private val fakeAppFunctionApi = FakeAppFunctionManagerApi()
    private val fakeTranslator = FakeTranslator()
    private lateinit var mAppFunctionManager: AppFunctionManager

    @Before
    fun setup() {
        mAppFunctionManager =
            AppFunctionManager(
                context,
                fakeAppFunctionReader,
                fakeAppFunctionApi,
                fakeTranslateSelector,
            )
    }

    @Test
    fun executeAppFunction_functionNotExist_shouldThrow() = runTest {
        fakeAppFunctionApi.executeAppFunctionResponse =
            ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY)

        val response =
            mAppFunctionManager.executeAppFunction(
                request = ExecuteAppFunctionRequest("x", "y", AppFunctionData.EMPTY)
            )

        val errorResponse = assertIs<ExecuteAppFunctionResponse.Error>(response)
        assertThat(errorResponse.error)
            .isInstanceOf(AppFunctionFunctionNotFoundException::class.java)
    }

    @Test
    fun executeAppFunction_appUsesV1AndHaveTranslator_enableTranslation() = runTest {
        val functionId = "functionId"
        val packageName = "com.pkg"
        fakeAppFunctionReader.addAppFunctionMetadata(
            AppFunctionMetadata(
                name = AppFunctionName(packageName = packageName, functionIdentifier = functionId),
                packageMetadata =
                    AppFunctionPackageMetadata(
                        packageName = packageName,
                        components = AppFunctionComponentsMetadata(),
                    ),
                isEnabled = true,
                schema =
                    AppFunctionSchemaMetadata(category = "notes", name = "createNote", version = 1),
                parameters = emptyList(),
                response =
                    AppFunctionResponseMetadata(AppFunctionUnitTypeMetadata(isNullable = false)),
            )
        )
        fakeTranslateSelector.setTranslator(fakeTranslator)
        fakeAppFunctionApi.executeAppFunctionResponse =
            ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY)

        val response =
            mAppFunctionManager.executeAppFunction(
                request = ExecuteAppFunctionRequest(packageName, functionId, AppFunctionData.EMPTY)
            )
        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        assertThat(fakeTranslator.downgradeRequestCalled).isTrue()
        assertThat(fakeTranslator.upgradeRequestCalled).isFalse()
        assertThat(fakeTranslator.downgradeResponseCalled).isFalse()
        assertThat(fakeTranslator.upgradeResponseCalled).isTrue()
    }

    @Test
    fun executeAppFunction_appUsesV2AndHaveTranslator_NoTranslation() = runTest {
        val functionId = "functionId"
        val packageName = "com.pkg"
        fakeAppFunctionReader.addAppFunctionMetadata(
            AppFunctionMetadata(
                name = AppFunctionName(packageName = packageName, functionIdentifier = functionId),
                packageMetadata =
                    AppFunctionPackageMetadata(
                        packageName = packageName,
                        components = AppFunctionComponentsMetadata(),
                    ),
                isEnabled = true,
                schema =
                    AppFunctionSchemaMetadata(category = "notes", name = "createNote", version = 2),
                parameters = emptyList(),
                response =
                    AppFunctionResponseMetadata(AppFunctionUnitTypeMetadata(isNullable = false)),
            )
        )
        fakeTranslateSelector.setTranslator(fakeTranslator)
        fakeAppFunctionApi.executeAppFunctionResponse =
            ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY)

        val response =
            mAppFunctionManager.executeAppFunction(
                request = ExecuteAppFunctionRequest(packageName, functionId, AppFunctionData.EMPTY)
            )

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        assertThat(fakeTranslator.downgradeRequestCalled).isFalse()
        assertThat(fakeTranslator.upgradeRequestCalled).isFalse()
        assertThat(fakeTranslator.downgradeResponseCalled).isFalse()
        assertThat(fakeTranslator.upgradeResponseCalled).isFalse()
    }

    @Test
    fun getInstance_whenProfileUser_returnsNull() {
        Shadows.shadowOf(context.getSystemService(UserManager::class.java)!!)
            .addProfile(1, 0, "Profile", ShadowUserManager.FLAG_PROFILE)

        val instance = AppFunctionManager.getInstance(context)

        assertThat(instance).isNull()
    }
}
