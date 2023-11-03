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
import androidx.privacysandbox.sdkruntime.core.Versions
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import dalvik.system.BaseDexClassLoader
import java.io.File
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
internal class LocalSdkProviderTest(
    private val sdkName: String,
    private val sdkVersion: Int
) {

    private lateinit var controller: TestStubController
    private lateinit var loadedSdk: LocalSdkProvider

    @Before
    fun setUp() {
        val sdkConfig = TestSdkConfigs.forSdkName(sdkName)

        controller = TestStubController()
        loadedSdk = loadTestSdkFromAssets(sdkConfig, controller)
        assertThat(loadedSdk.extractApiVersion())
            .isEqualTo(sdkVersion)
    }

    @Test
    fun loadSdk_attachCorrectContext() {
        val sdkContext = loadedSdk.extractSdkContext()
        assertThat(sdkContext.javaClass.name)
            .isEqualTo(SandboxedSdkContextCompat::class.java.name)
    }

    @Test
    fun onLoadSdk_callOnLoadSdkAndReturnResult() {
        val params = Bundle()

        val sandboxedSdkCompat = loadedSdk.onLoadSdk(params)

        val expectedBinder = loadedSdk.extractSdkProviderFieldValue<Binder>(
            fieldName = "onLoadSdkBinder",
        )
        assertThat(sandboxedSdkCompat.getInterface()).isEqualTo(expectedBinder)

        val lastParams = loadedSdk.extractSdkProviderFieldValue<Bundle>(
            fieldName = "lastOnLoadSdkParams",
        )
        assertThat(lastParams).isEqualTo(params)
    }

    @Test
    fun onLoadSdk_callOnLoadSdkAndThrowException() {
        val params = Bundle()
        params.putBoolean("needFail", true)

        val ex = assertThrows(LoadSdkCompatException::class.java) {
            loadedSdk.onLoadSdk(params)
        }

        assertThat(ex.extraInformation).isEqualTo(params)
    }

    @Test
    fun beforeUnloadSdk_callBeforeUnloadSdk() {
        loadedSdk.beforeUnloadSdk()

        val isBeforeUnloadSdkCalled = loadedSdk.extractSdkProviderFieldValue<Boolean>(
            fieldName = "isBeforeUnloadSdkCalled"
        )

        assertThat(isBeforeUnloadSdkCalled).isTrue()
    }

    @Test
    fun getSandboxedSdks_delegateToSdkController() {
        assumeTrue(
            "Requires Versions.API_VERSION >= 2",
            sdkVersion >= 2
        )

        val expectedResult = SandboxedSdkCompat(
            sdkInterface = Binder(),
            sdkInfo = SandboxedSdkInfo(
                name = "sdkName",
                version = 42
            )
        )
        controller.sandboxedSdksResult = listOf(
            expectedResult
        )

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
        assumeTrue(
            "Requires Versions.API_VERSION >= 4",
            sdkVersion >= 4
        )

        val expectedResult = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestAppOwnedSdk",
            version = 42,
            binder = Binder(),
        )
        controller.appOwnedSdksResult = listOf(
            expectedResult
        )

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
        assumeTrue(
            "Requires Versions.API_VERSION >= 3",
            sdkVersion >= 3
        )

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
        assumeTrue(
            "Requires Versions.API_VERSION >= 3",
            sdkVersion >= 3
        )

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
        assumeTrue(
            "Requires Versions.API_VERSION >= 3",
            sdkVersion >= 3
        )

        val handler = CatchingSdkActivityHandler()

        val testSdk = loadedSdk.loadTestSdk()
        val token = testSdk.registerSdkSandboxActivityHandler(handler)
        testSdk.unregisterSdkSandboxActivityHandler(handler)

        assertThat(controller.sdkActivityHandlers[token]).isNull()
    }

    internal class TestClassLoaderFactory(
        private val testStorage: TestLocalSdkStorage
    ) : SdkLoader.ClassLoaderFactory {
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

    companion object {

        /**
         * Create test params for each previously released [Versions.API_VERSION] + current one.
         * Each released version must have test-sdk named as "vX" (where X is version to test).
         * These TestSDKs should be registered in RuntimeEnabledSdkTable.xml and be compatible with
         * [TestSdkWrapper].
         */
        @Parameterized.Parameters(name = "sdk: {0}, version: {1}")
        @JvmStatic
        fun params(): List<Array<Any>> = buildList {
            for (apiVersion in 1..Versions.API_VERSION) {
                if (apiVersion == 3) {
                    continue // V3 was released as V4 (original release postponed)
                }
                add(
                    arrayOf(
                        "v$apiVersion",
                        apiVersion,
                    )
                )
            }

            add(
                arrayOf(
                    "current",
                    Versions.API_VERSION
                )
            )
        }

        private fun loadTestSdkFromAssets(
            sdkConfig: LocalSdkConfig,
            controller: TestStubController
        ): LocalSdkProvider {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val testStorage = TestLocalSdkStorage(
                context,
                rootFolder = File(context.cacheDir, "LocalSdkTest")
            )
            val sdkLoader = SdkLoader(
                TestClassLoaderFactory(testStorage),
                context,
                object : SdkLoader.ControllerFactory {
                    override fun createControllerFor(sdkConfig: LocalSdkConfig) = controller
                }
            )
            return sdkLoader.loadSdk(sdkConfig)
        }
    }

    internal class TestStubController : SdkSandboxControllerCompat.SandboxControllerImpl {

        var sandboxedSdksResult: List<SandboxedSdkCompat> = emptyList()
        var appOwnedSdksResult: List<AppOwnedSdkSandboxInterfaceCompat> = emptyList()
        var sdkActivityHandlers: MutableMap<IBinder, SdkSandboxActivityHandlerCompat> =
            mutableMapOf()

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
    }
}
