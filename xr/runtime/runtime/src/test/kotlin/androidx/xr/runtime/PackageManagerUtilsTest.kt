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

package androidx.xr.runtime

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.PackageManagerUtils.XR_PROJECTED_SYSTEM_FEATURE
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows

@RunWith(AndroidJUnit4::class)
class PackageManagerUtilsTest {

    @Test
    fun hasXrProjectedSystemFeature_whenFeatureExists_returnsTrue() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        shadowPackageManager.setSystemFeature(XR_PROJECTED_SYSTEM_FEATURE, true)

        assertThat(PackageManagerUtils.hasXrProjectedSystemFeature(context)).isTrue()
    }

    @Test
    fun hasXrProjectedSystemFeature_whenFeatureDoesNotExist_returnsFalse() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        shadowPackageManager.setSystemFeature(XR_PROJECTED_SYSTEM_FEATURE, false)

        assertThat(PackageManagerUtils.hasXrProjectedSystemFeature(context)).isFalse()
    }
}
