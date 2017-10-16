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

package android.arch.persistence.room;

import android.support.annotation.RestrictTo;

/**
 * Schema information about Room's master table.
 *
 * @hide
 */
@SuppressWarnings("WeakerAccess")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RoomMasterTable {
    /**
     * The master table where room keeps its metadata information.
     */
    public static final String TABLE_NAME = "room_master_table";
    // must match the runtime property Room#MASTER_TABLE_NAME
    public static final String NAME = "room_master_table";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_IDENTITY_HASH = "identity_hash";
    public static final String DEFAULT_ID = "42";

    public static final String CREATE_QUERY = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_IDENTITY_HASH + " TEXT)";

    public static final String READ_QUERY = "SELECT " + COLUMN_IDENTITY_HASH
            + " FROM " + TABLE_NAME + " WHERE "
            + COLUMN_ID + " = " + DEFAULT_ID + " LIMIT 1";

    /**
     * We don't escape here since we know what we are passing.
     */
    public static String createInsertQuery(String hash) {
        return "INSERT OR REPLACE INTO " + TABLE_NAME + " ("
                + COLUMN_ID + "," + COLUMN_IDENTITY_HASH + ")"
                + " VALUES(" + DEFAULT_ID + ", \"" + hash + "\")";
    }
}
