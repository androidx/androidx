/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.credentials.provider.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.credentials.provider.RemoteEntry
import androidx.credentials.provider.RemoteEntry.Companion.fromSlice
import androidx.credentials.provider.RemoteEntry.Companion.toSlice
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertNotNull
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class RemoteEntryTest {
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mIntent = Intent()
    private val mPendingIntent = PendingIntent.getActivity(mContext, 0, mIntent,
        PendingIntent.FLAG_IMMUTABLE)

    @Test
    fun constructor_success() {
        val entry = RemoteEntry(mPendingIntent)

        assertNotNull(entry)
        assertThat(mPendingIntent).isEqualTo(entry.pendingIntent)
    }

    @Test
    fun build_success() {
        val entry = RemoteEntry.Builder(mPendingIntent).build()

        assertNotNull(entry)
        assertThat(mPendingIntent).isEqualTo(entry.pendingIntent)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun fromSlice_success() {
        val originalEntry = RemoteEntry(mPendingIntent)

        val fromSlice = fromSlice(RemoteEntry.toSlice(originalEntry))

        assertThat(fromSlice).isNotNull()
        if (fromSlice != null) {
            assertThat(fromSlice.pendingIntent).isEqualTo(mPendingIntent)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun fromRemoteEntry_success() {
        val originalEntry = RemoteEntry(mPendingIntent)
        val slice = toSlice(originalEntry)
        Assert.assertNotNull(slice)

        val remoteEntry = RemoteEntry.fromRemoteEntry(
            android.service.credentials.RemoteEntry(slice))

        assertThat(remoteEntry).isNotNull()
        assertThat(remoteEntry!!.pendingIntent).isEqualTo(mPendingIntent)
    }
}
