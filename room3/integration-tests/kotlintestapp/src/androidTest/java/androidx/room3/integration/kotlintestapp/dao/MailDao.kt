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
package androidx.room3.integration.kotlintestapp.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.integration.kotlintestapp.vo.Mail
import kotlinx.coroutines.flow.Flow

@Dao
interface MailDao {
    @Insert fun insert(mail: Mail)

    @Insert fun insert(mails: List<Mail>)

    @Query("SELECT rowId, * FROM mail WHERE mail MATCH :searchQuery")
    fun getMail(searchQuery: String): List<Mail>

    @Query("SELECT rowId, * FROM mail WHERE subject MATCH :searchQuery")
    fun getMailWithSubject(searchQuery: String): List<Mail>

    @Query("SELECT rowId, * FROM mail WHERE body MATCH :searchQuery")
    fun getMailWithBody(searchQuery: String): List<Mail>

    @Query("SELECT snippet(mail) FROM mail WHERE body MATCH :searchQuery")
    fun getMailBodySnippets(searchQuery: String): List<String>

    @Query("SELECT rowId, * FROM mail") fun getFlowDataMail(): Flow<List<Mail>>

    @Query("INSERT INTO mail(`mail`) VALUES('optimize')") fun optimizeMail()

    @Query("INSERT INTO mail(`mail`) VALUES('rebuild')") fun rebuildMail()
}
