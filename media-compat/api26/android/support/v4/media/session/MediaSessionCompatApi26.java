/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v4.media.session;

import android.media.session.MediaSession;
import android.support.annotation.RequiresApi;

@RequiresApi(26)
class MediaSessionCompatApi26 {

    public static Object createCallback(Callback callback) {
        return new CallbackProxy<Callback>(callback);
    }

    public static void setRepeatMode(Object sessionObj, int repeatMode) {
        ((MediaSession) sessionObj).setRepeatMode(repeatMode);
    }

    public static void setShuffleModeEnabled(Object sessionObj, boolean enabled) {
        ((MediaSession) sessionObj).setShuffleModeEnabled(enabled);
    }

    public interface Callback extends MediaSessionCompatApi24.Callback {
        void onSetRepeatMode(int repeatMode);
        void onSetShuffleModeEnabled(boolean enabled);
    }

    static class CallbackProxy<T extends Callback>
            extends MediaSessionCompatApi24.CallbackProxy<T> {
        CallbackProxy(T callback) {
            super(callback);
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            mCallback.onSetRepeatMode(repeatMode);
        }

        @Override
        public void onSetShuffleModeEnabled(boolean enabled) {
            mCallback.onSetShuffleModeEnabled(enabled);
        }
    }
}
