/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.ext

import androidx.room.processor.Context
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.KotlinMetadataUtils
import me.eugeniomarletti.kotlin.metadata.isPrimary
import me.eugeniomarletti.kotlin.metadata.isSuspend
import me.eugeniomarletti.kotlin.metadata.jvm.getJvmConstructorSignature
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.shadow.serialization.deserialization.getName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement

/**
 * Utility class for processors that wants to run kotlin specific code.
 */
class KotlinMetadataElement private constructor(
    val context: Context,
    val element: Element,
    private val classMetadata: KotlinClassMetadata
) : KotlinMetadataUtils {

    override val processingEnv: ProcessingEnvironment
        get() = context.processingEnv

    /**
     * Returns the parameter names of the function or constructor if all have names embedded in the
     * metadata.
     */
    fun getParameterNames(method: ExecutableElement): List<String>? {
        val valueParameterList = classMetadata.data.getFunctionOrNull(method)?.valueParameterList
            ?: findConstructor(method)?.valueParameterList
            ?: return null
        return if (valueParameterList.all { it.hasName() }) {
            valueParameterList.map {
                classMetadata.data.nameResolver.getName(it.name)
                    .asString()
                    .replace("`", "")
                    .removeSuffix("?")
                    .trim()
            }
        } else {
            null
        }
    }

    /**
     * Finds the kotlin metadata for a constructor.
     */
    private fun findConstructor(
        executableElement: ExecutableElement
    ): ProtoBuf.Constructor? = classMetadata?.let { metadata ->
        val (nameResolver, classProto) = metadata.data
        val jvmSignature = executableElement.jvmMethodSignature
        // find constructor
        return classProto.constructorList.singleOrNull {
            it.getJvmConstructorSignature(nameResolver, classProto.typeTable) == jvmSignature
        }
    }

    /**
     * Finds the primary constructor signature of the class.
     */
    fun findPrimaryConstructorSignature() = classMetadata.data.let { data ->
        data.classProto
            .constructorList.first { it.isPrimary }
            .getJvmConstructorSignature(
                data.nameResolver,
                data.classProto.typeTable
            )
    }

    fun getMethodSignature(executableElement: ExecutableElement) =
        executableElement.jvmMethodSignature

    /**
     * Checks if a method is a suspend function.
     */
    fun isSuspendFunction(method: ExecutableElement) =
        classMetadata.data.getFunctionOrNull(method)?.isSuspend == true

    companion object {

        /**
         * Creates a [KotlinMetadataElement] for the given element if it contains Kotlin metadata,
         * otherwise this method returns null.
         *
         * Usually the [element] passed must represent a class. For example, if kotlin metadata is
         * desired for a method, then the containing method should be used as parameter.
         */
        fun createFor(context: Context, element: Element): KotlinMetadataElement? {
            val metadata = try {
                element.kotlinMetadata
            } catch (throwable: Throwable) {
                context.logger.d(element, "failed to read get kotlin metadata from %s", element)
            } as? KotlinClassMetadata
            return if (metadata != null) {
                KotlinMetadataElement(context, element, metadata)
            } else {
                null
            }
        }
    }
}