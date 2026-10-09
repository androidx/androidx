/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.car.app.activity.renderer.surface;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.car.app.activity.CarAppViewModel;
import androidx.car.app.activity.ServiceDispatcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

/** Tests for {@link TemplateSurfaceView}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class TemplateSurfaceViewTest {
    private final CarAppViewModel mViewModel = mock(CarAppViewModel.class);
    private final ServiceDispatcher mServiceDispatcher =
            new ServiceDispatcher(mViewModel, () -> true);
    private final ISurfaceControl mSurfaceControl = mock(ISurfaceControl.class);

    private TemplateSurfaceView mTemplateSurfaceView;

    @Before
    public void setUp() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        mTemplateSurfaceView = new TemplateSurfaceView(activity, null);
        // Attaches the view to a window, which registers the touch mode listener.
        activity.setContentView(mTemplateSurfaceView);
    }

    @Test
    public void onTouchModeChanged_noServiceDispatcher_doesNotCrash() throws Exception {
        mTemplateSurfaceView.mSurfaceControl = mSurfaceControl;

        dispatchOnTouchModeChanged(true);

        verify(mSurfaceControl, never()).onWindowFocusChanged(anyBoolean(), anyBoolean());
    }

    @Test
    public void onTouchModeChanged_noServiceDispatcherNorSurfaceControl_doesNotCrash() {
        dispatchOnTouchModeChanged(true);
    }

    @Test
    public void onTouchModeChanged_withServiceDispatcher_dispatchesToHost() throws Exception {
        mTemplateSurfaceView.setServiceDispatcher(mServiceDispatcher);
        mTemplateSurfaceView.mSurfaceControl = mSurfaceControl;

        dispatchOnTouchModeChanged(true);

        verify(mSurfaceControl).onWindowFocusChanged(anyBoolean(), anyBoolean());
    }

    @Test
    public void onFocusChanged_noServiceDispatcher_doesNotCrash() throws Exception {
        mTemplateSurfaceView.mSurfaceControl = mSurfaceControl;

        mTemplateSurfaceView.onFocusChanged(true, View.FOCUS_DOWN, null);

        verify(mSurfaceControl, never()).onWindowFocusChanged(anyBoolean(), anyBoolean());
    }

    @Test
    public void onFocusChanged_withServiceDispatcher_dispatchesToHost() throws Exception {
        mTemplateSurfaceView.setServiceDispatcher(mServiceDispatcher);
        mTemplateSurfaceView.mSurfaceControl = mSurfaceControl;

        mTemplateSurfaceView.onFocusChanged(true, View.FOCUS_DOWN, null);

        verify(mSurfaceControl).onWindowFocusChanged(true, mTemplateSurfaceView.isInTouchMode());
    }

    @Test
    public void onCreateInputConnection_noServiceDispatcher_returnsNull() {
        assertThat(mTemplateSurfaceView.onCreateInputConnection(new EditorInfo())).isNull();
    }

    @Test
    public void handleTouchEvent_noServiceDispatcher_returnsFalse() throws Exception {
        mTemplateSurfaceView.mSurfaceControl = mSurfaceControl;
        MotionEvent event = obtainMotionEvent();

        assertThat(mTemplateSurfaceView.handleTouchEvent(event)).isFalse();
        verify(mSurfaceControl, never()).onTouchEvent(any());
        event.recycle();
    }

    @Test
    public void handleTouchEvent_withServiceDispatcher_dispatchesToHost() throws Exception {
        mTemplateSurfaceView.setServiceDispatcher(mServiceDispatcher);
        mTemplateSurfaceView.mSurfaceControl = mSurfaceControl;
        MotionEvent event = obtainMotionEvent();

        assertThat(mTemplateSurfaceView.handleTouchEvent(event)).isTrue();
        verify(mSurfaceControl).onTouchEvent(any());
        event.recycle();
    }

    @Test
    public void dispatchKeyEvent_noServiceDispatcher_doesNotDispatchToHost() throws Exception {
        mTemplateSurfaceView.mSurfaceControl = mSurfaceControl;

        mTemplateSurfaceView.dispatchKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));

        verify(mSurfaceControl, never()).onKeyEvent(any());
    }

    @Test
    public void dispatchKeyEvent_withServiceDispatcher_dispatchesToHost() throws Exception {
        mTemplateSurfaceView.setServiceDispatcher(mServiceDispatcher);
        mTemplateSurfaceView.mSurfaceControl = mSurfaceControl;
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);

        assertThat(mTemplateSurfaceView.dispatchKeyEvent(event)).isTrue();
        verify(mSurfaceControl).onKeyEvent(event);
    }

    private void dispatchOnTouchModeChanged(boolean inTouchMode) {
        // ViewTreeObserver#dispatchOnTouchModeChanged is hidden, so it is invoked reflectively
        // to simulate the framework notifying a touch mode change.
        ReflectionHelpers.callInstanceMethod(
                mTemplateSurfaceView.getViewTreeObserver(),
                "dispatchOnTouchModeChanged",
                ClassParameter.from(boolean.class, inTouchMode));
    }

    private static MotionEvent obtainMotionEvent() {
        long now = SystemClock.uptimeMillis();
        return MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0f, 0f, 0);
    }
}
