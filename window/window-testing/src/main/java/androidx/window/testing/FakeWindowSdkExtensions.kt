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

package androidx.window.testing

import androidx.annotation.IntRange
import androidx.window.WindowSdkExtensions

/**
 * A fake [WindowSdkExtensions] implementation that can override [extensionVersion], which is
 * intended to be used during unit tests.
 */
internal class FakeWindowSdkExtensions : WindowSdkExtensions() {

    override val extensionVersion: Int
        get() = _extensionVersion

    private var _extensionVersion: Int = 0

    internal fun overrideExtensionVersion(@IntRange(from = 0) version: Int) {
        require(version >= 0) { "The override version must equal to or greater than 0." }
        _extensionVersion = version
    }
}
