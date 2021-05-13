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

package androidx.appcompat.view

import androidx.appcompat.getMinSdk
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LayoutDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Attr

@Suppress("UnstableApiUsage")
class OnClickXmlDetector : LayoutDetector() {
    companion object {
        internal val USING_ON_CLICK_IN_XML: Issue = Issue.create(
            "UsingOnClickInXml",
            "Using `android:onClick` on older version of the platform is broken",
            "Old versions of the platform do not properly support resolving `android:onClick`",
            Category.CORRECTNESS,
            1,
            Severity.WARNING,
            Implementation(OnClickXmlDetector::class.java, Scope.RESOURCE_FILE_SCOPE)
        )
    }

    override fun getApplicableAttributes(): Collection<String>? = listOf("onClick")

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        val onClickValue = attribute.value ?: return
        if (onClickValue.startsWith("@{")) {
            // This comes from data binding and is safe on all supported platform
            // versions
            return
        }
        if (context.getMinSdk() < 23) {
            // The resolution is not guaranteed to work on pre-23 versions of the platform
            context.report(
                USING_ON_CLICK_IN_XML,
                attribute,
                context.getLocation(attribute),
                "Use databinding or explicit wiring of click listener in code"
            )
        }
    }
}
