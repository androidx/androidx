/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.animation.animations

import androidx.ui.VoidCallback
import androidx.ui.animation.Animation
import androidx.ui.animation.AnimationStatus
import androidx.ui.animation.AnimationStatusListener

/**
 * An animation that is always stopped at a given value.
 *
 * The [status] is always [AnimationStatus.FORWARD].
 *
 * Since the [value] and [status] of an [AlwaysStoppedAnimation] can never
 * change, the listeners can never be called. It is therefore safe to reuse
 * an [AlwaysStoppedAnimation] instance in multiple places. If the [value] to
 * be used is known at compile time, the constructor should be called as a
 * `const` constructor.
 */
class AlwaysStoppedAnimation<T>(
    override val value: T
) : Animation<T>() {

    override fun addListener(listener: VoidCallback) {}

    override fun removeListener(listener: VoidCallback) {}

    override fun addStatusListener(listener: AnimationStatusListener) {}

    override fun removeStatusListener(listener: AnimationStatusListener) {}

    override val status = AnimationStatus.FORWARD

    override fun toStringDetails() = "${super.toStringDetails()} $value; paused"
}