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
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat.Companion.create
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`

@SmallTest
@RunWith(AndroidJUnit4::class)
// TODO(b/249981547) Update check when prebuilt with SdkSandbox APIs dropped to T
@SdkSuppress(minSdkVersion = UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
@RequiresApi(api = UPSIDE_DOWN_CAKE)
class SandboxedSdkProviderAdapterTest {

    private lateinit var mContext: Context
    private lateinit var mPackageManager: PackageManager
    private lateinit var providerUnderTest: SandboxedSdkProviderAdapter

    @Before
    fun setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext<Context>())
        mPackageManager = mock(PackageManager::class.java)
        `when`(mContext.packageManager).thenReturn(mPackageManager)
        providerUnderTest = SandboxedSdkProviderAdapter()
        providerUnderTest.attachContext(mContext)
    }

    @Test
    @Throws(LoadSdkException::class)
    fun onLoadSdk_shouldInstantiateDelegateAndAttachContext() {
        setupDelegate(TestOnLoadReturnResultSdkProvider::class.java)

        providerUnderTest.onLoadSdk(Bundle())

        val delegate = extractDelegate(TestOnLoadReturnResultSdkProvider::class.java)
        assertThat(delegate.context)
            .isSameInstanceAs(mContext)
    }

    @Test
    @Throws(LoadSdkException::class)
    fun onLoadSdk_shouldDelegateToCompatClassAndReturnResult() {
        setupDelegate(TestOnLoadReturnResultSdkProvider::class.java)
        val params = Bundle()

        val result = providerUnderTest.onLoadSdk(params)

        val delegate = extractDelegate(TestOnLoadReturnResultSdkProvider::class.java)
        assertThat(delegate.mLastOnLoadSdkBundle)
            .isSameInstanceAs(params)
        assertThat(result)
            .isEqualTo(delegate.mResult.toSandboxedSdk())
    }

    @Test
    fun loadSdk_shouldRethrowExceptionFromCompatClass() {
        setupDelegate(TestOnLoadThrowSdkProvider::class.java)

        val ex = assertThrows(LoadSdkException::class.java) {
            providerUnderTest.onLoadSdk(Bundle())
        }

        val delegate = extractDelegate(TestOnLoadThrowSdkProvider::class.java)
        assertThat(ex.cause)
            .isSameInstanceAs(delegate.mError.cause)
        assertThat(ex.extraInformation)
            .isSameInstanceAs(delegate.mError.extraInformation)
    }

    @Test
    fun loadSdk_shouldThrowIfCompatClassNotExists() {
        setupDelegateClassname("NOTEXISTS")

        assertThrows(RuntimeException::class.java) {
            providerUnderTest.onLoadSdk(Bundle())
        }
    }

    @Test
    fun beforeUnloadSdk_shouldDelegateToCompatProvider() {
        setupDelegate(TestOnBeforeUnloadDelegateSdkProvider::class.java)

        providerUnderTest.beforeUnloadSdk()

        val delegate = extractDelegate(TestOnBeforeUnloadDelegateSdkProvider::class.java)
        assertThat(delegate.mBeforeUnloadSdkCalled)
            .isTrue()
    }

    @Test
    fun getView_shouldDelegateToCompatProviderAndReturnResult() {
        setupDelegate(TestGetViewSdkProvider::class.java)
        val windowContext = mock(Context::class.java)
        val params = Bundle()
        val width = 1
        val height = 2

        val result = providerUnderTest.getView(windowContext, params, width, height)

        val delegate = extractDelegate(TestGetViewSdkProvider::class.java)
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

    private fun setupDelegate(clazz: Class<out SandboxedSdkProviderCompat>) {
        setupDelegateClassname(clazz.name)
    }

    private fun <T : SandboxedSdkProviderCompat?> extractDelegate(clazz: Class<T>): T {
        return clazz.cast(providerUnderTest.delegate)!!
    }

    private fun setupDelegateClassname(className: String) {
        val property = mock(PackageManager.Property::class.java)
        try {
            `when`(
                mPackageManager.getProperty(
                    eq("android.sdksandbox.PROPERTY_COMPAT_SDK_PROVIDER_CLASS_NAME"),
                    any(String::class.java)
                )
            ).thenReturn(property)
        } catch (ignored: PackageManager.NameNotFoundException) {
        }
        `when`(property.string)
            .thenReturn(className)
    }

    class TestOnLoadReturnResultSdkProvider : SandboxedSdkProviderCompat() {
        var mResult = create(Binder())
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
}