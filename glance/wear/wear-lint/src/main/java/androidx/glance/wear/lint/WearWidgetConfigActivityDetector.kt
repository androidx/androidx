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

import com.android.SdkConstants.ANDROID_MANIFEST_XML
import com.android.SdkConstants.ANDROID_URI
import com.android.SdkConstants.ATTR_NAME
import com.android.SdkConstants.PREFIX_RESOURCE_REF
import com.android.SdkConstants.TAG_ACTION
import com.android.SdkConstants.TAG_ACTIVITY
import com.android.SdkConstants.TAG_ACTIVITY_ALIAS
import com.android.SdkConstants.TAG_CATEGORY
import com.android.SdkConstants.TAG_INTENT_FILTER
import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.resources.ResourceRepository
import com.android.resources.ResourceFolderType
import com.android.resources.ResourceType
import com.android.resources.ResourceUrl
import com.android.tools.lint.client.api.ResourceRepositoryScope
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.android.utils.XmlUtils
import java.util.EnumSet
import org.w3c.dom.Element

/**
 * A [Detector] that validates that any configuration activity declared for a Wear Widget via
 * `configIntentAction` in `<wearwidget-provider>` XML includes the required
 * `com.google.android.clockwork.tiles.category.PROVIDER_CONFIG` category in its `<intent-filter>`.
 */
class WearWidgetConfigActivityDetector : Detector(), Detector.XmlScanner {

    private data class ActivityIntentFilterInfo(
        val actions: Set<String>,
        val hasProviderConfigCategory: Boolean,
        val location: Location,
        val isSuppressed: Boolean,
        val quickFix: LintFix?,
    )

    private val declaredConfigActions = mutableSetOf<String>()
    private val activityIntentFilters = mutableListOf<ActivityIntentFilterInfo>()

    override fun beforeCheckRootProject(context: Context) {
        declaredConfigActions.clear()
        activityIntentFilters.clear()
    }

    override fun appliesTo(folderType: ResourceFolderType): Boolean {
        return folderType == ResourceFolderType.XML
    }

    override fun getApplicableElements(): Collection<String> =
        listOf(TAG_WEARWIDGET_PROVIDER, TAG_INTENT_FILTER)

    override fun visitElement(context: XmlContext, element: Element) {
        when (element.tagName) {
            TAG_WEARWIDGET_PROVIDER -> visitWearWidgetProvider(context, element)
            TAG_INTENT_FILTER -> visitIntentFilter(context, element)
        }
    }

    private fun visitWearWidgetProvider(context: XmlContext, element: Element) {
        val rawAction =
            element.getAttribute(ATTR_CONFIG_INTENT_ACTION).ifEmpty {
                element.getAttributeNS(ANDROID_URI, ATTR_CONFIG_INTENT_ACTION)
            }
        resolveStringValue(context, rawAction)?.let(declaredConfigActions::add)
    }

    private fun visitIntentFilter(context: XmlContext, element: Element) {
        if (context.file.name != ANDROID_MANIFEST_XML) return
        val parentName = element.parentNode?.nodeName
        if (parentName != TAG_ACTIVITY && parentName != TAG_ACTIVITY_ALIAS) return

        val actions = mutableSetOf<String>()
        var hasProviderConfigCategory = false
        var lastChild: Element? = null

        for (child in XmlUtils.getSubTags(element)) {
            lastChild = child
            when (child.tagName) {
                TAG_ACTION -> {
                    resolveStringValue(context, child.getAttributeNS(ANDROID_URI, ATTR_NAME))
                        ?.let(actions::add)
                }
                TAG_CATEGORY -> {
                    if (child.getAttributeNS(ANDROID_URI, ATTR_NAME) == CATEGORY_PROVIDER_CONFIG) {
                        hasProviderConfigCategory = true
                    }
                }
            }
        }

        if (actions.isNotEmpty()) {
            val isSuppressed =
                context.driver.isSuppressed(
                    context,
                    MISSING_PROVIDER_CONFIG_CATEGORY_ISSUE,
                    element,
                )
            val quickFix =
                if (!hasProviderConfigCategory && !isSuppressed && lastChild != null) {
                    createAddCategoryFix(context, element, lastChild)
                } else {
                    null
                }
            activityIntentFilters.add(
                ActivityIntentFilterInfo(
                    actions = actions,
                    hasProviderConfigCategory = hasProviderConfigCategory,
                    location = context.getLocation(element),
                    isSuppressed = isSuppressed,
                    quickFix = quickFix,
                )
            )
        }
    }

