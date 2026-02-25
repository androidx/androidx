/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.watchface.complications.data.parser

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

/**
 * Parses static complication preview data from a provider's `AndroidManifest.xml` metadata.
 *
 * This utility is used to extract non-dynamic complication data that can be used for rendering the
 * complication previews. This allows a preview to be shown without needing to bind to the
 * complication provider service, which improves performance.
 *
 * The data is sourced from an XML resource file that is linked via a `<meta-data>` tag within the
 * complication provider service's declaration in the manifest.
 */
public object StaticPreviewDataParser {
    private const val TAG = "StaticPreviewDataParser"
    private const val META_DATA_KEY =
        "com.google.android.wearable.complications.STATIC_PREVIEW_DATA"

    /**
     * Parses all available static preview data from a provider's metadata declared in its
     * `AndroidManifest.xml`.
     *
     * This method looks for a `<meta-data>` tag with the key
     * `com.google.android.wearable.complications.STATIC_PREVIEW_DATA` within the service definition
     * of the specified `providerComponent`. The value of this tag should be a reference to an XML
     * resource containing static preview data definitions for each supported type.
     *
     * ### Example Manifest Declaration:
     * ```xml
     * <service
     * android:name=".MyComplicationProviderService"
     * android:exported="true"
     * android:label="@string/complication_provider_label"
     * android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER">
     *
     * <meta-data
     * android:name="com.google.android.wearable.comulations.STATIC_PREVIEW_DATA"
     * android:resource="@xml/my_complication_preview" />
     *
     * </service>
     * ```
     *
     * <p>Throws {@link android.content.pm.PackageManager.NameNotFoundException} if there is no
     * application with the given package name.
     *
     * <p>Throws {@link java.lang.SecurityException} if the Context requested can not be loaded into
     * the caller's process for security reasons (see {@link Context#CONTEXT_INCLUDE_CODE} for more
     * information}.
     *
     * @param context The `Context` of the calling application (e.g., the watch face). It is used to
     *   access the `PackageManager`.
     * @param providerComponent The `ComponentName` of the complication provider service from which
     *   to parse the preview data.
     * @return A [PreviewData] object if the metadata is found and successfully parsed. Returns
     *   `null` if the metadata tag is not present, if the resource ID is invalid, or if any error
     *   occurs during parsing.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission("com.google.wear.permission.SET_COMPLICATION_EXTRAS")
    @JvmStatic
    public fun parsePreviewData(context: Context, providerComponent: ComponentName): PreviewData? {
        val packageManager = context.packageManager
        try {
            val serviceInfo =
                packageManager.getServiceInfo(providerComponent, PackageManager.GET_META_DATA)
            val metaData = serviceInfo.metaData ?: return null
            val xmlResId = metaData.getInt(META_DATA_KEY)
            if (xmlResId == 0) {
                return null
            }

            val providerContext =
                context.createPackageContext(
                    providerComponent.packageName,
                    Context.CONTEXT_IGNORE_SECURITY,
                )
            providerContext.resources.getXml(xmlResId).use { parser ->
                return PreviewData.inflate(providerComponent, context, providerContext, parser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing complication preview data for $providerComponent", e)
        }
        return null
    }
}
