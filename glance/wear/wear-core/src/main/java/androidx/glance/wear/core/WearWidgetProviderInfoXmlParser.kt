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

package androidx.glance.wear.core

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.os.Build
import android.text.TextUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

internal object WearWidgetProviderInfoXmlParser {
    private const val TAG_PROVIDER = "wearwidget-provider"
    private const val TAG_CONTAINER = "container"
    private const val ATTR_LABEL = "label"
    private const val ATTR_DESCRIPTION = "description"
    private const val ATTR_ICON = "icon"
    private const val ATTR_PREFERRED_TYPE = "preferredType"
    private const val ATTR_GROUP = "group"
    private const val ATTR_CONFIG_INTENT_ACTION = "configIntentAction"
    private const val ATTR_TYPE = "type"
    private const val ATTR_PREVIEW_IMAGE = "previewImage"

    private val NAMESPACE_DISABLED: String? = null

    private const val MAX_SAFE_LABEL_LENGTH = 1000
    private const val DEFAULT_MAX_LABEL_SIZE_PX = 1000f

    @Throws(XmlPullParserException::class)
    internal fun XmlResourceParser.parseWearWidgetProviderInfo(
        resources: Resources,
        packageManager: PackageManager,
        providerService: ComponentName,
        serviceInfo: ServiceInfo,
        @ContainerInfo.ContainerType defaultPreferredContainerType: Int,
        defaultGroup: String,
    ): WearWidgetProviderInfo {
        while (this.next() != XmlPullParser.END_DOCUMENT) {
            if (this.eventType == XmlPullParser.START_TAG && this.name == TAG_PROVIDER) {
                return this.parseWearWidgetProviderTag(
                    resources,
                    packageManager,
                    providerService,
                    serviceInfo,
                    defaultPreferredContainerType,
                    defaultGroup,
                )
            }
        }
        throw XmlPullParserException("No <$TAG_PROVIDER> tag found in XML")
    }

    @Throws(XmlPullParserException::class)
    internal fun XmlResourceParser.parseWearWidgetProviderTag(
        resources: Resources,
        packageManager: PackageManager,
        providerService: ComponentName,
        serviceInfo: ServiceInfo,
        @ContainerInfo.ContainerType defaultPreferredContainerType: Int,
        defaultGroup: String,
    ): WearWidgetProviderInfo {
        val label =
            loadSafeText(resources, ATTR_LABEL)?.toString()
                ?: serviceInfo.loadLabel(packageManager)?.toString()
                ?: ""
        val description =
            loadSafeText(resources, ATTR_DESCRIPTION)?.toString()
                ?: loadTextResource(resources, serviceInfo.descriptionRes)?.toString()
                ?: ""
        val icon =
            getAttributeResourceValue(NAMESPACE_DISABLED, ATTR_ICON, Resources.ID_NULL).takeIf {
                it != Resources.ID_NULL
            }
                ?: serviceInfo.icon.takeIf { it != Resources.ID_NULL }
                ?: serviceInfo.applicationInfo.icon
        var preferredContainerType =
            parseContainerTypeAttr(resources, ATTR_PREFERRED_TYPE, defaultPreferredContainerType)
        val group = getAttributeValue(NAMESPACE_DISABLED, ATTR_GROUP) ?: defaultGroup
        val configIntentAction = getAttributeValue(NAMESPACE_DISABLED, ATTR_CONFIG_INTENT_ACTION)
        val unrecognisedAttributes = mutableMapOf<String, String>()
        val knownAttributes =
            setOf(
                ATTR_LABEL,
                ATTR_DESCRIPTION,
                ATTR_ICON,
                ATTR_PREFERRED_TYPE,
                ATTR_GROUP,
                ATTR_CONFIG_INTENT_ACTION,
            )
        for (i in 0 until attributeCount) {
            val attrName = getAttributeName(i)
            if (attrName !in knownAttributes) {
                unrecognisedAttributes[attrName.lowercase()] = getAttributeValue(i)
            }
        }

        val containers = mutableListOf<ContainerInfo>()
        val providerDepth = depth
        while (next() != XmlPullParser.END_TAG || depth > providerDepth) {
            if (eventType != XmlPullParser.START_TAG) {
                continue
            }
            if (name == TAG_CONTAINER) {
                containers.add(parseContainerInfo(resources))
            }
        }

        if (containers.none { it.type == preferredContainerType }) {
            val firstType = containers.firstOrNull()?.type
            if (firstType != null) {
                preferredContainerType = firstType
            }
        }

        return WearWidgetProviderInfo(
            providerService = providerService,
            label = label,
            description = description,
            icon = icon,
            containers = containers,
            preferredContainerType = preferredContainerType,
            group = group,
            configIntentAction = configIntentAction,
            unrecognisedAttributes = unrecognisedAttributes,
        )
    }

