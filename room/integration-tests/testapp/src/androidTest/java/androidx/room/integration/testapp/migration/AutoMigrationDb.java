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


import androidx.annotation.NonNull;
import androidx.room.AutoMigration;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.RoomDatabase;
import androidx.room.migration.AutoMigrationCallback;

import java.util.List;

@Database(
        version = AutoMigrationDb.LATEST_VERSION,
        entities = {
                AutoMigrationDb.Entity1.class,
                AutoMigrationDb.Entity2.class,
                AutoMigrationDb.Entity3.class,
                AutoMigrationDb.Entity4.class,
                AutoMigrationDb.Entity5.class,
                AutoMigrationDb.Entity6.class,
                AutoMigrationDb.Entity7.class,
                AutoMigrationDb.Entity8.class,
                AutoMigrationDb.Entity9.class,
                AutoMigrationDb.Entity10.class,
                AutoMigrationDb.Entity11.class,
                AutoMigrationDb.Entity12.class,
                AutoMigrationDb.Entity13.class,
                AutoMigrationDb.Entity14.class
        },
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
        public int addedInV1;
    }

    @Entity
    static class Entity2 {
        public static final String TABLE_NAME = "Entity2";
        @PrimaryKey
        public int id;
        public String name;
        @ColumnInfo(defaultValue = "1")
        public int addedInV1;
        @ColumnInfo(defaultValue = "2")
        public int addedInV2;
    }

    @Entity
    static class Entity3 {
        public static final String TABLE_NAME = "Entity3";
        @PrimaryKey
        public int id;
        public String name;
    }

    @Entity
    static class Entity4 {
        public static final String TABLE_NAME = "Entity4";
        @PrimaryKey
        public int id;
        public String name;
        @ColumnInfo(defaultValue = "2")
        public int addedInV1;
    }

    @Entity
    static class Entity5 {
        public static final String TABLE_NAME = "Entity5";
        @PrimaryKey
        public int id;
        public String name;
        @ColumnInfo(defaultValue = "1")
        public String addedInV1;
    }

    @Entity
    static class Entity6 {
        public static final String TABLE_NAME = "Entity6";
        @PrimaryKey
        public int id;
        public String name;
        // @ColumnInfo(defaultValue = "1") - now nullable
        public int addedInV1;
    }

    @Entity
    static class Entity7 {
        public static final String TABLE_NAME = "Entity7";
        @PrimaryKey
        public int id;
        public String name;
        @ColumnInfo(defaultValue = "1")
        public int addedInV1;
    }

    @Entity
    static class Entity8 {
        public static final String TABLE_NAME = "Entity8";
        public int id;
        @PrimaryKey
        @NonNull
        public String name;
        @ColumnInfo(defaultValue = "1")
        public int addedInV1;
    }

    @Entity(foreignKeys = {
            @ForeignKey(entity = Entity12.class,
                    parentColumns = "id",
                    childColumns = "id",
                    deferred = true)})
    static class Entity9 {
        public static final String TABLE_NAME = "Entity9";
        @PrimaryKey
        public int id;
        public String name;
        @ColumnInfo(defaultValue = "1")
        public int addedInV1;
    }

    @Entity(foreignKeys = {
            @ForeignKey(entity = Entity13.class,
                    parentColumns = "addedInV1",
                    childColumns = "addedInV1",
                    deferred = true)},
            indices = {@Index(value = "addedInV1", unique = true)})
    static class Entity10 {
        public static final String TABLE_NAME = "Entity10";
        @PrimaryKey
        public int id;
        public String name;
        @ColumnInfo(defaultValue = "1")
        public int addedInV1;
    }

    @Entity
    static class Entity11 {
        public static final String TABLE_NAME = "Entity11";
        @PrimaryKey
        public int id;
        public String name;
        @ColumnInfo(defaultValue = "1")
        public int addedInV1;
    }

    @Entity(indices = {@Index(value = "name", unique = true)})
    static class Entity12 {
        public static final String TABLE_NAME = "Entity12";
        @PrimaryKey
        public int id;
        public String name;
        @ColumnInfo(defaultValue = "1")
        public int addedInV1;
    }

    @Entity(indices = {@Index(value = "addedInV1", unique = true)})
    static class Entity13 {
        public static final String TABLE_NAME = "Entity13";
        @PrimaryKey
        public int id;
        public String name;
        @ColumnInfo(defaultValue = "1")
        public int addedInV1;
    }

    @Entity
    static class Entity14 {
        public static final String TABLE_NAME = "Entity14";
        @PrimaryKey
        public int id;
        public String name;
    }

    @Dao
    interface AutoMigrationDao {
        @Query("SELECT * from Entity1 ORDER BY id ASC")
        List<AutoMigrationDb.Entity1> getAllEntity1s();
    }

    @AutoMigration(from=1, to=2)
    interface SimpleAutoMigration extends AutoMigrationCallback {

    }
}