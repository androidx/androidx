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

package androidx.activity.result.contract;

import static androidx.activity.result.contract.ActivityResultContracts.RequestPermissions.ACTION_REQUEST_PERMISSIONS;
import static androidx.activity.result.contract.ActivityResultContracts.RequestPermissions.EXTRA_PERMISSIONS;
import static androidx.activity.result.contract.ActivityResultContracts.RequestPermissions.EXTRA_PERMISSION_GRANT_RESULTS;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCaller;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of some standard activity call contracts, as provided by android.
 */
public class ActivityResultContracts {
    private ActivityResultContracts() {}

    /**
     * An {@link ActivityResultContract} that doesn't do any type conversion, taking raw
     * {@link Intent} as an input and {@link ActivityResult} as an output.
     *
     * Can be used with {@link ActivityResultCaller#prepareCall} to avoid
     * having to manage request codes when calling an activity API for which a type-safe contract is
     * not available.
     */
    public static class StartActivityForResult
            extends ActivityResultContract<Intent, ActivityResult> {

        @NonNull
        @Override
        public Intent createIntent(@NonNull Intent input) {
            return input;
        }

        @NonNull
        @Override
        public ActivityResult parseResult(int resultCode, @Nullable Intent intent) {
            return new ActivityResult(resultCode, intent);
        }
    }

    /**
     * An {@link ActivityResultContract} to {@link Activity#requestPermissions request permissions}
     */
    public static class RequestPermissions
            extends ActivityResultContract<String[], java.util.Map<String, Boolean>> {


        /**
         * An {@link Intent} action for making a permission request via a regular
         * {@link Activity#startActivityForResult} API.
         *
         * Caller must provide a {@code String[]} extra {@link #EXTRA_PERMISSIONS}
         *
         * Result will be delivered via {@link Activity#onActivityResult(int, int, Intent)} with
         * {@code String[]} {@link #EXTRA_PERMISSIONS} and {@code int[]}
         * {@link #EXTRA_PERMISSION_GRANT_RESULTS}, similar to
         * {@link Activity#onRequestPermissionsResult(int, String[], int[])}
         *
         * @see Activity#requestPermissions(String[], int)
         * @see Activity#onRequestPermissionsResult(int, String[], int[])
         */
        public static final String ACTION_REQUEST_PERMISSIONS =
                "androidx.activity.result.contract.action.REQUEST_PERMISSIONS";

        /**
         * Key for the extra containing all the requested permissions.
         *
         * @see #ACTION_REQUEST_PERMISSIONS
         */
        public static final String EXTRA_PERMISSIONS =
                "androidx.activity.result.contract.extra.PERMISSIONS";

        /**
         * Key for the extra containing whether permissions were granted.
         *
         * @see #ACTION_REQUEST_PERMISSIONS
         */
        public static final String EXTRA_PERMISSION_GRANT_RESULTS =
                "androidx.activity.result.contract.extra.PERMISSION_GRANT_RESULTS";

        @NonNull
        @Override
        public Intent createIntent(@NonNull String[] input) {
            return new Intent(ACTION_REQUEST_PERMISSIONS).putExtra(EXTRA_PERMISSIONS, input);
        }

        @NonNull
        @Override
        public Map<String, Boolean> parseResult(int resultCode, @Nullable Intent intent) {
            if (resultCode != Activity.RESULT_OK) return Collections.emptyMap();
            if (intent == null) return Collections.emptyMap();

            String[] permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS);
            int[] grantResults = intent.getIntArrayExtra(EXTRA_PERMISSION_GRANT_RESULTS);
            if (grantResults == null || permissions == null) return Collections.emptyMap();

            Map<String, Boolean> result = new HashMap<String, Boolean>();
            for (int i = 0, size = permissions.length; i < size; i++) {
                result.put(permissions[i], grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
            return result;
        }
    }

    /**
     * An {@link ActivityResultContract} to {@link Intent#ACTION_DIAL dial a number}
     */
    public static class Dial extends ActivityResultContract<String, Boolean> {

        @NonNull
        @Override
        public Intent createIntent(@NonNull String input) {
            return new Intent(Intent.ACTION_DIAL).setData(Uri.parse("tel:" + input));
        }

        @NonNull
        @Override
        public Boolean parseResult(int resultCode, @Nullable Intent intent) {
            return resultCode == Activity.RESULT_OK;
        }
    }

    /**
     * An {@link ActivityResultContract} to {@link Activity#requestPermissions request a permission}
     */
    public static class RequestPermission extends ActivityResultContract<String, Boolean> {

        @NonNull
        @Override
        public Intent createIntent(@NonNull String input) {
            return new Intent(ACTION_REQUEST_PERMISSIONS)
                    .putExtra(EXTRA_PERMISSIONS, new String[] { input });
        }

        @NonNull
        @Override
        public Boolean parseResult(int resultCode, @Nullable Intent intent) {
            if (resultCode != Activity.RESULT_OK) return false;
            if (intent == null) return false;
            int[] grantResults = intent.getIntArrayExtra(EXTRA_PERMISSION_GRANT_RESULTS);
            if (grantResults == null) return false;
            return grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * An {@link ActivityResultContract} to
     * {@link MediaStore#ACTION_IMAGE_CAPTURE take a small picture} as a {@link Bitmap}
     */
    public static class TakePicture extends ActivityResultContract<Void, Bitmap> {

        @NonNull
        @Override
        public Intent createIntent(@Nullable Void input) {
            return new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        }

        @Nullable
        @Override
        public Bitmap parseResult(int resultCode, @Nullable Intent intent) {
            if (resultCode != Activity.RESULT_OK) return null;
            if (intent == null) return null;
            return intent.getParcelableExtra("data");
        }
    }
}
