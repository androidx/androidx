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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.applyTo
import androidx.room.compiler.processing.XExecutableElement
import androidx.room.compiler.processing.isConstructor
import androidx.room.compiler.processing.isMethod

/**
 * Each Entity / data class we process has a constructor. It might be the empty constructor or a
 * constructor with properties. It can also be a static factory function, such as in the case of an
 * AutoValue data class.
 */
data class Constructor(val element: XExecutableElement, val params: List<Param>) {

    fun hasProperty(property: Property): Boolean {
        return params.any {
            when (it) {
                is Param.PropertyParam -> it.property === property
                is Param.EmbeddedParam -> it.embedded.property === property
                is Param.RelationParam -> it.relation.property === property
            }
        }
    }

    fun writeConstructor(outVar: String, args: String, builder: XCodeBlock.Builder) {
        when {
            element.isConstructor() -> {
                builder.addStatement(
                    "%L = %L",
                    outVar,
                    XCodeBlock.ofNewInstance(element.enclosingElement.asClassName(), args)
                )
            }
            element.isMethod() -> {
                builder.applyTo { language ->
                    // TODO when we generate Kotlin code, we need to handle not having enclosing
                    //  elements.
                    val methodName =
                        when (language) {
                            CodeLanguage.JAVA -> element.jvmName
                            CodeLanguage.KOTLIN -> element.name
                        }
                    addStatement(
                        "%L = %T.%L(%L)",
                        outVar,
                        element.enclosingElement.asClassName(),
                        methodName,
                        args
                    )
                }
            }
            else -> throw IllegalStateException("Invalid constructor kind ${element.kindName()}")
        }
    }

    sealed class Param {

        abstract fun log(): String

        class PropertyParam(val property: Property) : Param() {
            override fun log(): String = property.getPath()
        }

        class EmbeddedParam(val embedded: EmbeddedProperty) : Param() {
            override fun log(): String = embedded.property.getPath()
        }

        class RelationParam(val relation: Relation) : Param() {
            override fun log(): String = relation.property.getPath()
        }
    }
}
