/*
 * Copyright 2023 The Android Open Source Project
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

import com.android.SdkConstants.ATTR_VALUE
import com.android.tools.lint.detector.api.AnnotationInfo
import com.android.tools.lint.detector.api.AnnotationUsageInfo
import com.android.tools.lint.detector.api.AnnotationUsageType
import com.android.tools.lint.detector.api.AnnotationUsageType.ASSIGNMENT_LHS
import com.android.tools.lint.detector.api.AnnotationUsageType.ASSIGNMENT_RHS
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import com.android.tools.lint.model.DefaultLintModelMavenName
import com.android.tools.lint.model.LintModelLibrary
import com.android.tools.lint.model.LintModelMavenName
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.compiled.ClsAnnotationImpl
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.UastEmptyExpression
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.util.isArrayInitializer

/** Adapted from com/android/tools/lint/checks/RestrictToDetector.kt in Android Studio repo. */
class RestrictToDetector : AbstractAnnotationDetector(), SourceCodeScanner {
    override fun applicableAnnotations(): List<String> = listOf(RESTRICT_TO_ANNOTATION)

    override fun inheritAnnotation(annotation: String): Boolean {
        // Require restriction annotations to be annotated everywhere
        return false
    }

    override fun isApplicableAnnotationUsage(type: AnnotationUsageType): Boolean {
        return type != ASSIGNMENT_LHS &&
            type != ASSIGNMENT_RHS &&
            super.isApplicableAnnotationUsage(type)
    }

    override fun visitAnnotationUsage(
        context: JavaContext,
        element: UElement,
        annotationInfo: AnnotationInfo,
        usageInfo: AnnotationUsageInfo
    ) {
        val type = usageInfo.type
        if (type == AnnotationUsageType.EXTENDS && element is UTypeReferenceExpression) {
            // If it's a constructor reference we don't need to also check the type
            // reference. Ideally we'd do a "parent is KtConstructorCalleeExpression"
            // here, but that points to impl classes in its hierarchy which leads to
            // class loading trouble.
            val sourcePsi = element.sourcePsi
            if (
                sourcePsi != null &&
                    isKotlin(sourcePsi.language) &&
                    sourcePsi.parent?.toString() == "CONSTRUCTOR_CALLEE"
            ) {
                return
            }
        }

        val member = usageInfo.referenced as? PsiMember
        val annotation = annotationInfo.annotation
        val qualifiedName = annotationInfo.qualifiedName
        if (RESTRICT_TO_ANNOTATION == qualifiedName) {
            checkRestrictTo(context, element, member, annotation, usageInfo)
        }
    }

    private fun isTestContext(context: JavaContext, element: UElement): Boolean {
        var current = element
        // (1) Is this compilation unit in a test source path?
        if (context.isTestSource) {
            return true
        }

        // (2) Is this AST node surrounded by a test-only annotation?
        while (true) {
            val owner = current.getParentOfType<UAnnotated>(true) ?: break

            //noinspection AndroidLintExternalAnnotations
            for (annotation in owner.uAnnotations) {
                val name = annotation.qualifiedName ?: continue
                when {
                    RESTRICT_TO_ANNOTATION == name -> {
                        val restrictionScope = getRestrictionScope(annotation)
                        if (restrictionScope and RESTRICT_TO_TESTS != 0) {
                            return true
                        }
                    }
                    name.endsWith(VISIBLE_FOR_TESTING_SUFFIX) -> return true
                    name.endsWith(".TestOnly") -> return true
                }
            }

            current = owner
        }

        return false
    }

    // TODO: Test XML access of restricted classes
    private fun checkRestrictTo(
        context: JavaContext,
        node: UElement,
        method: PsiMember?,
        annotation: UAnnotation,
        usageInfo: AnnotationUsageInfo
    ) {
        val scope = getRestrictionScope(annotation)
        if (scope != 0) {
            checkRestrictTo(context, node, method, usageInfo, scope)
        }
    }

