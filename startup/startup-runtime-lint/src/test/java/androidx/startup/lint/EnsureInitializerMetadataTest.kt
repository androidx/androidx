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

@file:Suppress("UnstableApiUsage")

package androidx.startup.lint

import androidx.startup.lint.Stubs.TEST_INITIALIZER
import androidx.startup.lint.Stubs.INITIALIZER
import androidx.startup.lint.Stubs.TEST_INITIALIZER_2
import androidx.startup.lint.Stubs.TEST_INITIALIZER_WITH_DEPENDENCIES
import com.android.tools.lint.checks.infrastructure.TestFiles.manifest
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Ignore
import org.junit.Test

class EnsureInitializerMetadataTest {
    @Ignore("b/187539166")
    @Test
    fun testFailureWhenNoMetadataIsProvided() {
        lint()
            .files(
                INITIALIZER,
                TEST_INITIALIZER
            )
            .issues(EnsureInitializerMetadataDetector.ISSUE)
            .run()
            /* ktlint-disable max-line-length */
            .expect(
                """
                src/com/example/TestInitializer.kt:5: Error: Every Initializer needs to be accompanied by a corresponding <meta-data> entry in the AndroidManifest.xml file. [EnsureInitializerMetadata]
                class TestInitializer: Initializer<Unit> {
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
                    <provider
                        android:name="androidx.startup.InitializationProvider"
                        android:authorities="com.example.androidx-startup"
                        android:exported="false"
                        tools:node="merge">
                        <meta-data
                            android:name="com.example.TestInitializer"
                            android:value="androidx.startup" />
                    </provider>
                  </application>
                </manifest>
        """
        ).indented()

        lint()
            .files(
                INITIALIZER,
                TEST_INITIALIZER,
                manifest
            )
            .issues(EnsureInitializerMetadataDetector.ISSUE)
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
                    <provider
                        android:name="androidx.startup.InitializationProvider"
                        android:authorities="com.example.androidx-startup"
                        android:exported="false"
                        tools:node="merge">
                        <meta-data
                            android:name="com.example.TestInitializer"
                            android:value="androidx.startup" />
                    </provider>
                  </application>
                </manifest>
        """
        ).indented()

        lint()
            .files(
                INITIALIZER,
                TEST_INITIALIZER_WITH_DEPENDENCIES,
                TEST_INITIALIZER_2,
                manifest
            )
            .issues(EnsureInitializerMetadataDetector.ISSUE)
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
                    <provider
                        android:name="androidx.startup.InitializationProvider"
                        android:authorities="com.example.androidx-startup"
                        android:exported="false"
                        tools:node="merge">
                        <meta-data
                            android:name="com.example.TestInitializer"
                            android:value="@string/androidx_startup" />
                    </provider>
                  </application>
                </manifest>
        """
        ).indented()

        lint()
            .files(
                INITIALIZER,
                TEST_INITIALIZER,
                manifest
            )
            .issues(EnsureInitializerMetadataDetector.ISSUE)
            .run()
            .expectClean()
    }
}
