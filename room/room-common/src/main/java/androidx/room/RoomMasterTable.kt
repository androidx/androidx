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
@file:JvmName("RoomMasterTable")

package androidx.room

import androidx.annotation.RestrictTo

/**
 * Schema information about Room's master table.
 *
 * @hide
 */
@SuppressWarnings("WeakerAccess")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public object RoomMasterTable {
    /**
     * The master table where room keeps its metadata information.
     */
    public const val TABLE_NAME: String = "room_master_table"

    // must match the runtime property Room#MASTER_TABLE_NAME
    public const val NAME: String = "room_master_table"
    private const val COLUMN_ID: String = "id"
    private const val COLUMN_IDENTITY_HASH: String = "identity_hash"
    public const val DEFAULT_ID: String = "42"

    public const val CREATE_QUERY: String = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
        " (" + COLUMN_ID + " INTEGER PRIMARY KEY," + COLUMN_IDENTITY_HASH + " TEXT)"

    public const val READ_QUERY: String = "SELECT " + COLUMN_IDENTITY_HASH + " FROM " +
        TABLE_NAME + " WHERE " + COLUMN_ID + " = " + DEFAULT_ID + " LIMIT 1"

    /**
     * We don't escape here since we know what we are passing.
     */
    @JvmStatic
    public fun createInsertQuery(hash: String): String {
        return "INSERT OR REPLACE INTO " + TABLE_NAME + " (" + COLUMN_ID + "," +
            COLUMN_IDENTITY_HASH + ")" + " VALUES(" + DEFAULT_ID + ", '" + hash + "')"
    }
}