    private fun checkRestrictTo(
        context: JavaContext,
        node: UElement,
        member: PsiMember?,
        usageInfo: AnnotationUsageInfo,
        scope: Int
    ) {

        val containingClass =
            when {
                node is UTypeReferenceExpression -> PsiTypesUtil.getPsiClass(node.type)
                member != null -> member.containingClass
                node is UCallExpression -> node.classReference?.resolve() as? PsiClass?
                node is PsiClass -> node
                else -> null
            }

        containingClass ?: return

        if (usageInfo.anyCloser { it.qualifiedName == RESTRICT_TO_ANNOTATION }) {
            return
        }

        if (scope and RESTRICT_TO_LIBRARY_GROUP != 0 && member != null) {
            val evaluator = context.evaluator
            val thisCoordinates = evaluator.getLibrary(node) ?: context.project.mavenCoordinate
            val methodCoordinates = context.findMavenCoordinate(member)
            val thisGroup = thisCoordinates?.groupId
            val methodGroup = methodCoordinates?.groupId
            if (thisGroup != methodGroup && methodGroup != null) {
                val thisGroupDisplayText = thisGroup ?: "<unknown>"
                val where =
                    "from within the same library group (referenced groupId=`$methodGroup` from " +
                        "groupId=`$thisGroupDisplayText`)"
                reportRestriction(where, containingClass, member, context, node, usageInfo)
            }
        } else if (scope and RESTRICT_TO_LIBRARY_GROUP_PREFIX != 0 && member != null) {
            val evaluator = context.evaluator
            val thisCoordinates = evaluator.getLibrary(node) ?: context.project.mavenCoordinate
            val methodCoordinates =
                evaluator.getLibrary(member)
                    ?: run {
                        if (thisCoordinates != null && member !is PsiCompiledElement) {
                            // Local source?
                            context.evaluator.getProject(member)?.mavenCoordinate
                        } else {
                            null
                        }
                    }
            val thisGroup = thisCoordinates?.groupId
            val methodGroup = methodCoordinates?.groupId
            if (
                methodGroup != null &&
                    (thisGroup == null || !sameLibraryGroupPrefix(thisGroup, methodGroup))
            ) {
                val expectedPrefix =
                    methodGroup.lastIndexOf('.').let {
                        if (it < 0) {
                            "\"\""
                        } else {
                            methodGroup.substring(0, it)
                        }
                    }
                val where =
                    "from within the same library group prefix (referenced " +
                        "groupId=`$methodGroup` with prefix $expectedPrefix" +
                        "${if (thisGroup != null) " from groupId=`$thisGroup`" else ""})"
                reportRestriction(where, containingClass, member, context, node, usageInfo)
            }
        } else if (scope and RESTRICT_TO_LIBRARY != 0 && member != null) {
            val evaluator = context.evaluator
            val thisCoordinates = evaluator.getLibrary(node) ?: context.project.mavenCoordinate
            val methodCoordinates = context.findMavenCoordinate(member)
            val thisGroup = thisCoordinates?.groupId
            val methodGroup = methodCoordinates?.groupId
            if (thisGroup != methodGroup && methodGroup != null) {
                val thisArtifact = thisCoordinates?.artifactId
                val methodArtifact = methodCoordinates.artifactId
                if (thisArtifact != methodArtifact) {
                    val name =
                        if (methodGroup == "__local_aars__") {
                            "missing Maven coordinate due to repackaging"
                        } else {
                            "$methodGroup:$methodArtifact"
                        }
                    val where = "from within the same library ($name)"
                    reportRestriction(where, containingClass, member, context, node, usageInfo)
                }
            } else if (member !is PsiCompiledElement) {
                // If the resolved method is source, make sure they're part
                // of the same Gradle project
                val project = context.evaluator.getProject(member)
                if (project != null && project != context.project) {
                    val coordinates = project.mavenCoordinate
                    val name =
                        if (coordinates != null) {
                            "${coordinates.groupId}:${coordinates.artifactId}"
                        } else {
                            project.name
                        }
                    val where = "from within the same library ($name)"
                    reportRestriction(where, containingClass, member, context, node, usageInfo)
                }
            }
        }

        if (scope and RESTRICT_TO_TESTS != 0) {
            if (!isTestContext(context, node)) {
                reportRestriction("from tests", containingClass, member, context, node, usageInfo)
            }
        }

        if (scope and RESTRICT_TO_SUBCLASSES != 0) {
            val qualifiedName = containingClass.qualifiedName
            if (qualifiedName != null) {
                val evaluator = context.evaluator

                var outer: UClass?
                var isSubClass = false
                var prev = node

                while (true) {
                    outer = prev.getParentOfType(UClass::class.java, true)
                    if (outer == null) {
                        break
                    }
                    if (evaluator.inheritsFrom(outer, qualifiedName, false)) {
                        isSubClass = true
                        break
                    }

                    if (evaluator.isStatic(outer)) {
                        break
                    }
                    prev = outer
                }

                if (!isSubClass) {
                    reportRestriction(
                        "from subclasses",
                        containingClass,
                        member,
                        context,
                        node,
                        usageInfo
                    )
                }
            }
        }
    }

