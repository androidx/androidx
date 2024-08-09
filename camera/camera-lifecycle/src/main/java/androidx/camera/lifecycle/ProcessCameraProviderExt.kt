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

package androidx.camera.lifecycle

import android.content.Context
import androidx.camera.core.InitializationException
import androidx.concurrent.futures.await

/**
 * Retrieves the [ProcessCameraProvider].
 *
 * This is a suspending function unlike [ProcessCameraProvider.getInstance] which returns a
 * [com.google.common.util.concurrent.ListenableFuture].
 *
 * @param context The application context.
 * @return A fully initialized ProcessCameraProvider for the current process.
 * @throws InitializationException If failed to retrieve the ProcessCameraProvider, use
 *   [InitializationException.cause] to get the error cause.
 * @see ProcessCameraProvider.getInstance
 */
public suspend fun ProcessCameraProvider.Companion.awaitInstance(
    context: Context
): ProcessCameraProvider = getInstance(context).await()
