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

package androidx.core.i18n

import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckDeviceSettingsTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test @SmallTest
    fun testDefaultLocale() {
        // A lot of the tests use the default device locale
        // (and that is OK, that's what we care about most)
        // But we need to make sure it is what we expect (en-US)
        Assert.assertEquals(Locale.US, Locale.getDefault())
    }

    @Test @SmallTest
    fun testHourSettings() {
        val deviceHour = Settings.System.getString(
            appContext.contentResolver,
            Settings.System.TIME_12_24
        )
        // A lot of the tests respect the user settings for the hour.
        // For the tests to pass we need that the be default / respect locale.
        // We would also need some tests to check that if it is forced to h12 / h24  we respect it.
        // But that would require a device (emulated or not) with that setting changed.
        // And a lot of tests disabled in that situation (because they expect locale hour style).
        // So we need to figure out how to do all that, and then refactor.
        // For now I did such testing "manually", and for that you need to disable this test.
        Assert.assertNull(deviceHour)
    }

    @Test @SmallTest
    fun testReportCldrAndIcuVersions() {
        // E.g.: API_24_v_NYC cldr:28.0 icu:56.1 unicode:8.0
        val result = StringBuilder()
        result.append("API_" + Build.VERSION.SDK_INT) // 17
        result.append("_v" + stripVersionTrailing(Build.VERSION.RELEASE))
        result.append("_" + Build.ID)
        val props = System.getProperties()
        result.append(" icu:" + props.getProperty("android.icu.library.version"))
        result.append(" cldr:" + props.getProperty("android.icu.cldr.version"))
        result.append(" unicode:" + props.getProperty("android.icu.unicode.version"))
        Log.i(this::class.simpleName, "Versions: $result")
    }

    private fun stripVersionTrailing(str: String): String {
        return str.replace(Regex("[0-9.]+$"), "")
    }
}
