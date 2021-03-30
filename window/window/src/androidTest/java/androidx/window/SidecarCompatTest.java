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

import static androidx.window.SidecarAdapter.getSidecarDisplayFeatures;
import static androidx.window.SidecarAdapter.setSidecarDevicePosture;
import static androidx.window.SidecarAdapter.setSidecarDisplayFeatures;
import static androidx.window.TestFoldingFeatureUtil.invalidFoldBounds;
import static androidx.window.TestFoldingFeatureUtil.validFoldBound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.graphics.Rect;
import android.os.IBinder;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.window.sidecar.SidecarDeviceState;
import androidx.window.sidecar.SidecarDisplayFeature;
import androidx.window.sidecar.SidecarInterface;
import androidx.window.sidecar.SidecarWindowLayoutInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link SidecarCompat} implementation of {@link ExtensionInterfaceCompat}. This class
 * uses a mocked Sidecar to verify the behavior of the implementation on any hardware.
 * <p>Because this class extends {@link SidecarCompatDeviceTest}, it will also run the mocked
 * versions of methods defined in {@link CompatDeviceTestInterface}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class SidecarCompatTest extends WindowTestBase
        implements CompatTestInterface {
    private static final Rect WINDOW_BOUNDS = new Rect(1, 1, 50, 100);

    private Activity mActivity;
    private SidecarCompat mSidecarCompat;

    @Before
    public void setUp() {
        mActivity = mock(Activity.class);
        mSidecarCompat = new SidecarCompat(mock(SidecarInterface.class), new SidecarAdapter());

        when(mActivity.getResources())
                .thenReturn(ApplicationProvider.getApplicationContext().getResources());

        Window window = spy(new TestWindow(mActivity));
        window.getAttributes().token = mock(IBinder.class);
        when(mActivity.getWindow()).thenReturn(window);

        TestWindowBoundsHelper mWindowBoundsHelper = new TestWindowBoundsHelper();
        mWindowBoundsHelper.setCurrentBounds(WINDOW_BOUNDS);
        WindowBoundsHelper.setForTesting(mWindowBoundsHelper);

        // Setup mocked sidecar responses
        SidecarDeviceState defaultDeviceState = new SidecarDeviceState();
        setSidecarDevicePosture(defaultDeviceState, SidecarDeviceState.POSTURE_HALF_OPENED);
        when(mSidecarCompat.mSidecar.getDeviceState()).thenReturn(defaultDeviceState);

        SidecarDisplayFeature sidecarDisplayFeature = newDisplayFeature(
                new Rect(0, 1, WINDOW_BOUNDS.width(), 1), SidecarDisplayFeature.TYPE_HINGE);
        SidecarWindowLayoutInfo sidecarWindowLayoutInfo = new SidecarWindowLayoutInfo();
        List<SidecarDisplayFeature> displayFeatures = new ArrayList<>();
        displayFeatures.add(sidecarDisplayFeature);
        setSidecarDisplayFeatures(sidecarWindowLayoutInfo, displayFeatures);
        when(mSidecarCompat.mSidecar.getWindowLayoutInfo(any()))
                .thenReturn(sidecarWindowLayoutInfo);
    }

    @After
    public void tearDown() {
        WindowBoundsHelper.setForTesting(null);
    }

    @Test
    @Override
    public void testGetDeviceState() {
        FakeExtensionImp fakeSidecarImp = new FakeExtensionImp(
                newDeviceState(SidecarDeviceState.POSTURE_OPENED),
                newWindowLayoutInfo(Collections.emptyList()));
        SidecarCompat compat = new SidecarCompat(fakeSidecarImp, new SidecarAdapter());
        ExtensionInterfaceCompat.ExtensionCallbackInterface mockCallback = mock(
                ExtensionInterfaceCompat.ExtensionCallbackInterface.class);
        compat.setExtensionCallback(mockCallback);
        compat.onDeviceStateListenersChanged(false);
        SidecarDeviceState deviceState = newDeviceState(SidecarDeviceState.POSTURE_HALF_OPENED);

        fakeSidecarImp.triggerDeviceState(deviceState);

        verify(mockCallback, atLeastOnce()).onDeviceStateChanged(any());
    }

    @Test
    @Override
    public void testGetWindowLayout() {
        FakeExtensionImp fakeSidecarImp = new FakeExtensionImp(
                newDeviceState(SidecarDeviceState.POSTURE_OPENED),
                newWindowLayoutInfo(Collections.emptyList()));
        SidecarCompat compat = new SidecarCompat(fakeSidecarImp, new SidecarAdapter());
        ExtensionInterfaceCompat.ExtensionCallbackInterface mockCallback = mock(
                ExtensionInterfaceCompat.ExtensionCallbackInterface.class);
        compat.setExtensionCallback(mockCallback);
        compat.onWindowLayoutChangeListenerAdded(mActivity);

        fakeSidecarImp.triggerGoodSignal();

        verify(mockCallback, atLeastOnce()).onWindowLayoutChanged(eq(mActivity),
                any(WindowLayoutInfo.class));
    }

    @Test
    public void testGetWindowLayout_featureWithEmptyBounds() {
        // Add a feature with an empty bounds to the reported list
        SidecarWindowLayoutInfo originalWindowLayoutInfo =
                mSidecarCompat.mSidecar.getWindowLayoutInfo(getActivityWindowToken(mActivity));
        List<SidecarDisplayFeature> sidecarDisplayFeatures =
                getSidecarDisplayFeatures(originalWindowLayoutInfo);
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
                getSidecarDisplayFeatures(originalWindowLayoutInfo);
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
                getSidecarDisplayFeatures(originalWindowLayoutInfo);
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
                getSidecarDisplayFeatures(originalWindowLayoutInfo);
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

    @Override
    public void testExtensionCallback_filterRemovesInvalidValues() {
        FakeExtensionImp fakeSidecarImp = new FakeExtensionImp(
                newDeviceState(SidecarDeviceState.POSTURE_OPENED),
                newWindowLayoutInfo(Collections.emptyList()));
        SidecarCompat compat = new SidecarCompat(fakeSidecarImp, new SidecarAdapter());
        ExtensionInterfaceCompat.ExtensionCallbackInterface mockCallback = mock(
                ExtensionInterfaceCompat.ExtensionCallbackInterface.class);
        compat.setExtensionCallback(mockCallback);
        compat.onWindowLayoutChangeListenerAdded(mActivity);

        fakeSidecarImp.triggerMalformedSignal();

        verify(mockCallback).onWindowLayoutChanged(any(),
                argThat(windowLayoutInfo -> windowLayoutInfo.getDisplayFeatures().isEmpty()));
    }

    @Test
    @Override
    public void testSetExtensionCallback() {
        ArgumentCaptor<SidecarInterface.SidecarCallback> sidecarCallbackCaptor =
                ArgumentCaptor.forClass(SidecarInterface.SidecarCallback.class);

        // Verify that the sidecar got the callback set
        ExtensionInterfaceCompat.ExtensionCallbackInterface callback =
                mock(ExtensionInterfaceCompat.ExtensionCallbackInterface.class);
        mSidecarCompat.setExtensionCallback(callback);

        verify(mSidecarCompat.mSidecar).setSidecarCallback(sidecarCallbackCaptor.capture());

        // Verify that the callback set for sidecar propagates the device state callback
        SidecarDeviceState sidecarDeviceState = new SidecarDeviceState();
        setSidecarDevicePosture(sidecarDeviceState, SidecarDeviceState.POSTURE_HALF_OPENED);

        sidecarCallbackCaptor.getValue().onDeviceStateChanged(sidecarDeviceState);
        ArgumentCaptor<DeviceState> deviceStateCaptor = ArgumentCaptor.forClass(DeviceState.class);
        verify(callback).onDeviceStateChanged(deviceStateCaptor.capture());
        assertEquals(DeviceState.POSTURE_HALF_OPENED, deviceStateCaptor.getValue().getPosture());

        // Verify that the callback set for sidecar propagates the window layout callback when a
        // window layout changed listener has been added.
        mSidecarCompat.onWindowLayoutChangeListenerAdded(mActivity);
        Rect bounds = new Rect(0, 1, WINDOW_BOUNDS.width(), 1);
        SidecarDisplayFeature sidecarDisplayFeature = newDisplayFeature(bounds,
                SidecarDisplayFeature.TYPE_HINGE);
        SidecarWindowLayoutInfo sidecarWindowLayoutInfo = new SidecarWindowLayoutInfo();
        List<SidecarDisplayFeature> displayFeatures = new ArrayList<>();
        displayFeatures.add(sidecarDisplayFeature);
        setSidecarDisplayFeatures(sidecarWindowLayoutInfo, displayFeatures);

        sidecarCallbackCaptor.getValue().onWindowLayoutChanged(getActivityWindowToken(mActivity),
                sidecarWindowLayoutInfo);
        ArgumentCaptor<WindowLayoutInfo> windowLayoutInfoCaptor =
                ArgumentCaptor.forClass(WindowLayoutInfo.class);
        verify(callback).onWindowLayoutChanged(eq(mActivity),
                windowLayoutInfoCaptor.capture());

        WindowLayoutInfo capturedLayout = windowLayoutInfoCaptor.getValue();
        assertEquals(1, capturedLayout.getDisplayFeatures().size());
        DisplayFeature capturedDisplayFeature = capturedLayout.getDisplayFeatures().get(0);
        FoldingFeature foldingFeature = (FoldingFeature) capturedDisplayFeature;
        assertNotNull(foldingFeature);
        assertEquals(bounds, capturedDisplayFeature.getBounds());
    }

    @Test
    public void testMissingCallToOnWindowLayoutChangedListenerAdded() {
        ArgumentCaptor<SidecarInterface.SidecarCallback> sidecarCallbackCaptor =
                ArgumentCaptor.forClass(SidecarInterface.SidecarCallback.class);

        // Verify that the sidecar got the callback set
        ExtensionInterfaceCompat.ExtensionCallbackInterface callback =
                mock(ExtensionInterfaceCompat.ExtensionCallbackInterface.class);
        mSidecarCompat.setExtensionCallback(callback);

        verify(mSidecarCompat.mSidecar).setSidecarCallback(sidecarCallbackCaptor.capture());

        // Verify that the callback set for sidecar propagates the window layout callback when a
        // window layout changed listener has been added.
        SidecarDisplayFeature sidecarDisplayFeature = new SidecarDisplayFeature();
        sidecarDisplayFeature.setType(SidecarDisplayFeature.TYPE_HINGE);
        Rect bounds = new Rect(1, 2, 3, 4);
        sidecarDisplayFeature.setRect(bounds);
        SidecarWindowLayoutInfo sidecarWindowLayoutInfo = new SidecarWindowLayoutInfo();
        List<SidecarDisplayFeature> displayFeatures = new ArrayList<>();
        displayFeatures.add(sidecarDisplayFeature);
        setSidecarDisplayFeatures(sidecarWindowLayoutInfo, displayFeatures);

        IBinder windowToken = mock(IBinder.class);
        sidecarCallbackCaptor.getValue().onWindowLayoutChanged(windowToken,
                sidecarWindowLayoutInfo);
        verifyZeroInteractions(callback);
    }

    @Test
    @Override
    public void testOnWindowLayoutChangeListenerAdded() {
        IBinder windowToken = getActivityWindowToken(mActivity);
        mSidecarCompat.onWindowLayoutChangeListenerAdded(mActivity);
        verify(mSidecarCompat.mSidecar).onWindowLayoutChangeListenerAdded(eq(windowToken));
    }

    @Test
    public void testOnWindowLayoutChangeListenerAdded_emitInitialValue() {
        SidecarWindowLayoutInfo layoutInfo = new SidecarWindowLayoutInfo();
        WindowLayoutInfo expectedLayoutInfo = new WindowLayoutInfo(new ArrayList<>());
        ExtensionInterfaceCompat.ExtensionCallbackInterface listener =
                mock(ExtensionInterfaceCompat.ExtensionCallbackInterface.class);
        mSidecarCompat.setExtensionCallback(listener);
        when(mSidecarCompat.mSidecar.getWindowLayoutInfo(any())).thenReturn(layoutInfo);

        mSidecarCompat.onWindowLayoutChangeListenerAdded(mActivity);

        verify(listener).onWindowLayoutChanged(mActivity, expectedLayoutInfo);
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
        Window fakeWindow = new TestWindow(mActivity, fakeView);
        doAnswer(invocation -> {
            View.OnAttachStateChangeListener stateChangeListener = invocation.getArgument(0);
            fakeWindow.getAttributes().token = mock(IBinder.class);
            stateChangeListener.onViewAttachedToWindow(fakeView);
            return null;
        }).when(fakeView).addOnAttachStateChangeListener(any());
        when(mActivity.getWindow()).thenReturn(fakeWindow);

        mSidecarCompat.onWindowLayoutChangeListenerAdded(mActivity);

        verify(listener).onWindowLayoutChanged(mActivity, expectedLayoutInfo);
    }

    @Test
    @Override
    public void testOnWindowLayoutChangeListenerRemoved() {
        IBinder windowToken = getActivityWindowToken(mActivity);
        mSidecarCompat.onWindowLayoutChangeListenerRemoved(mActivity);
        verify(mSidecarCompat.mSidecar).onWindowLayoutChangeListenerRemoved(eq(windowToken));
    }

    @Test
    @Override
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
    public void testOnDeviceStateChangedUpdatesWindowLayout() {
        FakeExtensionImp fakeSidecarImp = new FakeExtensionImp(
                newDeviceState(SidecarDeviceState.POSTURE_CLOSED),
                validWindowLayoutInfo());
        SidecarCompat compat = new SidecarCompat(fakeSidecarImp, new SidecarAdapter());
        ExtensionInterfaceCompat.ExtensionCallbackInterface mockCallback = mock(
                ExtensionInterfaceCompat.ExtensionCallbackInterface.class);
        compat.setExtensionCallback(mockCallback);
        compat.onWindowLayoutChangeListenerAdded(mActivity);
        ArgumentCaptor<WindowLayoutInfo> windowLayoutCaptor = ArgumentCaptor.forClass(
                WindowLayoutInfo.class);

        reset(mockCallback);
        fakeSidecarImp.triggerDeviceState(newDeviceState(SidecarDeviceState.POSTURE_OPENED));
        verify(mockCallback).onWindowLayoutChanged(eq(mActivity), windowLayoutCaptor.capture());
        FoldingFeature capturedFoldingFeature = (FoldingFeature) windowLayoutCaptor.getValue()
                .getDisplayFeatures().get(0);
        assertEquals(FoldingFeature.STATE_FLAT, capturedFoldingFeature.getState());

        reset(mockCallback);
        fakeSidecarImp.triggerDeviceState(newDeviceState(SidecarDeviceState.POSTURE_HALF_OPENED));
        verify(mockCallback).onWindowLayoutChanged(eq(mActivity), windowLayoutCaptor.capture());
        capturedFoldingFeature = (FoldingFeature) windowLayoutCaptor.getValue().getDisplayFeatures()
                .get(0);
        assertEquals(FoldingFeature.STATE_HALF_OPENED, capturedFoldingFeature.getState());

        // No display features must be reported in closed state or flipped state.
        reset(mockCallback);
        fakeSidecarImp.triggerDeviceState(newDeviceState(SidecarDeviceState.POSTURE_CLOSED));
        fakeSidecarImp.triggerDeviceState(newDeviceState(SidecarDeviceState.POSTURE_FLIPPED));
        verify(mockCallback).onWindowLayoutChanged(eq(mActivity), windowLayoutCaptor.capture());
        assertTrue(windowLayoutCaptor.getValue().getDisplayFeatures().isEmpty());
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
        setSidecarDisplayFeatures(info, features);
        return info;
    }

    private static SidecarWindowLayoutInfo validWindowLayoutInfo() {
        List<SidecarDisplayFeature> goodFeatures = new ArrayList<>();

        goodFeatures.add(newDisplayFeature(validFoldBound(WINDOW_BOUNDS),
                SidecarDisplayFeature.TYPE_FOLD));

        return newWindowLayoutInfo(goodFeatures);
    }

    private static SidecarDeviceState newDeviceState(int posture) {
        SidecarDeviceState state = new SidecarDeviceState();
        setSidecarDevicePosture(state, posture);
        return state;
    }

    private static final class FakeExtensionImp implements SidecarInterface {

        private SidecarDeviceState mDeviceState;
        private SidecarWindowLayoutInfo mInfo;
        private SidecarInterface.SidecarCallback mCallback;
        private List<IBinder> mTokens = new ArrayList<>();

        FakeExtensionImp(@NonNull SidecarDeviceState deviceState,
                @NonNull SidecarWindowLayoutInfo info) {
            mDeviceState = deviceState;
            mInfo = info;
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
            return mInfo;
        }

        @Override
        public void onWindowLayoutChangeListenerAdded(@NonNull IBinder windowToken) {
            mTokens.add(windowToken);
        }

        @Override
        public void onWindowLayoutChangeListenerRemoved(@NonNull IBinder windowToken) {
            mTokens.remove(windowToken);
        }

        @NonNull
        @Override
        public SidecarDeviceState getDeviceState() {
            return mDeviceState;
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
            mInfo = info;
            for (IBinder token : mTokens) {
                mCallback.onWindowLayoutChanged(token, info);
            }
        }

        public void triggerDeviceState(SidecarDeviceState state) {
            mDeviceState = state;
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
    }
}
