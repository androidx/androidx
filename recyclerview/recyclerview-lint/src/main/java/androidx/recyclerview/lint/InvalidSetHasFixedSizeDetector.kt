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

@file:Suppress("UnstableApiUsage")

package androidx.recyclerview.lint

import com.android.SdkConstants.ANDROID_URI
import com.android.SdkConstants.ATTR_ID
import com.android.SdkConstants.ATTR_LAYOUT_HEIGHT
import com.android.SdkConstants.ATTR_LAYOUT_WIDTH
import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.ResourceEvaluator
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.w3c.dom.Element
import java.util.EnumSet

class InvalidSetHasFixedSizeDetector : Detector(), XmlScanner, SourceCodeScanner {

    private val idExtractor = Regex("@\\+id/(.*)")

    // Synthetic access
    val recyclerViewIds = mutableListOf<String>()

    override fun appliesTo(folderType: ResourceFolderType): Boolean {
        // We are looking at layout files
        return folderType == ResourceFolderType.LAYOUT
    }

    override fun getApplicableElements() = listOf(RECYCLER_VIEW)

    override fun visitElement(context: XmlContext, element: Element) {
        val attributes = element.attributes
        val id = attributes.getNamedItemNS(ANDROID_URI, ATTR_ID)?.textContent
        val width = attributes.getNamedItemNS(ANDROID_URI, ATTR_LAYOUT_WIDTH)?.textContent
        val height = attributes.getNamedItemNS(ANDROID_URI, ATTR_LAYOUT_HEIGHT)?.textContent
        if (id != null) {
            if (height == "wrap_content" || width == "wrap_content") {
                val match = idExtractor.matchEntire(id)
                if (match != null) {
                    recyclerViewIds += match.groupValues[1]
                }
            }
        }
    }

    override fun getApplicableMethodNames() = listOf("setHasFixedSize")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator.isMemberInClass(method, RECYCLER_VIEW)) {
            val argumentValue = ConstantEvaluator.evaluate(context, node.valueArguments.first())
            if (argumentValue == true) {
                val location = context.getLocation(node)
                val sourceElement = node.receiver?.sourcePsi
                if (sourceElement != null) {
                    val elementTree = sourceElement.parentsWithSelf
                    var reported = false
                    val visitor = object : AbstractUastVisitor() {
                        override fun visitCallExpression(node: UCallExpression): Boolean {
                            if (node.methodName == "findViewById") {
                                val id = node.valueArguments.first()
                                val resource =
                                    ResourceEvaluator.getResource(context.evaluator, id)
                                if (resource?.name in recyclerViewIds) {
                                    reported = true
                                    context.report(
                                        issue = ISSUE,
                                        location = location,
                                        message = DESCRIPTION
                                    )
                                }
                            }
                            return true
                        }
                    }
                    for (element in elementTree) {
                        // Walk up the tree on the receivers source psi. That should have the
                        // definition of findViewById(...) we are looking for.
                        if (!reported) {
                            element.toUElement()?.accept(visitor)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val VIEW = "android.view.View"
        private const val RECYCLER_VIEW = "androidx.recyclerview.widget.RecyclerView"

        val DESCRIPTION = """
            When using `setHasFixedSize() in an `RecyclerView`, `wrap_content` cannot be used as \
            a value for `size` in the scrolling direction.
        """.trimIndent()

        val ISSUE = Issue.create(
            id = "InvalidSetHasFixedSize",
            briefDescription = DESCRIPTION,
            explanation = """
                When a RecyclerView uses `setHasFixedSize(...)` you cannot use `wrap_content` for \
                 size in the scrolling direction.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.FATAL,
            implementation = Implementation(
                InvalidSetHasFixedSizeDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.ALL_RESOURCE_FILES)
            )
        )
    }
}
