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

package androidx.compose.ui.test.samples

import androidx.annotation.Sampled
import androidx.compose.ui.test.FailureArtifact
import androidx.test.platform.io.PlatformTestStorageRegistry

@Sampled
fun failureArtifactStorageUsageSample(artifact: FailureArtifact) {
    val storage = PlatformTestStorageRegistry.getInstance()

    // Retrieve a Uri to share, delete, or process the file
    val uri = storage.getOutputFileUri(artifact.fileName)

    // Or open an input stream to read the generated file directly
    val inputStream = storage.openInputFile(artifact.fileName)
}
