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

package androidx.appfunctions.internal

import androidx.annotation.RestrictTo
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata

/**
 * An interface providing access to metadata for a set of registered AppFunctions.
 *
 * This interface defines a contract for accessing metadata associated with AppFunctions. Each
 * AppFunction implementation class has a corresponding generated inventory class that implements
 * this interface. This inventory class provides a mapping between AppFunction IDs and their
 * respective [androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata].
 *
 * For example, consider the following AppFunction implementation:
 * ```kotlin
 * package com.example.imageeditor
 *
 * class ImageFunctions : RotateImage, BlurImage {
 *   @AppFunction
 *   override suspend fun rotateImage(...): Image { ... }
 *
 *   @AppFunction
 *   override suspend fun blurImage(...): Image? { ... }
 * }
 * ```
 *
 * The generated inventory class for `ImageFunctions` would implement this interface and provide a
 * map containing metadata for both `rotateImage` and `blurImage`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AppFunctionInventory {
    /**
     * A map of function IDs to their corresponding
     * [androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata].
     */
    public val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata>

    /** Reusable components that could be shared within the function specification. */
    public val componentsMetadata: AppFunctionComponentsMetadata
}
