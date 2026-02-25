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
package androidx.compose.remote.integration.view.convert;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import android.annotation.SuppressLint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Round-trip tests for RemoteComposeConverter.
 * Ensures bit-for-bit identity for all available .rc samples.
 */
@SuppressLint("RestrictedApiAndroidX")
@RunWith(Parameterized.class)
public class RemoteComposeConverterTest {
    private static final boolean VERBOSE = false;
    private final File mRcFile;

    @SuppressLint("RestrictedApiAndroidX")
    public RemoteComposeConverterTest(File rcFile) {
        this.mRcFile = rcFile;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<File> getSamples() {
        List<File> samples = new ArrayList<>();
        File rawDir = new File("src/main/res/raw");
        if (!rawDir.exists()) {
            // Try relative to project root if run from different context
            rawDir = new File("integration-tests/player-view-demos/src/main/res/raw");
        }
        File[] files = rawDir.listFiles((dir, name) -> name.endsWith(".rc"));
        if (files != null) {
            for (File f : files) {
                samples.add(f);
            }
        }
        return samples;
    }

    @Test
    @SuppressLint("RestrictedApiAndroidX")
    public void testRoundTrip() throws Exception {
        System.out.println(mRcFile.toPath());
        byte[] originalBytes = Files.readAllBytes(mRcFile.toPath());
        assertNotNull("Original bytes should not be null for " + mRcFile.getName(), originalBytes);

        String json = RemoteComposeConverter.remoteComposeToJson(originalBytes);

        assertNotNull("Converted JSON should not be null for " + mRcFile.getName(), json);
        if (VERBOSE) {
            System.out.println("===========================================");
            System.out.println(mRcFile.getName());
            System.out.println("===========================================");
            System.out.println(json.substring(0, Math.min(json.length(), 1000)));
            System.out.println("===========================================");

        }
        System.out.println(System.getProperties().toString());
        byte[] reconstructedBytes = RemoteComposeConverter.jsonToRemoteCompose(json);

        assertArrayEquals(
                "Round-trip failed for " + mRcFile.getName() + " - bytes are not identical",
                originalBytes, reconstructedBytes);
    }
}
