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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.os.Build;

import androidx.annotation.GuardedBy;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CameraExecutorTest {

    private CameraExecutor mCameraExecutor;
    private FakeCameraFactory mCameraFactory;

    @Before
    public void setUp() {
        mCameraExecutor = new CameraExecutor();
        mCameraFactory = new FakeCameraFactory();
        mCameraFactory.insertCamera(CameraSelector.LENS_FACING_BACK, "0",
                () -> new FakeCamera(null,
                        new FakeCameraInfoInternal(0, CameraSelector.LENS_FACING_BACK)));
        mCameraFactory.insertCamera(CameraSelector.LENS_FACING_FRONT, "1",
                () -> new FakeCamera(null,
                        new FakeCameraInfoInternal(0, CameraSelector.LENS_FACING_FRONT)));
    }

    @After
    public void tearDown() {
        mCameraExecutor.deinit();
    }

    @Test
    public void canExecuteTaskBeforeInit() {
        Runnable run = mock(Runnable.class);
        mCameraExecutor.execute(run);
        verify(run, timeout(3000)).run();
    }

    @Test
    public void canExecuteMultiTaskAfterInit() {
        Runnable run0 = mock(Runnable.class);
        Runnable run1 = mock(Runnable.class);
        BlockedRunnable blockRun0 = new BlockedRunnable(run0);
        BlockedRunnable blockRun1 = new BlockedRunnable(run1);

        mCameraExecutor.init(mCameraFactory);
        mCameraExecutor.execute(blockRun0);
        mCameraExecutor.execute(blockRun1);

        verify(run0, timeout(3000)).run();
        verify(run1, timeout(3000)).run();

        // Release blocked threads.
        blockRun0.unblock();
        blockRun1.unblock();
    }

    @Test
    public void noRejectedExecutionException_afterDeinit() {
        mCameraExecutor.deinit();
        mCameraExecutor.execute(mock(Runnable.class));
    }

    @Test
    public void canExecuteTaskAfterReInit() {
        mCameraExecutor.deinit();
        mCameraExecutor.init(mCameraFactory);

        Runnable run = mock(Runnable.class);
        mCameraExecutor.execute(run);
        verify(run, timeout(3000)).run();
    }

    private static class BlockedRunnable implements Runnable {
        private final Runnable mDelegate;
        @GuardedBy("this")
        private Thread mThread;
        @GuardedBy("this")
        private boolean mUnblock;

        BlockedRunnable(Runnable runnable) {
            mDelegate = runnable;
        }

        @Override
        public void run() {
            mDelegate.run();

            synchronized (this) {
                if (mUnblock) {
                    return;
                }
                mThread = Thread.currentThread();
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }

        public void unblock() {
            synchronized (this) {
                mUnblock = true;

                if (mThread != null) {
                    mThread.interrupt();
                    mThread = null;
                }
            }
        }
    }
}
