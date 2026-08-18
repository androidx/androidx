/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.wear.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WearWidgetProviderXmlContainerTypeValidationTest : LintDetectorTest() {

    override fun getDetector(): Detector = WearWidgetProviderXmlDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            WearWidgetProviderXmlDetector.XML_MISSING_CONTAINER_ISSUE,
            WearWidgetProviderXmlDetector.XML_MISSING_PREVIEW_IMAGE_ISSUE,
            WearWidgetProviderXmlDetector.XML_MISSING_CONTAINER_TYPE_ISSUE,
            WearWidgetProviderXmlDetector.XML_DUPLICATE_CONTAINER_TYPE_ISSUE,
            WearWidgetProviderXmlDetector.XML_UNSUPPORTED_CONTAINER_TYPE_ISSUE,
            WearWidgetProviderXmlDetector.XML_UNRECOGNIZED_CONTAINER_TYPE_ISSUE,
        )

    @Test
    fun uniqueContainerTypes_passes() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type="1" previewImage="@drawable/preview1" />
                    <container type="2" previewImage="@drawable/preview2" />
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun duplicateContainerType_fails() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type="2" previewImage="@drawable/preview1" />
                    <container type="2" previewImage="@drawable/preview2" />
                    """
                )
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:3: Error: Duplicate container types are not allowed. Type '2' is duplicated. [WearWidgetDuplicateContainerType]
                    <container type="2" previewImage="@drawable/preview2" />
                               ~~~~~~~~
                    res/xml/wear_widget_info.xml:2: Previously defined here
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun duplicateContainerType_caseInsensitive_fails() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type="small" previewImage="@drawable/preview1" />
                    <container type="SMALL" previewImage="@drawable/preview2" />
                    """
                )
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:3: Error: Duplicate container types are not allowed. Type 'SMALL' is duplicated. [WearWidgetDuplicateContainerType]
                    <container type="SMALL" previewImage="@drawable/preview2" />
                               ~~~~~~~~~~~~
                    res/xml/wear_widget_info.xml:2: Previously defined here
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun duplicateContainerType_integerResource_fails() {
        lint()
            .files(
                integersXml("<integer name=\"container_type_small\">2</integer>"),
                widgetInfoXml(
                    """
                    <container type="2" previewImage="@drawable/preview1" />
                    <container type="@integer/container_type_small" previewImage="@drawable/preview2" />
                    """
                ),
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:3: Error: Duplicate container types are not allowed. Type '@integer/container_type_small' is duplicated. [WearWidgetDuplicateContainerType]
                    <container type="@integer/container_type_small" previewImage="@drawable/preview2" />
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    res/xml/wear_widget_info.xml:2: Previously defined here
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun duplicateContainerType_stringAndIntEquivalent_fails() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type="1" previewImage="@drawable/preview1" />
                    <container type="large" previewImage="@drawable/preview2" />
                    """
                )
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:3: Error: Duplicate container types are not allowed. Type 'large' is duplicated. [WearWidgetDuplicateContainerType]
                    <container type="large" previewImage="@drawable/preview2" />
                               ~~~~~~~~~~~~
                    res/xml/wear_widget_info.xml:2: Previously defined here
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun unsupportedContainerType_tileCompat_string_fails() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type="TILE_COMPAT" previewImage="@drawable/preview1" />
                    """
                )
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:2: Error: Tile compat container type is not supported in widget's metadata. [WearWidgetUnsupportedContainerType]
                    <container type="TILE_COMPAT" previewImage="@drawable/preview1" />
                               ~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun unsupportedContainerType_tileCompat_int_fails() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type="0" previewImage="@drawable/preview1" />
                    """
                )
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:2: Error: Tile compat container type is not supported in widget's metadata. [WearWidgetUnsupportedContainerType]
                    <container type="0" previewImage="@drawable/preview1" />
                               ~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun unsupportedContainerType_tileCompat_integerResource_fails() {
        lint()
            .files(
                integersXml("<integer name=\"compat_type\">0</integer>"),
                widgetInfoXml(
                    """
                    <container type="@integer/compat_type" previewImage="@drawable/preview1" />
                    """
                ),
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:2: Error: Tile compat container type is not supported in widget's metadata. [WearWidgetUnsupportedContainerType]
                    <container type="@integer/compat_type" previewImage="@drawable/preview1" />
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun unsupportedContainerType_tileCompat_caseInsensitive_fails() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type="tile_compat" previewImage="@drawable/preview1" />
                    """
                )
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:2: Error: Tile compat container type is not supported in widget's metadata. [WearWidgetUnsupportedContainerType]
                    <container type="tile_compat" previewImage="@drawable/preview1" />
                               ~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun unsupportedContainerType_tileCompat_stringResource_fails() {
        lint()
            .files(
                stringsXml("<string name=\"compat_str_type\">TILE_COMPAT</string>"),
                widgetInfoXml(
                    """
                    <container type="@string/compat_str_type" previewImage="@drawable/preview1" />
                    """
                ),
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:2: Error: Tile compat container type is not supported in widget's metadata. [WearWidgetUnsupportedContainerType]
                    <container type="@string/compat_str_type" previewImage="@drawable/preview1" />
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun unrecognizedContainerType_paddedInt_warns() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type=" 1 " previewImage="@drawable/preview1" />
                    """
                )
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:2: Warning: Unrecognized container type ' 1 '. [WearWidgetUnrecognizedContainerType]
                    <container type=" 1 " previewImage="@drawable/preview1" />
                               ~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun unrecognizedContainerType_paddedString_warns() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type=" small " previewImage="@drawable/preview1" />
                    """
                )
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:2: Warning: Unrecognized container type ' small '. [WearWidgetUnrecognizedContainerType]
                    <container type=" small " previewImage="@drawable/preview1" />
                               ~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun unrecognizedContainerType_invalidString_warns() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type="invalid_type" previewImage="@drawable/preview1" />
                    """
                )
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:2: Warning: Unrecognized container type 'invalid_type'. [WearWidgetUnrecognizedContainerType]
                    <container type="invalid_type" previewImage="@drawable/preview1" />
                               ~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun unrecognizedContainerType_unknownIntegerResource_warns() {
        lint()
            .files(
                integersXml("<integer name=\"unknown_int_type\">99</integer>"),
                widgetInfoXml(
                    """
                    <container type="@integer/unknown_int_type" previewImage="@drawable/preview1" />
                    """
                ),
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:2: Warning: Unrecognized container type '@integer/unknown_int_type'. [WearWidgetUnrecognizedContainerType]
                    <container type="@integer/unknown_int_type" previewImage="@drawable/preview1" />
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun unrecognizedContainerType_unresolvedStringResource_warns() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type="@string/widget_type" previewImage="@drawable/preview1" />
                    """
                )
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:2: Warning: Unrecognized container type '@string/widget_type'. [WearWidgetUnrecognizedContainerType]
                    <container type="@string/widget_type" previewImage="@drawable/preview1" />
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun unrecognizedContainerType_unknownStringResource_warns() {
        lint()
            .files(
                stringsXml("<string name=\"unknown_str_type\">unknown_custom_type</string>"),
                widgetInfoXml(
                    """
                    <container type="@string/unknown_str_type" previewImage="@drawable/preview1" />
                    """
                ),
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:2: Warning: Unrecognized container type '@string/unknown_str_type'. [WearWidgetUnrecognizedContainerType]
                    <container type="@string/unknown_str_type" previewImage="@drawable/preview1" />
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun validContainerTypes_stringsAndResources_passes() {
        lint()
            .files(
                integersXml("<integer name=\"small_type\">2</integer>"),
                widgetInfoXml(
                    """
                    <container type="large" previewImage="@drawable/preview1" />
                    <container type="@integer/small_type" previewImage="@drawable/preview2" />
                    """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun officialContainerTypeResources_passes() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type="@integer/glance_wear_container_type_large" previewImage="@drawable/preview1" />
                    <container type="@integer/glance_wear_container_type_small" previewImage="@drawable/preview2" />
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun duplicateContainerType_officialResourceAndString_fails() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type="@integer/glance_wear_container_type_small" previewImage="@drawable/preview1" />
                    <container type="SMALL" previewImage="@drawable/preview2" />
                    """
                )
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:3: Error: Duplicate container types are not allowed. Type 'SMALL' is duplicated. [WearWidgetDuplicateContainerType]
                    <container type="SMALL" previewImage="@drawable/preview2" />
                               ~~~~~~~~~~~~
                    res/xml/wear_widget_info.xml:2: Previously defined here
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun duplicateContainerType_chainedIntegerResource_fails() {
        lint()
            .files(
                integersXml(
                    """
                    <integer name="base_type">2</integer>
                    <integer name="alias_type">@integer/base_type</integer>
                    """
                ),
                widgetInfoXml(
                    """
                    <container type="SMALL" previewImage="@drawable/preview1" />
                    <container type="@integer/alias_type" previewImage="@drawable/preview2" />
                    """
                ),
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:3: Error: Duplicate container types are not allowed. Type '@integer/alias_type' is duplicated. [WearWidgetDuplicateContainerType]
                    <container type="@integer/alias_type" previewImage="@drawable/preview2" />
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~
                    res/xml/wear_widget_info.xml:2: Previously defined here
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun validContainerTypes_stringResource_passes() {
        lint()
            .files(
                stringsXml(
                    """
                    <string name="large_type">LARGE</string>
                    <string name="small_type">small</string>
                    """
                ),
                widgetInfoXml(
                    """
                    <container type="@string/large_type" previewImage="@drawable/preview1" />
                    <container type="@string/small_type" previewImage="@drawable/preview2" />
                    """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun duplicateContainerType_stringResource_fails() {
        lint()
            .files(
                stringsXml("<string name=\"container_type_small\">SMALL</string>"),
                widgetInfoXml(
                    """
                    <container type="SMALL" previewImage="@drawable/preview1" />
                    <container type="@string/container_type_small" previewImage="@drawable/preview2" />
                    """
                ),
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:3: Error: Duplicate container types are not allowed. Type '@string/container_type_small' is duplicated. [WearWidgetDuplicateContainerType]
                    <container type="@string/container_type_small" previewImage="@drawable/preview2" />
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    res/xml/wear_widget_info.xml:2: Previously defined here
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun duplicateContainerType_chainedStringResource_fails() {
        lint()
            .files(
                stringsXml(
                    """
                    <string name="base_type">LARGE</string>
                    <string name="alias_type">@string/base_type</string>
                    """
                ),
                widgetInfoXml(
                    """
                    <container type="LARGE" previewImage="@drawable/preview1" />
                    <container type="@string/alias_type" previewImage="@drawable/preview2" />
                    """
                ),
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:3: Error: Duplicate container types are not allowed. Type '@string/alias_type' is duplicated. [WearWidgetDuplicateContainerType]
                    <container type="@string/alias_type" previewImage="@drawable/preview2" />
                               ~~~~~~~~~~~~~~~~~~~~~~~~~
                    res/xml/wear_widget_info.xml:2: Previously defined here
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun duplicateContainerType_unrecognized_fails() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type="custom_unknown" previewImage="@drawable/preview1" />
                    <container type="custom_unknown" previewImage="@drawable/preview2" />
                    """
                )
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:3: Error: Duplicate container types are not allowed. Type 'custom_unknown' is duplicated. [WearWidgetDuplicateContainerType]
                    <container type="custom_unknown" previewImage="@drawable/preview2" />
                               ~~~~~~~~~~~~~~~~~~~~~
                    res/xml/wear_widget_info.xml:2: Previously defined here
                res/xml/wear_widget_info.xml:2: Warning: Unrecognized container type 'custom_unknown'. [WearWidgetUnrecognizedContainerType]
                    <container type="custom_unknown" previewImage="@drawable/preview1" />
                               ~~~~~~~~~~~~~~~~~~~~~
                res/xml/wear_widget_info.xml:3: Warning: Unrecognized container type 'custom_unknown'. [WearWidgetUnrecognizedContainerType]
                    <container type="custom_unknown" previewImage="@drawable/preview2" />
                               ~~~~~~~~~~~~~~~~~~~~~
                1 errors, 2 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun duplicateContainerType_tileCompat_fails() {
        lint()
            .files(
                widgetInfoXml(
                    """
                    <container type="TILE_COMPAT" previewImage="@drawable/preview1" />
                    <container type="0" previewImage="@drawable/preview2" />
                    """
                )
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:3: Error: Duplicate container types are not allowed. Type '0' is duplicated. [WearWidgetDuplicateContainerType]
                    <container type="0" previewImage="@drawable/preview2" />
                               ~~~~~~~~
                    res/xml/wear_widget_info.xml:2: Previously defined here
                res/xml/wear_widget_info.xml:2: Error: Tile compat container type is not supported in widget's metadata. [WearWidgetUnsupportedContainerType]
                    <container type="TILE_COMPAT" previewImage="@drawable/preview1" />
                               ~~~~~~~~~~~~~~~~~~
                res/xml/wear_widget_info.xml:3: Error: Tile compat container type is not supported in widget's metadata. [WearWidgetUnsupportedContainerType]
                    <container type="0" previewImage="@drawable/preview2" />
                               ~~~~~~~~
                3 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun resourceResolution_overriddenInQualifiers_prefersDefault() {
        lint()
            .files(
                integersXml("<integer name=\"custom_type\">2</integer>"),
                xml(
                        "res/values-sw600dp/integers.xml",
                        """
                    <resources>
                        <integer name="custom_type">99</integer>
                    </resources>
                    """,
                    )
                    .indented(),
                widgetInfoXml(
                    """
                    <container type="SMALL" previewImage="@drawable/preview1" />
                    <container type="@integer/custom_type" previewImage="@drawable/preview2" />
                    """
                ),
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:3: Error: Duplicate container types are not allowed. Type '@integer/custom_type' is duplicated. [WearWidgetDuplicateContainerType]
                    <container type="@integer/custom_type" previewImage="@drawable/preview2" />
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    res/xml/wear_widget_info.xml:2: Previously defined here
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    private fun widgetInfoXml(containers: String) =
        xml(
                "res/xml/wear_widget_info.xml",
                "<wearwidget-provider>\n${containers.trimIndent().prependIndent("    ")}\n</wearwidget-provider>",
            )
            .indented()

    private fun integersXml(content: String) =
        xml(
                "res/values/integers.xml",
                "<resources>\n${content.trimIndent().prependIndent("    ")}\n</resources>",
            )
            .indented()

    private fun stringsXml(content: String) =
        xml(
                "res/values/strings.xml",
                "<resources>\n${content.trimIndent().prependIndent("    ")}\n</resources>",
            )
            .indented()
}
