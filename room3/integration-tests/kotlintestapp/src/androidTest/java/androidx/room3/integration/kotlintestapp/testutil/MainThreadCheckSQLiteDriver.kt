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

package androidx.room3.integration.kotlintestapp.testutil

import android.os.Looper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement

/**
 * A utility driver that checks and throws an exception if a statement is prepared in the main
 * thread.
 */
class MainThreadCheckSQLiteDriver(private val delegate: SQLiteDriver) : SQLiteDriver {
    override fun open(fileName: String): SQLiteConnection {
        return MainThreadCheckConnection(delegate.open(fileName))
    }
}

private class MainThreadCheckConnection(private val delegate: SQLiteConnection) :
    SQLiteConnection by delegate {
    override fun prepare(sql: String): SQLiteStatement {
        check(!isMainThread) { "Main thread database operations are not allowed." }
        return delegate.prepare(sql)
    }
}

private val isMainThread: Boolean
    get() = Looper.getMainLooper().thread === Thread.currentThread()
