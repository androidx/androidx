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
package androidx.room3.integration.kotlintestapp

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.room3.Room
import androidx.room3.integration.kotlintestapp.database.RemoteEntity
import androidx.room3.integration.kotlintestapp.database.RemoteSampleDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.launch

/** For testing use of [RemoteSampleDatabase] in a remote process. */
@Suppress("RestrictedApiAndroidX") // Due to usage of getCoroutineScope()
class RemoteDatabaseService : Service() {

    private val binder: IRemoteDatabaseService.Stub =
        object : IRemoteDatabaseService.Stub() {
            override fun getPid(): Int {
                return Process.myPid()
            }

            override fun insertEntity(id: Long) {
                Log.i(TAG, "insertEntity() - id = $id")
                database.getCoroutineScope().launch { database.dao().insert(RemoteEntity(id)) }
            }
        }

    private lateinit var database: RemoteSampleDatabase

    override fun onBind(intent: Intent): IBinder {
        val databaseName = intent.getStringExtra(DATABASE_NAME_PARAM)
        requireNotNull(databaseName) { "Must pass database name in the intent" }
        check(!::database.isInitialized) { "Cannot re-use the same service for different tests" }
        database =
            Room.databaseBuilder<RemoteSampleDatabase>(this, databaseName)
                .setDriver(AndroidSQLiteDriver())
                .enableMultiInstanceInvalidation()
                .build()
        database.getCoroutineScope().launch {
            val count = database.dao().getEntityCount()
            check(count == 0) {
                "Remote database has rows. Either the database was not deleted between tests or " +
                    "the remote service was started after database operations."
            }
        }
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        database.close()
        return super.onUnbind(intent)
    }

    companion object {
        private const val TAG = "RemoteDatabaseService"
        private const val DATABASE_NAME_PARAM = "db-name"

        fun intentFor(context: Context, databaseName: String): Intent {
            val intent = Intent(context, RemoteDatabaseService::class.java)
            intent.putExtra(DATABASE_NAME_PARAM, databaseName)
            return intent
        }
    }
}
