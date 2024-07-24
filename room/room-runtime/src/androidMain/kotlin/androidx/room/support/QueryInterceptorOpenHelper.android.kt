/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.support

import androidx.room.DelegatingOpenHelper
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope

internal class QueryInterceptorOpenHelper(
    override val delegate: SupportSQLiteOpenHelper,
    private val queryCallbackScope: CoroutineScope,
    private val queryCallback: RoomDatabase.QueryCallback
) : SupportSQLiteOpenHelper by delegate, DelegatingOpenHelper {

    override val writableDatabase: SupportSQLiteDatabase
        get() =
            QueryInterceptorDatabase(delegate.writableDatabase, queryCallbackScope, queryCallback)

    override val readableDatabase: SupportSQLiteDatabase
        get() =
            QueryInterceptorDatabase(delegate.readableDatabase, queryCallbackScope, queryCallback)
}
