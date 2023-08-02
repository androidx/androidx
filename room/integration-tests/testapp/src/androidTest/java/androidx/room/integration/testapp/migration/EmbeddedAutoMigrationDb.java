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
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.RoomDatabase;
import androidx.room.RoomWarnings;

import java.io.Serializable;
import java.util.List;

@Database(
        version = EmbeddedAutoMigrationDb.LATEST_VERSION,
        entities = {
                EmbeddedAutoMigrationDb.Entity1.class,
                EmbeddedAutoMigrationDb.EmbeddedEntity1.class,
                EmbeddedAutoMigrationDb.EmbeddedEntity2.class
        },
        autoMigrations = {
                @AutoMigration(
                        from = 1, to = 2
                )
        },
        exportSchema = true
)
public abstract class EmbeddedAutoMigrationDb extends RoomDatabase {
    static final int LATEST_VERSION = 2;
    @Dao
    interface EmbeddedAutoMigrationDao {
        @Query("SELECT * from Entity1 ORDER BY id ASC")
        List<EmbeddedAutoMigrationDb.Entity1> getAllEntity1s();
    }

    abstract EmbeddedAutoMigrationDb.EmbeddedAutoMigrationDao dao();

    /**
     * No change between versions.
     */
    @Entity
    @SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
    static class Entity1 implements Serializable {
        public static final String TABLE_NAME = "Entity1";
        @PrimaryKey
        public int id;
        public String name;
        @ColumnInfo(defaultValue = "1")
        public int addedInV1;

        public EmbeddedEntity1 getEmbeddedEntity1() {
            return embeddedEntity1;
        }

        public void setEmbeddedEntity1(
                EmbeddedEntity1 embeddedEntity1) {
            this.embeddedEntity1 = embeddedEntity1;
        }

        @Embedded
        private EmbeddedEntity1 embeddedEntity1;
    }

    @Entity
    @SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
    static class EmbeddedEntity1 implements Serializable {
        public static final String TABLE_NAME = "EmbeddedEntity1";
        @PrimaryKey
        public int embeddedId1;

        @ColumnInfo(defaultValue = "1")
        public int addedInV2;

        public EmbeddedEntity2 getEmbeddedEntity2() {
            return embeddedEntity2;
        }

        public void setEmbeddedEntity2(
                EmbeddedEntity2 embeddedEntity2) {
            this.embeddedEntity2 = embeddedEntity2;
        }

        @Embedded
        private EmbeddedEntity2 embeddedEntity2;
    }

    @Entity
    @SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
    static class EmbeddedEntity2 implements Serializable {
        public static final String TABLE_NAME = "EmbeddedEntity2";

        public int getEmbeddedId2() {
            return embeddedId2;
        }

        public void setEmbeddedId2(int embeddedId2) {
            this.embeddedId2 = embeddedId2;
        }

        @PrimaryKey
        private int embeddedId2;
    }
}
