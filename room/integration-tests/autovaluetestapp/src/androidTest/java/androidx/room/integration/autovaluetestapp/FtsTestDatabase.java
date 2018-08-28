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

package androidx.room.integration.autovaluetestapp;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RoomDatabase;
import androidx.room.integration.autovaluetestapp.vo.Mail;

import java.util.List;

@Database(entities = {Mail.class}, version = 1, exportSchema = false)
public abstract class FtsTestDatabase extends RoomDatabase {
    public abstract MailDao getMailDao();

    @Dao
    public interface MailDao {
        @Insert
        void insert(Mail mail);

        @Query("SELECT * FROM mail WHERE mail MATCH :searchQuery")
        List<Mail> getMail(String searchQuery);
    }
}