    private fun reportRestriction(
        where: String?,
        containingClass: PsiClass,
        member: PsiMember?,
        context: JavaContext,
        node: UElement,
        usageInfo: AnnotationUsageInfo
    ) {
        var api: String
        api =
            if (member == null || member is PsiMethod && member.isConstructor) {
                member?.name ?: (containingClass.name + " constructor")
            } else
            //noinspection LintImplPsiEquals
            if (containingClass == member) {
                member.name ?: "class"
            } else {
                containingClass.name + "." + member.name
            }

        var locationNode = node
        if (node is UCallExpression) {
            val nameElement = node.methodIdentifier
            if (nameElement != null) {
                locationNode = nameElement
            }

            // If the annotation was reported on the class, and the left hand side
            // expression is that class, use it as the name node?
            val annotation = usageInfo.annotations[usageInfo.index]
            val annotated = annotation.annotated
            //noinspection LintImplPsiEquals
            if (where == null && annotated is PsiClass && annotated != usageInfo.referenced) {
                val qualifier = node.receiver
                val className = annotated.name
                if (
                    qualifier != null &&
                        className != null &&
                        qualifier.asSourceString() == className
                ) {
                    locationNode = qualifier
                    api = className
                }
            }
        }

        // If this error message changes, you need to also update
        // ResourceTypeInspection#guessLintIssue
        var message: String
        if (where == null) {
            message = "$api is marked as internal and should not be accessed from apps"
        } else {
            val refType = if (member is PsiMethod) "called" else "accessed"
            message = "$api can only be $refType $where"

            // Most users will encounter this for the support library; let's have a clearer error
            // message
            // for that specific scenario
            if (where == "from within the same library (groupId=com.android.support)") {
                // If this error message changes, you need to also update
                // ResourceTypeInspection#guessLintIssue
                message =
                    "This API is marked as internal to the support library and should not be " +
                        "accessed from apps"
            }
        }

        val location =
            if (locationNode is UCallExpression) {
                context.getCallLocation(
                    locationNode,
                    includeReceiver = false,
                    includeArguments = false
                )
            } else {
                context.getLocation(locationNode)
            }
        report(context, RESTRICTED, node, location, message, null)
    }

