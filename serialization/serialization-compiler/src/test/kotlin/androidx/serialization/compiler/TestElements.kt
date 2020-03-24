/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.serialization.compiler

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ElementVisitor
import javax.lang.model.element.Modifier
import javax.lang.model.element.Name
import javax.lang.model.element.NestingKind
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

/**
 * Generates a test implementation of [TypeElement].
 *
 * This allows testing code generation in isolation without a processing environment. The
 * [TypeElement.getEnclosingElement] of the returned type will be an appropriate package element
 * or a hierarchy of nested type elements if more than one simple name is supplied.
 */
internal fun testTypeElement(
    packageName: String,
    vararg simpleNames: String,
    kind: ElementKind = ElementKind.CLASS,
    modifiers: Set<Modifier> = emptySet()
): TypeElement {
    require(simpleNames.isNotEmpty())
    var qualifiedName = "$packageName.${simpleNames.first()}"
    var cursor = TestTypeElement(
        enclosingElement = TestPackageElement(packageName),
        nestingKind = NestingKind.TOP_LEVEL,
        simpleName = simpleNames.first(),
        qualifiedName = qualifiedName,
        modifiers = modifiers,
        kind = kind
    )

    for (i in 1 until simpleNames.size) {
        qualifiedName = "$qualifiedName.${simpleNames[i]}"
        cursor = TestTypeElement(
            enclosingElement = cursor,
            nestingKind = NestingKind.MEMBER,
            simpleName = simpleNames[i],
            qualifiedName = qualifiedName,
            modifiers = modifiers,
            kind = kind
        )
    }

    return cursor
}

/** Generates a test implementation of [VariableElement]. */
internal fun testVariableElement(
    simpleName: String,
    kind: ElementKind,
    enclosingElement: Element? = null,
    vararg modifiers: Modifier
): VariableElement {
    return TestVariableElement(
        simpleName,
        enclosingElement,
        kind,
        modifiers.toSet()
    )
}

private class TestName(
    private val name: String
) : Name, CharSequence by name {
    override fun contentEquals(cs: CharSequence): Boolean = name.contentEquals(cs)
    override fun toString(): String = name
}

private abstract class AbstractTestElement(
    simpleName: String,
    private val enclosingElement: Element?,
    private val kind: ElementKind,
    private val modifiers: Set<Modifier>
) : Element {
    private val enclosedElements = mutableListOf<AbstractTestElement>()
    private val simpleName = TestName(simpleName)

    init {
        enclosingElement?.let {
            when (it) { is AbstractTestElement -> it.enclosedElements += this }
        }
    }

    override fun getModifiers(): Set<Modifier> = modifiers
    override fun getSimpleName(): Name = simpleName
    override fun getKind(): ElementKind = kind
    override fun getEnclosingElement(): Element? = enclosingElement
    override fun getEnclosedElements(): List<Element> = enclosedElements

    override fun getAnnotationMirrors() = emptyList<AnnotationMirror>()

    override fun asType(): TypeMirror {
        throw UnsupportedOperationException("Test Element implementation")
    }

    override fun <A : Annotation> getAnnotationsByType(annotationType: Class<A>): Array<A> {
        notImplemented()
    }

    override fun <A : Annotation> getAnnotation(annotationType: Class<A>): A {
        notImplemented()
    }

    protected fun notImplemented(): Nothing {
        throw UnsupportedOperationException(
            "Test implementation of ${this::class.java.simpleName} does not implement this method"
        )
    }
}

private class TestPackageElement(
    qualifiedName: String
) : PackageElement, AbstractTestElement(
    simpleName = qualifiedName.split(".").last(),
    enclosingElement = null,
    kind = ElementKind.PACKAGE,
    modifiers = emptySet()
) {
    private val qualifiedName = TestName(qualifiedName)

    override fun isUnnamed(): Boolean = qualifiedName.isEmpty()
    override fun getQualifiedName(): Name = qualifiedName

    override fun <R, P> accept(v: ElementVisitor<R, P>, p: P): R {
        return v.visitPackage(this, p)
    }
}

private class TestTypeElement(
    simpleName: String,
    qualifiedName: String,
    enclosingElement: Element?,
    kind: ElementKind,
    modifiers: Set<Modifier>,
    private val nestingKind: NestingKind = NestingKind.TOP_LEVEL
) : TypeElement, AbstractTestElement(simpleName, enclosingElement, kind, modifiers) {
    private val qualifiedNameAsName = TestName(qualifiedName)

    override fun getQualifiedName(): Name = qualifiedNameAsName
    override fun getNestingKind(): NestingKind = nestingKind

    override fun getSuperclass(): TypeMirror = notImplemented()
    override fun getTypeParameters(): List<TypeParameterElement> = notImplemented()
    override fun getInterfaces(): List<TypeMirror> = notImplemented()

    override fun <R, P> accept(v: ElementVisitor<R, P>, p: P): R {
        return v.visitType(this, p)
    }
}

private class TestVariableElement(
    simpleName: String,
    enclosingElement: Element?,
    kind: ElementKind,
    modifiers: Set<Modifier>
) : VariableElement, AbstractTestElement(simpleName, enclosingElement, kind, modifiers) {
    override fun getConstantValue(): Any = notImplemented()

    override fun <R, P> accept(v: ElementVisitor<R, P>, p: P): R {
        return v.visitVariable(this, p)
    }
}
