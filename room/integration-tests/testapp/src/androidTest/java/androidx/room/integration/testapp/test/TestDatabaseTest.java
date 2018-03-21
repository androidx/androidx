/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import androidx.room.Room;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.dao.FunnyNamedDao;
import androidx.room.integration.testapp.dao.PetCoupleDao;
import androidx.room.integration.testapp.dao.PetDao;
import androidx.room.integration.testapp.dao.RawDao;
import androidx.room.integration.testapp.dao.SchoolDao;
import androidx.room.integration.testapp.dao.SpecificDogDao;
import androidx.room.integration.testapp.dao.ToyDao;
import androidx.room.integration.testapp.dao.UserDao;
import androidx.room.integration.testapp.dao.UserPetDao;
import androidx.room.integration.testapp.dao.WithClauseDao;

import org.junit.Before;

@SuppressWarnings("WeakerAccess")
public abstract class TestDatabaseTest {
    protected TestDatabase mDatabase;
    protected UserDao mUserDao;
    protected PetDao mPetDao;
    protected UserPetDao mUserPetDao;
    protected SchoolDao mSchoolDao;
    protected PetCoupleDao mPetCoupleDao;
    protected ToyDao mToyDao;
    protected SpecificDogDao mSpecificDogDao;
    protected WithClauseDao mWithClauseDao;
    protected FunnyNamedDao mFunnyNamedDao;
    protected RawDao mRawDao;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        mDatabase = Room.inMemoryDatabaseBuilder(context, TestDatabase.class).build();
        mUserDao = mDatabase.getUserDao();
        mPetDao = mDatabase.getPetDao();
        mUserPetDao = mDatabase.getUserPetDao();
        mSchoolDao = mDatabase.getSchoolDao();
        mPetCoupleDao = mDatabase.getPetCoupleDao();
        mToyDao = mDatabase.getToyDao();
        mSpecificDogDao = mDatabase.getSpecificDogDao();
        mWithClauseDao = mDatabase.getWithClauseDao();
        mFunnyNamedDao = mDatabase.getFunnyNamedDao();
        mRawDao = mDatabase.getRawDao();
    }
}
