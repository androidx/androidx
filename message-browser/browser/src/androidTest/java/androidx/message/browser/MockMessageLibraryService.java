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

package androidx.message.browser;

import android.os.Bundle;

/**
 * Mock implementation of {@link MessageLibraryService} for testing.
 */
public class MockMessageLibraryService extends MessageLibraryService {
    private static final String TAG = "MockMsgLibSvc";

    public static final String KEY_REFUSE_CONNECTION =
            "MockMessageLibraryService.KEY_REFUSE_CONNECTION";
    public static final String KEY_CRASH_CONNECTION =
            "MockMessageLibraryService.KEY_CRASH_CONNECTION";

    public MockMessageLibraryService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public MessageCommandGroup onConnect(BrowserInfo info) {
        Bundle connectionHints = info.getConnectionHints();
        if (connectionHints.getBoolean(KEY_REFUSE_CONNECTION)) {
            return null;
        } else if (connectionHints.getBoolean(KEY_CRASH_CONNECTION)) {
            throw new RuntimeException("Crash for testing.");
        }
        return super.onConnect(info);
    }
}
