/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.impl.utils

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class ContextUtilTest {
    companion object {
        const val ATTRIBUTION_TAG = "attributionTag"
        const val ATTRIBUTION_TAG_2 = "attributionTag2"
        const val VIRTUAL_DEVICE_ID = 2
        const val VIRTUAL_DEVICE_ID_2 = 3
    }

    @Test
    fun testGetPersistentApplicationContext() {
        val appContext = FakeAppContext("application")
        val context = FakeContext("non-application", appContext)
        val resultContext = ContextUtil.getPersistentApplicationContext(context) as FakeContext
        // Ensures the result context is created from application context.
        assertThat(resultContext.getTag()).isEqualTo(appContext.getTag())
    }

    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testGetPersistentApplicationContext_deviceIdAndAttributionTag() {
        val appContext = FakeAppContext("application")
        val context =
            FakeContext(
                "non-application",
                baseContext = appContext,
                deviceId = VIRTUAL_DEVICE_ID,
                attributionTag = ATTRIBUTION_TAG,
            )
        val resultContext = ContextUtil.getPersistentApplicationContext(context) as FakeContext
        assertThat(resultContext.attributionTag).isEqualTo(ATTRIBUTION_TAG)
        assertThat(resultContext.deviceId).isEqualTo(VIRTUAL_DEVICE_ID)
        // Ensures the result context is created from application context.
        assertThat(resultContext.getTag()).isEqualTo(appContext.getTag())
    }

    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testGetPersistentApplicationContext_virtualDeviceId() {
        val appContext = FakeAppContext("application")
        val context =
            FakeContext("non-application", baseContext = appContext, deviceId = VIRTUAL_DEVICE_ID)
        val resultContext = ContextUtil.getPersistentApplicationContext(context) as FakeContext
        assertThat(resultContext.deviceId).isEqualTo(VIRTUAL_DEVICE_ID)
        assertThat(resultContext.attributionTag).isEqualTo(null)
        // Ensures the result context is created from application context.
        assertThat(resultContext.getTag()).isEqualTo(appContext.getTag())
    }

    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testGetPersistentApplicationContext_defaultDeviceId() {
        val appContext = FakeAppContext("application", deviceId = VIRTUAL_DEVICE_ID)
        val context =
            FakeContext(
                "non-application",
                baseContext = appContext,
                deviceId = Context.DEVICE_ID_DEFAULT,
            )
        val resultContext = ContextUtil.getPersistentApplicationContext(context) as FakeContext
        assertThat(resultContext.deviceId).isEqualTo(Context.DEVICE_ID_DEFAULT)
        assertThat(resultContext.attributionTag).isEqualTo(null)
        // Ensures the result context is created from application context.
        assertThat(resultContext.getTag()).isEqualTo(appContext.getTag())
    }

    @Config(minSdk = Build.VERSION_CODES.R)
    @Test
    fun testGetPersistentApplicationContext_attributionTag() {
        val appContext = FakeAppContext("application")
        val context =
            FakeContext(
                "non-application",
                baseContext = appContext,
                attributionTag = ATTRIBUTION_TAG,
            )
        val resultContext = ContextUtil.getPersistentApplicationContext(context) as FakeContext
        assertThat(resultContext.attributionTag).isEqualTo(ATTRIBUTION_TAG)
        // Ensures the result context is created from application context.
        assertThat(resultContext.getTag()).isEqualTo(appContext.getTag())
    }

    @Config(minSdk = Build.VERSION_CODES.R)
    @Test
    fun testGetPersistentApplicationContext_appContextHasDifferentAttributionTag() {
        val appContext = FakeAppContext("application", attributionTag = ATTRIBUTION_TAG)
        val context =
            FakeContext("non-application", baseContext = appContext, attributionTag = null)
        val resultContext = ContextUtil.getPersistentApplicationContext(context) as FakeContext
        assertThat(resultContext.attributionTag).isNull()
        // Ensures the result context is created from application context.
        assertThat(resultContext.getTag()).isEqualTo(appContext.getTag())
    }

    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testGetPersistentApplicationContext_sameContextDeviceIdAndTag_returnsSameInstance() {
        val appContext = FakeAppContext("application")
        val context =
            FakeContext(
                "non-application",
                baseContext = appContext,
                deviceId = VIRTUAL_DEVICE_ID,
                attributionTag = ATTRIBUTION_TAG,
            )
        val resultContext1 = ContextUtil.getPersistentApplicationContext(context)
        val resultContext2 = ContextUtil.getPersistentApplicationContext(context)
        assertThat(resultContext1).isSameInstanceAs(resultContext2)
    }

    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testGetPersistentApplicationContext_differentContext_returnsDifferentInstances() {
        val appContext1 = FakeAppContext("application1")
        val context1 =
            FakeContext("non-application1", baseContext = appContext1, deviceId = VIRTUAL_DEVICE_ID)
        val appContext2 = FakeAppContext("application2")
        val context2 =
            FakeContext("non-application2", baseContext = appContext2, deviceId = VIRTUAL_DEVICE_ID)
        val resultContext1 = ContextUtil.getPersistentApplicationContext(context1)
        val resultContext2 = ContextUtil.getPersistentApplicationContext(context2)
        assertThat(resultContext1).isNotSameInstanceAs(resultContext2)
    }

    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testGetPersistentApplicationContext_differentDeviceId_returnsDifferentInstances() {
        val appContext = FakeAppContext("application")
        val context1 =
            FakeContext("non-application", baseContext = appContext, deviceId = VIRTUAL_DEVICE_ID)
        val context2 =
            FakeContext("non-application", baseContext = appContext, deviceId = VIRTUAL_DEVICE_ID_2)
        val resultContext1 = ContextUtil.getPersistentApplicationContext(context1)
        val resultContext2 = ContextUtil.getPersistentApplicationContext(context2)
        assertThat(resultContext1).isNotSameInstanceAs(resultContext2)
    }

    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testGetPersistentApplicationContext_differentAttributionTag_returnsDifferentInstances() {
        val appContext = FakeAppContext("application")
        val context1 =
            FakeContext(
                "non-application",
                baseContext = appContext,
                deviceId = VIRTUAL_DEVICE_ID,
                attributionTag = ATTRIBUTION_TAG,
            )
        val context2 =
            FakeContext(
                "non-application",
                baseContext = appContext,
                deviceId = VIRTUAL_DEVICE_ID,
                attributionTag = ATTRIBUTION_TAG_2,
            )
        val resultContext1 = ContextUtil.getPersistentApplicationContext(context1)
        val resultContext2 = ContextUtil.getPersistentApplicationContext(context2)
        assertThat(resultContext1).isNotSameInstanceAs(resultContext2)
    }

    @Test
    fun testGetApplication() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        val context1 = FakeContext(baseContext = application)
        val context2 = FakeContext(baseContext = context1)
        val resultContext = ContextUtil.getApplication(context2)
        assertThat(resultContext).isSameInstanceAs(application)
    }

    /**
     * Create a fake [Context] that is able to get deviceId and attributionTag and create new
     * [Context] with new device id and attributionTag.
     */
    open class FakeContext(
        /*
          tag is used to identify the source Context used to create new Context using
          createDeviceContext or createAttributionContext
        */
        private val tag: String? = null,
        baseContext: Context = mock(Context::class.java),
        private val deviceId: Int = Context.DEVICE_ID_DEFAULT,
        private val attributionTag: String? = null,
    ) : ContextWrapper(baseContext) {
        override fun getDeviceId(): Int = deviceId

        override fun getAttributionTag(): String? = attributionTag

        override fun createDeviceContext(newDeviceId: Int): Context =
            FakeContext(tag, this, deviceId = newDeviceId, attributionTag = attributionTag)

        override fun createAttributionContext(newAttributionTag: String?): Context =
            FakeContext(tag, this, deviceId = deviceId, attributionTag = newAttributionTag)

        fun getTag(): String? = tag
    }

    /**
     * Create a application context that has the capability of FakeContext and getApplicationContext
     * returns itself.
     */
    class FakeAppContext(
        /*
          tag is used to identify the source Context used to create new Context using
          createDeviceContext or createAttributionContext
        */
        tag: String? = null,
        deviceId: Int = Context.DEVICE_ID_DEFAULT,
        attributionTag: String? = null,
    ) : FakeContext(tag, ApplicationProvider.getApplicationContext(), deviceId, attributionTag) {
        override fun getApplicationContext(): Context {
            return this
        }
    }
}
