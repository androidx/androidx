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

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Fts4;
import androidx.room.Query;
import androidx.room.RoomDatabase;

/**
 * Not an actual JUnit test class, but it is here so that we can test that room-compiler will
 * correctly verify a table create query that contains a custom tokenizer.
 */
public class CustomFTSTokenizerTest {

    @Database(entities = TheEntity.class, version = 1, exportSchema = false)
    abstract static class CustomTokDatabase extends RoomDatabase  {
        public abstract TheDao getDao();
    }

    @Entity
    @Fts4(tokenizer = "customICU", tokenizerArgs = "en_AU")
    static class TheEntity {
        public String data;
    }

    @Dao
    interface TheDao {
        @Query("SELECT * FROM TheEntity WHERE data MATCH :term")
        TheEntity search(String term);
    }
}
