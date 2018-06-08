/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.leanback.media;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;

import androidx.leanback.testutils.PollingCheck;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaPlayerGlueTest {

    /**
     * Mockito spy not working on API 19 if class has package private method (b/35387610)
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void mediaPlayer() {
        // create a MediaPlayerGlue with updatePeriod = 100ms
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final MediaPlayerGlue[] result = new MediaPlayerGlue[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                result[0] = new MediaPlayerGlue(context);
            }
        });
        final MediaPlayerGlue glue = Mockito.spy(result[0]);
        Mockito.when(glue.getUpdatePeriod()).thenReturn(100);

        final PlaybackGlueHostImpl host = new PlaybackGlueHostImpl();

        glue.setHost(host);
        glue.setMode(MediaPlayerGlue.REPEAT_ALL);
        final boolean[] ready = new boolean[] {false};
        glue.addPlayerCallback(new PlaybackGlue.PlayerCallback() {
            @Override
            public void onPreparedStateChanged(PlaybackGlue glue) {
                if (glue.isPrepared()) {
                    glue.play();
                    ready[0] = true;
                }
            }
        });
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                glue.setMediaSource(Uri.parse(
                        "android.resource://androidx.leanback.test/raw/track_01"));
            }
        });
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return ready[0];
            }
        });

        // Test setProgressUpdatingEnabled(true) and setProgressUpdatingEnabled(false);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                glue.enableProgressUpdating(true);
            }
        });
        Mockito.reset(glue);
        SystemClock.sleep(1000);
        Mockito.verify(glue, atLeastOnce()).updateProgress();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                glue.enableProgressUpdating(false);
            }
        });
        Mockito.reset(glue);
        SystemClock.sleep(1000);
        Mockito.verify(glue, never()).updateProgress();

        // Test onStart()/onStop() will pause the updateProgress.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                host.notifyOnStart();
            }
        });
        Mockito.reset(glue);
        SystemClock.sleep(1000);
        Mockito.verify(glue, atLeastOnce()).updateProgress();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                host.notifyOnStop();
            }
        });
        Mockito.reset(glue);
        SystemClock.sleep(1000);
        Mockito.verify(glue, never()).updateProgress();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                host.notifyOnDestroy();
            }
        });
        assertNull(glue.getHost());
        Mockito.verify(glue, times(1)).onDetachedFromHost();
        Mockito.verify(glue, times(1)).release();
    }

}
