/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.build.testConfiguration

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory

/**
 * Simple check that the test config templates are able to be parsed as valid xml.
 */
@RunWith(JUnit4::class)
class AndroidTestXmlBuilderTest {

    private lateinit var builder: ConfigBuilder
    private lateinit var mediaBuilder: MediaConfigBuilder

    @Before
    fun init() {
        builder = ConfigBuilder()
        builder.isBenchmark(false)
            .applicationId("com.androidx.placeholder.Placeholder")
            .isPostsubmit(true)
            .minSdk("15")
            .tag("placeholder_tag")
            .testApkName("placeholder.apk")
            .testRunner("com.example.Runner")
        mediaBuilder = MediaConfigBuilder()
        mediaBuilder.clientApplicationId("com.androidx.client.Placeholder")
            .clientApkName("clientPlaceholder.apk")
            .serviceApplicationId("com.androidx.service.Placeholder")
            .serviceApkName("servicePlaceholder.apk")
            .minSdk("15")
            .tag("placeholder_tag")
            .tag("media_compat")
            .testRunner("com.example.Runner")
            .isClientPrevious(true)
            .isServicePrevious(false)
    }

    @Test
    fun testAgainstGoldenDefault() {
        MatcherAssert.assertThat(
            builder.build(),
            CoreMatchers.`is`(goldenDefaultConfig)
        )
    }

    @Test
    fun testAgainstMediaGoldenDefault() {
        MatcherAssert.assertThat(
            mediaBuilder.build(),
            CoreMatchers.`is`(goldenMediaDefaultConfig)
        )
    }

    @Test
    fun testValidTestConfigXml_default() {
        validate(builder.build())
    }

    @Test
    fun testValidTestConfigXml_benchmarkTrue() {
        builder.isBenchmark(true)
        validate(builder.build())
    }

    @Test
    fun testValidTestConfigXml_withAppApk() {
        builder.appApkName("Placeholder.apk")
        validate(builder.build())
    }

    @Test
    fun testValidTestConfigXml_presubmitWithAppApk() {
        builder.isPostsubmit(false)
            .appApkName("Placeholder.apk")
        validate(builder.build())
    }

    @Test
    fun testValidTestConfigXml_runAllTests() {
        builder.runAllTests(false)
        validate(builder.build())
    }

    @Test
    fun testValidTestConfigXml_multipleTags() {
        builder.tag("another_tag")
        MatcherAssert.assertThat(builder.tags.size, CoreMatchers.`is`(2))
        validate(builder.build())
    }

    @Test
    fun testValidTestConfigXml_presubmit() {
        builder.isPostsubmit(false)
        validate(builder.build())
    }

    @Test
    fun testValidTestConfigXml_presubmitBenchmark() {
        builder.isPostsubmit(false)
            .isBenchmark(true)
        validate(builder.build())
    }

    @Test
    fun testValidMediaConfigXml_default() {
        validate(mediaBuilder.build())
    }

    @Test
    fun testValidMediaConfigXml_presubmit() {
        mediaBuilder.isPostsubmit(false)
        validate(mediaBuilder.build())
    }

    private fun validate(xml: String) {
        val parser = SAXParserFactory.newInstance().newSAXParser()
        return parser.parse(
            InputSource(
                StringReader(
                    xml
                )
            ),
            DefaultHandler()
        )
    }
}

private val goldenDefaultConfig = """
    <?xml version="1.0" encoding="utf-8"?>
    <!-- Copyright (C) 2020 The Android Open Source Project
    Licensed under the Apache License, Version 2.0 (the "License")
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions
    and limitations under the License.-->
    <configuration description="Runs tests for the module">
    <object type="module_controller" class="com.android.tradefed.testtype.suite.module.MinApiLevelModuleController">
    <option name="min-api-level" value="15" />
    </object>
    <option name="test-suite-tag" value="placeholder_tag" />
    <option name="config-descriptor:metadata" key="applicationId" value="com.androidx.placeholder.Placeholder" />
    <option name="wifi:disable" value="true" />
    <include name="google/unbundled/common/setup" />
    <target_preparer class="com.android.tradefed.targetprep.suite.SuiteApkInstaller">
    <option name="cleanup-apks" value="false" />
    <option name="test-file-name" value="placeholder.apk" />
    </target_preparer>
    <test class="com.android.tradefed.testtype.AndroidJUnitTest">
    <option name="runner" value="com.example.Runner"/>
    <option name="package" value="com.androidx.placeholder.Placeholder" />
    </test>
    </configuration>
""".trimIndent()

private val goldenMediaDefaultConfig = """
    <?xml version="1.0" encoding="utf-8"?>
    <!-- Copyright (C) 2020 The Android Open Source Project
    Licensed under the Apache License, Version 2.0 (the "License")
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions
    and limitations under the License.-->
    <configuration description="Runs tests for the module">
    <object type="module_controller" class="com.android.tradefed.testtype.suite.module.MinApiLevelModuleController">
    <option name="min-api-level" value="15" />
    </object>
    <option name="test-suite-tag" value="placeholder_tag" />
    <option name="test-suite-tag" value="media_compat" />
    <option name="config-descriptor:metadata" key="applicationId" value="com.androidx.client.Placeholder;com.androidx.service.Placeholder" />
    <option name="wifi:disable" value="true" />
    <include name="google/unbundled/common/setup" />
    <target_preparer class="com.android.tradefed.targetprep.suite.SuiteApkInstaller">
    <option name="cleanup-apks" value="true" />
    <option name="test-file-name" value="clientPlaceholder.apk" />
    <option name="test-file-name" value="servicePlaceholder.apk" />
    </target_preparer>
    <test class="com.android.tradefed.testtype.AndroidJUnitTest">
    <option name="runner" value="com.example.Runner"/>
    <option name="package" value="com.androidx.client.Placeholder" />
    <option name="instrumentation-arg" key="client_version" value="previous" />
    <option name="instrumentation-arg" key="service_version" value="tot" />
    </test>
    <test class="com.android.tradefed.testtype.AndroidJUnitTest">
    <option name="runner" value="com.example.Runner"/>
    <option name="package" value="com.androidx.service.Placeholder" />
    <option name="instrumentation-arg" key="client_version" value="previous" />
    <option name="instrumentation-arg" key="service_version" value="tot" />
    </test>
    </configuration>
""".trimIndent()