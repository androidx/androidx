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

import android.os.Bundle;
import android.os.RemoteException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * An adapter class for accessing the hidden framework classes, IMediaBrowserServiceCallbacks and
 * IMediaBrowserServiceCallbacks.Stub using reflection.
 */
class ServiceCallbacksAdapterApi24 extends ServiceCallbacksAdapterApi21 {
    private Method mOnLoadChildrenMethodWithOptionsMethod;

    ServiceCallbacksAdapterApi24(Object callbackObject) {
        super(callbackObject);
        try {
            Class theClass = Class.forName("android.service.media.IMediaBrowserServiceCallbacks");
            Class parceledListSliceClass = Class.forName("android.content.pm.ParceledListSlice");
            mOnLoadChildrenMethodWithOptionsMethod = theClass.getMethod("onLoadChildrenWithOptions",
                    new Class[] { String.class, parceledListSliceClass, Bundle.class });
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    void onLoadChildrenWithOptions(String mediaId, Object parceledListSliceObj, Bundle options)
            throws RemoteException {
        try {
            mOnLoadChildrenMethodWithOptionsMethod.invoke(
                    mCallbackObject, mediaId, parceledListSliceObj, options);
        } catch (IllegalAccessException | InvocationTargetException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
