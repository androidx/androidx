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
package androidx.activity

import androidx.lifecycle.LifecycleOwner

/**
 * A class that has an [OnBackPressedDispatcher] that allows you to register a
 * [OnBackPressedCallback] for handling the system back button.
 *
 * It is expected that classes that implement this interface route the system back button
 * to the dispatcher
 *
 * @see OnBackPressedDispatcher
 */
interface OnBackPressedDispatcherOwner : LifecycleOwner {
    /**
     * The [OnBackPressedDispatcher] that should handle the system back button.
     */
    val onBackPressedDispatcher: OnBackPressedDispatcher
}
