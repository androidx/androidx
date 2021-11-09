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

package androidx.room.integration.testapp.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.integration.testapp.vo.Mail;

import java.util.List;

@Dao
public interface MailDao {

    @Insert
    void insert(Mail mail);

    @Insert
    void insert(List<Mail> mails);

    @Query("SELECT rowId, * FROM mail WHERE mail MATCH :searchQuery")
    List<Mail> getMail(String searchQuery);

    @Query("SELECT rowId, * FROM mail WHERE subject MATCH :searchQuery")
    List<Mail> getMailWithSubject(String searchQuery);

    @Query("SELECT rowId, * FROM mail WHERE body MATCH :searchQuery")
    List<Mail> getMailWithBody(String searchQuery);

    @Query("SELECT snippet(mail) FROM mail WHERE body MATCH :searchQuery")
    List<String> getMailBodySnippets(String searchQuery);

    @Query("SELECT rowId, * FROM mail")
    io.reactivex.Flowable<List<Mail>> rx2_getFlowableMail();

    @Query("SELECT rowId, * FROM mail")
    io.reactivex.rxjava3.core.Flowable<List<Mail>> rx3_getFlowableMail();

    @Query("SELECT rowId, * FROM mail")
    LiveData<List<Mail>> getLiveDataMail();

    @Query("INSERT INTO mail(`mail`) VALUES('optimize')")
    void optimizeMail();

    @Query("INSERT INTO mail(`mail`) VALUES('rebuild')")
    void rebuildMail();
}
