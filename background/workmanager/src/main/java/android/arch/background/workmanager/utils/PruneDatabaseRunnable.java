/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.background.workmanager.utils;

import android.arch.background.workmanager.WorkDatabase;

/**
 * A {@link Runnable} that prunes the database of all non-pending work.
 */

public class PruneDatabaseRunnable implements Runnable {

    private WorkDatabase mWorkDatabase;

    public PruneDatabaseRunnable(WorkDatabase workDatabase) {
        mWorkDatabase = workDatabase;
    }

    @Override
    public void run() {
        mWorkDatabase.beginTransaction();
        try {
            while (mWorkDatabase.workSpecDao().pruneLeaves() > 0) {
                // Loop until nothing else can be pruned.
            }
            mWorkDatabase.setTransactionSuccessful();
        } finally {
            mWorkDatabase.endTransaction();
        }
    }
}
