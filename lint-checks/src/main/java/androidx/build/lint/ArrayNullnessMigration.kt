/*
 * Copyright 2024 The Android Open Source Project
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
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiEllipsisType
import java.util.EnumSet
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter

/**
 * Repositions nullness annotations on arrays to facilitate migrating the nullness annotations to
 * TYPE_USE. See the issue description in the companion object for more detail.
 */
class ArrayNullnessMigration : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes() = listOf(UAnnotation::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return AnnotationChecker(context)
    }

    private inner class AnnotationChecker(val context: JavaContext) : UElementHandler() {
        override fun visitAnnotation(node: UAnnotation) {
            // Nullness annotations are only relevant for Java source.
            if (isKotlin(node.lang)) return

            // Verify this is a nullness annotation.
            val annotationName = node.qualifiedName ?: return
            if (annotationName !in nullnessAnnotations) return

            // Find the type of the annotated element, and only continue if it is an array.
            val annotated = node.uastParent ?: return
            val type =
                when (annotated) {
                    is UParameter -> annotated.type
                    is UMethod -> annotated.returnType
                    is UField -> annotated.type
                    else -> return
                }
            if (type !is PsiArrayType) return

            // Determine the file location for the autofix. This is a bit complicated because it
            // needs to avoid editing the wrong thing, like the doc comment preceding a method, but
            // also is doing some reformatting in the area around the annotation.
            // This is where the annotation itself is located.
            val annotationLocation = context.getLocation(node)
            // This is where the element being annotated is located.
            val annotatedLocation = context.getLocation(annotated as UElement)
            // If the annotation and annotated element aren't on the same line, that probably means
            // the annotation is on its own line, with indentation before it. To also get rid of
            // that indentation, start the range at the start of the annotation's line.
            // If the annotation and annotated element are on the same line, just start at the
            // annotation starting place to avoid including e.g. other parameters.
            val startLocation =
                if (annotatedLocation.start!!.sameLine(annotationLocation.start!!)) {
                    annotationLocation.start!!
                } else {
                    Location.create(
                            context.file,
                            context.getContents()!!.toString(),
                            annotationLocation.start!!.line
                        )
                        .start!!
                }
            val fixLocation =
                Location.create(annotatedLocation.file, startLocation, annotatedLocation.end)

            // Part 1 of the fix: remove the original annotation
            val annotationString = node.asSourceString()
            val removeOriginalAnnotation =
                fix()
                    .replace()
                    .range(fixLocation)
                    // In addition to the annotation, also remove any extra whitespace and trailing
                    // new line. The reformat option unfortunately doesn't do this.
                    .pattern("((    )*$annotationString ?\n?)")
                    .with("")
                    // Only remove one instance of the annotation.
                    .repeatedly(false)
                    .autoFix()
                    .build()

            // Vararg types are also arrays, determine which array marker is present here.
            val arraySuffix =
                if (type is PsiEllipsisType) {
                    "..."
                } else {
                    "[]"
                }
            // Part 2 of the fix: add a new annotation.
            val addNewAnnotation =
                fix()
                    .replace()
                    .range(fixLocation)
                    .text(arraySuffix)
                    .with(" $annotationString $arraySuffix")
                    // Only add one instance of the annotation. This will replace the first instance
                    // of []/..., which is correct. In `String @Nullable [][]` the annotation
                    // applies to the outer `String[][]` type, while in `String[] @Nullable []` it
                    // applies to the inner `String[]` arrays.
                    .repeatedly(false)
                    .autoFix()
                    .build()

            // Combine the two elements of the fix and report.
            val fix =
                fix()
                    .name("Move annotation")
                    .composite()
                    .add(removeOriginalAnnotation)
                    .add(addNewAnnotation)
                    .autoFix()
                    .build()

            val incident =
                Incident(context)
                    .message("Nullness annotation on array will apply to element")
                    .issue(ISSUE)
                    .location(context.getLocation(annotated as UElement))
                    .scope(annotated)
                    .fix(fix)
            context.report(incident)
        }
    }

    companion object {
        val nullnessAnnotations =
            listOf(
                "androidx.annotation.NonNull",
                "androidx.annotation.Nullable",
            )
        val ISSUE =
            Issue.create(
                "ArrayMigration",
                "Migrate arrays to type-use nullness annotations",
                """
                    When nullness annotations do not target TYPE_USE, the following definition means
                    that the type of `arg` is nullable:
                        @Nullable String[] arg
                    However, if the annotation targets TYPE_USE, it now applies to the component
                    type of the array, meaning that `arg`'s type is an array of nullable strings.
                    To retain the original meaning, the definition needs to be changed to this:
                        String @Nullable [] arg
                    This check performs that migration to enable converting nullness annotations to
                    target TYPE_USE.
                """,
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(
                    ArrayNullnessMigration::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}
