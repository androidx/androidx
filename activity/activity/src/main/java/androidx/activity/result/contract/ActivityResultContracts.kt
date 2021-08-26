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

import static androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSION_GRANT_RESULTS;

import static java.util.Collections.emptyMap;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.ArrayMap;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * A collection of some standard activity call contracts, as provided by android.
 */
public final class ActivityResultContracts {
    private ActivityResultContracts() {}

    /**
     * An {@link ActivityResultContract} that doesn't do any type conversion, taking raw
     * {@link Intent} as an input and {@link ActivityResult} as an output.
     *
     * Can be used with {@link ActivityResultCaller#registerForActivityResult} to avoid
     * having to manage request codes when calling an activity API for which a type-safe contract is
     * not available.
     */
    public static final class StartActivityForResult
            extends ActivityResultContract<Intent, ActivityResult> {

        /**
         * Key for the extra containing a {@link android.os.Bundle} generated from
         * {@link androidx.core.app.ActivityOptionsCompat#toBundle()} or
         * {@link android.app.ActivityOptions#toBundle()}.
         *
         * This will override any {@link ActivityOptionsCompat} passed to
         * {@link androidx.activity.result.ActivityResultLauncher#launch(Object,
         ActivityOptionsCompat)}
         */
        public static final String EXTRA_ACTIVITY_OPTIONS_BUNDLE = "androidx.activity.result"
                + ".contract.extra.ACTIVITY_OPTIONS_BUNDLE";

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull Intent input) {
            return input;
        }

