/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalRetryPolicy;
import androidx.camera.core.RetryPolicy;

/**
 * Internal interface for constructing tailored RetryPolicies.
 * @see RetryPolicy
 */
@ExperimentalRetryPolicy
public interface RetryPolicyInternal extends RetryPolicy {

    /**
     * Creates a RetryPolicy that mirrors retry logic but enforces a new timeout.
     *
     * @param timeoutInMillis The maximum duration for retries in milliseconds.
     * @return A RetryPolicy that seamlessly integrates the inherited retry logic with the
     * newly specified timeout.
     */
    @NonNull
    RetryPolicy copy(long timeoutInMillis);
}
