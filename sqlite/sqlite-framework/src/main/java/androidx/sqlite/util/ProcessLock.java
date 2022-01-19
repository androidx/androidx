/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.sqlite.util;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Utility class for in-process and multi-process key-based lock mechanism for safely doing
 * synchronized operations.
 * <p>
 * Acquiring the lock will be quick if no other thread or process has a lock with the same key.
 * But if the lock is already held then acquiring it will block, until the other thread or process
 * releases the lock. Note that the key and lock directory must be the same to achieve
 * synchronization.
 * <p>
 * Locking is done via two levels:
 * <ol>
 *   <li>
 *     Thread locking within the same JVM process is done via a map of String key to ReentrantLock
 *     objects.
 *   <li>
 *     Multi-process locking is done via a lock file whose name contains the key and FileLock
 *     objects.
 */
public final class ProcessLock {

    // in-process lock map
    private static final Map<String, Lock> sThreadLocks = new HashMap<>();

    private final File mLockFile;
    private final Lock mThreadLock;
    private final boolean mFileLevelLock;
    private FileChannel mLockChannel;

    /**
     * Creates a lock with {@code name} and using {@code lockDir} as the directory for the
     * lock files.
     * @param name the name of this lock.
     * @param lockDir the directory where the lock files will be located.
     * @param processLock whether to use file for process level locking or not by default. The
     *                    behaviour can be overridden via the {@link #lock(boolean)}} method.
     */
    @SuppressWarnings("StreamFiles") // Overloads with FileDescriptor or Stream are invalid.
    public ProcessLock(@NonNull String name, @NonNull File lockDir, boolean processLock) {
        mLockFile = new File(lockDir, name + ".lck");
        mThreadLock = getThreadLock(mLockFile.getAbsolutePath());
        mFileLevelLock = processLock;
    }

    /**
     * Attempts to grab the lock, blocking if already held by another thread or process.
     */
    public void lock() {
        lock(mFileLevelLock);
    }

    /**
     * Attempts to grab the lock, blocking if already held by another thread or process.
     *
     * @param processLock whether to use file for process level locking or not.
     */
    public void lock(boolean processLock) {
        mThreadLock.lock();
        if (processLock) {
            try {
                // Verify parent dir
                File parentDir = mLockFile.getParentFile();
                if (parentDir != null) {
                    parentDir.mkdirs();
                }
                mLockChannel = new FileOutputStream(mLockFile).getChannel();
                mLockChannel.lock();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to grab file lock.", e);
            }
        }
    }

    /**
     * Releases the lock.
     */
    public void unlock() {
        if (mLockChannel != null) {
            try {
                mLockChannel.close();
            } catch (IOException ignored) { }
        }
        mThreadLock.unlock();
    }

    private static Lock getThreadLock(String key) {
        synchronized (sThreadLocks) {
            Lock threadLock = sThreadLocks.get(key);
            if (threadLock == null) {
                threadLock = new ReentrantLock();
                sThreadLocks.put(key, threadLock);
            }
            return threadLock;
        }
    }
}
