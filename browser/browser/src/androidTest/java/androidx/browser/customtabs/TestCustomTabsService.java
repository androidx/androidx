/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.browser.customtabs;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A test class that simulates how a {@link CustomTabsService} would behave.
 */

public class TestCustomTabsService extends CustomTabsService {
    public static final String CALLBACK_BIND_TO_POST_MESSAGE = "BindToPostMessageService";
    private static TestCustomTabsService sInstance;

    private final CountDownLatch mFileReceivingLatch = new CountDownLatch(1);

    private boolean mPostMessageRequested;
    private CustomTabsSessionToken mSession;

    /** Returns the instance of the Service. Returns null if it hasn't been bound yet. */
    public static TestCustomTabsService getInstance() {
        return sInstance;
    }

    @NonNull
    @Override
    public IBinder onBind(Intent intent) {
        sInstance = this;
        return super.onBind(intent);
    }

    @Override
    protected boolean warmup(long flags) {
        return false;
    }

    @Override
    protected boolean newSession(@NonNull CustomTabsSessionToken sessionToken) {
        mSession = sessionToken;
        return true;
    }

    @Override
    protected boolean mayLaunchUrl(@NonNull CustomTabsSessionToken sessionToken,
            @NonNull Uri url, Bundle extras, List<Bundle> otherLikelyBundles) {
        return false;
    }

    @Override
    protected Bundle extraCommand(@NonNull String commandName, Bundle args) {
        return null;
    }

    @Override
    protected boolean updateVisuals(@NonNull CustomTabsSessionToken sessionToken, Bundle bundle) {
        return false;
    }

    @Override
    protected boolean requestPostMessageChannel(
            @NonNull CustomTabsSessionToken sessionToken, @NonNull Uri postMessageOrigin) {
        if (mSession == null) return false;
        mPostMessageRequested = true;
        mSession.getCallback().extraCallback(CALLBACK_BIND_TO_POST_MESSAGE, null);
        return true;
    }

    @Override
    protected int postMessage(@NonNull CustomTabsSessionToken sessionToken, @NonNull String message,
            Bundle extras) {
        if (!mPostMessageRequested) return CustomTabsService.RESULT_FAILURE_DISALLOWED;
        return CustomTabsService.RESULT_SUCCESS;
    }

    @Override
    protected boolean validateRelationship(@NonNull CustomTabsSessionToken sessionToken,
            @Relation int relation, @NonNull Uri origin, Bundle extras) {
        return false;
    }

    @Override
    protected boolean receiveFile(@NonNull CustomTabsSessionToken sessionToken, @NonNull Uri uri,
            int purpose, @Nullable Bundle extras) {
        boolean success = retrieveBitmap(uri);
        if (success) {
            mFileReceivingLatch.countDown();
        }
        return success;
    }

    private boolean retrieveBitmap(Uri uri) {
        try (ParcelFileDescriptor parcelFileDescriptor =
                     getContentResolver().openFileDescriptor(uri, "r")) {
            if (parcelFileDescriptor == null) return false;
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            if (fileDescriptor == null) return false;
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            return bitmap != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Waits until a splash image file is successfully received and decoded in {@link #receiveFile}.
     * Returns whether that happened before timeout.
     * If already received, returns "true" immediately.
     */
    public boolean waitForSplashImageFile(int timeoutMillis) {
        try {
            return mFileReceivingLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }
}
