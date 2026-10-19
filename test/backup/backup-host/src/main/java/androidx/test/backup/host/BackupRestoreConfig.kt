/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

/**
 * Defines global configuration parameters for the Automated Backup and Restore test runner suite.
 *
 * Applies to the annotated test class and to its [@Nested][org.junit.jupiter.api.Nested] test
 * classes, which can override individual attributes.
 *
 * Tests whose test suite properties name the tested app don't need this annotation: without it,
 * they target that app.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
public annotation class BackupRestoreConfig(
    /**
     * The target application ID/package name being tested, such as "com.example.myapp". Defaults to
     * "", which uses the `applicationId` of an enclosing class, or else the tested app that the
     * test suite properties name.
     */
    public val applicationId: String = "",
    /**
     * The package name of the test APK whose `androidx.test.backup.BackupRestoreTestRunner` runs
     * the device actions, such as "com.example.myapp.test". Needed only when several installed test
     * APKs have that runner for the app. Defaults to "", which uses the `testApplicationId` of the
     * nearest enclosing class that sets one, unless this class or a class in between sets
     * `applicationId`. Without one, it uses the only such test APK, or else the one named
     * `<applicationId>.test`. Device actions fail if the test APK that it names isn't installed or
     * has no such runner for the app.
     */
    public val testApplicationId: String = "",
)
