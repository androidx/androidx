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

import androidx.annotation.IntRange;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

/**
 * Utility for defining the number of invocations allowed while testing fake class methods.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CallTimes {
    protected int mTimes;

    /**
     * Creates a new instance of {@link CallTimes} with the given parameter.
     *
     * @param times the number of invocations allowed
     */

    public CallTimes(@IntRange(from = 0) int times) {
        Preconditions.checkArgument(times >= 0, "The invocation times can not be negative.");
        mTimes = times;
    }

    public int getTimes() {
        return mTimes;
    }

    /**
     * Checks if the number of invocation is exactly the same as specified.
     *
     * @param actualCallCount the occurred number of invocations
     *
     * @return {@code true} if the number of invocations is exactly the same as specified,
     *          {@code false} otherwise
     */
    public boolean isSatisfied(@IntRange(from = 0) int actualCallCount) {
        Preconditions.checkArgument(actualCallCount >= 0, "The invocation times can not be "
                + "negative.");
        return actualCallCount == mTimes;
    }
}
