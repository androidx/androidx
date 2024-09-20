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

import java.io.StringReader
import javax.xml.parsers.SAXParserFactory
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

/** Simple check that the test config templates are able to be parsed as valid xml. */
@RunWith(JUnit4::class)
class AndroidTestConfigBuilderTest {

    private lateinit var builder: ConfigBuilder

    @Before
    fun init() {
        builder = ConfigBuilder()
        builder
            .configName("placeHolderAndroidTest.xml")
            .isMicrobenchmark(false)
            .applicationId("com.androidx.placeholder.Placeholder")
            .isPostsubmit(true)
            .minSdk("15")
            .tag("placeholder_tag")
            .testApkName("placeholder.apk")
            .testApkSha256("123456")
            .testRunner("com.example.Runner")
    }

    @Test
    fun testXmlAgainstGoldenDefault() {
        MatcherAssert.assertThat(builder.buildXml(), CoreMatchers.`is`(goldenDefaultConfig))
    }

    @Test
    fun testXmlAgainstGoldenMainSandboxConfiguration() {
        builder.initialSetupApks(listOf("init-placeholder.apk"))
        builder.enablePrivacySandbox(true)
        MatcherAssert.assertThat(
            builder.buildXml(),
            CoreMatchers.`is`(goldenConfigForMainSandboxConfiguration)
        )
    }

    @Test
    fun testXmlAgainstGoldenWithSplits() {
        builder.appApkName("app.apk")
        builder.appSplits(listOf("split1.apk", "split2.apk"))
        MatcherAssert.assertThat(builder.buildXml(), CoreMatchers.`is`(goldenConfigWithSplits))
    }

    @Test
    fun testXmlAgainstGoldenMicrobenchmark() {
        builder.isMicrobenchmark(true)

        // NOTE: blocklisted arg is removed
        builder.instrumentationArgsMap["androidx.benchmark.profiling.skipWhenDurationRisksAnr"] =
            "true"
        MatcherAssert.assertThat(
            builder.buildXml(),
            CoreMatchers.`is`(goldenDefaultConfigBenchmark)
        )
    }

    @Test
    fun testXmlAgainstGoldenMacroBenchmark() {
        builder.isMacrobenchmark(true)
        builder.instrumentationArgsMap["androidx.test.argument1"] = "something1"
        builder.instrumentationArgsMap["androidx.test.argument2"] = "something2"
        builder.appApkSha256("654321")
        builder.appApkName("targetApp.apk")

        // NOTE: blocklisted arg is removed
        builder.instrumentationArgsMap["androidx.benchmark.profiling.skipWhenDurationRisksAnr"] =
            "true"
        MatcherAssert.assertThat(
            builder.buildXml(),
            CoreMatchers.`is`(goldenDefaultConfigMacroBenchmark)
        )
    }

    @Test
    fun testJsonAgainstGoldenDefault() {
        builder.instrumentationArgsMap["androidx.test.argument1"] = "something1"
        builder.instrumentationArgsMap["androidx.test.argument2"] = "something2"
        MatcherAssert.assertThat(
            builder.buildJson(),
            CoreMatchers.`is`(
                """
                {
                  "name": "placeHolderAndroidTest.xml",
                  "minSdkVersion": "15",
                  "testSuiteTags": [
                    "placeholder_tag"
                  ],
                  "testApk": "placeholder.apk",
                  "testApkSha256": "123456",
                  "instrumentationArgs": [
                    {
                      "key": "androidx.test.argument1",
                      "value": "something1"
                    },
                    {
                      "key": "androidx.test.argument2",
                      "value": "something2"
                    },
                    {
                      "key": "notAnnotation",
                      "value": "androidx.test.filters.FlakyTest"
                    }
                  ],
                  "additionalApkKeys": []
                }
            """
                    .trimIndent()
            )
        )
    }

    @Test
    fun testJsonAgainstGoldenAdditionalApkKey() {
        builder.additionalApkKeys(listOf("customKey"))
        MatcherAssert.assertThat(
            builder.buildJson(),
            CoreMatchers.`is`(
                """
                {
                  "name": "placeHolderAndroidTest.xml",
                  "minSdkVersion": "15",
                  "testSuiteTags": [
                    "placeholder_tag"
                  ],
                  "testApk": "placeholder.apk",
                  "testApkSha256": "123456",
                  "instrumentationArgs": [
                    {
                      "key": "notAnnotation",
                      "value": "androidx.test.filters.FlakyTest"
                    }
                  ],
                  "additionalApkKeys": [
                    "customKey"
                  ]
                }
            """
                    .trimIndent()
            )
        )
    }

