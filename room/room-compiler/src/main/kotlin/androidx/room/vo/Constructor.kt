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

package androidx.room.vo

import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.compiler.processing.XExecutableElement
import androidx.room.compiler.processing.isConstructor
import androidx.room.compiler.processing.isMethod
import com.squareup.javapoet.CodeBlock

/**
 * For each Entity / Pojo we process has a constructor. It might be the empty constructor or a
 * constructor with fields. It can also be a static factory method, such as in the case of an
 * AutoValue Pojo.
 */
data class Constructor(val element: XExecutableElement, val params: List<Param>) {

    fun hasField(field: Field): Boolean {
        return params.any {
            when (it) {
                is Param.FieldParam -> it.field === field
                is Param.EmbeddedParam -> it.embedded.field === field
                is Param.RelationParam -> it.relation.field === field
            }
        }
    }

    fun writeConstructor(outVar: String, args: String, builder: CodeBlock.Builder) {
        when {
            element.isConstructor() -> {
                builder.addStatement(
                    "$L = new $T($L)", outVar,
                    element.enclosingElement.className, args
                )
            }
            element.isMethod() -> {
                // TODO when we generate Kotlin code, we need to handle not having enclosing
                //  elements.
                builder.addStatement(
                    "$L = $T.$L($L)", outVar,
                    element.enclosingElement.className,
                    element.name, args
                )
            }
            else -> throw IllegalStateException("Invalid constructor kind ${element.kindName()}")
        }
    }

    sealed class Param {

        abstract fun log(): String

        class FieldParam(val field: Field) : Param() {
            override fun log(): String = field.getPath()
        }

        class EmbeddedParam(val embedded: EmbeddedField) : Param() {
            override fun log(): String = embedded.field.getPath()
        }

        class RelationParam(val relation: Relation) : Param() {
            override fun log(): String = relation.field.getPath()
        }
    }
}
