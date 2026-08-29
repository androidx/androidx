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

import static androidx.appsearch.localstorage.lock.UpgradeableReadWriteLockTestHelper.waitUntilThreadBlocks;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpgradeableReadWriteLockWriteLockTest {
    private UpgradeableReadWriteLock mLock;
    private ExecutorService mExecutor;

    @Before
    public void setUp() {
        mLock = new UpgradeableReadWriteLock();
        mExecutor = Executors.newCachedThreadPool();
    }

    @After
    public void tearDown() {
        mExecutor.shutdown();
    }

    @Test
    public void testWriteLock_lock_successAndExclusion() throws Exception {
        // Step 1: Main thread acquires the write lock.
        mLock.writeLock().lock();
        try {
            // Verify background readers, writers, and upgraders are all blocked.
            Future<Boolean> backgroundRead = mExecutor.submit(() -> mLock.readLock().tryLock());
            Future<Boolean> backgroundWrite = mExecutor.submit(() -> mLock.writeLock().tryLock());
            Future<Boolean> backgroundUpgrade =
                    mExecutor.submit(() -> mLock.upgradeLock().tryLock());

            assertThat(backgroundRead.get(2, TimeUnit.SECONDS)).isFalse();
            assertThat(backgroundWrite.get(2, TimeUnit.SECONDS)).isFalse();
            assertThat(backgroundUpgrade.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Step 2: Release the write lock.
            mLock.writeLock().unlock();
        }

        // After unlock, background thread can acquire write lock.
        Future<Boolean> backgroundWriteAfterUnlock =
                mExecutor.submit(
                        () -> {
                            if (mLock.writeLock().tryLock()) {
                                mLock.writeLock().unlock();
                                return true;
                            }
                            return false;
                        });
        assertThat(backgroundWriteAfterUnlock.get(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testWriteLock_lockInterruptibly_success() throws Exception {
        // Step 1: Acquire write lock interruptibly when no other thread holds conflicting locks.
        mLock.writeLock().lockInterruptibly();
        try {
            // Verify write exclusivity is in effect.
            Future<Boolean> backgroundRead = mExecutor.submit(() -> mLock.readLock().tryLock());
            assertThat(backgroundRead.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Step 2: Release the write lock.
            mLock.writeLock().unlock();
        }
    }

    @Test
    public void testWriteLock_lockInterruptibly_interrupted() throws Exception {
        // Step 1: Main thread acquires the write lock to block subsequent write requests.
        mLock.writeLock().lock();
        try {
            AtomicBoolean interruptedThrown = new AtomicBoolean(false);

            // Step 2: Background thread attempts lockInterruptibly() on writeLock and blocks.
            Thread blockedThread =
                    new Thread(
                            () -> {
                                try {
                                    mLock.writeLock().lockInterruptibly();
                                    mLock.writeLock().unlock();
                                } catch (InterruptedException e) {
                                    interruptedThrown.set(true);
                                }
                            });
            blockedThread.start();

            // Wait until the background thread enters a blocked/waiting state.
            waitUntilThreadBlocks(blockedThread);

            // Step 3: Interrupt the background thread while it is waiting.
            blockedThread.interrupt();
            blockedThread.join(2000);

            // Verify that InterruptedException was caught.
            assertThat(interruptedThrown.get()).isTrue();
        } finally {
            mLock.writeLock().unlock();
        }
    }

    @Test
    public void testWriteLock_tryLock_successAndFailure() throws Exception {
        // Step 1: tryLock() succeeds immediately when the lock is free.
        assertThat(mLock.writeLock().tryLock()).isTrue();

        try {
            // Step 2: Background thread tryLock() fails immediately while write lock is held.
            Future<Boolean> backgroundTryLock = mExecutor.submit(() -> mLock.writeLock().tryLock());
            assertThat(backgroundTryLock.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Step 3: Release the write lock.
            mLock.writeLock().unlock();
        }
    }

    @Test
    public void testWriteLock_tryLockWithTimeout_successAndTimeout() throws Exception {
        // Step 1: tryLock with timeout succeeds immediately when the lock is free.
        assertThat(mLock.writeLock().tryLock(500, TimeUnit.MILLISECONDS)).isTrue();

        try {
            // Step 2: Background thread tryLock with a short timeout fails because write lock is
            // held.
            Future<Boolean> backgroundTryLockWithTimeout =
                    mExecutor.submit(() -> mLock.writeLock().tryLock(50, TimeUnit.MILLISECONDS));
            assertThat(backgroundTryLockWithTimeout.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Step 3: Release the write lock.
            mLock.writeLock().unlock();
        }
    }

    @Test
    public void testWriteLock_tryLockWithTimeout_interrupted() throws Exception {
        // Step 1: Main thread acquires the write lock to block subsequent write requests.
        mLock.writeLock().lock();
        try {
            AtomicBoolean interruptedThrown = new AtomicBoolean(false);

            // Step 2: Background thread attempts tryLock with timeout and blocks.
            Thread blockedThread =
                    new Thread(
                            () -> {
                                try {
                                    if (mLock.writeLock().tryLock(5, TimeUnit.SECONDS)) {
                                        mLock.writeLock().unlock();
                                    }
                                } catch (InterruptedException e) {
                                    interruptedThrown.set(true);
                                }
                            });
            blockedThread.start();

            // Wait until the background thread enters a blocked/waiting state.
            waitUntilThreadBlocks(blockedThread);

            // Step 3: Interrupt the background thread while it is waiting.
            blockedThread.interrupt();
            blockedThread.join(2000);

            // Verify that InterruptedException was caught.
            assertThat(interruptedThrown.get()).isTrue();
        } finally {
            mLock.writeLock().unlock();
        }
    }

    @Test
    public void testWriteLock_tryLock_whenReaderHeld_releasesUpgradeMutexAndReturnsFalse()
            throws Exception {
        // Step 1: Main thread holds a regular read lock.
        mLock.readLock().lock();
        try {
            // Step 2: Background thread attempts writeLock().tryLock().
            // mUpgradeMutex is acquired, but mRwLock.writeLock().tryLock() fails due to readLock.
            // WriteLock.tryLock() must release mUpgradeMutex and return false.
            Future<Boolean> writeTryLock = mExecutor.submit(() -> mLock.writeLock().tryLock());
            assertThat(writeTryLock.get(2, TimeUnit.SECONDS)).isFalse();

            // Step 3: Verify mUpgradeMutex was properly released by having another thread
            // acquire the upgradeLock concurrently with the read lock.
            Future<Boolean> upgradeTryLock =
                    mExecutor.submit(
                            () -> {
                                if (mLock.upgradeLock().tryLock()) {
                                    mLock.upgradeLock().unlock();
                                    return true;
                                }
                                return false;
                            });
            assertThat(upgradeTryLock.get(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Test
    public void testWriteLock_tryLockWithTimeout_whenReaderHeld_timesOutAndReleasesMutex()
            throws Exception {
        // Step 1: Main thread holds a regular read lock.
        mLock.readLock().lock();
        try {
            // Step 2: Background thread attempts writeLock().tryLock(50, ms).
            // Times out waiting for read lock release, and must release mUpgradeMutex before
            // return.
            Future<Boolean> writeTryLockWithTimeout =
                    mExecutor.submit(() -> mLock.writeLock().tryLock(50, TimeUnit.MILLISECONDS));
            assertThat(writeTryLockWithTimeout.get(2, TimeUnit.SECONDS)).isFalse();

            // Step 3: Verify mUpgradeMutex was properly released after timeout.
            Future<Boolean> upgradeTryLock =
                    mExecutor.submit(
                            () -> {
                                if (mLock.upgradeLock().tryLock()) {
                                    mLock.upgradeLock().unlock();
                                    return true;
                                }
                                return false;
                            });
            assertThat(upgradeTryLock.get(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Test
    public void testWriteLock_reentrancy_multipleHoldsRequireMatchingUnlocks() throws Exception {
        // Step 1: Acquire write lock twice on the same thread (reentrant).
        mLock.writeLock().lock();
        mLock.writeLock().lock();

        try {
            // Step 2: Unlock once. The write lock should still be held.
            mLock.writeLock().unlock();

            Future<Boolean> backgroundRead = mExecutor.submit(() -> mLock.readLock().tryLock());
            assertThat(backgroundRead.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Step 3: Second unlock releases the write lock completely.
            mLock.writeLock().unlock();
        }

        // After all locks are released, background thread can read.
        Future<Boolean> backgroundReadAfterFullUnlock =
                mExecutor.submit(
                        () -> {
                            if (mLock.readLock().tryLock()) {
                                mLock.readLock().unlock();
                                return true;
                            }
                            return false;
                        });
        assertThat(backgroundReadAfterFullUnlock.get(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testWriteLock_lock_throwsIfCurrentThreadHoldsReadLock() {
        // Step 1: Main thread holds regular read lock.
        mLock.readLock().lock();
        try {
            // Step 2: Attempting to acquire write lock while holding a read lock must throw
            // IllegalStateException to prevent self-deadlock.
            assertThrows(IllegalStateException.class, () -> mLock.writeLock().lock());
            assertThrows(
                    IllegalStateException.class, () -> mLock.writeLock().lockInterruptibly());
            assertThrows(
                    IllegalStateException.class,
                    () -> mLock.writeLock().tryLock(10, TimeUnit.MILLISECONDS));
            assertThat(mLock.writeLock().tryLock()).isFalse();
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Test
    public void testWriteLock_lock_throwsIfCurrentThreadHoldsUpgradeLock() {
        // Step 1: Main thread holds upgrade lock in read mode.
        mLock.upgradeLock().lock();
        try {
            // Step 2: Attempting to acquire regular write lock while holding upgrade lock must
            // throw IllegalStateException (upgrading must be done via upgradeLock().upgrade()).
            assertThrows(IllegalStateException.class, () -> mLock.writeLock().lock());
            assertThrows(
                    IllegalStateException.class, () -> mLock.writeLock().lockInterruptibly());
            assertThrows(
                    IllegalStateException.class,
                    () -> mLock.writeLock().tryLock(10, TimeUnit.MILLISECONDS));
            assertThat(mLock.writeLock().tryLock()).isFalse();
        } finally {
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void testWriteLock_unlock_throwsIfNotHeld() {
        // Calling unlock on writeLock when not held throws IllegalMonitorStateException.
        assertThrows(IllegalMonitorStateException.class, () -> mLock.writeLock().unlock());
    }

    @Test
    public void testWriteLock_newCondition_unsupported() {
        assertThrows(UnsupportedOperationException.class, () -> mLock.writeLock().newCondition());
    }
}
