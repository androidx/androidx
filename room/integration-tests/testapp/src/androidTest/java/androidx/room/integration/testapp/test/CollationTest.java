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

import static org.hamcrest.MatcherAssert.assertThat;

import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class CollationTest {
    private CollateDb mDb;
    private CollateDao mDao;
    private Locale mDefaultLocale;
    private final CollateEntity mItem1 = new CollateEntity(1, "abı");
    private final CollateEntity mItem2 = new CollateEntity(2, "abi");
    private final CollateEntity mItem3 = new CollateEntity(3, "abj");
    private final CollateEntity mItem4 = new CollateEntity(4, "abç");

    @Before
    public void init() {
        mDefaultLocale = Locale.getDefault();
    }

    private void initDao(Locale systemLocale) {
        Locale.setDefault(systemLocale);
        mDb = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
                CollateDb.class).build();
        mDao = mDb.dao();
        mDao.insert(mItem1);
        mDao.insert(mItem2);
        mDao.insert(mItem3);
        mDao.insert(mItem4);
    }

    @After
    public void closeDb() {
        mDb.close();
        Locale.setDefault(mDefaultLocale);
    }

    @Test
    public void localized() {
        initDao(new Locale("tr", "TR"));
        List<CollateEntity> result = mDao.sortedByLocalized();
        assertThat(result, CoreMatchers.is(Arrays.asList(
                mItem4, mItem1, mItem2, mItem3
        )));
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void localized_asUnicode() {
        initDao(Locale.getDefault());
        List<CollateEntity> result = mDao.sortedByLocalizedAsUnicode();
        assertThat(result, CoreMatchers.is(Arrays.asList(
                mItem4, mItem2, mItem1, mItem3
        )));
    }

    @Test
    public void unicode_asLocalized() {
        initDao(new Locale("tr", "TR"));
        List<CollateEntity> result = mDao.sortedByUnicodeAsLocalized();
        assertThat(result, CoreMatchers.is(Arrays.asList(
                mItem4, mItem1, mItem2, mItem3
        )));
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void unicode() {
        initDao(Locale.getDefault());
        List<CollateEntity> result = mDao.sortedByUnicode();
        assertThat(result, CoreMatchers.is(Arrays.asList(
                mItem4, mItem2, mItem1, mItem3
        )));
    }

    @SuppressWarnings("WeakerAccess")
    @androidx.room.Entity
    static class CollateEntity {
        @PrimaryKey
        public final int id;
        @ColumnInfo(collate = ColumnInfo.LOCALIZED)
        public final String localizedName;
        @ColumnInfo(collate = ColumnInfo.UNICODE)
        public final String unicodeName;

        CollateEntity(int id, String name) {
            this.id = id;
            this.localizedName = name;
            this.unicodeName = name;
        }

        CollateEntity(int id, String localizedName, String unicodeName) {
            this.id = id;
            this.localizedName = localizedName;
            this.unicodeName = unicodeName;
        }

        @SuppressWarnings("SimplifiableIfStatement")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CollateEntity that = (CollateEntity) o;

            if (id != that.id) return false;
            if (!localizedName.equals(that.localizedName)) return false;
            return unicodeName.equals(that.unicodeName);
        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + localizedName.hashCode();
            result = 31 * result + unicodeName.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "CollateEntity{"
                    + "id=" + id
                    + ", localizedName='" + localizedName + '\''
                    + ", unicodeName='" + unicodeName + '\''
                    + '}';
        }
    }

    @Dao
    interface CollateDao {
        @Query("SELECT * FROM CollateEntity ORDER BY localizedName ASC")
        List<CollateEntity> sortedByLocalized();

        @Query("SELECT * FROM CollateEntity ORDER BY localizedName COLLATE UNICODE ASC")
        List<CollateEntity> sortedByLocalizedAsUnicode();

        @Query("SELECT * FROM CollateEntity ORDER BY unicodeName ASC")
        List<CollateEntity> sortedByUnicode();

        @Query("SELECT * FROM CollateEntity ORDER BY unicodeName COLLATE LOCALIZED ASC")
        List<CollateEntity> sortedByUnicodeAsLocalized();

        @Insert
        void insert(CollateEntity... entities);
    }

    @Database(entities = CollateEntity.class, version = 1, exportSchema = false)
    abstract static class CollateDb extends RoomDatabase {
        abstract CollateDao dao();
    }
}
