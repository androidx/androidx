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

package androidx.room.migration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.room.util.ViewInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ViewInfoTest {

    private SupportSQLiteDatabase mDb;

    @Test
    public void readSimple() {
        mDb = createDatabase(
                "CREATE TABLE foo (id INTEGER PRIMARY KEY, name TEXT)",
                "CREATE VIEW bar AS SELECT id, name FROM foo");
        ViewInfo info = ViewInfo.read(mDb, "bar");
        assertThat(info.name, is(equalTo("bar")));
        assertThat(info.sql, is(equalTo("CREATE VIEW bar AS SELECT id, name FROM foo")));
    }

    @Test
    public void notExisting() {
        mDb = createDatabase(
                "CREATE TABLE foo (id INTEGER PRIMARY KEY, name TEXT)");
        ViewInfo info = ViewInfo.read(mDb, "bar");
        assertThat(info.name, is(equalTo("bar")));
        assertThat(info.sql, is(nullValue()));
    }

    @Test
    public void infoEquals() {
        ViewInfo a = new ViewInfo("a", "a");
        ViewInfo b = new ViewInfo("a", null);
        ViewInfo c = new ViewInfo("a", "a");
        assertThat(a, is(not(equalTo(b))));
        assertThat(a, is(equalTo(c)));
    }

    @After
    public void closeDb() throws IOException {
        if (mDb != null && mDb.isOpen()) {
            mDb.close();
        }
    }

    private static SupportSQLiteDatabase createDatabase(final String... queries) {
        return new FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration
                        .builder(ApplicationProvider.getApplicationContext())
                        .name(null)
                        .callback(new SupportSQLiteOpenHelper.Callback(1) {
                            @Override
                            public void onCreate(SupportSQLiteDatabase db) {
                                for (String query : queries) {
                                    db.execSQL(query);
                                }
                            }

                            @Override
                            public void onUpgrade(SupportSQLiteDatabase db, int oldVersion,
                                    int newVersion) {
                                throw new IllegalStateException("should not be upgrading");
                            }
                        }).build()
        ).getWritableDatabase();
    }
}
