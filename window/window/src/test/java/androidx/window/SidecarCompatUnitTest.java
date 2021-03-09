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

package androidx.window;

import static androidx.window.ActivityUtil.getActivityWindowToken;
import static androidx.window.TestFoldingFeatureUtil.invalidFoldBounds;
import static androidx.window.TestFoldingFeatureUtil.validFoldBound;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.graphics.Rect;
import android.os.IBinder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.window.sidecar.SidecarDeviceState;
import androidx.window.sidecar.SidecarDisplayFeature;
import androidx.window.sidecar.SidecarInterface;
import androidx.window.sidecar.SidecarWindowLayoutInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link SidecarCompat} that run on the JVM.
 */
public final class SidecarCompatUnitTest {

    private static final Rect WINDOW_BOUNDS = new Rect(1, 1, 50, 100);

    private Activity mActivity;
    private SidecarCompat mSidecarCompat;

    @Before
    public void setUp() {
        TestWindowBoundsHelper mWindowBoundsHelper = new TestWindowBoundsHelper();
        mWindowBoundsHelper.setCurrentBounds(WINDOW_BOUNDS);
        WindowBoundsHelper.setForTesting(mWindowBoundsHelper);

        mActivity = mock(Activity.class);
        Window window = spy(new TestWindow(mActivity));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        doReturn(params).when(window).getAttributes();
        when(mActivity.getWindow()).thenReturn(window);

        SidecarInterface mockSidecarInterface = mock(SidecarInterface.class);
        when(mockSidecarInterface.getDeviceState()).thenReturn(
                newDeviceState(DeviceState.POSTURE_FLIPPED));
        when(mockSidecarInterface.getWindowLayoutInfo(any())).thenReturn(
                newWindowLayoutInfo(new ArrayList<>()));
        mSidecarCompat = new SidecarCompat(mockSidecarInterface, new SidecarAdapter());
    }

    @After
    public void tearDown() {
        WindowBoundsHelper.setForTesting(null);
    }

    @Test
    public void testGetDeviceState() {
        FakeExtensionImp fakeSidecarImp = new FakeExtensionImp();
        SidecarCompat compat = new SidecarCompat(fakeSidecarImp, new SidecarAdapter());
        ExtensionInterfaceCompat.ExtensionCallbackInterface mockCallback = mock(
                ExtensionInterfaceCompat.ExtensionCallbackInterface.class);
        compat.setExtensionCallback(mockCallback);
        compat.onDeviceStateListenersChanged(false);
        SidecarDeviceState deviceState = newDeviceState(SidecarDeviceState.POSTURE_OPENED);

        fakeSidecarImp.triggerDeviceState(deviceState);

        verify(mockCallback).onDeviceStateChanged(new DeviceState(DeviceState.POSTURE_OPENED));
    }

    @Test
    public void testGetWindowLayout_featureWithEmptyBounds() {
        // Add a feature with an empty bounds to the reported list
        SidecarWindowLayoutInfo originalWindowLayoutInfo =
                mSidecarCompat.mSidecar.getWindowLayoutInfo(getActivityWindowToken(mActivity));
        List<SidecarDisplayFeature> sidecarDisplayFeatures =
                originalWindowLayoutInfo.displayFeatures;
        SidecarDisplayFeature newFeature = new SidecarDisplayFeature();
        newFeature.setRect(new Rect());
        sidecarDisplayFeatures.add(newFeature);

        // Verify that this feature is skipped.
        WindowLayoutInfo windowLayoutInfo = mSidecarCompat.getWindowLayoutInfo(mActivity);

        assertEquals(sidecarDisplayFeatures.size() - 1,
                windowLayoutInfo.getDisplayFeatures().size());
    }

    @Test
    public void testGetWindowLayout_foldWithNonZeroArea() {
        SidecarWindowLayoutInfo originalWindowLayoutInfo =
                mSidecarCompat.mSidecar.getWindowLayoutInfo(mock(IBinder.class));
        List<SidecarDisplayFeature> sidecarDisplayFeatures =
                originalWindowLayoutInfo.displayFeatures;
        // Horizontal fold.
        sidecarDisplayFeatures.add(
                newDisplayFeature(new Rect(0, 1, WINDOW_BOUNDS.width(), 2),
                        SidecarDisplayFeature.TYPE_FOLD));
        // Vertical fold.
        sidecarDisplayFeatures.add(
                newDisplayFeature(new Rect(1, 0, 2, WINDOW_BOUNDS.height()),
                        SidecarDisplayFeature.TYPE_FOLD));

        // Verify that these features are skipped.
        WindowLayoutInfo windowLayoutInfo =
                mSidecarCompat.getWindowLayoutInfo(mActivity);

        assertEquals(sidecarDisplayFeatures.size() - 2,
                windowLayoutInfo.getDisplayFeatures().size());
    }

