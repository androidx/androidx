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

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.multiprocess.parcelable.ParcelConverters
import androidx.work.multiprocess.parcelable.ParcelableWorkInfo
import androidx.work.multiprocess.parcelable.ParcelableWorkInfos
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
public class ParcelableWorkInfoTest {

    // Setting the minSdkVersion to 27 otherwise we end up with SIGSEGVs.

    @Test
    @SmallTest
    public fun converterTest1() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val workInfo = WorkInfo(
            UUID.randomUUID(),
            WorkInfo.State.ENQUEUED,
            Data.EMPTY,
            listOf("tag1", "tag2"),
            Data.EMPTY,
            1
        )
        assertOn(workInfo)
    }

    @Test
    @SmallTest
    public fun converterTest2() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val data = Data.Builder()
            .put("test", "testString")
            .put("int", 10)
            .build()

        val workInfo = WorkInfo(
            UUID.randomUUID(),
            WorkInfo.State.ENQUEUED,
            data,
            listOf("tag1", "tag2"),
            Data.EMPTY,
            1
        )
        assertOn(workInfo)
    }

    @Test
    @SmallTest
    public fun arrayConverterTest1() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val data = Data.Builder()
            .put("test", "testString")
            .put("int", 10)
            .build()

        val workInfo = WorkInfo(
            UUID.randomUUID(),
            WorkInfo.State.ENQUEUED,
            data,
            listOf("tag1", "tag2"),
            Data.EMPTY,
            1
        )

        assertOn(listOf(workInfo, workInfo))
    }

    private fun assertOn(workInfos: List<WorkInfo>) {
        val parcelable = ParcelableWorkInfos(workInfos)
        val parcelled: ParcelableWorkInfos =
            ParcelConverters.unmarshall(
                ParcelConverters.marshall(parcelable),
                ParcelableWorkInfos.CREATOR
            )
        equal(workInfos, parcelled.workInfos)
    }

    private fun assertOn(workInfo: WorkInfo) {
        val parcelable = ParcelableWorkInfo(workInfo)
        val parcelled: ParcelableWorkInfo =
            ParcelConverters.unmarshall(
                ParcelConverters.marshall(parcelable),
                ParcelableWorkInfo.CREATOR
            )
        equal(workInfo, parcelled.workInfo)
    }

    private fun equal(first: List<WorkInfo>, second: List<WorkInfo>) {
        first.forEachIndexed { index, workInfo ->
            equal(workInfo, second[index])
        }
    }

    private fun equal(first: WorkInfo, second: WorkInfo) {
        assertEquals(first.id, second.id)
        assertEquals(first.state, second.state)
        assertEquals(first.outputData, second.outputData)
        assertEquals(first.tags, second.tags)
        assertEquals(first.progress, second.progress)
        assertEquals(first.runAttemptCount, second.runAttemptCount)
    }
}
