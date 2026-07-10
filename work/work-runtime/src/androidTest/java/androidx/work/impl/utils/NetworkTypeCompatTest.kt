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

package androidx.work.impl.utils

import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING
import android.net.NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.NetworkType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 28)
class NetworkTypeCompatTest {

    @Test
    fun toNetworkRequest_notRequired() {
        assertThat(NetworkType.NOT_REQUIRED.toNetworkRequest()).isNull()
    }

    @Test
    fun toNetworkRequest_unmetered() {
        assertThat(
                NetworkType.UNMETERED.toNetworkRequest()!!.hasCapability(NET_CAPABILITY_NOT_METERED)
            )
            .isTrue()
    }

    @Test
    fun toNetworkRequest_metered() {
        assertThat(NetworkType.METERED.toNetworkRequest()!!.hasTransport(TRANSPORT_CELLULAR))
            .isTrue()
    }

    @Test
    fun toNetworkRequest_notRoaming() {
        assertThat(
                NetworkType.NOT_ROAMING.toNetworkRequest()!!.hasCapability(
                    NET_CAPABILITY_NOT_ROAMING
                )
            )
            .isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun toNetworkRequest_temporarilyUnmetered() {
        assertThat(
                NetworkType.TEMPORARILY_UNMETERED.toNetworkRequest()!!.hasCapability(
                    NET_CAPABILITY_TEMPORARILY_NOT_METERED
                )
            )
            .isTrue()
    }
}
