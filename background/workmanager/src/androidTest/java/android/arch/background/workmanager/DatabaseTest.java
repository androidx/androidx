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

package android.arch.background.workmanager;

import android.support.test.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;

/**
 * An abstract class for getting an in-memory instance of the {@link WorkDatabase}.
 */
public abstract class DatabaseTest {
    protected WorkDatabase mDatabase;

    @Before
    public void initializeDb() {
        mDatabase = WorkDatabase.create(InstrumentationRegistry.getTargetContext(), true);
    }

    @After
    public void closeDb() {
        mDatabase.close();
    }

    public void insertBaseWork(BaseWork baseWork) {
        mDatabase.beginTransaction();
        try {
            mDatabase.workSpecDao().insertWorkSpec(baseWork.getWorkSpec());
            mDatabase.workInputDao().insert(baseWork.getWorkInput());
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }
}
