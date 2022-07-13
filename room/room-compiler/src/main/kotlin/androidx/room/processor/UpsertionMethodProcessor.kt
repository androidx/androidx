/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.processor

import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.solver.CodeGenScope
import androidx.room.solver.shortcut.binder.UpsertMethodBinder
import androidx.room.vo.ShortcutEntity
import androidx.room.vo.ShortcutQueryParameter
import androidx.room.vo.UpsertionMethod
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec

class UpsertionMethodProcessor(
    baseContext: Context,
    val containing: XType,
    val executableElement: XMethodElement
) {
    val context = baseContext.fork(executableElement)
    class StubUpsertMethodBinder : UpsertMethodBinder(null) {
        override fun convertAndReturn(
            parameters: List<ShortcutQueryParameter>,
            upsertionAdapters: Map<String, Pair<FieldSpec, TypeSpec>>,
            dbField: FieldSpec,
            scope: CodeGenScope
        ) {}
    }
    fun process(): UpsertionMethod {
        return UpsertionMethod(
            element = executableElement,
            returnType = executableElement.returnType,
            entities = emptyMap<String, ShortcutEntity>(),
            parameters = emptyList<ShortcutQueryParameter>(),
            methodBinder = StubUpsertMethodBinder()
        )
    }
}