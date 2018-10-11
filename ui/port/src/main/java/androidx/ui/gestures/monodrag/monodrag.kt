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

package androidx.ui.gestures.monodrag

import androidx.ui.gestures.drag_details.DragEndDetails

// / Signature for when a pointer that was previously in contact with the screen
// / and moving is no longer in contact with the screen.
// /
// / The velocity at which the pointer was moving when it stopped contacting
// / the screen is available in the `details`.
// /
// / See [DragGestureRecognizer.onEnd].
typealias GestureDragEndCallback = (details: DragEndDetails) -> Unit

// / Signature for when the pointer that previously triggered a
// / [GestureDragDownCallback] did not complete.
// /
// / See [DragGestureRecognizer.onCancel].
typealias GestureDragCancelCallback = () -> Unit