/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.mediarouter.testing;

import android.content.Context;

import androidx.annotation.MainThread;
import androidx.mediarouter.media.MediaRouter;

/**
 * A helper class for testing usages of {@link MediaRouter}.
 */
public class MediaRouterTestHelper {

    /**
     * Resets all internal state of {@link MediaRouter} for testing. Should be only used for
     * testing purpose.
     * <p>
     * After calling this method, the caller should stop using the existing media router instances.
     * Instead, the caller should create a new media router instance again by calling
     * {@link MediaRouter#getInstance(Context)}.
     * <p>
     * Note that the following classes' instances need to be recreated after calling this method,
     * as these classes store the media router instance on their constructor:
     * <ul>
     *     <li>{@link androidx.mediarouter.app.MediaRouteActionProvider}
     *     <li>{@link androidx.mediarouter.app.MediaRouteButton}
     *     <li>{@link androidx.mediarouter.app.MediaRouteChooserDialog}
     *     <li>{@link androidx.mediarouter.app.MediaRouteControllerDialog}
     *     <li>{@link androidx.mediarouter.app.MediaRouteDiscoveryFragment}
     * </ul>
     * Please make sure this is called in the main thread.
     */
    @MainThread
    public static void resetMediaRouter() {
        MediaRouter.resetGlobalRouter();
    }

    private MediaRouterTestHelper() {}
}
