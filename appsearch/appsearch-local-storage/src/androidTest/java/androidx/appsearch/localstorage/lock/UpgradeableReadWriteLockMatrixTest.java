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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class UpgradeableReadWriteLockMatrixTest {
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
    public void testConstructor_fairPolicy() {
        UpgradeableReadWriteLock fairLock = new UpgradeableReadWriteLock(true);
        assertThat(fairLock.readLock()).isNotNull();
        assertThat(fairLock.writeLock()).isNotNull();
        assertThat(fairLock.upgradeLock()).isNotNull();
    }

    @Test
    public void testConstructor_nonFairPolicy() {
        UpgradeableReadWriteLock nonFairLock = new UpgradeableReadWriteLock(false);
        assertThat(nonFairLock.readLock()).isNotNull();
        assertThat(nonFairLock.writeLock()).isNotNull();
        assertThat(nonFairLock.upgradeLock()).isNotNull();
    }

    // =========================================================================================
    // Category 1: Acquiring Read Lock
    // =========================================================================================

    @Test
    public void getReadLock_afterReadLock_sameThread() {
        // Acquire the first read lock.
        mLock.readLock().lock();
        try {
            // Verify that the same thread can reentrantly acquire a second read lock.
            assertThat(mLock.readLock().tryLock()).isTrue();
            mLock.readLock().unlock();
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Test
    public void getReadLock_afterReadLock_differentThread() throws Exception {
        // Step 1: Thread A (main thread) acquires a read lock.
        mLock.readLock().lock();
        try {
            // Step 2: Thread B attempts to acquire a read lock concurrently.
            Future<Boolean> threadBAcquiredReadLock =
                    mExecutor.submit(
                            () -> {
                                if (mLock.readLock().tryLock()) {
                                    mLock.readLock().unlock();
                                    return true;
                                }
                                return false;
                            });

            // Verify Thread B successfully acquired the read lock.
            assertThat(threadBAcquiredReadLock.get(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            // Release Thread A's read lock.
            mLock.readLock().unlock();
        }
    }

    @Test
    public void getReadLock_afterWriteLock_sameThread() {
        // Acquire write lock first.
        mLock.writeLock().lock();
        try {
            // Verify that lock downgrading is supported on the same thread: a thread holding the
            // write lock can reentrantly acquire the read lock.
            assertThat(mLock.readLock().tryLock()).isTrue();
            mLock.readLock().unlock();
        } finally {
            mLock.writeLock().unlock();
        }
    }

    @Test
    public void getReadLock_afterWriteLock_differentThread() throws Exception {
        // Step 1: Thread A (main thread) acquires the write lock.
        mLock.writeLock().lock();
        try {
            // Step 2: Thread B attempts to acquire a read lock.
            // While Thread A holds the write lock, Thread B cannot acquire a read lock.
            Future<Boolean> threadBAcquiredReadLock =
                    mExecutor.submit(() -> mLock.readLock().tryLock());

            // Verify Thread B failed to acquire the read lock.
            assertThat(threadBAcquiredReadLock.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Release Thread A's write lock.
            mLock.writeLock().unlock();
        }
    }

    @Test
    public void getReadLock_afterUpgradeLock_sameThread() {
        // Acquire the upgrade lock in read mode.
        mLock.upgradeLock().lock();
        try {
            // Verify that the same thread can also acquire a regular read lock while holding the
            // upgrade lock.
            assertThat(mLock.readLock().tryLock()).isTrue();
            mLock.readLock().unlock();
        } finally {
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void getReadLock_afterUpgradeLock_differentThread() throws Exception {
        // Step 1: Thread A (main thread) acquires the upgrade lock in read mode.
        mLock.upgradeLock().lock();
        try {
            // Step 2: Thread B attempts to acquire a read lock concurrently.
            // While Thread A holds the upgrade lock in read mode, other threads can still read
            // concurrently.
            Future<Boolean> threadBAcquiredReadLock =
                    mExecutor.submit(
                            () -> {
                                if (mLock.readLock().tryLock()) {
                                    mLock.readLock().unlock();
                                    return true;
                                }
                                return false;
                            });

            // Verify Thread B successfully acquired the read lock concurrently.
            assertThat(threadBAcquiredReadLock.get(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            // Release Thread A's upgrade lock.
            mLock.upgradeLock().unlock();
        }
    }

    // =========================================================================================
    // Category 2: Acquiring Write Lock
    // =========================================================================================

    @Test
    public void getWriteLock_afterReadLock_sameThread() {
        // Acquire a read lock first.
        mLock.readLock().lock();
        try {
            // Standard JDK ReentrantReadWriteLock does not allow upgrading from readLock to
            // writeLock on the same thread without releasing the readLock first.
            assertThrows(IllegalStateException.class, () -> mLock.writeLock().lock());
            assertThrows(
                    IllegalStateException.class, () -> mLock.writeLock().lockInterruptibly());
            assertThat(mLock.writeLock().tryLock()).isFalse();
            assertThrows(
                    IllegalStateException.class,
                    () -> mLock.writeLock().tryLock(10, TimeUnit.MILLISECONDS));
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Test
    public void getWriteLock_afterReadLock_differentThread() throws Exception {
        // Step 1: Thread A (main thread) acquires a read lock.
        mLock.readLock().lock();
        try {
            // Step 2: Thread B attempts to acquire the write lock.
            // While Thread A holds the read lock, Thread B cannot acquire the write lock.
            Future<Boolean> threadBAcquiredWriteLock =
                    mExecutor.submit(() -> mLock.writeLock().tryLock());

            // Verify Thread B failed to acquire the write lock.
            assertThat(threadBAcquiredWriteLock.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Release Thread A's read lock.
            mLock.readLock().unlock();
        }
    }

    @Test
    public void getWriteLock_afterWriteLock_sameThread() {
        // Acquire write lock first.
        mLock.writeLock().lock();
        try {
            // Verify that write lock is reentrant on the same thread.
            assertThat(mLock.writeLock().tryLock()).isTrue();
            mLock.writeLock().unlock();
        } finally {
            mLock.writeLock().unlock();
        }
    }

    @Test
    public void getWriteLock_afterWriteLock_differentThread() throws Exception {
        // Step 1: Thread A (main thread) acquires the write lock.
        mLock.writeLock().lock();
        try {
            // Step 2: Thread B attempts to acquire the write lock.
            // While Thread A holds the write lock, Thread B cannot acquire the write lock.
            Future<Boolean> threadBAcquiredWriteLock =
                    mExecutor.submit(() -> mLock.writeLock().tryLock());

            // Verify Thread B failed to acquire the write lock.
            assertThat(threadBAcquiredWriteLock.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Release Thread A's write lock.
            mLock.writeLock().unlock();
        }
    }

    @Test
    public void getWriteLock_afterUpgradeLock_sameThread() {
        // Acquire the upgrade lock in read mode.
        mLock.upgradeLock().lock();
        try {
            // Cannot directly acquire regular writeLock() while holding upgradeLock in read mode
            // because writeLock() requires acquiring mUpgradeMutex (already held by upgradeLock).
            // Upgrading must be performed via upgradeLock().upgrade().
            assertThrows(IllegalStateException.class, () -> mLock.writeLock().lock());
            assertThrows(
                    IllegalStateException.class, () -> mLock.writeLock().lockInterruptibly());
            assertThat(mLock.writeLock().tryLock()).isFalse();
            assertThrows(
                    IllegalStateException.class,
                    () -> mLock.writeLock().tryLock(10, TimeUnit.MILLISECONDS));
        } finally {
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void getWriteLock_afterUpgradeLock_differentThread() throws Exception {
        // Step 1: Thread A (main thread) acquires the upgrade lock in read mode.
        mLock.upgradeLock().lock();
        try {
            // Step 2: Thread B attempts to acquire the write lock.
            // While Thread A holds the upgrade lock (which includes read authorization), Thread B
            // cannot acquire the write lock.
            Future<Boolean> threadBAcquiredWriteLock =
                    mExecutor.submit(() -> mLock.writeLock().tryLock());

            // Verify Thread B failed to acquire the write lock.
            assertThat(threadBAcquiredWriteLock.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Release Thread A's upgrade lock.
            mLock.upgradeLock().unlock();
        }
    }

    // =========================================================================================
    // Category 3: Acquiring Upgrade Lock
    // =========================================================================================

    @Test
    public void getUpgradeLock_afterReadLock_sameThread() {
        // Acquire a regular read lock first.
        mLock.readLock().lock();
        try {
            // To prevent potential deadlocks, acquire upgrade lock while holding a regular read
            // lock is forbidden.
            assertThrows(IllegalStateException.class, () -> mLock.upgradeLock().lock());
            assertThrows(
                    IllegalStateException.class, () -> mLock.upgradeLock().lockInterruptibly());
            assertThat(mLock.upgradeLock().tryLock()).isFalse();
            try {
                assertThat(mLock.upgradeLock().tryLock(10, TimeUnit.MILLISECONDS)).isFalse();
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Test
    public void getUpgradeLock_afterReadLock_differentThread() throws Exception {
        // Step 1: Thread A (main thread) acquires a read lock.
        mLock.readLock().lock();
        try {
            // Step 2: Thread B attempts to acquire the upgrade lock.
            // The upgrade lock grants read access, so it can be acquired concurrently with existing
            // read locks.
            Future<Boolean> threadBAcquiredUpgradeLock =
                    mExecutor.submit(
                            () -> {
                                if (mLock.upgradeLock().tryLock()) {
                                    mLock.upgradeLock().unlock();
                                    return true;
                                }
                                return false;
                            });

            // Verify Thread B successfully acquired the upgrade lock.
            assertThat(threadBAcquiredUpgradeLock.get(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            // Release Thread A's read lock.
            mLock.readLock().unlock();
        }
    }

    @Test
    public void getUpgradeLock_afterWriteLock_sameThread() {
        // Acquire write lock first.
        mLock.writeLock().lock();
        try {
            // Acquire upgrade lock while holding the write lock is forbidden.
            assertThrows(IllegalStateException.class, () -> mLock.upgradeLock().lock());
            assertThrows(
                    IllegalStateException.class, () -> mLock.upgradeLock().lockInterruptibly());
            assertThat(mLock.upgradeLock().tryLock()).isFalse();
            try {
                assertThat(mLock.upgradeLock().tryLock(10, TimeUnit.MILLISECONDS)).isFalse();
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        } finally {
            mLock.writeLock().unlock();
        }
    }

    @Test
    public void getUpgradeLock_afterWriteLock_differentThread() throws Exception {
        // Step 1: Thread A (main thread) acquires the write lock.
        mLock.writeLock().lock();
        try {
            // Step 2: Thread B attempts to acquire the upgrade lock.
            // While Thread A holds the write lock, Thread B cannot acquire the upgrade lock.
            Future<Boolean> threadBAcquiredUpgradeLock =
                    mExecutor.submit(() -> mLock.upgradeLock().tryLock());

            // Verify Thread B failed to acquire the upgrade lock.
            assertThat(threadBAcquiredUpgradeLock.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Release Thread A's write lock.
            mLock.writeLock().unlock();
        }
    }

    @Test
    public void getUpgradeLock_afterUpgradeLock_sameThread() {
        // Acquire the upgrade lock in read mode.
        mLock.upgradeLock().lock();
        try {
            // Upgrade lock is explicitly non-reentrant.
            assertThrows(IllegalStateException.class, () -> mLock.upgradeLock().lock());
            assertThrows(
                    IllegalStateException.class, () -> mLock.upgradeLock().lockInterruptibly());
            assertThat(mLock.upgradeLock().tryLock()).isFalse();
            try {
                assertThat(mLock.upgradeLock().tryLock(10, TimeUnit.MILLISECONDS)).isFalse();
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        } finally {
            mLock.upgradeLock().unlock();
        }
    }

    @Test
    public void getUpgradeLock_afterUpgradeLock_differentThread() throws Exception {
        // Step 1: Thread A (main thread) acquires the upgrade lock.
        mLock.upgradeLock().lock();
        try {
            // Step 2: Thread B attempts to acquire the upgrade lock.
            // Only one thread may hold the upgrade lock at any time (enforced by the internal
            // mutex).
            Future<Boolean> threadBAcquiredUpgradeLock =
                    mExecutor.submit(() -> mLock.upgradeLock().tryLock());

            // Verify Thread B failed to acquire the upgrade lock.
            assertThat(threadBAcquiredUpgradeLock.get(2, TimeUnit.SECONDS)).isFalse();
        } finally {
            // Release Thread A's upgrade lock.
            mLock.upgradeLock().unlock();
        }
    }
}
