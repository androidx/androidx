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
import androidx.room.solver.shortcut.result.DeleteOrUpdateMethodAdapter
import androidx.room.vo.ShortcutQueryParameter
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec

/**
 * Connects the delete or update method, the database and the [DeleteOrUpdateMethodAdapter].
 *
 * The default implementation is [InstantDeleteOrUpdateMethodBinder] that executes the delete/update
 * synchronously.
 * If the delete/update is deferred, rather than synchronously, alternatives implementations can be
 * implemented using this interface (e.g. RxJava, coroutines etc).
 */
abstract class DeleteOrUpdateMethodBinder(val adapter: DeleteOrUpdateMethodAdapter?) {

    /**
     * Received the delete/update method parameters, the adapters and generates the code that
     * runs the delete/update and returns the result.
     *
     * For example, for the DAO method
     * ```
     * @Delete
     * fun deletePublishers(vararg publishers: Publisher)
     * ```
     * The following code will be generated:
     *
     * ```
     * __db.beginTransaction();
     * try {
     *   __deletionAdapterOfPublisher.handleMultiple(publishers);
     *   __db.setTransactionSuccessful();
     * } finally {
     *   __db.endTransaction();
     * }
     * ```
     */
    abstract fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        dbField: FieldSpec,
        scope: CodeGenScope
    )
}