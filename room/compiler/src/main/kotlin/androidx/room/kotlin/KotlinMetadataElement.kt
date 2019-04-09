/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.kotlin

import androidx.room.processor.Context
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement

/**
 * Utility class for processors that wants to run kotlin specific code.
 */
class KotlinMetadataElement(
    val context: Context,
    val element: Element,
    private val classMetadata: KotlinClassMetadata.Class
) {

    private val functionList: List<KmFunction> by lazy { classMetadata.readFunctions() }
    private val constructorList: List<KmConstructor> by lazy { classMetadata.readConstructors() }

    private val ExecutableElement.descriptor: String
        get() = descriptor(context.processingEnv.typeUtils)

    /**
     * Returns the parameter names of the function or constructor if all have names embedded in the
     * metadata.
     */
    fun getParameterNames(method: ExecutableElement): List<String>? {
        val methodSignature = method.descriptor
        val paramList =
            functionList.firstOrNull { it.descriptor == methodSignature }?.parameters
                ?: constructorList.firstOrNull { it.descriptor == methodSignature }?.parameters
        return paramList?.map { it.name }
    }

    /**
     * Finds the primary constructor descriptor of the class.
     */
    fun findPrimaryConstructorSignature() = constructorList.first { it.isPrimary() }.descriptor

    /**
     * Checks if a method is a suspend function.
     */
    fun isSuspendFunction(method: ExecutableElement) = functionList.firstOrNull {
        it.descriptor == method.descriptor
    }?.isSuspend() ?: false

    companion object {

        /**
         * Creates a [KotlinMetadataElement] for the given element if it contains Kotlin metadata,
         * otherwise this method returns null.
         *
         * Usually the [element] passed must represent a class. For example, if kotlin metadata is
         * desired for a method, then the containing method should be used as parameter.
         */
        fun createFor(context: Context, element: Element): KotlinMetadataElement? {
            val metadata = getMetadataAnnotation(element)?.run {
                KotlinClassHeader(
                    kind = kind,
                    metadataVersion = metadataVersion,
                    bytecodeVersion = bytecodeVersion,
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
                KotlinMetadataElement(context, element, metadata)
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