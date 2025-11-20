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

package androidx.wear.watchfacepush

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.wear.watchfacepush.test.RequiresWatch
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WatchFacePushManagerFactoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    @RequiresWatch
    @SdkSuppress(maxSdkVersion = 35)
    fun create_beforeSdk36_throwsUnsupportedException() {
        assertThrows(UnsupportedOperationException::class.java) {
            WatchFacePushManagerFactory.createWatchFacePushManager(context)
        }
    }

    @Test
    @RequiresWatch
    @SdkSuppress(maxSdkVersion = 35)
    fun create_beforeSdk36_returnsFalse() {
        val isWFpSupported = WatchFacePushManagerFactory.isSupported()

        assertThat(isWFpSupported).isFalse()
    }

    @Test
    @RequiresWatch
    @SdkSuppress(minSdkVersion = 36)
    fun create_afterSdk36_returnsNonNullManager() {
        val wfpManager = WatchFacePushManagerFactory.createWatchFacePushManager(context)

        assertThat(wfpManager).isNotNull()
    }

    @Test
    @RequiresWatch
    @SdkSuppress(minSdkVersion = 36)
    fun create_afterSdk36_returnsTrue() {
        val isWFpSupported = WatchFacePushManagerFactory.isSupported()

        assertThat(isWFpSupported).isTrue()
    }
}
