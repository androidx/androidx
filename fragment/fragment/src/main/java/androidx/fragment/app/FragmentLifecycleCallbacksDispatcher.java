/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.fragment.app;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dispatcher for events to {@link FragmentManager.FragmentLifecycleCallbacks} instances
 */
class FragmentLifecycleCallbacksDispatcher {

    private static final class FragmentLifecycleCallbacksHolder {
        @NonNull
        final FragmentManager.FragmentLifecycleCallbacks mCallback;
        final boolean mRecursive;

        FragmentLifecycleCallbacksHolder(
                @NonNull FragmentManager.FragmentLifecycleCallbacks callback,
                boolean recursive) {
            mCallback = callback;
            mRecursive = recursive;
        }
    }

    @NonNull
    private final CopyOnWriteArrayList<FragmentLifecycleCallbacksHolder>
            mLifecycleCallbacks = new CopyOnWriteArrayList<>();

    @NonNull
    private final FragmentManager mFragmentManager;

    FragmentLifecycleCallbacksDispatcher(@NonNull FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    /**
     * Registers a {@link FragmentManager.FragmentLifecycleCallbacks} to listen to fragment
     * lifecycle events happening in this FragmentManager. All registered callbacks will be
     * automatically unregistered when this FragmentManager is destroyed.
     *
     * @param cb Callbacks to register
     * @param recursive true to automatically register this callback for all child FragmentManagers
     */
    public void registerFragmentLifecycleCallbacks(
            @NonNull FragmentManager.FragmentLifecycleCallbacks cb,
            boolean recursive) {
        mLifecycleCallbacks.add(new FragmentLifecycleCallbacksHolder(cb, recursive));
    }

    /**
     * Unregisters a previously registered {@link FragmentManager.FragmentLifecycleCallbacks}.
     * If the callback was not previously registered this call has no effect. All registered
     * callbacks will be automatically unregistered when this FragmentManager is destroyed.
     *
     * @param cb Callbacks to unregister
     */
    public void unregisterFragmentLifecycleCallbacks(
            @NonNull FragmentManager.FragmentLifecycleCallbacks cb) {
        synchronized (mLifecycleCallbacks) {
            for (int i = 0, count = mLifecycleCallbacks.size(); i < count; i++) {
                if (mLifecycleCallbacks.get(i).mCallback == cb) {
                    mLifecycleCallbacks.remove(i);
                    break;
                }
            }
        }
    }

    void dispatchOnFragmentPreAttached(@NonNull Fragment f, boolean onlyRecursive) {
        Context context = mFragmentManager.getHost().getContext();
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentPreAttached(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentPreAttached(mFragmentManager, f, context);
            }
        }
    }

    void dispatchOnFragmentAttached(@NonNull Fragment f, boolean onlyRecursive) {
        Context context = mFragmentManager.getHost().getContext();
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentAttached(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentAttached(mFragmentManager, f, context);
            }
        }
    }

    void dispatchOnFragmentPreCreated(@NonNull Fragment f,
            @Nullable Bundle savedInstanceState, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentPreCreated(f, savedInstanceState, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentPreCreated(
                        mFragmentManager, f, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentCreated(@NonNull Fragment f,
            @Nullable Bundle savedInstanceState, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentCreated(f, savedInstanceState, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentCreated(
                        mFragmentManager, f, savedInstanceState);
            }
        }
    }

    @SuppressWarnings("deprecation")
    void dispatchOnFragmentActivityCreated(@NonNull Fragment f,
            @Nullable Bundle savedInstanceState, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentActivityCreated(f, savedInstanceState, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentActivityCreated(
                        mFragmentManager, f, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentViewCreated(@NonNull Fragment f, @NonNull View v,
            @Nullable Bundle savedInstanceState, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentViewCreated(f, v, savedInstanceState, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentViewCreated(
                        mFragmentManager, f, v, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentStarted(@NonNull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentStarted(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentStarted(mFragmentManager, f);
            }
        }
    }

    void dispatchOnFragmentResumed(@NonNull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentResumed(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentResumed(mFragmentManager, f);
            }
        }
    }

    void dispatchOnFragmentPaused(@NonNull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentPaused(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentPaused(mFragmentManager, f);
            }
        }
    }

    void dispatchOnFragmentStopped(@NonNull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentStopped(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentStopped(mFragmentManager, f);
            }
        }
    }

    void dispatchOnFragmentSaveInstanceState(@NonNull Fragment f, @NonNull Bundle outState,
            boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentSaveInstanceState(f, outState, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentSaveInstanceState(
                        mFragmentManager, f, outState);
            }
        }
    }

    void dispatchOnFragmentViewDestroyed(@NonNull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentViewDestroyed(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentViewDestroyed(mFragmentManager, f);
            }
        }
    }

    void dispatchOnFragmentDestroyed(@NonNull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentDestroyed(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentDestroyed(mFragmentManager, f);
            }
        }
    }

    void dispatchOnFragmentDetached(@NonNull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentDetached(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentDetached(mFragmentManager, f);
            }
        }
    }
}