    @Test
    fun testJsonAgainstGoldenPresubmitBenchmark() {
        builder.isMicrobenchmark(true).isPostsubmit(false)
        MatcherAssert.assertThat(
            builder.buildJson(),
            CoreMatchers.`is`(
                """
                {
                  "name": "placeHolderAndroidTest.xml",
                  "minSdkVersion": "15",
                  "testSuiteTags": [
                    "placeholder_tag"
                  ],
                  "testApk": "placeholder.apk",
                  "testApkSha256": "123456",
                  "instrumentationArgs": [
                    {
                      "key": "notAnnotation",
                      "value": "androidx.test.filters.FlakyTest"
                    },
                    {
                      "key": "androidx.benchmark.dryRunMode.enable",
                      "value": "true"
                    }
                  ],
                  "additionalApkKeys": []
                }
            """
                    .trimIndent()
            )
        )
    }

    @Test
    fun testJsonAgainstAppTestGolden() {
        builder.appApkName("app-placeholder.apk").appApkSha256("654321")
        MatcherAssert.assertThat(
            builder.buildJson(),
            CoreMatchers.`is`(
                """
                {
                  "name": "placeHolderAndroidTest.xml",
                  "minSdkVersion": "15",
                  "testSuiteTags": [
                    "placeholder_tag"
                  ],
                  "testApk": "placeholder.apk",
                  "testApkSha256": "123456",
                  "appApk": "app-placeholder.apk",
                  "appApkSha256": "654321",
                  "instrumentationArgs": [
                    {
                      "key": "notAnnotation",
                      "value": "androidx.test.filters.FlakyTest"
                    }
                  ],
                  "additionalApkKeys": []
                }
            """
                    .trimIndent()
            )
        )
    }

    @Test
    fun testAgainstMediaGoldenDefault() {
        MatcherAssert.assertThat(
            buildMediaJson(
                configName = "foo.json",
                forClient = true,
                clientApkName = "clientPlaceholder.apk",
                clientApkSha256 = "123456",
                isClientPrevious = true,
                isServicePrevious = false,
                minSdk = "15",
                serviceApkName = "servicePlaceholder.apk",
                serviceApkSha256 = "654321",
                tags = listOf("placeholder_tag", "media_compat"),
            ),
            CoreMatchers.`is`(
                """
                {
                  "name": "foo.json",
                  "minSdkVersion": "15",
                  "testSuiteTags": [
                    "placeholder_tag",
                    "media_compat"
                  ],
                  "testApk": "clientPlaceholder.apk",
                  "testApkSha256": "123456",
                  "appApk": "servicePlaceholder.apk",
                  "appApkSha256": "654321",
                  "instrumentationArgs": [
                    {
                      "key": "notAnnotation",
                      "value": "androidx.test.filters.FlakyTest"
                    },
                    {
                      "key": "client_version",
                      "value": "previous"
                    },
                    {
                      "key": "service_version",
                      "value": "tot"
                    }
                  ],
                  "additionalApkKeys": []
                }
                """
                    .trimIndent()
            )
        )
    }

    @Test
    fun testValidTestConfigXml_default() {
        validate(builder.buildXml())
    }

    @Test
    fun testValidTestConfigXml_benchmarkTrue() {
        builder.isMicrobenchmark(true)
        validate(builder.buildXml())
    }

    @Test
    fun testValidTestConfigXml_withAppApk() {
        builder.appApkName("Placeholder.apk")
        validate(builder.buildXml())
    }

    @Test
    fun testValidTestConfigXml_presubmitWithAppApk() {
        builder.isPostsubmit(false).appApkName("Placeholder.apk")
        validate(builder.buildXml())
    }

    @Test
    fun testValidTestConfigXml_runAllTests() {
        validate(builder.buildXml())
    }

    @Test
    fun testValidTestConfigXml_multipleTags() {
        builder.tag("another_tag")
        MatcherAssert.assertThat(builder.tags.size, CoreMatchers.`is`(2))
        validate(builder.buildXml())
    }

    @Test
    fun testValidTestConfigXml_presubmit() {
        builder.isPostsubmit(false)
        validate(builder.buildXml())
    }

    @Test
    fun testValidTestConfigXml_presubmitBenchmark() {
        builder.isPostsubmit(false).isMicrobenchmark(true)
        validate(builder.buildXml())
    }

    private fun validate(xml: String) {
        val parser = SAXParserFactory.newInstance().newSAXParser()
        return parser.parse(InputSource(StringReader(xml)), DefaultHandler())
    }
}

private val goldenDefaultConfig =
    """
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
    <option name="instrumentation-arg" key="notAnnotation" value="androidx.test.filters.FlakyTest" />
    <include name="google/unbundled/common/setup" />
    <target_preparer class="com.android.tradefed.targetprep.suite.SuiteApkInstaller">
    <option name="cleanup-apks" value="true" />
    <option name="install-arg" value="-t" />
    <option name="test-file-name" value="placeholder.apk" />
    </target_preparer>
    <test class="com.android.tradefed.testtype.AndroidJUnitTest">
    <option name="runner" value="com.example.Runner"/>
    <option name="package" value="com.androidx.placeholder.Placeholder" />
    </test>
    </configuration>
"""
        .trimIndent()

