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
 * Parser for config with paths to compat SDK configs for each SDK that bundled with app.
 *
 * The expected XML structure is:
 * <runtime-enabled-sdk-table>
 *     <runtime-enabled-sdk>
 *         <package-name>com.sdk1</package-name>
 *         <version-major>1</version-major>
 *         <compat-config-path>assets/RuntimeEnabledSdk-com.sdk1/CompatSdkConfig.xml</compat-config-path>
 *     </runtime-enabled-sdk>
 *     <runtime-enabled-sdk>
 *         <package-name>com.sdk2</package-name>
 *         <version-major>42</version-major>
 *         <compat-config-path>assets/RuntimeEnabledSdk-com.sdk2/CompatSdkConfig.xml</compat-config-path>
 *     </runtime-enabled-sdk>
 * </runtime-enabled-sdk-table>
 *
 */
internal class SdkTableConfigParser private constructor(
    private val xmlParser: XmlPullParser
) {

    private fun readSdkTable(): Set<SdkTableEntry> {
        xmlParser.require(XmlPullParser.START_DOCUMENT, NAMESPACE, null)
        xmlParser.nextTag()

        val packages = mutableSetOf<String>()

        return buildSet {
            xmlParser.require(START_TAG, NAMESPACE, SDK_TABLE_ELEMENT_NAME)
            while (xmlParser.next() != END_TAG) {
                if (xmlParser.eventType != START_TAG) {
                    continue
                }
                if (xmlParser.name == SDK_ENTRY_ELEMENT_NAME) {
                    val entry = readSdkEntry()
                    if (!packages.add(entry.packageName)) {
                        throw XmlPullParserException(
                            "Duplicate entry for ${entry.packageName} found"
                        )
                    }
                    add(entry)
                } else {
                    xmlParser.skipCurrentTag()
                }
            }
            xmlParser.require(END_TAG, NAMESPACE, SDK_TABLE_ELEMENT_NAME)
        }
    }

    private fun readSdkEntry(): SdkTableEntry {
        var packageName: String? = null
        var versionMajor: Int? = null
        var configPath: String? = null

        xmlParser.require(START_TAG, NAMESPACE, SDK_ENTRY_ELEMENT_NAME)
        while (xmlParser.next() != END_TAG) {
            if (xmlParser.eventType != START_TAG) {
                continue
            }
            when (xmlParser.name) {
                SDK_PACKAGE_NAME_ELEMENT_NAME -> {
                    if (packageName != null) {
                        throw XmlPullParserException(
                            "Duplicate $SDK_PACKAGE_NAME_ELEMENT_NAME tag found"
                        )
                    }
                    packageName = xmlParser.nextText()
                }

                VERSION_MAJOR_ELEMENT_NAME -> {
                    if (versionMajor != null) {
                        throw XmlPullParserException(
                            "Duplicate $VERSION_MAJOR_ELEMENT_NAME tag found"
                        )
                    }
                    versionMajor = xmlParser.nextText().toInt()
                }

                COMPAT_CONFIG_PATH_ELEMENT_NAME -> {
                    if (configPath != null) {
                        throw XmlPullParserException(
                            "Duplicate $COMPAT_CONFIG_PATH_ELEMENT_NAME tag found"
                        )
                    }
                    configPath = xmlParser.nextText()
                }

                else -> xmlParser.skipCurrentTag()
            }
        }
        xmlParser.require(END_TAG, NAMESPACE, SDK_ENTRY_ELEMENT_NAME)

        if (packageName == null) {
            throw XmlPullParserException(
                "No $SDK_PACKAGE_NAME_ELEMENT_NAME tag found"
            )
        }
        if (configPath == null) {
            throw XmlPullParserException(
                "No $COMPAT_CONFIG_PATH_ELEMENT_NAME tag found"
            )
        }

        return SdkTableEntry(packageName, versionMajor, configPath)
    }

    internal data class SdkTableEntry(
        val packageName: String,
        val versionMajor: Int?,
        val compatConfigPath: String,
    )

    companion object {
        private val NAMESPACE: String? = null // We don't use namespaces
        private const val SDK_TABLE_ELEMENT_NAME = "runtime-enabled-sdk-table"
        private const val SDK_ENTRY_ELEMENT_NAME = "runtime-enabled-sdk"
        private const val SDK_PACKAGE_NAME_ELEMENT_NAME = "package-name"
        private const val VERSION_MAJOR_ELEMENT_NAME = "version-major"
        private const val COMPAT_CONFIG_PATH_ELEMENT_NAME = "compat-config-path"

        fun parse(inputStream: InputStream): Set<SdkTableEntry> {
            val parser = Xml.newPullParser()
            try {
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(inputStream, null)
                return SdkTableConfigParser(parser).readSdkTable()
            } finally {
                parser.setInput(null)
            }
        }
    }
}
