/*
 * Copyright (C) 2018 The Android Open Source Project
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

import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class SupportSQLiteDatabaseTest {
    @Test fun exclusiveDefault() {
        val db = mock(SupportSQLiteDatabase::class.java)
        db.transaction {}
        verify(db).beginTransaction()
    }

    @Test fun exclusiveFalse() {
        val db = mock(SupportSQLiteDatabase::class.java)
        db.transaction(exclusive = false) {}
        verify(db).beginTransactionNonExclusive()
    }

    @Test fun exclusiveTrue() {
        val db = mock(SupportSQLiteDatabase::class.java)
        db.transaction(exclusive = true) {}
        verify(db).beginTransaction()
    }

    @Test fun bodyNormalCallsSuccessAndEnd() {
        val db = mock(SupportSQLiteDatabase::class.java)
        db.transaction {}
        verify(db).setTransactionSuccessful()
        verify(db).endTransaction()
    }

    @Suppress("UNREACHABLE_CODE") // A programming error might not invoke the lambda.
    @Test fun bodyThrowsDoesNotCallSuccess() {
        val db = mock(SupportSQLiteDatabase::class.java)
        try {
            db.transaction {
                throw IllegalStateException()
            }
            fail()
        } catch (e: IllegalStateException) {
        }
        verify(db, times(0)).setTransactionSuccessful()
        verify(db).endTransaction()
    }
}
