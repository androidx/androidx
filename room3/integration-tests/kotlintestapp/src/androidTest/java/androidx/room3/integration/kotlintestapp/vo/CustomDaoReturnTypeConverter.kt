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

package androidx.room3.integration.kotlintestapp.vo

import androidx.room3.DaoReturnTypeConverter
import androidx.room3.OperationType
import androidx.room3.RoomDatabase

class CustomDaoReturnType<T>(val data: T)

class CustomDaoReturnTypeConverter {
    @DaoReturnTypeConverter(operations = [OperationType.READ, OperationType.WRITE])
    suspend fun <T> convert(
        database: RoomDatabase,
        tableNames: Array<String>,
        executeAndConvert: suspend () -> T,
    ): CustomDaoReturnType<T> {
        val data = executeAndConvert.invoke()
        return createCustomType(data)
    }

    private fun <T> createCustomType(data: T) = CustomDaoReturnType(data)
}