    @Test
    public void testGetWindowLayout_hingeNotSpanningEntireWindow() {
        SidecarWindowLayoutInfo originalWindowLayoutInfo =
                mSidecarCompat.mSidecar.getWindowLayoutInfo(mock(IBinder.class));
        List<SidecarDisplayFeature> sidecarDisplayFeatures =
                originalWindowLayoutInfo.displayFeatures;
        // Horizontal hinge.
        sidecarDisplayFeatures.add(
                newDisplayFeature(new Rect(0, 1, WINDOW_BOUNDS.width() - 1, 2),
                        SidecarDisplayFeature.TYPE_FOLD));
        // Vertical hinge.
        sidecarDisplayFeatures.add(
                newDisplayFeature(new Rect(1, 0, 2, WINDOW_BOUNDS.height() - 1),
                        SidecarDisplayFeature.TYPE_FOLD));

        // Verify that these features are skipped.
        WindowLayoutInfo windowLayoutInfo =
                mSidecarCompat.getWindowLayoutInfo(mActivity);

        assertEquals(sidecarDisplayFeatures.size() - 2,
                windowLayoutInfo.getDisplayFeatures().size());
    }

    @Test
    public void testGetWindowLayout_foldNotSpanningEntireWindow() {
        SidecarWindowLayoutInfo originalWindowLayoutInfo =
                mSidecarCompat.mSidecar.getWindowLayoutInfo(mock(IBinder.class));
        List<SidecarDisplayFeature> sidecarDisplayFeatures =
                originalWindowLayoutInfo.displayFeatures;
        // Horizontal fold.
        sidecarDisplayFeatures.add(
                newDisplayFeature(new Rect(0, 1, WINDOW_BOUNDS.width() - 1, 2),
                        SidecarDisplayFeature.TYPE_FOLD));
        // Vertical fold.
        sidecarDisplayFeatures.add(
                newDisplayFeature(new Rect(1, 0, 2, WINDOW_BOUNDS.height() - 1),
                        SidecarDisplayFeature.TYPE_FOLD));

        // Verify that these features are skipped.
        WindowLayoutInfo windowLayoutInfo =
                mSidecarCompat.getWindowLayoutInfo(mActivity);

        assertEquals(sidecarDisplayFeatures.size() - 2,
                windowLayoutInfo.getDisplayFeatures().size());
    }

    @Test
    public void testOnWindowLayoutChangeListenerAdded() {
        IBinder expectedToken = mock(IBinder.class);
        mActivity.getWindow().getAttributes().token = expectedToken;
        mSidecarCompat.onWindowLayoutChangeListenerAdded(mActivity);
        verify(mSidecarCompat.mSidecar).onWindowLayoutChangeListenerAdded(eq(expectedToken));
    }

    @Test
    public void testOnWindowLayoutChangeListenerAdded_emitInitialValueDelayed() {
        SidecarWindowLayoutInfo layoutInfo = new SidecarWindowLayoutInfo();
        WindowLayoutInfo expectedLayoutInfo = new WindowLayoutInfo(new ArrayList<>());
        ExtensionInterfaceCompat.ExtensionCallbackInterface listener =
                mock(ExtensionInterfaceCompat.ExtensionCallbackInterface.class);
        mSidecarCompat.setExtensionCallback(listener);
        when(mSidecarCompat.mSidecar.getWindowLayoutInfo(any())).thenReturn(layoutInfo);
        View fakeView = mock(View.class);
        Window mockWindow = mock(Window.class);
        when(mockWindow.getAttributes()).thenReturn(new WindowManager.LayoutParams());
        doAnswer(invocation -> {
            View.OnAttachStateChangeListener stateChangeListener = invocation.getArgument(0);
            mockWindow.getAttributes().token = mock(IBinder.class);
            stateChangeListener.onViewAttachedToWindow(fakeView);
            return null;
        }).when(fakeView).addOnAttachStateChangeListener(any());
        when(mockWindow.getDecorView()).thenReturn(fakeView);
        when(mActivity.getWindow()).thenReturn(mockWindow);

        mSidecarCompat.onWindowLayoutChangeListenerAdded(mActivity);

        verify(listener).onWindowLayoutChanged(mActivity, expectedLayoutInfo);
        verify(mSidecarCompat.mSidecar).onWindowLayoutChangeListenerAdded(
                getActivityWindowToken(mActivity));
        verify(fakeView).addOnAttachStateChangeListener(any());
    }

    @Test
    public void testOnWindowLayoutChangeListenerRemoved() {
        IBinder windowToken = getActivityWindowToken(mActivity);
        mSidecarCompat.onWindowLayoutChangeListenerRemoved(mActivity);
        verify(mSidecarCompat.mSidecar).onWindowLayoutChangeListenerRemoved(eq(windowToken));
    }

    @Test
    public void testOnDeviceStateListenersChanged() {
        mSidecarCompat.onDeviceStateListenersChanged(true);
        verify(mSidecarCompat.mSidecar).onDeviceStateListenersChanged(eq(true));
    }

