/*
 * Copyright 2025 The Android Open Source Project
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

import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LocationType
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiElement
import kotlin.collections.contains
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.toUElement

class ExperimentalCompanionWithNonExperimentalElementsDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UClass::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitClass(node: UClass) {
                if (!isCompanionDeclaration(node)) {
                    return
                }

                val relevantExperimentalAnnotation =
                    getExperimentalAnnotationFromCompanionDeclaration(node, context)

                if (
                    relevantExperimentalAnnotation != null &&
                        !isContainingElementAnnotatedExperimental(node, context)
                ) {
                    getCompanionChildren(node)?.forEach { child ->
                        val uChild = child.toUElement()
                        if (
                            uChild != null &&
                                !uChild.isDirectlyAnnotatedExperimental(context.evaluator)
                        ) {
                            var incident =
                                Incident(context)
                                    .issue(ISSUE)
                                    .location(context.getLocation(uChild, LocationType.NAME))
                                    .scope(child)
                                    .message(
                                        "Elements in an experimental companion object must be annotated as experimental"
                                    )
                            val fix =
                                fix()
                                    .name("Annotate as experimental")
                                    .annotate(relevantExperimentalAnnotation, context, child)
                                    .autoFix()
                                    .build()
                            incident = incident.fix(fix)

                            context.report(incident)
                        }
                    }
                }
            }
        }

    private fun getExperimentalAnnotationFromCompanionDeclaration(
        companionObject: UClass,
        context: JavaContext,
    ): String? {
        return companionObject.uAnnotations
            .firstOrNull { it.isExperimental(context.evaluator) }
            ?.qualifiedName
    }

    /**
     * Recursively checks if the containing class/element, or any parent elements are annotated as
     * experimental.
     */
    private fun isContainingElementAnnotatedExperimental(
        declaration: UElement,
        context: JavaContext,
    ): Boolean {
        val containingElement = declaration.uastParent
        return containingElement != null &&
            (containingElement.isDirectlyAnnotatedExperimental(context.evaluator) ||
                isContainingElementAnnotatedExperimental(containingElement, context))
    }

    private fun isCompanionDeclaration(declaration: UClass): Boolean {
        return (declaration.sourcePsi as? KtObjectDeclaration)?.isCompanion() == true
    }

    /**
     * This function goes to the source psi to find the children of the companion because it needs
     * to get all the children defined in source. Using `declaration.children` or a combination of
     * `declaration.methods` and `declaration.fields` doesn't correctly fetch all children from
     * source.
     */
    private fun getCompanionChildren(declaration: UClass): Array<PsiElement>? {
        declaration.sourcePsi?.children?.forEach { element ->
            if (element is KtClassBody) {
                return element.children
            }
        }
        return null
    }

    private fun UAnnotation.isExperimental(evaluator: JavaEvaluator): Boolean {
        // The annotation itself is experimental
        if (this.qualifiedName in BanInappropriateExperimentalUsage.APPLICABLE_ANNOTATIONS) {
            return true
        }

        // The annotation is meta-annotated with an experimental annotation
        val cls = this.resolve()
        if (cls == null || !cls.isAnnotationType) return false
        val metaAnnotations = evaluator.getAnnotations(cls, inHierarchy = false)
        return metaAnnotations.find {
            it.qualifiedName in BanInappropriateExperimentalUsage.APPLICABLE_ANNOTATIONS
        } != null
    }

    private fun UElement.isDirectlyAnnotatedExperimental(evaluator: JavaEvaluator): Boolean {
        if (this !is UAnnotated) return false
        // Use [getAllAnnotations] instead of [uAnnotations] because for UFields generated from a
        // source property, [getAllAnnotations] will include all annotations on the property even
        // if they do not technically apply to the backing field, which is the case for experimental
        // annotations.
        return evaluator.getAllAnnotations(this).any { uAnnotation ->
            uAnnotation.isExperimental(evaluator)
        }
    }

    companion object {
        private val IMPLEMENTATION =
            Implementation(
                ExperimentalCompanionWithNonExperimentalElementsDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            )

        val ISSUE =
            Issue.create(
                id = "ExperimentalCompanionElement",
                explanation =
                    """
          This lint check looks for definitions of elements (constants, functions) in experimental companion objects.
          If the containing class isn't experimental and the element itself isn't marked as experimental, this could
          lead to unsafe usages in non-experimental contexts like `ContainingClass.Element` which aren't considered experimental.
          """,
                briefDescription = "Experimental companion element not marked as experimental",
                category = Category.LINT,
                priority = 5,
                severity = Severity.ERROR,
                androidSpecific = true,
                implementation = IMPLEMENTATION,
            )
    }
}
