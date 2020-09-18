/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.appsearch.localbackend;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;

public class LocalBackendTest {
    @Test
    public void testSameInstance() {
        LocalBackend b1 = LocalBackend.getInstance(ApplicationProvider.getApplicationContext())
                .getResultValue();
        LocalBackend b2 = LocalBackend.getInstance(ApplicationProvider.getApplicationContext())
                .getResultValue();
        assertThat(b1).isSameInstanceAs(b2);
    }

    @Test
    public void testInitBlocking() {
        LocalBackend backend = LocalBackend.getInstance(ApplicationProvider.getApplicationContext())
                .getResultValue();
        assertThat(backend.isInitialized()).isFalse();
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            backend.initialize().getResultValue();
        });
        t.setDaemon(true);
        t.setName("startInit");
        long startNs = System.nanoTime();
        t.start();
        backend.removeAll("db").getResultValue();
        long endNs = System.nanoTime();
        long elapsedMs = (endNs - startNs) / 1000 / 1000;
        assertThat(elapsedMs).isAtLeast(1000);
        assertThat(backend.isInitialized()).isTrue();
    }
}
