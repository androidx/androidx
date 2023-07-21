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
import android.view.View
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.privacysandbox.sdkruntime.client.loader.impl.SandboxedSdkContextCompat
import androidx.privacysandbox.sdkruntime.client.loader.storage.TestLocalSdkStorage
import androidx.privacysandbox.sdkruntime.client.loader.storage.toClassPathString
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkInfo
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkProviderCompat
import androidx.privacysandbox.sdkruntime.core.Versions
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import dalvik.system.BaseDexClassLoader
import java.io.File
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
internal class LocalSdkProviderTest(
    @Suppress("unused") private val sdkPath: String,
    private val sdkVersion: Int,
    private val controller: TestStubController,
    private val loadedSdk: LocalSdkProvider
) {

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

    class CurrentVersionProviderLoadTest : SandboxedSdkProviderCompat() {
        @JvmField
        var onLoadSdkBinder: Binder? = null

        @JvmField
        var lastOnLoadSdkParams: Bundle? = null

        @JvmField
        var isBeforeUnloadSdkCalled = false

        @Throws(LoadSdkCompatException::class)
        override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
            val result = CurrentVersionSdkTest(context!!)
            onLoadSdkBinder = result

            lastOnLoadSdkParams = params
            if (params.getBoolean("needFail", false)) {
                throw LoadSdkCompatException(RuntimeException(), params)
            }
            return SandboxedSdkCompat(result)
        }

        override fun beforeUnloadSdk() {
            isBeforeUnloadSdkCalled = true
        }

        override fun getView(
            windowContext: Context,
            params: Bundle,
            width: Int,
            height: Int
        ): View {
            return View(windowContext)
        }
    }

    @Suppress("unused") // Reflection calls
    internal class CurrentVersionSdkTest(
        private val context: Context
    ) : Binder() {
        fun getSandboxedSdks(): List<SandboxedSdkCompat> =
            SdkSandboxControllerCompat.from(context).getSandboxedSdks()
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

    internal class TestSdkInfo internal constructor(
        val apiVersion: Int,
        dexPath: String,
        sdkProviderClass: String
    ) {
        val localSdkConfig = LocalSdkConfig(
            packageName = "test.$apiVersion.$sdkProviderClass",
            dexPaths = listOf(dexPath),
            entryPoint = sdkProviderClass
        )
    }

    companion object {
        private val SDKS = arrayOf(
            TestSdkInfo(
                1,
                "RuntimeEnabledSdks/V1/classes.dex",
                "androidx.privacysandbox.sdkruntime.test.v1.CompatProvider"
            ),
            TestSdkInfo(
                2,
                "RuntimeEnabledSdks/V2/classes.dex",
                "androidx.privacysandbox.sdkruntime.test.v2.CompatProvider"
            )
        )

        @Parameterized.Parameters(name = "sdk: {0}, version: {1}")
        @JvmStatic
        fun params(): List<Array<Any>> = buildList {
            assertThat(SDKS.size).isEqualTo(Versions.API_VERSION)

            val controller = TestStubController()

            val assetsSdkLoader = createAssetsSdkLoader(controller)
            for (i in SDKS.indices) {
                val sdk = SDKS[i]
                assertThat(sdk.apiVersion).isEqualTo(i + 1)

                val loadedSdk = assetsSdkLoader.loadSdk(sdk.localSdkConfig)
                assertThat(loadedSdk.extractApiVersion())
                    .isEqualTo(sdk.apiVersion)

                add(
                    arrayOf(
                        sdk.localSdkConfig.dexPaths[0],
                        sdk.apiVersion,
                        controller,
                        loadedSdk
                    )
                )
            }

            // add SDK loaded from test sources
            add(
                arrayOf(
                    "BuiltFromSource",
                    Versions.API_VERSION,
                    controller,
                    loadTestSdkFromSource(controller),
                )
            )
        }

        private fun loadTestSdkFromSource(controller: TestStubController): LocalSdkProvider {
            val sdkLoader = SdkLoader(
                object : SdkLoader.ClassLoaderFactory {
                    override fun createClassLoaderFor(
                        sdkConfig: LocalSdkConfig,
                        parent: ClassLoader
                    ): ClassLoader = javaClass.classLoader!!
                },
                ApplicationProvider.getApplicationContext(),
                controller
            )

            return sdkLoader.loadSdk(
                LocalSdkConfig(
                    packageName = "test.CurrentVersionProviderLoadTest",
                    dexPaths = emptyList(),
                    entryPoint = CurrentVersionProviderLoadTest::class.java.name
                )
            )
        }

        private fun createAssetsSdkLoader(controller: TestStubController): SdkLoader {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val testStorage = TestLocalSdkStorage(
                context,
                rootFolder = File(context.cacheDir, "LocalSdkTest")
            )
            return SdkLoader(
                TestClassLoaderFactory(testStorage),
                context,
                controller
            )
        }
    }

    internal class TestStubController : SdkSandboxControllerCompat.SandboxControllerImpl {

        var sandboxedSdksResult: List<SandboxedSdkCompat> = emptyList()

        override fun getSandboxedSdks(): List<SandboxedSdkCompat> {
            return sandboxedSdksResult
        }
    }
}