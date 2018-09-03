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
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.solver.shortcut.result.InsertMethodAdapter
import androidx.room.vo.ShortcutQueryParameter
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror

/**
 * Binder that knows how to write insert methods that return Completable/Single/Maybe.
 *
 * For example, the generated code for:
 * ```
 * @Insert
 * fun addPublishers(vararg publishers: Publisher): Single<List<Long>>
 * ```
 * Will be:
 * ```
 * public Single<List<Long>> addPublishersSingle(Publisher... publishers){
 *      return Single.fromCallable(new Callable<List<Long>>() {
 *      @Override
 *      public List<Long> call() throws Exception {
 *          __db.beginTransaction();
 *          try {
 *              List<Long> _result =
 *                      __insertionAdapterOfPublisher.insertAndReturnIdsList(publishers);
 *              __db.setTransactionSuccessful();
 *          return _result;
 *          } finally {
 *          __db.endTransaction();
 *          }
 *      }
 *  });
 * }
 * ```
 * The generation of the code in the call method is delegated to the [InstantInsertMethodBinder].
 */
class RxCallableInsertMethodBinder(
    private val rxType: RxType,
    private val typeMirror: TypeMirror,
    adapter: InsertMethodAdapter?
) : InsertMethodBinder(adapter) {

    private val instantInsertMethodBinder = InstantInsertMethodBinder(adapter)

    override fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        insertionAdapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        scope: CodeGenScope
    ) {
        val callable = TypeSpec.anonymousClassBuilder("").apply {
            val typeName = typeMirror.typeName()
            if (rxType == RxType.COMPLETABLE) {
                // Since Completable is not parameterized and the Callable should return Void
                // We can just create a Callable without type
                superclass(java.util.concurrent.Callable::class.typeName())
            } else {
                // Create a parameterized Callable object
                superclass(ParameterizedTypeName.get(
                        java.util.concurrent.Callable::class.typeName(),
                        typeName
                ))
            }
            addMethod(createCallMethod(
                    parameters = parameters,
                    insertionAdapters = insertionAdapters,
                    scope = scope
            ))
        }.build()
        scope.builder().apply {
            addStatement("return $T.fromCallable($L)", rxType.className, callable)
        }
    }

    /**
     * Generate the implementation of the callable:
     * ```
     *  @Override
     *  public List<Long> call() throws Exception {
     *      __db.beginTransaction();
     *      try {
     *          List<Long> _result = __insertionAdapterOfPublisher.insertAndReturnIdsList(publishers);
     *          __db.setTransactionSuccessful();
     *          return _result;
     *      } finally {
     *         __db.endTransaction();
     *      }
     *  }
     * ```
     */
    private fun createCallMethod(
        parameters: List<ShortcutQueryParameter>,
        insertionAdapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        scope: CodeGenScope
    ): MethodSpec {
        val adapterScope = scope.fork()
        return MethodSpec.methodBuilder("call").apply {
            // For completable, we just return Void, instead of the type
            if (rxType == RxType.COMPLETABLE) {
                returns(Void::class.typeName())
            } else {
                returns(typeMirror.typeName())
            }
            addException(Exception::class.typeName())
            addModifiers(Modifier.PUBLIC)
            addAnnotation(Override::class.java)
            // delegate the generation of the code in the call method to the instant method binder
            instantInsertMethodBinder.convertAndReturn(
                    parameters = parameters,
                    insertionAdapters = insertionAdapters,
                    scope = adapterScope
            )
            addCode(adapterScope.generate())
        }.build()
    }

    /**
     * Supported types for insert
     */
    enum class RxType(val className: ClassName) {
        SINGLE(RxJava2TypeNames.SINGLE),
        MAYBE(RxJava2TypeNames.MAYBE),
        COMPLETABLE(RxJava2TypeNames.COMPLETABLE)
    }
}