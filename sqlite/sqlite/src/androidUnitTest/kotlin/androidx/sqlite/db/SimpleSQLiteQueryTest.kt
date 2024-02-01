/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.sqlite.db
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
@RunWith(JUnit4::class)
class SimpleSQLiteQueryTest {
    @Test
    fun sql() {
        val query = SimpleSQLiteQuery("foo", emptyArray())
        assertThat(query.sql, `is`("foo"))
    }
    @Test
    fun bindTo_noArgs() {
        val query = SimpleSQLiteQuery("foo", emptyArray())
        val program: SupportSQLiteProgram = Mockito.mock(SupportSQLiteProgram::class.java)
        query.bindTo(program)
        verifyNoMoreInteractions(program)
    }
    @Test
    fun bindTo_withArgs() {
        val bytes = ByteArray(3)
        val query = SimpleSQLiteQuery("foo", arrayOf("bar", 2, true, 0.5f, null, bytes))
        val program: SupportSQLiteProgram = Mockito.mock(SupportSQLiteProgram::class.java)
        query.bindTo(program)
        verify(program).bindString(1, "bar")
        verify(program).bindLong(2, 2)
        verify(program).bindLong(3, 1)
        verify(program).bindDouble(
            4,
            (0.5f).toDouble()
        )
        verify(program).bindNull(5)
        verify(program).bindBlob(6, bytes)
        verifyNoMoreInteractions(program)
    }
    @Test
    fun argCount_withArgs() {
        val query = SimpleSQLiteQuery("foo", arrayOf("bar", 2, true))
        assertThat(query.argCount, `is`(3))
    }
    @Test
    fun argCount_noArgs() {
        val query = SimpleSQLiteQuery("foo", emptyArray())
        assertThat(query.argCount, `is`(0))
    }
}
