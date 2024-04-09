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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

public class AutoClosingRoomOpenHelperFactoryTest {
    private val DB_NAME = "name"

    @Before
    public fun setUp() {
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(DB_NAME)
    }

    private fun getAutoClosingRoomOpenHelperFactory(
        timeoutMillis: Long = 10
    ): AutoClosingRoomOpenHelperFactory {
        val delegateOpenHelperFactory = FrameworkSQLiteOpenHelperFactory()

        return AutoClosingRoomOpenHelperFactory(
            delegateOpenHelperFactory,
            AutoCloser(timeoutMillis, TimeUnit.MILLISECONDS, Executors.newSingleThreadExecutor())
                .also { it.onAutoCloseCallback = Runnable {} }
        )
    }

    @SuppressLint("BanThreadSleep")
    @RequiresApi(Build.VERSION_CODES.N)
    @Test
    public fun testCallbacksCalled() {
        val autoClosingRoomOpenHelperFactory =
            getAutoClosingRoomOpenHelperFactory()

        val callbackCount = AtomicInteger()

        val countingCallback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                callbackCount.incrementAndGet()
            }

            override fun onConfigure(db: SupportSQLiteDatabase) {
                callbackCount.incrementAndGet()
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                callbackCount.incrementAndGet()
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            }
        }

        val autoClosingRoomOpenHelper = autoClosingRoomOpenHelperFactory.create(
            SupportSQLiteOpenHelper.Configuration
                .builder(ApplicationProvider.getApplicationContext())
                .callback(countingCallback)
                .name(DB_NAME)
                .build()
        )

        autoClosingRoomOpenHelper.writableDatabase

        // onConfigure + onCreate + onOpen
        assertEquals(3, callbackCount.get())

        Thread.sleep(100)

        autoClosingRoomOpenHelper.writableDatabase

        // onCreate won't be called the second time.
        // onConfigure + onCreate + onOpen + onConfigure + onOpen
        assertEquals(5, callbackCount.get())
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @Test
    public fun testDatabaseIsOpenForSlowCallbacks() {
        val autoClosingRoomOpenHelperFactory =
            getAutoClosingRoomOpenHelperFactory()

        val refCountCheckingCallback = object : SupportSQLiteOpenHelper.Callback(1) {
            @SuppressLint("BanThreadSleep")
            override fun onCreate(db: SupportSQLiteDatabase) {
                Thread.sleep(100)
                db.execSQL("create table user (idk int)")
            }

            @SuppressLint("BanThreadSleep")
            override fun onConfigure(db: SupportSQLiteDatabase) {
                Thread.sleep(100)
                db.setMaximumSize(100000)
            }

            @SuppressLint("BanThreadSleep")
            override fun onOpen(db: SupportSQLiteDatabase) {
                Thread.sleep(100)
                db.execSQL("select * from user")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            }
        }

        val autoClosingRoomOpenHelper = autoClosingRoomOpenHelperFactory.create(
            SupportSQLiteOpenHelper.Configuration
                .builder(ApplicationProvider.getApplicationContext())
                .callback(refCountCheckingCallback)
                .name(DB_NAME)
                .build()
        )

        val db = autoClosingRoomOpenHelper.writableDatabase
        assertTrue(db.isOpen)
    }
}
