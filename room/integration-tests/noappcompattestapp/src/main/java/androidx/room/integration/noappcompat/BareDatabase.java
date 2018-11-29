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

package androidx.room.integration.noappcompat;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.RoomDatabase;

@Database(
        version = 1,
        entities = {BareDatabase.BareEntity.class},
        exportSchema = false
)
abstract class BareDatabase extends RoomDatabase {
    abstract BareDao dao();

    @Dao
    interface BareDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(BareEntity entity);

        @Query("select * from BareEntity WHERE id = :id")
        BareEntity get(int id);
    }

    @SuppressWarnings("WeakerAccess") // to avoid naming them with m
    @Entity
    static class BareEntity {
        @PrimaryKey
        public final int id;
        public final String name;

        BareEntity(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BareEntity that = (BareEntity) o;
            return id == that.id && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return id + (name.hashCode() >> 31);
        }
    }
}
