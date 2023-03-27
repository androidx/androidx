/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

/**
 * Interface responsible for managing quota. Before initiating some action (e.g. starting an
 * animation) that uses a limited resource, {@link #tryAcquireQuota} should be called to see if the
 * target quota cap has been reached and if not, it will be updated. When action/resource is
 * finished, it should be release with {@link #releaseQuota}.
 *
 * <p>For example, this can be used for limiting the number of concurrently running animations.
 * Every time new animations is due to be played, it should request quota from {@link QuotaManager}
 * in amount that is equal to the number of animations that should be played (e.g. playing fade in
 * and slide in animation on one object should request amount of 2 quota.
 *
 * <p>It is callers responsibility to release acquired quota after limited resource has been
 * finished. For example, when animation is running, but surface became invisible, caller should
 * return acquired quota.
 */
public interface QuotaManager {

    /**
     * Tries to acquire the given amount of quota and returns true if successful. Otherwise, returns
     * false meaning that quota cap has already been reached and the quota won't be acquired.
     *
     * <p>It is callers responsibility to release acquired quota after limited resource has been
     * finished. For example, when animation is running, but surface became invisible, caller should
     * return acquired quota.
     */
    boolean tryAcquireQuota(int quota);

    /**
     * Releases the given amount of quota.
     *
     * @throws IllegalArgumentException if the given quota amount exceeds the amount of acquired
     *     quota.
     */
    void releaseQuota(int quota);
}
