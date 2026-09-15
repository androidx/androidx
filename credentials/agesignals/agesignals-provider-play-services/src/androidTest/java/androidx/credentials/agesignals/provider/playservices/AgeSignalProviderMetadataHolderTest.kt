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

package androidx.credentials.agesignals.provider.playservices

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.credentials.agesignals.AgeSignalProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AgeSignalProviderMetadataHolderTest {

    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    @Suppress("deprecation")
    fun metadataHolderService_isDeclaredInManifestAndInstantiatesProviderViaReflection() {
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_META_DATA or PackageManager.GET_SERVICES,
            )

        val serviceInfo =
            packageInfo.services?.firstOrNull {
                it.name == AgeSignalProviderMetadataHolder::class.java.name
            }

        assertThat(serviceInfo).isNotNull()
        assertThat(serviceInfo!!.exported).isFalse()
        assertThat(serviceInfo.enabled).isTrue()

        val providerClassName =
            serviceInfo.metaData?.getString(
                "androidx.credentials.agesignals.AGE_SIGNAL_PROVIDER_KEY"
            )
        assertThat(providerClassName).isEqualTo(PlayServicesAgeSignalProvider::class.java.name)

        val providerClass = Class.forName(providerClassName!!)
        val instance =
            providerClass.getConstructor(Context::class.java).newInstance(context)
                as AgeSignalProvider
        assertThat(instance).isInstanceOf(PlayServicesAgeSignalProvider::class.java)
    }

    @Test
    fun onBind_returnsLocalBinderWithServiceInstance() {
        val holder = AgeSignalProviderMetadataHolder()
        val binder = holder.onBind(Intent()) as AgeSignalProviderMetadataHolder.LocalBinder

        assertThat(binder.getService()).isSameInstanceAs(holder)
    }
}
