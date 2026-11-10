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
class WearWidgetConfigActivityDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = WearWidgetConfigActivityDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(WearWidgetConfigActivityDetector.MISSING_PROVIDER_CONFIG_CATEGORY_ISSUE)

    @Test
    fun xmlConfigIntentAction_withProviderConfigCategory_passes() {
        lint()
            .files(
                manifest(
                        """
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                            package="com.example.wear">
                            <application>
                                <service
                                    android:name=".MyWidgetService"
                                    android:exported="true">
                                    <meta-data
                                        android:name="androidx.glance.wear.widget.provider"
                                        android:resource="@xml/my_widget_info" />
                                </service>
                                <activity
                                    android:name=".MyConfigActivity"
                                    android:exported="true">
                                    <intent-filter>
                                        <action android:name="com.example.wear.ACTION_CONFIGURE" />
                                        <category android:name="android.intent.category.DEFAULT" />
                                        <category android:name="com.google.android.clockwork.tiles.category.PROVIDER_CONFIG" />
                                    </intent-filter>
                                </activity>
                            </application>
                        </manifest>
                        """
                    )
                    .indented(),
                xml(
                        "res/xml/my_widget_info.xml",
                        """
                        <wearwidget-provider
                            configIntentAction="com.example.wear.ACTION_CONFIGURE">
                            <container type="SMALL" previewImage="@drawable/preview" />
                        </wearwidget-provider>
                        """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun xmlConfigIntentAction_missingProviderConfigCategory_fails() {
        lint()
            .files(
                manifest(
                        """
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                            package="com.example.wear">
                            <application>
                                <service
                                    android:name=".MyWidgetService"
                                    android:exported="true">
                                    <meta-data
                                        android:name="androidx.glance.wear.widget.provider"
                                        android:resource="@xml/my_widget_info" />
                                </service>
                                <activity
                                    android:name=".MyConfigActivity"
                                    android:exported="true">
                                    <intent-filter>
                                        <action android:name="com.example.wear.ACTION_CONFIGURE" />
                                        <category android:name="android.intent.category.DEFAULT" />
                                    </intent-filter>
                                </activity>
                            </application>
                        </manifest>
                        """
                    )
                    .indented(),
                xml(
                        "res/xml/my_widget_info.xml",
                        """
                        <wearwidget-provider
                            configIntentAction="com.example.wear.ACTION_CONFIGURE">
                            <container type="SMALL" previewImage="@drawable/preview" />
                        </wearwidget-provider>
                        """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                AndroidManifest.xml:14: Error: Configuration activity intent-filter for action 'com.example.wear.ACTION_CONFIGURE' must include '<category android:name="com.google.android.clockwork.tiles.category.PROVIDER_CONFIG" />' [WearWidgetMissingProviderConfigCategory]
                            <intent-filter>
                            ^
                1 errors, 0 warnings
                """
            )
            .expectFixDiffs(
                """
                Fix for AndroidManifest.xml line 14: Add PROVIDER_CONFIG category:
                @@ -16,0 +17 @@
                +                <category android:name="com.google.android.clockwork.tiles.category.PROVIDER_CONFIG" />
                """
            )
    }

    @Test
    fun wrongProviderConfigCategory_missingClockworkCategory_fails() {
        lint()
            .files(
                manifest(
                        """
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                            package="com.example.wear">
                            <application>
                                <service
                                    android:name=".MyWidgetService"
                                    android:exported="true">
                                    <meta-data
                                        android:name="androidx.glance.wear.widget.provider"
                                        android:resource="@xml/my_widget_info" />
                                </service>
                                <activity
                                    android:name=".MyConfigActivity"
                                    android:exported="true">
                                    <intent-filter>
                                        <action android:name="com.example.wear.ACTION_CONFIGURE" />
                                        <category android:name="android.intent.category.DEFAULT" />
                                        <category android:name="androidx.wear.tiles.category.PROVIDER_CONFIG" />
                                    </intent-filter>
                                </activity>
                            </application>
                        </manifest>
                        """
                    )
                    .indented(),
                xml(
                        "res/xml/my_widget_info.xml",
                        """
                        <wearwidget-provider
                            configIntentAction="com.example.wear.ACTION_CONFIGURE">
                            <container type="SMALL" previewImage="@drawable/preview" />
                        </wearwidget-provider>
                        """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                AndroidManifest.xml:14: Error: Configuration activity intent-filter for action 'com.example.wear.ACTION_CONFIGURE' must include '<category android:name="com.google.android.clockwork.tiles.category.PROVIDER_CONFIG" />' [WearWidgetMissingProviderConfigCategory]
                            <intent-filter>
                            ^
                1 errors, 0 warnings
                """
            )
            .expectFixDiffs(
                """
                Fix for AndroidManifest.xml line 14: Add PROVIDER_CONFIG category:
                @@ -17,0 +18 @@
                +                <category android:name="com.google.android.clockwork.tiles.category.PROVIDER_CONFIG" />
                """
            )
    }

    @Test
    fun activityAlias_missingProviderConfigCategory_fails() {
        lint()
            .files(
                manifest(
                        """
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                            package="com.example.wear">
                            <application>
                                <service
                                    android:name=".MyWidgetService"
                                    android:exported="true">
                                    <meta-data
                                        android:name="androidx.glance.wear.widget.provider"
                                        android:resource="@xml/my_widget_info" />
                                </service>
                                <activity-alias
                                    android:name=".MyConfigAlias"
                                    android:targetActivity=".MyConfigActivity"
                                    android:exported="true">
                                    <intent-filter>
                                        <action android:name="com.example.wear.ACTION_CONFIGURE" />
                                        <category android:name="android.intent.category.DEFAULT" />
                                    </intent-filter>
                                </activity-alias>
                            </application>
                        </manifest>
                        """
                    )
                    .indented(),
                xml(
                        "res/xml/my_widget_info.xml",
                        """
                        <wearwidget-provider
                            configIntentAction="com.example.wear.ACTION_CONFIGURE">
                            <container type="SMALL" previewImage="@drawable/preview" />
                        </wearwidget-provider>
                        """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                AndroidManifest.xml:15: Error: Configuration activity intent-filter for action 'com.example.wear.ACTION_CONFIGURE' must include '<category android:name="com.google.android.clockwork.tiles.category.PROVIDER_CONFIG" />' [WearWidgetMissingProviderConfigCategory]
                            <intent-filter>
                            ^
                1 errors, 0 warnings
                """
            )
            .expectFixDiffs(
                """
                Fix for AndroidManifest.xml line 15: Add PROVIDER_CONFIG category:
                @@ -17,0 +18 @@
                +                <category android:name="com.google.android.clockwork.tiles.category.PROVIDER_CONFIG" />
                """
            )
    }

    @Test
    fun configActionWithNoMatchingActivity_ignored_passes() {
        lint()
            .files(
                manifest(
                        """
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                            package="com.example.wear">
                            <application>
                                <service
                                    android:name=".MyWidgetService"
                                    android:exported="true">
                                    <meta-data
                                        android:name="androidx.glance.wear.widget.provider"
                                        android:resource="@xml/my_widget_info" />
                                </service>
                                <activity
                                    android:name=".MainActivity"
                                    android:exported="true">
                                    <intent-filter>
                                        <action android:name="android.intent.action.MAIN" />
                                        <category android:name="android.intent.category.LAUNCHER" />
                                    </intent-filter>
                                </activity>
                            </application>
                        </manifest>
                        """
                    )
                    .indented(),
                xml(
                        "res/xml/my_widget_info.xml",
                        """
                        <wearwidget-provider
                            configIntentAction="com.example.wear.ACTION_OTHER_CONFIG">
                            <container type="SMALL" previewImage="@drawable/preview" />
                        </wearwidget-provider>
                        """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun stringResourcesInConfigActionAndManifest_missingCategory_fails() {
        lint()
            .files(
                xml(
                        "res/values/strings.xml",
                        """
                        <resources>
                            <string name="config_action">com.example.wear.ACTION_CONFIGURE</string>
                        </resources>
                        """,
                    )
                    .indented(),
                manifest(
                        """
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                            package="com.example.wear">
                            <application>
                                <activity
                                    android:name=".MyConfigActivity"
                                    android:exported="true">
                                    <intent-filter>
                                        <action android:name="@string/config_action" />
                                        <category android:name="android.intent.category.DEFAULT" />
                                    </intent-filter>
                                </activity>
                            </application>
                        </manifest>
                        """
                    )
                    .indented(),
                xml(
                        "res/xml/my_widget_info.xml",
                        """
                        <wearwidget-provider
                            configIntentAction="@string/config_action">
                            <container type="SMALL" previewImage="@drawable/preview" />
                        </wearwidget-provider>
                        """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                AndroidManifest.xml:7: Error: Configuration activity intent-filter for action 'com.example.wear.ACTION_CONFIGURE' must include '<category android:name="com.google.android.clockwork.tiles.category.PROVIDER_CONFIG" />' [WearWidgetMissingProviderConfigCategory]
                            <intent-filter>
                            ^
                1 errors, 0 warnings
                """
            )
            .expectFixDiffs(
                """
                Fix for AndroidManifest.xml line 7: Add PROVIDER_CONFIG category:
                @@ -9,0 +10 @@
                +                <category android:name="com.google.android.clockwork.tiles.category.PROVIDER_CONFIG" />
                """
            )
    }

    @Test
    fun suppressedWithToolsIgnore_missingProviderConfigCategory_passes() {
        lint()
            .files(
                manifest(
                        """
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                            xmlns:tools="http://schemas.android.com/tools"
                            package="com.example.wear">
                            <application>
                                <service
                                    android:name=".MyWidgetService"
                                    android:exported="true">
                                    <meta-data
                                        android:name="androidx.glance.wear.widget.provider"
                                        android:resource="@xml/my_widget_info" />
                                </service>
                                <activity
                                    android:name=".MyConfigActivity"
                                    android:exported="true">
                                    <intent-filter tools:ignore="WearWidgetMissingProviderConfigCategory">
                                        <action android:name="com.example.wear.ACTION_CONFIGURE" />
                                        <category android:name="android.intent.category.DEFAULT" />
                                    </intent-filter>
                                </activity>
                            </application>
                        </manifest>
                        """
                    )
                    .indented(),
                xml(
                        "res/xml/my_widget_info.xml",
                        """
                        <wearwidget-provider
                            configIntentAction="com.example.wear.ACTION_CONFIGURE">
                            <container type="SMALL" previewImage="@drawable/preview" />
                        </wearwidget-provider>
                        """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun singleActivityIntentFilterWithTwoConfigActions_missingCategory_reportsOnce() {
        lint()
            .files(
                manifest(
                        """
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                            package="com.example.wear">
                            <application>
                                <activity
                                    android:name=".SharedConfigActivity"
                                    android:exported="true">
                                    <intent-filter>
                                        <action android:name="com.example.wear.ACTION_CONFIGURE_1" />
                                        <action android:name="com.example.wear.ACTION_CONFIGURE_2" />
                                        <category android:name="android.intent.category.DEFAULT" />
                                    </intent-filter>
                                </activity>
                            </application>
                        </manifest>
                        """
                    )
                    .indented(),
                xml(
                        "res/xml/widget_1_info.xml",
                        """
                        <wearwidget-provider
                            configIntentAction="com.example.wear.ACTION_CONFIGURE_1">
                            <container type="SMALL" previewImage="@drawable/preview" />
                        </wearwidget-provider>
                        """,
                    )
                    .indented(),
                xml(
                        "res/xml/widget_2_info.xml",
                        """
                        <wearwidget-provider
                            configIntentAction="com.example.wear.ACTION_CONFIGURE_2">
                            <container type="SMALL" previewImage="@drawable/preview" />
                        </wearwidget-provider>
                        """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                AndroidManifest.xml:7: Error: Configuration activity intent-filter for action 'com.example.wear.ACTION_CONFIGURE_1' must include '<category android:name="com.google.android.clockwork.tiles.category.PROVIDER_CONFIG" />' [WearWidgetMissingProviderConfigCategory]
                            <intent-filter>
                            ^
                1 errors, 0 warnings
                """
            )
            .expectFixDiffs(
                """
                Fix for AndroidManifest.xml line 7: Add PROVIDER_CONFIG category:
                @@ -10,0 +11 @@
                +                <category android:name="com.google.android.clockwork.tiles.category.PROVIDER_CONFIG" />
                """
            )
    }
}
