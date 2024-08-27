/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.platform.client.impl

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Looper.getMainLooper
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.platform.client.impl.ipc.ClientConfiguration
import androidx.health.platform.client.impl.ipc.internal.ConnectionManager
import androidx.health.platform.client.impl.testing.FakeHealthDataService
import androidx.health.platform.client.permission.Permission
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.PermissionProto
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

private const val PROVIDER_PACKAGE_NAME = "com.google.fake.provider"

@RunWith(AndroidJUnit4::class)
class ServiceBackedHealthDataClientTest {
    private lateinit var ahpClient: ServiceBackedHealthDataClient
    private lateinit var fakeAhpServiceStub: FakeHealthDataService

    @Before
    fun setup() {
        val clientConfig =
            ClientConfiguration("FakeAHPProvider", PROVIDER_PACKAGE_NAME, "FakeProvider")
        val connectionManager =
            ConnectionManager(ApplicationProvider.getApplicationContext(), getMainLooper())
        ahpClient =
            ServiceBackedHealthDataClient(
                ApplicationProvider.getApplicationContext(),
                clientConfig,
                connectionManager
            )
        fakeAhpServiceStub = FakeHealthDataService()
        val bindIntent =
            Intent().setPackage(clientConfig.servicePackageName).setAction(clientConfig.bindAction)
        shadowOf(ApplicationProvider.getApplicationContext<Context>() as Application)
            .setComponentNameAndServiceForBindServiceForIntent(
                bindIntent,
                ComponentName(clientConfig.servicePackageName, clientConfig.bindAction),
                fakeAhpServiceStub
            )
        installPackage(ApplicationProvider.getApplicationContext(), PROVIDER_PACKAGE_NAME, true)
        Intents.init()
    }

    @After
    fun teardown() {
        Intents.release()
    }

    @Test
    fun getGrantedPermissions_noPermissionsGranted_expectEmptyGrantedList() {
        val readPermission =
            PermissionProto.Permission.newBuilder()
                .setDataType(DataProto.DataType.newBuilder().setName("HEART_RATE").build())
                .setAccessType(PermissionProto.AccessType.ACCESS_TYPE_READ)
                .build()
        val writePermission =
            PermissionProto.Permission.newBuilder()
                .setDataType(DataProto.DataType.newBuilder().setName("BLOOD_PRESSURE").build())
                .setAccessType(PermissionProto.AccessType.ACCESS_TYPE_WRITE)
                .build()

        val resultFuture = ahpClient.getGrantedPermissions(setOf(readPermission, writePermission))
        shadowOf(getMainLooper()).idle()

        assertSuccess(resultFuture, emptySet())
    }

    @Test
    fun getGrantedPermissions_somePermissionsGranted_expectCorrectGrantedList() {
        val readPermission =
            PermissionProto.Permission.newBuilder()
                .setDataType(DataProto.DataType.newBuilder().setName("HEART_RATE").build())
                .setAccessType(PermissionProto.AccessType.ACCESS_TYPE_READ)
                .build()
        val writePermission =
            PermissionProto.Permission.newBuilder()
                .setDataType(DataProto.DataType.newBuilder().setName("BLOOD_PRESSURE").build())
                .setAccessType(PermissionProto.AccessType.ACCESS_TYPE_WRITE)
                .build()

        fakeAhpServiceStub.addGrantedPermission(Permission(readPermission))
        val resultFuture = ahpClient.getGrantedPermissions(setOf(readPermission, writePermission))
        shadowOf(getMainLooper()).idle()

        val expected = setOf(readPermission)
        assertSuccess(resultFuture, expected)
    }

    @Test
    fun filterGrantedPermissions_somePermissionsGranted_expectCorrectGrantedList() {
        val readPermission =
            PermissionProto.Permission.newBuilder()
                .setPermission(HealthPermission.READ_HEART_RATE)
                .build()
        val writePermission =
            PermissionProto.Permission.newBuilder()
                .setPermission(HealthPermission.WRITE_BLOOD_PRESSURE)
                .build()

        fakeAhpServiceStub.addGrantedPermission(Permission(readPermission))
        val resultFuture = ahpClient.getGrantedPermissions(setOf(readPermission, writePermission))
        shadowOf(getMainLooper()).idle()

        val expected = setOf(readPermission)
        assertSuccess(resultFuture, expected)
    }

    @Test
    fun revokeAllPermissions_success() {
        val resultFuture: ListenableFuture<Unit> = ahpClient.revokeAllPermissions()
        shadowOf(getMainLooper()).idle()

        assertSuccess(resultFuture, Unit)
    }

    @Test
    fun apiCall_passRelevantForegroundFlag() {
        for (importance in
            setOf(IMPORTANCE_FOREGROUND, IMPORTANCE_FOREGROUND_SERVICE, IMPORTANCE_VISIBLE)) {
            setApplicationRunningImportance(importance)
            val resultFuture: ListenableFuture<Unit> = ahpClient.revokeAllPermissions()
            shadowOf(getMainLooper()).idle()
            assertSuccess(resultFuture, Unit)

            assertThat(fakeAhpServiceStub.lastRequestContext?.isInForeground).isTrue()
        }

        for (importance in setOf(IMPORTANCE_GONE, IMPORTANCE_CACHED)) {
            setApplicationRunningImportance(importance)
            val resultFuture: ListenableFuture<Unit> = ahpClient.revokeAllPermissions()
            shadowOf(getMainLooper()).idle()
            assertSuccess(resultFuture, Unit)

            assertThat(fakeAhpServiceStub.lastRequestContext?.isInForeground).isFalse()
        }
    }

    private fun setApplicationRunningImportance(importance: Int) {
        val activityManager =
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(ActivityManager::class.java)
        val runningInfo = ActivityManager.RunningAppProcessInfo()
        runningInfo.importance = importance
        shadowOf(activityManager).setProcesses(listOf(runningInfo))
    }

    // TODO(b/219327543): Add test cases.
    private fun installPackage(context: Context, packageName: String, enabled: Boolean) {
        val packageInfo = PackageInfo()
        packageInfo.packageName = packageName
        packageInfo.applicationInfo = ApplicationInfo()
        packageInfo.applicationInfo!!.enabled = enabled
        val packageManager = context.packageManager
        shadowOf(packageManager).installPackage(packageInfo)
    }
}

private fun <T> assertSuccess(future: ListenableFuture<T>, expectedValue: T) {
    val futureValue = future.get()
    assertThat(futureValue).isEqualTo(expectedValue)
}
