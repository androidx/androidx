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

package androidx.ui.core.pointerinput;

/**
 * The enumeration of passes where [PointerInputChange] traverses up and down the UI tree.
 */
// TODO(shepshapard): Convert to kotlin when kotlin enums don't cause an issue where Activities
// can't be found.
public enum PointerEventPass {
    InitialDown, PreUp, PreDown, PostUp, PostDown
}
