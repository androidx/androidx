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

package androidx.fragment.lint

import com.android.SdkConstants.VIEW_FRAGMENT
import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Element
import java.util.Collections

/**
 * Lint check for detecting use of fragment tag in layout xml files. This provides a warning that
 * recommends using [FragmentContainerView] instead.
 */
class FragmentTagDetector : ResourceXmlDetector() {

    companion object {
        val ISSUE = Issue.create(
            id = "FragmentTagUsage",
            briefDescription = "Use FragmentContainerView instead of the <fragment> tag",
            explanation = """FragmentContainerView replaces the <fragment> tag as the preferred \
                way of adding fragments via XML. Unlike the <fragment> tag, FragmentContainerView \
                uses a normal `FragmentTransaction` under the hood to add the initial fragment, \
                allowing further FragmentTransaction operations on the FragmentContainerView \
                and providing a consistent timing for lifecycle events.""",
            category = Category.CORRECTNESS,
            severity = Severity.WARNING,
            implementation = Implementation(
                FragmentTagDetector::class.java, Scope.RESOURCE_FILE_SCOPE
            ),
            androidSpecific = true
        ).addMoreInfo(
            "https://developer.android.com" +
                    "/reference/androidx/fragment/app/FragmentContainerView.html"
        )
    }

    override fun appliesTo(folderType: ResourceFolderType): Boolean {
        return folderType == ResourceFolderType.LAYOUT
    }

    override fun getApplicableElements(): Collection<String>? = Collections.singleton(VIEW_FRAGMENT)

    override fun visitElement(context: XmlContext, element: Element) {
        context.report(ISSUE, context.getNameLocation(element),
            "Replace the <fragment> tag with FragmentContainerView.",
            LintFix.create()
                .replace()
                .text(VIEW_FRAGMENT)
                .with("androidx.fragment.app.FragmentContainerView").build())
    }
}
