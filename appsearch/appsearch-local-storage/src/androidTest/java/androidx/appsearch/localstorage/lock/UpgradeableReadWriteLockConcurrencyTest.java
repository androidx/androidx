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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class UpgradeableReadWriteLockConcurrencyTest {
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
    public void testE2E_upgradeFlow_multipleReadersAndUpgrader() throws Exception {
        CountDownLatch thread1ReadAcquired = new CountDownLatch(1);
        CountDownLatch releaseThread1ReadLock = new CountDownLatch(1);
        CountDownLatch thread2UpgradeLockAcquired = new CountDownLatch(1);
        CountDownLatch thread2UpgradeStarted = new CountDownLatch(1);
        CountDownLatch thread3ReadAcquired = new CountDownLatch(1);
        CountDownLatch releaseThread3ReadLock = new CountDownLatch(1);

        // Thread 1: Acquires a regular read lock.
        Thread thread1 =
                new Thread(
                        () -> {
                            mLock.readLock().lock();
                            try {
                                thread1ReadAcquired.countDown();
                                releaseThread1ReadLock.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                mLock.readLock().unlock();
                            }
                        });
        thread1.start();

        // Ensure Thread 1 holds the read lock.
        assertThat(thread1ReadAcquired.await(2, TimeUnit.SECONDS)).isTrue();

        // Thread 3: Acquires another regular read lock concurrently with Thread 1 before upgrade
        // begins.
        Thread thread3 =
                new Thread(
                        () -> {
                            mLock.readLock().lock();
                            try {
                                thread3ReadAcquired.countDown();
                                releaseThread3ReadLock.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                mLock.readLock().unlock();
                            }
                        });
        thread3.start();

        // Ensure Thread 3 has acquired read lock.
        assertThat(thread3ReadAcquired.await(2, TimeUnit.SECONDS)).isTrue();

        // Thread 2 (Upgrader): Acquires the upgrade lock and initiates upgrade to write mode.
        Thread thread2 =
                new Thread(
                        () -> {
                            mLock.upgradeLock().lock();
                            try {
                                thread2UpgradeLockAcquired.countDown();
                                thread2UpgradeStarted.await();
                                mLock.upgradeLock().upgrade();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                mLock.upgradeLock().unlock();
                            }
                        });
        thread2.start();

        // Ensure Thread 2 holds the upgrade lock in read mode.
        assertThat(thread2UpgradeLockAcquired.await(2, TimeUnit.SECONDS)).isTrue();

        // Step 1: Thread 2 starts upgrading.
        thread2UpgradeStarted.countDown();

        // Wait until Thread 2 blocks waiting for Thread 1 and Thread 3 to release their read locks.
        waitUntilThreadBlocks(thread2);

        // Step 2: Thread 4 attempts to acquire a write lock while upgrade is pending.
        // Must fail because upgrade lock is held.
        Future<Boolean> thread4AcquireWriteLock =
                mExecutor.submit(() -> mLock.writeLock().tryLock());
        assertThat(thread4AcquireWriteLock.get(2, TimeUnit.SECONDS)).isFalse();

        // Step 3: Thread 5 attempts to acquire a second upgrade lock while upgrade is pending.
        // Must fail because only one upgrade lock is allowed at any time.
        Future<Boolean> thread5AcquireUpgradeLock =
                mExecutor.submit(() -> mLock.upgradeLock().tryLock());
        assertThat(thread5AcquireUpgradeLock.get(2, TimeUnit.SECONDS)).isFalse();

        // Step 4: Release Thread 1 and Thread 3 read locks.
        releaseThread1ReadLock.countDown();
        releaseThread3ReadLock.countDown();

        thread1.join(2000);
        thread3.join(2000);
        thread2.join(2000);

        // Step 5: After all threads release, a new writer can acquire the write lock.
        Future<Boolean> finalWriterAcquired =
                mExecutor.submit(
                        () -> {
                            if (mLock.writeLock().tryLock()) {
                                mLock.writeLock().unlock();
                                return true;
                            }
                            return false;
                        });
        assertThat(finalWriterAcquired.get(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testUpgradeLock_upgrade_atomicNoGapWithExternalWriters() throws Exception {
        AtomicReference<String> sharedState = new AtomicReference<>("initial_read_snapshot");
        CountDownLatch upgraderReadHeldLatch = new CountDownLatch(1);
        CountDownLatch externalWriterBlockedLatch = new CountDownLatch(1);
        CountDownLatch upgraderUpgradeDoneLatch = new CountDownLatch(1);
        CountDownLatch releaseUpgraderLatch = new CountDownLatch(1);

        // Thread 1 (Upgrader):
        // 1. Acquires upgradeLock (read mode).
        // 2. Reads data snapshot ("initial_read_snapshot").
        // 3. While external writer is waiting, calls upgrade().
        // 4. Verifies no other thread wrote during upgrade (state is still initial_read_snapshot).
        // 5. Writes new upgraded state ("upgraded_write").
        Thread upgraderThread =
                new Thread(
                        () -> {
                            mLock.upgradeLock().lock();
                            try {
                                String readSnapshot = sharedState.get();
                                assertThat(readSnapshot).isEqualTo("initial_read_snapshot");
                                upgraderReadHeldLatch.countDown();

                                // Wait until external writer is confirmed blocked waiting for
                                // write lock.
                                externalWriterBlockedLatch.await();

                                // Perform atomic upgrade. Zero-gap ensures external writer cannot
                                // write.
                                mLock.upgradeLock().upgrade();

                                // Assert that external writer did not modify data during the
                                // upgrade transition.
                                assertThat(sharedState.get()).isEqualTo("initial_read_snapshot");

                                // Write new state as upgraded writer.
                                sharedState.set("upgraded_write");
                                upgraderUpgradeDoneLatch.countDown();

                                // Hold the upgraded write lock until test signals release.
                                releaseUpgraderLatch.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                mLock.upgradeLock().unlock();
                            }
                        });
        upgraderThread.start();

        // Ensure upgrader is holding upgrade lock in read mode.
        assertThat(upgraderReadHeldLatch.await(2, TimeUnit.SECONDS)).isTrue();

        // Thread 2 (External Writer):
        // Attempts to acquire writeLock and write "external_writer_write".
        // Must be blocked because upgrader holds upgradeLock.
        Thread externalWriterThread =
                new Thread(
                        () -> {
                            mLock.writeLock().lock();
                            try {
                                // When external writer eventually acquires lock, it should see
                                // the upgraded write state, NOT the initial state.
                                assertThat(sharedState.get()).isEqualTo("upgraded_write");
                                sharedState.set("external_writer_write");
                            } finally {
                                mLock.writeLock().unlock();
                            }
                        });
        externalWriterThread.start();

        // Wait until external writer is blocked.
        waitUntilThreadBlocks(externalWriterThread);
        externalWriterBlockedLatch.countDown();

        // Wait until upgrader finishes upgrade and writes "upgraded_write".
        assertThat(upgraderUpgradeDoneLatch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(sharedState.get()).isEqualTo("upgraded_write");

        // Release upgrader so external writer can proceed.
        releaseUpgraderLatch.countDown();

        upgraderThread.join(2000);
        externalWriterThread.join(2000);

        // Verify final state was written by external writer after upgrader completed.
        assertThat(sharedState.get()).isEqualTo("external_writer_write");
    }

    @Test
    public void testE2E_downgradeFlow_multipleReadersAndUpgrader() throws Exception {
        CountDownLatch thread2UpgradedLatch = new CountDownLatch(1);
        CountDownLatch thread2DowngradeLatch = new CountDownLatch(1);
        CountDownLatch thread2DowngradedLatch = new CountDownLatch(1);
        CountDownLatch thread2ReleaseUpgradeLockLatch = new CountDownLatch(1);

        // Thread 2 (Upgrader): Acquires the upgrade lock and immediately upgrades to write mode.
        Thread thread2 =
                new Thread(
                        () -> {
                            mLock.upgradeLock().lock();
                            try {
                                mLock.upgradeLock().upgrade();
                                thread2UpgradedLatch.countDown();

                                // Wait until told to downgrade back to read mode.
                                thread2DowngradeLatch.await();
                                mLock.upgradeLock().downgrade();
                                thread2DowngradedLatch.countDown();

                                // Wait until test completes before releasing the upgrade lock.
                                thread2ReleaseUpgradeLockLatch.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                mLock.upgradeLock().unlock();
                            }
                        });
        thread2.start();

        // Ensure Thread 2 has upgraded to write mode.
        assertThat(thread2UpgradedLatch.await(2, TimeUnit.SECONDS)).isTrue();

        // Step 1: Thread 1 attempts to acquire a read lock while Thread 2 is in write mode.
        // Must fail because write lock is exclusively held by Thread 2.
        Future<Boolean> thread1AcquireReadLock = mExecutor.submit(() -> mLock.readLock().tryLock());
        assertThat(thread1AcquireReadLock.get(2, TimeUnit.SECONDS)).isFalse();

        // Step 2: Signal Thread 2 to downgrade back to read mode.
        thread2DowngradeLatch.countDown();
        assertThat(thread2DowngradedLatch.await(2, TimeUnit.SECONDS)).isTrue();

        // Step 3: Multiple reader threads (Thread 1 and Thread 3) acquire read locks concurrently
        // after downgrade.
        CountDownLatch thread1ReadLatch = new CountDownLatch(1);
        CountDownLatch thread3ReadLatch = new CountDownLatch(1);
        CountDownLatch releaseReadersLatch = new CountDownLatch(1);

        Thread thread1 =
                new Thread(
                        () -> {
                            mLock.readLock().lock();
                            try {
                                thread1ReadLatch.countDown();
                                releaseReadersLatch.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                mLock.readLock().unlock();
                            }
                        });
        Thread thread3 =
                new Thread(
                        () -> {
                            mLock.readLock().lock();
                            try {
                                thread3ReadLatch.countDown();
                                releaseReadersLatch.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                mLock.readLock().unlock();
                            }
                        });
        thread1.start();
        thread3.start();

        // Verify both Thread 1 and Thread 3 successfully acquire read locks concurrently with
        // Thread 2 (in read mode).
        assertThat(thread1ReadLatch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(thread3ReadLatch.await(2, TimeUnit.SECONDS)).isTrue();

        // Step 4: Another writer (Thread 4) or second upgrader (Thread 5) tries to acquire locks.
        // Both must fail because readers and Thread 2's upgrade lock (in read mode) are held.
        Future<Boolean> writerAcquired = mExecutor.submit(() -> mLock.writeLock().tryLock());
        Future<Boolean> secondUpgraderAcquired =
                mExecutor.submit(() -> mLock.upgradeLock().tryLock());

        assertThat(writerAcquired.get(2, TimeUnit.SECONDS)).isFalse();
        assertThat(secondUpgraderAcquired.get(2, TimeUnit.SECONDS)).isFalse();

        // Step 5: Release reader locks and upgrade lock.
        releaseReadersLatch.countDown();
        thread2ReleaseUpgradeLockLatch.countDown();

        thread1.join(2000);
        thread3.join(2000);
        thread2.join(2000);

        // Step 6: Verify a new writer can acquire the write lock after all locks are released.
        Future<Boolean> finalWriterAcquired =
                mExecutor.submit(
                        () -> {
                            if (mLock.writeLock().tryLock()) {
                                mLock.writeLock().unlock();
                                return true;
                            }
                            return false;
                        });
        assertThat(finalWriterAcquired.get(2, TimeUnit.SECONDS)).isTrue();
    }
}
