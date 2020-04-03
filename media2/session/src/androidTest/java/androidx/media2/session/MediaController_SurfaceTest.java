/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import static android.content.Context.KEYGUARD_SERVICE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.os.Build;
import android.os.Process;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaController#setSurface(Surface)}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaController_SurfaceTest extends MediaSessionTestBase {
    private static final String TAG = "MC_SurfaceTest";

    private Instrumentation mInstrumentation;
    private SurfaceActivity mActivity;
    private MockPlayer mPlayer;
    private MediaSession mSession;
    private MediaController mController;

    @Rule
    public ActivityTestRule<SurfaceActivity> mActivityRule =
            new ActivityTestRule<>(SurfaceActivity.class);

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mActivity = mActivityRule.getActivity();

        setKeepScreenOn();

        mPlayer = new MockPlayer(0);
        mSession = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, new MediaSession.SessionCallback() {
                    @Override
                    public SessionCommandGroup onConnect(
                            @NonNull MediaSession session,
                            @NonNull MediaSession.ControllerInfo controller) {
                        if (Process.myUid() == controller.getUid()) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }
                })
                .setId(TAG)
                .build();
        mController = createController(mSession.getToken());
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();

        mSession.close();
    }

    @Test
    public void setSurface() throws Exception {
        // Set
        final Surface testSurface = mActivity.getSurfaceHolder().getSurface();
        SessionResult result = mController.setSurface(testSurface)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(SessionResult.RESULT_SUCCESS, result.getResultCode());
        assertSame(testSurface, mPlayer.mSurface);

        // Reset
        result = mController.setSurface(null).get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(SessionResult.RESULT_SUCCESS, result.getResultCode());
        assertNull(mPlayer.mSurface);
    }

    private void setKeepScreenOn() throws Exception {
        try {
            setKeepScreenOnOrThrow();
        } catch (Throwable tr) {
            throw new Exception(tr);
        }
    }

    private void setKeepScreenOnOrThrow() throws Throwable {
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
