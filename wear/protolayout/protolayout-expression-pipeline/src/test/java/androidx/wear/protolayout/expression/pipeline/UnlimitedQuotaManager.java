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

package androidx.wear.protolayout.expression.pipeline;

/** Default, unlimited quota manager implementation that always returns true. */
public class UnlimitedQuotaManager implements QuotaManager {
    private int mQuotaCounter = 0;

    /**
     * @see QuotaManager#tryAcquireQuota
     *     <p>Note that this method is not thread safe.
     */
    @Override
    public boolean tryAcquireQuota(int quota) {
        mQuotaCounter += quota;
        return true;
    }

    /**
     * @see QuotaManager#releaseQuota
     *     <p>Note that this method is not thread safe.
     */
    @Override
    public void releaseQuota(int quota) {
        mQuotaCounter -= quota;
    }

    /** Returns true if all quota has been released. */
    public boolean isAllQuotaReleased() {
        return mQuotaCounter == 0;
    }
}
