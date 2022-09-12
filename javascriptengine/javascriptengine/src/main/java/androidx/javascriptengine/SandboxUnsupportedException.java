/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.javascriptengine;

import androidx.annotation.NonNull;

/**
 * Exception thrown when attempting to create a {@link JavaScriptSandbox} via
 * {@link JavaScriptSandbox#createConnectedInstanceAsync(Context)} when doing so is not supported.
 * <p>
 * This can occur when the WebView package is too old to provide a sandbox implementation.
 */
public final class SandboxUnsupportedException extends RuntimeException {
    public SandboxUnsupportedException(@NonNull String error) {
        super(error);
    }
}
