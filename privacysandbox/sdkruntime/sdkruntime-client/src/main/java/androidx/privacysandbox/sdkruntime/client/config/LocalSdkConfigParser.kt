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
import androidx.annotation.RestrictTo
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
 *     <dex-path>assets/RuntimeEnabledSdk-sdk.package.name/classes.dex</dex-path>
 *     <dex-path>assets/RuntimeEnabledSdk-sdk.package.name/classes2.dex</dex-path>
 *     <java-resource-path>assets/RuntimeEnabledSdk-sdk.package.name/</java-resource-path>
 *     <compat-entrypoint>com.sdk.EntryPointClass</compat-entrypoint>
 * </compat-config>
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class LocalSdkConfigParser private constructor(
    private val xmlParser: XmlPullParser
) {

    private fun readConfig(): LocalSdkConfig {
        xmlParser.require(XmlPullParser.START_DOCUMENT, NAMESPACE, null)
        xmlParser.nextTag()

        val dexPaths = mutableListOf<String>()
        var javaResourcesRoot: String? = null
        var entryPoint: String? = null

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

        return LocalSdkConfig(dexPaths, javaResourcesRoot, entryPoint)
    }

    companion object {
        private val NAMESPACE: String? = null // We don't use namespaces
        private const val CONFIG_ELEMENT_NAME = "compat-config"
        private const val DEX_PATH_ELEMENT_NAME = "dex-path"
        private const val RESOURCE_ROOT_ELEMENT_NAME = "java-resource-path"
        private const val ENTRYPOINT_ELEMENT_NAME = "compat-entrypoint"

        fun parse(inputStream: InputStream): LocalSdkConfig {
            val parser = Xml.newPullParser()
            try {
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(inputStream, null)
                return LocalSdkConfigParser(parser).readConfig()
            } finally {
                parser.setInput(null)
            }
        }
    }
}