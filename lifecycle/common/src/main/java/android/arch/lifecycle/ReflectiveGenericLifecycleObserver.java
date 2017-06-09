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

package android.arch.lifecycle;

import android.arch.lifecycle.Lifecycle.Event;
import android.support.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An internal implementation of {@link GenericLifecycleObserver} that relies on reflection.
 */
class ReflectiveGenericLifecycleObserver implements GenericLifecycleObserver {
    private final Object mWrapped;
    private final CallbackInfo mInfo;
    @SuppressWarnings("WeakerAccess")
    static final Map<Class, CallbackInfo> sInfoCache = new HashMap<>();

    ReflectiveGenericLifecycleObserver(Object wrapped) {
        mWrapped = wrapped;
        mInfo = getInfo(mWrapped.getClass());
    }

    @Override
    public void onStateChanged(LifecycleOwner source, Event event) {
        invokeCallbacks(mInfo, source, event);
    }

    private void invokeMethodsForEvent(List<MethodReference> handlers, LifecycleOwner source,
            Event event) {
        if (handlers != null) {
            for (int i = handlers.size() - 1; i >= 0; i--) {
                MethodReference reference = handlers.get(i);
                invokeCallback(reference, source, event);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void invokeCallbacks(CallbackInfo info, LifecycleOwner source, Event event) {
        invokeMethodsForEvent(info.mEventHandlers.get(event), source, event);
        invokeMethodsForEvent(info.mEventHandlers.get(Event.ON_ANY), source, event);

        // TODO prevent duplicate calls into the same method. Preferably while parsing
        if (info.mSuper != null) {
            invokeCallbacks(info.mSuper, source, event);
        }
        if (info.mInterfaces != null) {
            final int size = info.mInterfaces.size();
            for (int i = 0; i < size; i++) {
                invokeCallbacks(info.mInterfaces.get(i), source, event);
            }
        }
    }

    private void invokeCallback(MethodReference reference, LifecycleOwner source, Event event) {
        //noinspection TryWithIdenticalCatches
        try {
            switch (reference.mCallType) {
                case CALL_TYPE_NO_ARG:
                    reference.mMethod.invoke(mWrapped);
                    break;
                case CALL_TYPE_PROVIDER:
                    reference.mMethod.invoke(mWrapped, source);
                    break;
                case CALL_TYPE_PROVIDER_WITH_EVENT:
                    reference.mMethod.invoke(mWrapped, source, event);
                    break;
            }
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to call observer method", e.getCause());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getReceiver() {
        return mWrapped;
    }

    private static CallbackInfo getInfo(Class klass) {
        CallbackInfo existing = sInfoCache.get(klass);
        if (existing != null) {
            return existing;
        }
        existing = createInfo(klass);
        return existing;
    }

    private static CallbackInfo createInfo(Class klass) {
        Method[] methods = klass.getDeclaredMethods();
        Map<Event, List<MethodReference>> eventHandlers = new HashMap<>();
        for (Method method : methods) {
            OnLifecycleEvent annotation = method.getAnnotation(OnLifecycleEvent.class);
            if (annotation == null) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            int callType = CALL_TYPE_NO_ARG;
            if (params.length > 0) {
                callType = CALL_TYPE_PROVIDER;
                if (!params[0].isAssignableFrom(LifecycleOwner.class)) {
                    throw new IllegalArgumentException(
                            "invalid parameter type. Must be one and instanceof LifecycleOwner");
                }
            }
            if (params.length > 1) {
                callType = CALL_TYPE_PROVIDER_WITH_EVENT;
                if (!params[1].isAssignableFrom(Event.class)) {
                    throw new IllegalArgumentException(
                            "invalid parameter type. second arg must be an event");
                }
            }
            if (params.length > 2) {
                throw new IllegalArgumentException("cannot have more than 2 params");
            }
            Event event = annotation.value();
            List<MethodReference> methodReferences = eventHandlers.get(event);
            if (methodReferences == null) {
                methodReferences = new ArrayList<>();
                eventHandlers.put(event, methodReferences);
            }
            methodReferences.add(new MethodReference(event, callType, method));
        }
        CallbackInfo info = new CallbackInfo(eventHandlers);
        sInfoCache.put(klass, info);
        Class superclass = klass.getSuperclass();
        if (superclass != null) {
            info.mSuper = getInfo(superclass);
        }
        Class[] interfaces = klass.getInterfaces();
        for (Class intrfc : interfaces) {
            CallbackInfo interfaceInfo = getInfo(intrfc);
            if (!interfaceInfo.mEventHandlers.isEmpty()) {
                if (info.mInterfaces == null) {
                    info.mInterfaces = new ArrayList<>();
                }
                info.mInterfaces.add(interfaceInfo);
            }
        }
        return info;
    }

    @SuppressWarnings("WeakerAccess")
    static class CallbackInfo {
        final Map<Event, List<MethodReference>> mEventHandlers;
        @Nullable
        List<CallbackInfo> mInterfaces;
        @Nullable
        CallbackInfo mSuper;

        CallbackInfo(Map<Event, List<MethodReference>> eventHandlers) {
            mEventHandlers = eventHandlers;
        }
    }

    @SuppressWarnings("WeakerAccess")
    static class MethodReference {
        final Event mEvent;
        final int mCallType;
        final Method mMethod;

        MethodReference(Event event, int callType, Method method) {
            mEvent = event;
            mCallType = callType;
            mMethod = method;
            mMethod.setAccessible(true);
        }
    }

    private static final int CALL_TYPE_NO_ARG = 0;
    private static final int CALL_TYPE_PROVIDER = 1;
    private static final int CALL_TYPE_PROVIDER_WITH_EVENT = 2;
}
