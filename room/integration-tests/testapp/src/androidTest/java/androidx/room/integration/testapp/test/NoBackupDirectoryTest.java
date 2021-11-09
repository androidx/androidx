/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.integration.testapp.TestDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NoBackupDirectoryTest {

    private static final String NAME = "database.db";

    private Context mContext;
    private SupportSQLiteOpenHelper.Factory mFactory;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mFactory = new AlwaysNoBackupFactory();
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    public void testBuildDatabase_withCustomSupportSQLiteOpenHelper() {
        // Setting the minSdkVersion to 23 even though Context.getNoBackupFilesDir() is supported
        // on API 21+ because it was unused until API 23.
        RoomDatabase database = Room.databaseBuilder(mContext, TestDatabase.class, NAME)
                .openHelperFactory(mFactory)
                .build();

        SupportSQLiteOpenHelper helper = database.getOpenHelper();
        assertThat(helper.getDatabaseName(), is(NAME));
        SupportSQLiteDatabase sqLiteDatabase = helper.getWritableDatabase();
        String expectedPath = new File(mContext.getNoBackupFilesDir(), NAME).getPath();
        assertThat(sqLiteDatabase.getPath(), is(expectedPath));
        database.close();
    }

    @Test
    public void testBuildInMemoryDatabase_withAlwaysNoBackupFactory() {
        Throwable exception = null;
        try {
            RoomDatabase database = Room.inMemoryDatabaseBuilder(mContext, TestDatabase.class)
                    .openHelperFactory(mFactory)
                    .build();
            database.getOpenHelper().getWritableDatabase();
        } catch (Throwable expected) {
            exception = expected;
        }

        assertThat(exception, notNullValue());
        assertThat(exception.getMessage(),
                is("Must set a non-null database name to a configuration that uses the no backup "
                        + "directory."));
    }

    static class AlwaysNoBackupFactory implements SupportSQLiteOpenHelper.Factory {
        private final SupportSQLiteOpenHelper.Factory mDelegate;

        AlwaysNoBackupFactory() {
            mDelegate = new FrameworkSQLiteOpenHelperFactory();
        }

        @NonNull
        @Override
        public SupportSQLiteOpenHelper create(
                @NonNull SupportSQLiteOpenHelper.Configuration configuration) {
            SupportSQLiteOpenHelper.Configuration backupConfiguration =
                    SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
                            .callback(configuration.callback)
                            .name(configuration.name)
                            .noBackupDirectory(true)
                            .build();
            return mDelegate.create(backupConfiguration);
        }
    }
}
