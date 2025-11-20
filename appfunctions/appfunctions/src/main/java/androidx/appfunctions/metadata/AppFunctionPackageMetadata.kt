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
import android.content.res.XmlResourceParser
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.metadata.AppFunctionPackageMetadata.Companion.APP_METADATA_APPFUNCTIONS_LIBRARY_ATTRIBUTE_NAMESPACE
import androidx.appfunctions.metadata.AppFunctionPackageMetadata.Companion.APP_METADATA_ATTRIBUTE_NAMESPACE
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

            val targetAppInfo = pm.getApplicationInfo(packageName, /* flags= */ 0)
            val targetAppResources =
                pm.getResourcesForApplication(targetAppInfo, context.resources.configuration)
            // TODO: b/429150483 - Index resId(s) and constant values from XML in AppSearch already.
            val xmlParser = targetAppResources.getXml(appMetadataXmlRes)

            while (xmlParser.eventType != XmlPullParser.START_TAG) {
                xmlParser.next()

                if (xmlParser.eventType == XmlPullParser.END_DOCUMENT) {
                    // Empty XML
                    return null
                }
            }

            val description = getXmlAttributeValue(xmlParser, DESCRIPTION_ATTRIBUTE_NAME)

            val displayDescriptionResId =
                getXmlAttributeResourceValue(xmlParser, DISPLAY_DESCRIPTION_ATTRIBUTE_NAME)

            val displayDescription =
                if (displayDescriptionResId != 0) {
                    targetAppResources.getString(displayDescriptionResId)
                } else {
                    getXmlAttributeValue(xmlParser, DISPLAY_DESCRIPTION_ATTRIBUTE_NAME)
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

    /**
     * Retrieves the value of an attribute from an XML parser, checking both the generic and
     * library-specific namespaces.
     *
     * This function attempts to get the attribute value using [APP_METADATA_ATTRIBUTE_NAMESPACE]
     * first. If the attribute is not found, it then tries to get it using
     * [APP_METADATA_APPFUNCTIONS_LIBRARY_ATTRIBUTE_NAMESPACE].
     *
     * @param xmlParser The [XmlResourceParser] to read the attribute from.
     * @param attributeName The name of the attribute to retrieve.
     * @return The attribute value as a [String], or `null` if the attribute is not found in either
     *   namespace.
     * @see APP_METADATA_ATTRIBUTE_NAMESPACE
     * @see APP_METADATA_APPFUNCTIONS_LIBRARY_ATTRIBUTE_NAMESPACE
     */
    private fun getXmlAttributeValue(xmlParser: XmlResourceParser, attributeName: String): String? {
        return xmlParser.getAttributeValue(APP_METADATA_ATTRIBUTE_NAMESPACE, attributeName)
            ?: xmlParser.getAttributeValue(
                APP_METADATA_APPFUNCTIONS_LIBRARY_ATTRIBUTE_NAMESPACE,
                attributeName,
            )
    }

    /**
     * Retrieves the resource ID for the display description attribute from an XML parser.
     *
     * This function attempts to get the attribute resource value using
     * [APP_METADATA_ATTRIBUTE_NAMESPACE] first. If the attribute is not found (returns 0), it then
     * tries to get it using [APP_METADATA_APPFUNCTIONS_LIBRARY_ATTRIBUTE_NAMESPACE].
     *
     * @param xmlParser The [XmlResourceParser] to read the attribute from.
     * @return The resource ID as an [Int], or 0 if the attribute is not found in either namespace.
     * @see APP_METADATA_ATTRIBUTE_NAMESPACE
     * @see APP_METADATA_APPFUNCTIONS_LIBRARY_ATTRIBUTE_NAMESPACE
     */
    private fun getXmlAttributeResourceValue(
        xmlParser: XmlResourceParser,
        attributeName: String,
    ): Int {
        val displayDescriptionResIdWithGenericNamespace =
            xmlParser.getAttributeResourceValue(APP_METADATA_ATTRIBUTE_NAMESPACE, attributeName, 0)

        return if (displayDescriptionResIdWithGenericNamespace == 0) {
            xmlParser.getAttributeResourceValue(
                APP_METADATA_APPFUNCTIONS_LIBRARY_ATTRIBUTE_NAMESPACE,
                attributeName,
                0,
            )
        } else displayDescriptionResIdWithGenericNamespace
    }

    private companion object {
        private const val APP_METADATA_XML_PROPERTY = "android.app.appfunctions.app_metadata"

        /**
         * Build systems like Gradle merge library resources with app's resources hence `res-auto`
         * can be used.
         */
        private const val APP_METADATA_ATTRIBUTE_NAMESPACE =
            "http://schemas.android.com/apk/res-auto"

        /**
         * Build systems like Bazel keep app resources separate from the library hence users need to
         * explicitly mention the library package in the namespace.
         */
        private const val APP_METADATA_APPFUNCTIONS_LIBRARY_ATTRIBUTE_NAMESPACE =
            "http://schemas.android.com/apk/androidx.appfunctions"
        private const val DISPLAY_DESCRIPTION_ATTRIBUTE_NAME = "displayDescription"
        private const val DESCRIPTION_ATTRIBUTE_NAME = "description"
    }
}
