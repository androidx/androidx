/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

class ExperimentalPropertyAnnotationDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            /**
             * Checks for property accessors which are not annotated as experimental even though
             * their property is annotated as experimental.
             */
            override fun visitMethod(node: UMethod) {
                // Check if this is a property accessor method. The origin could either be a regular
                // property or a constructor property parameter.
                val source = node.sourcePsi
                val property = (source as? KtPropertyAccessor)?.property ?: source
                when (property) {
                    // Check if this check shouldn't apply to the property/parameter.
                    is KtProperty -> if (!appliesToProperty(property)) return
                    is KtParameter -> if (!appliesToParameter(property)) return
                    else -> return
                }

                // Check if this is a getter: getters typically have no parameters unless the
                // property is an extension property with a receiver type, then getters have one
                // parameter (the receiver type).
                if (node.uastParameters.isNotEmpty()) {
                    // If there are parameters and no receiver, this is a setter
                    (property as? KtProperty)?.receiverTypeReference ?: return
                    // There's more than one parameter, this is a setter
                    if (node.uastParameters.size > 1) return
                }

                // If the accessor is annotated as experimental (which technically isn't allowed for
                // getters, but has been done), don't flag the property).
                if (node.uAnnotations.any { it.isExperimental() }) return

                // Don't apply the lint to private properties
                // parent is either a KtProperty or KtParameter, both are KtModifierListOwner
                if ((property as KtModifierListOwner).isPrivate()) return

                // Don't apply the lint to properties in private classes
                if (property.parent.getParentOfType<KtClass>(true)?.isPrivate() == true) return

                // Find annotations which were applied to the property.
                val propertyAnnotations =
                    (property as KtAnnotated)
                        .annotationEntries
                        .filter {
                            val useSiteTarget = it.useSiteTarget?.getAnnotationUseSiteTarget()
                            // A null target applies to the property.
                            useSiteTarget == AnnotationUseSiteTarget.PROPERTY ||
                                useSiteTarget == null
                        }
                        .map { it.toUElement() }
                        .filterIsInstance<UAnnotation>()

                for (propertyAnnotation in propertyAnnotations) {
                    if (propertyAnnotation.isExperimental()) {
                        val message = "Experimental property will not appear experimental in Java"
                        val location = context.getLocation(propertyAnnotation)
                        val incident = Incident(ISSUE, propertyAnnotation, location, message)
                        context.report(incident)
                        return
                    }
                }
            }

            private fun UAnnotation.isExperimental(): Boolean {
                // If this annotation is not annotated with an experimental annotation, return
                val resolved = resolve()
                return BanInappropriateExperimentalUsage.APPLICABLE_ANNOTATIONS.any {
                    context.evaluator.getAnnotation(resolved, it) != null
                }
            }

            /**
             * Whether the lint check should apply to [property]. The check only applies to top
             * level or class properties, and does not apply to const, @JvmField, or delegated
             * properties.
             */
            fun appliesToProperty(property: KtProperty): Boolean {
                // Only applies to properties defined at the top level or in classes
                val propertyParent = property.parent
                if ((propertyParent !is KtClassBody && propertyParent !is KtFile)) return false

                // Don't apply lint to const properties, because they are static fields in java
                if (property.modifierList?.node?.findChildByType(KtTokens.CONST_KEYWORD) != null)
                    return false
                // Don't apply lint to @JvmField properties, because they are fields in java
                if (property.annotationEntries.any { it.shortName.toString() == "JvmField" })
                    return false

                // Don't apply lint to delegated properties
                if (property.delegate != null) return false

                return true
            }

            /**
             * Whether the lint check should apply to [parameter]. The check only applies to
             * constructor property parameters, and should not apply if the constructor is private.
             */
            fun appliesToParameter(parameter: KtParameter): Boolean {
                if (!parameter.isPropertyParameter()) return false

                // Don't apply to parameters of private constructors
                if (parameter.getParentOfType<KtConstructor<*>>(true)?.isPrivate() == true)
                    return false

                return true
            }
        }

    companion object {
        val ISSUE =
            Issue.create(
                "ExperimentalPropertyAnnotation",
                "Experimental properties are not allowed in projects targeting Java clients.",
                "Annotations on Kotlin properties will only apply to the private " +
                    "backing field itself, and not to the getter or setter " +
                    "(see https://kotlinlang.org/docs/annotations.html#annotation-use-site-targets).\n" +
                    "This means that usage of the property will not appear experimental for Java " +
                    "clients. The Kotlin compiler does not allow applying experimental " +
                    "annotations to getters, so API properties should not be experimental if " +
                    "they could be used by Java clients.\n" +
                    "If this project is not meant to target Java clients, its type should be " +
                    "updated in the build file.",
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(
                    ExperimentalPropertyAnnotationDetector::class.java,
                    Scope.JAVA_FILE_SCOPE,
                ),
            )
    }
}
