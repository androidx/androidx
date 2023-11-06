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

import com.android.SdkConstants
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LayoutDetector
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Element

@Suppress("UnstableApiUsage")
class TextViewCompoundDrawablesXmlDetector : LayoutDetector() {
    companion object {
        internal val ATTRS_MAP = mapOf(
            "drawableLeft" to "drawableLeftCompat",
            "drawableRight" to "drawableRightCompat",
            "drawableTop" to "drawableTopCompat",
            "drawableBottom" to "drawableBottomCompat",
            "drawableStart" to "drawableStartCompat",
            "drawableEnd" to "drawableEndCompat",
            "drawableTint" to "drawableTint",
            "drawableTintMode" to "drawableTintMode"
        )

        internal val NOT_USING_COMPAT_TEXT_VIEW_DRAWABLE_ATTRS: Issue = Issue.create(
            "UseCompatTextViewDrawableXml",
            "Compat compound drawable attributes should be used on `TextView`",
            "`TextView` uses `android:` compound drawable attributes instead of `app:` ones",
            Category.CORRECTNESS,
            1,
            Severity.WARNING,
            Implementation(
                TextViewCompoundDrawablesXmlDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )
    }

    override fun getApplicableElements(): Collection<String>? = listOf("TextView")

    override fun visitElement(context: XmlContext, element: Element) {
        // Go over each one of the attribute entries in the map
        for ((from, to) in ATTRS_MAP) {
            if (!element.hasAttributeNS(SdkConstants.ANDROID_URI, from)) {
                // This element doesn't have the "from" attribute in the android namespace
                continue
            }
            // The suggested fix is a composite of setting the "to" attribute in the app namespace
            // to the existing value of "from" attribute in the android namespace, and unsetting
            // that "from" attribute
            context.report(
                NOT_USING_COMPAT_TEXT_VIEW_DRAWABLE_ATTRS,
                element,
                context.getLocation(element.getAttributeNodeNS(SdkConstants.ANDROID_URI, from)),
                "Use `app:$to` instead of `android:$from`",
                LintFix.create().composite(
                    LintFix.create().set(
                        SdkConstants.AUTO_URI, to,
                        element.getAttributeNS(SdkConstants.ANDROID_URI, from)
                    ).build(),
                    LintFix.create().unset(SdkConstants.ANDROID_URI, from).build()
                )
            )
        }
    }
}
