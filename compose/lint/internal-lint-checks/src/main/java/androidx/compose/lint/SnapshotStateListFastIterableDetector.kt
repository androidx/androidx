/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression

/**
 * Lint [Detector] to prevent calling `fast*` methods on a `SnapshotStateList`. Instead, `toList()`
 * should be called before `fast*` methods (such as `toList().fastForEach`).
 *
 * Calling `fast*` methods on a `SnapshotStateList` causes two state reads per entry (one for list
 * size and one for element access via `get(index)`), whereas calling `toList()` first performs only
 * one state read for the whole snapshot.
 */
class SnapshotStateListFastIterableDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val methodName = node.methodName ?: return
                if (!methodName.startsWith("fast") || methodName == "fast") return
                val receiverType = node.receiverType ?: return
                if (!receiverType.inheritsFrom(Names.Runtime.Snapshots.SnapshotStateList)) return
                val resolved = node.resolve() ?: return
                val packageName = context.evaluator.getPackage(resolved)?.qualifiedName ?: return
                if (packageName != Names.Ui.Util.PackageName.javaPackageName) return
                val replacement = "toList().$methodName"
                val fix =
                    LintFix.create()
                        .replace()
                        .name("Replace with $replacement")
                        .text(methodName)
                        .with(replacement)
                        .build()
                context.report(
                    ISSUE,
                    node,
                    context.getNameLocation(node),
                    "Replace $methodName with $replacement on SnapshotStateList " +
                        "($methodName performs two state reads per entry while " +
                        "$replacement performs only one for the whole loop)",
                    fix,
                )
            }
        }

    companion object {
        val ISSUE =
            Issue.create(
                id = "SnapshotStateListFastIterable",
                briefDescription = "Calling fast methods on SnapshotStateList is inefficient",
                explanation =
                    "Calling fast* methods directly on SnapshotStateList causes two state reads " +
                        "per entry (one for list size and one for element access via get(index)). " +
                        "Call toList().fast* instead to perform only a single state read before iterating.",
                category = Category.PERFORMANCE,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        SnapshotStateListFastIterableDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE),
                    ),
            )
    }
}
