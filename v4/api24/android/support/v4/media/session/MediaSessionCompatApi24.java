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
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class MediaSessionCompatApi24 {
    private static final String TAG = "MediaSessionCompatApi24";

    public static Object createCallback(Callback callback) {
        return new CallbackProxy<Callback>(callback);
    }

    public static String getCallingPackage(Object sessionObj) {
        MediaSession session = (MediaSession) sessionObj;
        try {
            Method getCallingPackageMethod = session.getClass().getMethod("getCallingPackage");
            return (String) getCallingPackageMethod.invoke(session);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Log.e(TAG, "Cannot execute MediaSession.getCallingPackage()", e);
        }
        return null;
    }

    public interface Callback extends MediaSessionCompatApi23.Callback {
        public void onPrepare();
        public void onPrepareFromMediaId(String mediaId, Bundle extras);
        public void onPrepareFromSearch(String query, Bundle extras);
        public void onPrepareFromUri(Uri uri, Bundle extras);
    }

    static class CallbackProxy<T extends Callback>
            extends MediaSessionCompatApi23.CallbackProxy<T> {
        public CallbackProxy(T callback) {
            super(callback);
        }

        @Override
        public void onPrepare() {
            mCallback.onPrepare();
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            mCallback.onPrepareFromMediaId(mediaId, extras);
        }

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {
            mCallback.onPrepareFromSearch(query, extras);
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            mCallback.onPrepareFromUri(uri, extras);
        }
    }
}
