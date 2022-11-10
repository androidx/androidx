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
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.privacysandbox.sdkruntime.client.loader.impl.SandboxedSdkContextCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkProviderCompat
import androidx.privacysandbox.sdkruntime.core.Versions
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import dalvik.system.InMemoryDexClassLoader
import java.nio.ByteBuffer
import java.nio.channels.Channels
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
internal class LocalSdkTest(
    @Suppress("unused") private val sdkPath: String,
    @Suppress("unused") private val sdkVersion: Int,
    private val loadedSdk: LocalSdk
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

    class CurrentVersionProviderLoadTest : SandboxedSdkProviderCompat() {
        @JvmField
        val onLoadSdkBinder = Binder()

        @JvmField
        var lastOnLoadSdkParams: Bundle? = null

        @JvmField
        var isBeforeUnloadSdkCalled = false

        @Throws(LoadSdkCompatException::class)
        override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
            lastOnLoadSdkParams = params
            if (params.getBoolean("needFail", false)) {
                throw LoadSdkCompatException(RuntimeException(), params)
            }
            return SandboxedSdkCompat.create(onLoadSdkBinder)
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

    @RequiresApi(Build.VERSION_CODES.O)
    internal class TestClassLoaderFactory : SdkLoader.ClassLoaderFactory {
        override fun loadSdk(sdkConfig: LocalSdkConfig, parent: ClassLoader): ClassLoader {
            val assetManager = ApplicationProvider.getApplicationContext<Context>().assets
            assetManager.open(sdkConfig.dexPaths[0]).use { inputStream ->
                val byteBuffer = ByteBuffer.allocate(inputStream.available())
                Channels.newChannel(inputStream).read(byteBuffer)
                byteBuffer.flip()
                return InMemoryDexClassLoader(
                    byteBuffer,
                    parent
                )
            }
        }
    }

    internal class TestSdkInfo internal constructor(
        val apiVersion: Int,
        dexPath: String,
        sdkProviderClass: String
    ) {
        val mLocalSdkConfig: LocalSdkConfig = LocalSdkConfig(
            listOf(dexPath), javaResourcesRoot = null,
            sdkProviderClass
        )
    }

    companion object {
        private val SDKS = arrayOf(
            TestSdkInfo(
                1,
                "RuntimeEnabledSdks/V1/classes.dex",
                "androidx.privacysandbox.sdkruntime.test.v1.CompatProvider"
            )
        )

        @Parameterized.Parameters(name = "sdk: {0}, version: {1}")
        @JvmStatic
        fun params(): List<Array<Any>> = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(SDKS.size).isEqualTo(Versions.API_VERSION)

                for (i in SDKS.indices) {
                    val sdk = SDKS[i]
                    assertThat(sdk.apiVersion).isEqualTo(i + 1)

                    val loadedSdk = loadTestSdkFromAssets(sdk)
                    assertThat(loadedSdk.extractApiVersion())
                        .isEqualTo(sdk.apiVersion)

                    add(
                        arrayOf(
                            sdk.mLocalSdkConfig.dexPaths[0],
                            sdk.apiVersion,
                            loadedSdk
                        )
                    )
                }
            }

            // add SDK loaded from test sources
            add(
                arrayOf(
                    "BuiltFromSource",
                    Versions.API_VERSION,
                    loadTestSdkFromSource(),
                )
            )
        }

        private fun loadTestSdkFromSource(): LocalSdk {
            val sdkLoader = SdkLoader(
                object : SdkLoader.ClassLoaderFactory {
                    override fun loadSdk(
                        sdkConfig: LocalSdkConfig,
                        parent: ClassLoader
                    ): ClassLoader = javaClass.classLoader!!
                },
                ApplicationProvider.getApplicationContext()
            )

            return sdkLoader.loadSdk(
                LocalSdkConfig(
                    emptyList(),
                    javaResourcesRoot = null,
                    CurrentVersionProviderLoadTest::class.java.name
                )
            )
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun loadTestSdkFromAssets(sdk: TestSdkInfo): LocalSdk {
            val sdkLoader = SdkLoader(
                TestClassLoaderFactory(),
                ApplicationProvider.getApplicationContext()
            )
            return sdkLoader.loadSdk(sdk.mLocalSdkConfig)
        }
    }
}