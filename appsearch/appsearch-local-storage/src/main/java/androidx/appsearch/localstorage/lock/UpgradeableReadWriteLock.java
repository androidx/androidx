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

import org.jspecify.annotations.NonNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A read-write lock that supports an upgradeable read lock.
 *
 * <p>This lock allows multiple readers or a single writer, similar to {@link
 * ReentrantReadWriteLock}, but also provides an {@link UpgradeLock} via {@link #upgradeLock()}.
 *
 * <p>Holding the upgrade lock allows a thread to read while holding exclusive rights to upgrade to
 * write mode via {@link UpgradeLock#upgrade()} without deadlocking with other upgraders and without
 * allowing external writers to interleave during the upgrade transition (zero writer gap).
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * UpgradeableReadWriteLock lock = new UpgradeableReadWriteLock();
 * lock.upgradeLock().lock();
 * try {
 *     // Perform read operation (e.g. copy data to temp location)
 *     if (needToUpdate) {
 *         // Zero gap: no external writer can interleave before this completes
 *         lock.upgradeLock().upgrade();
 *         // Perform write operation
 *         lock.upgradeLock().downgrade(); // optional downgrade back to read mode
 *     }
 * } finally {
 *     lock.upgradeLock().unlock();
 * }
 * }</pre>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class UpgradeableReadWriteLock {
    private final ReentrantReadWriteLock mRwLock;
    private final ReentrantLock mUpgradeMutex;
    private final UpgradeLock mUpgradeLock;
    private final WriteLock mWriteLock;

    // State of the UpgradeLock (guarded by mUpgradeMutex):
    // mHoldingUpgradeLock: true if mUpgradeMutex is currently held via an UpgradeLock
    // mIsUpgraded: true if the UpgradeLock has been upgraded to write mode
    private boolean mHoldingUpgradeLock;
    private boolean mIsUpgraded;

    /** Creates a new non-fair {@link UpgradeableReadWriteLock}. */
    public UpgradeableReadWriteLock() {
        this(false);
    }

    /**
     * Creates a new {@link UpgradeableReadWriteLock} with the given fairness policy.
     *
     * <p>When {@code fair} is {@code true}, lock acquisition ordering favors the longest-waiting
     * thread. Under a fair policy, threads attempting to acquire locks will fail (for non-blocking
     * {@code tryLock} invocations) or block if any other threads are already queued and waiting for
     * access.
     *
     * <p>When {@code fair} is {@code false} (non-fair mode), threads attempting lock acquisition
     * can barge ahead of queued waiting threads if the lock becomes available at the exact moment
     * of the request. Non-fair locks generally offer higher throughput at the risk of potential
     * thread starvation.
     *
     * @param fair {@code true} if this lock should use a fair ordering policy; {@code false} for a
     *     non-fair policy.
     */
    public UpgradeableReadWriteLock(boolean fair) {
        mRwLock = new ReentrantReadWriteLock(fair);
        mUpgradeMutex = new ReentrantLock(fair);
        mUpgradeLock = new UpgradeLock();
        mWriteLock = new WriteLock();
    }

    /**
     * Returns the read lock associated with this upgradeable read-write lock.
     *
     * <p>Multiple threads can hold read locks concurrently as long as no thread holds a write lock
     * or is in an upgraded write state.
     *
     * @return the read lock.
     */
    public @NonNull Lock readLock() {
        return mRwLock.readLock();
    }

    /**
     * Returns the write lock associated with this upgradeable read-write lock.
     *
     * <p>Only a single thread can hold the write lock at any time.
     *
     * <p>When a write lock is held, no other thread can acquire read, write, or upgrade locks.
     *
     * @return the write lock.
     */
    public @NonNull Lock writeLock() {
        return mWriteLock;
    }

    /**
     * Returns the upgrade lock associated with this upgradeable read-write lock.
     *
     * <p>The upgrade lock grants read access while allowing the holding thread to safely upgrade to
     * write access via {@link UpgradeLock#upgrade()} with zero writer gap.
     *
     * @return the upgrade lock.
     */
    public @NonNull UpgradeLock upgradeLock() {
        return mUpgradeLock;
    }

    /**
     * The write lock for {@link UpgradeableReadWriteLock}.
     *
     * <p>This lock coordinates with {@link UpgradeLock} to provide mutual exclusion among writers
     * and between writers and readers, while guaranteeing zero writer interleaving during lock
     * upgrades.
     *
     * <p>To prevent race conditions and ensure that no external writer can write while an {@link
     * UpgradeLock} is in the process of upgrading from read to write mode, this lock acquires both
     * the internal upgrade mutex ({@code mUpgradeMutex}) and the underlying write lock ({@code
     * mRwLock.writeLock()}).
     */
    public class WriteLock implements Lock {
        /**
         * Acquires the write lock.
         *
         * <p>Acquires the internal upgrade mutex first, followed by the underlying write lock. If
         * an {@link UpgradeLock} is currently held by another thread (even in read mode), this
         * method blocks on the upgrade mutex until the upgrade lock is fully released.
         *
         * <p>Blocks until all concurrent readers and writers have released their locks.
         *
         * @throws IllegalStateException if the current thread holds a read lock or upgrade lock.
         */
        @Override
        public void lock() {
            if (mRwLock.getReadHoldCount() > 0) {
                throw new IllegalStateException(
                        "Cannot acquire write lock while holding a read lock or upgrade lock"
                                + " (deadlock risk)");
            }
            // Acquire the upgrade mutex first so that regular writers cannot enter or interleave
            // while an UpgradeLock is held or in the middle of upgrading.
            mUpgradeMutex.lock();
            try {
                mRwLock.writeLock().lock();
            } catch (Throwable t) {
                mUpgradeMutex.unlock();
                throw t;
            }
        }

        /**
         * Acquires the write lock unless the current thread is interrupted.
         *
         * <p>Acquires the internal upgrade mutex first, followed by the underlying write lock.
         *
         * @throws InterruptedException if the current thread is interrupted while acquiring either
         *     the upgrade mutex or the write lock.
         * @throws IllegalStateException if the current thread holds a read lock or upgrade lock.
         */
        @Override
        public void lockInterruptibly() throws InterruptedException {
            if (mRwLock.getReadHoldCount() > 0) {
                throw new IllegalStateException(
                        "Cannot acquire write lock while holding a read lock or upgrade lock"
                                + " (deadlock risk)");
            }
            mUpgradeMutex.lockInterruptibly();
            try {
                mRwLock.writeLock().lockInterruptibly();
            } catch (Throwable t) {
                mUpgradeMutex.unlock();
                throw t;
            }
        }

        /**
         * Acquires the write lock only if both the upgrade mutex and write lock are free at the
         * time of invocation.
         *
         * <p>Returns immediately with {@code true} if the write lock was successfully acquired, or
         * {@code false} if another thread holds the upgrade lock, a read lock, or a write lock, or
         * if the current thread holds a read lock or upgrade lock.
         *
         * @return {@code true} if the write lock was acquired; {@code false} otherwise.
         */
        @Override
        public boolean tryLock() {
            if (mRwLock.getReadHoldCount() > 0) {
                return false;
            }
            // Try acquiring the upgrade mutex first. If an UpgradeLock is held, this fails
            // immediately.
            if (!mUpgradeMutex.tryLock()) {
                return false;
            }
            try {
                if (!mRwLock.writeLock().tryLock()) {
                    mUpgradeMutex.unlock();
                    return false;
                }
                return true;
            } catch (Throwable t) {
                mUpgradeMutex.unlock();
                throw t;
            }
        }

        /**
         * Acquires the write lock if it becomes free within the given waiting time and the current
         * thread has not been interrupted.
         *
         * @param time the maximum time to wait for the write lock.
         * @param unit the time unit of the time argument.
         * @return {@code true} if the write lock was acquired; {@code false} if the waiting time
         *     elapsed before lock acquisition.
         * @throws InterruptedException if the current thread is interrupted while waiting.
         * @throws IllegalStateException if the current thread holds a read lock or upgrade lock.
         */
        @Override
        public boolean tryLock(long time, @NonNull TimeUnit unit) throws InterruptedException {
            if (mRwLock.getReadHoldCount() > 0) {
                throw new IllegalStateException(
                        "Cannot acquire write lock while holding a read lock or upgrade lock"
                                + " (deadlock risk)");
            }
            long startTime = System.nanoTime();
            if (!mUpgradeMutex.tryLock(time, unit)) {
                return false;
            }
            try {
                long elapsed = System.nanoTime() - startTime;
                long remaining = unit.toNanos(time) - elapsed;
                if (!mRwLock.writeLock().tryLock(Math.max(0, remaining), TimeUnit.NANOSECONDS)) {
                    mUpgradeMutex.unlock();
                    return false;
                }
                return true;
            } catch (Throwable t) {
                mUpgradeMutex.unlock();
                throw t;
            }
        }

        /**
         * Releases the write lock and the associated upgrade mutex.
         *
         * <p>Releases the underlying write lock first, then releases the upgrade mutex to allow
         * subsequent writers or upgraders to proceed.
         *
         * @throws IllegalMonitorStateException if the current thread does not hold the write lock.
         */
        @Override
        public void unlock() {
            try {
                mRwLock.writeLock().unlock();
            } finally {
                mUpgradeMutex.unlock();
            }
        }

        /**
         * Throws {@link UnsupportedOperationException} as condition variables are not supported on
         * {@link WriteLock}.
         *
         * <p>Because {@code WriteLock} holds both an internal upgrade mutex and the underlying
         * write lock, standard {@link Condition#await()} would only release the underlying write
         * lock while keeping the upgrade mutex held, causing deadlocks for other threads attempting
         * to acquire the write lock to signal the condition.
         *
         * @return never returns normally.
         * @throws UnsupportedOperationException always.
         */
        @Override
        public @NonNull Condition newCondition() {
            throw new UnsupportedOperationException("Conditions are not supported on WriteLock");
        }
    }

    /**
     * A lock that permits read access and provides atomic upgrade and downgrade capabilities with
     * zero writer gap.
     *
     * <p>An {@code UpgradeLock} allows a single thread to hold read access while reserving the
     * exclusive right to upgrade to write access via {@link #upgrade()}.
     *
     * <p>Only one thread may hold the upgrade lock at a time. Other threads can still hold regular
     * {@link #readLock()} read locks concurrently until the upgrade lock is upgraded to write mode.
     *
     * <p>The {@code UpgradeLock} is non-reentrant: a thread holding the upgrade lock cannot acquire
     * it a second time without releasing it first.
     */
    public class UpgradeLock implements Lock {
        /**
         * Acquires the upgrade lock.
         *
         * <p>Blocks if another thread holds the upgrade lock or a write lock. Upon return, the
         * calling thread holds read authorization.
         *
         * <p>This method must not be called reentrantly on the same thread, nor should it be called
         * while holding a regular read lock or write lock to avoid deadlock risks.
         *
         * @throws IllegalStateException if the upgrade lock is already held by the current thread
         *     or if the current thread holds a regular read lock or write lock.
         */
        @Override
        public void lock() {
            if (mUpgradeMutex.isHeldByCurrentThread() && mHoldingUpgradeLock) {
                throw new IllegalStateException("UpgradeLock is not reentrant");
            }
            if (mRwLock.getReadHoldCount() > 0) {
                throw new IllegalStateException(
                        "Cannot acquire upgrade lock while holding a regular read lock"
                                + " (deadlock risk)");
            }
            if (mRwLock.isWriteLockedByCurrentThread()) {
                throw new IllegalStateException(
                        "Cannot acquire upgrade lock while holding the write lock");
            }
            mUpgradeMutex.lock();
            try {
                mRwLock.readLock().lock();
                mHoldingUpgradeLock = true;
                mIsUpgraded = false;
            } catch (Throwable t) {
                mUpgradeMutex.unlock();
                throw t;
            }
        }

        /**
         * Acquires the upgrade lock unless the current thread is interrupted.
         *
         * <p>Blocks if another thread holds the upgrade lock or a write lock. Upon return, the
         * calling thread holds read authorization.
         *
         * @throws InterruptedException if the current thread is interrupted while waiting.
         * @throws IllegalStateException if the upgrade lock is already held by the current thread
         *     or if the current thread holds a regular read lock or write lock.
         */
        @Override
        public void lockInterruptibly() throws InterruptedException {
            if (mUpgradeMutex.isHeldByCurrentThread() && mHoldingUpgradeLock) {
                throw new IllegalStateException("UpgradeLock is not reentrant");
            }
            if (mRwLock.getReadHoldCount() > 0) {
                throw new IllegalStateException(
                        "Cannot acquire upgrade lock while holding a regular read lock"
                                + " (deadlock risk)");
            }
            if (mRwLock.isWriteLockedByCurrentThread()) {
                throw new IllegalStateException(
                        "Cannot acquire upgrade lock while holding the write lock");
            }
            mUpgradeMutex.lockInterruptibly();
            try {
                mRwLock.readLock().lockInterruptibly();
                mHoldingUpgradeLock = true;
                mIsUpgraded = false;
            } catch (Throwable t) {
                mUpgradeMutex.unlock();
                throw t;
            }
        }

        /**
         * Acquires the upgrade lock only if it is free and available at the time of invocation.
         *
         * <p>Returns immediately with {@code true} if the upgrade lock was acquired, or {@code
         * false} if another thread holds the upgrade lock, a write lock, or if the current thread
         * holds a regular read lock or write lock.
         *
         * @return {@code true} if the lock was acquired; {@code false} otherwise.
         */
        @Override
        public boolean tryLock() {
            if ((mUpgradeMutex.isHeldByCurrentThread() && mHoldingUpgradeLock)
                    || mRwLock.getReadHoldCount() > 0
                    || mRwLock.isWriteLockedByCurrentThread()) {
                return false;
            }
            if (!mUpgradeMutex.tryLock()) {
                return false;
            }
            try {
                if (!mRwLock.readLock().tryLock()) {
                    mUpgradeMutex.unlock();
                    return false;
                }
                mHoldingUpgradeLock = true;
                mIsUpgraded = false;
                return true;
            } catch (Throwable t) {
                mUpgradeMutex.unlock();
                throw t;
            }
        }

        /**
         * Acquires the upgrade lock if it becomes free within the given waiting time and the
         * current thread has not been interrupted.
         *
         * <p>Returns {@code true} if the upgrade lock was acquired within the timeout duration, or
         * {@code false} if the timeout elapsed or if the current thread holds a regular read lock
         * or write lock.
         *
         * @param time the maximum time to wait for the lock.
         * @param unit the time unit of the time argument.
         * @return {@code true} if the lock was acquired; {@code false} if the waiting time elapsed.
         * @throws InterruptedException if the current thread is interrupted while waiting.
         */
        @Override
        public boolean tryLock(long time, @NonNull TimeUnit unit) throws InterruptedException {
            if ((mUpgradeMutex.isHeldByCurrentThread() && mHoldingUpgradeLock)
                    || mRwLock.getReadHoldCount() > 0
                    || mRwLock.isWriteLockedByCurrentThread()) {
                return false;
            }
            long startTime = System.nanoTime();
            if (!mUpgradeMutex.tryLock(time, unit)) {
                return false;
            }
            try {
                long elapsed = System.nanoTime() - startTime;
                long remaining = unit.toNanos(time) - elapsed;
                if (!mRwLock.readLock().tryLock(Math.max(0, remaining), TimeUnit.NANOSECONDS)) {
                    mUpgradeMutex.unlock();
                    return false;
                }
                mHoldingUpgradeLock = true;
                mIsUpgraded = false;
                return true;
            } catch (Throwable t) {
                mUpgradeMutex.unlock();
                throw t;
            }
        }

        /**
         * Upgrades the lock from read mode to write mode with zero writer gap.
         *
         * <p>Must be called while holding the upgrade lock. This method temporarily releases the
         * underlying read lock to acquire the write lock.
         *
         * <p>Because the internal upgrade mutex remains held throughout this call and external
         * writers require the upgrade mutex to acquire the write lock, no other writer can
         * interleave or write during this transition window.
         *
         * <p>If other concurrent regular readers hold read locks, this method blocks until all
         * readers release their locks. While no external writers can interleave during this
         * transition (zero writer gap), concurrent readers may continue to acquire read locks until
         * the underlying write lock is queued.
         *
         * <p>If the lock has already been upgraded to write mode, this method returns immediately
         * without side effects.
         *
         * @throws IllegalMonitorStateException if the upgrade lock is not held by the current
         *     thread.
         * @throws IllegalStateException if the current thread holds extra regular read locks beyond
         *     the upgrade lock.
         */
        public void upgrade() {
            if (!mUpgradeMutex.isHeldByCurrentThread() || !mHoldingUpgradeLock) {
                throw new IllegalMonitorStateException("Upgrade lock not held by current thread");
            }
            if (mIsUpgraded) {
                return; // Already upgraded
            }
            if (mRwLock.getReadHoldCount() > 1) {
                throw new IllegalStateException(
                        "Cannot upgrade: current thread holds extra read locks");
            }
            // Release the read lock first to avoid self-deadlock when acquiring write lock.
            // Note: mUpgradeMutex is still held, preventing any external writer from interleaving!
            mRwLock.readLock().unlock();
            try {
                mRwLock.writeLock().lock();
                mIsUpgraded = true;
            } catch (Throwable t) {
                // Restore read lock state if write lock acquisition failed
                mRwLock.readLock().lock();
                throw t;
            }
        }

        /**
         * Releases the upgrade lock.
         *
         * <p>If the lock was upgraded to write mode, this releases the write lock. Otherwise, it
         * releases the read lock. Finally, it releases the internal upgrade mutex so other threads
         * can acquire the write or upgrade lock.
         *
         * @throws IllegalMonitorStateException if the upgrade lock is not held by the current
         *     thread.
         */
        @Override
        public void unlock() {
            if (!mUpgradeMutex.isHeldByCurrentThread() || !mHoldingUpgradeLock) {
                throw new IllegalMonitorStateException("Upgrade lock not held by current thread");
            }
            try {
                if (mIsUpgraded) {
                    mRwLock.writeLock().unlock();
                } else {
                    mRwLock.readLock().unlock();
                }
            } finally {
                // Reset state and release mutex even if unlock throws an exception
                mHoldingUpgradeLock = false;
                mIsUpgraded = false;
                mUpgradeMutex.unlock();
            }
        }

        /**
         * Downgrades the lock from write mode back to read mode.
         *
         * <p>Must be called while holding the upgrade lock in upgraded (write) state.
         *
         * <p>This operation is atomic in terms of read access: the thread will not lose its read
         * authorization during the transition from write mode back to read mode.
         *
         * @throws IllegalMonitorStateException if the lock is not in upgraded (write) state.
         * @throws IllegalStateException if the current thread holds nested write locks.
         */
        public void downgrade() {
            if (!mUpgradeMutex.isHeldByCurrentThread() || !mHoldingUpgradeLock || !mIsUpgraded) {
                throw new IllegalMonitorStateException("Lock not in upgraded (write) state");
            }
            if (mRwLock.getWriteHoldCount() != 1) {
                throw new IllegalStateException(
                        "Cannot downgrade: current thread holds nested write locks");
            }
            // Acquire the read lock before releasing the write lock.
            // This is standard lock downgrading which ensures seamless read access.
            mRwLock.readLock().lock();
            mRwLock.writeLock().unlock();
            mIsUpgraded = false;
        }

        /**
         * Throws {@link UnsupportedOperationException} as conditions are not supported on {@link
         * UpgradeLock}.
         *
         * @return never returns normally.
         * @throws UnsupportedOperationException always.
         */
        @Override
        public @NonNull Condition newCondition() {
            throw new UnsupportedOperationException("Conditions are not supported on UpgradeLock");
        }
    }
}
