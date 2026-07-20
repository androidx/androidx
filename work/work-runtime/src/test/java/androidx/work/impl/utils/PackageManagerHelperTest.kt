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

package androidx.work.impl.utils

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, minSdk = 23)
class PackageManagerHelperTest {

    private lateinit var context: Context
    private lateinit var pm: PackageManager

    @Before
    fun setUp() {
        context = getApplicationContext()
        pm = context.packageManager
    }

    @After
    fun tearDown() {
        clearManifestDefaultCache()
    }

    @Test
    fun testServiceEnabledByDefaultInManifest_skipsEnablementWhenSettingTrue() {
        val componentName = ComponentName(context, TestService::class.java.name)
        val info = shadowOf(pm).addServiceIfNotPresent(componentName)
        info.enabled = true

        assertThat(pm.getComponentEnabledSetting(componentName))
            .isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)

        setServiceEnabled(context, TestService::class.java, true)

        // Since it is enabled by default in manifest, setting it to true should skip PMS write
        assertThat(pm.getComponentEnabledSetting(componentName))
            .isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
    }

    @Test
    fun testServiceEnabledByDefaultInManifest_disablesWhenSettingFalse() {
        val componentName = ComponentName(context, TestService::class.java.name)
        val info = shadowOf(pm).addServiceIfNotPresent(componentName)
        info.enabled = true

        setServiceEnabled(context, TestService::class.java, false)

        assertThat(pm.getComponentEnabledSetting(componentName))
            .isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
    }

    @Test
    fun testReceiverDisabledByDefaultInManifest_enablesWhenSettingTrue() {
        val componentName = ComponentName(context, TestReceiver::class.java.name)
        val info = shadowOf(pm).addReceiverIfNotPresent(componentName)
        info.enabled = false

        assertThat(pm.getComponentEnabledSetting(componentName))
            .isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)

        setReceiverEnabled(context, TestReceiver::class.java, true)

        // Since it is disabled by default in manifest, setting it to true must write to PMS
        assertThat(pm.getComponentEnabledSetting(componentName))
            .isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
    }

    @Test
    fun testReceiverDisabledByDefaultInManifest_skipsWhenSettingFalse() {
        val componentName = ComponentName(context, TestReceiver::class.java.name)
        val info = shadowOf(pm).addReceiverIfNotPresent(componentName)
        info.enabled = false

        setReceiverEnabled(context, TestReceiver::class.java, false)

        // Since it is disabled by default in manifest, setting it to false should skip PMS write
        assertThat(pm.getComponentEnabledSetting(componentName))
            .isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
    }

    @Test
    fun testCaching_avoidsRepeatedManifestLookups() {
        val componentName = ComponentName(context, TestService::class.java.name)
        val info = shadowOf(pm).addServiceIfNotPresent(componentName)
        info.enabled = true

        setServiceEnabled(context, TestService::class.java, true)

        // Mutate manifest info in Robolectric shadow to false; if cache works, it should still use
        // true
        info.enabled = false
        setServiceEnabled(context, TestService::class.java, true)
        assertThat(pm.getComponentEnabledSetting(componentName))
            .isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
    }

    class TestService : Service() {
        override fun onBind(intent: Intent?): IBinder? = null
    }

    class TestReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {}
    }
}
