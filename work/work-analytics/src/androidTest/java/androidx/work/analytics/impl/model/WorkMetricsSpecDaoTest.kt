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

package androidx.work.analytics.impl.model

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.analytics.WorkMetricsInfo
import androidx.work.analytics.impl.WorkMetricsDatabase
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class WorkMetricsSpecDaoTest {

    private lateinit var database: WorkMetricsDatabase
    private lateinit var dao: WorkMetricsSpecDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WorkMetricsDatabase::class.java).build()
        dao = database.workMetricsSpecDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetWorkMetricsSpec() = runBlocking {
        val workSpecId = UUID.randomUUID().toString()
        val spec = createWorkMetricsSpec(workSpecId = workSpecId)

        dao.insertWorkMetricsSpec(spec)
        val result = dao.getWorkMetricsSpecs(workSpecId)

        assertEquals(1, result.size)
        assertEquals(spec, result[0])
    }

    @Test
    fun getWorkMetricsSpecs_isOrderedByEnqueueTime() = runBlocking {
        val workSpecId = UUID.randomUUID().toString()
        val spec1 =
            createWorkMetricsSpec(workSpecId = workSpecId, periodCount = 0, enqueueTime = 2000L)
        val spec2 =
            createWorkMetricsSpec(workSpecId = workSpecId, periodCount = 1, enqueueTime = 1000L)

        dao.insertWorkMetricsSpec(spec1)
        dao.insertWorkMetricsSpec(spec2)

        val result = dao.getWorkMetricsSpecs(workSpecId)

        assertEquals(2, result.size)
        assertEquals(spec2, result[0])
        assertEquals(spec1, result[1])
    }

    @Test
    fun typeConverters_handleComplexTypes() = runBlocking {
        val workSpecId = UUID.randomUUID().toString()
        val tags = listOf("tag1", "tag2")
        val stopReasons = mapOf(1 to 5, 2 to 3)

        val spec =
            createWorkMetricsSpec(
                workSpecId = workSpecId,
                tags = tags,
                stopReasonCounts = stopReasons,
            )

        dao.insertWorkMetricsSpec(spec)
        val result = dao.getWorkMetricsSpecs(workSpecId)[0]

        assertEquals(tags, result.tags)
        assertEquals(stopReasons, result.stopReasonCounts)
        assertEquals(WorkMetricsInfo.State.ENQUEUED_PENDING, result.state)
    }

    @Test
    fun getCurrentWorkMetricsSpec_returnsActiveRecord() = runBlocking {
        val workSpecId = UUID.randomUUID().toString()
        val oldSpec =
            createWorkMetricsSpec(
                workSpecId = workSpecId,
                generation = 0,
                periodCount = 0,
                state = WorkMetricsInfo.State.SUCCEEDED,
            )
        val newSpec =
            createWorkMetricsSpec(
                workSpecId = workSpecId,
                generation = 0,
                periodCount = 1,
                state = WorkMetricsInfo.State.ENQUEUED_PENDING,
            )

        dao.insertWorkMetricsSpec(oldSpec)
        dao.insertWorkMetricsSpec(newSpec)

        val current = dao.getCurrentWorkMetricsSpec(workSpecId)

        assertEquals(newSpec, current)
    }

    @Test
    fun setFinishTime_updatesCorrectly() = runBlocking {
        val workSpecId = UUID.randomUUID().toString()
        val spec =
            createWorkMetricsSpec(
                workSpecId = workSpecId,
                generation = 0,
                periodCount = 0,
                state = WorkMetricsInfo.State.ENQUEUED_PENDING,
            )
        dao.insertWorkMetricsSpec(spec)
        dao.setFinishTime(workId = workSpecId, generation = 0, periodCount = 0, finishTime = 3000L)

        val result = dao.getWorkMetricsSpecs(workSpecId)[0]
        assertEquals(3000L, result.finishTimeMillis)
    }

    @Test
    fun setUnblockTime_updatesCorrectly() = runBlocking {
        val workSpecId = UUID.randomUUID().toString()
        val spec =
            createWorkMetricsSpec(
                workSpecId = workSpecId,
                generation = 0,
                periodCount = 0,
                state = WorkMetricsInfo.State.ENQUEUED_PENDING,
            )
        dao.insertWorkMetricsSpec(spec)

        dao.setUnblockTime(
            workId = workSpecId,
            generation = 0,
            periodCount = 0,
            unblockTime = 4000L,
        )

        val result = dao.getWorkMetricsSpecs(workSpecId)[0]
        assertEquals(4000L, result.unblockTimeMillis)
    }

    @Test
    fun setFirstStartTime_updatesCorrectly() = runBlocking {
        val workSpecId = UUID.randomUUID().toString()
        val spec =
            createWorkMetricsSpec(
                workSpecId = workSpecId,
                generation = 0,
                periodCount = 0,
                state = WorkMetricsInfo.State.ENQUEUED_PENDING,
            )
        dao.insertWorkMetricsSpec(spec)

        dao.setFirstStartTime(
            workId = workSpecId,
            generation = 0,
            periodCount = 0,
            startTime = 5000L,
        )

        val result = dao.getWorkMetricsSpecs(workSpecId)[0]
        assertEquals(5000L, result.firstStartTimeMillis)
    }

    private fun createWorkMetricsSpec(
        workSpecId: String = UUID.randomUUID().toString(),
        generation: Int = 0,
        periodCount: Int = 0,
        enqueueTime: Long = 1000L,
        tags: List<String> = emptyList(),
        stopReasonCounts: Map<Int, Int> = emptyMap(),
        state: WorkMetricsInfo.State = WorkMetricsInfo.State.ENQUEUED_PENDING,
    ): WorkMetricsSpec {
        return WorkMetricsSpec(
            workSpecId = workSpecId,
            generation = generation,
            periodCount = periodCount,
            workerClassName = "MyWorker",
            state = state,
            tags = tags,
            enqueueTimeMillis = enqueueTime,
            stopReasonCounts = stopReasonCounts,
        )
    }
}
