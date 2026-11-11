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

package androidx.work

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Config.TARGET_SDK])
class ConfigurationTest {

    @Test
    fun jobSchedulerPersistenceEnabled_defaultsToFalse() {
        val configuration = Configuration.Builder().build()
        assertThat(configuration.isJobSchedulerPersistenceEnabled()).isFalse()
    }

    @Test
    fun jobSchedulerPersistenceEnabled_preservedByCopyConstructor() {
        val original = Configuration.Builder().setJobSchedulerPersistenceEnabled(true).build()
        val copy = Configuration.Builder(original).build()
        assertThat(copy.isJobSchedulerPersistenceEnabled()).isTrue()
    }

    @Test
    fun isJobSchedulerPersistenceEffective_falseWhenDisabled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context as Application)
            .grantPermissions(Manifest.permission.RECEIVE_BOOT_COMPLETED)
        val configuration = Configuration.Builder().setJobSchedulerPersistenceEnabled(false).build()
        assertThat(configuration.isJobSchedulerPersistenceEffective(context)).isFalse()
    }

    @Test
    fun isJobSchedulerPersistenceEffective_trueWhenEnabledWithBootPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context as Application)
            .grantPermissions(Manifest.permission.RECEIVE_BOOT_COMPLETED)
        val configuration = Configuration.Builder().setJobSchedulerPersistenceEnabled(true).build()
        assertThat(configuration.isJobSchedulerPersistenceEffective(context)).isTrue()
    }

    @Test
    fun isJobSchedulerPersistenceEffective_falseWithoutBootPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration.Builder().setJobSchedulerPersistenceEnabled(true).build()
        assertThat(configuration.isJobSchedulerPersistenceEffective(context)).isFalse()
    }

    @Test
    fun isJobSchedulerPersistenceEffective_cachesPermissionCheck() {
        val mockContext = mock(Context::class.java)
        `when`(
                mockContext.checkPermission(
                    eq(Manifest.permission.RECEIVE_BOOT_COMPLETED),
                    anyInt(),
                    anyInt(),
                )
            )
            .thenReturn(PackageManager.PERMISSION_DENIED)

        val configuration = Configuration.Builder().setJobSchedulerPersistenceEnabled(true).build()
        assertThat(configuration.isJobSchedulerPersistenceEffective(mockContext)).isFalse()
        assertThat(configuration.isJobSchedulerPersistenceEffective(mockContext)).isFalse()

        verify(mockContext, times(1))
            .checkPermission(eq(Manifest.permission.RECEIVE_BOOT_COMPLETED), anyInt(), anyInt())
    }
}
