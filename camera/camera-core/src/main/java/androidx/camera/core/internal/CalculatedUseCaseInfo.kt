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

package androidx.camera.core.internal

import androidx.annotation.RestrictTo
import androidx.camera.core.UseCase
import androidx.camera.core.streamsharing.StreamSharing

/**
 * A data class encapsulating the detailed outcome of a UseCase configuration calculation and
 * validation process.
 *
 * @param appUseCases The collection of all [UseCase]s provided by the application.
 * @param cameraUseCases The collection of [UseCase]s currently bound to the camera.
 * @param cameraUseCasesToAttach The [UseCase]s that need to be attached to the camera.
 * @param cameraUseCasesToKeep The [UseCase]s that should remain attached to the camera.
 * @param cameraUseCasesToDetach The [UseCase]s that need to be detached from the camera.
 * @param streamSharing The [StreamSharing] configuration if applicable.
 * @param placeholderForExtensions A placeholder [UseCase] for internal extensions.
 * @param useCaseConfigs A map of [UseCase] to their corresponding
 *   [CameraUseCaseAdapter.ConfigPair].
 * @param primaryStreamSpecResult The primary [StreamSpecQueryResult] for the use cases.
 * @param secondaryStreamSpecResult An optional secondary [StreamSpecQueryResult] for use cases.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class CalculatedUseCaseInfo(
    val appUseCases: Collection<UseCase>,
    val cameraUseCases: Collection<UseCase>,
    val cameraUseCasesToAttach: List<UseCase>,
    val cameraUseCasesToKeep: List<UseCase>,
    val cameraUseCasesToDetach: List<UseCase>,
    val streamSharing: StreamSharing?,
    val placeholderForExtensions: UseCase?,
    val useCaseConfigs: Map<UseCase, CameraUseCaseAdapter.ConfigPair>,
    val primaryStreamSpecResult: StreamSpecQueryResult,
    val secondaryStreamSpecResult: StreamSpecQueryResult?,
)
