/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.privacysandbox.sdkruntime.client.loader

import android.content.Context
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.privacysandbox.sdkruntime.client.EmptyActivity
import androidx.privacysandbox.sdkruntime.client.TestActivityHolder
import androidx.privacysandbox.sdkruntime.client.TestSdkConfigs
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.privacysandbox.sdkruntime.client.loader.impl.SandboxedSdkContextCompat
import androidx.privacysandbox.sdkruntime.client.loader.storage.TestLocalSdkStorage
import androidx.privacysandbox.sdkruntime.client.loader.storage.toClassPathString
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkInfo
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.LoadSdkCallback
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import androidx.privacysandbox.sdkruntime.core.internal.ClientApiVersion
import androidx.privacysandbox.sdkruntime.core.internal.ClientFeature
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import dalvik.system.BaseDexClassLoader
import java.io.File
import java.util.concurrent.Executor
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
internal class LocalSdkProviderTest(
    @Suppress("unused") private val label: String, // Added to test names by JUnit
    private val sdkName: String,
    private val originalSdkVersion: Int,
    private val forcedSdkVersion: Int,
) {

    private lateinit var controller: TestStubController
    private lateinit var loadedSdk: LocalSdkProvider

    @Before
    fun setUp() {
        val sdkConfig = TestSdkConfigs.forSdkName(sdkName)

        controller = TestStubController()

        val overrideVersionHandshake =
            if (originalSdkVersion != forcedSdkVersion) {
                VersionHandshake(forcedSdkVersion)
            } else {
                null
            }
        loadedSdk = loadTestSdkFromAssets(sdkConfig, controller, overrideVersionHandshake)
        assertThat(loadedSdk.extractApiVersion()).isEqualTo(originalSdkVersion)
    }

    @Test
    fun loadSdk_attachCorrectContext() {
        val sdkContext = loadedSdk.extractSdkContext()
        assertThat(sdkContext.javaClass.name).isEqualTo(SandboxedSdkContextCompat::class.java.name)
    }

    @Test
    fun onLoadSdk_callOnLoadSdkAndReturnResult() {
        val params = Bundle()

        val sandboxedSdkCompat = loadedSdk.onLoadSdk(params)

        val expectedBinder =
            loadedSdk.extractSdkProviderFieldValue<Binder>(
                fieldName = "onLoadSdkBinder",
            )
        assertThat(sandboxedSdkCompat.getInterface()).isEqualTo(expectedBinder)

        val lastParams =
            loadedSdk.extractSdkProviderFieldValue<Bundle>(
                fieldName = "lastOnLoadSdkParams",
            )
        assertThat(lastParams).isEqualTo(params)
    }

    @Test
    fun onLoadSdk_callOnLoadSdkAndThrowException() {
        val params = Bundle()
        params.putBoolean("needFail", true)

        val ex = assertThrows(LoadSdkCompatException::class.java) { loadedSdk.onLoadSdk(params) }

        assertThat(ex.extraInformation).isEqualTo(params)
    }

    @Test
    fun beforeUnloadSdk_callBeforeUnloadSdk() {
        loadedSdk.beforeUnloadSdk()

        val isBeforeUnloadSdkCalled =
            loadedSdk.extractSdkProviderFieldValue<Boolean>(fieldName = "isBeforeUnloadSdkCalled")

        assertThat(isBeforeUnloadSdkCalled).isTrue()
    }

    @Test
    fun getSandboxedSdks_delegateToSdkController() {
        val expectedResult =
            SandboxedSdkCompat(
                sdkInterface = Binder(),
                sdkInfo = SandboxedSdkInfo(name = "sdkName", version = 42)
            )
        controller.sandboxedSdksResult = listOf(expectedResult)

        val testSdk = loadedSdk.loadTestSdk()
        val sandboxedSdks = testSdk.getSandboxedSdks()
        assertThat(sandboxedSdks).hasSize(1)
        val result = sandboxedSdks[0]

        assertThat(result.getInterface()).isEqualTo(expectedResult.getInterface())
        assertThat(result.getSdkName()).isEqualTo(expectedResult.getSdkInfo()!!.name)
        assertThat(result.getSdkVersion()).isEqualTo(expectedResult.getSdkInfo()!!.version)
    }

    @Test
    fun getAppOwnedSdkSandboxInterfaces_delegateToSdkController() {
        assumeFeatureAvailable(ClientFeature.APP_OWNED_INTERFACES)

        val expectedResult =
            AppOwnedSdkSandboxInterfaceCompat(
                name = "TestAppOwnedSdk",
                version = 42,
                binder = Binder(),
            )
        controller.appOwnedSdksResult = listOf(expectedResult)

        val testSdk = loadedSdk.loadTestSdk()
        val appOwnedSdks = testSdk.getAppOwnedSdkSandboxInterfaces()
        assertThat(appOwnedSdks).hasSize(1)
        val result = appOwnedSdks[0]

        assertThat(result.getName()).isEqualTo(expectedResult.getName())
        assertThat(result.getVersion()).isEqualTo(expectedResult.getVersion())
        assertThat(result.getInterface()).isEqualTo(expectedResult.getInterface())
    }

    @Test
    fun registerSdkSandboxActivityHandler_delegateToSdkController() {
        assumeFeatureAvailable(ClientFeature.SDK_ACTIVITY_HANDLER)

        val catchingHandler = CatchingSdkActivityHandler()

        val testSdk = loadedSdk.loadTestSdk()
        val token = testSdk.registerSdkSandboxActivityHandler(catchingHandler)
        val localHandler = controller.sdkActivityHandlers[token]!!

        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                val activityHolder = TestActivityHolder(this)
                localHandler.onActivityCreated(activityHolder)

                val receivedActivityHolder = catchingHandler.result!!
                val receivedActivity = receivedActivityHolder.getActivity()
                assertThat(receivedActivity).isSameInstanceAs(activityHolder.getActivity())
            }
        }
    }

    @Test
    fun sdkSandboxActivityHandler_ReceivesLifecycleEventsFromOriginalActivityHolder() {
        assumeFeatureAvailable(ClientFeature.SDK_ACTIVITY_HANDLER)

        val catchingHandler = CatchingSdkActivityHandler()

        val testSdk = loadedSdk.loadTestSdk()
        val token = testSdk.registerSdkSandboxActivityHandler(catchingHandler)
        val localHandler = controller.sdkActivityHandlers[token]!!

        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                val activityHolder = TestActivityHolder(this)
                localHandler.onActivityCreated(activityHolder)
                val receivedActivityHolder = catchingHandler.result!!

                for (event in Lifecycle.Event.values().filter { it != Lifecycle.Event.ON_ANY }) {
                    activityHolder.lifecycleRegistry.handleLifecycleEvent(event)
                    assertThat(receivedActivityHolder.getLifeCycleCurrentState())
                        .isEqualTo(event.targetState)
                }
            }
        }
    }

    @Test
    fun unregisterSdkSandboxActivityHandler_delegateToSdkController() {
        assumeFeatureAvailable(ClientFeature.SDK_ACTIVITY_HANDLER)

        val handler = CatchingSdkActivityHandler()

        val testSdk = loadedSdk.loadTestSdk()
        val token = testSdk.registerSdkSandboxActivityHandler(handler)
        testSdk.unregisterSdkSandboxActivityHandler(handler)

        assertThat(controller.sdkActivityHandlers[token]).isNull()
    }

    @Test
    fun loadSdk_returnsResultFromSdkController() {
        assumeFeatureAvailable(ClientFeature.LOAD_SDK)

        val sdkName = "SDK"
        val sdkParams = Bundle()
        val expectedSdkInfo = SandboxedSdkInfo(sdkName, 42)
        val expectedResult = SandboxedSdkCompat(Binder(), expectedSdkInfo)
        controller.loadSdkResult = expectedResult

        val result = loadedSdk.loadTestSdk().loadSdk(sdkName, sdkParams)

        assertThat(result.getInterface()).isEqualTo(expectedResult.getInterface())
        assertThat(result.getSdkName()).isEqualTo(expectedSdkInfo.name)
        assertThat(result.getSdkVersion()).isEqualTo(expectedSdkInfo.version)

        assertThat(controller.lastLoadSdkName).isEqualTo(sdkName)
        assertThat(controller.lastLoadSdkParams).isSameInstanceAs(sdkParams)
    }

    @Test
    fun loadSdk_rethrowsExceptionFromSdkController() {
        assumeFeatureAvailable(ClientFeature.LOAD_SDK)

        val expectedError =
            LoadSdkCompatException(
                LoadSdkCompatException.LOAD_SDK_INTERNAL_ERROR,
                "message",
                RuntimeException(),
                Bundle()
            )
        controller.loadSdkError = expectedError

        val result =
            assertThrows(LoadSdkCompatException::class.java) {
                loadedSdk.loadTestSdk().loadSdk("SDK", Bundle())
            }

        assertThat(result.loadSdkErrorCode).isEqualTo(expectedError.loadSdkErrorCode)
        assertThat(result.message).isEqualTo(expectedError.message)
        assertThat(result.cause).isSameInstanceAs(expectedError.cause)
        assertThat(result.extraInformation).isSameInstanceAs(expectedError.extraInformation)
    }

    @Test
    fun getClientPackageName_returnsResultFromSdkController() {
        assumeFeatureAvailable(ClientFeature.GET_CLIENT_PACKAGE_NAME)

        val clientPackageName = "client.package.name"
        controller.clientPackageNameResult = clientPackageName

        val result = loadedSdk.loadTestSdk().getClientPackageName()

        assertThat(result).isEqualTo(clientPackageName)
    }

    internal class TestClassLoaderFactory(private val testStorage: TestLocalSdkStorage) :
        SdkLoader.ClassLoaderFactory {
        override fun createClassLoaderFor(
            sdkConfig: LocalSdkConfig,
            parent: ClassLoader
        ): ClassLoader {
            val sdkDexFiles = testStorage.dexFilesFor(sdkConfig)

            val optimizedDirectory = File(sdkDexFiles.files[0].parentFile, "DexOpt")
            if (!optimizedDirectory.exists()) {
                optimizedDirectory.mkdirs()
            }

            return BaseDexClassLoader(
                sdkDexFiles.toClassPathString(),
                optimizedDirectory,
                /* librarySearchPath = */ null,
                parent
            )
        }
    }

    private fun assumeFeatureAvailable(clientFeature: ClientFeature) {
        assumeTrue(
            "Requires $clientFeature available (API >= ${clientFeature.availableFrom})",
            clientFeature.isAvailable(forcedSdkVersion)
        )
    }

    companion object {

        /**
         * Create test params for each supported [ClientApiVersion] + current and future. Each
         * released version must have test-sdk named as "vX" (where X is version to test). These
         * TestSDKs should be registered in RuntimeEnabledSdkTable.xml and be compatible with
         * [SdkControllerWrapper].
         */
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun params(): List<Array<Any>> = buildList {
            ClientApiVersion.values().forEach { version ->
                // FUTURE_VERSION tested separately
                if (version != ClientApiVersion.FUTURE_VERSION) {
                    add(
                        arrayOf(
                            "v${version.apiLevel}",
                            "v${version.apiLevel}",
                            version.apiLevel,
                            version.apiLevel,
                        )
                    )
                }
            }

            add(
                arrayOf(
                    "current_version",
                    "current",
                    ClientApiVersion.CURRENT_VERSION.apiLevel,
                    ClientApiVersion.CURRENT_VERSION.apiLevel
                )
            )

            add(
                arrayOf(
                    "future_version",
                    "current",
                    ClientApiVersion.CURRENT_VERSION.apiLevel,
                    ClientApiVersion.FUTURE_VERSION.apiLevel
                )
            )
        }

        private fun loadTestSdkFromAssets(
            sdkConfig: LocalSdkConfig,
            controller: TestStubController,
            overrideVersionHandshake: VersionHandshake?
        ): LocalSdkProvider {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val testStorage =
                TestLocalSdkStorage(context, rootFolder = File(context.cacheDir, "LocalSdkTest"))
            val sdkLoader =
                SdkLoader(
                    TestClassLoaderFactory(testStorage),
                    context,
                    object : SdkLoader.ControllerFactory {
                        override fun createControllerFor(sdkConfig: LocalSdkConfig) = controller
                    }
                )
            return sdkLoader.loadSdk(sdkConfig, overrideVersionHandshake)
        }
    }

    internal class TestStubController : SdkSandboxControllerCompat.SandboxControllerImpl {

        var sandboxedSdksResult: List<SandboxedSdkCompat> = emptyList()
        var appOwnedSdksResult: List<AppOwnedSdkSandboxInterfaceCompat> = emptyList()
        var sdkActivityHandlers: MutableMap<IBinder, SdkSandboxActivityHandlerCompat> =
            mutableMapOf()

        var lastLoadSdkName: String? = null
        var lastLoadSdkParams: Bundle? = null
        var loadSdkResult: SandboxedSdkCompat? = null
        var loadSdkError: LoadSdkCompatException? = null
        var clientPackageNameResult: String? = null

        override fun loadSdk(
            sdkName: String,
            params: Bundle,
            executor: Executor,
            callback: LoadSdkCallback
        ) {
            lastLoadSdkName = sdkName
            lastLoadSdkParams = params

            if (loadSdkResult != null) {
                executor.execute { callback.onResult(loadSdkResult!!) }
            } else {
                executor.execute {
                    callback.onError(
                        loadSdkError
                            ?: LoadSdkCompatException(
                                LoadSdkCompatException.LOAD_SDK_INTERNAL_ERROR,
                                "Shouldn't be called without setting result or error"
                            )
                    )
                }
            }
        }

        override fun getSandboxedSdks(): List<SandboxedSdkCompat> {
            return sandboxedSdksResult
        }

        override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> =
            appOwnedSdksResult

        override fun registerSdkSandboxActivityHandler(
            handlerCompat: SdkSandboxActivityHandlerCompat
        ): IBinder {
            val token = Binder()
            sdkActivityHandlers[token] = handlerCompat
            return token
        }

        override fun unregisterSdkSandboxActivityHandler(
            handlerCompat: SdkSandboxActivityHandlerCompat
        ) {
            sdkActivityHandlers.values.remove(handlerCompat)
        }

        override fun getClientPackageName(): String = clientPackageNameResult!!
    }
}
