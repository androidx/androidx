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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class DefaultValueTest {

    @SuppressWarnings("unused")
    public static class TimestampConverter {

        private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

        static {
            FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @TypeConverter
        public String toTimestamp(Date date) {
            return FORMAT.format(date);
        }

        @TypeConverter
        public Date fromTimestamp(String timestamp) {
            try {
                return FORMAT.parse(timestamp);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Entity
    public static class Sample {
        @PrimaryKey
        public long id;
        public String name;
        @ColumnInfo(defaultValue = "No description")
        public String description;
        @ColumnInfo(defaultValue = "1")
        public boolean available;
        @ColumnInfo(defaultValue = "0")
        public int serial;
        @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
        @TypeConverters(TimestampConverter.class)
        public Date timestamp;
    }

    public static class NameAndId {
        public long id;
        public String name;

        public NameAndId(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Dao
    public interface SampleDao {
        @Query("INSERT INTO Sample (name) VALUES (:name)")
        long insert(String name);

        @Insert(entity = Sample.class)
        void insertName(NameAndId nameAndId);

        @Query("SELECT * FROM Sample WHERE id = :id")
        Sample byId(long id);
    }

    @Database(entities = {Sample.class}, version = 1, exportSchema = false)
    public abstract static class DefaultValueDatabase extends RoomDatabase {
        public abstract SampleDao dao();
    }

    private DefaultValueDatabase openDatabase() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return Room.inMemoryDatabaseBuilder(context, DefaultValueDatabase.class).build();
    }

    @Test
    public void defaultValues() {
        final DefaultValueDatabase db = openDatabase();
        final long id = db.dao().insert("A");
        final long now = System.currentTimeMillis();
        final Sample sample = db.dao().byId(id);
        assertThat(sample.name, is(equalTo("A")));
        assertThat(sample.description, is(equalTo("No description")));
        assertThat(sample.available, is(true));
        assertThat(sample.serial, is(0));
        assertThat((double) sample.timestamp.getTime(), is(closeTo(now, 3000)));
    }

    @Test
    public void defaultValues_partialEntity() {
        final DefaultValueDatabase db = openDatabase();
        final NameAndId nameAndId = new NameAndId(1, "A");
        db.dao().insertName(nameAndId);
        final long now = System.currentTimeMillis();
        final Sample sample = db.dao().byId(1);
        assertThat(sample.name, is(equalTo("A")));
        assertThat(sample.description, is(equalTo("No description")));
        assertThat(sample.available, is(true));
        assertThat(sample.serial, is(0));
        assertThat((double) sample.timestamp.getTime(), is(closeTo(now, 3000)));
    }
}
