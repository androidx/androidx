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
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.glance.wear.core.WearWidgetProviderInfoXmlParser.parseWearWidgetProviderInfo
import java.util.Objects
import org.xmlpull.v1.XmlPullParserException

/**
 * Describes the metadata for a Wear Widget provider.
 *
 * The fields in this class correspond to the fields in the `<wearwidget-provider>` xml tag.
 *
 * For example, the `AndroidManifest.xml` would contain:
 * ```xml
 * <service
 *     android:name=".MyWidgetService"
 *     android:exported="true">
 *     <meta-data
 *         android:name="androidx.glance.wear.widget.provider"
 *         android:resource="@xml/my_widget_info" />
 * </service>
 * ```
 *
 * And `res/xml/my_widget_info.xml` would contain:
 * ```xml
 * <wearwidget-provider
 *     label="@string/widget_label"
 *     description="@string/widget_description"
 *     icon="@drawable/widget_icon"
 *     preferredType="@integer/glance_wear_container_type_small" >
 *
 *     <container
 *         type="@integer/glance_wear_container_type_small"
 *         previewImage="@drawable/small_preview"/>
 *
 *     <container
 *         type="@integer/glance_wear_container_type_large"
 *         previewImage="@drawable/large_preview"
 *         label="@string/large_container_label_override" />
 *
 * </wearwidget-provider>
 * ```
 *
 * For container types, see [ContainerInfo.ContainerType]. In XML metadata for your widgets, these
 * should be referenced by using the public integer resources
 * `@integer/glance_wear_container_type_large` and `@integer/glance_wear_container_type_small`.
 * These correspond to the integer constants [ContainerInfo.CONTAINER_TYPE_LARGE] and
 * [ContainerInfo.CONTAINER_TYPE_SMALL].
 *
 * @property providerService The [ComponentName] of this widget provider.
 * @property label The label of this widget.
 * @property description The description of this widget.
 * @property icon The resource id of the icon for this widget.
 * @property containers The list of [ContainerInfo] supported for this widget provider. Each element
 *   represents a supported type.
 * @property preferredContainerType The preferred Container Type to use for a widget instance when
 *   type is not specified when adding a widget. This can be used when this provider is replacing a
 *   legacy Wear Tile provider. See [ContainerInfo].
 * @property group The name of the group this widget provider is associated with. Widget providers
 *   in the same group represent the same widget on the device. Only one provider service should be
 *   enabled at a time for a given group. This can be used to replace which provider service is
 *   associated with a widget on the device.
 *
 * Defaults to the fully qualified name of the provider service.
 *
 * This attribute is only used on devices on API version 37 and above. For backwards compatibility
 * with services being used on older devices, the default value of the fully qualified name of the
 * older service should be used.
 *
 * @property configIntentAction The intent action to launch an activity for configuring the widget.
 *   This can be null if no configuration is needed.
 * @property unrecognisedAttributes Any unrecognised attributes during the XML parsing of the
 *   provider info.
 */
public class WearWidgetProviderInfo
@RestrictTo(LIBRARY)
@Throws(XmlPullParserException::class)
public constructor(
    public val providerService: ComponentName,
    public val label: String,
    public val description: String,
    @DrawableRes public val icon: Int,
    public val containers: List<ContainerInfo>,
    @ContainerInfo.ContainerType public val preferredContainerType: Int,
    public val group: String = providerService.className,
    public val configIntentAction: String? = null,
    public val unrecognisedAttributes: Map<String, String> = emptyMap(),
) {
    init {
        // Validate the object and throw [XmlPullParserException] if it's invalid.
        if (containers.isEmpty()) {
            throw XmlPullParserException("At least one container must be defined")
        }

        val supportedContainerTypes = containers.map { it.type }
        val duplicateTypes =
            supportedContainerTypes.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        if (duplicateTypes.isNotEmpty()) {
            throw XmlPullParserException(
                "Cannot have multiple containers with the same type: ${duplicateTypes.joinToString() }}"
            )
        }
    }

    public companion object {
        /** Name for the `meta-data` tag for the provider info. */
        @RestrictTo(LIBRARY_GROUP)
        public const val META_DATA_WEAR_WIDGET_PROVIDER: String =
            "androidx.glance.wear.widget.provider"

        /** Intent action for binding to a Widget Service. */
        @RestrictTo(LIBRARY_GROUP)
        public const val ACTION_BIND_WIDGET_PROVIDER: String =
            "androidx.glance.wear.action.BIND_WIDGET_PROVIDER"

        /**
         * Intent identifier to signal support for [androidx.glance.wear.parcel.IWearWidgetProvider]
         * interface.
         */
        @RestrictTo(LIBRARY_GROUP)
        public const val WEAR_WIDGET_PROVIDER_SUPPORTED_IDENTIFIER: String =
            "androidx.glance.wear.WEAR_WIDGET_PROVIDER_SUPPORTED_IDENTIFIER"

        /**
         * Parses a [WearWidgetProviderInfo] from the metadata of a service.
         *
         * The metadata with name `androidx.glance.wear.widget.provider` should reference an XML
         * resource with the provider info.
         *
         * If not present, the values for label, description and icon are taken from the <service>
         * attributes.
         *
         * @param context The [Context] to use for resolving resources and package manager.
         * @param providerService The [ComponentName] of the widget provider service.
         * @throws [PackageManager.NameNotFoundException] if the meta-data resource is not found or
         *   the provider service is not found.
         * @throws [XmlPullParserException] if there is an error parsing the XML resource or if this
         *   is not a valid provider info, for example:
         *     - No `<container>` tags are defined.
         *     - Multiple `<container>` tags have the same `type`.
         *     - Invalid resource IDs.
         *     - Using `CONTAINER_TYPE_FULLSCREEN` which is not supported for widgets.
         */
        @Throws(PackageManager.NameNotFoundException::class, XmlPullParserException::class)
        @JvmStatic
        public fun parseFromService(
            context: Context,
            providerService: ComponentName,
        ): WearWidgetProviderInfo {
            val pm = context.packageManager
            val serviceInfo = pm.getServiceInfo(providerService, PackageManager.GET_META_DATA)
            val providerResources = pm.getResourcesForApplication(serviceInfo.applicationInfo)
            val xmlParser =
                try {
                    serviceInfo.loadXmlMetaData(pm, META_DATA_WEAR_WIDGET_PROVIDER)
                } catch (e: java.lang.ClassCastException) {
                    throw PackageManager.NameNotFoundException(
                        "Invalid meta-data value for $META_DATA_WEAR_WIDGET_PROVIDER for service $providerService. Meta-data should reference an xml resource."
                    )
                }

            if (xmlParser == null) {
                throw PackageManager.NameNotFoundException(
                    "Invalid meta-data name $META_DATA_WEAR_WIDGET_PROVIDER for service $providerService"
                )
            }
            return xmlParser.parseWearWidgetProviderInfo(
                providerResources,
                pm,
                providerService,
                serviceInfo,
                defaultPreferredContainerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                defaultGroup = providerService.className,
            )
        }
    }
}

