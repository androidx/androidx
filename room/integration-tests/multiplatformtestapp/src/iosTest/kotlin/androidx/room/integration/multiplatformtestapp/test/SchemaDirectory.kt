/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.integration.multiplatformtestapp.test

import platform.Foundation.NSBundle

/**
 * Gets the schema directory path for tests with [androidx.room.testing.MigrationTestHelper].
 *
 * For iOS, it will be the main resource directory in the bundle.
 */
internal actual fun getSchemaDirectoryPath(): String {
    return checkNotNull(NSBundle.mainBundle().resourcePath) + "/schemas-ksp"
}
