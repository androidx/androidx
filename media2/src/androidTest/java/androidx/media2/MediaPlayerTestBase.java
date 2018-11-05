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

package androidx.media2;

import static android.content.Context.KEYGUARD_SERVICE;

import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.CallSuper;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Base class for {@link MediaPlayer} tests.
 */
abstract class MediaPlayerTestBase extends MediaTestBase {
    private static final String TAG = "MediaPlayerTest";

    @Rule
    public ActivityTestRule<MediaStubActivity> mActivityRule =
            new ActivityTestRule<>(MediaStubActivity.class);

    Context mContext;
    Resources mResources;
    ExecutorService mExecutor;

    MediaPlayer mPlayer;
    MediaStubActivity mActivity;
    Instrumentation mInstrumentation;

    KeyguardManager mKeyguardManager;
    List<AssetFileDescriptor> mFdsToClose = new ArrayList<>();

    @Before
    @CallSuper
    public void setUp() throws Throwable {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mKeyguardManager = (KeyguardManager)
                mInstrumentation.getTargetContext().getSystemService(KEYGUARD_SERVICE);
        mActivity = mActivityRule.getActivity();
        setKeepScreenOn();

        try {
            mActivityRule.runOnUiThread(new Runnable() {
                public void run() {
                    mPlayer = new MediaPlayer(mActivity);
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "Failed to instantiate MediaPlayer", e);
            fail(e.getMessage());
        }
        mContext = mActivityRule.getActivity();
        mResources = mContext.getResources();
        mExecutor = Executors.newFixedThreadPool(1);
    }

    @After
    @CallSuper
    public void tearDown() throws Exception {
        if (mPlayer != null) {
            mPlayer.close();
            mPlayer = null;
        }
        if (mExecutor != null) {
            mExecutor.shutdown();
            mExecutor = null;
        }
        mActivity = null;
        for (AssetFileDescriptor afd :  mFdsToClose) {
            afd.close();
        }
        mFdsToClose.clear();
    }

    boolean loadResource(int resid) throws Exception {
        AssetFileDescriptor afd = mResources.openRawResourceFd(resid);
        try {
            mPlayer.setMediaItem(new FileMediaItem2.Builder(
                    afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength()).build());
        } finally {
            // Close descriptor later when test finishes since setMediaItem is async operation.
            mFdsToClose.add(afd);
        }
        return true;
    }

    private void setKeepScreenOn() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 27) {
                    mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    mActivity.setTurnScreenOn(true);
                    mActivity.setShowWhenLocked(true);
                    KeyguardManager keyguardManager = (KeyguardManager)
                            mInstrumentation.getTargetContext().getSystemService(KEYGUARD_SERVICE);
                    keyguardManager.requestDismissKeyguard(mActivity, null);
                } else {
                    mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                }
            }
        });
        mInstrumentation.waitForIdleSync();
    }
}
