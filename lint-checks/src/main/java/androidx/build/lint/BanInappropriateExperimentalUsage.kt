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

package androidx.build.lint

import com.android.tools.lint.detector.api.AnnotationUsageType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getContainingUClass

class BanInappropriateExperimentalUsage : Detector(), SourceCodeScanner {
    override fun applicableAnnotations(): List<String>? = listOf(
        JAVA_EXPERIMENTAL_ANNOTATION,
        KOTLIN_OPT_IN_ANNOTATION,
        KOTLIN_EXPERIMENTAL_ANNOTATION
    )

    override fun visitAnnotationUsage(
        context: JavaContext,
        usage: UElement,
        type: AnnotationUsageType,
        annotation: UAnnotation,
        qualifiedName: String,
        method: PsiMethod?,
        referenced: PsiElement?,
        annotations: List<UAnnotation>,
        allMemberAnnotations: List<UAnnotation>,
        allClassAnnotations: List<UAnnotation>,
        allPackageAnnotations: List<UAnnotation>
    ) {
        when (qualifiedName) {
            JAVA_EXPERIMENTAL_ANNOTATION,
            JAVA_OPT_IN_ANNOTATION,
            KOTLIN_EXPERIMENTAL_ANNOTATION,
            KOTLIN_OPT_IN_ANNOTATION -> {
                verifyExperimentalOrOptInUsageIsWithinSameGroup(
                    context, usage, annotation
                )
            }
        }
    }

    fun verifyExperimentalOrOptInUsageIsWithinSameGroup(
        context: JavaContext,
        usage: UElement,
        annotation: UAnnotation
    ) {
        val declaringGroup = getApproximateAnnotationMavenGroup(annotation)
        val usingGroup = getApproximateUsageSiteMavenGroup(usage)
        // Don't flag if group is null for some reason (for now at least)
        // Also exclude sample for now, since it doesn't work well with our workaround (includes
        // class)
        if (declaringGroup != null && usingGroup != null && declaringGroup != usingGroup &&
            usingGroup != "sample"
        ) {
            context.report(
                BanInappropriateExperimentalUsage.ISSUE, usage, context.getNameLocation(usage),
                "`Experimental`/`OptIn` APIs should only be used from within the same library " +
                    "or libraries within the same requireSameVersion group"
            )
        }
    }

    fun getApproximateAnnotationMavenGroup(annotation: UAnnotation): String? {
        if (annotation.getContainingUClass() == null || annotation.getContainingUClass()!!
            .qualifiedName == null
        ) {
            return null
        }
        return annotation.getContainingUClass()!!.qualifiedName!!.split(".").subList(0, 2)
            .joinToString(".")
    }

    fun getApproximateUsageSiteMavenGroup(usage: UElement): String? {
        if (usage.getContainingUClass() == null || usage.getContainingUClass()!!
            .qualifiedName == null
        ) {
            return null
        }
        return usage.getContainingUClass()!!.qualifiedName!!.split(".").subList(0, 2)
            .joinToString(".")
    }

    companion object {

        private const val KOTLIN_EXPERIMENTAL_ANNOTATION = "kotlin.Experimental"

        private const val JAVA_EXPERIMENTAL_ANNOTATION =
            "androidx.annotation.experimental.Experimental"

        private const val KOTLIN_OPT_IN_ANNOTATION =
            "kotlin.OptIn"

        private const val JAVA_OPT_IN_ANNOTATION =
            "androidx.annotation.OptIn"

        val ISSUE = Issue.create(
            "IllegalExperimentalApiUsage",
            "Using experimental api from separately versioned library",
            "APIs annotated with `@RequiresOptIn` or `@Experimental` are considered alpha." +
                "A caller from another library may not use them unless that the two libraries " +
                "are part of the same maven group and that group specifies requireSameVersion",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(BanInappropriateExperimentalUsage::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
