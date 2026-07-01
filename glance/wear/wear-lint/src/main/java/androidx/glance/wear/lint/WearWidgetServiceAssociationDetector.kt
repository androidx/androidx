/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.wear.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import org.jetbrains.uast.UAnonymousClass
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement

/**
 * A [Detector] that ensures all concrete subclasses of [GlanceWearWidgetService] are annotated with
 * [AssociateWithGlanceWearWidget] to declare their associated widget.
 */
class WearWidgetServiceAssociationDetector : Detector(), SourceCodeScanner {

    override fun applicableSuperClasses(): List<String> = listOf(GLANCE_WEAR_WIDGET_SERVICE)

    override fun visitClass(context: JavaContext, declaration: UClass) {
        // Skip abstract classes since they cannot be instantiated directly.
        if (context.evaluator.isAbstract(declaration)) {
            return
        }

        // Skip anonymous subclasses of GlanceWearWidgetService. Anonymous classes cannot be
        // declared in the AndroidManifest.xml and therefore cannot run as real Android Services at
        // runtime. Since they do not undergo DI service lifecycle instantiation, they do not
        // suffer from bypassed DI initialization crashes, nor do they pose any ANR risks. Skipping
        // them avoids false positives in tests and local mocks where they are heavily utilized.
        if (declaration is UAnonymousClass) {
            return
        }

        val annotation = declaration.findAnnotation(ASSOCIATE_WITH_GLANCE_WEAR_WIDGET)
        if (annotation != null) {
            return
        }

        val fix = suggestAssociationFix(context, declaration)
        context.report(
            SERVICE_ASSOCIATION_ANNOTATION_ISSUE,
            declaration,
            context.getNameLocation(declaration),
            ISSUE_BRIEF_DESCRIPTION,
            quickfixData = fix,
        )
    }

    companion object {
        private const val GLANCE_WEAR_WIDGET = "androidx.glance.wear.GlanceWearWidget"
        private const val GLANCE_WEAR_WIDGET_SERVICE =
            "androidx.glance.wear.GlanceWearWidgetService"
        private const val ASSOCIATE_WITH_GLANCE_WEAR_WIDGET =
            "androidx.glance.wear.AssociateWithGlanceWearWidget"
        private const val ISSUE_BRIEF_DESCRIPTION =
            "GlanceWearWidgetService subclasses must declare their associated widget using @AssociateWithGlanceWearWidget"
        private val EXPLANATION =
            """
            Glance Wear Widget services must explicitly declare their associated widget.

            This annotation must be used to ensure correct results of all Glance Wear
            APIs and that widgets behave as expected. It also prevents uninitialized
            properties from being accessed, which can lead to an Application Not
            Responding (ANR) error on the device.
            """
                .trimIndent()

        @JvmField
        val SERVICE_ASSOCIATION_ANNOTATION_ISSUE: Issue =
            Issue.create(
                id = "GlanceWearWidgetAnnotationMissing",
                briefDescription = ISSUE_BRIEF_DESCRIPTION,
                explanation = EXPLANATION,
                category = Category.CORRECTNESS,
                // set maximum priority (10) to elevate this warning to the top of all inspection
                // listings and reports.
                priority = 10,
                severity = Severity.FATAL,
                implementation =
                    Implementation(
                        WearWidgetServiceAssociationDetector::class.java,
                        Scope.JAVA_FILE_SCOPE,
                    ),
            )

        /**
         * Helper extension to resolve a [PsiClass] only if it is a subclass of [GlanceWearWidget],
         * excluding the base class itself.
         *
         * This ensures we only resolve actual custom/concrete widget implementations and prevent
         * matching the abstract base class [GlanceWearWidget] itself.
         */
        private fun PsiClass?.takeIfWidgetClass(context: JavaContext): PsiClass? {
            return this?.takeIf { context.evaluator.inheritsFrom(it, GLANCE_WEAR_WIDGET, true) }
        }

        private fun suggestAssociationFix(context: JavaContext, declaration: UClass): LintFix? {
            var targetWidgetClass: PsiClass? = null

            // 1. Try to check getWidget property return type and resolve its PsiClass
            val widgetMethod = declaration.methods.find { it.name == "getWidget" }
            val returnType = widgetMethod?.returnType as? PsiClassType
            targetWidgetClass = returnType?.resolve().takeIfWidgetClass(context)

            // 2. Fall back to evaluating the type of the initializer expression of the "widget"
            // field
            if (targetWidgetClass == null) {
                val widgetField = declaration.fields.find { it.name == "widget" }
                val initExpressionType =
                    widgetField?.uastInitializer?.getExpressionType() as? PsiClassType
                targetWidgetClass = initExpressionType?.resolve().takeIfWidgetClass(context)
            }

            // 3. Check parameterized field arguments (e.g. Lazy<MapsWidget>) and ensure the type
            // extends GlanceWearWidget
            if (targetWidgetClass == null) {
                for (field in declaration.fields) {
                    val fieldType = field.type as? PsiClassType ?: continue
                    for (param in fieldType.parameters) {
                        if (param is PsiClassType) {
                            targetWidgetClass = param.resolve().takeIfWidgetClass(context)
                            if (targetWidgetClass != null) break
                        }
                    }
                    if (targetWidgetClass != null) break
                }
            }

            // 4. If we couldn't resolve a valid concrete widget class, don't provide a broken
            // quick-fix
            val resolvedWidgetClass = targetWidgetClass ?: return null
            val widgetClassName = resolvedWidgetClass.name ?: return null
            val annotationText = "@AssociateWithGlanceWearWidget($widgetClassName::class)"

            return LintFix.create()
                .replace()
                .name("Annotate with @AssociateWithGlanceWearWidget")
                .range(context.getLocation(declaration as UElement))
                .beginning()
                .with("$annotationText\n")
                .imports(ASSOCIATE_WITH_GLANCE_WEAR_WIDGET)
                .build()
        }
    }
}
