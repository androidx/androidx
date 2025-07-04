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

@file:JvmName("RxJava3ViewCamera")

package androidx.xr.arcore.rxjava3

import androidx.xr.arcore.ViewCamera
import io.reactivex.rxjava3.core.Flowable
import kotlinx.coroutines.rx3.asFlowable

/** The current [State] of the view camera. */
public val ViewCamera.stateAsFlowable: Flowable<ViewCamera.State>
    get() = state.asFlowable()
