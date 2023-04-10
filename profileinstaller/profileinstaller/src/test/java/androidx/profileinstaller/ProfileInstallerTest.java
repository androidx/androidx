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

import static com.google.common.truth.Truth.assertThat;

import android.content.pm.PackageInfo;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
@RunWith(JUnit4.class)
public class ProfileInstallerTest extends TestCase {

    private Path mTmpDir;

    @Before
    public void mkTmpDir() {
        try {
            mTmpDir = Files.createTempDirectory("profileinstaller");
        } catch (IOException e) {
            assertNotNull(mTmpDir);
        }
    }

    @After
    public void rmTmpDir() {
        try {
            Files.walk(mTmpDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile).forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void whenEmptyDir_hasntWrittenProfile() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.lastUpdateTime = 5L;
        boolean result = ProfileInstaller.hasAlreadyWrittenProfileForThisInstall(packageInfo,
                mTmpDir.toFile(), new TraceDiagnostics());
        assertThat(result).isFalse();
    }

    @Test
    public void whenHasNotedProfile_hasWrittenProfile_whenPackageinfoLastUpdateSame() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.lastUpdateTime = 5L;
        TraceDiagnostics diagnosticsCallback = new TraceDiagnostics();
        File appFilesDir = mTmpDir.toFile();
        ProfileInstaller.noteProfileWrittenFor(packageInfo, appFilesDir);
        boolean result = ProfileInstaller.hasAlreadyWrittenProfileForThisInstall(packageInfo,
                appFilesDir, diagnosticsCallback);
        assertThat(result).isTrue();
    }

    @Test
    public void whenHasNotedProfile_andInstallTimeChanged_hasntWritenProfile() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.lastUpdateTime = 5L;
        TraceDiagnostics diagnosticsCallback = new TraceDiagnostics();
        File appFilesDir = mTmpDir.toFile();
        ProfileInstaller.noteProfileWrittenFor(packageInfo, appFilesDir);
        packageInfo.lastUpdateTime = 7L;
        boolean result = ProfileInstaller.hasAlreadyWrittenProfileForThisInstall(packageInfo,
                appFilesDir, diagnosticsCallback);
        assertThat(result).isFalse();
    }

    @Test
    public void whenAlreadyInstalled_diagnosticIsSent() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.lastUpdateTime = 5L;
        TraceDiagnostics diagnosticsCallback = new TraceDiagnostics();
        File appFilesDir = mTmpDir.toFile();
        ProfileInstaller.noteProfileWrittenFor(packageInfo, appFilesDir);
        boolean result = ProfileInstaller.hasAlreadyWrittenProfileForThisInstall(packageInfo,
                appFilesDir, diagnosticsCallback);

        assertThat(diagnosticsCallback.mResults).containsExactly(
                ProfileInstaller.RESULT_ALREADY_INSTALLED);
        assertThat(diagnosticsCallback.mDiagnostics).isEmpty();
    }

    @Test
    public void whenNotInstalled_noDiagnostic() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.lastUpdateTime = 5L;
        TraceDiagnostics diagnosticsCallback = new TraceDiagnostics();
        File appFilesDir = mTmpDir.toFile();
        ProfileInstaller.hasAlreadyWrittenProfileForThisInstall(
                packageInfo,
                appFilesDir,
                diagnosticsCallback
        );
        assertThat(diagnosticsCallback.mResults).isEmpty();
    }

    @Test
    public void verifySkipFileDeleted() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.lastUpdateTime = 5L;
        TraceDiagnostics diagnosticsCallback = new TraceDiagnostics();
        File appFilesDir = mTmpDir.toFile();
        ProfileInstaller.noteProfileWrittenFor(packageInfo, appFilesDir);
        boolean result = ProfileInstaller.hasAlreadyWrittenProfileForThisInstall(
                packageInfo,
                appFilesDir,
                diagnosticsCallback
        );
        assertTrue(result);
        ProfileInstaller.deleteProfileWrittenFor(appFilesDir);
        result = ProfileInstaller.hasAlreadyWrittenProfileForThisInstall(
                packageInfo,
                appFilesDir,
                diagnosticsCallback
        );
        assertFalse(result);
    }

    static class TraceDiagnostics implements ProfileInstaller.DiagnosticsCallback {
        List<Integer> mDiagnostics = new ArrayList<>();
        List<Integer> mResults = new ArrayList<>();

        @Override
        public void onDiagnosticReceived(int code, @Nullable Object data) {
            mDiagnostics.add(code);
        }

        @Override
        public void onResultReceived(int code, @Nullable Object data) {
            mResults.add(code);
        }
    }
}
