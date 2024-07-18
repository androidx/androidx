/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.sqlite.inspection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.inspection.ArtTooling.EntryHook;
import androidx.inspection.ArtTooling.ExitHook;
import androidx.inspection.InspectorEnvironment;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * The class allows for observing method's EntryHook parameters in ExitHook.
 * <p>
 * It works by registering both (entry and exit) hooks and keeping its own method frame stack.
 * On exit, it calls {@link OnExitCallback} provided by the user.
 * <p>
 * TODO: handle cases when frames could be dropped (e.g. because of an Exception) causing internal
 * state to be corrupted.
 * <p>
 * Thread safe.
 */
final class EntryExitMatchingHookRegistry {
    private final InspectorEnvironment mEnvironment;
    private final ThreadLocal<Deque<Frame>> mFrameStack;

    EntryExitMatchingHookRegistry(InspectorEnvironment environment) {
        mEnvironment = environment;
        mFrameStack = new ThreadLocal<Deque<Frame>>() {
            @NonNull
            @Override
            protected Deque<Frame> initialValue() {
                return new ArrayDeque<>();
            }
        };
    }

    void registerHook(Class<?> originClass, final String originMethod,
            final OnExitCallback onExitCallback) {
        mEnvironment.artTooling().registerEntryHook(originClass, originMethod,
                new EntryHook() {
                    @Override
                    public void onEntry(@Nullable Object thisObject,
                            @NonNull List<Object> args) {
                        getFrameStack().addLast(new Frame(originMethod, thisObject, args, null));
                    }
                });

        mEnvironment.artTooling().registerExitHook(originClass, originMethod,
                new ExitHook<Object>() {
                    @Override
                    public Object onExit(Object result) {
                        Frame entryFrame = getFrameStack().pollLast();
                        if (entryFrame == null || !originMethod.equals(entryFrame.mMethod)) {
                            // TODO: make more specific and handle
                            throw new IllegalStateException();
                        }

                        onExitCallback.onExit(new Frame(entryFrame.mMethod, entryFrame.mThisObject,
                                entryFrame.mArgs, result));
                        return result;
                    }
                });
    }

    private @NonNull Deque<Frame> getFrameStack() {
        /** It won't be null because of overridden {@link ThreadLocal#initialValue} */
        //noinspection ConstantConditions
        return mFrameStack.get();
    }

    static final class Frame {
        final String mMethod;
        final Object mThisObject;
        final List<Object> mArgs;
        final Object mResult;

        private Frame(String method, Object thisObject, List<Object> args, Object result) {
            mMethod = method;
            mThisObject = thisObject;
            mArgs = args;
            mResult = result;
        }
    }

    interface OnExitCallback {
        void onExit(Frame exitFrame);
    }
}
