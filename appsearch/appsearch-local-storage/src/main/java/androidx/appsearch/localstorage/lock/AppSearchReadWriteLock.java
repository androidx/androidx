/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appsearch.localstorage.lock;

import androidx.annotation.RestrictTo;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A read-write lock that uses an {@link UpgradeableReadWriteLock} if the upgradable lock is
 * enabled, or a {@link ReentrantReadWriteLock} otherwise.
 */
// TODO(b/448657720): Remove this class and use UpgradeableReadWriteLock directly once the
//  upgradable lock is fully rolled out.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AppSearchReadWriteLock implements ReadWriteLock {
    private final boolean mEnableUpgradableLock;
    // The lock used before the upgradable lock was introduced. Used when the upgradable lock is
    // disabled.
    private final ReentrantReadWriteLock mReentrantReadWriteLock = new ReentrantReadWriteLock();
    private final UpgradeableReadWriteLock mUpgradeableReadWriteLock =
            new UpgradeableReadWriteLock();

    /**
     * Creates an {@link AppSearchReadWriteLock}.
     *
     * @param enableUpgradableLock whether to use {@link UpgradeableReadWriteLock} instead of
     *     {@link ReentrantReadWriteLock}.
     */
    public AppSearchReadWriteLock(boolean enableUpgradableLock) {
        mEnableUpgradableLock = enableUpgradableLock;
    }

    /** Returns the read lock. */
    @Override
    public @NonNull Lock readLock() {
        return mEnableUpgradableLock
                ? mUpgradeableReadWriteLock.readLock() : mReentrantReadWriteLock.readLock();
    }

    /** Returns the write lock. */
    @Override
    public @NonNull Lock writeLock() {
        return mEnableUpgradableLock
                ? mUpgradeableReadWriteLock.writeLock() : mReentrantReadWriteLock.writeLock();
    }

    /**
     * Returns the upgrade lock of the {@link UpgradeableReadWriteLock}.
     *
     * @throws IllegalStateException if the upgradable lock is not enabled.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_UPGRADABLE_LOCK_AND_TWO_STAGE_OPTIMIZE)
    public UpgradeableReadWriteLock.@NonNull UpgradeLock upgradeLock() {
        if (!mEnableUpgradableLock) {
            throw new IllegalStateException("The upgradable lock is not enabled.");
        }
        return mUpgradeableReadWriteLock.upgradeLock();
    }
}
