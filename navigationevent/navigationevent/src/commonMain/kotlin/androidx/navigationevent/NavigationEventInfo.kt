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

package androidx.navigationevent

/**
 * Provides contextual information about a navigation state (e.g., a screen or route).
 *
 * Implement this interface on objects that represent a specific state in your UI. This allows you
 * to associate custom data with system navigation events.
 */
public interface NavigationEventInfo {

    /**
     * A default used when no specific information is associated with a navigation event.
     *
     * This serves as a null object when context about the UI state is unavailable or not needed.
     */
    public object NotProvided : NavigationEventInfo
}
