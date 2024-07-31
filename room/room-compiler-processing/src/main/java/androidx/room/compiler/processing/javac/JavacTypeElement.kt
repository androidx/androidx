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

import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XEnumEntry
import androidx.room.compiler.processing.XEnumTypeElement
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XMemberContainer
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.XTypeParameterElement
import androidx.room.compiler.processing.collectAllMethods
import androidx.room.compiler.processing.collectFieldsIncludingPrivateSupers
import androidx.room.compiler.processing.filterMethodsByConfig
import androidx.room.compiler.processing.javac.kotlin.KmClassContainer
import androidx.room.compiler.processing.util.MemoizedSequence
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.ClassName
import com.squareup.kotlinpoet.javapoet.JClassName
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter

internal sealed class JavacTypeElement(env: JavacProcessingEnv, override val element: TypeElement) :
    JavacElement(env, element), XTypeElement {

    override val name: String
        get() = element.simpleName.toString()

    override val packageName: String by lazy { packageElement.qualifiedName }

    @Suppress("UnstableApiUsage")
    override val packageElement: JavacPackageElement by lazy {
        JavacPackageElement(env, MoreElements.getPackage(element))
    }

    override val kotlinMetadata by lazy { KmClassContainer.createFor(env, element) }

    override val qualifiedName by lazy { element.qualifiedName.toString() }

    @Deprecated(
        "Use asClassName().toJavaPoet() to be clear the name is for JavaPoet.",
        replaceWith =
            ReplaceWith("asClassName().toJavaPoet()", "androidx.room.compiler.codegen.toJavaPoet")
    )
    override val className: ClassName by lazy { xClassName.java }

    private val xClassName: XClassName by lazy {
        XClassName(JClassName.get(element), XTypeName.UNAVAILABLE_KTYPE_NAME, XNullability.NONNULL)
    }

    override fun asClassName() = xClassName

    override val enclosingElement: XMemberContainer? by lazy { enclosingTypeElement }

    override val typeParameters: List<XTypeParameterElement> by lazy {
        element.typeParameters.mapIndexed { index, typeParameter ->
            val typeParameterMetadata = kotlinMetadata?.typeParameters?.get(index)
            JavacTypeParameterElement(env, this, typeParameter, typeParameterMetadata)
        }
    }

    override val closestMemberContainer: JavacTypeElement
        get() = this

    override val enclosingTypeElement: XTypeElement? by lazy { element.enclosingType(env) }

    private val _declaredFields by lazy {
        ElementFilter.fieldsIn(element.enclosedElements)
            .filterNot { it.kind == ElementKind.ENUM_CONSTANT }
            .map {
                JavacFieldElement(
                    env = env,
                    element = it,
                )
            }
            // To be consistent with KSP consider delegates to not have a backing field.
            .filterNot { it.kotlinMetadata?.isDelegated() == true }
    }

    private val allMethods = MemoizedSequence { collectAllMethods(this) }

    private val allFieldsIncludingPrivateSupers = MemoizedSequence {
        collectFieldsIncludingPrivateSupers(this)
    }

    override fun getAllMethods(): Sequence<XMethodElement> = allMethods

    override fun getAllFieldsIncludingPrivateSupers() = allFieldsIncludingPrivateSupers

    override fun getDeclaredFields(): List<XFieldElement> {
        return _declaredFields
    }

    override fun isKotlinObject() =
        kotlinMetadata?.isObject() == true || kotlinMetadata?.isCompanionObject() == true

    override fun isCompanionObject() = kotlinMetadata?.isCompanionObject() == true

    override fun isDataClass() = kotlinMetadata?.isDataClass() == true

    override fun isValueClass() = kotlinMetadata?.isValueClass() == true

    override fun isFunctionalInterface() = kotlinMetadata?.isFunctionalInterface() == true

    override fun isExpect() = kotlinMetadata?.isExpect() == true

    override fun isAnnotationClass(): Boolean {
        return kotlinMetadata?.isAnnotationClass() ?: (element.kind == ElementKind.ANNOTATION_TYPE)
    }

    override fun isClass(): Boolean {
        return kotlinMetadata?.isClass() ?: (element.kind == ElementKind.CLASS)
    }

    override fun isNested(): Boolean {
        return element.enclosingType(env) != null
    }

    override fun isInterface(): Boolean {
        return kotlinMetadata?.isInterface() ?: (element.kind == ElementKind.INTERFACE)
    }

    override fun isRecordClass(): Boolean {
        val recordType = env.findType("java.lang.Record") ?: return false
        return superClass?.let { recordType.isAssignableFrom(it) } == true
    }

    override fun findPrimaryConstructor(): JavacConstructorElement? {
        val primarySignature = kotlinMetadata?.primaryConstructorSignature ?: return null
        return getConstructors().firstOrNull { primarySignature == it.jvmDescriptor }
    }

    private val _declaredMethods by lazy {
        val companionObjectMethodDescriptors =
            getEnclosedTypeElements()
                .firstOrNull { it.isCompanionObject() }
                ?.getDeclaredMethods()
                ?.map { it.jvmDescriptor } ?: emptyList()

        val declaredMethods =
            ElementFilter.methodsIn(element.enclosedElements)
                .map { JavacMethodElement(env = env, element = it) }
                .filterMethodsByConfig(env)
        if (companionObjectMethodDescriptors.isEmpty()) {
            declaredMethods
        } else {
            buildList {
                addAll(
                    declaredMethods.filterNot { method ->
                        companionObjectMethodDescriptors.any { it == method.jvmDescriptor }
                    }
                )
                companionObjectMethodDescriptors.forEach {
                    for (method in declaredMethods) {
                        if (method.jvmDescriptor == it) {
                            add(method)
                            break
                        }
                    }
                }
            }
        }
    }

    override fun getDeclaredMethods(): List<JavacMethodElement> {
        // TODO(b/290800523): Remove the synthetic annotations method from the list
        //  of declared methods so that KAPT matches KSP.
        return _declaredMethods
    }

    fun getSyntheticMethodsForAnnotations(): List<JavacMethodElement> {
        return _declaredMethods.filter {
            it.kotlinMetadata?.isSyntheticMethodForAnnotations() == true
        }
    }

    override fun getConstructors(): List<JavacConstructorElement> {
        return ElementFilter.constructorsIn(element.enclosedElements).map {
            JavacConstructorElement(env = env, element = it)
        }
    }

    override fun getSuperInterfaceElements(): List<XTypeElement> {
        return element.interfaces.map { env.wrapTypeElement(MoreTypes.asTypeElement(it)) }
    }

    override fun getEnclosedTypeElements(): List<XTypeElement> {
        return ElementFilter.typesIn(element.enclosedElements).map { env.wrapTypeElement(it) }
    }

    override val type: JavacDeclaredType by lazy {
        env.wrap(
            typeMirror = element.asType(),
            kotlinType = kotlinMetadata?.type,
            elementNullability = element.nullability
        )
    }

    override val superClass: JavacType? by lazy {
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

    override val superInterfaces by lazy {
        val superTypesFromKotlinMetadata =
            kotlinMetadata?.superTypes?.associateBy { it.className } ?: emptyMap()
        element.interfaces.map {
            check(it is DeclaredType)
            val interfaceName = ClassName.get(MoreElements.asType(it.asElement()))
            val element = MoreTypes.asTypeElement(it)
            env.wrap<JavacType>(
                typeMirror = it,
                kotlinType = superTypesFromKotlinMetadata[interfaceName.canonicalName()],
                elementNullability = element.nullability
            )
        }
    }

    override fun isFromJava(): Boolean {
        return element.asType().kind != TypeKind.ERROR && !hasAnnotation(Metadata::class)
    }

    override fun isFromKotlin(): Boolean {
        return element.asType().kind != TypeKind.ERROR && hasAnnotation(Metadata::class)
    }

    class DefaultJavacTypeElement(env: JavacProcessingEnv, element: TypeElement) :
        JavacTypeElement(env, element)

    class JavacEnumTypeElement(env: JavacProcessingEnv, element: TypeElement) :
        JavacTypeElement(env, element), XEnumTypeElement {
        init {
            check(element.kind == ElementKind.ENUM)
        }

        override val entries: Set<XEnumEntry> by lazy {
            element.enclosedElements
                .filter { it.kind == ElementKind.ENUM_CONSTANT }
                .mapTo(mutableSetOf()) { JavacEnumEntry(env, it, this) }
        }
    }

    companion object {
        fun create(env: JavacProcessingEnv, typeElement: TypeElement): JavacTypeElement {
            return when (typeElement.kind) {
                ElementKind.ENUM -> JavacEnumTypeElement(env, typeElement)
                else -> DefaultJavacTypeElement(env, typeElement)
            }
        }
    }
}
