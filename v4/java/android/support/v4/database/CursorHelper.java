/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v4.database;

import android.database.Cursor;
import android.support.v4.content.ModernAsyncTask;

/**
 * Helper functions for dealing with cursors.
 */
public class CursorHelper {
    final static class CloseTask extends ModernAsyncTask<Cursor, Void, Void> {
        @Override
        protected Void doInBackground(Cursor... params) {
            params[0].close();
            return null;
        }
    }

    /**
     * Asynchronously close the given cursor.
     */
    public static void closeAsync(Cursor cursor) {
        CloseTask closeTask = new CloseTask();
        closeTask.executeOnExecutor(ModernAsyncTask.THREAD_POOL_EXECUTOR, cursor);
    }
}
