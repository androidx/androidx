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

import com.android.tools.lint.client.api.ResourceRepositoryScope
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Attr
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

        val resourceRepository =
            context.client.getResources(context.project, ResourceRepositoryScope.ALL_DEPENDENCIES)

        val foundTypes = mutableMapOf<ResolvedContainerType, Attr>()
        for (container in containers) {
            val hasPreviewImage = container.hasAttribute(ATTR_PREVIEW_IMAGE)

            if (!hasPreviewImage) {
                context.report(
                    XML_MISSING_PREVIEW_IMAGE_ISSUE,
                    container,
                    context.getLocation(container),
                    "This <container> tag is missing the 'previewImage' attribute",
                )
            }

            val typeAttrNode = container.getAttributeNode(ATTR_TYPE)
            if (typeAttrNode == null) {
                context.report(
                    XML_MISSING_CONTAINER_TYPE_ISSUE,
                    container,
                    context.getLocation(container),
                    "This <container> tag is missing the 'type' attribute",
                )
            } else {
                val typeAttrValue = typeAttrNode.value
                val resolvedType =
                    WearWidgetContainerTypeResolver.resolve(resourceRepository, typeAttrValue)
                when (resolvedType) {
                    is ResolvedContainerType.TileCompat -> {
                        context.report(
                            XML_UNSUPPORTED_CONTAINER_TYPE_ISSUE,
                            typeAttrNode,
                            context.getLocation(typeAttrNode),
                            "Tile compat container type is not supported in widget's metadata.",
                        )
                    }
                    is ResolvedContainerType.Unrecognized -> {
                        context.report(
                            XML_UNRECOGNIZED_CONTAINER_TYPE_ISSUE,
                            typeAttrNode,
                            context.getLocation(typeAttrNode),
                            "Unrecognized container type '$typeAttrValue'.",
                        )
                    }
                    is ResolvedContainerType.Large,
                    is ResolvedContainerType.Small -> {}
                }

                val existingAttrNode = foundTypes[resolvedType]
                if (existingAttrNode != null) {
                    val location =
                        context.getLocation(typeAttrNode).apply {
                            secondary =
                                context.getLocation(existingAttrNode).apply {
                                    message = "Previously defined here"
                                }
                        }

                    context.report(
                        XML_DUPLICATE_CONTAINER_TYPE_ISSUE,
                        typeAttrNode,
                        location,
                        "Duplicate container types are not allowed. Type '$typeAttrValue' is duplicated.",
                    )
                } else {
                    foundTypes[resolvedType] = typeAttrNode
                }
            }
        }
    }

    companion object {
        private const val TAG_CONTAINER = "container"
        private const val ATTR_PREVIEW_IMAGE = "previewImage"
        private const val ATTR_TYPE = "type"

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

        @JvmField
        val XML_MISSING_CONTAINER_TYPE_ISSUE: Issue =
            Issue.create(
                id = "WearWidgetMissingContainerType",
                briefDescription = "Wear widget container must specify type",
                explanation =
                    """
                    Each <container> tag within a <wearwidget-provider> configuration must explicitly define the type attribute.
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
        val XML_DUPLICATE_CONTAINER_TYPE_ISSUE: Issue =
            Issue.create(
                id = "WearWidgetDuplicateContainerType",
                briefDescription = "Wear widget containers cannot have duplicate type attributes",
                explanation =
                    """
                    Each <container> tag within a <wearwidget-provider> configuration must have a unique type attribute. Duplicate container types are not supported.
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
        val XML_UNSUPPORTED_CONTAINER_TYPE_ISSUE: Issue =
            Issue.create(
                id = "WearWidgetUnsupportedContainerType",
                briefDescription =
                    "Tile compat container type is not supported in widget's metadata",
                explanation =
                    """
                    Tile compat container type is not supported for widget's metadata.
                    While Tile compat is supported, it should be configured via the BIND_TILE_PROVIDER action instead.
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
        val XML_UNRECOGNIZED_CONTAINER_TYPE_ISSUE: Issue =
            Issue.create(
                id = "WearWidgetUnrecognizedContainerType",
                briefDescription = "Unrecognized container type",
                explanation =
                    """
                    Each <container> tag within a <wearwidget-provider> configuration must define a valid container type.
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.WARNING,
                implementation =
                    Implementation(
                        WearWidgetProviderXmlDetector::class.java,
                        Scope.RESOURCE_FILE_SCOPE,
                    ),
            )
    }
}
