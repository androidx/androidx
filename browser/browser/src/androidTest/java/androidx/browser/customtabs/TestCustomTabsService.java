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
import android.os.RemoteException;
import android.support.customtabs.ICustomTabsCallback;
import android.support.customtabs.ICustomTabsService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mockito.Mockito;

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
    public static final String ALLOWED_TARGET_ORIGIN = "www.example.com";
    private static TestCustomTabsService sInstance;

    private final CountDownLatch mFileReceivingLatch = new CountDownLatch(1);
    private boolean mPostMessageRequested;
    private CustomTabsSessionToken mSession;
    private ICustomTabsService mMock;

    /** Returns the instance of the Service. Returns null if it hasn't been bound yet. */
    public static TestCustomTabsService getInstance() {
        return sInstance;
    }

    public TestCustomTabsService() {
        mMock = Mockito.mock(ICustomTabsService.class);
    }

    private final ICustomTabsService.Stub mWrapper = new ICustomTabsService.Stub() {
        @Override
        public boolean warmup(long flags) throws RemoteException {
            return false;
        }

        @Override
        public boolean newSession(ICustomTabsCallback callback) throws RemoteException {
            return false;
        }

        @Override
        public boolean newSessionWithExtras(ICustomTabsCallback callback, Bundle extras)
                throws RemoteException {
            return false;
        }

        @Override
        public boolean mayLaunchUrl(ICustomTabsCallback callback, Uri url, Bundle extras,
                List<Bundle> otherLikelyBundles) throws RemoteException {
            return false;
        }

        @Override
        public void prefetch(ICustomTabsCallback callback, Uri url, Bundle options)
                throws RemoteException {
            mMock.prefetch(callback, url, options);
        }

        @Override
        public void prefetchWithMultipleUrls(ICustomTabsCallback callback, List<Uri> urls,
                Bundle options) throws RemoteException {
            mMock.prefetchWithMultipleUrls(callback, urls, options);
        }

        @Override
        public Bundle extraCommand(String commandName, Bundle args) throws RemoteException {
            return null;
        }

        @Override
        public boolean updateVisuals(ICustomTabsCallback callback, Bundle bundle)
                throws RemoteException {
            return false;
        }

        @Override
        public boolean requestPostMessageChannel(ICustomTabsCallback callback,
                Uri postMessageOrigin) throws RemoteException {
            return false;
        }

        @Override
        public boolean requestPostMessageChannelWithExtras(ICustomTabsCallback callback,
                Uri postMessageOrigin, Bundle extras) throws RemoteException {
            return false;
        }
        @Override
        public int postMessage(ICustomTabsCallback callback, String message, Bundle extras)
                throws RemoteException {
            return 0;
        }

        @Override
        public boolean validateRelationship(ICustomTabsCallback callback, int relation, Uri origin,
                Bundle extras) throws RemoteException {
            return false;
        }

        @Override
        public boolean receiveFile(ICustomTabsCallback callback, Uri uri, int purpose,
                Bundle extras) throws RemoteException {
            return false;
        }

        @Override
        public boolean isEngagementSignalsApiAvailable(ICustomTabsCallback customTabsCallback,
                Bundle extras) throws RemoteException {
            return mMock.isEngagementSignalsApiAvailable(customTabsCallback, extras);
        }

        @Override
        public boolean setEngagementSignalsCallback(ICustomTabsCallback customTabsCallback,
                IBinder callback, Bundle extras) throws RemoteException {
            return mMock.setEngagementSignalsCallback(customTabsCallback, callback, extras);
        }
    };

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
    protected void prefetch(@NonNull CustomTabsSessionToken sessionToken,
            @NonNull Uri url, @NonNull PrefetchOptions options) {
    }

    @NonNull
    @Override
    protected Bundle extraCommand(@NonNull String commandName, Bundle args) {
        return Bundle.EMPTY;
    }

    @Override
    protected boolean updateVisuals(@NonNull CustomTabsSessionToken sessionToken, Bundle bundle) {
        return false;
    }

    @Override
    protected boolean requestPostMessageChannel(
            @NonNull CustomTabsSessionToken sessionToken, @NonNull Uri postMessageOrigin) {
        return requestPostMessageChannel(sessionToken, postMessageOrigin, null, new Bundle());
    }

    @Override
    protected boolean requestPostMessageChannel(@NonNull CustomTabsSessionToken sessionToken,
            @NonNull Uri postMessageOrigin, @Nullable Uri postMessageTargetOrigin,
            @NonNull Bundle extras) {
        if (mSession == null) return false;
        if (postMessageTargetOrigin != null
                && !postMessageTargetOrigin.toString().equals(ALLOWED_TARGET_ORIGIN)) return false;
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

    /* package */ ICustomTabsService getStub() {
        return mWrapper;
    }

    /* package */ ICustomTabsService getMock() {
        return mMock;
    }
}
