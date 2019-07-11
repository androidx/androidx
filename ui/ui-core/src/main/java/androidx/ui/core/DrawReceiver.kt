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
package androidx.ui.core

/**
 * Receiver scope for [Draw] lambda that allows ordering the child drawing between
 * canvas operations. Most Draw calls don't accept children, but in some rare cases
 * a [Canvas] should be modified for the children. [DrawReceiver] is the receiver scope for
 * the `onPaint` to give access to [drawChildren].
 */
interface DrawReceiver : DensityReceiver {
    /**
     * Causes child drawing operations to run during the `onPaint` lambda.
     */
    fun drawChildren()
}
