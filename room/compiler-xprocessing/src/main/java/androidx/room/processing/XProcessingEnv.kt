/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.processing

import androidx.room.processing.javac.JavacProcessingEnv
import com.squareup.javapoet.TypeName
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import kotlin.reflect.KClass

interface XProcessingEnv {

    val messager: XMessager

    val options: Map<String, String>

    val filer: Filer

    fun findTypeElement(qName: String): XTypeElement?

    fun findType(qName: String): XType?

    fun requireType(qName: String): XType = checkNotNull(findType(qName)) {
        "cannot find required type $qName"
    }

    fun findGeneratedAnnotation(): XTypeElement?

    fun getDeclaredType(type: XTypeElement, vararg types: XType): XDeclaredType

    fun getArrayType(type: XType): XArrayType

    fun requireTypeElement(qName: String): XTypeElement {
        return checkNotNull(findTypeElement(qName)) {
            "Cannot find required type element $qName"
        }
    }

    // helpers for smooth migration, these could be extension methods
    fun requireType(typeName: TypeName) = requireType(typeName.toString())

    fun requireType(klass: KClass<*>) = requireType(klass.java.canonicalName!!)

    fun findType(typeName: TypeName) = findType(typeName.toString())

    fun findType(klass: KClass<*>) = findType(klass.java.canonicalName!!)

    fun requireTypeElement(typeName: TypeName) = requireTypeElement(typeName.toString())

    fun requireTypeElement(klass: KClass<*>) = requireTypeElement(klass.java.canonicalName!!)

    fun findTypeElement(typeName: TypeName) = findTypeElement(typeName.toString())

    fun findTypeElement(klass: KClass<*>) = findTypeElement(klass.java.canonicalName!!)

    fun getArrayType(typeName: TypeName) = getArrayType(
        requireType(typeName)
    )

    companion object {
        fun create(env: ProcessingEnvironment): XProcessingEnv = JavacProcessingEnv(env)
    }
}
