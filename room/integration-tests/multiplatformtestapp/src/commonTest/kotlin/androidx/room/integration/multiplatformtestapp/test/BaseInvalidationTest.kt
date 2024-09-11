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

package androidx.room.integration.multiplatformtestapp.test

import androidx.kruth.assertThat
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseInvalidationTest {

    private lateinit var db: SampleDatabase

    abstract fun getRoomDatabase(): SampleDatabase

    @BeforeTest
    fun before() {
        db = getRoomDatabase()
    }

    @AfterTest
    fun after() {
        db.close()
    }

    @Test
    fun observeOneTable() = runTest {
        val dao = db.dao()

        val tableName = SampleEntity::class.simpleName!!
        val invalidations = Channel<Set<String>>(capacity = 10)
        val collectJob =
            backgroundScope.launch(Dispatchers.IO) {
                db.invalidationTracker.createFlow(tableName).collect { invalidatedTables ->
                    invalidations.send(invalidatedTables)
                }
            }

        // Initial emission
        assertThat(invalidations.receive()).containsExactly(tableName)

        dao.insertItem(1)

        // Emissions due to insert
        assertThat(invalidations.receive()).containsExactly(tableName)

        collectJob.cancelAndJoin()

        dao.insertItem(2)

        // No emissions, flow collection canceled
        assertThat(invalidations.isEmpty).isTrue()
    }
}
