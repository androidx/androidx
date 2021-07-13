/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.phone.interactions.notifications

import android.content.Context
import android.content.Intent
import android.support.wearable.notifications.IBridgingManagerService
import androidx.test.core.app.ApplicationProvider
import androidx.wear.phone.interactions.WearPhoneInteractionsTestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBinder
import org.robolectric.shadows.ShadowPackageManager

/** Unit tests for [BridgingManagerService] and [BridgingManagerServiceImpl] classes.  */
@RunWith(WearPhoneInteractionsTestRunner::class)
@DoNotInstrument // Needed because it is defined in the "android" package.
class BridgingManagerServiceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val packageManager: ShadowPackageManager = Shadows.shadowOf(context.packageManager)

    @Before
    fun setUp() {
        ShadowBinder.setCallingUid(UID)
    }

    private fun setNameForUid(packageName: String) {
        packageManager.setNameForUid(UID, packageName)
    }

    @Test
    fun testGetBinderObject_setBridgingConfig_success() {
        setNameForUid(context.packageName)
        val testBridgingConfigurationHandler = TestBridgingConfigurationHandler()
        val bridgingManagerService =
            BridgingManagerService(context, testBridgingConfigurationHandler)
        val intent = Intent(BridgingManager.ACTION_BIND_BRIDGING_MANAGER)
        val bridgingConfig = BridgingConfig.Builder(context, isBridgingEnabled = false).build()

        val binder = bridgingManagerService.onBind(intent)

        assertNotNull(binder)
        val bridgingManagerServiceImpl = IBridgingManagerService.Stub.asInterface(binder)
        assertNotNull(bridgingManagerServiceImpl)

        bridgingManagerServiceImpl.setBridgingConfig(bridgingConfig.toBundle(context))
        assertEquals(bridgingConfig, testBridgingConfigurationHandler.bridgingConfig)
    }

    @Test
    fun testGetBinderObject_setBridgingConfig_wrongPackage() {
        setNameForUid("different.${context.packageName}")
        val testBridgingConfigurationHandler = TestBridgingConfigurationHandler()
        val bridgingManagerService =
            BridgingManagerService(context, testBridgingConfigurationHandler)
        val intent = Intent(BridgingManager.ACTION_BIND_BRIDGING_MANAGER)
        val bridgingConfig = BridgingConfig.Builder(
            context /* packageName = PACKAGE_NAME */, false
        ).build()

        var binder = bridgingManagerService.onBind(intent)

        assertNotNull(binder)
        val bridgingManagerServiceImpl = IBridgingManagerService.Stub.asInterface(binder)
        assertNotNull(bridgingManagerServiceImpl)

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            bridgingManagerServiceImpl.setBridgingConfig(bridgingConfig.toBundle(context))
        }
    }

    @Test
    fun testGetBinderObject_wrongIntent() {
        val bridgingManagerService =
            BridgingManagerService(context, TestBridgingConfigurationHandler())
        val intent = Intent()

        val binder = bridgingManagerService.onBind(intent)

        assertNull(binder)
    }

    @Test
    fun testGetBinderObject_nullIntent() {
        val bridgingManagerService =
            BridgingManagerService(context, TestBridgingConfigurationHandler())

        val binder = bridgingManagerService.onBind(intent = null)

        assertNull(binder)
    }

    companion object {
        private const val UID = 1234
    }
}

private class TestBridgingConfigurationHandler : BridgingConfigurationHandler {
    var bridgingConfig: BridgingConfig? = null

    override fun applyBridgingConfiguration(bridgingConfig: BridgingConfig) {
        this.bridgingConfig = bridgingConfig
    }
}