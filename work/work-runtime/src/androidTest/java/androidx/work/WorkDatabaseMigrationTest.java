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

package androidx.work;

import static android.content.Context.MODE_PRIVATE;
import static android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL;

import static androidx.work.impl.WorkDatabaseVersions.VERSION_1;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_10;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_11;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_12;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_14;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_15;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_16;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_17;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_19;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_2;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_20;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_21;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_3;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_4;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_5;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_6;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_7;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_8;
import static androidx.work.impl.WorkDatabaseVersions.VERSION_9;
import static androidx.work.impl.utils.IdGeneratorKt.NEXT_ALARM_MANAGER_ID_KEY;
import static androidx.work.impl.utils.IdGeneratorKt.NEXT_JOB_SCHEDULER_ID_KEY;
import static androidx.work.impl.utils.IdGeneratorKt.PREFERENCE_FILE_KEY;
import static androidx.work.impl.utils.PreferenceUtils.KEY_LAST_CANCEL_ALL_TIME_MS;
import static androidx.work.impl.utils.PreferenceUtils.KEY_RESCHEDULE_NEEDED;
import static androidx.work.impl.utils.PreferenceUtils.PREFERENCES_FILE_NAME;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.Constraints.ContentUriTrigger;
import androidx.work.impl.Migration_11_12;
import androidx.work.impl.Migration_12_13;
import androidx.work.impl.Migration_15_16;
import androidx.work.impl.Migration_16_17;
import androidx.work.impl.Migration_1_2;
import androidx.work.impl.Migration_3_4;
import androidx.work.impl.Migration_4_5;
import androidx.work.impl.Migration_6_7;
import androidx.work.impl.Migration_7_8;
import androidx.work.impl.Migration_8_9;
import androidx.work.impl.RescheduleMigration;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkMigration9To10;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkTypeConverters;
import androidx.work.worker.TestWorker;

