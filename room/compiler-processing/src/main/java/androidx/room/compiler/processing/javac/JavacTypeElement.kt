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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XEnumTypeElement
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.javac.kotlin.KotlinMetadataElement
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.ClassName
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter

internal sealed class JavacTypeElement(
    env: JavacProcessingEnv,
    override val element: TypeElement
) : JavacElement(env, element), XTypeElement, XHasModifiers by JavacHasModifiers(element) {

    override val name: String
        get() = element.simpleName.toString()

    @Suppress("UnstableApiUsage")
    override val packageName: String
        get() = MoreElements.getPackage(element).qualifiedName.toString()

    val kotlinMetadata by lazy {
        KotlinMetadataElement.createFor(element)
    }

    override val qualifiedName by lazy {
        element.qualifiedName.toString()
    }

    override val className: ClassName by lazy {
        ClassName.get(element)
    }
    override val enclosingTypeElement: XTypeElement? by lazy {
        element.enclosingType(env)
    }

    private val _allFieldsIncludingPrivateSupers by lazy {
        element.getAllFieldsIncludingPrivateSupers(
            env.elementUtils
        ).map {
            JavacFieldElement(
                env = env,
                element = it,
                containing = this
            )
        }
    }

    override fun getAllFieldsIncludingPrivateSupers(): List<XFieldElement> {
        return _allFieldsIncludingPrivateSupers
    }

    override fun isKotlinObject() = kotlinMetadata?.isObject() == true
    override fun isCompanionObject() = kotlinMetadata?.isCompanionObject() == true
    override fun isDataClass() = kotlinMetadata?.isDataClass() == true
    override fun isValueClass() = kotlinMetadata?.isValueClass() == true
    override fun isFunctionalInterface() = kotlinMetadata?.isFunctionalInterface() == true
    override fun isExpect() = kotlinMetadata?.isExpect() == true

    override fun isAnnotationClass(): Boolean {
        return kotlinMetadata?.isAnnotationClass()
            ?: (element.kind == ElementKind.ANNOTATION_TYPE)
    }

    override fun isClass(): Boolean {
        return kotlinMetadata?.isClass() ?: (element.kind == ElementKind.CLASS)
    }

    override fun isInterface(): Boolean {
        return kotlinMetadata?.isInterface() ?: (element.kind == ElementKind.INTERFACE)
    }

    override fun findPrimaryConstructor(): JavacConstructorElement? {
        val primarySignature = kotlinMetadata?.findPrimaryConstructorSignature() ?: return null
        return getConstructors().firstOrNull {
            primarySignature == it.descriptor
        }
    }

    private val _declaredMethods by lazy {
        ElementFilter.methodsIn(element.enclosedElements).map {
            JavacMethodElement(
                env = env,
                containing = this,
                element = it
            )
        }
    }

    override fun getDeclaredMethods(): List<XMethodElement> {
        return _declaredMethods
    }

    override fun getConstructors(): List<JavacConstructorElement> {
        return ElementFilter.constructorsIn(element.enclosedElements).map {
            JavacConstructorElement(
                env = env,
                containing = this,
                element = it
            )
        }
    }

    override fun getSuperInterfaceElements(): List<XTypeElement> {
        return element.interfaces.map {
            env.wrapTypeElement(MoreTypes.asTypeElement(it))
        }
    }

    override val type: JavacDeclaredType by lazy {
        env.wrap<JavacDeclaredType>(
            typeMirror = element.asType(),
            kotlinType = kotlinMetadata?.kmType,
            elementNullability = element.nullability
        )
    }

    override val superType: JavacType? by lazy {
        // javac models non-existing types as TypeKind.NONE but we prefer to make it nullable.
        // just makes more sense and safer as we don't need to check for none.

        // The result value is a JavacType instead of JavacDeclaredType to gracefully handle
        // cases where super is an error type.
        val superClass = element.superclass
        if (superClass.kind == TypeKind.NONE) {
            null
        } else {
            env.wrap<JavacType>(
                typeMirror = superClass,
                kotlinType = kotlinMetadata?.superType,
                elementNullability = element.nullability
            )
        }
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(element)
    }

    class DefaultJavacTypeElement(
        env: JavacProcessingEnv,
        element: TypeElement
    ) : JavacTypeElement(env, element)

    class JavacEnumTypeElement(
        env: JavacProcessingEnv,
        element: TypeElement
    ) : JavacTypeElement(env, element), XEnumTypeElement {
        init {
            check(element.kind == ElementKind.ENUM)
        }

        override val enumConstantNames: Set<String> by lazy {
            element.enclosedElements.filter {
                it.kind == ElementKind.ENUM_CONSTANT
            }.mapTo(mutableSetOf()) {
                it.simpleName.toString()
            }
        }
    }

    companion object {
        fun create(
            env: JavacProcessingEnv,
            typeElement: TypeElement
        ): JavacTypeElement {
            return when (typeElement.kind) {
                ElementKind.ENUM -> JavacEnumTypeElement(env, typeElement)
                else -> DefaultJavacTypeElement(env, typeElement)
            }
        }
    }
}
