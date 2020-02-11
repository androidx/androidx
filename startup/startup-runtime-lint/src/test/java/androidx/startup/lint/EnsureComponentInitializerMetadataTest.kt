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

package androidx.startup.lint

import androidx.startup.lint.Stubs.TEST_COMPONENT
import androidx.startup.lint.Stubs.COMPONENT_INITIALIZER
import androidx.startup.lint.Stubs.TEST_COMPONENT_2
import androidx.startup.lint.Stubs.TEST_COMPONENT_WITH_DEPENDENCIES
import com.android.tools.lint.checks.infrastructure.TestFiles.manifest
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class EnsureComponentInitializerMetadataTest {
    @Test
    fun testFailureWhenNoMetadataIsProvided() {
        lint()
            .files(
                COMPONENT_INITIALIZER,
                TEST_COMPONENT
            )
            .issues(EnsureComponentInitializerMetadataDetector.ISSUE)
            .run()
            /* ktlint-disable max-line-length */
            .expect(
                """
                src/com/example/TestComponentInitializer.kt:5: Error: Every ComponentInitializer needs to be accompanied by a corresponding <meta-data> entry in the AndroidManifest.xml file. [EnsureComponentInitializerMetadata]
                class TestComponentInitializer: ComponentInitializer<Unit> {
                ^
                1 errors, 0 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun testSuccessWhenMetadataIsProvided() {
        val manifest = manifest(
            """
               <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                  xmlns:tools="http://schemas.android.com/tools"
                  package="com.example">
                  <application>
                    <meta-data
                        android:name="com.example.TestComponentInitializer"
                        android:value="androidx.startup" />
                  </application>
                </manifest>
        """
        ).indented()

        lint()
            .files(
                COMPONENT_INITIALIZER,
                TEST_COMPONENT,
                manifest
            )
            .issues(EnsureComponentInitializerMetadataDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testSuccessWhenInCompleteMetadataIsProvided() {
        val manifest = manifest(
            """
               <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                  xmlns:tools="http://schemas.android.com/tools"
                  package="com.example">
                  <application>
                    <meta-data
                        android:name="com.example.TestComponentInitializer"
                        android:value="androidx.startup" />
                  </application>
                </manifest>
        """
        ).indented()

        lint()
            .files(
                COMPONENT_INITIALIZER,
                TEST_COMPONENT_WITH_DEPENDENCIES,
                TEST_COMPONENT_2,
                manifest
            )
            .issues(EnsureComponentInitializerMetadataDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testSuccessWhenMetadataIsProvidedWithStringResourceName() {
        val manifest = manifest(
            """
               <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                  xmlns:tools="http://schemas.android.com/tools"
                  package="com.example">
                  <application>
                    <meta-data
                        android:name="com.example.TestComponentInitializer"
                        android:value="@string/androidx_startup" />
                  </application>
                </manifest>
        """
        ).indented()

        lint()
            .files(
                COMPONENT_INITIALIZER,
                TEST_COMPONENT,
                manifest
            )
            .issues(EnsureComponentInitializerMetadataDetector.ISSUE)
            .run()
            .expectClean()
    }
}
