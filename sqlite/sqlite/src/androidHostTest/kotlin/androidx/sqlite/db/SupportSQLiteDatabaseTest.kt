/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.sqlite.db

import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class SupportSQLiteDatabaseTest {
    @Test
    fun exclusiveDefault() {
        val db = Mockito.mock(SupportSQLiteDatabase::class.java)
        db.transaction {}
        Mockito.verify(db).beginTransaction()
    }

    @Test
    fun exclusiveFalse() {
        val db = Mockito.mock(SupportSQLiteDatabase::class.java)
        db.transaction(exclusive = false) {}
        Mockito.verify(db).beginTransactionNonExclusive()
    }

    @Test
    fun exclusiveTrue() {
        val db = Mockito.mock(SupportSQLiteDatabase::class.java)
        db.transaction(exclusive = true) {}
        Mockito.verify(db).beginTransaction()
    }

    @Test
    fun bodyNormalCallsSuccessAndEnd() {
        val db = Mockito.mock(SupportSQLiteDatabase::class.java)
        db.transaction {}
        Mockito.verify(db).setTransactionSuccessful()
        Mockito.verify(db).endTransaction()
    }

    @Suppress("UNREACHABLE_CODE") // A programming error might not invoke the lambda.
    @Test
    fun bodyThrowsDoesNotCallSuccess() {
        val db = Mockito.mock(SupportSQLiteDatabase::class.java)
        try {
            db.transaction { throw IllegalStateException() }
            Assert.fail()
        } catch (e: IllegalStateException) {}
        Mockito.verify(db, Mockito.times(0)).setTransactionSuccessful()
        Mockito.verify(db).endTransaction()
    }

    @Test
    fun bodyNonLocalReturnCallsSuccessAndEnd() {
        val db = Mockito.mock(SupportSQLiteDatabase::class.java)
        callTransactionWithNonLocalReturnBody(db)
        Mockito.verify(db).setTransactionSuccessful()
        Mockito.verify(db).endTransaction()
    }

    @Test
    fun bodyLocalReturnCallsSuccessAndEnd() {
        val db = Mockito.mock(SupportSQLiteDatabase::class.java)
        callTransactionWithLocalReturnBody(db)
        Mockito.verify(db).setTransactionSuccessful()
        Mockito.verify(db).endTransaction()
    }

    private fun callTransactionWithNonLocalReturnBody(db: SupportSQLiteDatabase) {
        db.transaction {
            return
        }
    }

    private fun callTransactionWithLocalReturnBody(db: SupportSQLiteDatabase) {
        db.transaction {
            return@transaction
        }
    }
}
