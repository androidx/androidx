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


import static androidx.profileinstaller.ProfileTranscoder.MAGIC_PROF;
import static androidx.profileinstaller.ProfileTranscoder.MAGIC_PROFM;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.common.truth.Truth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;

@RequiresApi(api = Build.VERSION_CODES.O)
@RunWith(JUnit4.class)
public class ProfileTranscoderTests {
    private static final String APK_NAME = "base.apk";
    @Test
    public void testReadProfile() throws IOException {
        byte[] version = ProfileVersion.V010_P;
        File pprof = testFile("baseline-p.prof");
        try (InputStream is = new FileInputStream(pprof)) {
            expectBytes(is, MAGIC_PROF);
            expectBytes(is, version);
            DexProfileData[] data = ProfileTranscoder.readProfile(is, version, APK_NAME);
            Truth.assertThat(data).hasLength(1);
            DexProfileData item = data[0];
            Truth.assertThat(item.dexChecksum).isEqualTo(147004379);
            Truth.assertThat(item.numMethodIds).isEqualTo(18487);
            Truth.assertThat(item.hotMethodRegionSize).isEqualTo(140);
        }
    }

    @Test
    public void testTranscodeForN() throws IOException {
        assertGoldenTranscode(
                testFile("baseline.prof"),
                testFile("baseline-n.prof"),
                ProfileVersion.V001_N
        );
    }

    @Test
    public void testTranscodeForO() throws IOException {
        assertGoldenTranscode(
                testFile("baseline.prof"),
                testFile("baseline-o.prof"),
                ProfileVersion.V005_O
        );
    }

    @Test
    public void testTranscodeForO_MR1() throws IOException {
        assertGoldenTranscode(
                testFile("baseline.prof"),
                testFile("baseline-o-mr1.prof"),
                ProfileVersion.V009_O_MR1
        );
    }

    @Test
    public void testTranscodeForP() throws IOException {
        assertGoldenTranscode(
                testFile("baseline.prof"),
                testFile("baseline-p.prof"),
                ProfileVersion.V010_P
        );
    }

    @Test
    public void testMultidexTranscodeForO() throws IOException {
        assertGoldenTranscode(
                testFile("baseline-multidex.prof"),
                testFile("baseline-multidex-o.prof"),
                ProfileVersion.V005_O
        );
    }

    @Test
    public void testMultidexTranscodeForO_MR1() throws IOException {
        assertGoldenTranscode(
                testFile("baseline-multidex.prof"),
                testFile("baseline-multidex-o-mr1.prof"),
                ProfileVersion.V009_O_MR1
        );
    }

    @Test
    public void testMultidexTranscodeForN() throws IOException {
        assertGoldenTranscodeWithMeta(
                testFile("baseline-multidex.prof"),
                testFile("baseline-multidex.profm"),
                testFile("baseline-multidex-n.prof"),
                ProfileVersion.V001_N
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
            byte[] version = ProfileTranscoder.readHeader(is, MAGIC_PROF);
            ProfileTranscoder.writeHeader(os, desiredVersion);
            DexProfileData[] data = ProfileTranscoder.readProfile(
                    is,
                    version,
                    APK_NAME
            );
            ProfileTranscoder.transcodeAndWriteBody(os, desiredVersion, data);
            byte[] goldenBytes = Files.readAllBytes(golden.toPath());
            byte[] actualBytes = os.toByteArray();
            Truth.assertThat(Arrays.equals(goldenBytes, actualBytes)).isTrue();
        }
    }

    private static void assertGoldenTranscodeWithMeta(
            @NonNull File input,
            @NonNull File inputMeta,
            @NonNull File golden,
            @NonNull byte[] desiredVersion
    ) throws IOException {
        try (
                InputStream isProf = new FileInputStream(input);
                InputStream isProfM = new FileInputStream(inputMeta);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
        ) {
            byte[] version = ProfileTranscoder.readHeader(isProf, MAGIC_PROF);
            DexProfileData[] data = ProfileTranscoder.readProfile(
                    isProf,
                    version,
                    APK_NAME
            );

            byte[] metaVersion = ProfileTranscoder.readHeader(isProfM, MAGIC_PROFM);
            data = ProfileTranscoder.readMeta(isProfM, metaVersion, data);

            ProfileTranscoder.writeHeader(os, desiredVersion);
            ProfileTranscoder.transcodeAndWriteBody(os, desiredVersion, data);
            byte[] goldenBytes = Files.readAllBytes(golden.toPath());
            byte[] actualBytes = os.toByteArray();
            Truth.assertThat(Arrays.equals(goldenBytes, actualBytes)).isTrue();
        }
    }

    /**
     * Call this to quickly regenerate golden transcodes based on profile format changes.
     *
     * Please ensure that the new file is correct and provide a detailed description of the
     * change in the commit message whenever you call this.
     */
    private static void updateGoldenTranscode(
            @NonNull File input,
            @NonNull File golden,
            @NonNull byte[] desiredVersion
    ) throws IOException {
        try (
                InputStream is = new FileInputStream(input);
                OutputStream os = new FileOutputStream(golden)
        ) {
            byte[] version = ProfileTranscoder.readHeader(is, MAGIC_PROF);
            ProfileTranscoder.writeHeader(os, desiredVersion);
            DexProfileData[] data = ProfileTranscoder.readProfile(
                    is,
                    version,
                    APK_NAME
            );
            ProfileTranscoder.transcodeAndWriteBody(os, desiredVersion, data);
        }
    }

    private static void expectBytes(@NonNull InputStream is, @NonNull byte[] bytes)
            throws IOException {
        byte[] actual = Encoding.read(is, bytes.length);
        Truth.assertThat(actual).isEqualTo(bytes);
    }
}
