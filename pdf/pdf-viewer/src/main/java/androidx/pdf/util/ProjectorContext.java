/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * A directory of global objects that depend on the Application Context.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("UnusedVariable")
public class ProjectorContext {

    private static ProjectorContext sInstance;

    private final ProjectorGlobals mGlobals;

    /**
     *
     */
    public static void installProjectorGlobalsForTest(@NonNull Context appContext) {
        Preconditions.checkNotNull(appContext);
        sInstance = new ProjectorContext(appContext, new Screen(appContext));
    }

    /**
     *
     */
    @NonNull
    public static ProjectorContext get() {
        Preconditions.checkState(sInstance != null,
                "Must call installProjectorGlobals prior to get");
        return sInstance;
    }

    private ProjectorContext(Context appContext, Screen screen) {
        Log.d("ProjectorContext", String.format("appContext: %s", appContext));
        mGlobals = new ProjectorGlobals(screen);
    }

    /** Returns the global {@link Screen} of this application. */
    @NonNull
    public Screen getScreen() {
        return mGlobals.mScreen;
    }

    /** Global objects made available via {@link ProjectorContext}. */
    private static class ProjectorGlobals {

        final Screen mScreen;

        ProjectorGlobals(Screen screen) {
            this.mScreen = screen;
        }
    }
}