private val goldenConfigForMainSandboxConfiguration =
    """
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
    <option name="instrumentation-arg" key="notAnnotation" value="androidx.test.filters.FlakyTest" />
    <include name="google/unbundled/common/setup" />
    <target_preparer class="com.android.tradefed.targetprep.suite.SuiteApkInstaller">
    <option name="cleanup-apks" value="true" />
    <option name="install-arg" value="-t" />
    <option name="test-file-name" value="init-placeholder.apk" />
    <option name="test-file-name" value="placeholder.apk" />
    </target_preparer>
    <target_preparer class="com.android.tradefed.targetprep.RunCommandTargetPreparer">
    <option name="run-command" value="cmd sdk_sandbox set-state --enabled"/>
    <option name="run-command" value="device_config set_sync_disabled_for_tests persistent" />
    <option name="teardown-command" value="cmd sdk_sandbox set-state --reset"/>
    <option name="teardown-command" value="device_config set_sync_disabled_for_tests none" />
    </target_preparer>
    <test class="com.android.tradefed.testtype.AndroidJUnitTest">
    <option name="runner" value="com.example.Runner"/>
    <option name="package" value="com.androidx.placeholder.Placeholder" />
    </test>
    </configuration>
"""
        .trimIndent()

private val goldenConfigWithSplits =
    """
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
    <option name="instrumentation-arg" key="notAnnotation" value="androidx.test.filters.FlakyTest" />
    <include name="google/unbundled/common/setup" />
    <target_preparer class="com.android.tradefed.targetprep.suite.SuiteApkInstaller">
    <option name="cleanup-apks" value="true" />
    <option name="install-arg" value="-t" />
    <option name="test-file-name" value="placeholder.apk" />
    <option name="split-apk-file-names" value="app.apk,split1.apk,split2.apk" />
    </target_preparer>
    <test class="com.android.tradefed.testtype.AndroidJUnitTest">
    <option name="runner" value="com.example.Runner"/>
    <option name="package" value="com.androidx.placeholder.Placeholder" />
    </test>
    </configuration>
"""
        .trimIndent()

private val goldenDefaultConfigBenchmark =
    """
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
    <option name="instrumentation-arg" key="notAnnotation" value="androidx.test.filters.FlakyTest" />
    <option name="instrumentation-arg" key="androidx.benchmark.output.payload.testApkSha256" value="123456" />
    <include name="google/unbundled/common/setup" />
    <target_preparer class="com.android.tradefed.targetprep.suite.SuiteApkInstaller">
    <option name="cleanup-apks" value="true" />
    <option name="install-arg" value="-t" />
    <option name="test-file-name" value="placeholder.apk" />
    </target_preparer>
    <target_preparer class="com.android.tradefed.targetprep.RunCommandTargetPreparer">
    <option name="run-command" value="cmd package compile -f -m speed com.androidx.placeholder.Placeholder" />
    <option name="run-command-timeout" value="240000" />
    </target_preparer>
    <test class="com.android.tradefed.testtype.AndroidJUnitTest">
    <option name="runner" value="com.example.Runner"/>
    <option name="package" value="com.androidx.placeholder.Placeholder" />
    <option name="device-listeners" value="androidx.benchmark.junit4.InstrumentationResultsRunListener" />
    <option name="device-listeners" value="androidx.benchmark.junit4.SideEffectRunListener" />
    <option name="instrumentation-arg" key="androidx.benchmark.cpuEventCounter.enable" value="true" />
    </test>
    </configuration>
"""
        .trimIndent()

private val goldenDefaultConfigMacroBenchmark =
    """
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
    <option name="instrumentation-arg" key="notAnnotation" value="androidx.test.filters.FlakyTest" />
    <option name="instrumentation-arg" key="androidx.test.argument1" value="something1" />
    <option name="instrumentation-arg" key="androidx.test.argument2" value="something2" />
    <option name="instrumentation-arg" key="androidx.benchmark.output.payload.testApkSha256" value="123456" />
    <option name="instrumentation-arg" key="androidx.benchmark.output.payload.appApkSha256" value="654321" />
    <include name="google/unbundled/common/setup" />
    <target_preparer class="com.android.tradefed.targetprep.suite.SuiteApkInstaller">
    <option name="cleanup-apks" value="true" />
    <option name="install-arg" value="-t" />
    <option name="test-file-name" value="placeholder.apk" />
    <option name="test-file-name" value="targetApp.apk" />
    </target_preparer>
    <test class="com.android.tradefed.testtype.AndroidJUnitTest">
    <option name="runner" value="com.example.Runner"/>
    <option name="package" value="com.androidx.placeholder.Placeholder" />
    <option name="device-listeners" value="androidx.benchmark.macro.junit4.InstrumentationResultsRunListener" />
    <option name="device-listeners" value="androidx.benchmark.macro.junit4.SideEffectRunListener" />
    </test>
    </configuration>
"""
        .trimIndent()
