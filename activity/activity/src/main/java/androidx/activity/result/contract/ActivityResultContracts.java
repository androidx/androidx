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

import static androidx.activity.result.contract.ActivityResultContracts.RequestPermissions.EXTRA_PERMISSION_GRANT_RESULTS;

import static java.util.Collections.emptyMap;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCaller;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
        public Intent createIntent(@NonNull Context context, @NonNull String[] input) {
            return createIntent(input);
        }

        @Override
        public @Nullable SynchronousResult<Map<String, Boolean>> getSynchronousResult(
                @NonNull Context context, @Nullable String[] input) {

            if (input == null || input.length == 0) {
                return new SynchronousResult<Map<String, Boolean>>(Collections.<String, Boolean>emptyMap());
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
                return new SynchronousResult<Map<String, Boolean>>(grantState);
            }
            return null;
        }

        @NonNull
        @Override
        public Map<String, Boolean> parseResult(int resultCode,
                @Nullable Intent intent) {
            if (resultCode != Activity.RESULT_OK) return emptyMap();
            if (intent == null) return emptyMap();

            String[] permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS);
            int[] grantResults = intent.getIntArrayExtra(EXTRA_PERMISSION_GRANT_RESULTS);
            if (grantResults == null || permissions == null) return emptyMap();

            Map<String, Boolean> result = new HashMap<String, Boolean>();
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
     * An {@link ActivityResultContract} to {@link Intent#ACTION_DIAL dial a number}
     */
    public static class Dial extends ActivityResultContract<String, Boolean> {

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull String input) {
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
        public Intent createIntent(@NonNull Context context, @NonNull String input) {
            return RequestPermissions.createIntent(new String[] { input });
        }

        @NonNull
        @Override
        public Boolean parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return false;
            int[] grantResults = intent.getIntArrayExtra(EXTRA_PERMISSION_GRANT_RESULTS);
            if (grantResults == null) return false;
            return grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public @Nullable SynchronousResult<Boolean> getSynchronousResult(
                @NonNull Context context, @Nullable String input) {
            if (input == null) {
                return new SynchronousResult<Boolean>(false);
            } else if (ContextCompat.checkSelfPermission(context, input)
                    == PackageManager.PERMISSION_GRANTED) {
                return new SynchronousResult<Boolean>(true);
            } else {
                // proceed with permission request
                return null;
            }
        }
    }

    /**
     * An {@link ActivityResultContract} to
     * {@link MediaStore#ACTION_IMAGE_CAPTURE take small a picture} preview, returning it as a
     * {@link Bitmap}
     */
    public static class TakePicturePreview extends ActivityResultContract<Void, Bitmap> {

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @Nullable Void input) {
            return new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        }

        @Nullable
        @Override
        public Bitmap parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return null;
            return intent.getParcelableExtra("data");
        }
    }

    /**
     * An {@link ActivityResultContract} to
     * {@link MediaStore#ACTION_IMAGE_CAPTURE take a picture} saving it into the provided
     * content-{@link Uri}
     *
     * Returns a thumbnail.
     */
    public static class TakePicture extends ActivityResultContract<Uri, Bitmap> {

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull Uri input) {
            return new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    .putExtra(MediaStore.EXTRA_OUTPUT, input);
        }

        @Nullable
        @Override
        public Bitmap parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return null;
            return intent.getParcelableExtra("data");
        }
    }

    /**
     * An {@link ActivityResultContract} to
     * {@link MediaStore#ACTION_IMAGE_CAPTURE take a picture} saving it into the provided
     * content-{@link Uri}
     *
     * Returns a thumbnail.
     */
    public static class TakeVideo extends ActivityResultContract<Uri, Bitmap> {

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull Uri input) {
            return new Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                    .putExtra(MediaStore.EXTRA_OUTPUT, input);
        }

        @Nullable
        @Override
        public Bitmap parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return null;
            return intent.getParcelableExtra("data");
        }
    }

    /**
     * An {@link ActivityResultContract} to request the user to pick a contact from the contacts
     * app.
     * The result is a {@code content:} {@link Uri}
     *
     * @see ContactsContract
     */
    public static class PickContact extends ActivityResultContract<Void, Uri> {

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
     * An {@link ActivityResultContract} to send an email.
     *
     * @see Request
     */
    public static class Email extends ActivityResultContract<Email.Request, Boolean> {

        /**
         * The request to send an email.
         */
        public static class Request {
            String[] mTo;
            String[] mCc;
            String[] mBcc;
            Uri[] mAttachments;
            String mSubject;
            String mText;

            /** @see Intent#EXTRA_EMAIL */
            public @NonNull Request to(@Nullable String... to) {
                this.mTo = to;
                return this;
            }

            /** @see Intent#EXTRA_CC */
            public @NonNull Request cc(@Nullable String... cc) {
                this.mCc = cc;
                return this;
            }

            /** @see Intent#EXTRA_BCC */
            public @NonNull Request bcc(@Nullable String... bcc) {
                this.mBcc = bcc;
                return this;
            }

            /** @see Intent#EXTRA_STREAM */
            public @NonNull Request attachments(@Nullable Uri... attachments) {
                this.mAttachments = attachments;
                return this;
            }

            /** @see Intent#EXTRA_SUBJECT */
            public @NonNull Request subject(@Nullable String subject) {
                this.mSubject = subject;
                return this;
            }

            /** @see Intent#EXTRA_TEXT */
            public @NonNull Request text(@Nullable String text) {
                this.mText = text;
                return this;
            }
        }

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull Request input) {
            Intent intent = new Intent(
                    input.mAttachments != null && input.mAttachments.length > 1
                            ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND)
                    .setType("*/*");
            if (input.mTo != null) intent.putExtra(Intent.EXTRA_EMAIL, input.mTo);
            if (input.mCc != null) intent.putExtra(Intent.EXTRA_CC, input.mCc);
            if (input.mBcc != null) intent.putExtra(Intent.EXTRA_BCC, input.mBcc);
            if (input.mAttachments != null) {
                intent.putExtra(Intent.EXTRA_STREAM, input.mAttachments);
            }
            if (input.mSubject != null) intent.putExtra(Intent.EXTRA_SUBJECT, input.mSubject);
            if (input.mText != null) intent.putExtra(Intent.EXTRA_TEXT, input.mText);
            return intent;
        }

        @NonNull
        @Override
        public Boolean parseResult(int resultCode, @Nullable Intent intent) {
            return resultCode == Activity.RESULT_OK;
        }
    }

    /**
     * An {@link ActivityResultContract} to prompt the user to pick a file, receiving its copy as
     * a {@code file:/http:/content:} {@link Uri}
     *
     * The input is the mime type to filter by, e.g. {@code image/*}
     */
    public static class PickFile extends ActivityResultContract<String, Uri> {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull String input) {
            return new Intent(Intent.ACTION_GET_CONTENT).setType(input);
        }

        @Nullable
        @Override
        public Uri parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return null;
            return intent.getData();
        }
    }

    /**
     * An {@link ActivityResultContract} to prompt the user to pick (possibly multiple) files,
     * receiving their copies as
     * a {@code file:/http:/content:} {@link Uri}s
     *
     * The input is the mime type to filter by, e.g. {@code image/*}
     */
    @TargetApi(18)
    public static class PickFiles extends ActivityResultContract<String, List<Uri>> {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull String input) {
            return new Intent(Intent.ACTION_GET_CONTENT)
                    .setType(input)
                    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        @NonNull
        @Override
        public List<Uri> parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) {
                return Collections.<Uri>emptyList();
            }
            return getClipDataUris(intent);
        }

        @NonNull
        static List<Uri> getClipDataUris(@NonNull Intent intent) {
            ClipData clipData = intent.getClipData();
            if (clipData == null) return Collections.<Uri>emptyList();
            ArrayList<Uri> result = new ArrayList<>();
            int size = clipData.getItemCount();
            for (int i = 0; i < size; i++) {
                result.add(clipData.getItemAt(i).getUri());
            }
            return result;
        }
    }

    /**
     * An {@link ActivityResultContract} to prompt the user to open a document, receiving its
     * contents as a {@code file:/http:/content:} {@link Uri}
     *
     * The input is the mime types to filter by, e.g. {@code image/*}
     *
     * @see DocumentsContract
     */
    @TargetApi(19)
    public static class OpenDocument extends ActivityResultContract<String[], Uri> {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull String[] input) {
            return new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .putExtra(Intent.EXTRA_MIME_TYPES, input)
                    .setType("*/*");
        }

        @Nullable
        @Override
        public Uri parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return null;
            return intent.getData();
        }
    }

    /**
     * An {@link ActivityResultContract} to prompt the user to open  (possibly multiple)
     * documents, receiving their contents as {@code file:/http:/content:} {@link Uri}s
     *
     * The input is the mime types to filter by, e.g. {@code image/*}
     *
     * @see DocumentsContract
     */
    @TargetApi(19)
    public static class OpenDocuments extends ActivityResultContract<String[], List<Uri>> {
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
        public List<Uri> parseResult(int resultCode, @Nullable Intent intent) {
            if (resultCode != Activity.RESULT_OK) return null;
            if (intent == null) return null;
            return PickFiles.getClipDataUris(intent);
        }
    }

    /**
     * An {@link ActivityResultContract} to prompt the user to select a directory, returning the
     * user selection as a {@link Uri}.
     *
     * Apps can fully manage documents within the returned directory.
     *
     * The input is an optional {@link Uri} of the initial starting location.
     *
     * @see Intent#ACTION_OPEN_DOCUMENT_TREE
     * @see DocumentsContract#buildDocumentUriUsingTree
     * @see DocumentsContract#buildChildDocumentsUriUsingTree
     */
    @TargetApi(21)
    public static class OpenDocumentTree extends ActivityResultContract<Uri, Uri> {
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
        public Uri parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return null;
            return intent.getData();
        }
    }

    /**
     * An {@link ActivityResultContract} to prompt the user to select a path for creating a new
     * document, returning the {@code content:} {@link Uri} of the item that was created.
     *
     * The input is the suggested name for the new file.
     */
    @TargetApi(19)
    public static class CreateDocument extends ActivityResultContract<String, Uri> {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull String input) {
            return new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .setType("*/*")
                    .putExtra(Intent.EXTRA_TITLE, input);
        }

        @Nullable
        @Override
        public Uri parseResult(int resultCode, @Nullable Intent intent) {
            if (intent == null || resultCode != Activity.RESULT_OK) return null;
            return intent.getData();
        }
    }
}
