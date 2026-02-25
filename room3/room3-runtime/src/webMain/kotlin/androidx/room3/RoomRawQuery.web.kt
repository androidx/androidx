/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3

import androidx.sqlite.SQLiteStatement

internal actual fun newBindOnlySQLiteStatement(delegate: SQLiteStatement): BindOnlySQLiteStatement =
    BindOnlySQLiteStatementImpl(delegate)

private class BindOnlySQLiteStatementImpl(delegate: SQLiteStatement) :
    BindOnlySQLiteStatement(delegate) {
    override suspend fun stepAsync(): Boolean {
        error(ONLY_BIND_CALLS_ALLOWED_ERROR)
    }
}
