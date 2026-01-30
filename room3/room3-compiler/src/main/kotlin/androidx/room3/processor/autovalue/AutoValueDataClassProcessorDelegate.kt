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

package androidx.room3.processor.autovalue

import androidx.room3.Ignore
import androidx.room3.compiler.processing.XExecutableElement
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.processor.Context
import androidx.room3.processor.DataClassProcessor
import androidx.room3.processor.DataClassProcessor.Companion.TARGET_METHOD_ANNOTATIONS
import androidx.room3.processor.ProcessorErrors
import androidx.room3.vo.Constructor
import androidx.room3.vo.DataClass
import androidx.room3.vo.EmbeddedProperty
import androidx.room3.vo.Property
import androidx.room3.vo.Warning
import com.google.auto.value.AutoValue.CopyAnnotations

/** Delegate to process generated AutoValue class as a data class. */
class AutoValueDataClassProcessorDelegate(
    private val context: Context,
    private val autoValueElement: XTypeElement,
) : DataClassProcessor.Delegate {

    private val autoValueDeclaredType: XType by lazy { autoValueElement.type }

    override fun onPreProcess(element: XTypeElement) {
        val allMethods = autoValueElement.getAllMethods()
        val autoValueAbstractGetters =
            allMethods.filter { it.isAbstract() && it.parameters.size == 0 }

        // Warn about missing @AutoValue.CopyAnnotations in the property getters.
        autoValueAbstractGetters.forEach {
            val hasRoomAnnotation = it.hasAnnotationWithPackage("androidx.room3")
            if (hasRoomAnnotation && !it.hasAnnotation(CopyAnnotations::class)) {
                context.logger.w(
                    Warning.MISSING_COPY_ANNOTATIONS,
                    it,
                    ProcessorErrors.MISSING_COPY_ANNOTATIONS,
                )
            }
        }

        // Check that certain Room annotations with @Target(METHOD) are not used in methods other
        // than the auto value abstract getters.
        (allMethods - autoValueAbstractGetters)
            .filter { it.hasAnyAnnotation(*TARGET_METHOD_ANNOTATIONS) }
            .forEach { method ->
                val annotationName =
                    TARGET_METHOD_ANNOTATIONS.first { method.hasAnnotation(it) }.java.simpleName
                context.logger.e(
                    method,
                    ProcessorErrors.invalidAnnotationTarget(annotationName, method.kindName()),
                )
            }
    }

    override fun findConstructors(element: XTypeElement): List<XExecutableElement> {
        return autoValueElement.getDeclaredMethods().filter {
            it.isStatic() &&
                !it.hasAnnotation(Ignore::class) &&
                !it.isPrivate() &&
                it.returnType.isSameType(autoValueElement.type)
        }
    }

    override fun createDataClass(
        element: XTypeElement,
        declaredType: XType,
        properties: List<Property>,
        embeddedProperties: List<EmbeddedProperty>,
        relations: List<androidx.room3.vo.Relation>,
        constructor: Constructor?,
    ): DataClass {
        return DataClass(
            element = element,
            type = autoValueDeclaredType,
            properties = properties,
            embeddedProperties = embeddedProperties,
            relations = relations,
            constructor = constructor,
        )
    }

    companion object {
        /**
         * Gets the generated class name of an AutoValue annotated class.
         *
         * This is the same naming strategy used by AutoValue's processor.
         */
        fun getGeneratedClassName(element: XTypeElement): String {
            var type = element
            var name = type.name
            while (type.enclosingTypeElement != null) {
                type = type.enclosingTypeElement!!
                name = "${type.name}_$name"
            }
            val pkg = type.packageName
            return "$pkg${if (pkg.isEmpty()) "" else "."}AutoValue_$name"
        }
    }
}
