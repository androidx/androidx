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

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;

/** Quota manager with fixed quota cap. This class is not thread safe. */
@RestrictTo(Scope.LIBRARY_GROUP)
public class FixedQuotaManagerImpl implements QuotaManager {
    private final int mQuotaCap;

    private int mQuotaCounter = 0;

    /** Creates a {@link FixedQuotaManagerImpl} with the given quota cap. */
    public FixedQuotaManagerImpl(int quotaCap) {
        this.mQuotaCap = quotaCap;
    }

    /**
     * @see QuotaManager#tryAcquireQuota
     *     <p>Note that this method is not thread safe.
     */
    @Override
    public boolean tryAcquireQuota(int quota) {
        if (mQuotaCounter + quota <= mQuotaCap) {
            mQuotaCounter += quota;
            return true;
        } else {
            return false;
        }
    }

    /**
     * @see QuotaManager#releaseQuota
     *     <p>Note that this method is not thread safe.
     */
    @Override
    public void releaseQuota(int quota) {
        if (mQuotaCounter - quota < 0) {
            throw new IllegalArgumentException(
                    "Trying to release more quota than it was acquired!");
        }
        mQuotaCounter -= quota;
    }

    /** Returns true if all quota has been released. */
    @VisibleForTesting
    public boolean isAllQuotaReleased() {
        return mQuotaCounter == 0;
    }

    /** Returns the remaining quota. */
    @VisibleForTesting
    public int getRemainingQuota() {
        return mQuotaCap - mQuotaCounter;
    }
}
