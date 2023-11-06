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

import androidx.privacysandbox.sdkruntime.client.config.SdkTableConfigParser.Companion.parse
import androidx.privacysandbox.sdkruntime.client.config.SdkTableConfigParser.SdkTableEntry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParserException

@SmallTest
@RunWith(AndroidJUnit4::class)
class SdkTableConfigParserTest {

    @Test
    fun parse_skipUnknownTagsAndReturnSetWithSdkTableEntries() {
        val xml = """
            <runtime-enabled-sdk-table>
                <runtime-enabled-sdk>
                    <package-name>sdk1</package-name>
                    <unknown-tag>new parameter from future library version</unknown-tag>
                    <compat-config-path>config1.xml</compat-config-path>
                </runtime-enabled-sdk>
                <future-version-runtime-enabled-sdk>
                    <unknown-tag>new sdk type without old tags</unknown-tag>
                </future-version-runtime-enabled-sdk>
                <runtime-enabled-sdk>
                    <unknown-tag2>new parameter from future library version</unknown-tag2>
                    <package-name>sdk2</package-name>
                    <version-major>42</version-major>
                    <compat-config-path>config2.xml</compat-config-path>
                </runtime-enabled-sdk>
            </runtime-enabled-sdk-table>
        """.trimIndent()

        val result = tryParse(xml)

        assertThat(result)
            .containsExactly(
                SdkTableEntry(
                    packageName = "sdk1",
                    versionMajor = null,
                    compatConfigPath = "config1.xml"
                ),
                SdkTableEntry(
                    packageName = "sdk2",
                    versionMajor = 42,
                    compatConfigPath = "config2.xml"
                )
            )
    }

    @Test
    fun parse_whenEmptyTable_returnsEmptyMap() {
        val xml = """
            <runtime-enabled-sdk-table>
            </runtime-enabled-sdk-table>
        """.trimIndent()

        val result = tryParse(xml)

        assertThat(result).isEmpty()
    }

    @Test
    fun parse_whenNoPackageName_throwsException() {
        val xml = """
            <runtime-enabled-sdk-table>
                <runtime-enabled-sdk>
                    <compat-config-path>config1.xml</compat-config-path>
                </runtime-enabled-sdk>
            </runtime-enabled-sdk-table>
        """.trimIndent()

        assertThrows<XmlPullParserException> {
            tryParse(xml)
        }.hasMessageThat().isEqualTo(
            "No package-name tag found"
        )
    }

    @Test
    fun parse_whenMultiplePackageNames_throwsException() {
        val xml = """
            <runtime-enabled-sdk-table>
                <runtime-enabled-sdk>
                    <package-name>sdk1</package-name>
                    <package-name>sdk2</package-name>
                    <compat-config-path>config1.xml</compat-config-path>
                </runtime-enabled-sdk>
            </runtime-enabled-sdk-table>
        """.trimIndent()

        assertThrows<XmlPullParserException> {
            tryParse(xml)
        }.hasMessageThat().isEqualTo(
            "Duplicate package-name tag found"
        )
    }

    @Test
    fun parse_whenMultipleVersionMajor_throwsException() {
        val xml = """
            <runtime-enabled-sdk-table>
                <runtime-enabled-sdk>
                    <package-name>sdk1</package-name>
                    <version-major>1</version-major>
                    <version-major>2</version-major>
                    <compat-config-path>config1.xml</compat-config-path>
                </runtime-enabled-sdk>
            </runtime-enabled-sdk-table>
        """.trimIndent()

        assertThrows<XmlPullParserException> {
            tryParse(xml)
        }.hasMessageThat().isEqualTo(
            "Duplicate version-major tag found"
        )
    }

    @Test
    fun parse_whenNoConfigPath_throwsException() {
        val xml = """
            <runtime-enabled-sdk-table>
                <runtime-enabled-sdk>
                    <package-name>sdk1</package-name>
                </runtime-enabled-sdk>
            </runtime-enabled-sdk-table>
        """.trimIndent()

        assertThrows<XmlPullParserException> {
            tryParse(xml)
        }.hasMessageThat().isEqualTo(
            "No compat-config-path tag found"
        )
    }

    @Test
    fun parse_whenMultipleConfigPaths_throwsException() {
        val xml = """
            <runtime-enabled-sdk-table>
                <runtime-enabled-sdk>
                    <package-name>sdk1</package-name>
                    <compat-config-path>config1.xml</compat-config-path>
                    <compat-config-path>config2.xml</compat-config-path>
                </runtime-enabled-sdk>
            </runtime-enabled-sdk-table>
        """.trimIndent()

        assertThrows<XmlPullParserException> {
            tryParse(xml)
        }.hasMessageThat().isEqualTo(
            "Duplicate compat-config-path tag found"
        )
    }

    @Test
    fun parse_whenDuplicatePackageName_throwsException() {
        val xml = """
            <runtime-enabled-sdk-table>
                <runtime-enabled-sdk>
                    <package-name>sdk1</package-name>
                    <compat-config-path>config1.xml</compat-config-path>
                </runtime-enabled-sdk>
                <runtime-enabled-sdk>
                    <package-name>sdk1</package-name>
                    <compat-config-path>config2.xml</compat-config-path>
                </runtime-enabled-sdk>
            </runtime-enabled-sdk-table>
        """.trimIndent()

        assertThrows<XmlPullParserException> {
            tryParse(xml)
        }.hasMessageThat().isEqualTo(
            "Duplicate entry for sdk1 found"
        )
    }

    private fun tryParse(xml: String): Set<SdkTableEntry> {
        ByteArrayInputStream(xml.toByteArray()).use { inputStream ->
            return parse(inputStream)
        }
    }
}
