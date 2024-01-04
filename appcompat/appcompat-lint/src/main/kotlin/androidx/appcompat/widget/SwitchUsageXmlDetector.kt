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

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LayoutDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Element

@Suppress("UnstableApiUsage")
class SwitchUsageXmlDetector : LayoutDetector() {
    companion object {
        internal val USING_CORE_SWITCH_XML: Issue = Issue.create(
            "UseSwitchCompatOrMaterialXml",
            "Replace usage of `Switch` widget",
            "Use `SwitchCompat` from AppCompat or `MaterialSwitch` from Material library",
            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            Implementation(SwitchUsageXmlDetector::class.java, Scope.RESOURCE_FILE_SCOPE)
        )
    }

    override fun getApplicableElements(): Collection<String>? = listOf("Switch")

    override fun visitElement(context: XmlContext, element: Element) {
        context.report(
            USING_CORE_SWITCH_XML,
            element,
            context.getLocation(element),
            "Use `SwitchCompat` from AppCompat or `MaterialSwitch` from Material library"
        )
    }
}
