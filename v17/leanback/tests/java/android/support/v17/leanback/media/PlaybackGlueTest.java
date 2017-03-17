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

package android.support.v17.leanback.media;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.times;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PlaybackGlueTest {


    public static class PlaybackGlueImpl extends PlaybackGlue {

        public PlaybackGlueImpl(Context context) {
            super(context);
        }
    }

    @Test
    public void glueAndHostInteraction() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlaybackGlue glue = Mockito.spy(new PlaybackGlueImpl(context));
        PlaybackGlueHostImpl host = new PlaybackGlueHostImpl();

        glue.setHost(host);
        Mockito.verify(glue, times(1)).onAttachedToHost(host);
        assertSame(glue, host.mGlue);
        assertSame(host, glue.getHost());

        host.notifyOnStart();
        Mockito.verify(glue, times(1)).onHostStart();

        host.notifyOnResume();
        Mockito.verify(glue, times(1)).onHostResume();

        host.notifyOnPause();
        Mockito.verify(glue, times(1)).onHostPause();

        host.notifyOnStop();
        Mockito.verify(glue, times(1)).onHostStop();

        PlaybackGlue glue2 = Mockito.spy(new PlaybackGlueImpl(context));
        glue2.setHost(host);
        Mockito.verify(glue, times(1)).onDetachedFromHost();
        Mockito.verify(glue2, times(1)).onAttachedToHost(host);
        assertSame(glue2, host.mGlue);
        assertSame(host, glue2.getHost());
        assertNull(glue.getHost());

        host.notifyOnDestroy();
        assertNull(glue2.getHost());
        assertNull(host.mGlue);
        Mockito.verify(glue2, times(1)).onDetachedFromHost();
    }

}
