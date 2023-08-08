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
package androidx.privacysandbox.sdkruntime.core

import android.app.sdksandbox.LoadSdkException
import android.content.Context
import android.os.Binder
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.ext.SdkExtensions.AD_SERVICES
import android.view.View
import androidx.annotation.RequiresExtension
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.reflect.KClass
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@SmallTest
@RunWith(AndroidJUnit4::class)
// TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
@RequiresExtension(extension = AD_SERVICES, version = 4)
@SdkSuppress(minSdkVersion = TIRAMISU)
class SandboxedSdkProviderAdapterTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        assumeTrue("Requires Sandbox API available", isSandboxApiAvailable())
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testAdapterGetCompatClassNameFromAsset() {
        val expectedClassName = context.assets
            .open("SandboxedSdkProviderCompatClassName.txt")
            .use { inputStream ->
                inputStream.bufferedReader().readLine()
            }

        val adapter = SandboxedSdkProviderAdapter()
        adapter.attachContext(context)

        adapter.onLoadSdk(Bundle())

        val delegate = adapter.extractDelegate<SandboxedSdkProviderCompat>()
        assertThat(delegate.javaClass.name)
            .isEqualTo(expectedClassName)
    }

    @Test
    fun onLoadSdk_shouldInstantiateDelegateAndAttachContext() {
        val adapter = createAdapterFor(TestOnLoadReturnResultSdkProvider::class)

        adapter.onLoadSdk(Bundle())

        val delegate = adapter.extractDelegate<TestOnLoadReturnResultSdkProvider>()
        assertThat(delegate.context)
            .isSameInstanceAs(context)
    }

    @Test
    fun onLoadSdk_shouldDelegateToCompatClassAndReturnResult() {
        val adapter = createAdapterFor(TestOnLoadReturnResultSdkProvider::class)
        val params = Bundle()

        val result = adapter.onLoadSdk(params)

        val delegate = adapter.extractDelegate<TestOnLoadReturnResultSdkProvider>()
        assertThat(delegate.mLastOnLoadSdkBundle)
            .isSameInstanceAs(params)
        assertThat(result.getInterface())
            .isEqualTo(delegate.mResult.getInterface())
    }

    @Test
    fun loadSdk_shouldRethrowExceptionFromCompatClass() {
        val adapter = createAdapterFor(TestOnLoadThrowSdkProvider::class)

        val ex = assertThrows(LoadSdkException::class.java) {
            adapter.onLoadSdk(Bundle())
        }

        val delegate = adapter.extractDelegate<TestOnLoadThrowSdkProvider>()
        assertThat(ex.cause)
            .isSameInstanceAs(delegate.mError.cause)
        assertThat(ex.extraInformation)
            .isSameInstanceAs(delegate.mError.extraInformation)
    }

    @Test
    fun loadSdk_shouldThrowIfCompatClassNotExists() {
        val adapter = createAdapterFor("NOTEXISTS")

        assertThrows(ClassNotFoundException::class.java) {
            adapter.onLoadSdk(Bundle())
        }
    }

    @Test
    fun beforeUnloadSdk_shouldDelegateToCompatProvider() {
        val adapter = createAdapterFor(TestOnBeforeUnloadDelegateSdkProvider::class)

        adapter.beforeUnloadSdk()

        val delegate = adapter.extractDelegate<TestOnBeforeUnloadDelegateSdkProvider>()
        assertThat(delegate.mBeforeUnloadSdkCalled)
            .isTrue()
    }

    @Test
    fun getView_shouldDelegateToCompatProviderAndReturnResult() {
        val adapter = createAdapterFor(TestGetViewSdkProvider::class)
        val windowContext = mock(Context::class.java)
        val params = Bundle()
        val width = 1
        val height = 2

        val result = adapter.getView(windowContext, params, width, height)

        val delegate = adapter.extractDelegate<TestGetViewSdkProvider>()
        assertThat(result)
            .isSameInstanceAs(delegate.mView)
        assertThat(delegate.mLastWindowContext)
            .isSameInstanceAs(windowContext)
        assertThat(delegate.mLastParams)
            .isSameInstanceAs(params)
        assertThat(delegate.mLastWidth)
            .isSameInstanceAs(width)
        assertThat(delegate.mLastHeigh)
            .isSameInstanceAs(height)
    }

    private fun createAdapterFor(
        clazz: KClass<out SandboxedSdkProviderCompat>
    ): SandboxedSdkProviderAdapter = createAdapterFor(clazz.java.name)

    private fun createAdapterFor(delegateClassName: String): SandboxedSdkProviderAdapter {
        val adapter = SandboxedSdkProviderAdapter(
            object : SandboxedSdkProviderAdapter.CompatClassNameProvider {
                override fun getCompatProviderClassName(context: Context): String {
                    return delegateClassName
                }
            })
        adapter.attachContext(context)
        return adapter
    }

    private inline fun <reified T : SandboxedSdkProviderCompat>
        SandboxedSdkProviderAdapter.extractDelegate(): T = delegate as T

    class TestOnLoadReturnResultSdkProvider : SandboxedSdkProviderCompat() {
        var mResult = SandboxedSdkCompat(Binder())
        var mLastOnLoadSdkBundle: Bundle? = null

        override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
            mLastOnLoadSdkBundle = params
            return mResult
        }

        override fun getView(
            windowContext: Context,
            params: Bundle,
            width: Int,
            height: Int
        ): View {
            throw RuntimeException("Not implemented")
        }
    }

    class TestOnLoadThrowSdkProvider : SandboxedSdkProviderCompat() {
        var mError = LoadSdkCompatException(RuntimeException(), Bundle())

        @Throws(LoadSdkCompatException::class)
        override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
            throw mError
        }

        override fun getView(
            windowContext: Context,
            params: Bundle,
            width: Int,
            height: Int
        ): View {
            throw RuntimeException("Stub!")
        }
    }

    class TestOnBeforeUnloadDelegateSdkProvider : SandboxedSdkProviderCompat() {
        var mBeforeUnloadSdkCalled = false

        override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
            throw RuntimeException("Not implemented")
        }

        override fun beforeUnloadSdk() {
            mBeforeUnloadSdkCalled = true
        }

        override fun getView(
            windowContext: Context,
            params: Bundle,
            width: Int,
            height: Int
        ): View {
            throw RuntimeException("Not implemented")
        }
    }

    class TestGetViewSdkProvider : SandboxedSdkProviderCompat() {
        val mView: View = mock(View::class.java)

        var mLastWindowContext: Context? = null
        var mLastParams: Bundle? = null
        var mLastWidth = 0
        var mLastHeigh = 0

        override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
            throw RuntimeException("Not implemented")
        }

        override fun getView(
            windowContext: Context,
            params: Bundle,
            width: Int,
            height: Int
        ): View {
            mLastWindowContext = windowContext
            mLastParams = params
            mLastWidth = width
            mLastHeigh = height

            return mView
        }
    }

    private fun isSandboxApiAvailable() =
        AdServicesInfo.isAtLeastV4()
}
