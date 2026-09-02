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
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class MemorySnapshotTest {

    @Test
    public void testCapture() {
        MemorySnapshot snapshot = MemorySnapshot.capture();
        assertNotNull("MemorySnapshot.capture() should return a non-null snapshot", snapshot);
        verifySnapshot(snapshot, "ProcStatus");
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

    private void verifySnapshot(MemorySnapshot snapshot, String source) {
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

        assertTrue("RssBytes should be non-negative", rss >= 0);
        assertTrue("AnonRssBytes should be non-negative or -1", anon >= 0 || anon == -1);
        assertTrue("FileRssBytes should be non-negative or -1", file >= 0 || file == -1);
        assertTrue("ShmemRssBytes should be non-negative or -1", shmem >= 0 || shmem == -1);
        assertTrue("SwapBytes should be non-negative or -1", swap >= 0 || swap == -1);
        assertTrue("VssBytes should be non-negative", vss >= 0);
        assertTrue("RssHwmBytes should be non-negative or -1", hwm >= 0 || hwm == -1);

        assertTrue("ProcessMemoryUsageBytes should be non-negative or -1",
                processMemoryUsage >= 0 || processMemoryUsage == -1);
        assertTrue("PackageMemoryUsageBytes should be non-negative or -1",
                packageMemoryUsage >= 0 || packageMemoryUsage == -1);
    }
}
