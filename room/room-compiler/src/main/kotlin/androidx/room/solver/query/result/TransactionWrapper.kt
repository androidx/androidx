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

package androidx.room.solver.query.result

import androidx.room.compiler.codegen.XCodeBlock

/** helper class to create correct transaction code. */
interface TransactionWrapper {
    fun beginTransactionWithControlFlow()

    fun commitTransaction()

    fun endTransactionWithControlFlow()
}

fun XCodeBlock.Builder.transactionWrapper(dbPropertyName: String) =
    object : TransactionWrapper {
        override fun beginTransactionWithControlFlow() {
            addStatement("%N.beginTransaction()", dbPropertyName)
            beginControlFlow("try")
        }

        override fun commitTransaction() {
            addStatement("%N.setTransactionSuccessful()", dbPropertyName)
        }

        override fun endTransactionWithControlFlow() {
            nextControlFlow("finally")
            addStatement("%N.endTransaction()", dbPropertyName)
            endControlFlow()
        }
    }
