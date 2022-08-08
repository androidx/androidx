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

package androidx.sqlite.db;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public class SimpleSQLiteQueryTestTest {

    @Test
    public void getSql() {
        SimpleSQLiteQuery query = new SimpleSQLiteQuery("foo");
        assertThat(query.getSql(), is("foo"));
    }

    @Test
    public void bindTo_noArgs() {
        SimpleSQLiteQuery query = new SimpleSQLiteQuery("foo");
        SupportSQLiteProgram program = Mockito.mock(SupportSQLiteProgram.class);
        query.bindTo(program);
        verifyNoMoreInteractions(program);
    }

    @Test
    public void bindTo_withArgs() {
        byte[] bytes = new byte[3];
        SimpleSQLiteQuery query = new SimpleSQLiteQuery("foo",
                new Object[]{"bar", 2, true, .5f, null, bytes});
        SupportSQLiteProgram program = Mockito.mock(SupportSQLiteProgram.class);
        query.bindTo(program);
        verify(program).bindString(1, "bar");
        verify(program).bindLong(2, 2);
        verify(program).bindLong(3, 1);
        verify(program).bindDouble(4, .5f);
        verify(program).bindNull(5);
        verify(program).bindBlob(6, bytes);
        verifyNoMoreInteractions(program);
    }

    @Test
    public void getArgCount_withArgs() {
        SimpleSQLiteQuery query = new SimpleSQLiteQuery("foo",
                new Object[]{"bar", 2, true});
        assertThat(query.getArgCount(), is(3));
    }

    @Test
    public void getArgCount_noArgs() {
        SimpleSQLiteQuery query = new SimpleSQLiteQuery("foo");
        assertThat(query.getArgCount(), is(0));
    }
}
