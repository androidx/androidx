/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.fragment.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType

/**
 * Lint check for detecting calls to the suspend `repeatOnLifecycle` APIs using `lifecycleOwner`
 * instead of `viewLifecycleOwner` in [androidx.fragment.app.Fragment] classes but not in
 * [androidx.fragment.app.DialogFragment] classes.
 *
 * DialogFragments are allowed to use `lifecycleOwner` since they don't always have a `view`
 * attached to them.
 */
class UnsafeRepeatOnLifecycleDetector : Detector(), SourceCodeScanner {

    companion object {
        val ISSUE = Issue.create(
            id = "UnsafeRepeatOnLifecycleDetector",
            briefDescription = "RepeatOnLifecycle should be used with viewLifecycleOwner in " +
                "Fragments.",
            explanation = """The repeatOnLifecycle APIs should be used with the viewLifecycleOwner \
                in Fragments as opposed to lifecycleOwner.""",
            category = Category.CORRECTNESS,
            severity = Severity.ERROR,
            implementation = Implementation(
                UnsafeRepeatOnLifecycleDetector::class.java, Scope.JAVA_FILE_SCOPE
            ),
            androidSpecific = true
        )
    }

    override fun getApplicableMethodNames() = listOf("repeatOnLifecycle")

    private val lifecycleMethods = setOf(
        "onCreateView", "onViewCreated", "onActivityCreated",
        "onViewStateRestored"
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        // Check that repeatOnLifecycle is called in a Fragment
        if (!hasFragmentAsAncestorType(node.getParentOfType())) return

        // Check that repeatOnLifecycle is called in the proper Lifecycle function
        if (!isCalledInViewLifecycleFunction(node.getParentOfType())) return

        // Look at the entire launch scope
        var launchScope = node.getParentOfType<UCallExpression>()?.receiver
        while (
            launchScope != null && !containsViewLifecycleOwnerCall(launchScope.sourcePsi?.text)
        ) {
            launchScope = launchScope.getParentOfType()
        }
        // Report issue if there is no viewLifecycleOwner in the launch scope
        if (!containsViewLifecycleOwnerCall(launchScope?.sourcePsi?.text)) {
            context.report(
                ISSUE,
                context.getLocation(node),
                "The repeatOnLifecycle API should be used with viewLifecycleOwner"
            )
        }
    }

    private fun containsViewLifecycleOwnerCall(sourceText: String?): Boolean {
        if (sourceText == null) return false
        return sourceText.contains(VIEW_LIFECYCLE_KOTLIN_PROP, ignoreCase = true) ||
            sourceText.contains(VIEW_LIFECYCLE_FUN, ignoreCase = true)
    }

    private fun isCalledInViewLifecycleFunction(uMethod: UMethod?): Boolean {
        if (uMethod == null) return false
        return lifecycleMethods.contains(uMethod.name)
    }

    /**
     * Check if `uClass` has FRAGMENT as a super type but not DIALOG_FRAGMENT
     */
    @Suppress("UNCHECKED_CAST")
    private fun hasFragmentAsAncestorType(uClass: UClass?): Boolean {
        if (uClass == null) return false
        return hasFragmentAsSuperType(uClass.superTypes as Array<PsiType>)
    }

    private fun hasFragmentAsSuperType(superTypes: Array<PsiType>): Boolean {
        for (superType in superTypes) {
            if (superType.canonicalText == DIALOG_FRAGMENT_CLASS) return false
            if (superType.canonicalText == FRAGMENT_CLASS) return true
            if (hasFragmentAsSuperType(superType.superTypes)) return true
        }
        return false
    }
}

private const val VIEW_LIFECYCLE_KOTLIN_PROP = "viewLifecycleOwner"
private const val VIEW_LIFECYCLE_FUN = "getViewLifecycleOwner"
private const val FRAGMENT_CLASS = "androidx.fragment.app.Fragment"
private const val DIALOG_FRAGMENT_CLASS = "androidx.fragment.app.DialogFragment"
