/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ElementVisitor
import javax.lang.model.element.Modifier
import javax.lang.model.element.Name
import javax.lang.model.type.TypeMirror

/**
 * Implementation of [OriginatingElementWrapper] to wrap a [KSFile] for the case of a dependency
 * within the sources being processed.
 */
internal data class KSFileAsOriginatingElement(
    val ksFile: KSFile
) : OriginatingElementWrapper(ksFile.fileName)

/**
 * Implementation of [OriginatingElementWrapper] to wrap a [KSClassDeclaration] for the case of
 * a dependency in the classpath, in which case a [KSFile] is not available.
 */
internal data class KSClassDeclarationAsOriginatingElement(
    val ksClassDeclaration: KSClassDeclaration
) : OriginatingElementWrapper(ksClassDeclaration.simpleName.asString())

/**
 * When generating java code, JavaPoet only provides an API that receives Element.
 * This wrapper class helps us wrap KSP constructs which KspFiler can unwrap later.
 */
internal sealed class OriginatingElementWrapper(
    val elementSimpleName: String
) : Element {
    override fun getAnnotationMirrors(): List<AnnotationMirror> {
        return emptyList()
    }

    override fun <A : Annotation?> getAnnotation(annotationType: Class<A>?): A? {
        return null
    }

    override fun <A : Annotation?> getAnnotationsByType(annotationType: Class<A>): Array<A> {
        @Suppress("UNCHECKED_CAST")
        return arrayOfNulls<Any?>(size = 0) as Array<A>
    }

    override fun asType(): TypeMirror {
        throw UnsupportedOperationException(
            "${this::class.simpleName} cannot be converted to a type"
        )
    }

    override fun getKind(): ElementKind {
        return ElementKind.OTHER
    }

    override fun getModifiers(): Set<Modifier> {
        return emptySet()
    }

    override fun getSimpleName(): Name {
        return NameImpl(elementSimpleName)
    }

    override fun getEnclosingElement(): Element? {
        return null
    }

    override fun getEnclosedElements(): List<Element> {
        return emptyList()
    }

    override fun <R : Any?, P : Any?> accept(v: ElementVisitor<R, P>?, p: P): R? {
        return null
    }

    private class NameImpl(private val str: String) : Name, CharSequence by str {
        override fun contentEquals(cs: CharSequence): Boolean {
            return str == cs.toString()
        }
    }
}
