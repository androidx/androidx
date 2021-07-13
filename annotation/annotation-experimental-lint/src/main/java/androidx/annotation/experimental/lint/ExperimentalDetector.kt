/*
 * Copyright 2019 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage", "SyntheticAccessor")

package androidx.annotation.experimental.lint

import com.android.tools.lint.client.api.AnnotationLookup
import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.detector.api.AnnotationUsageType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UArrayAccessExpression
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCallableReferenceExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UEnumConstant
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.java.JavaUAnnotation
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.tryResolve
import java.util.ArrayList
import java.util.Locale

class ExperimentalDetector : Detector(), SourceCodeScanner {
    private val visitedUsages: MutableMap<UElement, MutableSet<String>> = mutableMapOf()

    override fun applicableAnnotations(): List<String> = listOf(
        JAVA_EXPERIMENTAL_ANNOTATION,
        KOTLIN_EXPERIMENTAL_ANNOTATION,
        JAVA_REQUIRES_OPT_IN_ANNOTATION,
        KOTLIN_REQUIRES_OPT_IN_ANNOTATION
    )

    override fun applicableSuperClasses(): List<String> = listOf(
        "java.lang.Object"
    )

    override fun visitClass(
        context: JavaContext,
        declaration: UClass,
    ) {
        declaration.methods.forEach { method ->
            val eval = context.evaluator
            if (eval.isOverride(method, true)) {
                checkMethodOverride(context, method)
            }
        }
    }

    /**
     * Extract the relevant annotations from the method override and run the checks
     * on the annotations.
     *
     * Based on Lint's `AnnotationHandler.checkCall)()`.
     */
    private fun checkMethodOverride(
        context: JavaContext,
        methodDeclaration: UMethod,
    ) {
        methodDeclaration.findSuperMethods().forEach { superMethod ->
            val evaluator = context.evaluator
            val allAnnotations = evaluator.getAllAnnotations(superMethod, inHierarchy = true)
            val methodAnnotations = filterRelevantAnnotations(
                evaluator, allAnnotations, methodDeclaration,
            )

            // Look for annotations on the class as well: these trickle
            // down to all the methods in the class
            val containingClass: PsiClass? = superMethod.containingClass
            val (classAnnotations, pkgAnnotations) = getClassAndPkgAnnotations(
                containingClass, evaluator, methodDeclaration
            )

            doCheckMethodOverride(
                context,
                superMethod,
                methodAnnotations,
                classAnnotations,
                pkgAnnotations,
                methodDeclaration,
                containingClass,
            )
        }
    }

    /**
     * Do the checks of a method override based on the method, class, and package
     * annotations given.
     *
     * Based on Lint's `AnnotationHandler.doCheckCall()`.
     */
    private fun doCheckMethodOverride(
        context: JavaContext,
        superMethod: PsiMethod?,
        methodAnnotations: List<UAnnotation>,
        classAnnotations: List<UAnnotation>,
        pkgAnnotations: List<UAnnotation>,
        method: UMethod,
        containingClass: PsiClass?,
    ) {
        if (methodAnnotations.isNotEmpty()) {
            checkAnnotations(
                context,
                argument = method,
                type = AnnotationUsageType.METHOD_CALL,
                method = superMethod,
                referenced = superMethod,
                annotations = methodAnnotations,
                allMethodAnnotations = methodAnnotations,
                allClassAnnotations = classAnnotations,
                packageAnnotations = pkgAnnotations,
                annotated = superMethod,
            )
        }

        if (containingClass != null && classAnnotations.isNotEmpty()) {
            checkAnnotations(
                context,
                argument = method,
                type = AnnotationUsageType.METHOD_CALL_CLASS,
                method = superMethod,
                referenced = superMethod,
                annotations = classAnnotations,
                allMethodAnnotations = methodAnnotations,
                allClassAnnotations = classAnnotations,
                packageAnnotations = pkgAnnotations,
                annotated = containingClass,
            )
        }

        if (pkgAnnotations.isNotEmpty()) {
            checkAnnotations(
                context,
                argument = method,
                type = AnnotationUsageType.METHOD_CALL_PACKAGE,
                method = superMethod,
                referenced = superMethod,
                annotations = pkgAnnotations,
                allMethodAnnotations = methodAnnotations,
                allClassAnnotations = classAnnotations,
                packageAnnotations = pkgAnnotations,
                annotated = null,
            )
        }
    }

    /**
     * Copied from Lint's `AnnotationHandler.checkAnnotations()` with modifications to operate on
     * this detector only, rather than a list of scanners.
     */
    private fun checkAnnotations(
        context: JavaContext,
        argument: UElement,
        type: AnnotationUsageType,
        method: PsiMethod?,
        referenced: PsiElement?,
        annotations: List<UAnnotation>,
        allMethodAnnotations: List<UAnnotation> = emptyList(),
        allClassAnnotations: List<UAnnotation> = emptyList(),
        packageAnnotations: List<UAnnotation> = emptyList(),
        annotated: PsiElement?
    ) {
        for (annotation in annotations) {
            val signature = annotation.qualifiedName ?: continue
            var uAnnotations: List<UAnnotation>? = null
            var psiAnnotations: Array<out PsiAnnotation>? = null

            // Modification: Removed loop over uastScanners list.
            if (isApplicableAnnotationUsage(type)) {

                // Some annotations should not be treated as inherited though
                // the hierarchy: if that's the case for this annotation in
                // this scanner, check whether it's inherited and if so, skip it
                if (annotated != null && !inheritAnnotation(signature)) {
                    // First try to look by directly checking the owner element of
                    // the annotation.
                    val annotationOwner = (annotation.sourcePsi as? PsiAnnotation)?.owner
                    val owner =
                        if (annotationOwner is PsiElement) {
                            PsiTreeUtil.getParentOfType(
                                annotationOwner,
                                PsiModifierListOwner::class.java
                            )
                        } else {
                            null
                        }
                    if (owner != null) {
                        val annotatedPsi = (annotated as? UElement)?.sourcePsi ?: annotated
                        if (owner != annotatedPsi) {
                            continue
                        }
                    } else {
                        // Figure out if this is an inherited annotation: it would be
                        // if it's not annotated on the element
                        if (annotated is UAnnotated) {
                            var found = false
                            for (
                                uAnnotation in uAnnotations ?: run {
                                    val list = context.evaluator.getAllAnnotations(
                                        annotated,
                                        inHierarchy = false
                                    )
                                    uAnnotations = list
                                    list
                                }
                            ) {
                                val qualifiedName = uAnnotation.qualifiedName
                                if (qualifiedName == signature) {
                                    found = true
                                    break
                                }
                            }
                            if (!found) {
                                continue
                            }
                        }
                        if (annotated is PsiModifierListOwner) {
                            var found = false

                            for (
                                psiAnnotation in psiAnnotations ?: run {
                                    val array =
                                        context.evaluator.getAllAnnotations(annotated, false)
                                    psiAnnotations = array
                                    array
                                }
                            ) {
                                val qualifiedName = psiAnnotation.qualifiedName
                                if (qualifiedName == signature) {
                                    found = true
                                    break
                                }
                            }
                            if (!found) {
                                continue
                            }
                        }
                    }
                }

                visitAnnotationUsage(
                    context, argument, type, annotation,
                    signature, method, referenced, annotations, allMethodAnnotations,
                    allClassAnnotations, packageAnnotations
                )
            }
        }
    }

    /**
     * Copied from Lint's `AnnotationHandler`.
     */
    private fun getClassAndPkgAnnotations(
        containingClass: PsiClass?,
        evaluator: JavaEvaluator,
        context: UElement,
    ): Pair<List<UAnnotation>, List<UAnnotation>> {

        // Yes, returning a pair is ugly. But we are initializing two lists, and splitting this
        // into two methods takes more lines of code then it saves over copying this block into
        // two methods.
        // Plus, destructuring assignment makes using the results less verbose.
        val classAnnotations: List<UAnnotation>
        val pkgAnnotations: List<UAnnotation>

        if (containingClass != null) {
            val annotations = evaluator.getAllAnnotations(containingClass, inHierarchy = true)
            classAnnotations = filterRelevantAnnotations(evaluator, annotations, context)

            val pkg = evaluator.getPackage(containingClass)
            pkgAnnotations = if (pkg != null) {
                val annotations2 = evaluator.getAllAnnotations(pkg, inHierarchy = false)
                filterRelevantAnnotations(evaluator, annotations2)
            } else {
                emptyList()
            }
        } else {
            classAnnotations = emptyList()
            pkgAnnotations = emptyList()
        }

        return Pair(classAnnotations, pkgAnnotations)
    }

    /**
     * Copied from Lint's `AnnotationHandler`.
     */
    private fun filterRelevantAnnotations(
        evaluator: JavaEvaluator,
        annotations: Array<PsiAnnotation>,
        context: UElement? = null
    ): List<UAnnotation> {
        var result: MutableList<UAnnotation>? = null
        val length = annotations.size
        if (length == 0) {
            return emptyList()
        }
        for (annotation in annotations) {
            val signature = annotation.qualifiedName
            if (signature == null ||
                (
                    signature.startsWith("kotlin.") ||
                        signature.startsWith("java.")
                    ) && !relevantAnnotations.contains(signature)
            ) {
                // @Override, @SuppressWarnings etc. Ignore
                continue
            }

            if (relevantAnnotations.contains(signature)) {
                val uAnnotation = JavaUAnnotation.wrap(annotation)

                // Common case: there's just one annotation; no need to create a list copy
                if (length == 1) {
                    return listOf(uAnnotation)
                }
                if (result == null) {
                    result = ArrayList(2)
                }
                result.add(uAnnotation)
                continue
            }

            // Special case @IntDef and @StringDef: These are used on annotations
            // themselves. For example, you create a new annotation named @foo.bar.Baz,
            // annotate it with @IntDef, and then use @foo.bar.Baz in your signatures.
            // Here we want to map from @foo.bar.Baz to the corresponding int def.
            // Don't need to compute this if performing @IntDef or @StringDef lookup

            val cls = annotation.nameReferenceElement?.resolve() ?: run {
                val project = annotation.project
                JavaPsiFacade.getInstance(project).findClass(
                    signature,
                    GlobalSearchScope.projectScope(project)
                )
            } ?: continue
            if (cls !is PsiClass || !cls.isAnnotationType) {
                continue
            }
            val innerAnnotations = evaluator.getAllAnnotations(cls, inHierarchy = false)
            for (j in innerAnnotations.indices) {
                val inner = innerAnnotations[j]
                val a = inner.qualifiedName
                if (a != null && relevantAnnotations.contains(a)) {
                    if (result == null) {
                        result = ArrayList(2)
                    }
                    val innerU = annotationLookup.findRealAnnotation(inner, cls, context)
                    result.add(innerU)
                }
            }
        }

        return result ?: emptyList()
    }

    // Used for drop-in compatibility with code from Lint's `AnnotationHandler`.
    private val relevantAnnotations: List<String>
        get() = applicableAnnotations()

    private val annotationLookup = AnnotationLookup()

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
            JAVA_EXPERIMENTAL_ANNOTATION, JAVA_REQUIRES_OPT_IN_ANNOTATION -> {
                // Only allow Java annotations, since the Kotlin compiler doesn't understand our
                // annotations and could get confused when it's trying to opt-in to some random
                // annotation that it doesn't understand.
                checkExperimentalUsage(
                    context,
                    annotation,
                    referenced,
                    usage,
                    listOf(
                        JAVA_USE_EXPERIMENTAL_ANNOTATION,
                        JAVA_OPT_IN_ANNOTATION
                    ),
                )
            }
            KOTLIN_EXPERIMENTAL_ANNOTATION, KOTLIN_REQUIRES_OPT_IN_ANNOTATION -> {
                // Don't check usages of Kotlin annotations from Kotlin sources, since the Kotlin
                // compiler handles that already. Allow either Java or Kotlin annotations, since
                // we can enforce both and it's possible that a Kotlin-sourced experimental library
                // is being used from Java without the Kotlin stdlib in the classpath.
                if (!isKotlin(usage.sourcePsi)) {
                    checkExperimentalUsage(
                        context,
                        annotation,
                        referenced,
                        usage,
                        listOf(
                            KOTLIN_USE_EXPERIMENTAL_ANNOTATION,
                            KOTLIN_OPT_IN_ANNOTATION,
                            JAVA_USE_EXPERIMENTAL_ANNOTATION,
                            JAVA_OPT_IN_ANNOTATION
                        ),
                    )
                }
            }
        }
    }

    /**
     * Check whether the given experimental API [annotation] can be referenced from [usage] call
     * site.
     *
     * @param context the lint scanning context
     * @param annotation the experimental opt-in annotation detected on the referenced element
     * @param usage the element whose usage should be checked
     * @param optInFqNames fully-qualified class name for experimental opt-in annotation
     */
    private fun checkExperimentalUsage(
        context: JavaContext,
        annotation: UAnnotation,
        referenced: PsiElement?,
        usage: UElement,
        optInFqNames: List<String>
    ) {
        val annotationFqName = (annotation.uastParent as? UClass)?.qualifiedName ?: return

        // This method may get called multiple times when there is more than one instance of the
        // annotation in the hierarchy. We don't care which one we're looking at, but we shouldn't
        // report the same usage and annotation pair multiple times.
        val visitedAnnotations = visitedUsages.getOrPut(usage, { mutableSetOf() })
        if (!visitedAnnotations.add(annotationFqName)) {
            return
        }

        // Check whether the usage actually considered experimental.
        val decl = referenced.toUElement() ?: usage.getReferencedElement() ?: return
        if (!decl.isExperimentalityRequired(context, annotationFqName)) {
            return
        }

        // Check whether the usage is acceptable, either due to opt-in or propagation.
        if (usage.isExperimentalityAccepted(context, annotationFqName, optInFqNames)) {
            return
        }

        // For some reason we can't read the explicit default level from the compiled version
        // of `kotlin.Experimental` (but we can from `kotlin.RequiresOptIn`... go figure). It's
        // possible that we'll fail to read the level for other reasons, but the safest
        // fallback is `ERROR` either way.
        val level = annotation.extractAttribute(context, "level", "ERROR")
        if (level != null) {
            report(
                context,
                usage,
                annotationFqName,
                "This declaration is opt-in and its usage should be marked with " +
                    "`@$annotationFqName` or `@OptIn(markerClass = $annotationFqName.class)`",
                level
            )
        } else {
            // This is a more serious failure where we obtained a representation that we
            // couldn't understand.
            report(
                context,
                usage,
                annotationFqName,
                "Failed to read `level` from `@$annotationFqName` -- assuming `ERROR`. " +
                    "This declaration is opt-in and its usage should be marked with " +
                    "`@$annotationFqName` or `@OptIn(markerClass = $annotationFqName.class)`",
                "ERROR"
            )
        }
    }

    /**
     * Determines if the element is within scope of the experimental marker identified by
     * [annotationFqName], thus whether it requires either opt-in or propagation of the marker.
     *
     * This is functionally equivalent to a containment check for [annotationFqName] on the result
     * of the Kotlin compiler's implementation of `DeclarationDescriptor.loadExperimentalities()`
     * within `ExperimentalUsageChecker`.
     */
    private fun UElement.isExperimentalityRequired(
        context: JavaContext,
        annotationFqName: String,
    ): Boolean {
        // Is the element itself experimental?
        if (isDeclarationAnnotatedWith(annotationFqName)) {
            return true
        }

        // Is a parent of the element experimental? Kotlin's implementation skips this check if
        // the current element is a constructor method, but it's required when we're looking at
        // the syntax tree through UAST. Unclear why.
        if ((uastParent as? UClass)?.isExperimentalityRequired(context, annotationFqName) == true) {
            return true
        }

        // Is the containing package experimental?
        if (context.evaluator.getPackage(this)?.getAnnotation(annotationFqName) != null) {
            return true
        }

        return false
    }

    /**
     * Returns whether the element has accepted the scope of the experimental marker identified by
     * [annotationFqName], either by opting-in via an annotation in [optInFqNames] or propagating
     * the marker.
     *
     * This is functionally equivalent to the Kotlin compiler's implementation of
     * `PsiElement.isExperimentalityAccepted()` within `ExperimentalUsageChecker`.
     */
    private fun UElement.isExperimentalityAccepted(
        context: JavaContext,
        annotationFqName: String,
        optInFqNames: List<String>,
    ): Boolean {
        val config = context.configuration
        return config.getOption(ISSUE_ERROR, "opt-in")?.contains(annotationFqName) == true ||
            config.getOption(ISSUE_WARNING, "opt-in")?.contains(annotationFqName) == true ||
            anyParentMatches({ element ->
                element.isDeclarationAnnotatedWith(annotationFqName) ||
                    element.isDeclarationAnnotatedWithOptInOf(annotationFqName, optInFqNames)
            })
    }

    /**
     * Reports an issue and trims indentation on the [message].
     */
    private fun report(
        context: JavaContext,
        usage: UElement,
        annotation: String,
        message: String,
        level: String,
    ) {
        val issue = when (level) {
            ENUM_ERROR -> ISSUE_ERROR
            ENUM_WARNING -> ISSUE_WARNING
            else -> throw IllegalArgumentException(
                "Level was \"$level\" but must be one of: $ENUM_ERROR, $ENUM_WARNING"
            )
        }

        try {
            if (context.configuration.getOption(issue, "opt-in")?.contains(annotation) != true) {
                context.report(issue, usage, context.getNameLocation(usage), message.trimIndent())
            }
        } catch (e: UnsupportedOperationException) {
            if ("Method not implemented" == e.message) {
                // Workaround for b/191286558 where lint attempts to read annotations from a
                // compiled UAST parent of `usage`. Swallow the exception and don't report anything.
            } else {
                throw e
            }
        }
    }

    companion object {
        private val IMPLEMENTATION = Implementation(
            ExperimentalDetector::class.java,
            Scope.JAVA_FILE_SCOPE,
        )

        const val KOTLIN_EXPERIMENTAL_ANNOTATION = "kotlin.Experimental"
        const val KOTLIN_USE_EXPERIMENTAL_ANNOTATION = "kotlin.UseExperimental"

        const val KOTLIN_OPT_IN_ANNOTATION = "kotlin.OptIn"
        const val KOTLIN_REQUIRES_OPT_IN_ANNOTATION = "kotlin.RequiresOptIn"

        const val JAVA_EXPERIMENTAL_ANNOTATION =
            "androidx.annotation.experimental.Experimental"
        const val JAVA_USE_EXPERIMENTAL_ANNOTATION =
            "androidx.annotation.experimental.UseExperimental"

        const val JAVA_REQUIRES_OPT_IN_ANNOTATION =
            "androidx.annotation.RequiresOptIn"
        const val JAVA_OPT_IN_ANNOTATION =
            "androidx.annotation.OptIn"

        const val ENUM_ERROR = "ERROR"
        const val ENUM_WARNING = "WARNING"

        private fun issueForLevel(
            levelEnum: String,
            severity: Severity,
        ): Issue {
            val levelText = levelEnum.toLowerCaseAsciiOnly()
            val issueId = "UnsafeOptInUsage${levelText.capitalizeAsciiOnly()}"
            return Issue.create(
                id = issueId,
                briefDescription = "Unsafe opt-in usage intended to be $levelText-level severity",
                explanation = """
                This API has been flagged as opt-in with $levelText-level severity.

                Any declaration annotated with this marker is considered part of an unstable or
                otherwise non-standard API surface and its call sites should accept the opt-in
                aspect of it by using the `@OptIn` annotation, using the marker annotation --
                effectively causing further propagation of the opt-in aspect -- or configuring
                the `$issueId` check's options for project-wide opt-in.

                To configure project-wide opt-in, specify the `opt-in` option value in `lint.xml`
                as a comma-delimited list of opted-in annotations:

                ```
                <lint>
                    <issue id="$issueId">
                        <option name="opt-in" value="com.foo.ExperimentalBarAnnotation" />
                    </issue>
                </lint>
                ```
                """.trimIndent(),
                category = Category.CORRECTNESS,
                priority = 4,
                severity = severity,
                implementation = IMPLEMENTATION,
            )
        }

        val ISSUE_ERROR = issueForLevel(ENUM_ERROR, Severity.ERROR)
        val ISSUE_WARNING = issueForLevel(ENUM_WARNING, Severity.WARNING)

        val ISSUES = listOf(
            ISSUE_ERROR,
            ISSUE_WARNING,
        )
    }
}

