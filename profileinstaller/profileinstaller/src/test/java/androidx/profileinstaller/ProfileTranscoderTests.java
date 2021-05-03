/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.profileinstaller;


import static androidx.profileinstaller.ProfileTranscoder.MAGIC;

import androidx.annotation.NonNull;

import com.google.common.truth.Truth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;

@RunWith(JUnit4.class)
public class ProfileTranscoderTests {
    @Test
    public void testReadProfile() throws IOException {
        byte[] version = ProfileVersion.V010_P;
        File pprof = testFile("baseline-p.prof");
        try (InputStream is = new FileInputStream(pprof)) {
            expectBytes(is, MAGIC);
            expectBytes(is, version);
            Map<String, DexProfileData> data = ProfileTranscoder.readProfile(is, version);
            Truth.assertThat(data).hasSize(1);
            DexProfileData item = data.values().iterator().next();
            Truth.assertThat(item.dexChecksum).isEqualTo(147004379);
            Truth.assertThat(item.numMethodIds).isEqualTo(18487);
            Truth.assertThat(item.hotMethodRegionSize).isEqualTo(140);
        }
    }

    @Test
    public void testTranscodeForN() throws IOException {
        assertGoldenTranscode(
                testFile("baseline-p.prof"),
                testFile("baseline-n.prof"),
                ProfileVersion.V001_N
        );
    }

    @Test
    public void testTranscodeForO() throws IOException {
        assertGoldenTranscode(
                testFile("baseline-p.prof"),
                testFile("baseline-o.prof"),
                ProfileVersion.V005_O
        );
    }

    @Test
    public void testTranscodeForP() throws IOException {
        assertGoldenTranscode(
                testFile("baseline-p.prof"),
                testFile("baseline-p.prof"),
                ProfileVersion.V010_P
        );
    }

    private static File testFile(@NonNull String fileName) {
        return new File("src/test/test-data", fileName);
    }

    private static void assertGoldenTranscode(
            @NonNull File input,
            @NonNull File golden,
            @NonNull byte[] desiredVersion
    ) throws IOException {
        try (
                InputStream is = new FileInputStream(input);
                ByteArrayOutputStream os = new ByteArrayOutputStream()
        ) {
            byte[] version = ProfileTranscoder.readHeader(is);
            ProfileTranscoder.writeHeader(os, desiredVersion);
            if (!Arrays.equals(desiredVersion, ProfileVersion.V010_P)) {
                Map<String, DexProfileData> data = ProfileTranscoder.readProfile(
                        is,
                        version
                );
                ProfileTranscoder.transcodeAndWriteBody(os, desiredVersion, data);
            } else {
                Encoding.writeAll(is, os);
            }
            byte[] goldenBytes = Files.readAllBytes(golden.toPath());
            byte[] actualBytes = os.toByteArray();
            Truth.assertThat(actualBytes).isEqualTo(goldenBytes);
        }
    }

    private static void expectBytes(@NonNull InputStream is, @NonNull byte[] bytes)
            throws IOException {
        byte[] actual = Encoding.read(is, bytes.length);
        Truth.assertThat(actual).isEqualTo(bytes);
    }
}
