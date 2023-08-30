/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.integration.core

import android.Manifest
import android.app.ActivityManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.LifecycleOwnerUtils
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CameraXServiceTest {
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private lateinit var serviceConnection: ServiceConnection
    private lateinit var service: CameraXService

    @Before
    fun setUp() = runBlocking {
        assumeFalse(isBackgroundRestricted())

        service = bindService()

        // Ensure service is started.
        LifecycleOwnerUtils.waitUntilState(service, Lifecycle.State.STARTED)
    }

    @After
    fun tearDown() {
        if (this::service.isInitialized) {
            context.unbindService(serviceConnection)

            // Ensure service is destroyed
            LifecycleOwnerUtils.waitUntilState(service, Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun canStartServiceAsForeground() {
        assertThat(isForegroundService(service)).isTrue()
    }

    private fun createServiceIntent() = Intent(context, CameraXService::class.java)

    private fun isForegroundService(service: Service): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (serviceInfo in manager.getRunningServices(Int.MAX_VALUE)) {
            if (service.javaClass.name == serviceInfo.service.className) {
                return serviceInfo.foreground
            }
        }
        return false
    }

    private suspend fun bindService(): CameraXService {
        val serviceDeferred = CompletableDeferred<CameraXService>()
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as CameraXService.CameraXServiceBinder
                serviceDeferred.complete(binder.service)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
            }
        }
        context.bindService(createServiceIntent(), serviceConnection, Service.BIND_AUTO_CREATE)
        return withTimeout(3000L) {
            serviceDeferred.await()
        }
    }

    private fun isBackgroundRestricted(): Boolean =
        if (Build.VERSION.SDK_INT >= 28) activityManager.isBackgroundRestricted else false
}
