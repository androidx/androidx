/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.remotecallback;

import static androidx.remotecallback.RemoteCallback.EXTRA_METHOD;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;

import java.lang.reflect.InvocationTargetException;

/**
 * The holder for callbacks that are tagged with {@link RemoteCallable}.
 * Note: This should only be referenced by generated code, there is no reason to reference this
 * otherwise.
 */
public class CallbackHandlerRegistry {
    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static final CallbackHandlerRegistry sInstance = new CallbackHandlerRegistry();
    private static final String TAG = "CallbackHandlerRegistry";

    private final ArrayMap<Class<? extends CallbackReceiver>, ClsHandler> mClsLookup =
            new ArrayMap<>();

    /**
     * @hide
     */
    @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public <T extends CallbackReceiver> T getAndResetStub(Class<? extends CallbackReceiver> cls,
            Context context, String authority) {
        ensureInitialized(cls);
        ClsHandler stub = findMap(cls);
        initStub(stub.mCallStub, cls, context, authority);
        return (T) stub.mCallStub;
    }

    private void initStub(CallbackReceiver stub, Class<? extends CallbackReceiver> cls,
            Context context, String authority) {
        ClsHandler clsHandler = findMap(cls);
        clsHandler.mContext = context;
        if (stub instanceof ContentProvider) {
            clsHandler.mAuthority = determineAuthority(context, authority, cls);
        } else {
            clsHandler.mAuthority = null;
        }
    }

    private String determineAuthority(Context context, String authority, Class<?> aClass) {
        if (authority != null) {
            return authority;
        }
        try {
            ProviderInfo info = context.getPackageManager().getProviderInfo(
                    new ComponentName(context.getPackageName(), aClass.getName()),
                    0);
            return info.authority;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Couldn't find provider " + aClass, e);
            return null;
        }
    }

    <T extends CallbackReceiver> void ensureInitialized(Class<T> cls) {
        synchronized (this) {
            if (!mClsLookup.containsKey(cls)) {
                runInit(cls);
            }
        }
    }

    /**
     * Trigger a call to a callback using arguments that were generated with
     * {@link RemoteCallback#getArgumentBundle()}.
     */
    public <T extends CallbackReceiver> void invokeCallback(Context context, T receiver,
            Intent intent) {
        invokeCallback(context, receiver, intent.getExtras());
    }

    /**
     * Trigger a call to a callback using arguments that were generated with
     * {@link RemoteCallback#getArgumentBundle()}.
     */
    public <T extends CallbackReceiver> void invokeCallback(Context context, T receiver,
            Bundle bundle) {
        Class<? extends CallbackReceiver> receiverClass = receiver.getClass();
        ensureInitialized(receiverClass);
        ClsHandler map = findMap(receiverClass);
        if (map == null) {
            Log.e(TAG, "No map found for " + receiverClass.getName());
            return;
        }
        String method = bundle.getString(EXTRA_METHOD);
        @SuppressWarnings("unchecked")
        CallbackHandler<T> callbackHandler = (CallbackHandler<T>) map.mHandlers.get(method);
        if (callbackHandler == null) {
            Log.e(TAG, "No handler found for " + method + " on " + receiverClass.getName());
            return;
        }
        callbackHandler.executeCallback(context, receiver, bundle);
    }

    private ClsHandler findMap(Class<?> aClass) {
        ClsHandler map;
        synchronized (this) {
            map = mClsLookup.get(aClass);
        }
        if (map != null) {
            return map;
        }
        if (aClass.getSuperclass() != null) {
            return findMap(aClass.getSuperclass());
        }
        return null;
    }

    private <T extends CallbackReceiver> void runInit(Class<T> cls) {
        try {
            // This is the only bit of reflection/keeping that needs to exist, one init class
            // per callback receiver.
            ClsHandler clsHandler = new ClsHandler();
            mClsLookup.put(cls, clsHandler);
            clsHandler.mCallStub =
                    (CallbackReceiver) findInitClass(cls).getDeclaredConstructor().newInstance();
        } catch (InstantiationException e) {
            Log.e(TAG, "Unable to initialize " + cls.getName(), e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Unable to initialize " + cls.getName(), e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Unable to initialize " + cls.getName(), e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Unable to initialize " + cls.getName(), e);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Unable to initialize " + cls.getName(), e);
        }
    }

    private <T extends CallbackReceiver> void registerHandler(Class<T> cls, String method,
            CallbackHandler<T> handler) {
        ClsHandler map = mClsLookup.get(cls);
        if (map == null) {
            throw new IllegalStateException("registerHandler called before init was run");
        }
        map.mHandlers.put(method, handler);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Runnable> findInitClass(Class<? extends CallbackReceiver> cls)
            throws ClassNotFoundException {
        String pkg = cls.getPackage().getName();
        String c = String.format("%s.%sInitializer", pkg, cls.getSimpleName());
        return (Class<? extends Runnable>) Class.forName(c, false, cls.getClassLoader());
    }

    /**
     * Registers a callback handler to be executed when a given PendingIntent is fired
     * for a {@link RemoteCallback}.
     * Note: This should only be called by generated code, there is no reason to reference this
     * otherwise.
     */
    public static <T extends CallbackReceiver> void registerCallbackHandler(Class<T> cls,
            String method, CallbackHandler<T> handler) {
        sInstance.registerHandler(cls, method, handler);
    }

    /**
     * Turns a callback receiver stub into a remote callback.
     * Note: This should only be called by generated code, there is no reason to reference this
     * otherwise.
     */
    @SuppressWarnings("unchecked")
    public static RemoteCallback stubToRemoteCallback(CallbackReceiver receiver,
            Class<? extends CallbackReceiver> cls, Bundle args, String method) {
        if (!(receiver instanceof CallbackBase)) {
            throw new IllegalArgumentException(
                    "May only be called on classes that extend a *WithCallbacks base class.");
        }
        ClsHandler clsHandler = sInstance.findMap(cls);
        Context context = clsHandler.mContext;
        String authority = clsHandler.mAuthority;
        // Clear out context and authority to avoid context leak.
        clsHandler.mContext = null;
        clsHandler.mAuthority = null;
        return ((CallbackBase) receiver).toRemoteCallback(cls, context, authority, args, method);
    }

    static class ClsHandler {
        final ArrayMap<String, CallbackHandler<? extends CallbackReceiver>> mHandlers =
                new ArrayMap<>();
        public String mAuthority;
        Context mContext;
        CallbackReceiver mCallStub;
    }

    /**
     * The interface used to trigger a callback when the pending intent is fired.
     * Note: This should only be referenced by generated code, there is no reason to reference
     * this otherwise.
     *
     * @param <T> The receiver type for this callback handler.
     */
    public interface CallbackHandler<T extends CallbackReceiver> {
        /**
         * Executes a callback given a Bundle of arguments.
         * Note: This should only be called by generated code, there is no reason to reference this
         * otherwise.
         */
        void executeCallback(Context context, T receiver, Bundle arguments);
    }
}
