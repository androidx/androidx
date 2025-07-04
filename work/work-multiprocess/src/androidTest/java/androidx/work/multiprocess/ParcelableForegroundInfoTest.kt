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

package androidx.work.multiprocess

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.ForegroundInfo
import androidx.work.multiprocess.parcelable.ParcelConverters
import androidx.work.multiprocess.parcelable.ParcelableForegroundInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 27)
class ParcelableForegroundInfoTest {

    lateinit var context: Context

    @Before
    public fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    @SmallTest
    public fun converterTest1() {
        val channelId = "channelId"
        val notificationId = 10
        val title = "Some title"
        val description = "Some description"
        val notification =
            NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(description)
                .setTicker(title)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()

        val foregroundInfo =
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )

        assertOn(foregroundInfo)
    }

    private fun assertOn(foregroundInfo: ForegroundInfo) {
        val parcelable = ParcelableForegroundInfo(foregroundInfo)
        val parcelled: ParcelableForegroundInfo =
            ParcelConverters.unmarshall(
                ParcelConverters.marshall(parcelable),
                ParcelableForegroundInfo.CREATOR,
            )
        equal(foregroundInfo, parcelled.foregroundInfo)
    }

    private fun equal(first: ForegroundInfo, second: ForegroundInfo) {
        assertThat(first.notificationId).isEqualTo(second.notificationId)
        assertThat(first.foregroundServiceType).isEqualTo(second.foregroundServiceType)
        equal(first.notification, second.notification)
    }

    private fun equal(first: Notification, second: Notification) {
        assertThat(first.channelId).isEqualTo(second.channelId)
        assertThat(first.tickerText).isEqualTo(second.tickerText)
    }
}
