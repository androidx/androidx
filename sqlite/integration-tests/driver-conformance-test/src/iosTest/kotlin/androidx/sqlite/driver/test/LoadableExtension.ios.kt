/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.sqlite.driver.test

import platform.Foundation.NSBundle

/**
 * Gets the compiled native library filename of the test SQLite extension (sqlite_extension.cpp).
 *
 * The name `sqliteExtension` is based on the native compilation name defined in the `build.gradle`
 * if it changes, so should this.
 *
 * TODO(b/433546325): Expose or generate named based of Gradle configuration
 */
actual fun getExtensionFileName(): String =
    NSBundle.mainBundle().resourcePath + "/libsqliteExtension.dylib"
