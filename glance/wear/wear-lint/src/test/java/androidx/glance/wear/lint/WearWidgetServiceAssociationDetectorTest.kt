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
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WearWidgetServiceAssociationDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = WearWidgetServiceAssociationDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(WearWidgetServiceAssociationDetector.SERVICE_ASSOCIATION_ANNOTATION_ISSUE)

    private val glanceWearWidgetServiceStub: TestFile =
        kotlin(
                """
        package androidx.glance.wear

        abstract class GlanceWearWidgetService
        """
            )
            .indented()

    private val associateAnnotationStub: TestFile =
        kotlin(
                """
        package androidx.glance.wear

        import kotlin.reflect.KClass

        annotation class AssociateWithGlanceWearWidget(val value: KClass<*>)
        """
            )
            .indented()

    @Test
    fun testServiceWithAnnotation_validWidget_passes() {
        lint()
            .files(
                glanceWearWidgetServiceStub,
                associateAnnotationStub,
                kotlin(
                        """
                    package com.example

                    import androidx.glance.wear.AssociateWithGlanceWearWidget
                    import androidx.glance.wear.GlanceWearWidgetService

                    class MyWidget

                    @AssociateWithGlanceWearWidget(MyWidget::class)
                    class MyWidgetService : GlanceWearWidgetService()
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testServiceWithoutAnnotation_reports() {
        lint()
            .files(
                glanceWearWidgetServiceStub,
                associateAnnotationStub,
                kotlin(
                        """
                    package com.example

                    import androidx.glance.wear.GlanceWearWidgetService

                    class MyWidgetService : GlanceWearWidgetService()
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/MyWidgetService.kt:5: Error: GlanceWearWidgetService subclasses must declare their associated widget using @AssociateWithGlanceWearWidget [GlanceWearWidgetAnnotationMissing]
                class MyWidgetService : GlanceWearWidgetService()
                      ~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testAbstractServiceWithoutAnnotation_passes() {
        lint()
            .files(
                glanceWearWidgetServiceStub,
                associateAnnotationStub,
                kotlin(
                        """
                    package com.example

                    import androidx.glance.wear.GlanceWearWidgetService

                    abstract class BaseWidgetService : GlanceWearWidgetService()
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testAnonymousService_passes() {
        lint()
            .files(
                glanceWearWidgetServiceStub,
                associateAnnotationStub,
                kotlin(
                        """
                    package com.example

                    import androidx.glance.wear.GlanceWearWidgetService

                    val service = object : GlanceWearWidgetService() {}
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }
}