        @NonNull
        @Override
        public ActivityResult parseResult(
                int resultCode, @Nullable Intent intent) {
            return new ActivityResult(resultCode, intent);
        }
    }

    /**
     * An {@link ActivityResultContract} that calls
     * {@link Activity#startIntentSender(IntentSender, Intent, int, int, int)}.
     *
     * This {@link ActivityResultContract} takes an {@link IntentSenderRequest}, which must be
     * constructed using an {@link IntentSenderRequest.Builder}.
     *
     * If the call to
     * {@link Activity#startIntentSenderForResult(IntentSender, int, Intent, int, int, int)}
     * throws an {@link android.content.IntentSender.SendIntentException} the
     * {@link androidx.activity.result.ActivityResultCallback} will receive an
     * {@link ActivityResult} with an {@link Activity#RESULT_CANCELED} {@code resultCode} and
     * whose intent has the {@link Intent#getAction() action} of
     * {@link #ACTION_INTENT_SENDER_REQUEST} and an extra {@link #EXTRA_SEND_INTENT_EXCEPTION}
     * that contains the thrown exception.
     */
    public static final class StartIntentSenderForResult
            extends ActivityResultContract<IntentSenderRequest, ActivityResult> {

        /**
         * An {@link Intent} action for making a request via the
         * {@link Activity#startIntentSenderForResult} API.
         */
        public static final String ACTION_INTENT_SENDER_REQUEST = "androidx.activity.result"
                + ".contract.action.INTENT_SENDER_REQUEST";

        /**
         * Key for the extra containing the {@link IntentSenderRequest}.
         *
         * @see #ACTION_INTENT_SENDER_REQUEST
         */
        public static final String EXTRA_INTENT_SENDER_REQUEST = "androidx.activity.result"
                + ".contract.extra.INTENT_SENDER_REQUEST";

        /**
         * Key for the extra containing the {@link android.content.IntentSender.SendIntentException}
         * if the call to
         * {@link Activity#startIntentSenderForResult(IntentSender, int, Intent, int, int, int)}
         * fails.
         */
        public static final String EXTRA_SEND_INTENT_EXCEPTION = "androidx.activity.result"
                + ".contract.extra.SEND_INTENT_EXCEPTION";

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull IntentSenderRequest input) {
            return new Intent(ACTION_INTENT_SENDER_REQUEST)
                    .putExtra(EXTRA_INTENT_SENDER_REQUEST, input);
        }

        @NonNull
        @Override
        public ActivityResult parseResult(
                int resultCode, @Nullable Intent intent) {
            return new ActivityResult(resultCode, intent);
        }
    }

    /**
     * An {@link ActivityResultContract} to {@link Activity#requestPermissions request permissions}
     */
    public static final class RequestMultiplePermissions
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
        public Intent createIntent(@NonNull Context context, @NonNull String[] input) {
            return createIntent(input);
        }

        @Override
        public @Nullable SynchronousResult<Map<String, Boolean>> getSynchronousResult(
                @NonNull Context context, @Nullable String[] input) {

            if (input == null || input.length == 0) {
                return new SynchronousResult<>(Collections.<String, Boolean>emptyMap());
            }

            Map<String, Boolean> grantState = new ArrayMap<>();
            boolean allGranted = true;
            for (String permission : input) {
                boolean granted = ContextCompat.checkSelfPermission(context, permission)
                        == PackageManager.PERMISSION_GRANTED;
                grantState.put(permission, granted);
                if (!granted) allGranted = false;
            }

            if (allGranted) {
                return new SynchronousResult<>(grantState);
            }
            return null;
        }

        @NonNull
        @Override
        @SuppressWarnings("MixedMutabilityReturnType")
        public Map<String, Boolean> parseResult(int resultCode,
                @Nullable Intent intent) {
            if (resultCode != Activity.RESULT_OK) return emptyMap();
            if (intent == null) return emptyMap();

            String[] permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS);
            int[] grantResults = intent.getIntArrayExtra(EXTRA_PERMISSION_GRANT_RESULTS);
            if (grantResults == null || permissions == null) return emptyMap();

            Map<String, Boolean> result = new HashMap<>();
            for (int i = 0, size = permissions.length; i < size; i++) {
                result.put(permissions[i], grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
            return result;
        }

        @NonNull
        static Intent createIntent(@NonNull String[] input) {
            return new Intent(ACTION_REQUEST_PERMISSIONS).putExtra(EXTRA_PERMISSIONS, input);
        }
    }

    /**
     * An {@link ActivityResultContract} to {@link Activity#requestPermissions request a permission}
     */
    public static final class RequestPermission extends ActivityResultContract<String, Boolean> {

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull String input) {
            return RequestMultiplePermissions.createIntent(new String[] { input });
        }

        @SuppressWarnings("AutoBoxing")
        @NonNull
        @Override
        public Boolean parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return false;
            int[] grantResults = intent.getIntArrayExtra(EXTRA_PERMISSION_GRANT_RESULTS);
            if (grantResults == null || grantResults.length == 0) return false;
            return grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public @Nullable SynchronousResult<Boolean> getSynchronousResult(
                @NonNull Context context, @Nullable String input) {
            if (input == null) {
                return new SynchronousResult<>(false);
            } else if (ContextCompat.checkSelfPermission(context, input)
                    == PackageManager.PERMISSION_GRANTED) {
                return new SynchronousResult<>(true);
            } else {
                // proceed with permission request
                return null;
            }
        }
    }

    /**
     * An {@link ActivityResultContract} to
     * {@link MediaStore#ACTION_IMAGE_CAPTURE take small a picture} preview, returning it as a
     * {@link Bitmap}.
     * <p>
     * This can be extended to override {@link #createIntent} if you wish to pass additional
     * extras to the Intent created by {@code super.createIntent()}.
     */
    public static class TakePicturePreview extends ActivityResultContract<Void, Bitmap> {

        @CallSuper
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @Nullable Void input) {
            return new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        }

        @Nullable
        @Override
        public final SynchronousResult<Bitmap> getSynchronousResult(@NonNull Context context,
                @Nullable Void input) {
            return null;
        }

        @Nullable
        @Override
        public final Bitmap parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return null;
            return intent.getParcelableExtra("data");
        }
    }

    /**
     * An {@link ActivityResultContract} to
     * {@link MediaStore#ACTION_IMAGE_CAPTURE take a picture} saving it into the provided
     * content-{@link Uri}.
     * <p>
     * Returns {@code true} if the image was saved into the given {@link Uri}.
     * <p>
     * This can be extended to override {@link #createIntent} if you wish to pass additional
     * extras to the Intent created by {@code super.createIntent()}.
     */
    public static class TakePicture extends ActivityResultContract<Uri, Boolean> {

        @CallSuper
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull Uri input) {
            return new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    .putExtra(MediaStore.EXTRA_OUTPUT, input);
        }

        @Nullable
        @Override
        public final SynchronousResult<Boolean> getSynchronousResult(@NonNull Context context,
                @NonNull Uri input) {
            return null;
        }

        @SuppressWarnings("AutoBoxing")
        @NonNull
        @Override
        public final Boolean parseResult(int resultCode, @Nullable Intent intent) {
            return resultCode == Activity.RESULT_OK;
        }
    }

    /**
     * An {@link ActivityResultContract} to
     * {@link MediaStore#ACTION_VIDEO_CAPTURE take a video} saving it into the provided
     * content-{@link Uri}.
     * <p>
     * Returns a thumbnail.
     * <p>
     * This can be extended to override {@link #createIntent} if you wish to pass additional
     * extras to the Intent created by {@code super.createIntent()}.
     *
     * @deprecated The thumbnail bitmap is rarely returned and is not a good signal to determine
     * whether the video was actually successfully captured. Use {@link CaptureVideo} to instead.
     */
    @Deprecated
    public static class TakeVideo extends ActivityResultContract<Uri, Bitmap> {

        @CallSuper
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull Uri input) {
            return new Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                    .putExtra(MediaStore.EXTRA_OUTPUT, input);
        }

        @Nullable
        @Override
        public final SynchronousResult<Bitmap> getSynchronousResult(@NonNull Context context,
                @NonNull Uri input) {
            return null;
        }

        @Nullable
        @Override
        public final Bitmap parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return null;
            return intent.getParcelableExtra("data");
        }
    }

    /**
     * An {@link ActivityResultContract} to
     * {@link MediaStore#ACTION_VIDEO_CAPTURE take a video} saving it into the provided
     * content-{@link Uri}.
     * <p>
     * Returns {@code true} if the video was saved into the given {@link Uri}.
     * <p>
     * This can be extended to override {@link #createIntent} if you wish to pass additional
     * extras to the Intent created by {@code super.createIntent()}.
     */
    public static class CaptureVideo extends ActivityResultContract<Uri, Boolean> {

        @CallSuper
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull Uri input) {
            return new Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                    .putExtra(MediaStore.EXTRA_OUTPUT, input);
        }

        @SuppressWarnings("AutoBoxing")
        @Nullable
        @Override
        public final SynchronousResult<Boolean> getSynchronousResult(@NonNull Context context,
                @NonNull Uri input) {
            return null;
        }

        @SuppressWarnings("AutoBoxing")
        @NonNull
        @Override
        public final Boolean parseResult(int resultCode, @Nullable Intent intent) {
            return resultCode == Activity.RESULT_OK;
        }
    }

    /**
     * An {@link ActivityResultContract} to request the user to pick a contact from the contacts
     * app.
     * <p>
     * The result is a {@code content:} {@link Uri}.
     *
     * @see ContactsContract
     */
    public static final class PickContact extends ActivityResultContract<Void, Uri> {

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @Nullable Void input) {
            return new Intent(Intent.ACTION_PICK).setType(ContactsContract.Contacts.CONTENT_TYPE);
        }

        @Nullable
        @Override
        public Uri parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return null;
            return intent.getData();
        }
    }

    /**
     * An {@link ActivityResultContract} to prompt the user to pick a piece of content, receiving
     * a {@code content://} {@link Uri} for that content that allows you to use
     * {@link android.content.ContentResolver#openInputStream(Uri)} to access the raw data. By
     * default, this adds {@link Intent#CATEGORY_OPENABLE} to only return content that can be
     * represented as a stream.
     * <p>
     * The input is the mime type to filter by, e.g. {@code image/*}.
     * <p>
     * This can be extended to override {@link #createIntent} if you wish to pass additional
     * extras to the Intent created by {@code super.createIntent()}.
     */
    public static class GetContent extends ActivityResultContract<String, Uri> {

        @CallSuper
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull String input) {
            return new Intent(Intent.ACTION_GET_CONTENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(input);
        }

        @Nullable
        @Override
        public final SynchronousResult<Uri> getSynchronousResult(@NonNull Context context,
                @NonNull String input) {
            return null;
        }

        @Nullable
        @Override
        public final Uri parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return null;
            return intent.getData();
        }
    }

    /**
     * An {@link ActivityResultContract} to prompt the user to pick one or more a pieces of
     * content, receiving a {@code content://} {@link Uri} for each piece of content that allows
     * you to use {@link android.content.ContentResolver#openInputStream(Uri)}
     * to access the raw data. By default, this adds {@link Intent#CATEGORY_OPENABLE} to only
     * return content that can be represented as a stream.
     * <p>
     * The input is the mime type to filter by, e.g. {@code image/*}.
     * <p>
     * This can be extended to override {@link #createIntent} if you wish to pass additional
     * extras to the Intent created by {@code super.createIntent()}.
     */
    @RequiresApi(18)
    public static class GetMultipleContents extends ActivityResultContract<String, List<Uri>> {

        @CallSuper
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull String input) {
            return new Intent(Intent.ACTION_GET_CONTENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(input)
                    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        @Nullable
        @Override
        public final SynchronousResult<List<Uri>> getSynchronousResult(@NonNull Context context,
                @NonNull String input) {
            return null;
        }

        @NonNull
        @Override
        public final List<Uri> parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) {
                return Collections.emptyList();
            }
            return getClipDataUris(intent);
        }

        @NonNull
        @SuppressWarnings("MixedMutabilityReturnType")
        static List<Uri> getClipDataUris(@NonNull Intent intent) {
            // Use a LinkedHashSet to maintain any ordering that may be
            // present in the ClipData
            LinkedHashSet<Uri> resultSet = new LinkedHashSet<>();
            if (intent.getData() != null) {
                resultSet.add(intent.getData());
            }
            ClipData clipData = intent.getClipData();
            if (clipData == null && resultSet.isEmpty()) {
                return Collections.emptyList();
            } else if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    if (uri != null) {
                        resultSet.add(uri);
                    }
                }
            }
            return new ArrayList<>(resultSet);
        }
    }

    /**
     * An {@link ActivityResultContract} to prompt the user to open a document, receiving its
     * contents as a {@code file:/http:/content:} {@link Uri}.
     * <p>
     * The input is the mime types to filter by, e.g. {@code image/*}.
     * <p>
     * This can be extended to override {@link #createIntent} if you wish to pass additional
     * extras to the Intent created by {@code super.createIntent()}.
     *
     * @see DocumentsContract
     */
    @RequiresApi(19)
    public static class OpenDocument extends ActivityResultContract<String[], Uri> {

        @CallSuper
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull String[] input) {
            return new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .putExtra(Intent.EXTRA_MIME_TYPES, input)
                    .setType("*/*");
        }

        @Nullable
        @Override
        public final SynchronousResult<Uri> getSynchronousResult(@NonNull Context context,
                @NonNull String[] input) {
            return null;
        }

        @Nullable
        @Override
        public final Uri parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return null;
            return intent.getData();
        }
    }

    /**
     * An {@link ActivityResultContract} to prompt the user to open  (possibly multiple)
     * documents, receiving their contents as {@code file:/http:/content:} {@link Uri}s.
     * <p>
     * The input is the mime types to filter by, e.g. {@code image/*}.
     * <p>
     * This can be extended to override {@link #createIntent} if you wish to pass additional
     * extras to the Intent created by {@code super.createIntent()}.
     *
     * @see DocumentsContract
     */
    @RequiresApi(19)
    public static class OpenMultipleDocuments extends ActivityResultContract<String[], List<Uri>> {

        @CallSuper
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull String[] input) {
            return new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .putExtra(Intent.EXTRA_MIME_TYPES, input)
                    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    .setType("*/*");
        }

        @Nullable
        @Override
        public final SynchronousResult<List<Uri>> getSynchronousResult(@NonNull Context context,
                @NonNull String[] input) {
            return null;
        }

        @NonNull
        @Override
        public final List<Uri> parseResult(int resultCode, @Nullable Intent intent) {
            if (resultCode != Activity.RESULT_OK || intent == null) return Collections.emptyList();
            return GetMultipleContents.getClipDataUris(intent);
        }
    }

    /**
     * An {@link ActivityResultContract} to prompt the user to select a directory, returning the
     * user selection as a {@link Uri}. Apps can fully manage documents within the returned
     * directory.
     * <p>
     * The input is an optional {@link Uri} of the initial starting location.
     * <p>
     * This can be extended to override {@link #createIntent} if you wish to pass additional
     * extras to the Intent created by {@code super.createIntent()}.
     *
     * @see Intent#ACTION_OPEN_DOCUMENT_TREE
     * @see DocumentsContract#buildDocumentUriUsingTree
     * @see DocumentsContract#buildChildDocumentsUriUsingTree
     */
    @RequiresApi(21)
    public static class OpenDocumentTree extends ActivityResultContract<Uri, Uri> {

        @CallSuper
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @Nullable Uri input) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && input != null) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, input);
            }
            return intent;
        }

        @Nullable
        @Override
        public final SynchronousResult<Uri> getSynchronousResult(@NonNull Context context,
                @Nullable Uri input) {
            return null;
        }

        @Nullable
        @Override
        public final Uri parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return null;
            return intent.getData();
        }
    }

    /**
     * An {@link ActivityResultContract} to prompt the user to select a path for creating a new
     * document, returning the {@code content:} {@link Uri} of the item that was created.
     * <p>
     * The input is the suggested name for the new file.
     * <p>
     * This can be extended to override {@link #createIntent} if you wish to pass additional
     * extras to the Intent created by {@code super.createIntent()}.
     */
    @RequiresApi(19)
    public static class CreateDocument extends ActivityResultContract<String, Uri> {

        @CallSuper
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull String input) {
            return new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .setType("*/*")
                    .putExtra(Intent.EXTRA_TITLE, input);
        }

        @Nullable
        @Override
        public final SynchronousResult<Uri> getSynchronousResult(@NonNull Context context,
                @NonNull String input) {
            return null;
        }

        @Nullable
        @Override
        public final Uri parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return null;
            return intent.getData();
        }
    }
}
