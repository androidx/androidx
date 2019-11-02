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

package androidx.lifecycle.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.util.isConstructorCall

/**
 * Lint check for ensuring that [androidx.lifecycle.MutableLiveData] objects are explicitly
 * declared with nullable type parameter in Kotlin.
 */
class NonNullableMutableLiveDataDetector : Detector(), SourceCodeScanner {

    companion object {
        val ISSUE = Issue.Companion.create(
            id = "NullSafeMutableLiveData",
            briefDescription = "MutableLiveData should be initialized with nullable type.",
            explanation = """Kotlin interoperability does not support enforcing explicit \
                null-safety when using generic Java type parameters. Since LiveData is a Java \
                class its value can always be null even when its type is explicitly declared as \
                non-nullable. This can lead to runtime exceptions from reading a null LiveData \
                value that is assumed to be non-nullable.""",
            category = Category.INTEROPERABILITY_KOTLIN,
            severity = Severity.WARNING,
            implementation = Implementation(
                NonNullableMutableLiveDataDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            ),
            androidSpecific = true
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? =
        listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val sourcePsi = node.sourcePsi
                if (isKotlin(sourcePsi) && node.isConstructorCall()) {
                    val psiMethod = node.resolve()
                    // We can only perform checks on MutableLiveData because checking any
                    // type that extends LiveData would result in false negatives for java LiveData
                    // subclasses that have build in non-null values.
                    if (context.evaluator.isMemberInClass(psiMethod,
                            "androidx.lifecycle.MutableLiveData")) {
                        val typeArgument = (sourcePsi as KtCallExpression).typeArguments[0]
                        if (typeArgument?.typeReference?.typeElement !is KtNullableType) {
                            context.report(ISSUE,
                                typeArgument,
                                context.getLocation(typeArgument),
                                "Use nullable type parameter.",
                                fix().replace()
                                    .with("?")
                                    .range(context.getLocation(typeArgument))
                                    .end()
                                    .build())
                        }
                    }
                }
            }
        }
}