import org.hamcrest.Matchers;
import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class WorkDatabaseMigrationTest {

    private static final String TEST_DATABASE = "workdatabase-test";
    private static final boolean VALIDATE_DROPPED_TABLES = true;
    private static final String COLUMN_WORKSPEC_ID = "work_spec_id";
    private static final String COLUMN_SYSTEM_ID = "system_id";
    private static final String COLUMN_ALARM_ID = "alarm_id";
    private static final String COLUMN_RUN_IN_FOREGROUND = "run_in_foreground";
    private static final String COLUMN_OUT_OF_QUOTA_POLICY = "out_of_quota_policy";

    // Queries
    private static final String INSERT_ALARM_INFO = "INSERT INTO alarmInfo VALUES (?, ?)";
    private static final String INSERT_SYSTEM_ID_INFO = "INSERT INTO SystemIdInfo VALUES (?, ?)";
    private static final String CHECK_SYSTEM_ID_INFO = "SELECT * FROM SystemIdInfo";
    private static final String CHECK_ALARM_INFO = "SELECT * FROM alarmInfo";
    private static final String CHECK_TABLE_NAME = "SELECT * FROM %s";
    private static final String CHECK_INDEX = "PRAGMA index_list(%s)";
    private static final String CHECK_TABLE_FIELD = "PRAGMA table_info(%s)";

    private static final String TABLE_ALARM_INFO = "alarmInfo";
    private static final String TABLE_SYSTEM_ID_INFO = "SystemIdInfo";
    private static final String TABLE_WORKSPEC = "WorkSpec";
    private static final String TABLE_WORKTAG = "WorkTag";
    private static final String TABLE_WORKNAME = "WorkName";
    private static final String TABLE_WORKPROGRESS = "WorkProgress";
    private static final String TABLE_PREFERENCE = "Preference";
    private static final String INDEX_PERIOD_START_TIME = "index_WorkSpec_period_start_time";

    private static final String NAME = "name";
    private static final String TRIGGER_CONTENT_UPDATE_DELAY = "trigger_content_update_delay";
    private static final String TRIGGER_MAX_CONTENT_DELAY = "trigger_max_content_delay";
    private static final String REQUIRED_NETWORK_TYPE = "required_network_type";
    private static final String CONTENT_URI_TRIGGERS = "content_uri_triggers";

    private static final String PERIOD_COUNT = "period_count";

    private static final String LAST_ENQUEUE_TIME = "last_enqueue_time";

    private Context mContext;
    private File mDatabasePath;

    @Rule
    public MigrationTestHelper mMigrationTestHelper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), WorkDatabase.class);

    @Before
    public void setUp() {
        // Delete the database if it exists.
        mContext = ApplicationProvider.getApplicationContext();
        mDatabasePath = ApplicationProvider.getApplicationContext().getDatabasePath(TEST_DATABASE);
        if (mDatabasePath.exists()) {
            mDatabasePath.delete();
        }
    }

    @Test
    @MediumTest
    public void testMigrationVersion1To2() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_1);

        String[] prepopulatedWorkSpecIds = new String[]{
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        };
        for (String workSpecId : prepopulatedWorkSpecIds) {
            ContentValues contentValues = contentValuesPre8(workSpecId);
            database.insert("workspec", CONFLICT_FAIL, contentValues);

            if (workSpecId.equals(prepopulatedWorkSpecIds[0])) {
                ContentValues tagValues = new ContentValues();
                tagValues.put("tag", TestWorker.class.getName());
                tagValues.put("work_spec_id", workSpecId);
                database.insert("worktag", CONFLICT_FAIL, tagValues);
            }
        }

        String workSpecId1 = UUID.randomUUID().toString();
        String workSpecId2 = UUID.randomUUID().toString();

        // insert alarmInfos
        database.execSQL(INSERT_ALARM_INFO, new Object[]{workSpecId1, 1});
        database.execSQL(INSERT_ALARM_INFO, new Object[]{workSpecId2, 2});

        database.close();

        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_2,
                VALIDATE_DROPPED_TABLES,
                Migration_1_2.INSTANCE);

        Cursor tagCursor = database.query("SELECT * FROM worktag");
        assertThat(tagCursor.getCount(), is(prepopulatedWorkSpecIds.length));
        boolean[] foundWorkSpecId = new boolean[prepopulatedWorkSpecIds.length];
        for (int i = 0; i < prepopulatedWorkSpecIds.length; ++i) {
            tagCursor.moveToPosition(i);
            assertThat(tagCursor.getString(tagCursor.getColumnIndex("tag")),
                    is(TestWorker.class.getName()));
            String currentId = tagCursor.getString(tagCursor.getColumnIndex("work_spec_id"));
            for (int j = 0; j < prepopulatedWorkSpecIds.length; ++j) {
                if (prepopulatedWorkSpecIds[j].equals(currentId)) {
                    foundWorkSpecId[j] = true;
                    break;
                }
            }
        }
        for (int i = 0; i < prepopulatedWorkSpecIds.length; ++i) {
            assertThat(foundWorkSpecId[i], is(true));
        }
        tagCursor.close();

        Cursor cursor = database.query(CHECK_SYSTEM_ID_INFO);
        assertThat(cursor.getCount(), is(2));
        cursor.moveToFirst();
        assertThat(cursor.getString(cursor.getColumnIndex(COLUMN_WORKSPEC_ID)), is(workSpecId1));
        assertThat(cursor.getInt(cursor.getColumnIndex(COLUMN_SYSTEM_ID)), is(1));
        cursor.moveToNext();
        assertThat(cursor.getString(cursor.getColumnIndex(COLUMN_WORKSPEC_ID)), is(workSpecId2));
        assertThat(cursor.getInt(cursor.getColumnIndex(COLUMN_SYSTEM_ID)), is(2));
        cursor.close();

        assertThat(checkExists(database, TABLE_ALARM_INFO), is(false));
        assertThat(checkExists(database, TABLE_WORKSPEC), is(true));
        assertThat(checkExists(database, TABLE_WORKTAG), is(true));
        assertThat(checkExists(database, TABLE_WORKNAME), is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion2To3() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_2);
        RescheduleMigration migration2To3 =
                new RescheduleMigration(mContext, VERSION_2, VERSION_3);

        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_3,
                VALIDATE_DROPPED_TABLES,
                migration2To3);

        SharedPreferences sharedPreferences =
                mContext.getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);
        assertThat(sharedPreferences.getBoolean(KEY_RESCHEDULE_NEEDED, false), is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion3To4() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_3);

        String oneTimeWorkSpecId = UUID.randomUUID().toString();
        long scheduleRequestedAt = System.currentTimeMillis();
        ContentValues oneTimeWorkSpecContentValues = contentValuesPre8(oneTimeWorkSpecId);
        oneTimeWorkSpecContentValues.put("schedule_requested_at", scheduleRequestedAt);

        String periodicWorkSpecId = UUID.randomUUID().toString();
        ContentValues periodicWorkSpecContentValues = contentValuesPre8(periodicWorkSpecId);
        periodicWorkSpecContentValues.put("interval_duration", 15 * 60 * 1000L);

        database.insert("workspec", CONFLICT_FAIL, oneTimeWorkSpecContentValues);
        database.insert("workspec", CONFLICT_FAIL, periodicWorkSpecContentValues);

        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_4,
                VALIDATE_DROPPED_TABLES,
                Migration_3_4.INSTANCE);

        Cursor cursor = database.query("SELECT * from workspec");
        assertThat(cursor.getCount(), is(2));
        cursor.moveToFirst();
        assertThat(cursor.getString(cursor.getColumnIndex("id")),
                is(oneTimeWorkSpecId));
        assertThat(cursor.getLong(cursor.getColumnIndex("schedule_requested_at")),
                is(scheduleRequestedAt));
        cursor.moveToNext();
        assertThat(cursor.getString(cursor.getColumnIndex("id")),
                is(periodicWorkSpecId));
        assertThat(cursor.getLong(cursor.getColumnIndex("schedule_requested_at")),
                is(0L));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion4To5() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_4);
        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_5,
                VALIDATE_DROPPED_TABLES,
                Migration_4_5.INSTANCE);
        assertThat(checkExists(database, TABLE_WORKSPEC), is(true));
        assertThat(
                checkColumnExists(database, TABLE_WORKSPEC, TRIGGER_CONTENT_UPDATE_DELAY),
                is(true));
        assertThat(
                checkColumnExists(database, TABLE_WORKSPEC, TRIGGER_MAX_CONTENT_DELAY),
                is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion5To6() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_5);
        RescheduleMigration migration5To6 = new RescheduleMigration(mContext, VERSION_5, VERSION_6);

        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_6,
                VALIDATE_DROPPED_TABLES,
                migration5To6);

        SharedPreferences sharedPreferences =
                mContext.getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);
        assertThat(sharedPreferences.getBoolean(KEY_RESCHEDULE_NEEDED, false), is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion6To7() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_6);
        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_7,
                VALIDATE_DROPPED_TABLES,
                Migration_6_7.INSTANCE);
        assertThat(checkExists(database, TABLE_WORKPROGRESS), is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion7To8() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_7);
        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_8,
                VALIDATE_DROPPED_TABLES,
                Migration_7_8.INSTANCE);

        assertThat(checkIndexExists(database, INDEX_PERIOD_START_TIME, TABLE_WORKSPEC), is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion8To9() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_8);
        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_9,
                VALIDATE_DROPPED_TABLES,
                Migration_8_9.INSTANCE);

        assertThat(checkColumnExists(database, TABLE_WORKSPEC, COLUMN_RUN_IN_FOREGROUND),
                is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion9To10() throws IOException {
        long lastCancelTimeMillis = 1L;
        int nextJobSchedulerId = 10;
        int nextAlarmId = 20;
        // Setup
        mContext.getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_CANCEL_ALL_TIME_MS, lastCancelTimeMillis)
                .putBoolean(KEY_RESCHEDULE_NEEDED, true)
                .apply();

        mContext.getSharedPreferences(PREFERENCE_FILE_KEY, MODE_PRIVATE)
                .edit()
                .putInt(NEXT_JOB_SCHEDULER_ID_KEY, nextJobSchedulerId)
                .putInt(NEXT_ALARM_MANAGER_ID_KEY, nextAlarmId)
                .apply();

        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_9);
        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_10,
                VALIDATE_DROPPED_TABLES,
                new WorkMigration9To10(mContext));

        assertThat(checkExists(database, TABLE_PREFERENCE), is(true));
        String query = "SELECT * FROM `Preference` where `key`=@key";
        String[] keys = new String[]{
                KEY_RESCHEDULE_NEEDED,
                KEY_LAST_CANCEL_ALL_TIME_MS,
                NEXT_JOB_SCHEDULER_ID_KEY,
                NEXT_ALARM_MANAGER_ID_KEY
        };
        long[] expectedValues = new long[]{
                1L,
                lastCancelTimeMillis,
                nextJobSchedulerId,
                nextAlarmId
        };
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            long expected = expectedValues[i];
            Cursor cursor = database.query(query, new Object[]{key});
            assertThat(cursor.getCount(), is(1));
            cursor.moveToFirst();
            assertThat(cursor.getLong(cursor.getColumnIndex("long_value")), is(expected));
            cursor.close();
        }
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion10To11() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_10);
        RescheduleMigration migration10To11 =
                new RescheduleMigration(mContext, VERSION_10, VERSION_11);
        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_11,
                VALIDATE_DROPPED_TABLES,
                migration10To11);

        String[] keys = new String[]{
                KEY_RESCHEDULE_NEEDED,
        };
        long[] expectedValues = new long[]{
                1L,
        };

        String query = "SELECT * FROM `Preference` where `key`=@key";
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            long expected = expectedValues[i];
            Cursor cursor = database.query(query, new Object[]{key});
            assertThat(cursor.getCount(), is(1));
            cursor.moveToFirst();
            assertThat(cursor.getLong(cursor.getColumnIndex("long_value")), is(expected));
            cursor.close();
        }
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion11To12() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_11);
        database = mMigrationTestHelper.runMigrationsAndValidate(
                TEST_DATABASE,
                VERSION_12,
                VALIDATE_DROPPED_TABLES,
                Migration_11_12.INSTANCE);

        assertThat(checkColumnExists(database, TABLE_WORKSPEC, COLUMN_OUT_OF_QUOTA_POLICY),
                is(true));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion12To14Network() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_12);
        String nullNetworkTypeRequestId = UUID.randomUUID().toString();
        ContentValues nullRequiredNetworkTypeRequest = contentValuesPre15(nullNetworkTypeRequestId);
        nullRequiredNetworkTypeRequest.put(REQUIRED_NETWORK_TYPE, (String) null);

        String connectedRequestId = UUID.randomUUID().toString();
        ContentValues connectedRequest = contentValuesPre15(connectedRequestId);
        connectedRequest.put(REQUIRED_NETWORK_TYPE, 1);

        database.insert("workspec", CONFLICT_FAIL, nullRequiredNetworkTypeRequest);
        database.insert("workspec", CONFLICT_FAIL, connectedRequest);

        mMigrationTestHelper.runMigrationsAndValidate(TEST_DATABASE, VERSION_14,
                true, Migration_12_13.INSTANCE);

        assertThat(queryRequiredNetworkType(database, nullNetworkTypeRequestId), is(0));
        assertThat(queryRequiredNetworkType(database, connectedRequestId), is(1));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion12To14NetworkContentUris() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_12);
        String nullContentUrisId = UUID.randomUUID().toString();
        ContentValues nullContentUris = contentValuesPre15(nullContentUrisId);
        nullContentUris.put(CONTENT_URI_TRIGGERS, (String) null);

        String contentUrisId = UUID.randomUUID().toString();
        Set<ContentUriTrigger> triggers = new HashSet<>();
        triggers.add(new ContentUriTrigger(Uri.parse("http://cs.android.com"), false));
        ContentValues contentUrisRequest = contentValuesPre15(contentUrisId);
        contentUrisRequest.put(CONTENT_URI_TRIGGERS,
                WorkTypeConverters.setOfTriggersToByteArray(triggers));

        database.insert("workspec", CONFLICT_FAIL, nullContentUris);
        database.insert("workspec", CONFLICT_FAIL, contentUrisRequest);

        mMigrationTestHelper.runMigrationsAndValidate(TEST_DATABASE, VERSION_14,
                true, Migration_12_13.INSTANCE);

        assertThat(queryContentUris(database, nullContentUrisId).size(), is(0));
        assertThat(queryContentUris(database, contentUrisId), is(triggers));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion14To15EnqueueTime() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_14);

        String firstPeriodId = UUID.randomUUID().toString();
        ContentValues firstPeriod = contentValuesPre15(firstPeriodId);
        firstPeriod.put("period_start_time", 0L);
        firstPeriod.put("interval_duration", 1000L);
        database.insert("workspec", CONFLICT_FAIL, firstPeriod);

        String secondPeriodId = UUID.randomUUID().toString();
        ContentValues secondPeriod = contentValuesPre15(secondPeriodId);
        secondPeriod.put("period_start_time", 1000L);
        secondPeriod.put("interval_duration", 1000L);
        database.insert("workspec", CONFLICT_FAIL, secondPeriod);

        String oneTimeId = UUID.randomUUID().toString();
        ContentValues oneTime = contentValuesPre15(oneTimeId);
        // could be the case if work is blocked by another one
        oneTime.put("period_start_time", 0L);
        oneTime.put("interval_duration", 0L);
        database.insert("workspec", CONFLICT_FAIL, oneTime);
        long beforeMigration = System.currentTimeMillis();
        mMigrationTestHelper.runMigrationsAndValidate(TEST_DATABASE, VERSION_15, true);
        HashMap<String, Long> enqueueTimes = new HashMap<>();
        HashMap<String, Integer> periodCounts = new HashMap<>();
        try (Cursor cursor = database.query(
                "SELECT id, last_enqueue_time, period_count FROM workspec")) {
            int idColumn = cursor.getColumnIndex("id");
            int lastEnqueueTimeColumn = cursor.getColumnIndex("last_enqueue_time");
            int periodCount = cursor.getColumnIndex("period_count");
            while (cursor.moveToNext()) {
                String id = cursor.getString(idColumn);
                enqueueTimes.put(id, cursor.getLong(lastEnqueueTimeColumn));
                periodCounts.put(id, cursor.getInt(periodCount));
            }
        }
        assertThat(enqueueTimes.get(oneTimeId), is(0L));
        assertThat(enqueueTimes.get(firstPeriodId), Matchers.greaterThan(beforeMigration));
        assertThat(enqueueTimes.get(secondPeriodId), is(1000L));

        assertThat(periodCounts.get(oneTimeId), is(0));
        assertThat(periodCounts.get(firstPeriodId), is(0));
        assertThat(periodCounts.get(secondPeriodId), is(1));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion15_16() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_15);
        database.execSQL("PRAGMA foreign_keys = FALSE");
        String id = UUID.randomUUID().toString();
        ContentValues values = contentValuesPre16(id);
        database.insert("workspec", CONFLICT_FAIL, values);
        ContentValues existingSystemIdInfo = new ContentValues();
        existingSystemIdInfo.put("system_id", 1);
        existingSystemIdInfo.put("work_spec_id", id);
        database.insert("SystemIdInfo", CONFLICT_FAIL, existingSystemIdInfo);
        ContentValues nonExistingSystemIdInfo = new ContentValues();
        nonExistingSystemIdInfo.put("system_id", 2);
        nonExistingSystemIdInfo.put("work_spec_id", "aaaaabbbb");
        database.insert("SystemIdInfo", CONFLICT_FAIL, nonExistingSystemIdInfo);
        mMigrationTestHelper.runMigrationsAndValidate(TEST_DATABASE, VERSION_16, true,
                Migration_15_16.INSTANCE);
        checkColumnExists(database, "workspec", "generation");
        Cursor systemIdInfos = database.query("SELECT system_id, work_spec_id FROM SystemIdInfo");
        assertThat(systemIdInfos.getCount(), is(1));
        assertThat(systemIdInfos.moveToFirst(), is(true));

        assertThat(systemIdInfos.getString(systemIdInfos.getColumnIndex("work_spec_id")),
                is(id));
        Cursor cursor = database.query("PRAGMA foreign_key_check(`SystemIdInfo`)");
        if (cursor.getCount() > 0) {
            throw new AssertionError("failed check");
        }
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion16_17() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_16);
        String idOne = UUID.randomUUID().toString();
        String idTwo = UUID.randomUUID().toString();
        ContentValues valuesOne = contentValuesPre16(idOne);
        valuesOne.remove("input_merger_class_name");
        database.insert("workspec", CONFLICT_FAIL, valuesOne);
        ContentValues valuesTwo = contentValuesPre16(idTwo);
        database.insert("workspec", CONFLICT_FAIL, valuesTwo);
        mMigrationTestHelper.runMigrationsAndValidate(TEST_DATABASE, VERSION_17, true,
                Migration_16_17.INSTANCE);
        Cursor workSpecs = database.query("SELECT id, input_merger_class_name FROM WorkSpec");
        assertThat(workSpecs.getCount(), is(2));
        assertThat(workSpecs.moveToNext(), is(true));
        assertThat(workSpecs.getString(workSpecs.getColumnIndex("id")), is(idOne));
        assertThat(workSpecs.getString(workSpecs.getColumnIndex("input_merger_class_name")),
                is(OverwritingInputMerger.class.getName()));
        assertThat(workSpecs.moveToNext(), is(true));
        assertThat(workSpecs.getString(workSpecs.getColumnIndex("id")), is(idTwo));
        assertThat(workSpecs.getString(workSpecs.getColumnIndex("input_merger_class_name")),
                is(OverwritingInputMerger.class.getName()));
        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion19_20() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_19);
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();

        ContentValues values1 = contentValuesPre16(id1);
        ContentValues values2 = contentValuesPre16(id2);
        values2.put(LAST_ENQUEUE_TIME, 500L);
        database.insert("workspec", CONFLICT_FAIL, values1);
        database.insert("workspec", CONFLICT_FAIL, values2);
        mMigrationTestHelper.runMigrationsAndValidate(TEST_DATABASE, VERSION_20, true);
        Cursor workSpecs = database.query("SELECT id, last_enqueue_time FROM WorkSpec");
        assertThat(workSpecs.getCount(), is(2));
        assertThat(workSpecs.moveToNext(), is(true));
        assertThat(workSpecs.getString(workSpecs.getColumnIndex("id")), is(id1));
        assertThat(workSpecs.getLong(workSpecs.getColumnIndex("last_enqueue_time")),
                is(-1L));
        assertThat(workSpecs.moveToNext(), is(true));
        assertThat(workSpecs.getString(workSpecs.getColumnIndex("id")), is(id2));
        assertThat(workSpecs.getLong(workSpecs.getColumnIndex("last_enqueue_time")),
                is(500L));

        database.close();
    }

    @Test
    @MediumTest
    public void testMigrationVersion20_21() throws IOException {
        SupportSQLiteDatabase database =
                mMigrationTestHelper.createDatabase(TEST_DATABASE, VERSION_20);

        String id = UUID.randomUUID().toString();
        database.insert("workspec", CONFLICT_FAIL, contentValuesPre20(id));
        mMigrationTestHelper.runMigrationsAndValidate(TEST_DATABASE, VERSION_21, true);
        Cursor workSpecs = database.query("SELECT id, required_network_request FROM WorkSpec");
        assertThat(workSpecs.getCount(), is(1));
        assertThat(workSpecs.moveToNext(), is(true));
        assertThat(workSpecs.getString(workSpecs.getColumnIndex("id")), is(id));
        byte[] networkRequest = workSpecs.getBlob(
                workSpecs.getColumnIndex("required_network_request"));
        assertThat(networkRequest.length, is(0));
    }

    // doesn't have COLUMN_RUN_IN_FOREGROUND
    private @NonNull ContentValues contentValuesPre8(String workSpecId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("id", workSpecId);
        contentValues.put("state", WorkTypeConverters.StateIds.ENQUEUED);
        contentValues.put("worker_class_name", TestWorker.class.getName());
        contentValues.put("input_merger_class_name", OverwritingInputMerger.class.getName());
        contentValues.put("input", Data.EMPTY.toByteArray());
        contentValues.put("output", Data.EMPTY.toByteArray());
        contentValues.put("initial_delay", 0L);
        contentValues.put("interval_duration", 0L);
        contentValues.put("flex_duration", 0L);
        contentValues.put("required_network_type", false);
        contentValues.put("requires_charging", false);
        contentValues.put("requires_device_idle", false);
        contentValues.put("requires_battery_not_low", false);
        contentValues.put("requires_storage_not_low", false);
        contentValues.put("content_uri_triggers", new byte[0]);
        contentValues.put("run_attempt_count", 0);
        contentValues.put("backoff_policy",
                WorkTypeConverters.backoffPolicyToInt(BackoffPolicy.EXPONENTIAL));
        contentValues.put("backoff_delay_duration", WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS);
        // pre 14 last_enqueue_time was called period_start_time
        contentValues.put("period_start_time", 0L);
        contentValues.put("minimum_retention_duration", 0L);
        contentValues.put("schedule_requested_at", WorkSpec.SCHEDULE_NOT_REQUESTED_YET);
        return contentValues;
    }

    private ContentValues contentValuesPre15(String workSpecId) {
        ContentValues contentValues = contentValuesPre8(workSpecId);
        contentValues.put(REQUIRED_NETWORK_TYPE, 0);
        contentValues.put(COLUMN_RUN_IN_FOREGROUND, false);
        contentValues.put(COLUMN_OUT_OF_QUOTA_POLICY, 0);
        contentValues.put(TRIGGER_CONTENT_UPDATE_DELAY, -1);
        contentValues.put(TRIGGER_MAX_CONTENT_DELAY, -1);
        return contentValues;
    }

    private ContentValues contentValuesPre16(String workSpecId) {
        ContentValues contentValues = contentValuesPre15(workSpecId);
        contentValues.remove("period_start_time");
        contentValues.put(LAST_ENQUEUE_TIME, 0L);
        return contentValues;
    }

    private ContentValues contentValuesPre20(String workSpecId) {
        ContentValues contentValues = contentValuesPre16(workSpecId);
        contentValues.put(LAST_ENQUEUE_TIME, -1L);
        return contentValues;
    }

    private boolean checkExists(SupportSQLiteDatabase database, String tableName) {
        Cursor cursor = null;
        try {
            cursor = database.query(String.format(CHECK_TABLE_NAME, tableName));
            return true;
        } catch (SQLiteException ignored) {
            // Should fail with a SQLiteException (no such table: tableName)
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean checkIndexExists(
            @NonNull SupportSQLiteDatabase database,
            @NonNull String indexName,
            @NonNull String tableName) {

        Cursor cursor = null;
        try {
            cursor = database.query(String.format(CHECK_INDEX, tableName));
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                // https://www.sqlite.org/pragma.html#pragma_table_info
                // Columns are: (seq, name, uniq)
                String name = cursor.getString(cursor.getColumnIndex(NAME));
                if (indexName.equals(name)) {
                    return true;
                }
                cursor.moveToNext();
            }
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean checkColumnExists(
            SupportSQLiteDatabase database,
            String tableName,
            String columnName) {

        Cursor cursor = null;
        try {
            cursor = database.query(String.format(CHECK_TABLE_FIELD, tableName));
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                // https://www.sqlite.org/pragma.html#pragma_table_info
                // Columns are: (cid, name, type, notnull, dfit_value, pk)
                String name = cursor.getString(cursor.getColumnIndex(NAME));
                if (columnName.equals(name)) {
                    return true;
                }
                cursor.moveToNext();
            }
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static int queryRequiredNetworkType(SupportSQLiteDatabase db, String workSpecId) {
        Cursor migratedNull = db.query(
                "SELECT required_network_type FROM workspec where id = ?",
                new Object[]{workSpecId});
        migratedNull.moveToFirst();
        int networkType = migratedNull.getInt(migratedNull.getColumnIndex(REQUIRED_NETWORK_TYPE));
        migratedNull.close();
        return networkType;
    }

    private static Set<ContentUriTrigger> queryContentUris(
            SupportSQLiteDatabase db, String workSpecId) {
        Cursor migratedNull = db.query(
                "SELECT content_uri_triggers FROM workspec where id = ?",
                new Object[]{workSpecId});
        migratedNull.moveToFirst();
        byte[] blob = migratedNull.getBlob(migratedNull.getColumnIndex(CONTENT_URI_TRIGGERS));
        migratedNull.close();
        return WorkTypeConverters.byteArrayToSetOfTriggers(blob);
    }
}
