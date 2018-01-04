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

package android.arch.persistence.room.integration.testapp.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.db.SupportSQLiteQuery;
import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.RawQuery;
import android.arch.persistence.room.integration.testapp.vo.NameAndLastName;
import android.arch.persistence.room.integration.testapp.vo.User;
import android.arch.persistence.room.integration.testapp.vo.UserAndAllPets;
import android.arch.persistence.room.integration.testapp.vo.UserAndPet;

import java.util.Date;
import java.util.List;

@Dao
public interface RawDao {
    @RawQuery
    User getUser(String query);
    @RawQuery
    UserAndAllPets getUserAndAllPets(String query);
    @RawQuery
    User getUser(SupportSQLiteQuery query);
    @RawQuery
    UserAndPet getUserAndPet(String query);
    @RawQuery
    NameAndLastName getUserNameAndLastName(String query);
    @RawQuery(observedEntities = User.class)
    NameAndLastName getUserNameAndLastName(SupportSQLiteQuery query);
    @RawQuery
    int count(String query);
    @RawQuery
    List<User> getUserList(String query);
    @RawQuery
    List<UserAndPet> getUserAndPetList(String query);
    @RawQuery(observedEntities = User.class)
    LiveData<User> getUserLiveData(String query);
    @RawQuery
    UserNameAndBirthday getUserAndBirthday(String query);
    class UserNameAndBirthday {
        @ColumnInfo(name = "mName")
        public final String name;
        @ColumnInfo(name = "mBirthday")
        public final Date birthday;

        public UserNameAndBirthday(String name, Date birthday) {
            this.name = name;
            this.birthday = birthday;
        }
    }
}
