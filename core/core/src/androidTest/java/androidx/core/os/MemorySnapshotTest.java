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

package androidx.core.os;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class MemorySnapshotTest {

    @Rule
    public TemporaryFolder mTempFolder = new TemporaryFolder();

    @Test
    public void testCapture() {
        MemorySnapshot snapshot = MemorySnapshot.capture();
        assertNotNull("MemorySnapshot.capture() should return a non-null snapshot", snapshot);
        verifySnapshot(snapshot, "ProcStatus");
    }

    @Test
    public void testCapture_withPidZero() {
        MemorySnapshot snapshot = MemorySnapshot.capture(0);
        assertNotNull("MemorySnapshot.capture(0) should return a non-null snapshot", snapshot);
        verifySnapshot(snapshot, "PidZero");
    }

    @Test
    public void testCapture_withCurrentPid() {
        int myPid = Process.myPid();
        MemorySnapshot snapshot = MemorySnapshot.capture(myPid);
        assertNotNull("MemorySnapshot.capture(myPid) should return a non-null snapshot", snapshot);
        verifySnapshot(snapshot, "CurrentPid");
    }

    @Test
    public void testCapture_negativePid() {
        assertThrows(IllegalArgumentException.class, () -> MemorySnapshot.capture(-1));
        assertThrows(IllegalArgumentException.class, () -> MemorySnapshot.capture(-100));
    }

    @Test
    public void testCapture_nonExistentPid() {
        assertNull("MemorySnapshot.capture(9999999) should return null",
                MemorySnapshot.capture(9999999));
    }

    @Test
    public void testCapture_foreignProcessDenied() {
        // PID 1 (init) belongs to root (UID 0) and cannot be captured due to UID isolation
        assertThrows(IllegalArgumentException.class, () -> MemorySnapshot.capture(1));
    }

    @LargeTest
    @Test
    public void testCapture_secondaryProcess() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch pidLatch = new CountDownLatch(1);
        AtomicInteger secondaryPid = new AtomicInteger(-1);

        Handler replyHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MemorySnapshotTestService.MSG_PID_REPLY) {
                    secondaryPid.set(msg.arg1);
                    pidLatch.countDown();
                }
            }
        };
        Messenger replyMessenger = new Messenger(replyHandler);

        AtomicReference<Messenger> serviceMessengerRef = new AtomicReference<>();
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                serviceMessengerRef.set(new Messenger(service));
                connectedLatch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                serviceMessengerRef.set(null);
            }
        };

        Intent intent = new Intent(context, MemorySnapshotTestService.class);
        boolean bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        assertTrue("Failed to bind to MemorySnapshotTestService", bound);

        try {
            assertTrue("Timed out waiting for service connection",
                    connectedLatch.await(10, TimeUnit.SECONDS));

            Message msg = Message.obtain(null, MemorySnapshotTestService.MSG_GET_PID);
            msg.replyTo = replyMessenger;
            serviceMessengerRef.get().send(msg);

            assertTrue("Timed out waiting for PID reply from secondary process",
                    pidLatch.await(10, TimeUnit.SECONDS));

            int pid = secondaryPid.get();
            assertTrue("Secondary PID should be positive: " + pid, pid > 0);
            int myPid = Process.myPid();
            assertTrue("Secondary PID (" + pid + ") should differ from calling PID (" + myPid + ")",
                    pid != myPid);

            ActivityManager am = ContextCompat.getSystemService(context, ActivityManager.class);
            assertNotNull("ActivityManager should not be null", am);
            List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
            int myUid = Process.myUid();
            String pkgName = context.getPackageName();

            Map<Integer, String> targetPids = new HashMap<>();
            targetPids.put(myPid, pkgName);
            targetPids.put(pid, pkgName + ":secondary");
            if (procs != null) {
                for (ActivityManager.RunningAppProcessInfo info : procs) {
                    if (info.uid == myUid && !targetPids.containsKey(info.pid)) {
                        targetPids.put(info.pid, info.processName);
                    }
                }
            }

            List<Integer> capturedPids = new ArrayList<>();
            for (Map.Entry<Integer, String> entry : targetPids.entrySet()) {
                int targetPid = entry.getKey();
                String procName = entry.getValue();
                boolean isOwnedByTest = targetPid == myPid || targetPid == pid
                        || (pkgName + ":secondary").equals(procName);
                MemorySnapshot snapshot = MemorySnapshot.capture(targetPid);
                // Leftover non-test processes in the same UID may exit concurrently, causing
                // post-kill cgroup teardown (EIO) so capture() returns null.
                if (!isOwnedByTest && snapshot == null && !isProcessAlive(targetPid)) {
                    continue;
                }
                assertNotNull("MemorySnapshot.capture(" + targetPid + ") [" + procName
                        + "] returned null", snapshot);
                verifySnapshot(snapshot, procName + "(" + targetPid + ")", targetPid != myPid,
                        isOwnedByTest);
                if (snapshot.getProcessMemoryUsageBytes() >= 0
                        && snapshot.getPackageMemoryUsageBytes() >= 0) {
                    assertTrue("Package memory usage must be >= process memory usage for "
                            + procName, snapshot.getPackageMemoryUsageBytes()
                            >= snapshot.getProcessMemoryUsageBytes());
                }
                capturedPids.add(targetPid);
            }
            assertTrue("Calling PID (" + myPid + ") must be captured",
                    capturedPids.contains(myPid));
            assertTrue("Secondary PID (" + pid + ") must be captured",
                    capturedPids.contains(pid));
        } finally {
            context.unbindService(connection);
        }
    }

    @Test
    public void testParseKbToBytes_valid() {
        assertEquals(784 * 1024L, MemorySnapshot.parseKbToBytes("VmRSS:       784 kB"));
        assertEquals(1844 * 1024L, MemorySnapshot.parseKbToBytes("VmSize: 1844 kB"));
        assertEquals(0L, MemorySnapshot.parseKbToBytes("VmSwap: 0 kB"));
        assertEquals(42 * 1024L, MemorySnapshot.parseKbToBytes("VmRSS:0042 kB"));
        assertEquals(100 * 1024L, MemorySnapshot.parseKbToBytes("VmRSS: 100"));
    }

    @Test
    public void testParseKbToBytes_arbitraryWhitespaces() {
        assertEquals(784 * 1024L, MemorySnapshot.parseKbToBytes("VmRSS:\t\t784\tkB"));
        assertEquals(784 * 1024L, MemorySnapshot.parseKbToBytes("VmRSS: \t 784 \t kB \r\n"));
        assertEquals(784 * 1024L, MemorySnapshot.parseKbToBytes("VmRSS:784\tkB"));
    }

    @Test
    public void testParseKbToBytes_overflowProtection() {
        // Max valid kB value that fits into signed positive long when multiplied by 1024
        long maxKb = Long.MAX_VALUE >> 10;
        assertEquals(maxKb * 1024L,
                MemorySnapshot.parseKbToBytes("VmRSS: " + maxKb + " kB"));

        // Overflow values should return -1 without throwing
        assertEquals(-1L, MemorySnapshot.parseKbToBytes("VmRSS: " + (maxKb + 1) + " kB"));
        assertEquals(-1L,
                MemorySnapshot.parseKbToBytes("VmRSS: 9999999999999999999999999999 kB"));
    }

    @Test
    public void testParseKbToBytes_invalidInputs() {
        assertEquals(-1L, MemorySnapshot.parseKbToBytes(""));
        assertEquals(-1L, MemorySnapshot.parseKbToBytes("VmRSS"));
        assertEquals(-1L, MemorySnapshot.parseKbToBytes("VmRSS:"));
        assertEquals(-1L, MemorySnapshot.parseKbToBytes("VmRSS:   \t"));
        assertEquals(-1L, MemorySnapshot.parseKbToBytes("VmRSS: -100 kB"));
        assertEquals(-1L, MemorySnapshot.parseKbToBytes("VmRSS: abc kB"));
        assertEquals(-1L, MemorySnapshot.parseKbToBytes("VmRSS: 123abc456 kB"));
    }

    @Test
    public void testRead_selfProcessWithCgroup() throws Exception {
        File procRoot = mTempFolder.newFolder("proc_self");
        File selfDir = new File(procRoot, "self");
        assertTrue(selfDir.mkdirs());
        File procStatus = new File(selfDir, "status");
        writeToFile(procStatus,
                "Name:\ttest_process\n"
                        + "VmSize:\t    2000 kB\n"
                        + "VmHWM:\t    1500 kB\n"
                        + "VmRSS:\t    1000 kB\n"
                        + "RssAnon:\t     400 kB\n"
                        + "RssFile:\t     500 kB\n"
                        + "RssShmem:\t     100 kB\n"
                        + "VmSwap:\t      50 kB\n"
                        + "Threads:\t12\n");

        File cgroupDir = mTempFolder.newFolder("cgroup");
        File pidDir = new File(cgroupDir, "pid_" + Process.myPid());
        assertTrue(pidDir.mkdirs());
        writeToFile(new File(pidDir, "memory.current"), "1234567\n");
        writeToFile(new File(cgroupDir, "memory.current"), "9876543\n");

        MemorySnapshot snapshot = invokeRead(0, procRoot.getAbsolutePath(), cgroupDir);
        assertNotNull(snapshot);
        assertEquals(2000 * 1024L, snapshot.getVssBytes());
        assertEquals(1500 * 1024L, snapshot.getRssHwmBytes());
        assertEquals(1000 * 1024L, snapshot.getRssBytes());
        assertEquals(400 * 1024L, snapshot.getAnonRssBytes());
        assertEquals(500 * 1024L, snapshot.getFileRssBytes());
        assertEquals(100 * 1024L, snapshot.getShmemRssBytes());
        assertEquals(50 * 1024L, snapshot.getSwapBytes());
        assertEquals(1234567L, snapshot.getProcessMemoryUsageBytes());
        assertEquals(9876543L, snapshot.getPackageMemoryUsageBytes());
    }

    @Test
    public void testRead_selfProcessMissingRequiredFields() throws Exception {
        File procRootNoVss = mTempFolder.newFolder("proc_no_vss");
        File selfDirNoVss = new File(procRootNoVss, "self");
        assertTrue(selfDirNoVss.mkdirs());
        writeToFile(new File(selfDirNoVss, "status"), "VmRSS:\t1000 kB\n");
        assertNull(invokeRead(0, procRootNoVss.getAbsolutePath(), null));

        File procRootNoRss = mTempFolder.newFolder("proc_no_rss");
        File selfDirNoRss = new File(procRootNoRss, "self");
        assertTrue(selfDirNoRss.mkdirs());
        writeToFile(new File(selfDirNoRss, "status"), "VmSize:\t2000 kB\n");
        assertNull(invokeRead(0, procRootNoRss.getAbsolutePath(), null));
    }

    @Test
    public void testRead_secondaryProcessProcfsPrioritizedOverCgroup()
            throws Exception {
        int secondaryPid = getValidNonSelfPid();
        File procRoot = mTempFolder.newFolder("proc_secondary");
        File procPidDir = new File(procRoot, String.valueOf(secondaryPid));
        assertTrue(procPidDir.mkdirs());
        writeToFile(new File(procPidDir, "status"),
                "VmSize:\t3000 kB\n"
                        + "VmRSS:\t1200 kB\n"
                        + "RssAnon:\t600 kB\n");

        File cgroupDir = mTempFolder.newFolder("cgroup_secondary_proc");
        File pidDir = new File(cgroupDir, "pid_" + secondaryPid);
        assertTrue(pidDir.mkdirs());
        writeToFile(new File(pidDir, "memory.current"), "2222222\n");
        writeToFile(new File(cgroupDir, "memory.current"), "8888888\n");
        // Write different values to memory.stat to verify procfs values are kept when readable
        writeToFile(new File(pidDir, "memory.stat"), "anon 9999999\nfile 9999999\n");

        MemorySnapshot snapshot = invokeRead(
                secondaryPid, procRoot.getAbsolutePath(), cgroupDir);
        assertNotNull(snapshot);
        assertEquals(3000 * 1024L, snapshot.getVssBytes());
        assertEquals(1200 * 1024L, snapshot.getRssBytes());
        assertEquals(600 * 1024L, snapshot.getAnonRssBytes());
        assertEquals(2222222L, snapshot.getProcessMemoryUsageBytes());
        assertEquals(8888888L, snapshot.getPackageMemoryUsageBytes());
    }

    @Test
    public void testRead_secondaryProcessFallbackToCgroup() throws Exception {
        int secondaryPid = getValidNonSelfPid();
        File cgroupDir = mTempFolder.newFolder("cgroup_fallback");
        File pidDir = new File(cgroupDir, "pid_" + secondaryPid);
        assertTrue(pidDir.mkdirs());
        writeToFile(new File(pidDir, "memory.current"), "4000000\n");
        writeToFile(new File(cgroupDir, "memory.current"), "9000000\n");
        writeToFile(new File(pidDir, "memory.swap.current"), "128000\n");
        writeToFile(new File(pidDir, "memory.peak"), "5000000\n");
        writeToFile(new File(pidDir, "memory.stat"),
                "anon_thp 0\n"
                        + "anon\n"
                        + "anon 1000000\n"
                        + "file_mapped 0\n"
                        + "file\n"
                        + "file 2000000\n"
                        + "shmem_thp 0\n"
                        + "shmem\n"
                        + "shmem 500000\n"
                        + "slab 12345\n");

        // Non-existent procRoot forces fallback to cgroup v2 files
        MemorySnapshot snapshot = invokeRead(
                secondaryPid,
                new File(cgroupDir, "non_existent_proc").getAbsolutePath(),
                cgroupDir);
        assertNotNull(snapshot);
        assertEquals(4000000L, snapshot.getRssBytes());
        assertEquals(1000000L, snapshot.getAnonRssBytes());
        assertEquals(1500000L, snapshot.getFileRssBytes());
        assertEquals(500000L, snapshot.getShmemRssBytes());
        assertEquals(128000L, snapshot.getSwapBytes());
        assertEquals(5000000L, snapshot.getRssHwmBytes());
        assertEquals(-1L, snapshot.getVssBytes());
        assertEquals(4000000L, snapshot.getProcessMemoryUsageBytes());
        assertEquals(9000000L, snapshot.getPackageMemoryUsageBytes());
    }

    @Test
    public void testRead_secondaryProcessFallbackWithoutCurrentOrPeak()
            throws Exception {
        int secondaryPid = getValidNonSelfPid();
        File cgroupDir = mTempFolder.newFolder("cgroup_partial");
        File pidDir = new File(cgroupDir, "pid_" + secondaryPid);
        assertTrue(pidDir.mkdirs());
        // Omit memory.current and memory.peak; set memory.swap.current to "max"
        writeToFile(new File(pidDir, "memory.swap.current"), "max\n");
        writeToFile(new File(pidDir, "memory.stat"), "anon 1024000\nfile 0\nshmem 0\n");

        MemorySnapshot snapshot = invokeRead(
                secondaryPid,
                new File(cgroupDir, "non_existent_proc").getAbsolutePath(),
                cgroupDir);
        assertNotNull(snapshot);
        assertEquals(1024000L, snapshot.getRssBytes());
        assertEquals(1024000L, snapshot.getAnonRssBytes());
        assertEquals(0L, snapshot.getFileRssBytes());
        assertEquals(0L, snapshot.getShmemRssBytes());
        assertEquals(Long.MAX_VALUE, snapshot.getSwapBytes());
        assertEquals(1024000L, snapshot.getRssHwmBytes());
        assertEquals(-1L, snapshot.getProcessMemoryUsageBytes());
        assertEquals(-1L, snapshot.getPackageMemoryUsageBytes());
    }

    @Test
    public void testRead_secondaryProcessEmptyCgroupReturnsNull() throws Exception {
        int secondaryPid = getValidNonSelfPid();
        File cgroupDir = mTempFolder.newFolder("cgroup_empty");
        File pidDir = new File(cgroupDir, "pid_" + secondaryPid);
        assertTrue(pidDir.mkdirs());
        // Empty memory.current file to exercise empty file handling in readLongFromFile
        writeToFile(new File(pidDir, "memory.current"), "");

        assertNull(invokeRead(
                secondaryPid,
                new File(cgroupDir, "non_existent_proc").getAbsolutePath(),
                cgroupDir));
    }

    @Test
    public void testRead_secondaryProcessFallbackZeroRssCompacted() throws Exception {
        int secondaryPid = getValidNonSelfPid();
        File nonExistentProc = new File(mTempFolder.getRoot(), "non_existent_proc_zero");

        // Case 1: Both memory.current == 0 and memory.stat fields == 0 (compacted to swap).
        File cgroupCompacted = mTempFolder.newFolder("cgroup_compacted");
        File pidCompacted = new File(cgroupCompacted, "pid_" + secondaryPid);
        assertTrue(pidCompacted.mkdirs());
        writeToFile(new File(pidCompacted, "memory.current"), "0\n");
        writeToFile(new File(cgroupCompacted, "memory.current"), "50000000\n");
        writeToFile(new File(pidCompacted, "memory.swap.current"), "2097152\n");
        writeToFile(new File(pidCompacted, "memory.peak"), "2097152\n");
        writeToFile(new File(pidCompacted, "memory.stat"), "anon 0\nfile 0\nshmem 0\n");

        MemorySnapshot compacted = invokeRead(
                secondaryPid, nonExistentProc.getAbsolutePath(), cgroupCompacted);
        assertNotNull(compacted);
        assertEquals(0L, compacted.getRssBytes());
        assertEquals(0L, compacted.getAnonRssBytes());
        assertEquals(0L, compacted.getFileRssBytes());
        assertEquals(0L, compacted.getShmemRssBytes());
        assertEquals(2097152L, compacted.getSwapBytes());
        assertEquals(2097152L, compacted.getRssHwmBytes());
        assertEquals(-1L, compacted.getVssBytes());
        assertEquals(0L, compacted.getProcessMemoryUsageBytes());
        assertEquals(50000000L, compacted.getPackageMemoryUsageBytes());

        // Case 2: Only memory.stat was read with 0 values; memory.current and memory.peak absent.
        File cgroupStatOnly = mTempFolder.newFolder("cgroup_stat_zero_only");
        File pidStatOnly = new File(cgroupStatOnly, "pid_" + secondaryPid);
        assertTrue(pidStatOnly.mkdirs());
        writeToFile(new File(pidStatOnly, "memory.stat"), "anon 0\nfile 0\nshmem 0\n");

        MemorySnapshot statOnly = invokeRead(
                secondaryPid, nonExistentProc.getAbsolutePath(), cgroupStatOnly);
        assertNotNull(statOnly);
        assertEquals(0L, statOnly.getRssBytes());
        assertEquals(0L, statOnly.getAnonRssBytes());
        assertEquals(0L, statOnly.getFileRssBytes());
        assertEquals(0L, statOnly.getShmemRssBytes());
        assertEquals(-1L, statOnly.getSwapBytes());
        assertEquals(-1L, statOnly.getVssBytes());
        assertEquals(0L, statOnly.getRssHwmBytes());
        assertEquals(-1L, statOnly.getProcessMemoryUsageBytes());
        assertEquals(-1L, statOnly.getPackageMemoryUsageBytes());
    }

    @Test
    public void testRead_secondaryProcessFallbackMissingOrInvalidMemoryStatReturnsNull()
            throws Exception {
        int secondaryPid = getValidNonSelfPid();
        File nonExistentProc = new File(mTempFolder.getRoot(), "non_existent_proc_stat_err");

        // Missing memory.stat returns null even when memory.current is present.
        File cgroupCurrentOnly = mTempFolder.newFolder("cgroup_current_only");
        File pidCurrentOnly = new File(cgroupCurrentOnly, "pid_" + secondaryPid);
        assertTrue(pidCurrentOnly.mkdirs());
        writeToFile(new File(pidCurrentOnly, "memory.current"), "4096\n");

        assertNull(invokeRead(
                secondaryPid, nonExistentProc.getAbsolutePath(), cgroupCurrentOnly));

        // Partial memory.stat returns null even when memory.current is present.
        File cgroupPartial = mTempFolder.newFolder("cgroup_partial_stat");
        File pidPartial = new File(cgroupPartial, "pid_" + secondaryPid);
        assertTrue(pidPartial.mkdirs());
        writeToFile(new File(pidPartial, "memory.current"), "4096\n");
        writeToFile(new File(pidPartial, "memory.stat"), "anon 0\nfile 0\n");

        assertNull(invokeRead(secondaryPid, nonExistentProc.getAbsolutePath(), cgroupPartial));

        // Malformed field value in memory.stat returns null.
        File cgroupMalformed = mTempFolder.newFolder("cgroup_malformed_stat");
        File pidMalformed = new File(cgroupMalformed, "pid_" + secondaryPid);
        assertTrue(pidMalformed.mkdirs());
        writeToFile(new File(pidMalformed, "memory.stat"), "anon invalid\nfile 0\nshmem 0\n");

        assertNull(invokeRead(secondaryPid, nonExistentProc.getAbsolutePath(), cgroupMalformed));

        // Unrecognized memory.stat content returns null.
        File cgroupInvalid = mTempFolder.newFolder("cgroup_invalid_stat");
        File pidInvalid = new File(cgroupInvalid, "pid_" + secondaryPid);
        assertTrue(pidInvalid.mkdirs());
        writeToFile(new File(pidInvalid, "memory.stat"), "invalid\n");

        assertNull(invokeRead(secondaryPid, nonExistentProc.getAbsolutePath(), cgroupInvalid));
    }

    @Test
    public void testRead_secondaryProcessFallbackRssSumSaturatesOnOverflow() throws Exception {
        int secondaryPid = getValidNonSelfPid();
        File cgroupDir = mTempFolder.newFolder("cgroup_overflow");
        File pidDir = new File(cgroupDir, "pid_" + secondaryPid);
        assertTrue(pidDir.mkdirs());
        writeToFile(
                new File(pidDir, "memory.stat"),
                "anon " + Long.MAX_VALUE + "\nfile 1024\nshmem 0\n");

        MemorySnapshot snapshot = invokeRead(
                secondaryPid,
                new File(cgroupDir, "non_existent_proc").getAbsolutePath(),
                cgroupDir);
        assertNotNull(snapshot);
        assertEquals(Long.MAX_VALUE, snapshot.getRssBytes());
        assertEquals(Long.MAX_VALUE, snapshot.getRssHwmBytes());
        assertEquals(Long.MAX_VALUE, snapshot.getAnonRssBytes());
        assertEquals(1024L, snapshot.getFileRssBytes());
        assertEquals(0L, snapshot.getShmemRssBytes());
    }

    @Test
    public void testFindCgroupFd() throws Exception {
        File nonExistentDir = new File(mTempFolder.getRoot(), "does_not_exist");
        assertEquals(-1, invokeFindCgroupFd(nonExistentDir));

        File fdDir = mTempFolder.newFolder("fds_none");
        writeToFile(new File(fdDir, "not_a_symlink"), "data");
        Os.symlink("/dev/null", new File(fdDir, "3").getAbsolutePath());
        assertEquals(-1, invokeFindCgroupFd(fdDir));

        File appsDir = mTempFolder.newFolder("fds_apps");
        Os.symlink("/sys/fs/cgroup/apps/uid_10000", new File(appsDir, "41").getAbsolutePath());
        assertEquals(41, invokeFindCgroupFd(appsDir));

        File systemDir = mTempFolder.newFolder("fds_system");
        Os.symlink("/sys/fs/cgroup/system/uid_1000", new File(systemDir, "42").getAbsolutePath());
        assertEquals(42, invokeFindCgroupFd(systemDir));

        File rootUidDir = mTempFolder.newFolder("fds_uid");
        Os.symlink("/sys/fs/cgroup/uid_10000", new File(rootUidDir, "43").getAbsolutePath());
        assertEquals(43, invokeFindCgroupFd(rootUidDir));
    }

    private static int getValidNonSelfPid() {
        int myPid = Process.myPid();
        int myTid = Process.myTid();
        assertTrue("Test runner thread TID (" + myTid + ") should differ from PID (" + myPid + ")",
                myTid != myPid);
        return myTid;
    }

    private static MemorySnapshot invokeRead(
            int pid, String procRoot, File cgroupDir)
            throws Exception {
        Class<?> procStatusClass = Class.forName("androidx.core.os.MemorySnapshot$ProcStatus");
        Field procRootField = procStatusClass.getDeclaredField("procRoot");
        procRootField.setAccessible(true);
        String originalProcRoot = (String) procRootField.get(null);

        Class<?> holderClass = Class.forName("androidx.core.os.MemorySnapshot$CgroupFdHolder");
        Field cachedFdField = holderClass.getDeclaredField("cachedFd");
        cachedFdField.setAccessible(true);
        int originalCachedFd = cachedFdField.getInt(null);

        FileDescriptor dirFd = null;
        try {
            procRootField.set(null, procRoot);
            if (cgroupDir != null) {
                dirFd = Os.open(cgroupDir.getAbsolutePath(), OsConstants.O_RDONLY, 0);
                int fdNum = findFdForPath(cgroupDir.getCanonicalPath());
                cachedFdField.setInt(null, fdNum);
            }
            return MemorySnapshot.capture(pid);
        } finally {
            procRootField.set(null, originalProcRoot);
            cachedFdField.setInt(null, originalCachedFd);
            if (dirFd != null) {
                Os.close(dirFd);
            }
        }
    }

    private static int findFdForPath(String targetPath) throws Exception {
        File[] fds = new File("/proc/self/fd").listFiles();
        assertNotNull(fds);
        for (File fdFile : fds) {
            try {
                if (targetPath.equals(Os.readlink(fdFile.getAbsolutePath()))) {
                    return Integer.parseInt(fdFile.getName());
                }
            } catch (Exception ignored) {
            }
        }
        throw new IllegalStateException("Could not find open fd for " + targetPath);
    }

    private static int invokeFindCgroupFd(File fdDir) throws Exception {
        Class<?> holderClass = Class.forName("androidx.core.os.MemorySnapshot$CgroupFdHolder");
        Field instanceField = holderClass.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        Object instance = instanceField.get(null);

        Method findCgroupFd = holderClass.getDeclaredMethod("findCgroupFd", File.class);
        findCgroupFd.setAccessible(true);
        return (int) findCgroupFd.invoke(instance, fdDir);
    }

    private static void writeToFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

    private void verifySnapshot(MemorySnapshot snapshot, String source) {
        verifySnapshot(snapshot, source, false, true);
    }

    private void verifySnapshot(MemorySnapshot snapshot, String source, boolean isSecondary,
            boolean requirePositiveRss) {
        long rss = snapshot.getRssBytes();
        long anon = snapshot.getAnonRssBytes();
        long file = snapshot.getFileRssBytes();
        long shmem = snapshot.getShmemRssBytes();
        long swap = snapshot.getSwapBytes();
        long vss = snapshot.getVssBytes();
        long hwm = snapshot.getRssHwmBytes();
        long processMemoryUsage = snapshot.getProcessMemoryUsageBytes();
        long packageMemoryUsage = snapshot.getPackageMemoryUsageBytes();

        android.util.Log.d("MemorySnapshotTest", "[" + source + "] RssBytes: " + rss
                + ", AnonRssBytes: " + anon
                + ", FileRssBytes: " + file
                + ", ShmemRssBytes: " + shmem
                + ", SwapBytes: " + swap
                + ", VssBytes: " + vss
                + ", RssHwmBytes: " + hwm
                + ", ProcessMemoryUsageBytes: " + processMemoryUsage
                + ", PackageMemoryUsageBytes: " + packageMemoryUsage);

        if (!isSecondary || requirePositiveRss) {
            assertTrue(source + ": RssBytes should be positive, was " + rss, rss > 0);
        } else {
            assertTrue(source + ": RssBytes should be non-negative, was " + rss, rss >= 0);
        }
        assertTrue("AnonRssBytes should be non-negative or -1", anon >= 0 || anon == -1);
        assertTrue("FileRssBytes should be non-negative or -1", file >= 0 || file == -1);
        assertTrue("ShmemRssBytes should be non-negative or -1", shmem >= 0 || shmem == -1);
        if (anon >= 0 && file >= 0 && shmem >= 0) {
            long rssBreakdown = anon + file + shmem;
            assertTrue(source + ": Anon + File + Shmem (" + rssBreakdown
                    + ") must be <= RssBytes (" + rss + ")", rssBreakdown <= rss);
        }
        assertTrue("SwapBytes should be non-negative or -1", swap >= 0 || swap == -1);
        assertTrue("VssBytes should be non-negative or -1", vss >= 0 || vss == -1);
        if (!isSecondary) {
            assertTrue("VssBytes should be non-negative for calling process", vss >= 0);
        }
        assertTrue("RssHwmBytes should be non-negative or -1", hwm >= 0 || hwm == -1);
        if (hwm >= 0) {
            assertTrue(source + ": RssHwmBytes (" + hwm + ") must be >= RssBytes (" + rss + ")",
                    hwm >= rss);
        }

        assertTrue("ProcessMemoryUsageBytes should be non-negative or -1",
                processMemoryUsage >= 0 || processMemoryUsage == -1);
        assertTrue("PackageMemoryUsageBytes should be non-negative or -1",
                packageMemoryUsage >= 0 || packageMemoryUsage == -1);
    }

    private static boolean isProcessAlive(int pid) {
        try {
            Os.kill(pid, 0);
            return true;
        } catch (ErrnoException e) {
            return e.errno != OsConstants.ESRCH;
        }
    }
}
