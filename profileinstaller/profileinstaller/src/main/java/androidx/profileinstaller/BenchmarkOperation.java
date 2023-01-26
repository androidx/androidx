/*
 * Copyright 2022 The Android Open Source Project
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

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;

class BenchmarkOperation {
    private BenchmarkOperation() {}

    static void dropShaderCache(
            @NonNull Context context,
            @NonNull ProfileInstallReceiver.ResultDiagnostics callback
    ) {
        File shaderDirectory;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // shaders started using code cache dir once it was added in N
            shaderDirectory = Api24ContextHelper.getDeviceProtectedCodeCacheDir(context);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // getCodeCacheDir was added in L, but not used by platform for shaders until M
            shaderDirectory = Api21ContextHelper.getCodeCacheDir(context);
        } else {
            shaderDirectory = context.getCacheDir();
        }
        if (deleteFilesRecursively(shaderDirectory)) {
            callback.onResultReceived(ProfileInstaller.RESULT_BENCHMARK_OPERATION_SUCCESS, null);
        } else {
            callback.onResultReceived(ProfileInstaller.RESULT_BENCHMARK_OPERATION_FAILURE, null);
        }

    }

    /**
     * Returns true on success
     *
     * If hits a failure to access a directory, returns false but keeps going
     */
    static boolean deleteFilesRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) {
                return false;
            }
            boolean success = true;
            for (File child : children) {
                success = deleteFilesRecursively(child) && success;
            }
            return success;
        } else {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            return true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static class Api21ContextHelper {
        static File getCodeCacheDir(Context context) {
            // Code cache dir added in 21
            return context.getCodeCacheDir();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static class Api24ContextHelper {
        static File getDeviceProtectedCodeCacheDir(Context context) {
            // Code device protected storage added in 24
            return context.createDeviceProtectedStorageContext().getCodeCacheDir();
        }
    }
}
