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

package androidx.media2.player.exoplayer;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.os.Build;
import android.system.Os;
import android.system.OsConstants;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Utility methods for handling file descriptors.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
/* package */ final class FileDescriptorUtil {

    /**
     * {@link OsConstants} was added in API 21 and initializes its fields lazily, so we directly
     * specify the constant for the {@code lseek} {@code whence} argument for earlier API versions.
     */
    private static final int SEEK_SET = 0;

    // Before API 21 we access the hidden Posix.lseek API using reflection.
    private static final Object sPosixLockV14 = new Object();
    @GuardedBy("sPosixLockV14")
    @Nullable
    private static Object sPosixObjectV14;
    @GuardedBy("sPosixLockV14")
    private static @Nullable Method sLseekMethodV14;
    @GuardedBy("sPosixLockV14")
    private static @Nullable Method sDupMethodV14;
    @GuardedBy("sPosixLockV14")
    private static @Nullable Method sCloseMethodV14;

    public static FileDescriptor dup(FileDescriptor fileDescriptor) throws IOException {
        if (Build.VERSION.SDK_INT >= 21) {
            return dupV21(fileDescriptor);
        } else {
            return dupV14(fileDescriptor);
        }
    }

    public static void seek(FileDescriptor fileDescriptor, long position) throws IOException {
        if (Build.VERSION.SDK_INT >= 21) {
            seekV21(fileDescriptor, position);
        } else {
            seekV14(fileDescriptor, position);
        }
    }

    public static void close(FileDescriptor fileDescriptor) throws IOException {
        if (Build.VERSION.SDK_INT >= 21) {
            closeV21(fileDescriptor);
        } else {
            closeV14(fileDescriptor);
        }
    }

    @RequiresApi(21)
    private static FileDescriptor dupV21(FileDescriptor fileDescriptor) throws IOException {
        try {
            return Os.dup(fileDescriptor);
        } catch (Exception e) {
            throw new IOException("Failed to dup the file descriptor", e);
        }
    }

    @SuppressLint("PrivateApi")
    private static FileDescriptor dupV14(FileDescriptor fileDescriptor) throws IOException {
        try {
            Method method;
            Object object;
            synchronized (sPosixLockV14) {
                ensurePosixObjectsInitialized();
                object = sPosixObjectV14;
                method = sDupMethodV14;
            }
            return (FileDescriptor) method.invoke(object, fileDescriptor);
        } catch (Exception e) {
            throw new IOException("Failed to dup the file descriptor", e);
        }
    }

    @RequiresApi(21)
    private static void seekV21(FileDescriptor fileDescriptor, long position) throws IOException {
        try {
            Os.lseek(fileDescriptor, position, /* whence= */ OsConstants.SEEK_SET);
        } catch (Exception e) {
            throw new IOException("Failed to seek the file descriptor", e);
        }
    }

    @SuppressLint("PrivateApi")
    private static void seekV14(FileDescriptor fileDescriptor, long position) throws IOException {
        try {
            Method method;
            Object object;
            synchronized (sPosixLockV14) {
                ensurePosixObjectsInitialized();
                object = sPosixObjectV14;
                method = sLseekMethodV14;
            }
            method.invoke(object, fileDescriptor, position, /* whence= */ SEEK_SET);
        } catch (Exception e) {
            throw new IOException("Failed to seek the file descriptor", e);
        }
    }

    @RequiresApi(21)
    private static void closeV21(FileDescriptor fileDescriptor) throws IOException {
        try {
            Os.close(fileDescriptor);
        } catch (Exception e) {
            throw new IOException("Failed to close the file descriptor", e);
        }
    }

    @SuppressLint("PrivateApi")
    private static FileDescriptor closeV14(FileDescriptor fileDescriptor) throws IOException {
        try {
            Method method;
            Object object;
            synchronized (sPosixLockV14) {
                ensurePosixObjectsInitialized();
                object = sPosixObjectV14;
                method = sCloseMethodV14;
            }
            return (FileDescriptor) method.invoke(object, fileDescriptor);
        } catch (Exception e) {
            throw new IOException("Failed to close the file descriptor", e);
        }
    }

    private static void ensurePosixObjectsInitialized() throws Exception {
        synchronized (sPosixLockV14) {
            if (sPosixObjectV14 != null) {
                return;
            }
            Class<?> posixClass = Class.forName("libcore.io.Posix");
            Constructor<?> constructor = posixClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            sLseekMethodV14 = posixClass.getMethod(
                    "lseek", FileDescriptor.class, Long.TYPE, Integer.TYPE);
            sDupMethodV14 = posixClass.getMethod("dup", FileDescriptor.class);
            sCloseMethodV14 = posixClass.getMethod("close", FileDescriptor.class);
            sPosixObjectV14 = constructor.newInstance();
        }
    }

    private FileDescriptorUtil() {
        // Prevent instantiation.
    }

}
