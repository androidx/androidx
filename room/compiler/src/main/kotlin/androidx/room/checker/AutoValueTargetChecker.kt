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

package androidx.room.checker

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.ext.getAllAbstractMethodsIncludingSupers
import androidx.room.ext.hasAnnotation
import androidx.room.ext.hasAnyOf
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import asTypeElement
import com.google.auto.common.MoreElements
import com.google.auto.value.AutoValue
import com.google.common.collect.SetMultimap
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter

/**
 * Checks that certain Room annotations with @Target(METHOD) are not used in methods others than
 * those implemented by the AutoValue processor.
 */
class AutoValueTargetChecker(
    val context: Context,
    private val annotatedElements: SetMultimap<Class<out Annotation>, Element>
) {
    fun check() {
        val annotatedMethods = supportedAnnotations.flatMap { annotation ->
            annotatedElements[annotation]?.let {
                ElementFilter.methodsIn(it).map { MoreElements.asExecutable(it) }
            } ?: emptySet<ExecutableElement>()
        }

        val autoValueElements = annotatedElements[AutoValue::class.java] ?: emptySet()
        val autoValueAbstractGetters = autoValueElements.map { MoreElements.asType(it) }.flatMap {
            it.getAllAbstractMethodsIncludingSupers().filter { it.parameters.size == 0 }
        }
        val autoValueImplementedGetters = annotatedMethods.filter {
            !it.hasAnyOf(Modifier.STATIC, Modifier.ABSTRACT) && it.parameters.size == 0 &&
                    MoreElements.asType(it.enclosingElement).isAutoValueChild()
        }

        val allowedMethods = autoValueAbstractGetters + autoValueImplementedGetters
        (annotatedMethods - allowedMethods).forEach { method ->
            val annotationName = supportedAnnotations.first { method.hasAnnotation(it) }.simpleName
            context.logger.e(method,
                    ProcessorErrors.invalidAnnotationTarget(annotationName, method.kind))
        }
    }

    companion object {
        private val supportedAnnotations = arrayOf(
                PrimaryKey::class.java,
                ColumnInfo::class.java,
                Embedded::class.java,
                Relation::class.java)

        fun requestedAnnotations() = mutableSetOf(*supportedAnnotations, AutoValue::class.java)
    }
}

private fun TypeElement.isAutoValueChild(): Boolean {
    if (superclass.kind != TypeKind.NONE) {
        val superElement = superclass.asTypeElement()
        return superElement.hasAnnotation(AutoValue::class) || superElement.isAutoValueChild()
    } else {
        return false
    }
}