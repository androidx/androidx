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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import android.os.Build;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.Executor;


@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class DeferrableSurfaceTest {
    DeferrableSurface mDeferrableSurface;

    @Before
    public void setup() {
        mDeferrableSurface = new DeferrableSurface() {
            @Nullable
            @Override
            public ListenableFuture<Surface> getSurface() {
                return null;
            }
        };
    }

    @Test
    public void attachCountIsCorrect() {

        mDeferrableSurface.notifySurfaceAttached();
        mDeferrableSurface.notifySurfaceDetached();
        mDeferrableSurface.notifySurfaceAttached();
        mDeferrableSurface.notifySurfaceDetached();
        mDeferrableSurface.notifySurfaceAttached();
        mDeferrableSurface.notifySurfaceDetached();
        mDeferrableSurface.notifySurfaceAttached();
        mDeferrableSurface.notifySurfaceDetached();

        assertThat(mDeferrableSurface.getAttachedCount()).isEqualTo(0);

        mDeferrableSurface.notifySurfaceAttached();
        mDeferrableSurface.notifySurfaceAttached();
        mDeferrableSurface.notifySurfaceAttached();
        mDeferrableSurface.notifySurfaceDetached();

        assertThat(mDeferrableSurface.getAttachedCount()).isEqualTo(2);
    }

    @Test
    public void onSurfaceDetachListenerIsCalledWhenDetachedLater() {
        DeferrableSurface.OnSurfaceDetachedListener listener =
                Mockito.mock(DeferrableSurface.OnSurfaceDetachedListener.class);

        Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };

        mDeferrableSurface.notifySurfaceAttached();
        mDeferrableSurface.setOnSurfaceDetachedListener(executor, listener);
        mDeferrableSurface.notifySurfaceAttached();
        mDeferrableSurface.notifySurfaceDetached();
        mDeferrableSurface.notifySurfaceDetached();

        Mockito.verify(listener, times(1)).onSurfaceDetached();
    }

    @Test
    public void onSurfaceDetachListenerIsCalledWhenDetachedAlready() {
        DeferrableSurface.OnSurfaceDetachedListener listener =
                Mockito.mock(DeferrableSurface.OnSurfaceDetachedListener.class);

        Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };

        mDeferrableSurface.notifySurfaceAttached();
        mDeferrableSurface.notifySurfaceDetached();

        mDeferrableSurface.setOnSurfaceDetachedListener(executor, listener);

        Mockito.verify(listener, times(1)).onSurfaceDetached();
    }

    @Test
    public void onSurfaceDetachListenerRunInCorrectExecutor() {
        Executor executor = Mockito.mock(Executor.class);
        DeferrableSurface.OnSurfaceDetachedListener listener =
                Mockito.mock(DeferrableSurface.OnSurfaceDetachedListener.class);

        mDeferrableSurface.notifySurfaceAttached();
        mDeferrableSurface.setOnSurfaceDetachedListener(executor, listener);
        mDeferrableSurface.notifySurfaceDetached();

        Mockito.verify(executor, times(1)).execute(any(Runnable.class));

    }

    @Test(expected = IllegalStateException.class)
    public void detachInWrongState_throwException() {
        mDeferrableSurface.notifySurfaceAttached();
        mDeferrableSurface.notifySurfaceDetached();

        mDeferrableSurface.notifySurfaceDetached();
    }
}
