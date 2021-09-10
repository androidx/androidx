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

package androidx.room.compiler.processing.javac.kotlin

import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement

/**
 * Utility class for processors that wants to run kotlin specific code.
 */
internal class KotlinMetadataElement(
    val element: Element,
    private val classMetadata: KotlinClassMetadata.Class
) {
    private val typeInfo: KmClassTypeInfo by lazy {
        lateinit var result: KmClassTypeInfo
        classMetadata.accept(
            ClassAsKmTypeReader {
                result = it
            }
        )
        result
    }
    val kmType
        get() = typeInfo.kmType
    val superType
        get() = typeInfo.superType
    private val functionList: List<KmFunction> by lazy { classMetadata.readFunctions() }
    private val constructorList: List<KmConstructor> by lazy { classMetadata.readConstructors() }
    private val propertyList: List<KmProperty> by lazy { classMetadata.readProperties() }
    private val classFlags: KotlinMetadataClassFlags by lazy {
        KotlinMetadataClassFlags(classMetadata)
    }

    private val ExecutableElement.descriptor: String
        get() = descriptor()

    fun findPrimaryConstructorSignature() = constructorList.firstOrNull {
        it.isPrimary()
    }?.descriptor

    fun isObject(): Boolean = classFlags.isObject()
    fun isCompanionObject(): Boolean = classFlags.isCompanionObject()
    fun isAnnotationClass(): Boolean = classFlags.isAnnotationClass()
    fun isClass(): Boolean = classFlags.isClass()
    fun isInterface(): Boolean = classFlags.isInterface()
    fun isDataClass(): Boolean = classFlags.isDataClass()
    fun isValueClass(): Boolean = classFlags.isValueClass()
    fun isFunctionalInterface(): Boolean = classFlags.isFunctionalInterface()
    fun isExpect(): Boolean = classFlags.isExpect()

    fun getFunctionMetadata(method: ExecutableElement): KmFunction? {
        check(method.kind == ElementKind.METHOD) {
            "must pass an element type of method"
        }
        val methodSignature = method.descriptor
        return functionList.firstOrNull { it.descriptor == methodSignature }
    }

    fun getConstructorMetadata(method: ExecutableElement): KmConstructor? {
        check(method.kind == ElementKind.CONSTRUCTOR) {
            "must pass an element type of constructor"
        }
        val methodSignature = method.descriptor
        return constructorList.firstOrNull { it.descriptor == methodSignature }
    }

    fun getPropertyMetadata(propertyName: String) = propertyList.firstOrNull {
        it.name == propertyName
    }

    companion object {
        /**
         * Creates a [KotlinMetadataElement] for the given element if it contains Kotlin metadata,
         * otherwise this method returns null.
         *
         * Usually the [element] passed must represent a class. For example, if kotlin metadata is
         * desired for a method, then the containing class should be used as parameter.
         */
        fun createFor(element: Element): KotlinMetadataElement? {
            val metadata = getMetadataAnnotation(element)?.run {
                KotlinClassHeader(
                    kind = kind,
                    metadataVersion = metadataVersion,
                    data1 = data1,
                    data2 = data2,
                    extraString = extraString,
                    packageName = packageName,
                    extraInt = extraInt
                ).let {
                    // TODO: Support more metadata kind (file facade, synthetic class, etc...)
                    KotlinClassMetadata.read(it) as? KotlinClassMetadata.Class
                }
            }
            return if (metadata != null) {
                KotlinMetadataElement(element, metadata)
            } else {
                null
            }
        }

        /**
         * Search for Kotlin's Metadata annotation across the element's hierarchy.
         */
        private fun getMetadataAnnotation(element: Element?): Metadata? =
            if (element != null) {
                element.getAnnotation(Metadata::class.java)
                    ?: getMetadataAnnotation(element.enclosingElement)
            } else {
                null
            }
    }
}