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

import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfigParser.Companion.parse
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
class LocalSdkConfigParserTest {

    @Test
    fun parse_skipUnknownTagsAndReturnParsedResult() {
        val xml = """
            <compat-config>
                <compat-entrypoint>compat.sdk.provider</compat-entrypoint>
                <unknown-tag>new parameter from future library version</unknown-tag>
                <dex-path>1.dex</dex-path>
                <future-version-tag>
                    <unknown-tag>new inner tag</unknown-tag>
                </future-version-tag>
                <dex-path>2.dex</dex-path>
                <java-resource-path>javaResPath/</java-resource-path>
            </compat-config>
        """.trimIndent()

        val (dexPaths, javaResourcesRoot, entryPoint) = tryParse(xml)

        assertThat(dexPaths)
            .containsExactly("1.dex", "2.dex")

        assertThat(javaResourcesRoot)
            .isEqualTo("javaResPath/")

        assertThat(entryPoint)
            .isEqualTo("compat.sdk.provider")
    }

    @Test
    fun parse_whenNoEntryPoint_throwsException() {
        val xml = """
            <compat-config>
                <dex-path>1.dex</dex-path>
                <java-resource-path>path1/</java-resource-path>
            </compat-config>
        """.trimIndent()

        assertThrows<XmlPullParserException> {
            tryParse(xml)
        }.hasMessageThat().isEqualTo(
            "No compat-entrypoint tag found"
        )
    }

    @Test
    fun parse_whenDuplicateEntryPoint_throwsException() {
        val xml = """
            <compat-config>
                <compat-entrypoint>compat.sdk.provider</compat-entrypoint>
                <compat-entrypoint>compat.sdk.provider2</compat-entrypoint>
                <dex-path>1.dex</dex-path>
                <java-resource-path>path1/</java-resource-path>
            </compat-config>
        """.trimIndent()

        assertThrows<XmlPullParserException> {
            tryParse(xml)
        }.hasMessageThat().isEqualTo(
            "Duplicate compat-entrypoint tag found"
        )
    }

    @Test
    fun parse_whenNoDexPath_throwsException() {
        val xml = """
            <compat-config>
                <compat-entrypoint>compat.sdk.provider</compat-entrypoint>
                <java-resource-path>path1/</java-resource-path>
            </compat-config>
        """.trimIndent()

        assertThrows<XmlPullParserException> {
            tryParse(xml)
        }.hasMessageThat().isEqualTo(
            "No dex-path tags found"
        )
    }

    @Test
    fun parse_whenNoJavaResourceRoot_ReturnParsedResult() {
        val xml = """
            <compat-config>
                <compat-entrypoint>compat.sdk.provider</compat-entrypoint>
                <dex-path>1.dex</dex-path>
            </compat-config>
        """.trimIndent()

        val (dexPaths, javaResourcesRoot, entryPoint) = tryParse(xml)

        assertThat(dexPaths)
            .containsExactly("1.dex")

        assertThat(javaResourcesRoot)
            .isNull()

        assertThat(entryPoint)
            .isEqualTo("compat.sdk.provider")
    }

    @Test
    fun parse_whenDuplicateJavaResourceRoot_throwsException() {
        val xml = """
            <compat-config>
                <compat-entrypoint>compat.sdk.provider</compat-entrypoint>
                <dex-path>1.dex</dex-path>
                <java-resource-path>path1/</java-resource-path>
                <java-resource-path>path2/</java-resource-path>
            </compat-config>
        """.trimIndent()

        assertThrows<XmlPullParserException> {
            tryParse(xml)
        }.hasMessageThat().isEqualTo(
            "Duplicate java-resource-path tag found"
        )
    }

    private fun tryParse(xml: String): LocalSdkConfig {
        ByteArrayInputStream(xml.toByteArray()).use { inputStream ->
            return parse(inputStream)
        }
    }
}