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

package androidx.ui.gestures.recognizer

// / The possible states of a [PrimaryPointerGestureRecognizer].
// /
// / The recognizer advances from [ready] to [possible] when starts tracking a
// / primary pointer. When the primary pointer is resolve (either accepted or
// / or rejected), the recognizers advances to [defunct]. Once the recognizer
// / has stopped tracking any remaining pointers, the recognizer returns to
// / [ready].
enum class GestureRecognizerState {
    // / The recognizer is ready to start recognizing a gesture.
    ready,

    // / The sequence of pointer events seen thus far is consistent with the
    // / gesture the recognizer is attempting to recognize but the gesture has not
    // / been accepted definitively.
    possible,

    // / Further pointer events cannot cause this recognizer to recognize the
    // / gesture until the recognizer returns to the [ready] state (typically when
    // / all the pointers the recognizer is tracking are removed from the screen).
    defunct
}