private fun UAnnotation.hasMatchingAttributeValueClass(
    attributeName: String,
    className: String
): Boolean {
    val attributeValue = findDeclaredAttributeValue(attributeName)
    if (attributeValue.getFullyQualifiedName() == className) {
        return true
    }
    if (attributeValue is UCallExpression) {
        return attributeValue.valueArguments.any { attrValue ->
            attrValue.getFullyQualifiedName() == className
        }
    }
    return false
}

/**
 * Returns the fully-qualified class name for a given attribute value, if any.
 */
private fun UExpression?.getFullyQualifiedName(): String? {
    val type = if (this is UClassLiteralExpression) this.type else this?.evaluate()
    return (type as? PsiClassType)?.canonicalText
}

private fun UElement?.getReferencedElement(): UElement? =
    when (this) {
        is UBinaryExpression ->
            leftOperand.tryResolve() // or referenced
        is UMethod ->
            this // or referenced
        is UClass ->
            uastSuperTypes.firstNotNullOfOrNull {
                PsiTypesUtil.getPsiClass(it.type)
            } // or referenced
        is USimpleNameReferenceExpression ->
            resolve().let { field -> field as? PsiField ?: field as? PsiMethod } // or referenced
        is UCallExpression ->
            resolve() ?: classReference?.resolve() // referenced is empty for constructor
        is UCallableReferenceExpression ->
            resolve() as? PsiMethod // or referenced
        is UAnnotation ->
            null
        is UEnumConstant ->
            resolveMethod() // or referenced
        is UArrayAccessExpression ->
            (receiver as? UReferenceExpression)?.resolve() // or referenced
        is UVariable ->
            this
        else ->
            null
    }.toUElement()

