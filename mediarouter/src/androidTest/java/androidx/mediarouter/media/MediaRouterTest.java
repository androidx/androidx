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

package androidx.mediarouter.media;

import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.session.MediaSessionCompat;

import android.support.v4.media.session.MediaControllerCompat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test {@link MediaRouter}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaRouterTest {
   // The maximum time to wait for an operation.
   private static final long TIME_OUT_MS = 3000L;
   private static final String SESSION_TAG = "test-session";
   private final Object mWaitLock = new Object();

   private Context mContext;
   private MediaRouter mRouter;
   private MediaSessionCompat mSession;
   private MediaSessionCallback mSessionCallback = new MediaSessionCallback();

   @Before
   public void setUp() throws Exception {
       getInstrumentation().runOnMainSync(new Runnable() {
           @Override
           public void run() {
               mContext = getContext();
               mRouter = MediaRouter.getInstance(mContext);
               mSession = new MediaSessionCompat(mContext, SESSION_TAG);
           }
       });
   }

   @After
   public void tearDown() throws Exception {
       mSession.release();
   }

   /**
    * This test checks whether the session callback work properly after setMediaSessionCompat() is
    * called.
    */
   @Test
   @SmallTest
   public void testSessionCallbackAfterSetMediaSessionCompat() throws Exception {
       getInstrumentation().runOnMainSync(new Runnable() {
           @Override
           public void run() {
               mSession.setCallback(mSessionCallback);
               mRouter.setMediaSessionCompat(mSession);
           }
       });

       MediaControllerCompat controller = mSession.getController();
       MediaControllerCompat.TransportControls controls = controller.getTransportControls();
       synchronized (mWaitLock) {
           mSessionCallback.reset();
           controls.play();
           mWaitLock.wait(TIME_OUT_MS);
           assertTrue(mSessionCallback.mOnPlayCalled);

           mSessionCallback.reset();
           controls.pause();
           mWaitLock.wait(TIME_OUT_MS);
           assertTrue(mSessionCallback.mOnPauseCalled);
       }
   }

   private class MediaSessionCallback extends MediaSessionCompat.Callback {
       private boolean mOnPlayCalled;
       private boolean mOnPauseCalled;

       public void reset() {
           mOnPlayCalled = false;
           mOnPauseCalled = false;
       }

       @Override
       public void onPlay() {
           synchronized (mWaitLock) {
               mOnPlayCalled = true;
               mWaitLock.notify();
           }
       }

       @Override
       public void onPause() {
           synchronized (mWaitLock) {
               mOnPauseCalled = true;
               mWaitLock.notify();
           }
       }
   }
}