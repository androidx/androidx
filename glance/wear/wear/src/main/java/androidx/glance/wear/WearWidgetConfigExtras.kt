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

package androidx.glance.wear

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.glance.wear.core.WidgetInstanceId
import java.util.Objects

/**
 * Holds the configuration parameters passed to a Wear Widget configuration activity via its
 * launching [Intent].
 *
 * When the Wear OS host launches a widget configuration activity (specified via
 * `configIntentAction` in the `<wearwidget-provider>` XML metadata and matching
 * [CATEGORY_PROVIDER_CONFIG]), it includes the target widget instance ID and provider service
 * [ComponentName] in the launching intent's extras. Use [fromIntent] to extract these values in
 * your configuration activity.
 *
 * @property instanceId The [WidgetInstanceId] of the widget instance being configured.
 * @property provider The [ComponentName] of the [GlanceWearWidgetService] providing the widget.
 */
public class WearWidgetConfigExtras(
    public val instanceId: WidgetInstanceId,
    public val provider: ComponentName,
) {

    public override fun equals(other: Any?): Boolean =
        when {
            this === other -> true
            other !is WearWidgetConfigExtras -> false
            else -> instanceId == other.instanceId && provider == other.provider
        }

    public override fun hashCode(): Int = Objects.hash(instanceId, provider)

    public override fun toString(): String =
        "WearWidgetConfigExtras(instanceId=$instanceId, provider=$provider)"

    public companion object {

        /**
         * Category for widget provider configuration activities.
         *
         * The configuration activity launched for configuring a widget provider must specify this
         * category in its intent filter.
         *
         * For example, in `AndroidManifest.xml`:
         * ```xml
         * <activity
         *     android:name=".ProviderConfigurationActivity"
         *     android:exported="true">
         *     <intent-filter>
         *         <action android:name="com.example.ACTION_CONFIGURE_WIDGET" />
         *         <category android:name="com.google.android.clockwork.tiles.category.PROVIDER_CONFIG" />
         *         <category android:name="android.intent.category.DEFAULT" />
         *     </intent-filter>
         * </activity>
         * ```
         */
        public const val CATEGORY_PROVIDER_CONFIG: String =
            "com.google.android.clockwork.tiles.category.PROVIDER_CONFIG"

        /**
         * Intent extra used to supply the widget instance ID ([Int], matching
         * [WidgetInstanceId.id]) to a widget provider configuration activity.
         */
        // ActionValue lint requires an "androidx.glance.wear.extra." prefix and matching suffix,
        // but this value must match the legacy extra key sent by the Wear OS host.
        @SuppressLint("ActionValue")
        public const val EXTRA_CONFIG_WIDGET_ID: String =
            "com.google.android.clockwork.EXTRA_PROVIDER_CONFIG_TILE_ID"

        /**
         * Intent extra used to supply the widget provider [ComponentName] (the
         * [GlanceWearWidgetService]) to a widget provider configuration activity.
         */
        // ActionValue lint requires an "androidx.glance.wear.extra." prefix, but this value must
        // match the legacy extra key sent by the Wear OS host.
        @SuppressLint("ActionValue")
        public const val EXTRA_CONFIG_PROVIDER_COMPONENT: String =
            "com.google.android.clockwork.EXTRA_CONFIG_PROVIDER_COMPONENT"

        /**
         * Extracts [WearWidgetConfigExtras] from the launching configuration [intent].
         *
         * @param intent The [Intent] that launched the widget configuration activity.
         * @return The parsed [WearWidgetConfigExtras], or `null` if the [intent] does not have
         *   [CATEGORY_PROVIDER_CONFIG] or if the required extras ([EXTRA_CONFIG_WIDGET_ID] and
         *   [EXTRA_CONFIG_PROVIDER_COMPONENT]) are missing or invalid.
         */
        @JvmStatic
        public fun fromIntent(intent: Intent): WearWidgetConfigExtras? {
            if (!intent.hasCategory(CATEGORY_PROVIDER_CONFIG)) {
                return null
            }
            val extras = intent.extras ?: return null
            val widgetId = extras.getInt(EXTRA_CONFIG_WIDGET_ID, -1)
            if (widgetId < 0) {
                return null
            }
            val provider =
                BundleCompat.getParcelable(
                    extras,
                    EXTRA_CONFIG_PROVIDER_COMPONENT,
                    ComponentName::class.java,
                ) ?: return null
            return WearWidgetConfigExtras(
                instanceId = WidgetInstanceId(WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE, widgetId),
                provider = provider,
            )
        }
    }
}
