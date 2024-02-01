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

package androidx.compose.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.lang.jvm.types.JvmType
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.UObjectLiteralExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.tryResolve

/**
 * Lint [Detector] that catches patterns that are disallowed in MPP (JS + Native) in common module.
 * Most Compose modules are compiled only for JVM in AndroidX, so Kotlin compiler doesn't report
 * these issues.
 */
class CommonModuleIncompatibilityDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(
            UImportStatement::class.java,
            USimpleNameReferenceExpression::class.java,
            UClass::class.java,
            UObjectLiteralExpression::class.java
        )

    override fun createUastHandler(context: JavaContext): UElementHandler {
        if (!context.file.absolutePath.contains(COMMON_MAIN_PATH_PREFIX)) {
            return UElementHandler.NONE
        }

        return object : UElementHandler() {
            override fun visitImportStatement(node: UImportStatement) {
                val reference = node.importReference?.asRenderString() ?: return
                val isPlatformImport = PLATFORM_PACKAGES.any { platformPackage ->
                    (platformPackage == reference && node.isOnDemand) ||
                        reference.startsWith("$platformPackage.")
                }
                if (!isPlatformImport) return

                val target = node.importReference!!
                context.report(
                    IMPORT_ISSUE,
                    target,
                    context.getLocation(target),
                    "Platform-dependent import in a common module"
                )
            }

            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                if (node.identifier in RESTRICTED_PROPERTIES) {
                    val method = node.tryResolve()
                    if (method !is PsiMethod) return

                    val fqName = RESTRICTED_PROPERTIES[node.identifier]!!
                    if (method.name != fqName.shortName) return
                    if (!method.isInPackageName(fqName.packageName)) return

                    context.report(
                        REFERENCE_ISSUE,
                        node,
                        context.getLocation(node),
                        "Platform reference in a common module"
                    )
                }
            }

            override fun visitClass(node: UClass) {
                val extendsLambda = node.uastSuperTypes.any { it.type.isLambda() }
                if (extendsLambda) {
                    context.report(
                        EXTENDS_LAMBDA_ISSUE,
                        node,
                        context.getLocation(node.nameIdentifier),
                        "Extending Kotlin lambda interfaces is not allowed in common code"
                    )
                }
            }

            override fun visitObjectLiteralExpression(node: UObjectLiteralExpression) {
                val extendsLambda = node.declaration.uastSuperTypes.any { it.type.isLambda() }
                if (extendsLambda) {
                    context.report(
                        EXTENDS_LAMBDA_ISSUE,
                        node,
                        context.getLocation(node),
                        "Extending Kotlin lambda interfaces is not allowed in common code"
                    )
                }
            }
        }
    }

    companion object {
        val IMPORT_ISSUE = Issue.create(
            id = "PlatformImportInCommonModule",
            briefDescription = "Platform-dependent import in a common module",
            explanation = "Common Kotlin module cannot contain references to JVM or Android " +
                "classes, as it reduces future portability to other Kotlin targets. Consider " +
                "alternative methods allowed in common Kotlin code, or use expect/actual " +
                "to reference the platform code instead.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                CommonModuleIncompatibilityDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        val REFERENCE_ISSUE = Issue.create(
            id = "PlatformReferenceInCommonModule",
            briefDescription = "Platform-dependent reference in a common module",
            explanation = "Common Kotlin module cannot contain references to JVM or Android " +
                "classes, as it reduces future portability to other Kotlin targets. Consider " +
                "alternative methods allowed in common Kotlin code, or use expect/actual " +
                "to reference the platform code instead.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                CommonModuleIncompatibilityDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        val EXTENDS_LAMBDA_ISSUE = Issue.create(
            id = "ExtendedFunctionNInterface",
            briefDescription = "Extending Kotlin FunctionN interfaces in common code",
            explanation = "Common Kotlin module are ported to other Kotlin targets, including JS." +
                " Kotlin JS backend does not support extending lambda interfaces. Consider" +
                "extending fun interface in common Kotlin code, or use expect/actual instead.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                CommonModuleIncompatibilityDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        private const val COMMON_MAIN_PATH_PREFIX = "src/commonMain"
        private val PLATFORM_PACKAGES = listOf(
            "java",
            "javax",
            "android"
        )
        private val RESTRICTED_PROPERTIES = mapOf(
            "javaClass" to Name(Package("kotlin.jvm"), "getJavaClass"),
            "java" to Name(Package("kotlin.jvm"), "getJavaClass"),
        )
    }
}

private const val FunctionPrefix = "kotlin.jvm.functions.Function"

@Suppress("UnstableApiUsage")
private fun JvmType.isLambda(): Boolean =
    (this is PsiClassReferenceType && reference.qualifiedName.startsWith(FunctionPrefix))
