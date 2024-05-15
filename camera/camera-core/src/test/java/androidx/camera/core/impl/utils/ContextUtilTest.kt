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
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ContextUtilTest {
    companion object {
        const val ATTRIBUTION_TAG = "attributionTag"
        const val DEVICE_ID = 2
    }

    @Test
    fun testGetApplicationContext() {
        val appContext = FakeAppContext("application")
        val context = FakeContext("non-application", appContext)
        val resultContext = ContextUtil.getApplicationContext(context) as FakeContext
        // Ensures the result context is created from application context.
        assertThat(resultContext.getTag()).isEqualTo(appContext.getTag())
    }

    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testGetApplicationContext_deviceIdAndAttributionTag() {
        val appContext = FakeAppContext("application")
        val context = FakeContext(
            "non-application",
            baseContext = appContext,
            deviceId = DEVICE_ID,
            attributionTag = ATTRIBUTION_TAG
        )
        val resultContext = ContextUtil.getApplicationContext(context) as FakeContext
        assertThat(resultContext.attributionTag).isEqualTo(ATTRIBUTION_TAG)
        assertThat(resultContext.deviceId).isEqualTo(DEVICE_ID)
        // Ensures the result context is created from application context.
        assertThat(resultContext.getTag()).isEqualTo(appContext.getTag())
    }

    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testGetApplicationContext_deviceId() {
        val appContext = FakeAppContext("application")
        val context = FakeContext(
            "non-application",
            baseContext = appContext,
            deviceId = DEVICE_ID,
        )
        val resultContext = ContextUtil.getApplicationContext(context) as FakeContext
        assertThat(resultContext.deviceId).isEqualTo(DEVICE_ID)
        assertThat(resultContext.attributionTag).isEqualTo(null)
        // Ensures the result context is created from application context.
        assertThat(resultContext.getTag()).isEqualTo(appContext.getTag())
    }

    @Config(minSdk = Build.VERSION_CODES.R)
    @Test
    fun testGetApplicationContext_attributionTag() {
        val appContext = FakeAppContext("application")
        val context = FakeContext(
            "non-application",
            baseContext = appContext,
            attributionTag = ATTRIBUTION_TAG,
        )
        val resultContext = ContextUtil.getApplicationContext(context) as FakeContext
        assertThat(resultContext.attributionTag).isEqualTo(ATTRIBUTION_TAG)
        // Ensures the result context is created from application context.
        assertThat(resultContext.getTag()).isEqualTo(appContext.getTag())
    }

    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testGetBaseContext_deviceIdAndAttributionTag() {
        val baseContext = FakeContext("baseContext")
        val context = FakeContext(
            "non-baseContext",
            baseContext = baseContext,
            deviceId = DEVICE_ID,
            attributionTag = ATTRIBUTION_TAG
        )
        val resultContext = ContextUtil.getBaseContext(context) as FakeContext
        assertThat(resultContext.attributionTag).isEqualTo(ATTRIBUTION_TAG)
        assertThat(resultContext.deviceId).isEqualTo(DEVICE_ID)
        // Ensures the result context is created from base context.
        assertThat(resultContext.getTag()).isEqualTo(baseContext.getTag())
    }

    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testGetBaseContext_deviceId() {
        val baseContext = FakeContext("baseContext")
        val context = FakeContext(
            "non-baseContext",
            baseContext = baseContext,
            deviceId = DEVICE_ID,
        )
        val resultContext = ContextUtil.getBaseContext(context) as FakeContext
        assertThat(resultContext.deviceId).isEqualTo(DEVICE_ID)
        assertThat(resultContext.attributionTag).isEqualTo(null)
        // Ensures the result context is created from base context.
        assertThat(resultContext.getTag()).isEqualTo(baseContext.getTag())
    }

    @Config(minSdk = Build.VERSION_CODES.R)
    @Test
    fun testGetBaseContext_attributionTag() {
        val baseContext = FakeContext("baseContext")
        val context = FakeContext(
            "non-baseContext",
            baseContext = baseContext,
            attributionTag = ATTRIBUTION_TAG,
        )
        val resultContext = ContextUtil.getBaseContext(context) as FakeContext
        assertThat(resultContext.attributionTag).isEqualTo(ATTRIBUTION_TAG)
        // Ensures the result context is created from base context.
        assertThat(resultContext.getTag()).isEqualTo(baseContext.getTag())
    }

    @Test
    fun testGetApplicationFromContext() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        val context1 = FakeContext(baseContext = application)
        val context2 = FakeContext(baseContext = context1)
        val resultContext = ContextUtil.getApplicationFromContext(context2)
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
        private val attributionTag: String? = null
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
     * Create a application context that has the capability of FakeContext and
     * getApplicationContext returns itself.
     */
    class FakeAppContext(
        /*
           tag is used to identify the source Context used to create new Context using
           createDeviceContext or createAttributionContext
         */
        tag: String? = null,
        deviceId: Int = Context.DEVICE_ID_DEFAULT,
        attributionTag: String? = null
    ) : FakeContext(tag, ApplicationProvider.getApplicationContext(), deviceId, attributionTag) {
        override fun getApplicationContext(): Context {
            return this
        }
    }
}
