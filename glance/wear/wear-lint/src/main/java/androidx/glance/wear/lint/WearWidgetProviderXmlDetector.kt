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

package androidx.glance.wear.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Element

/** A [Detector] that validates '<wearwidget-provider>' XML configuration files. */
class WearWidgetProviderXmlDetector : Detector(), Detector.XmlScanner {

    override fun getApplicableElements(): Collection<String> = listOf("wearwidget-provider")

    override fun visitElement(context: XmlContext, element: Element) {
        val containers =
            (0 until element.childNodes.length).mapNotNull { i ->
                (element.childNodes.item(i) as? Element)?.takeIf { it.localName == TAG_CONTAINER }
            }

        if (containers.isEmpty()) {
            context.report(
                XML_MISSING_CONTAINER_ISSUE,
                element,
                context.getLocation(element),
                "Wear widget provider info must include at least one <container> tag",
            )
            return
        }

        for (container in containers) {
            val hasPreviewImage = container.hasAttribute(ATTR_PREVIEW_IMAGE)

            if (!hasPreviewImage) {
                context.report(
                    XML_MISSING_PREVIEW_IMAGE_ISSUE,
                    container,
                    context.getLocation(container),
                    "This <container> tag is missing the previewImage attribute",
                )
            }

            // TODO: b/464458215 - Enforce the type attribute. For each <container> node, verify
            // that the type attribute exists and not duplicated.

            // TODO: b/464458215 - Validate container type values. Ensure the configuration must not
            // use CONTAINER_TYPE_TILE_COMPAT. Also warn when an unrecognizable type is assigned.
        }
    }

    companion object {
        private const val TAG_CONTAINER = "container"
        private const val ATTR_PREVIEW_IMAGE = "previewImage"
        @JvmField
        val XML_MISSING_CONTAINER_ISSUE: Issue =
            Issue.create(
                id = "WearWidgetMissingContainer",
                briefDescription =
                    "Wear widget provider info must include at least one <container> tag",
                explanation =
                    """
                    A <wearwidget-provider> configuration file must include at least one <container> tag to allow system discovery.
                    """,
                category = Category.CORRECTNESS,
                priority = 6,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        WearWidgetProviderXmlDetector::class.java,
                        Scope.RESOURCE_FILE_SCOPE,
                    ),
            )

        @JvmField
        val XML_MISSING_PREVIEW_IMAGE_ISSUE: Issue =
            Issue.create(
                id = "WearWidgetMissingPreviewImage",
                briefDescription = "Wear widget container must specify previewImage",
                explanation =
                    """
                    Each <container> tag within a <wearwidget-provider> configuration must explicitly define the previewImage attribute to provide a preview of the widget.
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        WearWidgetProviderXmlDetector::class.java,
                        Scope.RESOURCE_FILE_SCOPE,
                    ),
            )
    }
}
