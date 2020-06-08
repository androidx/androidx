/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core.keyinput

/**
 * When a user presses a key on a hardware keyboard, a [KeyEvent] is sent to the
 * [KeyInputModifier] that is currently active.
 *
 * @param key the key that was pressed.
 * @param type the [type][KeyEventType] of key event.
 */
class KeyEvent(val key: Key, val type: KeyEventType)

/**
 * The type of Key Event.
 */
enum class KeyEventType { KeyUp, KeyDown }