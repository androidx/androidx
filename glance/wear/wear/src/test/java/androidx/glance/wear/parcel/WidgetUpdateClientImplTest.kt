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

package androidx.glance.wear.parcel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Looper
import android.provider.Settings
import androidx.glance.wear.parcel.legacy.TileUpdateRequestData
import androidx.glance.wear.parcel.legacy.TileUpdateRequesterService
import androidx.glance.wear.proto.legacy.TileUpdateRequest
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowPackageManager

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class WidgetUpdateClientImplTest {
    private val appContext: Context = getApplicationContext<Context>()
    private var standardSysUiFakeReceiver = UpdateService()
    private var otherSysUiFakeReceiver = UpdateService()
    private var updateClient = WidgetUpdateClientImpl()

    @Before
    @Throws(Exception::class)
    fun setUp() {

        Settings.Global.putString(appContext.contentResolver, SYSUI_SETTINGS_KEY, null)

        // SysUiTileUpdateRequester searches PM for the service accepting
        // ACTION_BIND_UPDATE_REQUESTER; register it as a package here...
        val intentFilter = IntentFilter(WidgetUpdateClientImpl.ACTION_BIND_UPDATE_REQUESTER)

        // Used on U when the app's target SDK is higher than U.
        val homeIntentFilter =
            IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(WidgetUpdateClientImpl.CATEGORY_HOME_MAIN)
                addCategory(Intent.CATEGORY_DEFAULT)
            }

        // Robolectric won't tell us what service was bound (it'll just tell us the
        // ServiceConnection, which doesn't help). Instead, use two different instances of
        // TileUpdateReceiver, and just check which one received the call.
        val spm: ShadowPackageManager = Shadows.shadowOf(appContext.packageManager)
        spm.addServiceIfNotPresent(STANDARD_SYSUI_RECEIVER_COMPONENT_NAME)
        spm.addServiceIfNotPresent(OTHER_SYSUI_RECEIVER_COMPONENT_NAME)
        spm.addIntentFilterForService(STANDARD_SYSUI_RECEIVER_COMPONENT_NAME, intentFilter)
        spm.addIntentFilterForService(OTHER_SYSUI_RECEIVER_COMPONENT_NAME, intentFilter)

        spm.addActivityIfNotPresent(HOME_ACTIVITY_COMPONENT_NAME)
        spm.addIntentFilterForActivity(HOME_ACTIVITY_COMPONENT_NAME, homeIntentFilter)

        val shadowApp = Shadows.shadowOf(appContext as Application?)
        shadowApp.registerForBindService(
            STANDARD_SYSUI_RECEIVER_COMPONENT_NAME,
            standardSysUiFakeReceiver,
        )
        shadowApp.registerForBindService(
            OTHER_SYSUI_RECEIVER_COMPONENT_NAME,
            otherSysUiFakeReceiver,
        )
    }

    @Test
    fun requestUpdate_canBeCalled() {
        updateClient.requestUpdate(appContext, TEST_PROVIDER_COMPONENT)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        Truth.assertThat(otherSysUiFakeReceiver.requestedComponents).isEmpty()
        Truth.assertThat(standardSysUiFakeReceiver.requestedComponents)
            .contains(TEST_PROVIDER_COMPONENT)
    }

    @Test
    fun requestUpdate_unbindsAfterCall() {
        val shadowApp = Shadows.shadowOf(appContext as Application?)

        updateClient.requestUpdate(appContext, TEST_PROVIDER_COMPONENT)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        Truth.assertThat(shadowApp.boundServiceConnections).isEmpty()
        Truth.assertThat(shadowApp.unboundServiceConnections).hasSize(1)
    }

    @Test
    fun requestUpdate_queuesUpdatesWhileBinding() {
        updateClient.requestUpdate(appContext, TEST_PROVIDER_COMPONENT)
        updateClient.requestUpdate(appContext, ANOTHER_TEST_PROVIDER_COMPONENT)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        Truth.assertThat(standardSysUiFakeReceiver.requestedComponents)
            .contains(TEST_PROVIDER_COMPONENT)
        Truth.assertThat(standardSysUiFakeReceiver.requestedComponents)
            .contains(ANOTHER_TEST_PROVIDER_COMPONENT)

        // Ensure that there was only one connection made.
        val shadowApp = Shadows.shadowOf(appContext as Application?)
        Truth.assertThat(shadowApp.boundServiceConnections).isEmpty()
        Truth.assertThat(shadowApp.unboundServiceConnections).hasSize(1)
    }

    @Test
    fun requestUpdate_multipleUpdatesDebounced() {
        updateClient.requestUpdate(appContext, TEST_PROVIDER_COMPONENT)
        updateClient.requestUpdate(appContext, TEST_PROVIDER_COMPONENT)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        Truth.assertThat(standardSysUiFakeReceiver.requestedComponents).hasSize(1)
    }

    @Test
    fun requestUpdate_usesGivenSysUiIfSet() {
        Settings.Global.putString(
            appContext.contentResolver,
            SYSUI_SETTINGS_KEY,
            OTHER_SYSUI_RECEIVER_COMPONENT_NAME.packageName,
        )

        updateClient.requestUpdate(appContext, TEST_PROVIDER_COMPONENT)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        Truth.assertThat(standardSysUiFakeReceiver.requestedComponents).isEmpty()
        Truth.assertThat(otherSysUiFakeReceiver.requestedComponents).hasSize(1)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun requestUpdate_onU_withHigherTargetSdk_usesSysUiPackageFromHomeActivity() {
        updateClient.requestUpdate(appContext, TEST_PROVIDER_COMPONENT)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        Truth.assertThat(standardSysUiFakeReceiver.requestedComponents).isEmpty()
        Truth.assertThat(otherSysUiFakeReceiver.requestedComponents).hasSize(1)
    }

    internal class UpdateService : TileUpdateRequesterService.Stub() {
        var requestedComponents = mutableListOf<ComponentName>()
        var requestedIds = mutableListOf<Int>()

        override fun getApiVersion() = API_VERSION

        override fun requestUpdate(component: ComponentName?, updateData: TileUpdateRequestData?) {
            if (component == null || updateData == null) {
                return
            }
            requestedComponents.add(component)
            val request = TileUpdateRequest.Companion.ADAPTER.decode(updateData.contents)
            request.tile_id?.let { requestedIds.add(it) }
        }
    }

    private companion object {
        const val SYSUI_SETTINGS_KEY = "clockwork_sysui_package"

        val STANDARD_SYSUI_RECEIVER_COMPONENT_NAME =
            ComponentName("com.google.android.wearable.app", "TileUpdateReceiver")
        val OTHER_SYSUI_RECEIVER_COMPONENT_NAME =
            ComponentName("my.awesome.sysui", "UpdateReceiver")
        val HOME_ACTIVITY_COMPONENT_NAME =
            ComponentName(OTHER_SYSUI_RECEIVER_COMPONENT_NAME.packageName, "HomeActivity")

        val TEST_PROVIDER_COMPONENT = ComponentName("some.package", "some.package.ClassName")
        val ANOTHER_TEST_PROVIDER_COMPONENT =
            ComponentName("some.other.package", "some.other.package.ClassName")

        fun ShadowApplication.registerForBindService(
            componentName: ComponentName,
            service: TileUpdateRequesterService.Stub,
        ) {
            val bindIntent =
                Intent(WidgetUpdateClientImpl.ACTION_BIND_UPDATE_REQUESTER).apply {
                    `package` = componentName.packageName
                    component = componentName
                }
            this.setComponentNameAndServiceForBindServiceForIntent(
                bindIntent,
                componentName,
                service.asBinder(),
            )
        }
    }
}
