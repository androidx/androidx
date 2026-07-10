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

package androidx.work.analytics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class WorkMetricsQueryTest {

    @Test
    fun testBuilderValidQuery() {
        val workId = UUID.randomUUID()
        val query =
            WorkMetricsQuery.Builder()
                .addWorkIds(workId)
                .addStates(WorkMetricsInfo.State.RUNNING)
                .addTags("tag1", "tag2")
                .addWorkerClassNames("MyWorker")
                .setBeginTimeMillis(0L)
                .setEndTimeMillis(100L)
                .build()

        assertEquals(listOf(workId), query.workIds)
        assertEquals(listOf(WorkMetricsInfo.State.RUNNING), query.states)
        assertEquals(listOf("tag1", "tag2"), query.tags)
        assertEquals(listOf("MyWorker"), query.workerClassNames)
        assertEquals(0L, query.beginTimeMillis)
        assertEquals(100L, query.endTimeMillis)
    }

    @Test
    fun testBuilderEmptyQuerySucceeds() {
        val query = WorkMetricsQuery.Builder().build()
        assertTrue(query.workIds.isEmpty())
        assertTrue(query.states.isEmpty())
        assertTrue(query.tags.isEmpty())
        assertTrue(query.workerClassNames.isEmpty())
        assertEquals(0L, query.beginTimeMillis)
        assertEquals(Long.MAX_VALUE, query.endTimeMillis)
    }

    @Test
    fun testBuilderInvalidTimeRangeThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkMetricsQuery.Builder()
                .addWorkIds(listOf(UUID.randomUUID()))
                .setBeginTimeMillis(100L)
                .setEndTimeMillis(50L)
                .build()
        }
    }

    @Test
    fun testBuilderNegativeTimeThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkMetricsQuery.Builder().setBeginTimeMillis(-1L).build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkMetricsQuery.Builder().setEndTimeMillis(-1L).build()
        }
    }
}
