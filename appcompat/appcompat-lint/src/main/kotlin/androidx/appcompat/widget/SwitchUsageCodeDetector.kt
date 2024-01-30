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

package androidx.appcompat.widget

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UVariable

@Suppress("UnstableApiUsage")
class SwitchUsageCodeDetector : Detector(), Detector.UastScanner {
    companion object {
        private const val USING_CORE_SWITCH_DESCRIPTION =
            "Use `SwitchCompat` from AppCompat or `MaterialSwitch` from Material library"

        internal val USING_CORE_SWITCH_CODE: Issue = Issue.create(
            "UseSwitchCompatOrMaterialCode",
            "Replace usage of `Switch` widget",
            USING_CORE_SWITCH_DESCRIPTION,
            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            Implementation(SwitchUsageCodeDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableUastTypes() = listOf<Class<out UElement>>(
        UVariable::class.java, UClass::class.java
    )

    override fun createUastHandler(context: JavaContext) = SwitchUsageUastHandler(context)

    class SwitchUsageUastHandler(private val context: JavaContext) : UElementHandler() {
        override fun visitClass(node: UClass) = node.uastSuperTypes.forEach {
            checkAndReport(it.type, it)
        }

        override fun visitVariable(node: UVariable) = checkAndReport(node.type, node)

        private fun checkAndReport(type: PsiType, node: UElement) {
            if (context.evaluator.typeMatches(type, "android.widget.Switch")) {
                context.report(
                    USING_CORE_SWITCH_CODE,
                    node,
                    context.getLocation(node),
                    USING_CORE_SWITCH_DESCRIPTION
                )
            }
        }
    }
}
