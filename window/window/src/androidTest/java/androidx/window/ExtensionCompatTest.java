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

import static androidx.window.ExtensionInterfaceCompat.ExtensionCallbackInterface;
import static androidx.window.TestBoundUtil.invalidFoldBounds;
import static androidx.window.TestBoundUtil.invalidHingeBounds;
import static androidx.window.TestBoundUtil.validFoldBound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.window.extensions.ExtensionDeviceState;
import androidx.window.extensions.ExtensionDisplayFeature;
import androidx.window.extensions.ExtensionInterface;
import androidx.window.extensions.ExtensionWindowLayoutInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link ExtensionCompat} implementation of {@link ExtensionInterfaceCompat}. This
 * class uses a mocked Extension to verify the behavior of the implementation on any hardware.
 * <p>Because this class extends {@link ExtensionCompatDeviceTest}, it will also run the mocked
 * versions of methods defined in {@link CompatDeviceTestInterface}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class ExtensionCompatTest extends WindowTestBase
        implements CompatTestInterface {
    private static final Rect WINDOW_BOUNDS = new Rect(0, 0, 50, 100);

    ExtensionCompat mExtensionCompat;
    private ExtensionInterface mMockExtensionInterface;
    private Activity mActivity;

    @Before
    public void setUp() {
        mMockExtensionInterface = mock(ExtensionInterface.class);
        mExtensionCompat = new ExtensionCompat(mMockExtensionInterface, new ExtensionAdapter());
        mActivity = mock(Activity.class);

        TestWindowBoundsHelper mWindowBoundsHelper = new TestWindowBoundsHelper();
        mWindowBoundsHelper.setCurrentBounds(WINDOW_BOUNDS);
        WindowBoundsHelper.setForTesting(mWindowBoundsHelper);

    }

    @After
    public void tearDown() {
        WindowBoundsHelper.setForTesting(null);
    }

    @Test
    @Override
    public void testGetDeviceState() {
        FakeExtensionImp fakeExtensionImp = new FakeExtensionImp();
        ExtensionCompat compat = new ExtensionCompat(fakeExtensionImp, new ExtensionAdapter());
        ExtensionCallbackInterface mockCallback = mock(ExtensionCallbackInterface.class);
        compat.setExtensionCallback(mockCallback);
        compat.onWindowLayoutChangeListenerAdded(mock(Activity.class));
        ExtensionDeviceState deviceState =
                new ExtensionDeviceState(ExtensionDeviceState.POSTURE_OPENED);

        fakeExtensionImp.triggerDeviceState(deviceState);

        verify(mockCallback).onDeviceStateChanged(new DeviceState(DeviceState.POSTURE_OPENED));
    }

    @Test
    @Override
    public void testGetWindowLayout() {
        FakeExtensionImp fakeExtensionImp = new FakeExtensionImp();
        ExtensionCompat compat = new ExtensionCompat(fakeExtensionImp, new ExtensionAdapter());
        ExtensionCallbackInterface mockCallback = mock(ExtensionCallbackInterface.class);
        compat.setExtensionCallback(mockCallback);
        compat.onWindowLayoutChangeListenerAdded(mock(Activity.class));

        fakeExtensionImp.triggerValidSignal();

        verify(mockCallback).onWindowLayoutChanged(any(),
                argThat(windowLayoutInfo -> !windowLayoutInfo.getDisplayFeatures().isEmpty()));
    }

    @Test
    @Override
    public void testSetExtensionCallback() {
        ArgumentCaptor<ExtensionInterface.ExtensionCallback> extensionCallbackCaptor =
                ArgumentCaptor.forClass(ExtensionInterface.ExtensionCallback.class);

        // Verify that the extension got the callback set
        ExtensionCallbackInterface callback =
                mock(ExtensionCallbackInterface.class);
        mExtensionCompat.setExtensionCallback(callback);

        verify(mExtensionCompat.mWindowExtension).setExtensionCallback(
                extensionCallbackCaptor.capture());

        // Verify that the callback set for extension propagates the device state callback
        ExtensionDeviceState extensionDeviceState = new ExtensionDeviceState(
                ExtensionDeviceState.POSTURE_HALF_OPENED);

        extensionCallbackCaptor.getValue().onDeviceStateChanged(extensionDeviceState);
        ArgumentCaptor<DeviceState> deviceStateCaptor = ArgumentCaptor.forClass(DeviceState.class);
        verify(callback).onDeviceStateChanged(deviceStateCaptor.capture());
        assertEquals(DeviceState.POSTURE_HALF_OPENED, deviceStateCaptor.getValue().getPosture());

        // Verify that the callback set for extension propagates the window layout callback when
        // a listener has been registered.
        mExtensionCompat.onWindowLayoutChangeListenerAdded(mActivity);
        Rect bounds = new Rect(WINDOW_BOUNDS.left, WINDOW_BOUNDS.top, WINDOW_BOUNDS.width(), 1);
        ExtensionDisplayFeature extensionDisplayFeature =
                new ExtensionDisplayFeature(bounds, ExtensionDisplayFeature.TYPE_HINGE);
        List<ExtensionDisplayFeature> displayFeatures = new ArrayList<>();
        displayFeatures.add(extensionDisplayFeature);
        ExtensionWindowLayoutInfo extensionWindowLayoutInfo =
                new ExtensionWindowLayoutInfo(displayFeatures);

        extensionCallbackCaptor.getValue().onWindowLayoutChanged(mActivity,
                extensionWindowLayoutInfo);
        ArgumentCaptor<WindowLayoutInfo> windowLayoutInfoCaptor =
                ArgumentCaptor.forClass(WindowLayoutInfo.class);
        verify(callback).onWindowLayoutChanged(eq(mActivity), windowLayoutInfoCaptor.capture());

        WindowLayoutInfo capturedLayout = windowLayoutInfoCaptor.getValue();
        assertEquals(1, capturedLayout.getDisplayFeatures().size());
        DisplayFeature capturedDisplayFeature = capturedLayout.getDisplayFeatures().get(0);
        assertEquals(DisplayFeature.TYPE_HINGE, capturedDisplayFeature.getType());
        assertEquals(bounds, capturedDisplayFeature.getBounds());
    }

    @Override
    public void testExtensionCallback_filterRemovesInvalidValues() {
        FakeExtensionImp fakeExtensionImp = new FakeExtensionImp();
        ExtensionCompat compat = new ExtensionCompat(fakeExtensionImp, new ExtensionAdapter());
        ExtensionCallbackInterface mockCallback = mock(ExtensionCallbackInterface.class);
        compat.setExtensionCallback(mockCallback);
        compat.onWindowLayoutChangeListenerAdded(mock(Activity.class));

        fakeExtensionImp.triggerMalformedSignal();

        verify(mockCallback).onWindowLayoutChanged(any(),
                argThat(windowLayoutInfo -> windowLayoutInfo.getDisplayFeatures().isEmpty()));
    }

    @Test
    @Override
    public void testOnWindowLayoutChangeListenerAdded() {
        mExtensionCompat.onWindowLayoutChangeListenerAdded(mActivity);
        verify(mExtensionCompat.mWindowExtension).onWindowLayoutChangeListenerAdded(eq(mActivity));
    }

    @Test
    @Override
    public void testOnWindowLayoutChangeListenerRemoved() {
        mExtensionCompat.onWindowLayoutChangeListenerRemoved(mActivity);
        verify(mExtensionCompat.mWindowExtension)
                .onWindowLayoutChangeListenerRemoved(eq(mActivity));
    }

    @Test
    @Override
    public void testOnDeviceStateListenersChanged() {
        mExtensionCompat.onDeviceStateListenersChanged(true);
        verify(mExtensionCompat.mWindowExtension).onDeviceStateListenersChanged(eq(true));
    }

    @Test
    public void testValidateExtensionInterface() {
        assertTrue(mExtensionCompat.validateExtensionInterface());
    }

    private static final class FakeExtensionImp implements ExtensionInterface {

        private ExtensionCallback mCallback;
        private final List<Activity> mActivities = new ArrayList<>();

        FakeExtensionImp() {
            mCallback = new ExtensionCallback() {
                @Override
                public void onDeviceStateChanged(@NonNull ExtensionDeviceState newDeviceState) {

                }

                @Override
                public void onWindowLayoutChanged(@NonNull Activity activity,
                        @NonNull ExtensionWindowLayoutInfo newLayout) {

                }
            };
        }

        @Override
        public void setExtensionCallback(@NonNull ExtensionCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onWindowLayoutChangeListenerAdded(@NonNull Activity activity) {
            mActivities.add(activity);
        }

        @Override
        public void onWindowLayoutChangeListenerRemoved(@NonNull Activity activity) {
            mActivities.remove(activity);
        }

        @Override
        public void onDeviceStateListenersChanged(boolean isEmpty) {

        }

        void triggerMalformedSignal() {
            triggerSignal(malformedWindowLayoutInfo());
        }

        void triggerValidSignal() {
            triggerSignal(validWindowLayoutInfo());
        }

        void triggerSignal(ExtensionWindowLayoutInfo info) {
            for (Activity activity: mActivities) {
                mCallback.onWindowLayoutChanged(activity, info);
            }
        }

        public void triggerDeviceState(ExtensionDeviceState state) {
            mCallback.onDeviceStateChanged(state);
        }

        private ExtensionWindowLayoutInfo malformedWindowLayoutInfo() {
            List<ExtensionDisplayFeature> malformedFeatures = new ArrayList<>();

            for (Rect malformedBound : invalidFoldBounds(WINDOW_BOUNDS)) {
                malformedFeatures.add(new ExtensionDisplayFeature(malformedBound,
                        ExtensionDisplayFeature.TYPE_FOLD));
            }

            for (Rect malformedBound : invalidHingeBounds(WINDOW_BOUNDS)) {
                malformedFeatures.add(new ExtensionDisplayFeature(malformedBound,
                        ExtensionDisplayFeature.TYPE_HINGE));
            }

            return new ExtensionWindowLayoutInfo(malformedFeatures);
        }

        private ExtensionWindowLayoutInfo validWindowLayoutInfo() {
            List<ExtensionDisplayFeature> validFeatures = new ArrayList<>();

            validFeatures.add(new ExtensionDisplayFeature(validFoldBound(WINDOW_BOUNDS),
                    ExtensionDisplayFeature.TYPE_FOLD));

            return new ExtensionWindowLayoutInfo(validFeatures);
        }
    }
}
