/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.solver.transaction.binder

import androidx.room.compiler.processing.XType
import androidx.room.solver.CodeGenScope
import androidx.room.solver.transaction.result.TransactionMethodAdapter
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec

/**
 * Connects a transaction method, database and a [TransactionMethodAdapter].
 *
 * The default implementation is [InstantTransactionMethodBinder] that executes the transaction
 * synchronously. Other deferred transactions are unsupported expect for coroutines, for such
 * binding then [CoroutineTransactionMethodBinder] is used.
 */
abstract class TransactionMethodBinder(val adapter: TransactionMethodAdapter) {

    /**
     * Receives the method's return type, parameters along with the Dao class names to generate the
     * transaction wrapper body that delegates to the non-abstract or default dao method.
     */
    abstract fun executeAndReturn(
        returnType: XType,
        parameterNames: List<String>,
        daoName: ClassName,
        daoImplName: ClassName,
        dbField: FieldSpec,
        scope: CodeGenScope
    )
}