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

package androidx.appcompat.res

import com.android.SdkConstants
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import org.w3c.dom.Element

@Suppress("UnstableApiUsage")
class ColorStateListAlphaDetector : Detector(), XmlScanner {
    companion object {
        internal val NOT_USING_ANDROID_ALPHA: Issue = Issue.create(
            "UseAndroidAlpha",
            "`android:alpha` attribute missing on `ColorStateList`",
            "`ColorStateList` uses app:alpha without `android:alpha`",
            Category.CORRECTNESS,
            1,
            Severity.ERROR,
            Implementation(ColorStateListAlphaDetector::class.java, Scope.RESOURCE_FILE_SCOPE)
        )
    }

    override fun getApplicableElements(): Collection<String>? = listOf("selector")

    override fun visitElement(context: XmlContext, element: Element) {
        val items = element.getElementsByTagName("item")
        for (index in 0 until items.length) {
            val item = items.item(index) as Element
            // Only look at items that have android:color
            if (!item.hasAttributeNS(SdkConstants.ANDROID_URI, "color")) {
                continue
            }
            val hasAppAlphaAttr = item.hasAttributeNS(SdkConstants.AUTO_URI, "alpha")
            val hasAndroidAlphaAttr = item.hasAttributeNS(SdkConstants.ANDROID_URI, "alpha")
            if (hasAppAlphaAttr && !hasAndroidAlphaAttr) {
                context.report(
                    NOT_USING_ANDROID_ALPHA,
                    item,
                    context.getLocation(item.getAttributeNodeNS(SdkConstants.AUTO_URI, "alpha")),
                    "Must use `android:alpha` if `app:alpha` is used",
                    LintFix.create().set(
                        SdkConstants.ANDROID_URI, "alpha",
                        item.getAttributeNS(SdkConstants.AUTO_URI, "alpha")
                    ).build()
                )
            }
        }
    }
}