/**
 * Tests each parent in the elements hierarchy including the element itself. Returns `true` for the
 * first element where [positivePredicate] matches or `false` for the first element where
 * [negativePredicate] returns matches. If neither predicate is matched, returns [defaultValue].
 */
private inline fun UElement.anyParentMatches(
    positivePredicate: (element: UElement) -> Boolean,
    negativePredicate: (element: UElement) -> Boolean = { false },
    defaultValue: Boolean = false
): Boolean {
    var element = this
    while (true) {
        if (positivePredicate(element)) return true
        if (negativePredicate(element)) return false
        element = element.uastParent ?: return defaultValue
    }
}

/**
 * Returns whether the element declaration is annotated with the specified annotation.
 */
private fun UElement.isDeclarationAnnotatedWith(
    annotationFqName: String,
) = (this as? UAnnotated)?.findAnnotation(annotationFqName) != null

/**
 * Returns whether the element declaration is annotated with any of the specified opt-in
 * annotations where the value of `markerClass` contains the specified annotation.
 */
private fun UElement.isDeclarationAnnotatedWithOptInOf(
    annotationFqName: String,
    optInFqNames: List<String>,
) = (this as? UAnnotated)?.let { annotated ->
    optInFqNames.any { optInFqName ->
        annotated.findAnnotation(optInFqName)?.hasMatchingAttributeValueClass(
            "markerClass",
            annotationFqName,
        ) == true
    }
} == true
