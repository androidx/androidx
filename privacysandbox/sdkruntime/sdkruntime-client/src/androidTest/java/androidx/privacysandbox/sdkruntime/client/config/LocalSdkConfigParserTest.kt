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
                <java-resources-root-path>javaResPath/</java-resources-root-path>
                <resource-id-remapping>
                    <unknown-tag>new remapping tag</unknown-tag>
                    <r-package-class>com.test.sdk.RPackage</r-package-class>
                    <resources-package-id>42</resources-package-id>
                </resource-id-remapping>
            </compat-config>
        """.trimIndent()

        val result = tryParse(xml, packageName = "com.test.sdk.package", versionMajor = 1)

        assertThat(result)
            .isEqualTo(
                LocalSdkConfig(
                    packageName = "com.test.sdk.package",
                    versionMajor = 1,
                    dexPaths = listOf("1.dex", "2.dex"),
                    entryPoint = "compat.sdk.provider",
                    javaResourcesRoot = "javaResPath/",
                    resourceRemapping = ResourceRemappingConfig(
                        rPackageClassName = "com.test.sdk.RPackage",
                        packageId = 42
                    )
                )
            )
    }

    @Test
    fun parse_whenOnlyMandatoryElements_returnParsedResult() {
        val xml = """
            <compat-config>
                <compat-entrypoint>compat.sdk.provider</compat-entrypoint>
                <dex-path>1.dex</dex-path>
            </compat-config>
        """.trimIndent()

        val result = tryParse(xml, packageName = "com.test.sdk.package")

        assertThat(result)
            .isEqualTo(
                LocalSdkConfig(
                    packageName = "com.test.sdk.package",
                    versionMajor = null,
                    dexPaths = listOf("1.dex"),
                    entryPoint = "compat.sdk.provider",
                    javaResourcesRoot = null,
                    resourceRemapping = null
                )
            )
    }

    @Test
    fun parse_whenNoEntryPoint_throwsException() {
        assertParsingFailWithReason(
            xml = """
                <compat-config>
                    <dex-path>1.dex</dex-path>
                </compat-config>
            """.trimIndent(),
            reason = "No compat-entrypoint tag found"
        )
    }

    @Test
    fun parse_whenDuplicateEntryPoint_throwsException() {
        assertParsingFailWithReason(
            xml = """
                <compat-config>
                    <compat-entrypoint>compat.sdk.provider</compat-entrypoint>
                    <compat-entrypoint>compat.sdk.provider2</compat-entrypoint>
                    <dex-path>1.dex</dex-path>
                </compat-config>
            """.trimIndent(),
            reason = "Duplicate compat-entrypoint tag found"
        )
    }

    @Test
    fun parse_whenNoDexPath_throwsException() {
        assertParsingFailWithReason(
            xml = """
                <compat-config>
                    <compat-entrypoint>compat.sdk.provider</compat-entrypoint>
                </compat-config>
            """.trimIndent(),
            reason = "No dex-path tags found"
        )
    }

    @Test
    fun parse_whenDuplicateJavaResourceRoot_throwsException() {
        assertParsingFailWithReason(
            xml = """
                <compat-config>
                    <compat-entrypoint>compat.sdk.provider</compat-entrypoint>
                    <dex-path>1.dex</dex-path>
                    <java-resources-root-path>path1/</java-resources-root-path>
                    <java-resources-root-path>path2/</java-resources-root-path>
                </compat-config>
            """.trimIndent(),
            reason = "Duplicate java-resources-root-path tag found"
        )
    }

    @Test
    fun parse_whenDuplicateResourceRemapping_throwsException() {
        assertParsingFailWithReason(
            xml = """
                <compat-config>
                    <compat-entrypoint>compat.sdk.provider</compat-entrypoint>
                    <dex-path>1.dex</dex-path>
                    <resource-id-remapping>
                        <r-package-class>com.test.sdk.RPackage</r-package-class>
                        <resources-package-id>42</resources-package-id>
                    </resource-id-remapping>
                    <resource-id-remapping>
                        <r-package-class>com.test.sdk.RPackage</r-package-class>
                        <resources-package-id>42</resources-package-id>
                    </resource-id-remapping>
                </compat-config>
            """.trimIndent(),
            reason = "Duplicate resource-id-remapping tag found"
        )
    }

    @Test
    fun parse_whenNoClassInResourceRemapping_throwsException() {
        assertParsingFailWithReason(
            xml = """
                <compat-config>
                    <compat-entrypoint>compat.sdk.provider</compat-entrypoint>
                    <dex-path>1.dex</dex-path>
                    <resource-id-remapping>
                        <resources-package-id>42</resources-package-id>
                    </resource-id-remapping>
                </compat-config>
            """.trimIndent(),
            reason = "No r-package-class tag found"
        )
    }

    @Test
    fun parse_whenDuplicateClassInResourceRemapping_throwsException() {
        assertParsingFailWithReason(
            xml = """
                <compat-config>
                    <compat-entrypoint>compat.sdk.provider</compat-entrypoint>
                    <dex-path>1.dex</dex-path>
                    <resource-id-remapping>
                        <r-package-class>com.test.sdk.RPackage</r-package-class>
                        <r-package-class>com.test.sdk.RPackage</r-package-class>
                        <resources-package-id>42</resources-package-id>
                    </resource-id-remapping>
                </compat-config>
            """.trimIndent(),
            reason = "Duplicate r-package-class tag found"
        )
    }

    @Test
    fun parse_whenNoPackageIdInResourceRemapping_throwsException() {
        assertParsingFailWithReason(
            xml = """
                <compat-config>
                    <compat-entrypoint>compat.sdk.provider</compat-entrypoint>
                    <dex-path>1.dex</dex-path>
                    <resource-id-remapping>
                        <r-package-class>com.test.sdk.RPackage</r-package-class>
                    </resource-id-remapping>
                </compat-config>
            """.trimIndent(),
            reason = "No resources-package-id tag found"
        )
    }

    @Test
    fun parse_whenDuplicatePackageIdInResourceRemapping_throwsException() {
        assertParsingFailWithReason(
            xml = """
                <compat-config>
                    <compat-entrypoint>compat.sdk.provider</compat-entrypoint>
                    <dex-path>1.dex</dex-path>
                    <resource-id-remapping>
                        <r-package-class>com.test.sdk.RPackage</r-package-class>
                        <resources-package-id>42</resources-package-id>
                        <resources-package-id>42</resources-package-id>
                    </resource-id-remapping>
                </compat-config>
            """.trimIndent(),
            reason = "Duplicate resources-package-id tag found"
        )
    }

    private fun assertParsingFailWithReason(xml: String, reason: String) {
        assertThrows<XmlPullParserException> {
            tryParse(xml)
        }.hasMessageThat().isEqualTo(
            reason
        )
    }

    private fun tryParse(
        xml: String,
        packageName: String = "sdkPackageName",
        versionMajor: Int? = null
    ): LocalSdkConfig {
        ByteArrayInputStream(xml.toByteArray()).use { inputStream ->
            return parse(inputStream, packageName, versionMajor)
        }
    }
}
