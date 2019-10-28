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

package androidx.room.processor.autovalue

import androidx.room.Ignore
import androidx.room.ext.getAllMethodsIncludingSupers
import androidx.room.ext.hasAnnotation
import androidx.room.ext.hasAnyOf
import androidx.room.ext.typeName
import androidx.room.processor.Context
import androidx.room.processor.PojoProcessor
import androidx.room.processor.PojoProcessor.Companion.TARGET_METHOD_ANNOTATIONS
import androidx.room.processor.ProcessorErrors
import androidx.room.vo.Constructor
import androidx.room.vo.EmbeddedField
import androidx.room.vo.Field
import androidx.room.vo.Pojo
import androidx.room.vo.Warning
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.auto.value.AutoValue.CopyAnnotations
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.ElementFilter

/**
 * Delegate to process generated AutoValue class as a Pojo.
 */
class AutoValuePojoProcessorDelegate(
    private val context: Context,
    private val autoValueElement: TypeElement
) : PojoProcessor.Delegate {

    private val autoValueDeclaredType: DeclaredType by lazy {
        MoreTypes.asDeclared(autoValueElement.asType())
    }

    override fun onPreProcess(element: TypeElement) {
        val allMethods = autoValueElement.getAllMethodsIncludingSupers()
        val autoValueAbstractGetters = allMethods
            .filter { it.hasAnyOf(Modifier.ABSTRACT) && it.parameters.size == 0 }

        // Warn about missing @AutoValue.CopyAnnotations in the property getters.
        autoValueAbstractGetters.forEach {
            val hasRoomAnnotation = it.annotationMirrors.map {
                it.annotationType.typeName().toString()
            }.any { it.contains("androidx.room") }
            if (hasRoomAnnotation && !it.hasAnnotation(CopyAnnotations::class)) {
                context.logger.w(Warning.MISSING_COPY_ANNOTATIONS, it,
                        ProcessorErrors.MISSING_COPY_ANNOTATIONS)
            }
        }

        // Check that certain Room annotations with @Target(METHOD) are not used in methods other
        // than the auto value abstract getters.
        (allMethods - autoValueAbstractGetters)
            .filter { it.hasAnyOf(*TARGET_METHOD_ANNOTATIONS) }
            .forEach { method ->
                val annotationName = TARGET_METHOD_ANNOTATIONS.first { method.hasAnnotation(it) }
                    .java.simpleName
                context.logger.e(method,
                    ProcessorErrors.invalidAnnotationTarget(annotationName, method.kind))
            }
    }

    override fun findConstructors(element: TypeElement): List<ExecutableElement> {
        val typeUtils = context.processingEnv.typeUtils
        return ElementFilter.methodsIn(autoValueElement.enclosedElements).filter {
            it.hasAnyOf(Modifier.STATIC) &&
                    !it.hasAnnotation(Ignore::class) &&
                    !it.hasAnyOf(Modifier.PRIVATE) &&
                    typeUtils.isSameType(it.returnType, autoValueElement.asType())
        }
    }

    override fun createPojo(
        element: TypeElement,
        declaredType: DeclaredType,
        fields: List<Field>,
        embeddedFields: List<EmbeddedField>,
        relations: List<androidx.room.vo.Relation>,
        constructor: Constructor?
    ): Pojo {
        return Pojo(element = element,
                type = autoValueDeclaredType,
                fields = fields,
                embeddedFields = embeddedFields,
                relations = relations,
                constructor = constructor)
    }

    companion object {
        /**
         * Gets the generated class name of an AutoValue annotated class.
         *
         * This is the same naming strategy used by AutoValue's processor.
         */
        fun getGeneratedClassName(element: TypeElement): String {
            var type = element
            var name = type.simpleName.toString()
            while (type.enclosingElement is TypeElement) {
                type = type.enclosingElement as TypeElement
                name = "${type.simpleName}_$name"
            }
            val pkg = MoreElements.getPackage(type).qualifiedName.toString()
            return "$pkg${if (pkg.isEmpty()) "" else "."}AutoValue_$name"
        }
    }
}