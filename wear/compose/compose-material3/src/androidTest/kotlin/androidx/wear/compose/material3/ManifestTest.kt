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

package androidx.wear.compose.material3

import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test

class ManifestTest {
    @Test
    fun verifyGlobalStatusBarMetaData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appInfo =
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA,
            )
        val value = appInfo.metaData?.getBoolean("com.google.wear.ENABLE_GLOBAL_STATUS_BAR")
        Assert.assertTrue(
            "Manifest metadata 'com.google.wear.ENABLE_GLOBAL_STATUS_BAR' should be true",
            value == true,
        )
    }
}
