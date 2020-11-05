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

import androidx.room.compiler.processing.XAnnotated
import androidx.room.compiler.processing.XAnnotationBox
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import kotlin.reflect.KClass

internal sealed class KspAnnotated(
    val env: KspProcessingEnv
) : XAnnotated {
    abstract fun annotations(): Sequence<KSAnnotation>

    override fun <T : Annotation> toAnnotationBox(annotation: KClass<T>): XAnnotationBox<T>? {
        return annotations().firstOrNull {
            val qName = it.annotationType.resolve().declaration.qualifiedName?.asString()
            qName == annotation.qualifiedName
        }?.let {
            KspAnnotationBox(
                env = env,
                annotationClass = annotation.java,
                annotation = it
            )
        }
    }

    override fun hasAnnotationWithPackage(pkg: String): Boolean {
        return annotations().any {
            it.annotationType.resolve().declaration.qualifiedName?.getQualifier() == pkg
        }
    }

    override fun hasAnnotation(annotation: KClass<out Annotation>): Boolean {
        return annotations().any {
            val qName = it.annotationType.resolve().declaration.qualifiedName?.asString()
            qName == annotation.qualifiedName
        }
    }

    operator fun plus(other: KspAnnotated): XAnnotated = Combined(env, this, other)

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

    private class Combined(
        env: KspProcessingEnv,
        private val first: KspAnnotated,
        private val second: KspAnnotated
    ) : KspAnnotated(env) {
        override fun annotations(): Sequence<KSAnnotation> {
            return first.annotations() + second.annotations()
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
            val acceptNull: Boolean,
            val acceptedTarget: AnnotationUseSiteTarget
        ) : UseSiteFilter {
            override fun accept(annotation: KSAnnotation): Boolean {
                val target = annotation.useSiteTarget
                return if (target == null) {
                    acceptNull
                } else {
                    acceptedTarget == target
                }
            }
        }

        companion object {
            val FIELD: UseSiteFilter = Impl(true, AnnotationUseSiteTarget.FIELD)
            val PROPERTY_GETTER: UseSiteFilter = Impl(false, AnnotationUseSiteTarget.GET)
            val PROPERTY_SETTER: UseSiteFilter = Impl(false, AnnotationUseSiteTarget.SET)
            val PROPERTY_SETTER_PARAMETER: UseSiteFilter =
                Impl(false, AnnotationUseSiteTarget.SETPARAM)
            val METHOD_PARAMETER: UseSiteFilter = Impl(true, AnnotationUseSiteTarget.PARAM)
            val NO_USE_SITE = object : UseSiteFilter {
                override fun accept(annotation: KSAnnotation): Boolean {
                    return annotation.useSiteTarget == null
                }
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
    }
}
