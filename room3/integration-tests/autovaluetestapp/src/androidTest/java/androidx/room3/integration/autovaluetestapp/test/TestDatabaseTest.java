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

package androidx.room3.integration.autovaluetestapp.test;

import android.content.Context;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.room3.Room;
import androidx.room3.integration.autovaluetestapp.TestDatabase;
import androidx.room3.integration.autovaluetestapp.dao.ParcelableEntityDao;
import androidx.room3.integration.autovaluetestapp.dao.PersonDao;
import androidx.room3.integration.autovaluetestapp.dao.PetDao;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;

import kotlinx.coroutines.ExecutorsKt;

@SuppressWarnings("WeakerAccess")
public abstract class TestDatabaseTest {

    @Rule
    public CountingTaskExecutorRule mExecutorRule = new CountingTaskExecutorRule();

    protected TestDatabase mDatabase;
    protected PersonDao mPersonDao;
    protected PetDao mPetDao;
    protected ParcelableEntityDao mParcelableEntityDao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        mDatabase = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                .setQueryCoroutineContext(ExecutorsKt.from(ArchTaskExecutor.getIOThreadExecutor()))
                .build();
        mPersonDao = mDatabase.getPersonDao();
        mPetDao = mDatabase.getPetDao();
        mParcelableEntityDao = mDatabase.getParcelableEntityDao();
    }
}
