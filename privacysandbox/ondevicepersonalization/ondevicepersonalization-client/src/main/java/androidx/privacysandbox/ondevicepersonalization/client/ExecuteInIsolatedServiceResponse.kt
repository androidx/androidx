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

package androidx.privacysandbox.ondevicepersonalization.client

import android.adservices.ondevicepersonalization.SurfacePackageToken

/**
 * The response of [OnDevicePersonalizationManager.executeInIsolatedService].
 *
 * @param surfacePackageToken An opaque reference to content that can be displayed in a
 *   [android.view.SurfaceView]. This may be `null` if the Isolated Service has not generated any
 *   content to be displayed within the calling app.
 */
public class ExecuteInIsolatedServiceResponse
internal constructor(public val surfacePackageToken: SurfacePackageToken?)
