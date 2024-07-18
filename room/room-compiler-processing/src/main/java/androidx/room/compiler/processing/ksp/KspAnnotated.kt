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
import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XAnnotationBox
import androidx.room.compiler.processing.unwrapRepeatedAnnotationsFromContainer
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSTypeAlias
import java.lang.annotation.ElementType
import kotlin.reflect.KClass

internal sealed class KspAnnotated(
    val env: KspProcessingEnv
) : InternalXAnnotated {
    abstract fun annotations(): Sequence<KSAnnotation>

    private fun <T : Annotation> findAnnotations(annotation: KClass<T>): Sequence<KSAnnotation> {
        return annotations().filter { it.isSameAnnotationClass(annotation) }
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
            it.annotationType.resolve().declaration.packageName.asString() == pkg
        }
    }

    override fun hasAnnotation(
        annotation: KClass<out Annotation>,
        containerAnnotation: KClass<out Annotation>?
    ): Boolean {
        return annotations().any {
            it.isSameAnnotationClass(annotation) ||
                (containerAnnotation != null && it.isSameAnnotationClass(containerAnnotation))
        }
    }

    private class KSAnnotatedDelegate(
        env: KspProcessingEnv,
        private val delegate: KSAnnotated,
        private val useSiteFilter: UseSiteFilter
    ) : KspAnnotated(env) {
        override fun annotations(): Sequence<KSAnnotation> {
            return delegate.annotations.filter {
                useSiteFilter.accept(env, it)
            }
        }
    }

    private class NotAnnotated(env: KspProcessingEnv) : KspAnnotated(env) {
        override fun annotations(): Sequence<KSAnnotation> {
            return emptySequence()
        }
    }

    /**
     * Annotation use site filter
     *
     * https://kotlinlang.org/docs/annotations.html#annotation-use-site-targets
     */
    interface UseSiteFilter {
        fun accept(env: KspProcessingEnv, annotation: KSAnnotation): Boolean

        private class Impl(
            val acceptedSiteTarget: AnnotationUseSiteTarget? = null,
            val acceptedTargets: Set<AnnotationTarget>,
            private val acceptNoTarget: Boolean = true,
        ) : UseSiteFilter {
            override fun accept(env: KspProcessingEnv, annotation: KSAnnotation): Boolean {
                val useSiteTarget = annotation.useSiteTarget
                val annotationTargets = annotation.getDeclaredTargets(env)
                return if (useSiteTarget != null) {
                    acceptedSiteTarget == useSiteTarget
                } else if (annotationTargets.isNotEmpty()) {
                    annotationTargets.any { acceptedTargets.contains(it) }
                } else {
                    acceptNoTarget
                }
            }
        }

        companion object {
            val NO_USE_SITE = object : UseSiteFilter {
                override fun accept(env: KspProcessingEnv, annotation: KSAnnotation): Boolean {
                    return annotation.useSiteTarget == null
                }
            }
            val NO_USE_SITE_OR_CONSTRUCTOR: UseSiteFilter = Impl(
                acceptedTargets = setOf(AnnotationTarget.CONSTRUCTOR)
            )
            val NO_USE_SITE_OR_METHOD: UseSiteFilter = Impl(
                acceptedTargets = setOf(AnnotationTarget.FUNCTION)
            )
            val NO_USE_SITE_OR_FIELD: UseSiteFilter = Impl(
                acceptedSiteTarget = AnnotationUseSiteTarget.FIELD,
                acceptedTargets = setOf(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
            )
            val NO_USE_SITE_OR_METHOD_PARAMETER: UseSiteFilter = Impl(
                acceptedSiteTarget = AnnotationUseSiteTarget.PARAM,
                acceptedTargets = setOf(AnnotationTarget.VALUE_PARAMETER)
            )
            val NO_USE_SITE_OR_GETTER: UseSiteFilter = Impl(
                acceptedSiteTarget = AnnotationUseSiteTarget.GET,
                acceptedTargets = setOf(AnnotationTarget.PROPERTY_GETTER)
            )
            val NO_USE_SITE_OR_SETTER: UseSiteFilter = Impl(
                acceptedSiteTarget = AnnotationUseSiteTarget.SET,
                acceptedTargets = setOf(AnnotationTarget.PROPERTY_SETTER)
            )
            val NO_USE_SITE_OR_SET_PARAM: UseSiteFilter = Impl(
                acceptedSiteTarget = AnnotationUseSiteTarget.SETPARAM,
                acceptedTargets = setOf(AnnotationTarget.PROPERTY_SETTER)
            )
            val NO_USE_SITE_OR_RECEIVER: UseSiteFilter = Impl(
                acceptedSiteTarget = AnnotationUseSiteTarget.RECEIVER,
                acceptedTargets = setOf(AnnotationTarget.VALUE_PARAMETER)
            )
            val FILE: UseSiteFilter = Impl(
                acceptedSiteTarget = AnnotationUseSiteTarget.FILE,
                acceptedTargets = setOf(AnnotationTarget.FILE),
                acceptNoTarget = false
            )

            internal fun KSAnnotation.getDeclaredTargets(
                env: KspProcessingEnv
            ): Set<AnnotationTarget> {
                val annotationDeclaration = this.annotationType.resolve().declaration
                val kotlinTargets = annotationDeclaration.annotations.firstOrNull {
                    it.isSameAnnotationClass(kotlin.annotation.Target::class)
                }?.let { targetAnnotation ->
                    KspAnnotation(env, targetAnnotation)
                        .asAnnotationBox(kotlin.annotation.Target::class.java)
                        .value.allowedTargets
                }?.toSet() ?: emptySet()
                val javaTargets = annotationDeclaration.annotations.firstOrNull {
                    it.isSameAnnotationClass(java.lang.annotation.Target::class)
                }?.let { targetAnnotation ->
                    KspAnnotation(env, targetAnnotation)
                        .asAnnotationBox(java.lang.annotation.Target::class.java)
                        .value.value.toList()
                }?.mapNotNull { it.toAnnotationTarget() }?.toSet() ?: emptySet()
                return kotlinTargets + javaTargets
            }

            private fun ElementType.toAnnotationTarget() = when (this) {
                ElementType.TYPE -> AnnotationTarget.CLASS
                ElementType.FIELD -> AnnotationTarget.FIELD
                ElementType.METHOD -> AnnotationTarget.FUNCTION
                ElementType.PARAMETER -> AnnotationTarget.VALUE_PARAMETER
                ElementType.CONSTRUCTOR -> AnnotationTarget.CONSTRUCTOR
                ElementType.LOCAL_VARIABLE -> AnnotationTarget.LOCAL_VARIABLE
                ElementType.ANNOTATION_TYPE -> AnnotationTarget.ANNOTATION_CLASS
                ElementType.TYPE_PARAMETER -> AnnotationTarget.TYPE_PARAMETER
                ElementType.TYPE_USE -> AnnotationTarget.TYPE
                else -> null
            }
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

        internal fun KSAnnotation.isSameAnnotationClass(
            annotationClass: KClass<out Annotation>
        ): Boolean {
            var declaration = annotationType.resolve().declaration
            while (declaration is KSTypeAlias) {
                declaration = declaration.type.resolve().declaration
            }
            val qualifiedName = declaration.qualifiedName?.asString() ?: return false
            return qualifiedName == annotationClass.qualifiedName
        }
    }
}
