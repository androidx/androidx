/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v4.app;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Base class for {@code FragmentActivity} to be able to use v16 APIs.
 *
 * @hide
 */
abstract class BaseFragmentActivityJB extends BaseFragmentActivityHoneycomb {

    // We need to keep track of whether startActivityForResult originated from a Fragment, so we
    // can conditionally check whether the requestCode collides with our reserved ID space for the
    // request index (see above). Unfortunately we can't just call
    // super.startActivityForResult(...) to bypass the check when the call didn't come from a
    // fragment, since we need to use the ActivityCompat version for backward compatibility.
    boolean mStartedActivityFromFragment;

    @Override
    public void startActivityForResult(Intent intent, int requestCode,
            @Nullable Bundle options) {
        // If this was started from a Fragment we've already checked the upper 16 bits were not in
        // use, and then repurposed them for the Fragment's index.
        if (!mStartedActivityFromFragment) {
            if (requestCode != -1) {
                checkForValidRequestCode(requestCode);
            }
        }
        super.startActivityForResult(intent, requestCode, options);
    }

    @Override
    public void startIntentSenderForResult(IntentSender intent, int requestCode,
            @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags,
            Bundle options) throws IntentSender.SendIntentException {
        // If this was started from a Fragment we've already checked the upper 16 bits were not in
        // use, and then repurposed them for the Fragment's index.
        if (!mStartedIntentSenderFromFragment) {
            if (requestCode != -1) {
                checkForValidRequestCode(requestCode);
            }
        }
        super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues,
                extraFlags, options);
    }
}
