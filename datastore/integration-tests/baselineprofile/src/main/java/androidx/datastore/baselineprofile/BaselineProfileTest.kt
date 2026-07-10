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

package androidx.datastore.baselineprofile

import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.R
import androidx.benchmark.DeviceInfo
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@SdkSuppress(minSdkVersion = 28)
@RunWith(Parameterized::class)
class BaselineProfileTest(val startWithoutDatastoreFile: Boolean) {

    @get:Rule val baselineRule = BaselineProfileRule()

    fun setup(dataStoreType: String) {
        InstrumentationRegistry.getInstrumentation()
            .context
            .startActivity(
                Intent().apply {
                    action = DATA_STORE_SETUP_ACTION
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (startWithoutDatastoreFile) {
                        putExtra("delete datastore file", dataStoreType)
                    } else {
                        putExtra("copy datastore file", dataStoreType)
                    }
                }
            )
    }

    @Test
    fun preferencesDataStore() {
        assumeTrue(DeviceInfo.isRooted || SDK_INT >= R)

        // Collects the baseline profile
        baselineRule.collect(
            packageName = TARGET_PACKAGE,
            profileBlock = {
                // Launch App (Trigger Read).
                setup("preferences")
                device.waitForIdle()
                startActivityAndWait(Intent(PREFERENCES_DATA_STORE_ACTION))

                // Update Settings (Trigger Writes).
                device.updateValues()
            },
            filterPredicate = DATASTORE_FILTER,
        )
    }

    @Test
    fun jsonDataStore() {
        assumeTrue(DeviceInfo.isRooted || SDK_INT >= R)

        // Collects the baseline profile
        baselineRule.collect(
            packageName = TARGET_PACKAGE,
            profileBlock = {
                // Launch App (Trigger Read).
                setup("json")
                device.waitForIdle()
                startActivityAndWait(Intent(JSON_DATA_STORE_ACTION))

                // Update Settings (Trigger Writes).
                device.updateValues()
            },
            filterPredicate = DATASTORE_FILTER,
        )
    }

    @Test
    fun protoDataStore() {
        assumeTrue(DeviceInfo.isRooted || SDK_INT >= R)

        // Collects the baseline profile
        baselineRule.collect(
            packageName = TARGET_PACKAGE,
            profileBlock = {
                // Launch App (Trigger Read).
                setup("proto")
                device.waitForIdle()
                startActivityAndWait(Intent(PROTO_DATA_STORE_ACTION))

                // Update Settings (Trigger Writes).
                device.updateValues()
            },
            filterPredicate = DATASTORE_FILTER,
        )
    }

    // Run this only to collect baseline profiles for the target (Non DataStore code paths).
    // @Test
    fun noDataStore() {
        assumeTrue(startWithoutDatastoreFile && (DeviceInfo.isRooted || SDK_INT >= R))

        // Collects the baseline profile
        baselineRule.collect(
            packageName = TARGET_PACKAGE,
            profileBlock = {
                // Launch App (Trigger Read).
                startActivityAndWait(Intent(NO_DATA_STORE_ACTION))

                // Update Settings (Trigger Writes).
                device.updateValues()
            },
        )
    }

    private fun UiDevice.updateValues() {
        delay()
        findObject(By.res("UserTextField")).setText("Jane Doe")
        delay()
        findObject(By.res("DisplayModeDropdownMenu")).click()
        delay()
        findObject(By.res("DisplayModeOption2")).click()
        delay()
        findObject(By.res("CameraDropdownMenu")).click()
        delay()
        findObject(By.res("CameraOption2")).click()
        delay()
        findObject(By.res("VolumeSlider")).swipe(Direction.RIGHT, 0.8f)
        delay()
        findObject(By.res("BrightnessSlider")).swipe(Direction.RIGHT, 0.8f)
        delay()
        findObject(By.res("DarkModeSwitch")).click()
        delay()
    }

    private fun UiDevice.delay() {
        Thread.sleep(300L)
        waitForIdle()
    }

    companion object {

        @Parameterized.Parameters(name = "{0}") @JvmStatic fun parameters() = arrayOf(false, true)

        private const val TARGET_PACKAGE = "androidx.datastore.macrobenchmark.target"
        private const val DATA_STORE_SETUP_ACTION = "$TARGET_PACKAGE.DataStoreSetup"
        private const val PREFERENCES_DATA_STORE_ACTION = "$TARGET_PACKAGE.PreferencesDataStore"
        private const val PROTO_DATA_STORE_ACTION = "$TARGET_PACKAGE.ProtoDataStore"
        private const val JSON_DATA_STORE_ACTION = "$TARGET_PACKAGE.JsonDataStore"
        private const val NO_DATA_STORE_ACTION = "$TARGET_PACKAGE.NoDataStore"

        // Include baseline profiles for datastore, but exclude the macrobenchmark.
        private val DATASTORE_FILTER: (String) -> Boolean = {
            it.contains("androidx/datastore".toRegex()) &&
                !it.contains("androidx/datastore/macrobenchmark/target".toRegex())
        }
    }
}
