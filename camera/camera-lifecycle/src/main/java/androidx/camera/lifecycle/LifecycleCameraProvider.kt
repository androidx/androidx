/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.camera.core.CameraProvider
import androidx.camera.core.UseCase
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

/**
 * Provides access to a camera which has has its opening and closing controlled by a
 * [LifecycleOwner].
 */
internal interface LifecycleCameraProvider : CameraProvider {
    /**
     * Returns `true` if the [UseCase] is bound to a lifecycle. Otherwise returns `false`.
     *
     * After binding a use case, use cases remain bound until the lifecycle reaches a
     * [Lifecycle.State.DESTROYED] state or if is unbound by calls to [unbind] or [unbindAll].
     */
    fun isBound(useCase: UseCase): Boolean

    /**
     * Unbinds all specified use cases from the lifecycle provider.
     *
     * This will initiate a close of every open camera which has zero [UseCase] associated with it
     * at the end of this call.
     *
     * If a use case in the argument list is not bound, then it is simply ignored.
     *
     * After unbinding a UseCase, the UseCase can be bound to another [Lifecycle] however listeners
     * and settings should be reset by the application.
     *
     * @param useCases The collection of use cases to remove.
     * @throws IllegalStateException If not called on main thread.
     */
    fun unbind(vararg useCases: UseCase?)

    /**
     * Unbinds all use cases from the lifecycle provider and removes them from CameraX.
     *
     * This will initiate a close of every currently open camera.
     *
     * @throws IllegalStateException If not called on main thread.
     */
    fun unbindAll()
}
