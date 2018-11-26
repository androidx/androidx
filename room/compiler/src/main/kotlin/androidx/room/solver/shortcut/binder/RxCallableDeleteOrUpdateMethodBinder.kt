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

import androidx.room.ext.L
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.T
import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.solver.shortcut.result.DeleteOrUpdateMethodAdapter
import androidx.room.vo.ShortcutQueryParameter
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.type.TypeMirror

/**
 * Binder that knows how to write delete and update methods that return Completable/Single/Maybe.
 *
 * For example, the generated code for the DAO function:
 * ```
 * @Delete
 * fun deletePublishers(vararg publishers: Publisher): Single<Int>
 * ```
 * Will be:
 * ```
 * public override fun deletePublishers(vararg publishers: Publisher): Single<Int> {
 *  return Single.fromCallable(Callable {
 *      var _total = 0
 *      __db.beginTransaction()
 *      try {
 *          _total += __deletionAdapterOfPublisher.handleMultiple(publishers)
 *          __db.setTransactionSuccessful()
 *          return@Callable _total
 *      } finally {
 *      __db.endTransaction()
 *      }
 *  })
 * }
 * ```
 * The generation of the code in the call method is delegated to
 * [InstantDeleteOrUpdateMethodBinder].
 */
class RxCallableDeleteOrUpdateMethodBinder(
    private val rxType: RxType,
    private val typeMirror: TypeMirror,
    adapter: DeleteOrUpdateMethodAdapter?
) : DeleteOrUpdateMethodBinder(adapter) {

    private val instantDeleteOrUpdateMethodBinder = InstantDeleteOrUpdateMethodBinder(adapter)

    /**
     * Generate the implementation of the callable:
     * ```
     *  @Override
     *  public Integer call() throws Exception {
     *      int _total = 0;
     *      __db.beginTransaction();
     *      try {
     *          __deletionAdapterOfPublisher.handleMultiple(publishers);
     *          __db.setTransactionSuccessful();
     *          return _total;
     *      } finally {
     *      __db.endTransaction();
     *  }
     * ```
     */

    override fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        scope: CodeGenScope
    ) {
        val paramType = if (rxType == RxType.COMPLETABLE) {
            Void::class.typeName()
        } else {
            typeMirror.typeName()
        }
        val callable = CallableTypeSpecBuilder(paramType) {
            val adapterScope = scope.fork()
            // delegate the generation of the code in the call method to the instant method binder
            instantDeleteOrUpdateMethodBinder.convertAndReturn(
                parameters = parameters,
                adapters = adapters,
                scope = adapterScope
            )
            addCode(adapterScope.generate())
        }.build()
        scope.builder().apply {
            addStatement("return $T.fromCallable($L)", rxType.className, callable)
        }
    }

    /**
     * Supported types for delete and update
     */
    enum class RxType(val className: ClassName) {
        SINGLE(RxJava2TypeNames.SINGLE),
        MAYBE(RxJava2TypeNames.MAYBE),
        COMPLETABLE(RxJava2TypeNames.COMPLETABLE)
    }
}