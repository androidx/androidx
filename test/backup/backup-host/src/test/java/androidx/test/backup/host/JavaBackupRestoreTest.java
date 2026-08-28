/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;

import java.util.Collections;

public class JavaBackupRestoreTest {

    @Test
    public void testBackupActionResultSuccessConstruction() {
        BackupActionResult.Success success =
                new BackupActionResult.Success(Collections.singletonMap("user_id", "123"));
        assertEquals("123", success.getData().get("user_id"));
    }

    @Test
    public void testBackupActionResultFailureConstruction() {
        BackupActionResult.Failure failure = new BackupActionResult.Failure("Sample failure");
        assertEquals("Sample failure", failure.getErrorMessage());

        BackupActionResult.Failure failureWithStack =
                new BackupActionResult.Failure("Sample failure", "at Class.method(File.java:10)");
        assertEquals("Sample failure", failureWithStack.getErrorMessage());
        assertEquals("at Class.method(File.java:10)", failureWithStack.getStackTrace());
    }

    @Test
    public void testBackupTransportModeConstants() {
        assertEquals("DEVICE_TO_DEVICE", BackupTransportMode.DEVICE_TO_DEVICE.toString());
        assertEquals("CLOUD_ENCRYPTED", BackupTransportMode.CLOUD_ENCRYPTED.toString());
        assertEquals("CLOUD_UNENCRYPTED", BackupTransportMode.CLOUD_UNENCRYPTED.toString());
        assertEquals("LOCAL", BackupTransportMode.LOCAL.toString());
    }

    @Test
    public void testStorageDomainInstantiations() {
        StorageDomain.Preference prefDomain =
                new StorageDomain.Preference("my_prefs", "user_key", "user_val");
        assertEquals("my_prefs", prefDomain.getPrefName());
        assertEquals("user_key", prefDomain.getKey());
        assertEquals("user_val", prefDomain.getValue());

        // Test overloads / primitives in Preference
        StorageDomain.Preference prefPrimitive =
                new StorageDomain.Preference("my_prefs", "int_key", 42);
        assertEquals("my_prefs", prefPrimitive.getPrefName());
        assertEquals("int_key", prefPrimitive.getKey());
        assertEquals(42, prefPrimitive.getValue());

        StorageDomain.TextFile textDomain =
                new StorageDomain.TextFile("files/data.txt", "file content");
        assertEquals("files/data.txt", textDomain.getPath());
        assertEquals("file content", textDomain.getContent());

        byte[] bytes = new byte[] {0x1, 0x2, 0x3};
        StorageDomain.BinaryFile binaryDomain =
                new StorageDomain.BinaryFile("files/image.png", bytes);
        assertEquals("files/image.png", binaryDomain.getPath());
        assertArrayEquals(bytes, binaryDomain.getContent());
        assertNotSame(bytes, binaryDomain.getContent());

        StorageDomain.Database dbDomain =
                new StorageDomain.Database(
                        "my_db.db", "my_table", "id", 1, Collections.singletonMap("score", 100));
        assertEquals("my_db.db", dbDomain.getDbName());
        assertEquals("my_table", dbDomain.getTable());
        assertEquals("id", dbDomain.getPrimaryKeyCol());
        assertEquals(1, dbDomain.getPrimaryKeyVal());
        assertEquals(100, dbDomain.getColumnValues().get("score"));
    }

    @Test
    public void testIsolationPolicyEnum() {
        assertEquals(IsolationPolicy.AUTOMATIC, IsolationPolicy.valueOf("AUTOMATIC"));
        assertEquals(IsolationPolicy.MANUAL, IsolationPolicy.valueOf("MANUAL"));
    }

    @Test
    public void testBackupRestoreControllerAsyncCompilation() {
        BackupRestoreController device = org.mockito.Mockito.mock(BackupRestoreController.class);
        ListenableFuture<BackupRestoreController> future = device.stopAppAsync();
        // Simply asserting that stopAppAsync compiles and exists on the interface for
        // Java consumers.
        org.junit.Assert.assertNull(future);
    }
}
