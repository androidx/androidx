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

package androidx.sqlite.inspection.test

import androidx.sqlite.inspection.test.CountingDelegatingExecutorService.Event.FINISHED
import androidx.sqlite.inspection.test.CountingDelegatingExecutorService.Event.STARTED
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.util.concurrent.Executor
import java.util.concurrent.Executors.newCachedThreadPool

@MediumTest
@RunWith(AndroidJUnit4::class)
class CancellationQueryTest {
    private val countingExecutorService = CountingDelegatingExecutorService(newCachedThreadPool())
    @get:Rule
    val environment = SqliteInspectorTestEnvironment(countingExecutorService)

    @get:Rule
    val temporaryFolder = TemporaryFolder(getInstrumentation().context.cacheDir)

    @Test
    fun test_query_cancellations() = runBlocking {
        val db = Database("db", emptyList()).createInstance(temporaryFolder)
        db.enableWriteAheadLogging()
        val databaseId = environment.inspectDatabase(db)
        // very long-running query
        val job = launch(Dispatchers.IO) {
            environment.issueQuery(databaseId, mandelbrotQuery(10000000))
        }
        // check that task with the query is actually started, but there is still no hard guarantee
        // that next query still won't win the race and execute query first.
        assertThat(countingExecutorService.events.receive()).isEqualTo(STARTED)
        // even though we have long running query, other queries aren't blocked
        val result = environment.issueQuery(databaseId, mandelbrotQuery(10))
        // drain events after query
        assertThat(countingExecutorService.events.receive()).isEqualTo(STARTED)
        assertThat(countingExecutorService.events.receive()).isEqualTo(FINISHED)
        assertThat(result.rowsCount).isEqualTo(22)
        assertThat(countingExecutorService.events.tryReceive().getOrNull()).isNull()
        job.cancelAndJoin()
        // check that task finished after cancellation
        assertThat(countingExecutorService.events.receive()).isEqualTo(FINISHED)
    }
}

class CountingDelegatingExecutorService(val executor: Executor) : Executor {
    enum class Event {
        STARTED,
        FINISHED
    }

    private val channel = Channel<Event>(Channel.UNLIMITED)

    val events: ReceiveChannel<Event>
        get() = channel

    override fun execute(command: Runnable) {
        executor.execute {
            channel.trySend(STARTED)
            try {
                command.run()
            } finally {
                channel.trySend(FINISHED)
            }
        }
    }
}

// https://sqlite.org/lang_with.html see "Outlandish Recursive Query"
// language=SQLite
private fun mandelbrotQuery(iterations: Int) = """
    WITH RECURSIVE
      xaxis(x) AS (VALUES(-2.0) UNION ALL SELECT x+0.05 FROM xaxis WHERE x<1.2),
      yaxis(y) AS (VALUES(-1.0) UNION ALL SELECT y+0.1 FROM yaxis WHERE y<1.0),
      m(iter, cx, cy, x, y) AS (
        SELECT 0, x, y, 0.0, 0.0 FROM xaxis, yaxis
        UNION ALL
        SELECT iter+1, cx, cy, x*x-y*y + cx, 2.0*x*y + cy FROM m
         WHERE (x*x + y*y) < 4.0 AND iter<$iterations
      ),
      m2(iter, cx, cy) AS (
        SELECT max(iter), cx, cy FROM m GROUP BY cx, cy
      ),
      a(t) AS (
        SELECT group_concat( substr(' .+*#', 1+min(iter/7,4), 1), '')
        FROM m2 GROUP BY cy
      )
    SELECT * FROM a;
"""