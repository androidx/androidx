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

import static androidx.room.integration.testapp.test.BoxedPrimitivesTest.BaseBoxed.DEFAULT_BOOLEAN_VALUE;
import static androidx.room.integration.testapp.test.BoxedPrimitivesTest.BaseBoxed.DEFAULT_NUMBER_VALUE;

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
import androidx.room.RoomWarnings;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.google.common.truth.Truth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class BoxedPrimitivesTest {
    private BoxingTestDatabase mDb;

    @Before
    public void initDb() {
        Context context = ApplicationProvider.getApplicationContext();
        mDb = Room.inMemoryDatabaseBuilder(
                context,
                BoxingTestDatabase.class)
                .build();
    }

    @After
    public void closeDb() {
        mDb.close();
    }

    @Test
    public void unboxedConstructor() {
        test(mDb.constructor());
    }

    @Test
    public void unboxedGetter() {
        test(mDb.field());
    }

    /**
     * Assert that if a nullable pojo is provided, we can actually insert/read null
     */
    private void test(BaseDao<? extends BaseBoxed> dao) {
        long rowId = dao.insert(new BoxedBooleanHolder(null, null));
        BaseBoxed read = dao.find(rowId);
        Truth.assertThat(read.mFlag).isNull();
        Truth.assertThat(read.mNumber).isNull();
    }

    @Test
    public void testInsert_constructor() {
        long rowId = mDb.constructor().insertTyped(new ConstructorEntity(null, null));
        testInsertedAsEntity(mDb.constructor(), rowId);
    }

    @Test
    public void testInsert_field() {
        long rowId = mDb.field().insertTyped(new FieldEntity());
        testInsertedAsEntity(mDb.field(), rowId);
    }

    /**
     * assert the case where row was inserted via entity hence we should've read the default values
     * already.
     */
    private void testInsertedAsEntity(BaseDao<? extends BaseBoxed> dao, long rowId) {
        BaseBoxed read = dao.find(rowId);
        // default getter value
        Truth.assertThat(read.mFlag).isEqualTo(DEFAULT_BOOLEAN_VALUE);
        Truth.assertThat(read.mNumber).isEqualTo(DEFAULT_NUMBER_VALUE);
    }

    static class BaseBoxed {
        @ColumnInfo(name = "boxed_bool")
        Boolean mFlag;
        @ColumnInfo(name = "boxed_int")
        Integer mNumber;

        static final boolean DEFAULT_BOOLEAN_VALUE = true;
        static final int DEFAULT_NUMBER_VALUE = 41;

        @SuppressWarnings("unused")
        boolean getFlag() {
            return mFlag == null ? DEFAULT_BOOLEAN_VALUE : mFlag;
        }

        @SuppressWarnings("unused")
        int getNumber() {
            return mNumber == null ? DEFAULT_NUMBER_VALUE : mNumber;
        }
    }

    @Entity
    @SuppressWarnings(RoomWarnings.MISMATCHED_GETTER)
    static class ConstructorEntity extends BaseBoxed {
        @PrimaryKey(autoGenerate = true)
        public long rowId = 0;

        ConstructorEntity(Boolean flag, Integer number) {
            this.mFlag = flag;
            this.mNumber = number;
        }
    }

    @Entity
    @SuppressWarnings({RoomWarnings.MISMATCHED_GETTER, RoomWarnings.MISMATCHED_SETTER})
    static class FieldEntity extends BaseBoxed {
        @PrimaryKey(autoGenerate = true)
        public long rowId = 0;

        void setBoxed(Boolean boxed) {
            this.mFlag = boxed;
        }

        void setNumber(Integer number) {
            this.mNumber = number;
        }
    }

    static class BoxedBooleanHolder {
        @ColumnInfo(name = "boxed_bool")
        final Boolean mFlag;
        @ColumnInfo(name = "boxed_int")
        final Integer mNumber;

        BoxedBooleanHolder(Boolean flag, Integer number) {
            mFlag = flag;
            mNumber = number;
        }
    }

    interface BaseDao<T> {
        long insert(BoxedBooleanHolder t);

        @Insert
        long insertTyped(T t);

        T find(long rowId);
    }

    @Dao
    interface BoxedConstructorDao extends BaseDao<ConstructorEntity> {
        @Override
        @Insert(entity = ConstructorEntity.class)
        long insert(BoxedBooleanHolder item);

        @Override
        @Query("SELECT * FROM ConstructorEntity WHERE rowId = :rowId")
        ConstructorEntity find(long rowId);
    }

    @Dao
    interface BoxedFieldDao extends BaseDao<FieldEntity> {

        @Insert(entity = FieldEntity.class)
        long insert(BoxedBooleanHolder item);

        @Override
        @Query("SELECT * FROM FieldEntity WHERE rowId = :rowId")
        FieldEntity find(long rowId);
    }

    @Database(
            entities = {FieldEntity.class, ConstructorEntity.class},
            version = 1,
            exportSchema = false
    )
    abstract static class BoxingTestDatabase extends RoomDatabase {
        abstract BoxedConstructorDao constructor();

        abstract BoxedFieldDao field();
    }
}
