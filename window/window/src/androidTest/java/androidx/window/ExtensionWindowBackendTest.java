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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;

import androidx.core.util.Consumer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/** Tests for {@link ExtensionWindowBackend} class. */
@SuppressWarnings({"deprecation", "unchecked"}) // TODO(b/173739071) remove DeviceState
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class ExtensionWindowBackendTest extends WindowTestBase {
    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        ExtensionWindowBackend.resetInstance();
    }

    @Test
    public void testGetInstance() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        assertNotNull(backend);

        // Verify that getInstance always returns the same value
        ExtensionWindowBackend newBackend = ExtensionWindowBackend.getInstance(mContext);
        assertEquals(backend, newBackend);
    }

    @Test
    public void testInitAndVerifyExtension() {
        Version extensionVersion = ExtensionCompat.getExtensionVersion();
        assumeTrue(extensionVersion != null);
        assertTrue(ExtensionWindowBackend.isExtensionVersionSupported(extensionVersion));

        ExtensionInterfaceCompat extension =
                ExtensionWindowBackend.initAndVerifyExtension(mContext);
        assertNotNull(extension);
        assertTrue(extension instanceof ExtensionCompat);
        assertTrue(extension.validateExtensionInterface());
    }

    @Test
    public void testInitAndVerifySidecar() {
        Version sidecarVersion = SidecarCompat.getSidecarVersion();
        assumeTrue(sidecarVersion != null);
        assertTrue(ExtensionWindowBackend.isExtensionVersionSupported(sidecarVersion));

        ExtensionInterfaceCompat sidecar =
                ExtensionWindowBackend.initAndVerifyExtension(mContext);
        assertNotNull(sidecar);
        assertTrue(sidecar instanceof SidecarCompat);
        assertTrue(sidecar.validateExtensionInterface());
    }

    @Test
    public void testRegisterLayoutChangeCallback() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        // Check registering the layout change callback
        Consumer<WindowLayoutInfo> consumer = mock(WindowLayoutInfoConsumer.class);
        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        backend.registerLayoutChangeCallback(activity, Runnable::run, consumer);

        assertEquals(1, backend.mWindowLayoutChangeCallbacks.size());
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerAdded(activity);

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(consumer);

        assertTrue(backend.mWindowLayoutChangeCallbacks.isEmpty());
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerRemoved(eq(activity));
    }

    @Test
    public void testRegisterLayoutChangeCallback_callsExtensionOnce() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        // Check registering the layout change callback
        Consumer<WindowLayoutInfo> consumer = mock(WindowLayoutInfoConsumer.class);
        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        backend.registerLayoutChangeCallback(activity, Runnable::run, consumer);
        backend.registerLayoutChangeCallback(activity, Runnable::run,
                mock(WindowLayoutInfoConsumer.class));

        assertEquals(2, backend.mWindowLayoutChangeCallbacks.size());
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerAdded(activity);

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(consumer);

        assertEquals(1, backend.mWindowLayoutChangeCallbacks.size());
        verify(backend.mWindowExtension, times(0))
                .onWindowLayoutChangeListenerRemoved(eq(activity));
    }

    @Test
    public void testRegisterLayoutChangeCallback_clearListeners() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        // Check registering the layout change callback
        Consumer<WindowLayoutInfo> firstConsumer = mock(WindowLayoutInfoConsumer.class);
        Consumer<WindowLayoutInfo> secondConsumer = mock(WindowLayoutInfoConsumer.class);
        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        backend.registerLayoutChangeCallback(activity, Runnable::run, firstConsumer);
        backend.registerLayoutChangeCallback(activity, Runnable::run, secondConsumer);

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(firstConsumer);
        backend.unregisterLayoutChangeCallback(secondConsumer);

        assertTrue(backend.mWindowLayoutChangeCallbacks.isEmpty());
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerRemoved(activity);
    }

    @Test
    public void testLayoutChangeCallback_emitNewValue() {
        ExtensionWindowBackend backend = ExtensionWindowBackend.getInstance(mContext);
        backend.mWindowExtension = mock(ExtensionInterfaceCompat.class);

        // Check that callbacks from the extension are propagated correctly
        Consumer<WindowLayoutInfo> consumer = mock(WindowLayoutInfoConsumer.class);
        TestActivity activity = mActivityTestRule.launchActivity(new Intent());

        backend.registerLayoutChangeCallback(activity, Runnable::run, consumer);
        WindowLayoutInfo windowLayoutInfo = newTestWindowLayoutInfo();

        ExtensionWindowBackend.ExtensionListenerImpl backendListener =
                backend.new ExtensionListenerImpl();
        backendListener.onWindowLayoutChanged(activity, windowLayoutInfo);

        verify(consumer).accept(eq(windowLayoutInfo));
    }

    @Test
    public void testWindowLayoutInfo_updatesOnSubsequentRegistration() {
        SwitchOnUnregisterExtensionInterfaceCompat interfaceCompat =
                new SwitchOnUnregisterExtensionInterfaceCompat();
        ExtensionWindowBackend backend = new ExtensionWindowBackend(interfaceCompat);
        Activity activity = mock(Activity.class);
        SimpleConsumer<WindowLayoutInfo> consumer = new SimpleConsumer<>();
        Executor executor = MoreExecutors.directExecutor();
        List<WindowLayoutInfo> expected = new ArrayList<>();

        backend.registerLayoutChangeCallback(activity, executor, consumer);
        expected.add(interfaceCompat.currentWindowLayoutInfo());
        backend.unregisterLayoutChangeCallback(consumer);
        backend.registerLayoutChangeCallback(activity, executor, consumer);
        expected.add(interfaceCompat.currentWindowLayoutInfo());
        backend.unregisterLayoutChangeCallback(consumer);

        assertEquals(expected, consumer.mValues);
    }

    private static WindowLayoutInfo newTestWindowLayoutInfo() {
        WindowLayoutInfo.Builder builder = new WindowLayoutInfo.Builder();
        WindowLayoutInfo windowLayoutInfo = builder.build();

        assertTrue(windowLayoutInfo.getDisplayFeatures().isEmpty());

        DisplayFeature feature1 = new FoldingFeature(new Rect(0, 2, 3, 4),
                FoldingFeature.TYPE_HINGE, FoldingFeature.STATE_FLAT);
        DisplayFeature feature2 = new FoldingFeature(new Rect(0, 1, 5, 1),
                FoldingFeature.TYPE_HINGE, FoldingFeature.STATE_FLAT);

        List<DisplayFeature> displayFeatures = new ArrayList<>();
        displayFeatures.add(feature1);
        displayFeatures.add(feature2);

        builder = new WindowLayoutInfo.Builder();
        builder.setDisplayFeatures(displayFeatures);
        return builder.build();
    }

    private static DeviceState newTestDeviceState() {
        DeviceState.Builder builder = new DeviceState.Builder();
        builder.setPosture(DeviceState.POSTURE_OPENED);
        return builder.build();
    }

    private interface DeviceStateConsumer extends Consumer<DeviceState> { }

    private interface WindowLayoutInfoConsumer extends Consumer<WindowLayoutInfo> { }

    private static class SimpleConsumer<T> implements Consumer<T> {
        private final List<T> mValues;

        SimpleConsumer() {
            mValues = new ArrayList<>();
        }

        @Override
        public void accept(T t) {
            mValues.add(t);
        }

        List<T> allValues() {
            return mValues;
        }

        T lastValue() {
            return mValues.get(mValues.size() - 1);
        }
    }
}
