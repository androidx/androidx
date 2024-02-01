/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.room

import androidx.kruth.assertThat
import androidx.sqlite.db.SupportSQLiteStatement
import java.util.concurrent.FutureTask
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(JUnit4::class)
class SharedSQLiteStatementTest {
    private lateinit var mSharedStmt: SharedSQLiteStatement
    lateinit var mDb: RoomDatabase
    @Before
    fun init() {
        val mdata: RoomDatabase = mock()
        whenever(mdata.compileStatement(anyOrNull())).thenAnswer {
            mock<SupportSQLiteStatement>()
        }
        whenever(mdata.invalidationTracker).thenReturn(mock())
        mDb = mdata
        mSharedStmt = object : SharedSQLiteStatement(mdata) {
            override fun createQuery(): String {
                return "foo"
            }
        }
    }

    @Test
    fun checkMainThread() {
        mSharedStmt.acquire()
        verify(mDb).assertNotMainThread()
    }

    @Test
    fun basic() {
        assertThat(mSharedStmt.acquire()).isNotNull()
    }

    @Test
    fun twiceWithoutReleasing() {
            val stmt1 = mSharedStmt.acquire()
            val stmt2 = mSharedStmt.acquire()
            assertThat(stmt1).isNotNull()
            assertThat(stmt2).isNotNull()
            assertThat(stmt1).isNotEqualTo(stmt2)
        }

    @Test
    fun twiceWithReleasing() {
            val stmt1 = mSharedStmt.acquire()
            mSharedStmt.release(stmt1)
            val stmt2 = mSharedStmt.acquire()
            assertThat(stmt1).isNotNull()
            assertThat(stmt1).isEqualTo(stmt2)
        }

    @Test
    fun fromAnotherThreadWhileHolding() {
        val stmt1 = mSharedStmt.acquire()
        val task = FutureTask { mSharedStmt.acquire() }
        Thread(task).start()
        val stmt2 = task.get()
        assertThat(stmt1).isNotNull()
        assertThat(stmt2).isNotNull()
        assertThat(stmt1).isNotEqualTo(stmt2)
    }

    @Test
    fun fromAnotherThreadAfterReleasing() {
        val stmt1 = mSharedStmt.acquire()
        mSharedStmt.release(stmt1)
        val task = FutureTask { mSharedStmt.acquire() }
        Thread(task).start()
        val stmt2 = task.get()
        assertThat(stmt1).isNotNull()
        assertThat(stmt1).isEqualTo(stmt2)
    }
}
