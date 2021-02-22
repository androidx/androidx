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

package androidx.appcompat.demo.receivecontent;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.OnReceiveContentListener;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.FileNotFoundException;

/**
 * Sample {@link OnReceiveContentListener} implementation that accepts all URIs, and delegates
 * handling for all other content to the platform.
 */
final class MyReceiver implements OnReceiveContentListener {
    public static final String[] SUPPORTED_MIME_TYPES = new String[]{"image/*"};

    private final AttachmentsRepo mAttachmentsRepo;
    private final AttachmentsRecyclerViewAdapter mAttachmentsRecyclerViewAdapter;

    MyReceiver(@NonNull AttachmentsRepo attachmentsRepo,
            @NonNull AttachmentsRecyclerViewAdapter attachmentsRecyclerViewAdapter) {
        mAttachmentsRepo = attachmentsRepo;
        mAttachmentsRecyclerViewAdapter = attachmentsRecyclerViewAdapter;
    }

    @Nullable
    @Override
    public ContentInfoCompat onReceiveContent(@NonNull View view,
            @NonNull ContentInfoCompat contentInfo) {
        Pair<ContentInfoCompat, ContentInfoCompat> split = contentInfo.partition(
                item -> item.getUri() != null);
        ContentInfoCompat uriContent = split.first;
        ContentInfoCompat remaining = split.second;
        if (uriContent != null) {
            ContentResolver contentResolver = view.getContext().getContentResolver();
            ClipData clip = uriContent.getClip();
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                String mimeType = contentResolver.getType(uri);
                receive(view, uri, mimeType);
            }
        }
        return remaining;
    }

    /**
     * Handles incoming content URIs. If the content is an image, stores it as an attachment in the
     * app's private storage. If the content is any other type, simply shows a toast with the type
     * of the content and its size in bytes.
     */
    private void receive(@NonNull View view, @NonNull Uri uri, @NonNull String mimeType) {
        Log.i(Logcat.TAG, "Receiving " + mimeType + ": " + uri);
        if (ClipDescription.compareMimeTypes(mimeType, "image/*")) {
            createAttachment(uri, mimeType);
        } else {
            showMessage(view, uri, mimeType);
        }
    }

    /**
     * Reads the image at the given URI and writes it to private storage. Then shows the image in
     * the UI by passing the URI pointing to the locally stored copy to the recycler view adapter.
     */
    private void createAttachment(@NonNull Uri uri, @NonNull String mimeType) {
        ListenableFuture<Uri> addAttachmentFuture = MyExecutors.bg().submit(() ->
                mAttachmentsRepo.write(uri)
        );
        Futures.addCallback(addAttachmentFuture, new FutureCallback<Uri>() {
            @SuppressLint("SyntheticAccessor")
            @Override
            public void onSuccess(Uri result) {
                mAttachmentsRecyclerViewAdapter.addAttachment(result);
                mAttachmentsRecyclerViewAdapter.notifyDataSetChanged();
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e(Logcat.TAG,
                        "Error receiving content: uri=" + uri + ", mimeType" + mimeType, t);
            }
        }, MyExecutors.main());
    }

    /**
     * Reads the size of the given content URI and shows a toast with the type of the content and
     * its size in bytes.
     */
    private void showMessage(@NonNull View view, @NonNull Uri uri, @NonNull String mimeType) {
        Context applicationContext = view.getContext().getApplicationContext();
        MyExecutors.bg().execute(() -> {
            ContentResolver contentResolver = applicationContext.getContentResolver();
            long lengthBytes;
            try {
                AssetFileDescriptor fd = contentResolver.openAssetFileDescriptor(uri, "r");
                lengthBytes = fd.getLength();
            } catch (FileNotFoundException e) {
                Log.e(Logcat.TAG, "Error opening content URI: " + uri, e);
                return;
            }
            String msg = "Received " + mimeType + " (" + lengthBytes + " bytes): " + uri;
            Log.i(Logcat.TAG, msg);
            MyExecutors.main().execute(() -> {
                Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show();
            });
        });
    }
}
