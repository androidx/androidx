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

package androidx.biometric.auth;

/**
 * A handle to the prompt that is shown while the user is authenticating.
 *
 * <p>This interface is common across all sub-types of authentication prompts.
 */
public interface AuthPrompt {
    /**
     * Cancels an ongoing authentication attempt and dismisses the prompt.
     */
    void cancelAuthentication();
}
