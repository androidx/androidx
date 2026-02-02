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
package androidx.room3.integration.kotlintestapp.test

import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Fts4
import androidx.room3.FtsOptions
import androidx.room3.Query
import androidx.room3.RoomDatabase

/**
 * Not an actual test class, but it is here so that we can test that room-compiler will correctly
 * verify a table's CREATE statement that contains a custom tokenizer.
 */
class CustomFTSTokenizerTest {
    @Database(
        entities = [TheEntity::class, TheEntityWithICU::class],
        version = 1,
        exportSchema = false,
    )
    abstract class CustomTokDatabase : RoomDatabase() {
        abstract fun getDao(): TheDao
    }

    @Entity
    @Fts4(tokenizer = "customICU", tokenizerArgs = ["en_AU"])
    data class TheEntity(val data: String)

    // For b/201753224
    @Entity
    @Fts4(tokenizer = FtsOptions.TOKENIZER_ICU)
    data class TheEntityWithICU(val data: String)

    @Dao
    interface TheDao {
        @Query("SELECT * FROM TheEntity WHERE data MATCH :term") fun search(term: String): TheEntity
    }
}
