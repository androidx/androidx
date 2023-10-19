/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.mocks;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A mock implementations of {@link ImageCapture.ScreenFlashUiControl} for testing purpose.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class MockScreenFlashUiControl implements ImageCapture.ScreenFlashUiControl {
    /**
     * Represents
     * {@link ImageCapture.ScreenFlashUiControl#applyScreenFlashUi(ImageCapture.ScreenFlashUiCompleter)}
     * event.
     */
    public static final int APPLY_SCREEN_FLASH = 0;
    /**
     * Represents {@link ImageCapture.ScreenFlashUiControl#clearScreenFlashUi()} event.
     */
    public static final int CLEAR_SCREEN_FLASH = 1;

    /**
     * The event types in {@link ImageCapture.ScreenFlashUiControl}.
     */
    @IntDef({APPLY_SCREEN_FLASH, CLEAR_SCREEN_FLASH})
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE_USE})
    public @interface ScreenFlashUiEvent {
    }

    private final Object mLock = new Object();
    private final List<@ScreenFlashUiEvent Integer> mEventList = new ArrayList<>();
    private final CountDownLatch mScreenFlashUiClearLatch = new CountDownLatch(1);

    /**
     * Returns a list of {@link ScreenFlashUiEvent} in the same order as invoked.
     */
    @NonNull
    public List<@ScreenFlashUiEvent Integer> getScreenFlashUiEvents() {
        synchronized (mLock) {
            return new ArrayList<>(mEventList);
        }
    }

    /**
     * Waits for {@link #clearScreenFlashUi} to be invoked once.
     *
     * @param timeoutInMillis The timeout of waiting in milliseconds.
     * @return True if {@link #clearScreenFlashUi} was invoked, false if timed out.
     */
    public boolean awaitScreenFlashUiClear(long timeoutInMillis) {
        try {
            return mScreenFlashUiClearLatch.await(timeoutInMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applyScreenFlashUi(
            @NonNull ImageCapture.ScreenFlashUiCompleter screenFlashUiCompleter) {
        synchronized (mLock) {
            mEventList.add(APPLY_SCREEN_FLASH);
            screenFlashUiCompleter.complete();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void clearScreenFlashUi() {
        synchronized (mLock) {
            mEventList.add(CLEAR_SCREEN_FLASH);
            mScreenFlashUiClearLatch.countDown();
        }
    }
}
