/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.projected

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.xr.projected.ProjectedServiceBinding.ACTION_BIND
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@RunWith(RobolectricTestRunner::class)
class ProjectedServiceBindingTest {

    private lateinit var mockContext: Context
    private lateinit var mockPackageManager: PackageManager
    private lateinit var mockServiceConnection: ServiceConnection

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPackageManager = mock(PackageManager::class.java)
        mockServiceConnection = mock(ServiceConnection::class.java)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
    }

    @Test
    fun bind_whenOneSystemServiceExists_bindsSuccessfully() {
        // Arrange: Setup package manager to return one system service.
        val resolveInfo = createResolveInfo(SYSTEM_PACKAGE_NAME, SYSTEM_CLASS_NAME)
        val appInfo = createApplicationInfo(isSystemApp = true)
        `when`(
                mockPackageManager.queryIntentServices(
                    any(Intent::class.java),
                    eq(PackageManager.GET_RESOLVED_FILTER),
                )
            )
            .thenReturn(listOf(resolveInfo))
        `when`(mockPackageManager.getApplicationInfo(eq(SYSTEM_PACKAGE_NAME), eq(0)))
            .thenReturn(appInfo)

        // Act
        ProjectedServiceBinding.bind(mockContext, mockServiceConnection)

        // Assert: Verify that bindService is called with the correct intent.
        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(mockContext)
            .bindService(
                intentCaptor.capture(),
                eq(mockServiceConnection),
                eq(Context.BIND_AUTO_CREATE),
            )

        val capturedIntent = intentCaptor.value
        assertThat(capturedIntent.action).isEqualTo(ACTION_BIND)
        assertThat(capturedIntent.component)
            .isEqualTo(ComponentName(SYSTEM_PACKAGE_NAME, SYSTEM_CLASS_NAME))
    }

    @Test
    fun bind_whenNoServiceExists_throwsIllegalStateException() {
        // Arrange: Setup package manager to return no services.
        `when`(
                mockPackageManager.queryIntentServices(
                    any(Intent::class.java),
                    eq(PackageManager.GET_RESOLVED_FILTER),
                )
            )
            .thenReturn(emptyList())

        // Act & Assert
        val exception =
            assertThrows(IllegalStateException::class.java) {
                ProjectedServiceBinding.bind(mockContext, mockServiceConnection)
            }
        assertThat(exception.message)
            .isEqualTo("System doesn't include a service supporting Projected XR devices.")

        verify(mockContext, never()).bindService(any(), any(), anyInt())
    }

    @Test
    fun bind_whenMultipleSystemServicesExist_throwsIllegalStateException() {
        // Arrange: Setup package manager to return two system services.
        val resolveInfo1 = createResolveInfo("pkg1", "cls1")
        val resolveInfo2 = createResolveInfo("pkg2", "cls2")
        val appInfo = createApplicationInfo(isSystemApp = true)
        `when`(
                mockPackageManager.queryIntentServices(
                    any(Intent::class.java),
                    eq(PackageManager.GET_RESOLVED_FILTER),
                )
            )
            .thenReturn(listOf(resolveInfo1, resolveInfo2))
        `when`(mockPackageManager.getApplicationInfo(any(), eq(0))).thenReturn(appInfo)

        // Act & Assert
        val exception =
            assertThrows(IllegalStateException::class.java) {
                ProjectedServiceBinding.bind(mockContext, mockServiceConnection)
            }
        assertThat(exception.message)
            .isEqualTo("More than one system service found for action: $ACTION_BIND.")
        verify(mockContext, never()).bindService(any(), any(), anyInt())
    }

    @Test
    fun bind_whenOnlyNonSystemServiceExists_throwsIllegalStateException() {
        // Arrange: Setup package manager to return one non-system service.
        val resolveInfo = createResolveInfo(USER_PACKAGE_NAME, USER_CLASS_NAME)
        val appInfo = createApplicationInfo(isSystemApp = false)
        `when`(
                mockPackageManager.queryIntentServices(
                    any(Intent::class.java),
                    eq(PackageManager.GET_RESOLVED_FILTER),
                )
            )
            .thenReturn(listOf(resolveInfo))
        `when`(mockPackageManager.getApplicationInfo(eq(USER_PACKAGE_NAME), eq(0)))
            .thenReturn(appInfo)

        // Act & Assert: The non-system app should be filtered out, resulting in no services found.
        val exception =
            assertThrows(IllegalStateException::class.java) {
                ProjectedServiceBinding.bind(mockContext, mockServiceConnection)
            }
        assertThat(exception.message)
            .isEqualTo("System doesn't include a service supporting Projected XR devices.")
        verify(mockContext, never()).bindService(any(), any(), anyInt())
    }

    private fun createResolveInfo(packageName: String, className: String): ResolveInfo {
        val serviceInfo = ServiceInfo()
        serviceInfo.packageName = packageName
        serviceInfo.name = className
        val resolveInfo = ResolveInfo()
        resolveInfo.serviceInfo = serviceInfo
        return resolveInfo
    }

    private fun createApplicationInfo(isSystemApp: Boolean): ApplicationInfo {
        val appInfo = ApplicationInfo()
        if (isSystemApp) {
            appInfo.flags = ApplicationInfo.FLAG_SYSTEM
        }
        return appInfo
    }

    companion object {
        private const val SYSTEM_PACKAGE_NAME = "com.system.service"
        private const val SYSTEM_CLASS_NAME = "com.system.service.ProjectedService"
        private const val USER_PACKAGE_NAME = "com.user.app"
        private const val USER_CLASS_NAME = "com.user.app.SomeService"
    }
}
