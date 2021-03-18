/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.integration.testapp.migration;


import androidx.room.AutoMigration;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.RoomDatabase;
import androidx.room.migration.AutoMigrationCallback;

import java.util.List;

@Database(
        version = AutoMigrationDb.LATEST_VERSION,
        entities = AutoMigrationDb.Entity1.class,
        autoMigrations = AutoMigrationDb.SimpleAutoMigration.class,
        exportSchema = true
)
public abstract class AutoMigrationDb extends RoomDatabase {
    static final int LATEST_VERSION = 2;
    abstract AutoMigrationDb.AutoMigrationDao dao();
    @Entity
    static class Entity1 {
        public static final String TABLE_NAME = "Entity1";
        @PrimaryKey
        public int id;
        public String name;
        @ColumnInfo(defaultValue = "1")
        public int value1;
        @ColumnInfo(defaultValue = "2")
        public int value2;
        @ColumnInfo(defaultValue = "3")
        public int addedInV2;
    }

    @Dao
    interface AutoMigrationDao {
        @Query("SELECT * from Entity1")
        List<Entity1> getAllEntity1s();
        @Insert
        void insert(AutoMigrationDb.Entity1... entity1);
    }

    @AutoMigration(from=1, to=2)
    interface SimpleAutoMigration extends AutoMigrationCallback {}
}
