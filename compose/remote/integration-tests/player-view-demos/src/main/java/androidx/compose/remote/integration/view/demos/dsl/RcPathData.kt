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

package androidx.compose.remote.integration.view.demos.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.creation.RemotePathBase

/**
 * Utility class in DSL to help with creation of path data for remotePathData(). Implements
 * RcPlatformServices.RcPathArrayCreator.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("RestrictedApiAndroidX") // Referring to drawLine, drawPath, remote-creation
class RcPathData : RemotePathBase, RcPlatformServices.RcPathArrayCreator {
    private var customCreator: RcPlatformServices.RcPathArrayCreator? = null

    constructor() : super()

    constructor(bufferSize: Int) : super(bufferSize)

    constructor(pathData: String) : super() {
        customCreator = parsePath(pathData)
    }

    override fun createFloatArray(): FloatArray {
        return customCreator?.createFloatArray() ?: wrappedRemotePath.createFloatArray()
    }
}
