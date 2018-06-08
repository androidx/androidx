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
package androidx.work.integration.testapp.sherlockholmes;

import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.integration.testapp.db.TestDatabase;

/**
 * A Worker that deletes the final results file.
 */
public class TextStartupWorker extends Worker {

    @Override
    public @NonNull Result doWork() {
        TestDatabase db = TestDatabase.getInstance(getApplicationContext());
        db.getWordCountDao().clear();
        Log.d("Startup", "Database cleared");
        return Result.SUCCESS;
    }
}
