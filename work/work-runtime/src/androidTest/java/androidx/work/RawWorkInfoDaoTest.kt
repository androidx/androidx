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

package androidx.work

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.impl.utils.RawQueries
import androidx.work.worker.RetryWorker
import androidx.work.worker.TestWorker
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RawWorkInfoDaoTest : DatabaseTest() {

    @Test
    @SmallTest
    fun namesOnlyTest1() {
        val test = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialState(WorkInfo.State.RUNNING)
            .build()

        val retry = OneTimeWorkRequest.from(RetryWorker::class.java)

        insertWork(test)
        insertTags(test)

        insertWork(retry)
        insertTags(retry)
        insertName("name", retry)

        val querySpec = WorkQuery.Builder.fromUniqueWorkNames(listOf("name"))
            .build()

        val pojos = mDatabase.rawWorkInfoDao().getWorkInfoPojos(
            RawQueries
                .workQueryToRawQuery(querySpec)
        )
        assertThat(pojos.size, `is`(1))
        assertThat(pojos[0].id, `is`(retry.stringId))
    }

    @Test
    @SmallTest
    fun namesOnlyTest2() {
        val test = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialState(WorkInfo.State.RUNNING)
            .build()

        val retry = OneTimeWorkRequest.from(RetryWorker::class.java)

        insertWork(test)
        insertTags(test)
        insertName("name1", test)

        insertWork(retry)
        insertTags(retry)
        insertName("name2", retry)

        val querySpec = WorkQuery.Builder
            .fromUniqueWorkNames(listOf("name1"))
            .addUniqueWorkNames(listOf("name2"))
            .build()

        val pojos = mDatabase.rawWorkInfoDao().getWorkInfoPojos(
            RawQueries
                .workQueryToRawQuery(querySpec)
        )
        assertThat(pojos.size, `is`(2))
    }

    @Test
    @SmallTest
    fun idsOnlyTest1() {
        val test = OneTimeWorkRequest.from(TestWorker::class.java)
        val retry = OneTimeWorkRequest.from(RetryWorker::class.java)

        insertWork(test)
        insertWork(retry)

        val querySpec = WorkQuery.Builder
            .fromIds(listOf(test.id))
            .build()

        val pojos = mDatabase.rawWorkInfoDao().getWorkInfoPojos(
            RawQueries
                .workQueryToRawQuery(querySpec)
        )
        assertThat(pojos.size, `is`(1))
        assertThat(pojos[0].id, `is`(test.stringId))
    }

    @Test
    @SmallTest
    fun tagsOnlyTest1() {
        val test = OneTimeWorkRequest.from(TestWorker::class.java)
        val retry = OneTimeWorkRequest.from(RetryWorker::class.java)

        insertWork(test)
        insertTags(test)

        insertWork(retry)
        insertTags(retry)

        val querySpec = WorkQuery.Builder
            .fromTags(listOf(TestWorker::class.java.name))
            .build()

        val pojos = mDatabase.rawWorkInfoDao().getWorkInfoPojos(
            RawQueries
                .workQueryToRawQuery(querySpec)
        )
        assertThat(pojos.size, `is`(1))
        assertThat(pojos[0].id, `is`(test.stringId))
    }

    @Test
    @SmallTest
    fun tagsOnlyTest2() {
        val test = OneTimeWorkRequest.from(TestWorker::class.java)
        val retry = OneTimeWorkRequest.from(RetryWorker::class.java)

        insertWork(test)
        insertTags(test)

        insertWork(retry)
        insertTags(retry)

        val querySpec = WorkQuery.Builder
            .fromTags(listOf(TestWorker::class.java.name, RetryWorker::class.java.name))
            .build()

        val pojos = mDatabase.rawWorkInfoDao().getWorkInfoPojos(
            RawQueries
                .workQueryToRawQuery(querySpec)
        )
        assertThat(pojos.size, `is`(2))
    }

    @Test
    @SmallTest
    fun statesOnlyTest() {
        val test1 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialState(WorkInfo.State.RUNNING)
            .build()

        val test2 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialState(WorkInfo.State.CANCELLED)
            .build()

        val test3 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialState(WorkInfo.State.ENQUEUED)
            .build()

        insertWork(test1)
        insertTags(test1)

        insertWork(test2)
        insertTags(test2)

        insertWork(test3)
        insertTags(test3)

        val querySpec = WorkQuery.Builder
            .fromStates(listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.CANCELLED))
            .build()

        val pojos = mDatabase.rawWorkInfoDao().getWorkInfoPojos(
            RawQueries
                .workQueryToRawQuery(querySpec)
        )
        val ids = pojos.map { it.id }
        assertThat(pojos.size, `is`(2))
        assertThat(ids, containsInAnyOrder(test2.stringId, test3.stringId))
    }

    @Test
    @SmallTest
    fun idsAndTagsTest1() {
        val test = OneTimeWorkRequest.from(TestWorker::class.java)
        val retry = OneTimeWorkRequest.from(RetryWorker::class.java)

        insertWork(test)
        insertTags(test)

        insertWork(retry)
        insertTags(retry)

        val querySpec = WorkQuery.Builder
            .fromIds(listOf(test.id, retry.id))
            .addTags(listOf(TestWorker::class.java.name))
            .build()

        val pojos = mDatabase.rawWorkInfoDao().getWorkInfoPojos(
            RawQueries
                .workQueryToRawQuery(querySpec)
        )
        assertThat(pojos.size, `is`(1))
        assertThat(pojos[0].id, `is`(test.stringId))
    }

    @Test
    @SmallTest
    fun statesAndTags1() {
        val test = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialState(WorkInfo.State.RUNNING)
            .build()

        val retry = OneTimeWorkRequest.from(RetryWorker::class.java)

        insertWork(test)
        insertTags(test)

        insertWork(retry)
        insertTags(retry)

        val querySpec = WorkQuery.Builder
            .fromStates(listOf(WorkInfo.State.ENQUEUED))
            .addTags(listOf(TestWorker::class.java.name))
            .build()

        val pojos = mDatabase.rawWorkInfoDao().getWorkInfoPojos(
            RawQueries
                .workQueryToRawQuery(querySpec)
        )
        assertThat(pojos.size, `is`(0))
    }

    @Test
    @SmallTest
    fun statesAndTags2() {
        val test = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialState(WorkInfo.State.RUNNING)
            .build()

        val retry = OneTimeWorkRequest.from(RetryWorker::class.java)

        insertWork(test)
        insertTags(test)

        insertWork(retry)
        insertTags(retry)

        val querySpec = WorkQuery.Builder
            .fromStates(listOf(WorkInfo.State.RUNNING))
            .addTags(listOf(TestWorker::class.java.name))
            .build()

        val pojos = mDatabase.rawWorkInfoDao().getWorkInfoPojos(
            RawQueries
                .workQueryToRawQuery(querySpec)
        )
        assertThat(pojos.size, `is`(1))
        assertThat(pojos[0].id, `is`(test.stringId))
    }

    @Test
    @SmallTest
    fun idsAndName() {
        val test = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialState(WorkInfo.State.RUNNING)
            .build()

        val retry = OneTimeWorkRequest.from(RetryWorker::class.java)

        insertWork(test)
        insertTags(test)

        insertWork(retry)
        insertTags(retry)
        insertName("name", retry)

        val querySpec = WorkQuery.Builder
            .fromIds(listOf(retry.id, test.id))
            .addUniqueWorkNames(listOf("name"))
            .build()

        val pojos = mDatabase.rawWorkInfoDao().getWorkInfoPojos(
            RawQueries
                .workQueryToRawQuery(querySpec)
        )
        assertThat(pojos.size, `is`(1))
        assertThat(pojos[0].id, `is`(retry.stringId))
    }

    @Test
    @SmallTest
    fun statesAndName() {
        val test = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialState(WorkInfo.State.RUNNING)
            .build()

        val retry = OneTimeWorkRequest.from(RetryWorker::class.java)

        insertWork(test)
        insertTags(test)

        insertWork(retry)
        insertTags(retry)
        insertName("name", retry)

        val querySpec = WorkQuery.Builder
            .fromStates(listOf(WorkInfo.State.ENQUEUED))
            .addUniqueWorkNames(listOf("name"))
            .build()

        val pojos = mDatabase.rawWorkInfoDao().getWorkInfoPojos(
            RawQueries
                .workQueryToRawQuery(querySpec)
        )
        assertThat(pojos.size, `is`(1))
        assertThat(pojos[0].id, `is`(retry.stringId))
    }

    @Test
    @SmallTest
    fun tagsAndName() {
        val test = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialState(WorkInfo.State.RUNNING)
            .build()

        val retry = OneTimeWorkRequest.from(RetryWorker::class.java)

        insertWork(test)
        insertTags(test)

        insertWork(retry)
        insertTags(retry)
        insertName("name", retry)

        val querySpec = WorkQuery.Builder
            .fromTags(listOf(TestWorker::class.java.name, RetryWorker::class.java.name))
            .addUniqueWorkNames(listOf("name"))
            .build()

        val pojos = mDatabase.rawWorkInfoDao().getWorkInfoPojos(
            RawQueries
                .workQueryToRawQuery(querySpec)
        )
        assertThat(pojos.size, `is`(1))
        assertThat(pojos[0].id, `is`(retry.stringId))
    }

    @Test
    @SmallTest
    fun statesTagsAndName() {
        val test1 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialState(WorkInfo.State.ENQUEUED)
            .build()

        val test2 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialState(WorkInfo.State.RUNNING)
            .build()

        val retry = OneTimeWorkRequest.from(RetryWorker::class.java)

        insertWork(test1)
        insertTags(test1)
        insertName("name", test1)

        insertWork(test2)
        insertTags(test2)

        insertWork(retry)
        insertTags(retry)

        val querySpec = WorkQuery.Builder
            .fromStates(listOf(WorkInfo.State.ENQUEUED))
            .addTags(listOf(TestWorker::class.java.name, RetryWorker::class.java.name))
            .addUniqueWorkNames(listOf("name"))
            .build()

        val pojos = mDatabase.rawWorkInfoDao().getWorkInfoPojos(
            RawQueries
                .workQueryToRawQuery(querySpec)
        )
        assertThat(pojos.size, `is`(1))
        assertThat(pojos[0].id, `is`(test1.stringId))
    }

    @Test
    @SmallTest
    fun idsStatesTagsAndName() {
        val test1 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialState(WorkInfo.State.ENQUEUED)
            .build()

        val test2 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialState(WorkInfo.State.RUNNING)
            .build()

        val retry = OneTimeWorkRequest.from(RetryWorker::class.java)

        insertWork(test1)
        insertTags(test1)
        insertName("name", test1)

        insertWork(test2)
        insertTags(test2)

        insertWork(retry)
        insertTags(retry)

        val querySpec = WorkQuery.Builder
            .fromStates(listOf(WorkInfo.State.ENQUEUED))
            .addIds(listOf(test1.id, test2.id, retry.id))
            .addTags(listOf(TestWorker::class.java.name, RetryWorker::class.java.name))
            .addUniqueWorkNames(listOf("name"))
            .build()

        val pojos = mDatabase.rawWorkInfoDao().getWorkInfoPojos(
            RawQueries
                .workQueryToRawQuery(querySpec)
        )
        assertThat(pojos.size, `is`(1))
        assertThat(pojos[0].id, `is`(test1.stringId))
    }

    @Test(expected = IllegalArgumentException::class)
    @SmallTest
    fun invalidWorkQuery() {
        WorkQuery.Builder.fromStates(listOf()).build()
    }
}
