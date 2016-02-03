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

package android.support.v4.media;

import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * An adapter class for accessing the hidden framework classes, IMediaBrowserServiceCallbacks and
 * IMediaBrowserServiceCallbacks.Stub using reflection.
 */
class IMediaBrowserServiceCallbacksAdapterApi21 {

    Object mCallbackObject;
    private Method mAsBinderMethod;
    private Method mOnConnectMethod;
    private Method mOnConnectFailedMethod;
    private Method mOnLoadChildrenMethod;

    IMediaBrowserServiceCallbacksAdapterApi21(Object callbackObject) {
        mCallbackObject = callbackObject;
        try {
            Class theClass = Class.forName("android.service.media.IMediaBrowserServiceCallbacks");
            Class parceledListSliceClass = Class.forName("android.content.pm.ParceledListSlice");
            mAsBinderMethod = theClass.getMethod("asBinder");
            mOnConnectMethod = theClass.getMethod("onConnect",
                    new Class[] { String.class, MediaSession.Token.class, Bundle.class });
            mOnConnectFailedMethod = theClass.getMethod("onConnectFailed");
            mOnLoadChildrenMethod = theClass.getMethod("onLoadChildren",
                    new Class[] { String.class, parceledListSliceClass });
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    IBinder asBinder() {
        IBinder result = null;
        try {
            result = (IBinder) mAsBinderMethod.invoke(mCallbackObject);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return result;
    }

    void onConnect(String root, Object session, Bundle extras) throws RemoteException {
        try {
            mOnConnectMethod.invoke(mCallbackObject, root, session, extras);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    void onConnectFailed() throws RemoteException {
        try {
            mOnConnectFailedMethod.invoke(mCallbackObject);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    void onLoadChildren(String mediaId, Object parceledListSliceObj) throws RemoteException {
        try {
            mOnLoadChildrenMethod.invoke(mCallbackObject, mediaId, parceledListSliceObj);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    static class Stub {
        static Method sAsInterfaceMethod;
        static {
            try {
                Class theClass = Class.forName(
                        "android.service.media.IMediaBrowserServiceCallbacks$Stub");
                sAsInterfaceMethod = theClass.getMethod(
                        "asInterface", new Class[] { IBinder.class });
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        static Object asInterface(IBinder binder) {
            Object result = null;
            try {
                result = sAsInterfaceMethod.invoke(null, binder);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            return result;
        }
    }
}
