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

package androidx.camera.core;

import static java.lang.annotation.RetentionPolicy.CLASS;

import androidx.annotation.experimental.Experimental;

import java.lang.annotation.Retention;

/**
 * Denotes that the annotated method uses an experimental API that swaps the default threading
 * implementation with a user-defined threading implementation.
 *
 * <p>These APIs may not always be compatible with all user-defined implementations. For example.
 * using a main thread executor, such as the one returned by
 * {@link androidx.core.content.ContextCompat#getMainExecutor(android.content.Context)} may cause
 * undesired behavior such as causing the app's UI to stutter.
 *
 * <p>CameraX often provides default threading implementations that are optimized for the given
 * API, so customizing threads should only be done by advanced users with very specific threading
 * requirements.
 */
@Retention(CLASS)
@Experimental
public @interface ExperimentalCustomizableThreads {
}
