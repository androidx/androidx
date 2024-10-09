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

package androidx.fragment.app;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.MainThread;

import org.jspecify.annotations.NonNull;

/**
 * Listener for receiving a callback immediately following {@link Fragment#onAttach(Context)}.
 * This can be used to perform any additional setup / provide any dependencies that the Fragment
 * may need prior to child fragments being attached or the Fragment going through
 * {@link Fragment#onCreate(Bundle)}.
 *
 * @see FragmentManager#addFragmentOnAttachListener(FragmentOnAttachListener)
 */
public interface FragmentOnAttachListener {
    /**
     * Called after the fragment has been attached to its host. This is called
     * immediately after {@link Fragment#onAttach(Context)} and before
     * {@link Fragment#onAttach(Context)} has been called on any child fragments.
     *
     * @param fragmentManager FragmentManager the fragment is now attached to. This will
     *                        be the same FragmentManager that is returned by
     *                        {@link Fragment#getParentFragmentManager()}.
     * @param fragment Fragment that just received a callback to {@link Fragment#onAttach(Context)}
     */
    @MainThread
    void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment);
}
