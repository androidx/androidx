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

package com.android.support.room;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import android.content.Context;

import com.android.support.db.SupportSQLiteOpenHelper;
import com.android.support.db.framework.FrameworkSQLiteOpenHelperFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BuilderTest {
    @Test(expected = IllegalArgumentException.class)
    public void nullContext() {
        //noinspection ConstantConditions
        Room.databaseBuilder(null, RoomDatabase.class, "bla").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullContext2() {
        //noinspection ConstantConditions
        Room.inMemoryDatabaseBuilder(null, RoomDatabase.class).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullName() {
        //noinspection ConstantConditions
        Room.databaseBuilder(mock(Context.class), RoomDatabase.class, null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyName() {
        Room.databaseBuilder(mock(Context.class), RoomDatabase.class, "  ").build();
    }

    @Test
    public void createBasic() {
        Context context = mock(Context.class);
        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class).build();
        assertThat(db, instanceOf(BuilderTest_TestDatabase_Impl.class));
        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        assertThat(config, notNullValue());
        assertThat(config.context, is(context));
        assertThat(config.name, is(nullValue()));
        assertThat(config.version, is(1));
        assertThat(config.sqliteOpenHelperFactory,
                instanceOf(FrameworkSQLiteOpenHelperFactory.class));
    }

    @Test
    public void createWithFactoryAndVersion() {
        Context context = mock(Context.class);
        SupportSQLiteOpenHelper.Factory factory = mock(SupportSQLiteOpenHelper.Factory.class);

        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                .version(41)
                .openHelperFactory(factory)
                .build();
        assertThat(db, instanceOf(BuilderTest_TestDatabase_Impl.class));
        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        assertThat(config, notNullValue());
        assertThat(config.version, is(41));
        assertThat(config.sqliteOpenHelperFactory, is(factory));
    }

    abstract static class TestDatabase extends RoomDatabase {}
}
