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

package androidx.mediarouter.media;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test proper initialization of {@link MediaRouter}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaRouterInitializationTest {
    /**
     * This test checks weather MediaRouter is initialized well if an empty route exists
     */
    @Test
    @MediumTest
    public void testEmptyUserRoute() throws Exception {
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            final Context context = getApplicationContext();
                            android.media.MediaRouter router =
                                    (android.media.MediaRouter)
                                            context.getSystemService(Context.MEDIA_ROUTER_SERVICE);

                            // Add empty user route
                            android.media.MediaRouter.RouteCategory category =
                                    router.createRouteCategory("", false);
                            android.media.MediaRouter.UserRouteInfo routeInfo =
                                    router.createUserRoute(category);
                            router.addUserRoute(routeInfo);

                            MediaRouter mediaRouter = MediaRouter.getInstance(context);
                            assertTrue(mediaRouter.getDefaultRoute() != null);
                        });
    }
}
