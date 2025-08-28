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

@file:Suppress("UnstableApiUsage")

package androidx.compose.runtime.lint

import androidx.compose.lint.Names
import androidx.compose.lint.inheritsFrom
import androidx.compose.lint.isInPackageName
import androidx.compose.lint.isVoidOrUnit
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.util.InheritanceUtil
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression

class RetainDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf(Names.Runtime.Retain.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(Names.Runtime.PackageName)) return
        val callExpressionType = node.getExpressionType()

        if (callExpressionType.isVoidOrUnit && RememberDetector.isReallyUnit(node, method)) {
            context.report(
                issue = RetainUnitType,
                scope = node,
                location = context.getNameLocation(node),
                message = "`retain` calls must not return `Unit`.",
                quickfixData = null,
            )
        }

        if (callExpressionType?.isNotRetainable() == true) {
            context.report(
                issue = RetainRememberObserver,
                scope = node,
                location = context.getNameLocation(node),
                message =
                    "Declared retained type `${callExpressionType.canonicalText}` implements " +
                        "`RememberObserver` but not `RetainObserver`.",
                quickfixData = createReplaceWithRememberQuickFix(),
            )
        }

        if (callExpressionType?.isKnownContextLeakOrWrapper() == true) {
            context.report(
                issue = RetainContextLeak,
                scope = node,
                location = context.getNameLocation(node),
                message =
                    "Retaining ${callExpressionType.canonicalText} will leak a Context " +
                        "reference.",
                quickfixData = createReplaceWithRememberQuickFix(),
            )
        }

        val doNotRetainAnnotation = callExpressionType?.findDoNotRetainAnnotation()
        if (doNotRetainAnnotation != null) {
            val doNotRetainReason =
                doNotRetainAnnotation.parameterList.attributes
                    .firstOrNull { it.name == "explanation" }
                    ?.literalValue
                    .orEmpty()

            context.report(
                issue = RetainMarkedType,
                scope = node,
                location = context.getNameLocation(node),
                message =
                    "${callExpressionType.canonicalText} is annotated as `@DoNotRetain`" +
                        if (doNotRetainReason.isNotEmpty()) ": $doNotRetainReason" else "",
                quickfixData = createReplaceWithRememberQuickFix(),
            )
        }
    }

    private fun createReplaceWithRememberQuickFix(): LintFix {
        return LintFix.create()
            .replace()
            .name("Replace with `remember`")
            .text(Names.Runtime.Retain.shortName)
            .with(Names.Runtime.Remember.shortName)
            .imports(Names.Runtime.Remember.javaFqn)
            .autoFix()
            .build()
    }

    private fun PsiType.isNotRetainable(): Boolean {
        val isRememberObserver = inheritsFrom(Names.Runtime.RememberObserver)
        val isRetainObserver = inheritsFrom(Names.Runtime.RetainObserver)

        return isRememberObserver && !isRetainObserver
    }

    private fun PsiType.isKnownContextLeakOrWrapper(): Boolean {
        return if (isKnownContextLeak()) {
            true
        } else if (isKnownGenericContainerType() && this is PsiClassType) {
            parameters.any { parameter ->
                when (parameter) {
                    is PsiWildcardType -> parameter.bound?.isKnownContextLeakOrWrapper() == true
                    else -> parameter.isKnownContextLeakOrWrapper()
                }
            }
        } else {
            false
        }
    }

    private fun PsiType.isKnownContextLeak(): Boolean {
        return knownContextLeakSupertypeFqNames.any { fqName ->
            InheritanceUtil.isInheritor(this, fqName)
        }
    }

    /**
     * Returns information from the `@DoNotRetain` if it is present. The possible values of this
     * return type are:
     * - Null, if the annotation is absent
     * - An empty string, if the annotation is present but no rationale was specified
     * - A non-empty string with a user-provided rationale of why the receiver type or one of its
     *   supertypes is not able to be retained.
     */
    private fun PsiType.findDoNotRetainAnnotation(): PsiAnnotation? {
        return getDoNotRetainAnnotation()
            ?: superTypes.firstNotNullOfOrNull { it.getDoNotRetainAnnotation() }
            ?: getDoNotRetainAnnotationFromKnownContainerType()
    }

    private fun PsiType.getDoNotRetainAnnotation(): PsiAnnotation? {
        return (this as? PsiClassType)
            ?.resolve()
            ?.getAnnotation(Names.Runtime.Annotation.DoNotRetain.javaFqn)
    }

    private fun PsiType.getDoNotRetainAnnotationFromKnownContainerType(): PsiAnnotation? {
        return if (isKnownGenericContainerType() && this is PsiClassType) {
            parameters.firstNotNullOfOrNull { parameter ->
                when (parameter) {
                    is PsiWildcardType -> parameter.bound?.findDoNotRetainAnnotation()
                    else -> parameter.findDoNotRetainAnnotation()
                }
            }
        } else {
            null
        }
    }

    private fun PsiType.isKnownGenericContainerType(): Boolean {
        return knownGenericContainerTypeFqNames.any { fqName ->
            InheritanceUtil.isInheritor(this, fqName)
        }
    }

    companion object {
        private val knownContextLeakSupertypeFqNames =
            setOf(
                "android.content.Context",
                "android.view.View",
                "android.view.Window",
                "android.content.res.Resources",
                "androidx.fragment.app.Fragment",
                "android.app.Fragment",
                "android.app.Dialog",
            )

        private val knownGenericContainerTypeFqNames =
            setOf(
                // Kotlin
                "kotlin.collections.Collection",
                "kotlin.collections.Map",
                "kotlin.Pair",
                "kotlin.Triple",
                "kotlin.Result",
                // Java
                "java.util.Collection",
                "java.util.Map",
                // Kotlinx
                "kotlinx.coroutines.flow.Flow",
                "kotlinx.collections.immutable.ImmutableCollection",
                "kotlinx.collections.immutable.ImmutableMap",
                // Android
                "android.util.SparseArray",
                // Androidx
                "androidx.collection.ArraySet",
                "androidx.collection.CircularArray",
                "androidx.collection.FloatObjectMap",
                "androidx.collection.ObjectFloatMap",
                "androidx.collection.IntObjectMap",
                "androidx.collection.ObjectIntMap",
                "androidx.collection.LongObjectMap",
                "androidx.collection.ObjectLongMap",
                "androidx.collection.ScatterMap",
                "androidx.collection.OrderedScatterSet",
                "androidx.collection.SparseArrayCompat",
                "androidx.collection.LruCache",
                "androidx.core.util.Pair",
                "androidx.lifecycle.LiveData",
                // Guava
                "com.google.common.base.Optional",
                "com.google.common.collect.Multimap",
                "com.google.common.collect.Table",
                // Arrow
                "arrow.core.Option",
                "arrow.core.Either",
            )

        val RetainUnitType =
            Issue.create(
                id = "RetainUnitType",
                briefDescription = "`retain` calls must not return `Unit`",
                explanation =
                    "A call to `retain` that returns `Unit` is always an error. This typically " +
                        "happens when using `retain` to perform an action or mutate variables " +
                        "on an object. Instead, use `SideEffect` (or `RetainedEffect`) to make " +
                        "deferred changes once the composition succeeds, or mutate " +
                        "`MutableState` backed variables directly, as these will handle " +
                        "composition failure for you.",
                category = Category.CORRECTNESS,
                priority = 3,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        RetainDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )

        val RetainRememberObserver =
            Issue.create(
                id = "RetainRememberObserver",
                briefDescription =
                    "Values returned by `retain { ... }` must not implement RememberObserver " +
                        "unless they also implement RetainObserver.",
                explanation =
                    "Objects that implement RememberObserver and not RetainObserver are unaware " +
                        "of the retainment lifecycle. They cannot be correctly retained because " +
                        "there is no valid way to dispatch the RememberObserver callbacks and " +
                        "are therefore prohibited as return values to the `calculation` lambda " +
                        "of `retain`. Attempting to retain a value that implements " +
                        "`RememberObserver` without also implementing `RetainObserver` will " +
                        "throw an exception. Either remember the value instead of retaining it, " +
                        "or implement RetainObserver on the object." +
                        "\n\nNote that this inspection checks the statically declared return " +
                        "type to ensure it does not declare itself as implementing " +
                        "`RememberObserver` without also implementing `RetainObserver`. The " +
                        "actual runtime types are not checked, which may lead to false negatives " +
                        "or false positives if the `calculation` lambda returns a different type " +
                        "than the call to `retain`.",
                category = Category.CORRECTNESS,
                priority = 3,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        RetainDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )

        val RetainContextLeak =
            Issue.create(
                id = "RetainLeaksContext",
                briefDescription =
                    "Using `retain { ... }` to store a value that extends from " +
                        "or references `Context` will cause a memory leak.",
                explanation =
                    "The lifespan of a retained object can extend beyond the lifecycle " +
                        "of the host activity. Retaining a `Context` (or another type that holds a " +
                        "strong reference to a `Context`) will leak the Context and prevent its " +
                        "memory from being properly reclaimed." +
                        "\n\nIf caching is necessary, consider remembering offending values instead " +
                        "of retaining them.",
                category = Category.CORRECTNESS,
                priority = 3,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        RetainDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )

        val RetainMarkedType =
            Issue.create(
                id = "RetainingDoNotRetainType",
                briefDescription =
                    "Types annotated with `@DoNotRetain` should not be returned as the result " +
                        "of `retain`, either directly or transitively.",
                explanation =
                    "Objects annotated with `@DoNotRetain` are not intended to be used with " +
                        "retain. Types marked with this annotation are not designed for " +
                        "retention, generally because they have an independent lifecycle that " +
                        "exceeds the retention lifespan and will leak resources. The type's" +
                        "documentation may have more information about why it does not support " +
                        "retention." +
                        "\n\nThis inspection checks that marked types are never directly " +
                        "returned by the `calculation` lambda of `retain`. Types marked with " +
                        "`@DoNotRetain` ",
                category = Category.CORRECTNESS,
                priority = 3,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        RetainDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )
    }
}