    private fun createAddCategoryFix(
        context: XmlContext,
        intentFilter: Element,
        lastChild: Element,
    ): LintFix {
        val lastChildLocation = context.getLocation(lastChild)
        val indent = " ".repeat(lastChildLocation.start?.column ?: 0)
        val androidPrefix = intentFilter.lookupPrefix(ANDROID_URI) ?: "android"
        return fix()
            .name("Add PROVIDER_CONFIG category")
            .replace()
            .range(lastChildLocation)
            .end()
            .with(
                "\n$indent<$TAG_CATEGORY $androidPrefix:$ATTR_NAME=\"$CATEGORY_PROVIDER_CONFIG\" />"
            )
            .autoFix()
            .build()
    }

    override fun afterCheckRootProject(context: Context) {
        if (declaredConfigActions.isEmpty() || activityIntentFilters.isEmpty()) return

        val actionsMissingCategory =
            declaredConfigActions.filterTo(mutableSetOf()) { configAction ->
                activityIntentFilters.none {
                    configAction in it.actions && it.hasProviderConfigCategory
                }
            }
        if (actionsMissingCategory.isEmpty()) return

        for (filter in activityIntentFilters) {
            if (filter.isSuppressed) continue
            val matchedAction =
                filter.actions.firstOrNull { it in actionsMissingCategory } ?: continue
            context.report(
                issue = MISSING_PROVIDER_CONFIG_CATEGORY_ISSUE,
                location = filter.location,
                message =
                    "Configuration activity intent-filter for action '$matchedAction' must " +
                        "include '<category android:name=\"$CATEGORY_PROVIDER_CONFIG\" />'",
                quickfixData = filter.quickFix,
            )
        }
    }

    private fun resolveStringValue(context: XmlContext, rawValue: String): String? {
        if (rawValue.isBlank()) return null
        if (!rawValue.startsWith(PREFIX_RESOURCE_REF)) return rawValue

        var current = rawValue
        val visited = mutableSetOf<String>()
        var resourceRepository: ResourceRepository? = null

        while (visited.add(current)) {
            val resUrl = ResourceUrl.parse(current)
            if (resUrl == null || resUrl.type != ResourceType.STRING || resUrl.isFramework) {
                break
            }
            if (resourceRepository == null) {
                resourceRepository =
                    context.client.getResources(
                        context.project,
                        ResourceRepositoryScope.ALL_DEPENDENCIES,
                    )
            }
            val items =
                resourceRepository.getResources(
                    ResourceNamespace.RES_AUTO,
                    ResourceType.STRING,
                    resUrl.name,
                )
            current =
                items.firstOrNull { it.configuration.isDefault }?.resourceValue?.value
                    ?: items.firstOrNull()?.resourceValue?.value
                    ?: break
        }
        return current.takeIf { it.isNotBlank() }
    }

    companion object {
        private const val TAG_WEARWIDGET_PROVIDER = "wearwidget-provider"
        private const val ATTR_CONFIG_INTENT_ACTION = "configIntentAction"

        private const val CATEGORY_PROVIDER_CONFIG =
            "com.google.android.clockwork.tiles.category.PROVIDER_CONFIG"

        @JvmField
        val MISSING_PROVIDER_CONFIG_CATEGORY_ISSUE: Issue =
            Issue.create(
                id = "WearWidgetMissingProviderConfigCategory",
                briefDescription =
                    "Widget configuration activity must include the PROVIDER_CONFIG category",
                explanation =
                    """
                    When a Wear Widget declares a configuration activity via `configIntentAction` in `<wearwidget-provider>` XML, the Wear OS host launches it with an implicit Intent containing the `com.google.android.clockwork.tiles.category.PROVIDER_CONFIG` category.

                    The target `<activity>` or `<activity-alias>` must include `<category android:name="com.google.android.clockwork.tiles.category.PROVIDER_CONFIG" />` in its `<intent-filter>` so the configuration intent resolves at runtime.
                    """,
                category = Category.CORRECTNESS,
                priority = 6,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        WearWidgetConfigActivityDetector::class.java,
                        EnumSet.of(Scope.MANIFEST, Scope.RESOURCE_FILE),
                    ),
            )
    }
}
