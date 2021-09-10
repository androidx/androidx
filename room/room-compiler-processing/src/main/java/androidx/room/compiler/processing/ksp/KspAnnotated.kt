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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.InternalXAnnotated
import androidx.room.compiler.processing.XAnnotationBox
import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.unwrapRepeatedAnnotationsFromContainer
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSTypeAlias
import kotlin.reflect.KClass

@OptIn(KspExperimental::class)
internal sealed class KspAnnotated(
    val env: KspProcessingEnv
) : InternalXAnnotated {
    abstract fun annotations(): Sequence<KSAnnotation>

    private fun <T : Annotation> findAnnotations(annotation: KClass<T>): Sequence<KSAnnotation> {
        return annotations().filter { isSameAnnotationClass(it, annotation) }
    }

    override fun getAllAnnotations(): List<XAnnotation> {
        return annotations().map { ksAnnotated ->
            KspAnnotation(env, ksAnnotated)
        }.flatMap { annotation ->
            annotation.unwrapRepeatedAnnotationsFromContainer() ?: listOf(annotation)
        }.toList()
    }

    override fun <T : Annotation> getAnnotations(
        annotation: KClass<T>,
        containerAnnotation: KClass<out Annotation>?
    ): List<XAnnotationBox<T>> {
        // we'll try both because it can be the container or the annotation itself.
        // try container first
        if (containerAnnotation != null) {
            // if container also repeats, this won't work but we don't have that use case
            findAnnotations(containerAnnotation).firstOrNull()?.let {
                return KspAnnotationBox(
                    env = env,
                    annotation = it,
                    annotationClass = containerAnnotation.java,
                ).getAsAnnotationBoxArray<T>("value").toList()
            }
        }
        // didn't find anything with the container, try the annotation class
        return findAnnotations(annotation).map {
            KspAnnotationBox(
                env = env,
                annotationClass = annotation.java,
                annotation = it
            )
        }.toList()
    }

    override fun hasAnnotationWithPackage(pkg: String): Boolean {
        return annotations().any {
            it.annotationType.resolve().declaration.qualifiedName?.getQualifier() == pkg
        }
    }

    override fun hasAnnotation(
        annotation: KClass<out Annotation>,
        containerAnnotation: KClass<out Annotation>?
    ): Boolean {
        return annotations().any {
            isSameAnnotationClass(it, annotation) ||
                (containerAnnotation != null && isSameAnnotationClass(it, containerAnnotation))
        }
    }

    private fun isSameAnnotationClass(
        ksAnnotation: KSAnnotation,
        annotationClass: KClass<out Annotation>
    ): Boolean {
        var declaration = ksAnnotation.annotationType.resolve().declaration
        while (declaration is KSTypeAlias) {
            declaration = declaration.type.resolve().declaration
        }
        val qualifiedName = declaration.qualifiedName?.asString() ?: return false
        return qualifiedName == annotationClass.qualifiedName
    }

    private class KSAnnotatedDelegate(
        env: KspProcessingEnv,
        private val delegate: KSAnnotated,
        private val useSiteFilter: UseSiteFilter
    ) : KspAnnotated(env) {
        override fun annotations(): Sequence<KSAnnotation> {
            return delegate.annotations.asSequence().filter {
                useSiteFilter.accept(it)
            }
        }
    }

    private class NotAnnotated(env: KspProcessingEnv) : KspAnnotated(env) {
        override fun annotations(): Sequence<KSAnnotation> {
            return emptySequence()
        }
    }

    /**
     * TODO: The implementation of UseSiteFilter is not 100% correct until
     * https://github.com/google/ksp/issues/96 is fixed.
     * https://kotlinlang.org/docs/reference/annotations.html
     *
     * More specifically, when a use site is not defined in an annotation, we need to find the
     * declaration of the annotation and decide on the use site based on that.
     * Unfortunately, due to KSP issue #96, we cannot yet read values from a `@Target` annotation
     * which prevents implementing it correctly.
     *
     * Current implementation just approximates it which should work for Room.
     */
    interface UseSiteFilter {
        fun accept(annotation: KSAnnotation): Boolean

        private class Impl(
            val acceptedTarget: AnnotationUseSiteTarget,
            private val acceptNoTarget: Boolean = true,
        ) : UseSiteFilter {
            override fun accept(annotation: KSAnnotation): Boolean {
                val target = annotation.useSiteTarget
                return if (target == null) {
                    acceptNoTarget
                } else {
                    acceptedTarget == target
                }
            }
        }

        companion object {
            val NO_USE_SITE = object : UseSiteFilter {
                override fun accept(annotation: KSAnnotation): Boolean {
                    return annotation.useSiteTarget == null
                }
            }
            val NO_USE_SITE_OR_FIELD: UseSiteFilter = Impl(AnnotationUseSiteTarget.FIELD)
            val NO_USE_SITE_OR_METHOD_PARAMETER: UseSiteFilter =
                Impl(AnnotationUseSiteTarget.PARAM)
            val NO_USE_SITE_OR_GETTER: UseSiteFilter = Impl(AnnotationUseSiteTarget.GET)
            val NO_USE_SITE_OR_SETTER: UseSiteFilter = Impl(AnnotationUseSiteTarget.SET)
            val NO_USE_SITE_OR_SET_PARAM: UseSiteFilter = Impl(AnnotationUseSiteTarget.SETPARAM)
            val FILE: UseSiteFilter = Impl(
                acceptedTarget = AnnotationUseSiteTarget.FILE,
                acceptNoTarget = false
            )
        }
    }

    companion object {
        fun create(
            env: KspProcessingEnv,
            delegate: KSAnnotated?,
            filter: UseSiteFilter
        ): KspAnnotated {
            return delegate?.let {
                KSAnnotatedDelegate(env, it, filter)
            } ?: NotAnnotated(env)
        }
    }
}