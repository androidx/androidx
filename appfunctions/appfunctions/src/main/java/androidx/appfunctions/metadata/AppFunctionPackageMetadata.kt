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

package androidx.appfunctions.metadata

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import org.xmlpull.v1.XmlPullParser

/**
 * Represents metadata about a package providing app functions.
 *
 * @property packageName name of the package.
 * @property appFunctions list of [AppFunctionMetadata] for each app function provided by the app.
 */
public class AppFunctionPackageMetadata(
    public val packageName: String,
    public val appFunctions: List<AppFunctionMetadata>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionPackageMetadata

        if (packageName != other.packageName) return false
        if (appFunctions != other.appFunctions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + appFunctions.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppFunctionPackageMetadata(packageName='$packageName', appFunctions=$appFunctions)"
    }

    /**
     * Resolves and parses the [AppFunctionAppMetadata] for this package.
     *
     * This function parses the AppFunctionAppMetadata from the XML file specified in the
     * `AndroidManifest.xml` within the `<application>` tag, where it is linked as a resource using
     * a `<property>` tag with the name `android.app.appfunctions.app_metadata`.
     *
     * For example, in target app's AndroidManifest this will be defined as:
     * ```xml
     * <application ...>
     *  ...
     *  <property
     *      android:name="android.app.appfunctions.app_metadata"
     *      android:resource="@xml/your_metadata_file" />
     * </application>
     * ```
     *
     * This method should be called on a background/worker thread as it reads resources from another
     * app.
     *
     * @param context The [Context] used to access the resources. Any resource references in the
     *   metadata, are resolved using this context's current configuration (e.g., for localization).
     * @return The parsed [AppFunctionAppMetadata] on success, or `null` if the metadata is not
     *   defined, the package is not found.
     */
    @WorkerThread
    @RequiresApi(Build.VERSION_CODES.S)
    public fun resolveAppFunctionAppMetadata(context: Context): AppFunctionAppMetadata? {
        val pm = context.packageManager

        return try {
            val appMetadataXmlRes =
                pm.getProperty(APP_METADATA_XML_PROPERTY, packageName).resourceId

            if (appMetadataXmlRes == Resources.ID_NULL) {
                return null
            }

            val targetAppResources = pm.getResourcesForApplication(packageName)
            // TODO: b/429150483 - Index resId(s) and constant values from XML in AppSearch already.
            val xmlParser = targetAppResources.getXml(appMetadataXmlRes)

            while (xmlParser.eventType != XmlPullParser.START_TAG) {
                xmlParser.next()

                if (xmlParser.eventType == XmlPullParser.END_DOCUMENT) {
                    // Empty XML
                    return null
                }
            }

            val description =
                xmlParser.getAttributeValue(
                    APP_METADATA_ATTRIBUTE_NAMESPACE,
                    DESCRIPTION_ATTRIBUTE_NAME,
                )

            val displayDescriptionResId =
                xmlParser.getAttributeResourceValue(
                    APP_METADATA_ATTRIBUTE_NAMESPACE,
                    DISPLAY_DESCRIPTION_ATTRIBUTE_NAME,
                    0,
                )

            val displayDescription =
                if (displayDescriptionResId != 0) {
                    targetAppResources.getString(displayDescriptionResId)
                } else {
                    xmlParser.getAttributeValue(
                        APP_METADATA_ATTRIBUTE_NAMESPACE,
                        DISPLAY_DESCRIPTION_ATTRIBUTE_NAME,
                    )
                }

            AppFunctionAppMetadata(
                description = description ?: "",
                displayDescription = displayDescription ?: "",
            )
        } catch (ex: Exception) {
            Log.d(
                APP_FUNCTIONS_TAG,
                "Encountered an error while resolving app metadata for package: $packageName.",
                ex,
            )
            null
        }
    }

    private companion object {
        private const val APP_METADATA_XML_PROPERTY = "android.app.appfunctions.app_metadata"
        private const val APP_METADATA_ATTRIBUTE_NAMESPACE =
            "http://schemas.android.com/apk/res-auto"
        private const val DISPLAY_DESCRIPTION_ATTRIBUTE_NAME = "displayDescription"
        private const val DESCRIPTION_ATTRIBUTE_NAME = "description"
    }
}