    @Test
    public void testOnDeviceStateListenersAdded_emitInitialValue() {
        SidecarDeviceState deviceState = new SidecarDeviceState();
        DeviceState expectedDeviceState = new DeviceState(DeviceState.POSTURE_UNKNOWN);
        ExtensionInterfaceCompat.ExtensionCallbackInterface listener =
                mock(ExtensionInterfaceCompat.ExtensionCallbackInterface.class);
        mSidecarCompat.setExtensionCallback(listener);
        when(mSidecarCompat.mSidecar.getDeviceState()).thenReturn(deviceState);

        mSidecarCompat.onDeviceStateListenersChanged(false);

        verify(listener).onDeviceStateChanged(expectedDeviceState);
    }

    @Test
    public void testExtensionCallback_deduplicateValues() {
        ExtensionInterfaceCompat.ExtensionCallbackInterface callback = mock(
                ExtensionInterfaceCompat.ExtensionCallbackInterface.class);
        FakeExtensionImp fakeExtensionImp = new FakeExtensionImp();
        SidecarCompat compat = new SidecarCompat(fakeExtensionImp, new SidecarAdapter());
        compat.setExtensionCallback(callback);
        mActivity.getWindow().getAttributes().token = mock(IBinder.class);

        compat.onWindowLayoutChangeListenerAdded(mActivity);
        fakeExtensionImp.triggerDeviceState(fakeExtensionImp.getDeviceState());
        fakeExtensionImp.triggerDeviceState(fakeExtensionImp.getDeviceState());

        verify(callback, times(1)).onWindowLayoutChanged(any(), any());
    }

    private static SidecarDisplayFeature newDisplayFeature(Rect rect, int type) {
        SidecarDisplayFeature feature = new SidecarDisplayFeature();
        feature.setRect(rect);
        feature.setType(type);
        return feature;
    }

    private static SidecarWindowLayoutInfo newWindowLayoutInfo(
            List<SidecarDisplayFeature> features) {
        SidecarWindowLayoutInfo info = new SidecarWindowLayoutInfo();
        info.displayFeatures = new ArrayList<>();
        info.displayFeatures.addAll(features);
        return info;
    }

    private static SidecarDeviceState newDeviceState(int posture) {
        SidecarDeviceState state = new SidecarDeviceState();
        state.posture = posture;
        return state;
    }

    private static final class FakeExtensionImp implements SidecarInterface {

        private SidecarInterface.SidecarCallback mCallback;
        private List<IBinder> mTokens = new ArrayList<>();

        FakeExtensionImp() {
            mCallback = new SidecarInterface.SidecarCallback() {
                @Override
                public void onDeviceStateChanged(@NonNull SidecarDeviceState newDeviceState) {

                }

                @Override
                public void onWindowLayoutChanged(@NonNull IBinder windowToken,
                        @NonNull SidecarWindowLayoutInfo newLayout) {

                }
            };
        }

        @Override
        public void setSidecarCallback(@NonNull SidecarCallback callback) {
            mCallback = callback;
        }

        @NonNull
        @Override
        public SidecarWindowLayoutInfo getWindowLayoutInfo(@NonNull IBinder windowToken) {
            return null;
        }

        @Override
        public void onWindowLayoutChangeListenerAdded(@NonNull IBinder windowToken) {

        }

        @Override
        public void onWindowLayoutChangeListenerRemoved(@NonNull IBinder windowToken) {

        }

        @NonNull
        @Override
        public SidecarDeviceState getDeviceState() {
            SidecarDeviceState state = new SidecarDeviceState();
            return state;
        }

        @Override
        public void onDeviceStateListenersChanged(boolean isEmpty) {

        }

        void triggerMalformedSignal() {
            triggerSignal(malformedWindowLayoutInfo());
        }

        void triggerGoodSignal() {
            triggerSignal(validWindowLayoutInfo());
        }

        void triggerSignal(SidecarWindowLayoutInfo info) {
            for (IBinder token: mTokens) {
                triggerSignal(token, info);
            }
        }

        void triggerSignal(IBinder token, SidecarWindowLayoutInfo info) {
            mCallback.onWindowLayoutChanged(token, info);
        }

        public void triggerDeviceState(SidecarDeviceState state) {
            mCallback.onDeviceStateChanged(state);

        }

        private SidecarWindowLayoutInfo malformedWindowLayoutInfo() {
            List<SidecarDisplayFeature> malformedFeatures = new ArrayList<>();

            for (Rect malformedBound : invalidFoldBounds(WINDOW_BOUNDS)) {
                malformedFeatures.add(newDisplayFeature(malformedBound,
                        SidecarDisplayFeature.TYPE_FOLD));
            }

            for (Rect malformedBound : invalidFoldBounds(WINDOW_BOUNDS)) {
                malformedFeatures.add(newDisplayFeature(malformedBound,
                        SidecarDisplayFeature.TYPE_HINGE));
            }

            return newWindowLayoutInfo(malformedFeatures);
        }

        private SidecarWindowLayoutInfo validWindowLayoutInfo() {
            List<SidecarDisplayFeature> goodFeatures = new ArrayList<>();

            goodFeatures.add(newDisplayFeature(validFoldBound(WINDOW_BOUNDS),
                    SidecarDisplayFeature.TYPE_FOLD));

            return newWindowLayoutInfo(goodFeatures);
        }
    }
}
