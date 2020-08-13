/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.solver.shortcut.binder

import androidx.room.solver.CodeGenScope
import androidx.room.solver.shortcut.result.InsertMethodAdapter
import androidx.room.vo.ShortcutQueryParameter
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec

/**
 * Connects the insert method, the database and the [InsertMethodAdapter].
 *
 * The default implementation is [InstantInsertMethodBinder] that executes the insert synchronously.
 * If the insert is deferred, rather than synchronously, alternatives implementations can be
 * implemented using this interface (e.g. RxJava, coroutines etc).
 */
abstract class InsertMethodBinder(val adapter: InsertMethodAdapter?) {

    /**
     * Received the insert method parameters, the insertion adapters and generations the code that
     * runs the insert and returns the result.
     *
     * For example, for the DAO method:
     * ```
     * @Insert
     * fun addPublishers(vararg publishers: Publisher): List<Long>
     * ```
     * The following code will be generated:
     *
     * ```
     * __db.beginTransaction();
     * try {
     *  List<Long> _result = __insertionAdapterOfPublisher.insertAndReturnIdsList(publishers);
     *  __db.setTransactionSuccessful();
     *  return _result;
     * } finally {
     *  __db.endTransaction();
     * }
     * ```
     */
    abstract fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        insertionAdapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        dbField: FieldSpec,
        scope: CodeGenScope
    )
}