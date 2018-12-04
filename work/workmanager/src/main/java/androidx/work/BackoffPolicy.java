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

package androidx.work;

import java.util.concurrent.TimeUnit;

/**
 * An enumeration of backoff policies when retrying work.  These policies are used when you have a
 * return {@link Result#retry()} from a worker to determine the correct backoff time.  Backoff
 * policies are set in {@link WorkRequest.Builder#setBackoffCriteria(BackoffPolicy, long, TimeUnit)}
 * or one of its variants.
 */

public enum BackoffPolicy {

    /**
     * Used to indicate that {@link WorkManager} should increase the backoff time exponentially
     */
    EXPONENTIAL,

    /**
     * Used to indicate that {@link WorkManager} should increase the backoff time linearly
     */
    LINEAR
}
