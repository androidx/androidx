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

package androidx.browser.customtabs;

import android.net.Uri;
import android.os.Bundle;

import java.util.List;

/**
 * A test class that simulates how a {@link CustomTabsService} would behave.
 */

public class TestCustomTabsService extends CustomTabsService {
    public static final String CALLBACK_BIND_TO_POST_MESSAGE = "BindToPostMessageService";
    private boolean mPostMessageRequested;
    private CustomTabsSessionToken mSession;

    @Override
    protected boolean warmup(long flags) {
        return false;
    }

    @Override
    protected boolean newSession(CustomTabsSessionToken sessionToken) {
        mSession = sessionToken;
        return true;
    }

    @Override
    protected boolean mayLaunchUrl(CustomTabsSessionToken sessionToken,
                                   Uri url, Bundle extras, List<Bundle> otherLikelyBundles) {
        return false;
    }

    @Override
    protected Bundle extraCommand(String commandName, Bundle args) {
        return null;
    }

    @Override
    protected boolean updateVisuals(CustomTabsSessionToken sessionToken, Bundle bundle) {
        return false;
    }

    @Override
    protected boolean requestPostMessageChannel(
            CustomTabsSessionToken sessionToken, Uri postMessageOrigin) {
        if (mSession == null) return false;
        mPostMessageRequested = true;
        mSession.getCallback().extraCallback(CALLBACK_BIND_TO_POST_MESSAGE, null);
        return true;
    }

    @Override
    protected int postMessage(CustomTabsSessionToken sessionToken, String message, Bundle extras) {
        if (!mPostMessageRequested) return CustomTabsService.RESULT_FAILURE_DISALLOWED;
        return CustomTabsService.RESULT_SUCCESS;
    }

    @Override
    protected boolean validateRelationship(CustomTabsSessionToken sessionToken,
                                           @Relation int relation, Uri origin, Bundle extras) {
        return false;
    }
}
