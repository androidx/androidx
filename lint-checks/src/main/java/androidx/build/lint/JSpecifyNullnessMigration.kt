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
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiPrimitiveType
import java.util.EnumSet
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter

/**
 * Repositions nullness annotations to facilitate migrating the nullness annotations to JSpecify
 * TYPE_USE annotations. See the issue description in the companion object for more detail.
 */
class JSpecifyNullnessMigration : Detector(), Detector.UastScanner {
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
            if (annotationName !in nullnessAnnotations.keys) return
            val replacementAnnotationName = nullnessAnnotations[annotationName]!!

            val fix = createFix(node, replacementAnnotationName)
            val incident =
                Incident(context)
                    .message("Switch nullness annotation to JSpecify")
                    .issue(ISSUE)
                    .location(context.getLocation(node as UElement))
                    .scope(node)
                    .fix(fix)
            context.report(incident)
        }

        fun createFix(node: UAnnotation, replacementAnnotationName: String): LintFix? {
            // Find the type of the annotated element.
            val annotated = node.uastParent ?: return null
            val type =
                when (annotated) {
                    is UParameter -> annotated.type
                    is UMethod -> annotated.returnType
                    is UField -> annotated.type
                    is ULocalVariable -> annotated.type
                    else -> return null
                } ?: return null

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
            val annotatedStart = annotatedLocation.start ?: return null
            val annotationStart = annotationLocation.start ?: return null
            val startLocation =
                if (annotatedStart.sameLine(annotationStart)) {
                    annotationStart
                } else {
                    Location.create(
                            context.file,
                            context.getContents()!!.toString(),
                            annotationStart.line
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

            // The jspecify annotations can't be applied to primitive types (since primitives are
            // non-null by definition) or local variables, so just remove the annotation in those
            // cases. For all other cases, also add a new annotation to the correct position.
            return if (type is PsiPrimitiveType || annotated is ULocalVariable) {
                removeOriginalAnnotation
            } else {
                // Create a regex pattern for where to insert the annotation. The replacement lint
                // removes the first capture group (section in parentheses) of the supplied regex.
                // Since this fix is really just to insert an annotation, use an empty capture group
                // so nothing is removed.
                val (prefix, textToReplace) =
                    when {
                        // For a vararg type where the component type is an array, the annotation
                        // goes before the array instead of the vararg ("String @NonNull []..."),
                        // so only match the "..." when the component isn't an array.
                        type is PsiEllipsisType && type.componentType !is PsiArrayType ->
                            Pair(" ", "()\\.\\.\\.")
                        type is PsiArrayType -> Pair(" ", "()\\[\\]")
                        // Make sure to match the right usage of the class name: find the name
                        // preceded by a space or dot, and followed by a space, open angle bracket,
                        // or newline character.
                        type is PsiClassType -> Pair("", "[ .]()${type.className}[ <\\n\\r]")
                        else -> Pair("", "()${type.presentableText}")
                    }
                val replacement = "$prefix@$replacementAnnotationName "

                // Part 2 of the fix: add a new annotation.
                val addNewAnnotation =
                    fix()
                        .replace()
                        .range(fixLocation)
                        .pattern(textToReplace)
                        .with(replacement)
                        // Only add one instance of the annotation. For nested array types, this
                        // will replace the first instance of []/..., which is correct. In
                        // `String @Nullable [][]` the annotation applies to the outer `String[][]`
                        // type, while in `String[] @Nullable []` it applies to the inner `String[]`
                        // arrays.
                        .repeatedly(false)
                        .shortenNames()
                        .autoFix()
                        .build()

                // Combine the two elements of the fix.
                return fix()
                    .name("Move annotation")
                    .composite()
                    .add(removeOriginalAnnotation)
                    .add(addNewAnnotation)
                    .autoFix()
                    .build()
            }
        }
    }

    companion object {
        val nullnessAnnotations =
            mapOf(
                "androidx.annotation.NonNull" to "NonNull",
                "androidx.annotation.Nullable" to "Nullable",
            )
        val ISSUE =
            Issue.create(
                "JSpecifyNullness",
                "Migrate nullness annotations to type-use position",
                """
                    Switches from AndroidX nullness annotations to JSpecify, which are type-use.
                    Type-use annotations have different syntactic positions than non-type-use
                    annotations in some cases.

                    For instance, when nullness annotations do not target TYPE_USE, the following
                    definition means that the type of `arg` is nullable:
                        @Nullable String[] arg
                    However, if the annotation targets TYPE_USE, it now applies to the component
                    type of the array, meaning that `arg`'s type is an array of nullable strings.
                    To retain the original meaning, the definition needs to be changed to this:
                        String @Nullable [] arg

                    Type-use nullness annotations must go before the simple class name of a
                    qualified type. For instance, `java.lang.@Nullable String` is required instead
                    of `@Nullable java.lang.String`.
                """,
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(
                    JSpecifyNullnessMigration::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}
