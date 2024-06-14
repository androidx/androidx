/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.security.state

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
class SecurityStateManagerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var securityStateManager: SecurityStateManager

    @Before
    fun setup() {
        securityStateManager = SecurityStateManager(context)
    }

    @Test
    fun testGetGlobalSecurityState() {
        val bundle = securityStateManager.getGlobalSecurityState()

        // Check if dates are in the format YYYY-MM-DD
        val dateRegex = "^\\d{4}-\\d{2}-\\d{2}$"
        assertTrue(bundle.getString("system_spl")!!.matches(dateRegex.toRegex()))
        assertTrue(bundle.getString("vendor_spl")!!.matches(dateRegex.toRegex()))

        // Check if kernel version is in the format X.X.XX
        val versionRegex = "^\\d+\\.\\d+\\.\\d+$"
        assertTrue(bundle.getString("kernel_version")!!.matches(versionRegex.toRegex()))
    }
}
