/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.room3.integration.kotlintestapp.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.integration.kotlintestapp.vo.Fts5Mail
import kotlinx.coroutines.flow.Flow

@Dao
interface Fts5MailDao {
    @Insert fun insert(mail: Fts5Mail)

    @Insert fun insert(mails: List<Fts5Mail>)

    @Query("SELECT rowId, * FROM Fts5Mail WHERE Fts5Mail MATCH :searchQuery")
    fun getMail(searchQuery: String): List<Fts5Mail>

    @Query("SELECT rowId, * FROM Fts5Mail WHERE subject MATCH :searchQuery")
    fun getMailWithSubject(searchQuery: String): List<Fts5Mail>

    @Query("SELECT rowId, * FROM Fts5Mail WHERE body MATCH :searchQuery")
    fun getMailWithBody(searchQuery: String): List<Fts5Mail>

    @Query(
        "SELECT highlight(Fts5Mail, 1, '<b>', '</b>') FROM Fts5Mail WHERE body MATCH :searchQuery"
    )
    fun getMailBodyHighlight(searchQuery: String): List<String>

    @Query("SELECT rowId, * FROM Fts5Mail") fun getFlowDataMail(): Flow<List<Fts5Mail>>
}
