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

package androidx.navigation.lint.common

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.getParameterForArgument
import org.jetbrains.uast.toUElement

/**
 * Checks for missing annotations on type-safe route declarations
 *
 * Retrieves route classes/objects by tracing KClasses passed as route during NavDestination
 * creation
 */
abstract class BaseTypeSafeDestinationMissingAnnotationDetector(
    private val methodNames: List<String>,
    private val constructorNames: List<String>
) : Detector(), SourceCodeScanner {

    // Issue instantiation requires specific Detector class which is the child class
    abstract fun getMissingSerializableIssue(): Issue

    abstract fun getMissingKeepIssue(): Issue

    final override fun getApplicableMethodNames(): List<String> = methodNames

    final override fun getApplicableConstructorTypes(): List<String> = constructorNames

    final override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod
    ) {
        val packageName = (method.containingFile as? PsiClassOwner)?.packageName
        if (
            packageName != NAVIGATION_PACKAGE_NAME && packageName != NAVIGATION_COMPOSE_PACKAGE_NAME
        )
            return

        val receiver = node.receiver?.getExpressionType()?.canonicalText

        // Get the destination Type, but we only want to lint routes from safe args apis.
        // Such routes come in two possible forms:
        // 1. route as regular parameter specifically for NavigatorProvider.navigation
        // 2. reified type parameter for all other destination builder apis
        val kClazzType =
            when {
                // route as parameter
                node.methodName == "navigation" &&
                    receiver == "androidx.navigation.NavigatorProvider" -> node.getRouteKClassType()
                // route as reified Type
                node.typeArguments.isNotEmpty() ->
                    (node.typeArguments.first() as? PsiClassReferenceType)?.resolve()
                else -> null
            } ?: return

        checkMissingSerializableAnnotation(kClazzType, context)

        // filter for Enums in Class fields
        val enums = kClazzType.getEnumFields()
        if (enums.isNotEmpty()) checkMissingKeepAnnotation(enums, context)
    }

    final override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod
    ) {
        val kClazzType = node.getRouteKClassType() ?: return
        checkMissingSerializableAnnotation(kClazzType, context)

        // filter for Enums in Class fields
        val enums = kClazzType.getEnumFields()
        if (enums.isNotEmpty()) checkMissingKeepAnnotation(enums, context)
    }

    // resolves and returns the actual type of KClass<*>
    private fun UCallExpression.getRouteKClassType(): PsiClass? {
        val routeNode =
            valueArguments.find {
                getParameterForArgument(it)?.name == "route" &&
                    it.sourcePsi is KtClassLiteralExpression
            } ?: return null
        return routeNode.getKClassType()
    }

    // check that the Type is annotated with @Serializable
    private fun checkMissingSerializableAnnotation(kClazz: PsiClass, context: JavaContext) {
        if (
            kClazz !is PsiCompiledElement &&
                !kClazz.isInterface &&
                !kClazz.hasAnnotation("kotlinx.serialization.Serializable")
        ) {
            val uElement = kClazz.toUElement() ?: return
            context.report(
                getMissingSerializableIssue(),
                uElement,
                context.getNameLocation(uElement),
                """To use this class or object as a type-safe destination, annotate it with @Serializable"""
            )
        }
    }

    private fun PsiClass.getEnumFields(): List<PsiClass> {
        return fields.mapNotNull {
            val resolved = (it.type as? PsiClassReferenceType)?.resolve()
            resolved?.takeIf { resolved.isEnum }
        }
    }

    private fun checkMissingKeepAnnotation(fields: List<PsiClass>, context: JavaContext) {
        fields.onEach {
            if (!it.hasAnnotation("androidx.annotation.Keep")) {
                val uElement = it.toUElement() ?: return
                context.report(
                    getMissingKeepIssue(),
                    uElement,
                    context.getNameLocation(uElement),
                    """To prevent this Enum's serializer from being obfuscated in minified builds, annotate it with @androidx.annotation.Keep
                        """
                        .trimMargin()
                )
            }
        }
    }
}

val NAVIGATION_PACKAGE_NAME = "androidx.navigation"

val NAVIGATION_COMPOSE_PACKAGE_NAME = "androidx.navigation.compose"

fun createMissingSerializableAnnotationIssue(
    detectorClass: Class<out BaseTypeSafeDestinationMissingAnnotationDetector>
) =
    Issue.create(
        id = "MissingSerializableAnnotation",
        briefDescription =
            "Type-safe NavDestinations must be annotated with " +
                "@kotlinx.serialization.Serializable.",
        explanation =
            "The destination needs to be annotated with @Serializable " +
                "in order for Navigation library to convert the class or object declaration " +
                "into a NavDestination.",
        category = Category.CORRECTNESS,
        severity = Severity.ERROR,
        implementation = Implementation(detectorClass, Scope.JAVA_FILE_SCOPE)
    )

fun createMissingKeepAnnotationIssue(
    detectorClass: Class<out BaseTypeSafeDestinationMissingAnnotationDetector>
) =
    Issue.create(
        id = "MissingKeepAnnotation",
        briefDescription =
            "In minified builds, Enum classes used as type-safe " +
                "Navigation arguments should be annotated with @androidx.annotation.Keep ",
        explanation =
            "Type-safe nav arguments such as Enum types can get " +
                "incorrectly obfuscated in minified builds when not referenced directly",
        category = Category.CORRECTNESS,
        severity = Severity.WARNING,
        implementation = Implementation(detectorClass, Scope.JAVA_FILE_SCOPE)
    )
