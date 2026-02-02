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

package androidx.room3.compiler.processing.ksp

import androidx.room3.compiler.processing.InternalXAnnotated
import androidx.room3.compiler.processing.XAnnotation
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSTypeAlias
import java.lang.annotation.ElementType
import kotlin.reflect.KClass

internal sealed class KspAnnotated(val env: KspProcessingEnv) : InternalXAnnotated {
    protected abstract val ksAnnotations: Sequence<KSAnnotation>

    private val annotations: List<XAnnotation> by lazy {
        ksAnnotations.map { KspAnnotation(env, it) }.toList()
    }

    override fun getAllAnnotations() = annotations

    override fun <T : Annotation> getAnnotations(
        annotation: KClass<T>,
        containerAnnotation: KClass<out Annotation>?,
    ): List<XAnnotation> {
        // we'll try both because it can be the container or the annotation itself.
        // try container first
        if (containerAnnotation != null) {
            // if container also repeats, this won't work but we don't have that use case
            findAnnotations(containerAnnotation).firstOrNull()?.let {
                return KspAnnotation(env = env, ksAnnotation = it).getAsAnnotationList("value")
            }
        }
        // didn't find anything with the container, try the annotation class
        return findAnnotations(annotation)
            .map { KspAnnotation(env = env, ksAnnotation = it) }
            .toList()
    }

    private fun <T : Annotation> findAnnotations(annotation: KClass<T>): Sequence<KSAnnotation> {
        return ksAnnotations.filter { it.isSameAnnotationClass(annotation) }
    }

    override fun hasAnnotationWithPackage(pkg: String): Boolean {
        return ksAnnotations.any {
            it.annotationType.resolve().declaration.packageName.asString() == pkg
        }
    }

    override fun hasAnnotation(
        annotation: KClass<out Annotation>,
        containerAnnotation: KClass<out Annotation>?,
    ): Boolean {
        return ksAnnotations.any {
            it.isSameAnnotationClass(annotation) ||
                (containerAnnotation != null && it.isSameAnnotationClass(containerAnnotation))
        }
    }

    private class KSAnnotatedDelegate(
        env: KspProcessingEnv,
        delegate: KSAnnotated,
        useSiteFilter: UseSiteFilter,
    ) : KspAnnotated(env) {
        override val ksAnnotations by lazy {
            delegate.annotations.filter { useSiteFilter.accept(env, it) }
        }
    }

    private class NotAnnotated(env: KspProcessingEnv) : KspAnnotated(env) {
        override val ksAnnotations = emptySequence<KSAnnotation>()
    }

