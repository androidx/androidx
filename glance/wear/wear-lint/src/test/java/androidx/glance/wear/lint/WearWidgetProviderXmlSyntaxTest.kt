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
class WearWidgetProviderXmlSyntaxTest : LintDetectorTest() {

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
    fun validXml_passes() {
        lint()
            .files(
                xml(
                        "res/xml/wear_widget_info.xml",
                        """
                    <wearwidget-provider>
                        <container type="1" previewImage="@drawable/preview" />
                    </wearwidget-provider>
                    """,
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun nonWearWidgetXml_passes() {
        lint()
            .files(
                xml(
                        "res/xml/appwidget_info.xml",
                        """
                    <appwidget-provider>
                    </appwidget-provider>
                    """,
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun missingContainer_fails() {
        lint()
            .files(
                xml(
                        "res/xml/wear_widget_info.xml",
                        """
                    <wearwidget-provider>
                    </wearwidget-provider>
                    """,
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:1: Error: Wear widget provider info must include at least one <container> tag [WearWidgetMissingContainer]
                <wearwidget-provider>
                ^
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun missingPreviewImageOnContainer_fails() {
        lint()
            .files(
                xml(
                        "res/xml/wear_widget_info.xml",
                        """
                    <wearwidget-provider>
                        <container type="1" />
                    </wearwidget-provider>
                    """,
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:2: Error: This <container> tag is missing the 'previewImage' attribute [WearWidgetMissingPreviewImage]
                    <container type="1" />
                    ~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun missingContainerType_fails() {
        lint()
            .files(
                xml(
                        "res/xml/wear_widget_info.xml",
                        """
                    <wearwidget-provider>
                        <container previewImage="@drawable/preview" />
                    </wearwidget-provider>
                    """,
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                res/xml/wear_widget_info.xml:2: Error: This <container> tag is missing the 'type' attribute [WearWidgetMissingContainerType]
                    <container previewImage="@drawable/preview" />
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }
}
