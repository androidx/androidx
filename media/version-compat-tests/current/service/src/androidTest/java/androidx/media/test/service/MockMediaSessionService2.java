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

package androidx.media.test.service;

import androidx.media2.MediaSession2;
import androidx.media2.MediaSessionService2;
import androidx.media2.SessionToken2;

/**
 * Dummy MediaSessionService2 for testing {@link SessionToken2}.
 */
public class MockMediaSessionService2 extends MediaSessionService2 {
    // Keep in sync with the AndroidManifest.xml
    public static final String ID = "TestSession";

    @Override
    public MediaSession2 onCreateSession(String sessionId) {
        return null;
    }
}
