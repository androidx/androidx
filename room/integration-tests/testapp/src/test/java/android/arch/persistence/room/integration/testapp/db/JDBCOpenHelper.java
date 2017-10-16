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

package android.arch.persistence.room.integration.testapp.db;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;

public class JDBCOpenHelper implements SupportSQLiteOpenHelper {
    @Override
    public String getDatabaseName() {
        return null;
    }

    @Override
    public void setWriteAheadLoggingEnabled(boolean enabled) {

    }

    @Override
    public SupportSQLiteDatabase getWritableDatabase() {
        return null;
    }

    @Override
    public SupportSQLiteDatabase getReadableDatabase() {
        return null;
    }

    @Override
    public void close() {

    }
}
