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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpgradeableReadWriteLockUpgradeLockTest {
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
    public void testUpgradeLock_lock_successAndState() throws Exception {
        // Step 1: Acquire the upgrade lock in read mode.
        mLock.upgradeLock().lock();
        try {
            // While held, background reader can acquire read lock concurrently.
            Future<Boolean> backgroundReadLock =
                    mExecutor.submit(
                            () -> {
                                if (mLock.readLock().tryLock()) {
                                    mLock.readLock().unlock();
                                    return true;
                                }
                                return false;
                            });
            assertThat(backgroundReadLock.get(2, TimeUnit.SECONDS)).isTrue();

            // While held, background writer is blocked.
            Future<Boolean> backgroundWriteLock =
                    mExecutor.submit(() -> mLock.writeLock().tryLock());
            assertThat(backgroundWriteLock.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Step 2: Release the upgrade lock.
            mLock.upgradeLock().unlock();
        }

        // After unlock, background writer can acquire the write lock.
        Future<Boolean> backgroundWriteLockAfterUnlock =
                mExecutor.submit(
                        () -> {
                            if (mLock.writeLock().tryLock()) {
                                mLock.writeLock().unlock();
                                return true;
                            }
                            return false;
                        });
        assertThat(backgroundWriteLockAfterUnlock.get(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testUpgradeLock_lockInterruptibly_success() throws Exception {
        // Step 1: Acquire the upgrade lock interruptibly when no thread holds conflicting locks.
        mLock.upgradeLock().lockInterruptibly();
        try {
            // Verify that background writers are blocked, indicating the lock is held.
            Future<Boolean> backgroundWrite = mExecutor.submit(() -> mLock.writeLock().tryLock());
            assertThat(backgroundWrite.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Step 2: Release the upgrade lock.
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void testUpgradeLock_lockInterruptibly_interrupted() throws Exception {
        // Step 1: Main thread acquires the upgrade lock to block subsequent upgrader requests.
        mLock.upgradeLock().lock();
        try {
            AtomicBoolean interruptedThrown = new AtomicBoolean(false);

            // Step 2: Background thread attempts lockInterruptibly() and is blocked by main thread.
            Thread blockedThread =
                    new Thread(
                            () -> {
                                try {
                                    mLock.upgradeLock().lockInterruptibly();
                                    mLock.upgradeLock().unlock();
                                } catch (InterruptedException e) {
                                    interruptedThrown.set(true);
                                }
                            });
            blockedThread.start();

            // Wait deterministically until the background thread enters a blocked/waiting state.
            waitUntilThreadBlocks(blockedThread);

            // Step 3: Interrupt the background thread while it is waiting.
            blockedThread.interrupt();
            blockedThread.join(2000);

            // Verify that InterruptedException was caught and handled by the background thread.
            assertThat(interruptedThrown.get()).isTrue();
        } finally {
            // Release the upgrade lock on the main thread.
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void testUpgradeLock_tryLock_successAndFailure() throws Exception {
        // Step 1: tryLock() succeeds immediately when the lock is free.
        assertThat(mLock.upgradeLock().tryLock()).isTrue();

        try {
            // Step 2: Background thread tryLock() fails immediately while main thread holds the
            // upgrade lock.
            Future<Boolean> backgroundTryLock =
                    mExecutor.submit(() -> mLock.upgradeLock().tryLock());
            assertThat(backgroundTryLock.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Step 3: Release the upgrade lock on main thread.
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void testUpgradeLock_tryLockWithTimeout_successAndTimeout() throws Exception {
        // Step 1: tryLock with timeout succeeds immediately when the lock is free.
        assertThat(mLock.upgradeLock().tryLock(500, TimeUnit.MILLISECONDS)).isTrue();

        try {
            // Step 2: Background thread tryLock with a short timeout fails because main thread
            // holds the upgrade lock.
            Future<Boolean> backgroundTryLockWithTimeout =
                    mExecutor.submit(() -> mLock.upgradeLock().tryLock(50, TimeUnit.MILLISECONDS));
            assertThat(backgroundTryLockWithTimeout.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Step 3: Release the upgrade lock on main thread.
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void testUpgradeLock_tryLockWithTimeout_interrupted() throws Exception {
        // Step 1: Main thread acquires the upgrade lock to block subsequent upgrader requests.
        mLock.upgradeLock().lock();
        try {
            AtomicBoolean interruptedThrown = new AtomicBoolean(false);

            // Step 2: Background thread attempts tryLock with timeout and is blocked by main
            // thread.
            Thread blockedThread =
                    new Thread(
                            () -> {
                                try {
                                    if (mLock.upgradeLock().tryLock(5, TimeUnit.SECONDS)) {
                                        mLock.upgradeLock().unlock();
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
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void testUpgradeLock_upgrade_whenNoOtherThreadHoldsReadLock() throws Exception {
        // Step 1: Acquire the upgrade lock in read mode.
        mLock.upgradeLock().lock();
        try {
            // Step 2: Upgrade to write mode when no other threads hold read locks.
            mLock.upgradeLock().upgrade();

            // Verify that background readers are blocked while upgraded to write mode.
            Future<Boolean> backgroundRead = mExecutor.submit(() -> mLock.readLock().tryLock());
            assertThat(backgroundRead.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Release the upgrade lock (which releases write lock and mutex).
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void testUpgradeLock_upgrade_whenOtherThreadHoldsReadLock_blocksUntilReleased()
            throws Exception {
        // Step 1: Reader thread acquires a regular read lock.
        CountDownLatch readerAcquiredLatch = new CountDownLatch(1);
        CountDownLatch releaseReaderLatch = new CountDownLatch(1);
        CountDownLatch upgraderUpgradeStarted = new CountDownLatch(1);
        AtomicBoolean upgraderCompleted = new AtomicBoolean(false);

        Thread readerThread =
                new Thread(
                        () -> {
                            mLock.readLock().lock();
                            try {
                                readerAcquiredLatch.countDown();
                                releaseReaderLatch.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                mLock.readLock().unlock();
                            }
                        });
        readerThread.start();
        assertThat(readerAcquiredLatch.await(2, TimeUnit.SECONDS)).isTrue();

        // Step 2: Upgrader thread acquires upgrade lock and attempts to upgrade to write mode.
        Thread upgraderThread =
                new Thread(
                        () -> {
                            mLock.upgradeLock().lock();
                            try {
                                upgraderUpgradeStarted.countDown();
                                mLock.upgradeLock().upgrade();
                                upgraderCompleted.set(true);
                            } finally {
                                mLock.upgradeLock().unlock();
                            }
                        });
        upgraderThread.start();
        assertThat(upgraderUpgradeStarted.await(2, TimeUnit.SECONDS)).isTrue();

        // Step 3: Wait until upgrader blocks waiting for reader thread to release its read lock.
        waitUntilThreadBlocks(upgraderThread);

        // Verify upgrader has NOT completed upgrade yet because reader thread still holds read
        // lock.
        assertThat(upgraderCompleted.get()).isFalse();

        // Step 4: Release reader thread's read lock.
        releaseReaderLatch.countDown();
        upgraderThread.join(2000);

        // Verify upgrader completed upgrade successfully after reader released its read lock.
        assertThat(upgraderCompleted.get()).isTrue();
    }

    @Test
    public void testUpgradeLock_upgrade_idempotent() {
        // Step 1: Acquire the upgrade lock in read mode.
        mLock.upgradeLock().lock();
        try {
            // Step 2: First upgrade call transitions to write mode.
            mLock.upgradeLock().upgrade();

            // Step 3: Second upgrade call on the same thread is a no-op (idempotent).
            mLock.upgradeLock().upgrade();
        } finally {
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void testUpgradeLock_upgrade_throwsIfNotHeld() {
        // Calling upgrade without holding the upgrade lock must throw IllegalMonitorStateException.
        assertThrows(IllegalMonitorStateException.class, () -> mLock.upgradeLock().upgrade());
    }

    @Test
    public void testUpgradeLock_upgrade_throwsIfCurrentThreadHoldsExtraReadLocks() {
        // Step 1: Acquire the upgrade lock in read mode.
        mLock.upgradeLock().lock();
        try {
            // Step 2: Current thread acquires an additional regular read lock.
            mLock.readLock().lock();
            try {
                // Step 3: Calling upgrade while holding an extra read lock on the same thread
                // must throw IllegalStateException to prevent self-deadlock.
                assertThrows(IllegalStateException.class, () -> mLock.upgradeLock().upgrade());
            } finally {
                mLock.readLock().unlock();
            }
        } finally {
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void testUpgradeLock_downgrade_successful() throws Exception {
        // Step 1: Acquire the upgrade lock and upgrade to write mode.
        mLock.upgradeLock().lock();
        try {
            mLock.upgradeLock().upgrade();

            // Step 2: Verify that while in write mode, background readers are blocked.
            Future<Boolean> backgroundReadWhileWriting =
                    mExecutor.submit(() -> mLock.readLock().tryLock());
            assertThat(backgroundReadWhileWriting.get(2, TimeUnit.SECONDS)).isFalse();

            // Step 3: Downgrade from write mode back to read mode.
            mLock.upgradeLock().downgrade();

            // Step 4: Verify that after downgrade, background readers can read concurrently.
            Future<Boolean> backgroundReadAfterDowngrade =
                    mExecutor.submit(
                            () -> {
                                if (mLock.readLock().tryLock()) {
                                    mLock.readLock().unlock();
                                    return true;
                                }
                                return false;
                            });
            assertThat(backgroundReadAfterDowngrade.get(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            // Release the upgrade lock.
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void testUpgradeLock_downgrade_throwsIfNotUpgraded() {
        // Step 1: Acquire the upgrade lock in read mode (not upgraded to write mode).
        mLock.upgradeLock().lock();
        try {
            // Calling downgrade when not in write mode must throw IllegalMonitorStateException.
            assertThrows(IllegalMonitorStateException.class, () -> mLock.upgradeLock().downgrade());
        } finally {
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void testUpgradeLock_downgrade_throwsIfNotHeld() {
        // Calling downgrade when the upgrade lock is not held must throw
        // IllegalMonitorStateException.
        assertThrows(IllegalMonitorStateException.class, () -> mLock.upgradeLock().downgrade());
    }

    @Test
    public void testUpgradeLock_downgrade_throwsIfCurrentThreadHoldsNestedWriteLocks() {
        // Step 1: Acquire and upgrade to write mode.
        mLock.upgradeLock().lock();
        try {
            mLock.upgradeLock().upgrade();

            // Step 2: Reentrantly acquire write lock.
            // writeLock() throws IllegalStateException if read locks are held, but here the
            // lock is in write mode (read lock was released during upgrade), so reentrant write
            // acquisition succeeds on the same thread.
            mLock.writeLock().lock();
            try {
                // Step 3: Calling downgrade while holding nested write locks must throw
                // IllegalStateException to prevent lock corruption and leak.
                assertThrows(
                        IllegalStateException.class, () -> mLock.upgradeLock().downgrade());
            } finally {
                mLock.writeLock().unlock();
            }
        } finally {
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void testUpgradeLock_unlock_whenInReadMode_resetsStateAndReleasesMutex()
            throws Exception {
        // Step 1: Acquire the upgrade lock in read mode (without upgrading).
        mLock.upgradeLock().lock();

        // Step 2: Unlock while in read mode.
        mLock.upgradeLock().unlock();

        // Verify that another thread can now acquire the upgrade lock cleanly.
        Future<Boolean> backgroundUpgradeLock =
                mExecutor.submit(
                        () -> {
                            if (mLock.upgradeLock().tryLock()) {
                                mLock.upgradeLock().unlock();
                                return true;
                            }
                            return false;
                        });
        assertThat(backgroundUpgradeLock.get(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testUpgradeLock_unlock_resetsStateAndReleasesMutex() throws Exception {
        // Step 1: Acquire and upgrade the lock.
        mLock.upgradeLock().lock();
        mLock.upgradeLock().upgrade();

        // Step 2: Unlock the upgrade lock.
        mLock.upgradeLock().unlock();

        // Verify that another thread can now acquire the upgrade lock cleanly.
        Future<Boolean> backgroundUpgradeLock =
                mExecutor.submit(
                        () -> {
                            if (mLock.upgradeLock().tryLock()) {
                                mLock.upgradeLock().unlock();
                                return true;
                            }
                            return false;
                        });
        assertThat(backgroundUpgradeLock.get(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testUpgradeLock_reacquire_afterUnlock_sameThread() {
        // Step 1: Acquire and unlock.
        mLock.upgradeLock().lock();
        mLock.upgradeLock().unlock();

        // Step 2: Same thread should be able to acquire upgrade lock again cleanly.
        mLock.upgradeLock().lock();
        try {
            mLock.upgradeLock().upgrade();
            mLock.upgradeLock().downgrade();
        } finally {
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void testUpgradeLock_unlock_throwsIfNotHeld() {
        // Calling unlock without holding the upgrade lock must throw IllegalMonitorStateException.
        assertThrows(IllegalMonitorStateException.class, () -> mLock.upgradeLock().unlock());
    }

    @Test
    public void testUpgradeLock_newCondition_unsupported() {
        // Verify that newCondition() throws UnsupportedOperationException.
        assertThrows(UnsupportedOperationException.class, () -> mLock.upgradeLock().newCondition());
    }
}