    /**
     * Annotation use site filter
     *
     * https://kotlinlang.org/docs/annotations.html#annotation-use-site-targets
     */
    enum class UseSiteFilter(
        val acceptedSiteTarget: AnnotationUseSiteTarget? = null,
        val acceptedTargets: Set<AnnotationTarget>,
        private val acceptNoTarget: Boolean = true,
    ) {
        NO_USE_SITE(acceptedTargets = emptySet()),
        NO_USE_SITE_OR_CONSTRUCTOR(acceptedTargets = setOf(AnnotationTarget.CONSTRUCTOR)),
        NO_USE_SITE_OR_METHOD(acceptedTargets = setOf(AnnotationTarget.FUNCTION)),
        NO_USE_SITE_OR_FIELD(
            acceptedSiteTarget = AnnotationUseSiteTarget.FIELD,
            acceptedTargets = setOf(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY),
        ),
        NO_USE_SITE_OR_METHOD_PARAMETER(
            acceptedSiteTarget = AnnotationUseSiteTarget.PARAM,
            acceptedTargets = setOf(AnnotationTarget.VALUE_PARAMETER),
        ),
        NO_USE_SITE_OR_GETTER(
            acceptedSiteTarget = AnnotationUseSiteTarget.GET,
            acceptedTargets = setOf(AnnotationTarget.PROPERTY_GETTER),
        ),
        NO_USE_SITE_OR_SETTER(
            acceptedSiteTarget = AnnotationUseSiteTarget.SET,
            acceptedTargets = setOf(AnnotationTarget.PROPERTY_SETTER),
        ),
        NO_USE_SITE_OR_SET_PARAM(
            acceptedSiteTarget = AnnotationUseSiteTarget.SETPARAM,
            acceptedTargets = setOf(AnnotationTarget.VALUE_PARAMETER),
        ),
        NO_USE_SITE_OR_RECEIVER(
            acceptedSiteTarget = AnnotationUseSiteTarget.RECEIVER,
            acceptedTargets = setOf(AnnotationTarget.VALUE_PARAMETER),
        ),
        FILE(
            acceptedSiteTarget = AnnotationUseSiteTarget.FILE,
            acceptedTargets = setOf(AnnotationTarget.FILE),
            acceptNoTarget = false,
        );

        fun accept(env: KspProcessingEnv, annotation: KSAnnotation): Boolean {
            if (annotation.useSiteTarget != null) {
                return acceptedSiteTarget == annotation.useSiteTarget
            }
            return this == NO_USE_SITE ||
                annotation.getDeclaredTargets(env).let { targets ->
                    if (targets.isNotEmpty()) {
                        targets.any { acceptedTargets.contains(it) }
                    } else {
                        acceptNoTarget
                    }
                }
        }

        companion object {
            internal fun KSAnnotation.getDeclaredTargets(
                env: KspProcessingEnv
            ): Set<AnnotationTarget> {
                val annotationDeclaration = this.annotationType.resolve().declaration
                val kotlinTargets =
                    annotationDeclaration.annotations
                        .firstOrNull { it.isSameAnnotationClass(kotlin.annotation.Target::class) }
                        ?.let { targetAnnotation ->
                            KspAnnotation(env, targetAnnotation)["allowedTargets"]?.asEnumList()
                                ?.map { AnnotationTarget.valueOf(it.name) }
                        }
                        ?.toSet() ?: emptySet()
                val javaTargets =
                    annotationDeclaration.annotations
                        .firstOrNull {
                            it.isSameAnnotationClass(java.lang.annotation.Target::class)
                        }
                        ?.let { targetAnnotation ->
                            KspAnnotation(env, targetAnnotation)["value"]?.asEnumList()?.map {
                                ElementType.valueOf(it.name)
                            }
                        }
                        ?.flatMap { it.toAnnotationTargets() }
                        ?.toSet() ?: emptySet()
                return kotlinTargets + javaTargets
            }

            // As of Java 22 and Kotlin 1.9, Java annotations can't be used on
            // AnnotationTarget.EXPRESSION, AnnotationTarget.FILE, and AnnotationTarget.TYPEALIAS,
            // but can be used on AnnotationTarget.PROPERTY with @property: if
            // it doesn't have Target defined. There are no mappings from
            // ElementType.PACKAGE, ElementType.MODULE or ElementType.RECORD_COMPONENT
            // to AnnotationTarget.
            private fun ElementType.toAnnotationTargets() =
                when (this) {
                    ElementType.TYPE ->
                        listOf(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
                    ElementType.FIELD -> listOf(AnnotationTarget.FIELD)
                    ElementType.METHOD ->
                        listOf(
                            AnnotationTarget.FUNCTION,
                            AnnotationTarget.PROPERTY_GETTER,
                            AnnotationTarget.PROPERTY_SETTER,
                        )
                    ElementType.PARAMETER -> listOf(AnnotationTarget.VALUE_PARAMETER)
                    ElementType.CONSTRUCTOR -> listOf(AnnotationTarget.CONSTRUCTOR)
                    ElementType.LOCAL_VARIABLE -> listOf(AnnotationTarget.LOCAL_VARIABLE)
                    ElementType.ANNOTATION_TYPE -> listOf(AnnotationTarget.ANNOTATION_CLASS)
                    ElementType.TYPE_PARAMETER -> listOf(AnnotationTarget.TYPE_PARAMETER)
                    ElementType.TYPE_USE -> listOf(AnnotationTarget.TYPE)
                    else -> emptyList()
                }
        }
    }

    companion object {
        fun create(
            env: KspProcessingEnv,
            delegate: KSAnnotated?,
            filter: UseSiteFilter,
        ): KspAnnotated {
            return delegate?.let { KSAnnotatedDelegate(env, it, filter) } ?: NotAnnotated(env)
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
