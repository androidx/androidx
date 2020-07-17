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

package androidx.room.processing.javac

import androidx.room.processing.XDeclaredType
import androidx.room.processing.XMessager
import androidx.room.processing.XProcessingEnv
import androidx.room.processing.XType
import androidx.room.processing.XTypeElement
import com.google.auto.common.GeneratedAnnotations
import com.google.auto.common.MoreTypes
import java.util.Locale
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

internal class JavacProcessingEnv(
    val delegate: ProcessingEnvironment
) : XProcessingEnv {

    val elementUtils: Elements = delegate.elementUtils

    val typeUtils: Types = delegate.typeUtils

    override val messager: XMessager by lazy {
        JavacProcessingEnvMessager(delegate)
    }

    override val filer = delegate.filer

    override val options: Map<String, String>
        get() = delegate.options

    override fun findTypeElement(qName: String): JavacTypeElement? {
        val result = delegate.elementUtils.getTypeElement(qName)
        return result?.let(this::wrapTypeElement)
    }

    override fun findType(qName: String): XType? {
        // check for primitives first
        PRIMITIVE_TYPES[qName]?.let {
            return wrap(
                typeUtils.getPrimitiveType(it)
            )
        }
        return findTypeElement(qName)?.type
    }

    override fun findGeneratedAnnotation(): XTypeElement? {
        val element = GeneratedAnnotations.generatedAnnotation(elementUtils, delegate.sourceVersion)
        return if (element.isPresent) {
            wrapTypeElement(element.get())
        } else {
            null
        }
    }

    override fun getDeclaredType(type: XTypeElement, vararg types: XType): XDeclaredType {
        check(type is JavacTypeElement)
        val args = types.map {
            check(it is JavacType)
            it.typeMirror
        }.toTypedArray()
        check(types.all {
            it is JavacType
        })
        return wrap<JavacDeclaredType>(
            typeUtils.getDeclaredType(type.element, *args)
        )
    }

    // maybe cache here ?
    fun wrapTypeElement(element: TypeElement) = JavacTypeElement(this, element)

    inline fun <reified T : JavacType> wrapTypes(types: Iterable<TypeMirror>): List<T> {
        return types.map {
            wrap<T>(it)
        }
    }

    inline fun <reified T : JavacType> wrap(typeMirror: TypeMirror): T {
        return when (typeMirror.kind) {
            TypeKind.DECLARED -> JavacDeclaredType(
                env = this,
                typeMirror = MoreTypes.asDeclared(typeMirror)
            )
            else -> DefaultJavacType(this, typeMirror)
        } as T
    }

    companion object {
        val PRIMITIVE_TYPES = TypeKind.values().filter {
            it.isPrimitive
        }.associateBy {
            it.name.toLowerCase(Locale.US)
        }
    }
}
