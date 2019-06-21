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

package androidx.room.verifier

import java.sql.SQLException

object DatabaseVerificationErrors {
    private val CANNOT_VERIFY_QUERY: String = "There is a problem with the query: %s"
    fun cannotVerifyQuery(exception: SQLException): String {
        return CANNOT_VERIFY_QUERY.format(exception.message)
    }

    private val CANNOT_CREATE_SQLITE_CONNECTION: String = "Room cannot create an SQLite" +
            " connection to verify the queries. Query verification will be disabled. Error: %s"
    fun cannotCreateConnection(exception: Exception): String {
        return CANNOT_CREATE_SQLITE_CONNECTION.format(exception.message)
    }
}
