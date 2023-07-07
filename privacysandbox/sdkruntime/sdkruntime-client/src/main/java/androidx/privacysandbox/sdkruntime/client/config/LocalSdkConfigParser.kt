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
package androidx.privacysandbox.sdkruntime.client.config

import android.util.Xml
import java.io.InputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParser.END_TAG
import org.xmlpull.v1.XmlPullParser.START_TAG
import org.xmlpull.v1.XmlPullParserException

/**
 * Parser for SDK config.
 *
 * The expected XML structure is:
 * <compat-config>
 *     <dex-path>RuntimeEnabledSdk-sdk.package.name/dex/classes.dex</dex-path>
 *     <dex-path>RuntimeEnabledSdk-sdk.package.name/dex/classes2.dex</dex-path>
 *     <java-resources-root-path>RuntimeEnabledSdk-sdk.package.name/res</java-resources-root-path>
 *     <compat-entrypoint>com.sdk.EntryPointClass</compat-entrypoint>
 *     <resource-id-remapping>
 *         <r-package-class>com.test.sdk.RPackage</r-package-class>
 *         <resources-package-id>123</resources-package-id>
 *     </resource-id-remapping>
 * </compat-config>
 */
internal class LocalSdkConfigParser private constructor(
    private val xmlParser: XmlPullParser
) {

    private fun readConfig(
        packageName: String,
        versionMajor: Int?
    ): LocalSdkConfig {
        xmlParser.require(XmlPullParser.START_DOCUMENT, NAMESPACE, null)
        xmlParser.nextTag()

        val dexPaths = mutableListOf<String>()
        var javaResourcesRoot: String? = null
        var entryPoint: String? = null
        var resourceRemapping: ResourceRemappingConfig? = null

        xmlParser.require(START_TAG, NAMESPACE, CONFIG_ELEMENT_NAME)
        while (xmlParser.next() != END_TAG) {
            if (xmlParser.eventType != START_TAG) {
                continue
            }
            when (xmlParser.name) {
                DEX_PATH_ELEMENT_NAME -> {
                    val dexPath = xmlParser.nextText()
                    dexPaths.add(dexPath)
                }

                RESOURCE_ROOT_ELEMENT_NAME -> {
                    if (javaResourcesRoot != null) {
                        throw XmlPullParserException(
                            "Duplicate $RESOURCE_ROOT_ELEMENT_NAME tag found"
                        )
                    }
                    javaResourcesRoot = xmlParser.nextText()
                }

                ENTRYPOINT_ELEMENT_NAME -> {
                    if (entryPoint != null) {
                        throw XmlPullParserException(
                            "Duplicate $ENTRYPOINT_ELEMENT_NAME tag found"
                        )
                    }
                    entryPoint = xmlParser.nextText()
                }

                RESOURCE_REMAPPING_ENTRY_ELEMENT_NAME -> {
                    if (resourceRemapping != null) {
                        throw XmlPullParserException(
                            "Duplicate $RESOURCE_REMAPPING_ENTRY_ELEMENT_NAME tag found"
                        )
                    }
                    resourceRemapping = readResourceRemappingConfig()
                }

                else -> xmlParser.skipCurrentTag()
            }
        }
        xmlParser.require(END_TAG, NAMESPACE, CONFIG_ELEMENT_NAME)

        if (entryPoint == null) {
            throw XmlPullParserException("No $ENTRYPOINT_ELEMENT_NAME tag found")
        }
        if (dexPaths.isEmpty()) {
            throw XmlPullParserException("No $DEX_PATH_ELEMENT_NAME tags found")
        }

        return LocalSdkConfig(
            packageName,
            versionMajor,
            dexPaths,
            entryPoint,
            javaResourcesRoot,
            resourceRemapping
        )
    }

    private fun readResourceRemappingConfig(): ResourceRemappingConfig {
        var rPackageClassName: String? = null
        var packageId: Int? = null

        xmlParser.require(START_TAG, NAMESPACE, RESOURCE_REMAPPING_ENTRY_ELEMENT_NAME)
        while (xmlParser.next() != END_TAG) {
            if (xmlParser.eventType != START_TAG) {
                continue
            }
            when (xmlParser.name) {
                RESOURCE_REMAPPING_CLASS_ELEMENT_NAME -> {
                    if (rPackageClassName != null) {
                        throw XmlPullParserException(
                            "Duplicate $RESOURCE_REMAPPING_CLASS_ELEMENT_NAME tag found"
                        )
                    }
                    rPackageClassName = xmlParser.nextText()
                }

                RESOURCE_REMAPPING_ID_ELEMENT_NAME -> {
                    if (packageId != null) {
                        throw XmlPullParserException(
                            "Duplicate $RESOURCE_REMAPPING_ID_ELEMENT_NAME tag found"
                        )
                    }
                    packageId = xmlParser.nextText().toInt()
                }

                else -> xmlParser.skipCurrentTag()
            }
        }
        xmlParser.require(END_TAG, NAMESPACE, RESOURCE_REMAPPING_ENTRY_ELEMENT_NAME)

        if (rPackageClassName == null) {
            throw XmlPullParserException(
                "No $RESOURCE_REMAPPING_CLASS_ELEMENT_NAME tag found"
            )
        }
        if (packageId == null) {
            throw XmlPullParserException(
                "No $RESOURCE_REMAPPING_ID_ELEMENT_NAME tag found"
            )
        }

        return ResourceRemappingConfig(rPackageClassName, packageId)
    }

    companion object {
        private val NAMESPACE: String? = null // We don't use namespaces
        private const val CONFIG_ELEMENT_NAME = "compat-config"
        private const val DEX_PATH_ELEMENT_NAME = "dex-path"
        private const val RESOURCE_ROOT_ELEMENT_NAME = "java-resources-root-path"
        private const val ENTRYPOINT_ELEMENT_NAME = "compat-entrypoint"
        private const val RESOURCE_REMAPPING_ENTRY_ELEMENT_NAME = "resource-id-remapping"
        private const val RESOURCE_REMAPPING_CLASS_ELEMENT_NAME = "r-package-class"
        private const val RESOURCE_REMAPPING_ID_ELEMENT_NAME = "resources-package-id"

        fun parse(
            inputStream: InputStream,
            packageName: String,
            versionMajor: Int?
        ): LocalSdkConfig {
            val parser = Xml.newPullParser()
            try {
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(inputStream, null)
                return LocalSdkConfigParser(parser).readConfig(packageName, versionMajor)
            } finally {
                parser.setInput(null)
            }
        }
    }
}
