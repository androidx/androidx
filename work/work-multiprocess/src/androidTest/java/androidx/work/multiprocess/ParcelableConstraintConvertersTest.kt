/*
 * Copyright 2020 The Android Open Source Project
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

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.multiprocess.parcelable.ParcelConverters.marshall
import androidx.work.multiprocess.parcelable.ParcelConverters.unmarshall
import androidx.work.multiprocess.parcelable.ParcelableConstraints
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
public class ParcelableConstraintConvertersTest {
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    public fun converterTest1() {
        val uri = Uri.parse("test://foo")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(true)
            .setRequiresStorageNotLow(true)
            .addContentUriTrigger(uri, true)
            .build()

        assertOn(constraints)
    }

    @Test
    @SmallTest
    public fun converterTest2() {
        val constraints = Constraints.Builder().build()
        assertOn(constraints)
    }

    private fun assertOn(constraints: Constraints) {
        val parcelable = ParcelableConstraints(constraints)
        val parcelled: ParcelableConstraints =
            unmarshall(marshall(parcelable), ParcelableConstraints.CREATOR)
        assertEquals(constraints, parcelled.constraints)
    }
}
