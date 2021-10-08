/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.room.util;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.os.CancellationSignal;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class DBUtilTest {

    @Test
    public void verifyInstanceOfWindowedCursor() {
        SupportSQLiteDatabase db = createDatabase(
                "CREATE TABLE foo (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "name TEXT)");

        Cursor result = db.query("SELECT * FROM foo");

        boolean isWindowedCursor = result instanceof AbstractWindowedCursor;
        assertTrue("SupportSQLiteDatabase should return results that inherit "
                        + "AbstractWindowedCursor. If this is not intended behaviour then this "
                        + "test along with DBUtil#query() should be revisited.",
                isWindowedCursor);
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

    @Test
    @SdkSuppress(minSdkVersion = 16)
    public void createCancellationSignal() {
        CancellationSignal signal = DBUtil.createCancellationSignal();
        assertThat(signal, notNullValue());
    }
}