/**
 * Describes a container supported by a widget provider.
 *
 * A container is one representation of a widget, with a given size and shape.
 *
 * The fields in this class correspond to the fields in the `<container>` xml tag.
 *
 * For example:
 * ```xml
 * <container
 *     android:type="@integer/glance_wear_container_type_large"
 *     android:previewImage="@drawable/large_preview"
 *     android:label="@string/large_label" />
 * ```
 *
 * @property type The type of this widget container. This can be one of
 *   [ContainerInfo.CONTAINER_TYPE_LARGE] or [ContainerInfo.CONTAINER_TYPE_SMALL].
 * @property previewImage The resource id of the preview image for this widget container.
 * @property label The override label for this widget container.
 * @property description The override description for this widget container.
 */
public class ContainerInfo
@RestrictTo(LIBRARY)
public constructor(
    @ContainerType public val type: Int,
    @DrawableRes public val previewImage: Int,
    public val label: String? = null,
    public val description: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContainerInfo) return false
        return type == other.type &&
            previewImage == other.previewImage &&
            label == other.label &&
            description == other.description
    }

    override fun hashCode(): Int {
        return Objects.hash(type, previewImage, label, description)
    }

    /** The container type of a widget. It defines the size and shape of the container. */
    @IntDef(CONTAINER_TYPE_FULLSCREEN, CONTAINER_TYPE_LARGE, CONTAINER_TYPE_SMALL)
    @RestrictTo(LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    public annotation class ContainerType

    public companion object {
        /** Represents a fullscreen widget container, equivalent to a Wear Tile. */
        public const val CONTAINER_TYPE_FULLSCREEN: Int = 0

        /**
         * Represents a large widget container. Support for this container type is device dependent.
         */
        public const val CONTAINER_TYPE_LARGE: Int = 1
        /**
         * Represents a small widget container. Support for this container type is device dependent.
         */
        public const val CONTAINER_TYPE_SMALL: Int = 2

        private const val STRING_CONTAINER_TYPE_FULLSCREEN = "FULLSCREEN"
        private const val STRING_CONTAINER_TYPE_LARGE = "LARGE"
        private const val STRING_CONTAINER_TYPE_SMALL = "SMALL"

        @RestrictTo(LIBRARY_GROUP)
        @JvmStatic
        public fun containerTypeToString(@ContainerType containerType: Int): String =
            when (containerType) {
                CONTAINER_TYPE_FULLSCREEN -> STRING_CONTAINER_TYPE_FULLSCREEN
                CONTAINER_TYPE_SMALL -> STRING_CONTAINER_TYPE_SMALL
                CONTAINER_TYPE_LARGE -> STRING_CONTAINER_TYPE_LARGE
                else -> containerType.toString()
            }

        @RestrictTo(LIBRARY_GROUP)
        @JvmStatic
        @ContainerType
        public fun containerTypeFromString(input: String): Int? =
            when (input.uppercase()) {
                STRING_CONTAINER_TYPE_FULLSCREEN -> CONTAINER_TYPE_FULLSCREEN
                STRING_CONTAINER_TYPE_SMALL -> CONTAINER_TYPE_SMALL
                STRING_CONTAINER_TYPE_LARGE -> CONTAINER_TYPE_LARGE
                else -> null
            }
    }
}
