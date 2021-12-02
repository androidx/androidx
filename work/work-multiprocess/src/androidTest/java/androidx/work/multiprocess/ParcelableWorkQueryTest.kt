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
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.multiprocess.parcelable.ParcelConverters
import androidx.work.multiprocess.parcelable.ParcelableWorkQuery
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
public class ParcelableWorkQueryTest {

    @Test
    @SmallTest
    public fun converterTest1() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val workQuery = WorkQuery.Builder.fromUniqueWorkNames(listOf("name1"))
            .addTags(listOf("tag1", "tag2"))
            .addIds(listOf(UUID.randomUUID()))
            .addStates(listOf(WorkInfo.State.ENQUEUED))
            .build()

        assertOn(workQuery)
    }

    @Test
    @SmallTest
    public fun converterTest2() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val workQuery = WorkQuery.Builder.fromUniqueWorkNames(listOf("name1"))
            .build()

        assertOn(workQuery)
    }

    @Test
    @SmallTest
    public fun converterTest3() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val workQuery = WorkQuery.Builder.fromTags(listOf("tag1", "tag2"))
            .build()

        assertOn(workQuery)
    }

    @Test
    @SmallTest
    public fun converterTest4() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val workQuery = WorkQuery.Builder.fromStates(listOf(WorkInfo.State.ENQUEUED))
            .build()

        assertOn(workQuery)
    }

    private fun assertOn(workQuery: WorkQuery) {
        val parcelable = ParcelableWorkQuery(workQuery)
        val parcelled: ParcelableWorkQuery =
            ParcelConverters.unmarshall(
                ParcelConverters.marshall(parcelable),
                ParcelableWorkQuery.CREATOR
            )
        equal(workQuery, parcelled.workQuery)
    }

    private fun equal(first: WorkQuery, second: WorkQuery) {
        assertEquals(first.ids, second.ids)
        assertEquals(first.uniqueWorkNames, second.uniqueWorkNames)
        assertEquals(first.tags, second.tags)
        assertEquals(first.states, second.states)
    }
}
