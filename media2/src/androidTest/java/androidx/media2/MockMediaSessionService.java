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

package androidx.media2;

import androidx.media2.MediaSession.SessionCallback;
import androidx.media2.TestUtils.SyncHandler;

import java.util.concurrent.Executor;

/**
 * Mock implementation of {@link MediaSessionService} for testing.
 */
public class MockMediaSessionService extends MediaSessionService {
    /**
     * ID of the session that this service will create.
     */
    public static final String ID = "TestSession";

    private MediaSession mSession;

    @Override
    public void onCreate() {
        TestServiceRegistry.getInstance().setServiceInstance(this);
        super.onCreate();
    }

    @Override
    public MediaSession onGetSession() {
        TestServiceRegistry registry = TestServiceRegistry.getInstance();
        TestServiceRegistry.OnGetSessionHandler onGetSessionHandler =
                registry.getOnGetSessionHandler();
        if (onGetSessionHandler != null) {
            return onGetSessionHandler.onGetSession();
        }
        if (getSessions().size() > 0) {
            return getSessions().get(0);
        }
        final MockPlayer player = new MockPlayer(1);
        final SyncHandler handler = (SyncHandler) registry.getHandler();
        final Executor executor = new Executor() {
            @Override
            public void execute(Runnable runnable) {
                handler.post(runnable);
            }
        };
        SessionCallback sessionCallback = registry.getSessionCallback();
        if (sessionCallback == null) {
            // Ensures non-null
            sessionCallback = new SessionCallback() {};
        }
        mSession = new MediaSession.Builder(this, player)
                .setSessionCallback(executor, sessionCallback)
                .setId(ID)
                .build();
        return mSession;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        TestServiceRegistry.getInstance().cleanUp();
    }
}