    private fun XmlResourceParser.parseContainerInfo(resources: Resources): ContainerInfo {
        val type = parseContainerTypeAttr(resources, ATTR_TYPE)
        val previewImage =
            getAttributeResourceValue(NAMESPACE_DISABLED, ATTR_PREVIEW_IMAGE, Resources.ID_NULL)
        val label = loadSafeText(resources, ATTR_LABEL)?.toString()
        val description = loadSafeText(resources, ATTR_DESCRIPTION)?.toString()
        return ContainerInfo(
            type = type,
            previewImage = previewImage,
            label = label,
            description = description,
        )
    }

    @Throws(XmlPullParserException::class)
    private fun XmlResourceParser.parseContainerTypeAttr(
        resources: Resources,
        attrName: String,
        @ContainerInfo.ContainerType defaultValue: Int? = null,
    ): Int {
        val attrResId = getAttributeResourceValue(NAMESPACE_DISABLED, attrName, Resources.ID_NULL)
        val type =
            if (attrResId != Resources.ID_NULL) {
                try {
                    resources.getInteger(attrResId)
                } catch (e: Resources.NotFoundException) {
                    throw XmlPullParserException(
                        "Invalid container type resource. Resource must have an integer value.",
                        this,
                        e,
                    )
                }
            } else {
                // Parse String as either one of the types or an integer.
                val attrValue = getAttributeValue(NAMESPACE_DISABLED, attrName)
                if (attrValue != null) {
                    ContainerInfo.Companion.containerTypeFromString(attrValue)
                        ?: attrValue.toIntOrNull()
                } else {
                    defaultValue
                }
            } ?: throw XmlPullParserException("Failed to parse Container Type for $attrName")
        if (type == ContainerInfo.Companion.CONTAINER_TYPE_FULLSCREEN) {
            throw XmlPullParserException("Fullscreen container type is not supported for widgets")
        }
        return type
    }

    /**
     * Load attribute as string or string resource and clean string from bad characters. See
     * [android.content.pm.PackageItemInfo.loadSafeLabel].
     */
    private fun XmlResourceParser.loadSafeText(
        resources: Resources,
        attrName: String,
    ): CharSequence? {
        val label = loadTextAttr(resources, attrName) ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return TextUtils.makeSafeForPresentation(
                label.toString(),
                MAX_SAFE_LABEL_LENGTH,
                DEFAULT_MAX_LABEL_SIZE_PX,
                TextUtils.SAFE_STRING_FLAG_TRIM or TextUtils.SAFE_STRING_FLAG_FIRST_LINE,
            )
        }
        return label
    }

    private fun XmlResourceParser.loadTextAttr(
        resources: Resources,
        attrName: String,
    ): CharSequence? {
        val resourceId = getAttributeResourceValue(NAMESPACE_DISABLED, attrName, Resources.ID_NULL)
        return loadTextResource(resources, resourceId, attrName = attrName)
            ?: getAttributeValue(NAMESPACE_DISABLED, attrName)
    }

    private fun loadTextResource(
        resources: Resources,
        resId: Int,
        attrName: String? = null,
    ): CharSequence? {
        if (resId == Resources.ID_NULL) {
            return null
        }
        try {
            return resources.getText(resId)
        } catch (e: Resources.NotFoundException) {
            throw XmlPullParserException("Invalid resource for attr $attrName")
        }
    }
}
