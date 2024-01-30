/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.mocks.helpers;

import androidx.annotation.RequiresApi;

/**
 * An interface for matching arguments in {@link ArgumentCaptor} class.
 *
 * @param <T> the type of the arguments to capture
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ArgumentMatcher<T> {

    /**
     * Matches an argument according to matching criteria and returns if it is matched or not.
     *
     * @param argument the argument to match according to criteria
     * @return  {@code true} if argument is matched according desired criteria,
     *          {@code false} otherwise
     */
    boolean matches(T argument);
}
