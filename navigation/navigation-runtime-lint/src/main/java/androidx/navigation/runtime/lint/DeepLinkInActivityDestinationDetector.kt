/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.navigation.runtime.lint

import com.android.SdkConstants.TAG_ACTIVITY
import com.android.SdkConstants.TAG_DEEP_LINK
import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import java.util.Collections
import org.w3c.dom.Element

/** Lint check for detecting use of <deeplink> inside of <activity>. */
class DeepLinkInActivityDestinationDetector : ResourceXmlDetector() {

    override fun appliesTo(folderType: ResourceFolderType): Boolean {
        return folderType == ResourceFolderType.NAVIGATION
    }

    override fun getApplicableElements(): Collection<String>? = Collections.singleton(TAG_DEEP_LINK)

    override fun visitElement(context: XmlContext, element: Element) {
        if (element.parentNode?.nodeName == TAG_ACTIVITY) {
            val incident =
                Incident(context)
                    .issue(DeepLinkInActivityDestination)
                    .location(context.getLocation(element))
                    .message(
                        "Do not attach a <deeplink> to an <activity> destination. " +
                            "Attach the deeplink directly to the second activity or the start " +
                            "destination of a nav host in the second activity instead."
                    )
            context.report(incident)
        }
    }

    companion object {
        val DeepLinkInActivityDestination =
            Issue.create(
                id = "DeepLinkInActivityDestination",
                briefDescription =
                    "A <deeplink> should not be attached to an <activity> destination",
                explanation =
                    """Attaching a <deeplink> to an <activity> destination will never give \
                the right behavior when using an implicit deep link on another app's task \
                (where the system back should immediately take the user back to the app that \
                triggered the deep link). Instead, attach the deep link directly to \
                the second activity (either by manually writing the appropriate <intent-filter> \
                or by adding the <deeplink> to the start destination of a nav host in that second \
                activity).""",
                category = Category.CORRECTNESS,
                severity = Severity.WARNING,
                implementation =
                    Implementation(
                        DeepLinkInActivityDestinationDetector::class.java,
                        Scope.RESOURCE_FILE_SCOPE
                    ),
                androidSpecific = true
            )
    }
}