    companion object {
        private val IMPLEMENTATION =
            Implementation(RestrictToDetector::class.java, Scope.JAVA_FILE_SCOPE)

        private const val RESTRICT_TO_ANNOTATION = "androidx.annotation.RestrictTo"
        private const val VISIBLE_FOR_TESTING_SUFFIX = ".VisibleForTesting"

        /** `RestrictTo(RestrictTo.Scope.GROUP_ID` */
        private const val RESTRICT_TO_LIBRARY_GROUP = 1 shl 0

        /** `RestrictTo(RestrictTo.Scope.GROUP_ID` */
        private const val RESTRICT_TO_LIBRARY = 1 shl 1

        /** `RestrictTo(RestrictTo.Scope.GROUP_ID` */
        private const val RESTRICT_TO_LIBRARY_GROUP_PREFIX = 1 shl 2

        /** `RestrictTo(RestrictTo.Scope.TESTS` */
        private const val RESTRICT_TO_TESTS = 1 shl 3

        /** `RestrictTo(RestrictTo.Scope.SUBCLASSES` */
        private const val RESTRICT_TO_SUBCLASSES = 1 shl 4

        private fun getRestrictionScope(annotation: UAnnotation): Int {
            val value = annotation.findDeclaredAttributeValue(ATTR_VALUE)
            if (value != null) {
                return getRestrictionScope(value, annotation)
            }
            return 0
        }

        @Suppress("UnstableApiUsage") // JvmAnnotation.findAttribute()
        private fun getRestrictionScope(expression: UExpression?, annotation: UAnnotation): Int {
            var scope = 0
            if (expression != null) {
                if (expression.isArrayInitializer()) {
                    val initializerExpression = expression as UCallExpression?
                    val initializers = initializerExpression!!.valueArguments
                    for (initializer in initializers) {
                        scope = scope or getRestrictionScope(initializer, annotation)
                    }
                } else if (expression is UReferenceExpression) {
                    val resolved = expression.resolve()
                    if (resolved is PsiField) {
                        val name = resolved.name
                        scope =
                            when (name) {
                                "GROUP_ID",
                                "LIBRARY_GROUP" -> RESTRICT_TO_LIBRARY_GROUP
                                "SUBCLASSES" -> RESTRICT_TO_SUBCLASSES
                                "TESTS" -> RESTRICT_TO_TESTS
                                "LIBRARY" -> RESTRICT_TO_LIBRARY
                                "LIBRARY_GROUP_PREFIX" -> RESTRICT_TO_LIBRARY_GROUP_PREFIX
                                else -> 0
                            }
                    }
                } else if (expression is UastEmptyExpression) {
                    // See JavaUAnnotation.findDeclaredAttributeValue
                    val psi = annotation.sourcePsi
                    if (psi is ClsAnnotationImpl) {
                        val otherwise = psi.findAttribute(ATTR_VALUE)
                        val v = otherwise?.attributeValue
                        if (v is JvmAnnotationConstantValue) {
                            val constant = v.constantValue
                            if (constant is Number) {
                                scope = constant.toInt()
                            }
                        }
                    }
                }
            }

            return scope
        }

        /**
         * Implements the group prefix equality that is described in the documentation for the
         * RestrictTo.Scope.LIBRARY_GROUP_PREFIX enum constant.
         */
        fun sameLibraryGroupPrefix(group1: String, group2: String): Boolean {
            // TODO: Allow group1.startsWith(group2) || group2.startsWith(group1) ?

            if (group1 == group2) {
                return true
            }

            // Implementation for AndroidX differs from the standard RestrictToDetector, since we
            // treat LIBRARY_GROUP_PREFIX as anything in the androidx.* package. See b/297047524.
            if (group1.startsWith(ANDROIDX_PREFIX) && group2.startsWith(ANDROIDX_PREFIX)) {
                return true
            }

            val i1 = group1.lastIndexOf('.')
            val i2 = group2.lastIndexOf('.')
            if (i2 != i1 || i1 == -1) {
                return false
            }

            return group1.regionMatches(0, group2, 0, i1)
        }

        private const val ANDROIDX_PREFIX = "androidx."

        /** Using a restricted API. */
        @JvmField
        val RESTRICTED =
            Issue.create(
                id = "RestrictedApiAndroidX",
                briefDescription = "Restricted API",
                explanation =
                    """
                This API has been flagged with a restriction that has not been met.

                Examples of API restrictions:
                * Method can only be invoked by a subclass
                * Method can only be accessed from within the same library (defined by the Gradle library group id)
                * Method can only be accessed from tests.

                You can add your own API restrictions with the `@RestrictTo` annotation.""",
                category = Category.CORRECTNESS,
                priority = 4,
                severity = Severity.ERROR,
                implementation = IMPLEMENTATION
            )
    }
}

/** Attempts to parse an unversioned Maven name from the library identifier. */
internal fun LintModelLibrary.getMavenNameFromIdentifier(): LintModelMavenName? {
    val indexOfSentinel = identifier.indexOf(":@@:")
    if (indexOfSentinel < 0) return null

    // May be suffixed with something like ::debug.
    val project = identifier.substring(indexOfSentinel + 4).substringBefore("::")
    val indexOfLastSeparator = project.lastIndexOf(':')
    if (indexOfLastSeparator < 0) return null

    val groupId = project.substring(0, indexOfLastSeparator).replace(':', '.')
    val artifactId = project.substring(indexOfLastSeparator + 1)
    return DefaultLintModelMavenName("androidx.$groupId", artifactId)
}
