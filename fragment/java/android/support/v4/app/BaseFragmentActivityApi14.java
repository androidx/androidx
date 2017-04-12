/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.v4.app;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

@RequiresApi(14)
abstract class BaseFragmentActivityApi14 extends SupportActivity {

    // We need to keep track of whether startIntentSenderForResult originated from a Fragment, so we
    // can conditionally check whether the requestCode collides with our reserved ID space for the
    // request index (see above). Unfortunately we can't just call
    // super.startIntentSenderForResult(...) to bypass the check when the call didn't come from a
    // fragment, since we need to use the ActivityCompat version for backward compatibility.
    boolean mStartedIntentSenderFromFragment;

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        final View v = dispatchFragmentsOnCreateView(parent, name, context, attrs);
        if (v == null) {
            return super.onCreateView(parent, name, context, attrs);
        }
        return v;
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        final View v = dispatchFragmentsOnCreateView(null, name, context, attrs);
        if (v == null) {
            return super.onCreateView(name, context, attrs);
        }
        return v;
    }

    abstract View dispatchFragmentsOnCreateView(View parent, String name,
            Context context, AttributeSet attrs);

    @Override
    public void startIntentSenderForResult(IntentSender intent, int requestCode,
            @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags)
            throws IntentSender.SendIntentException {
        // If this was started from a Fragment we've already checked the upper 16 bits were not in
        // use, and then repurposed them for the Fragment's index.
        if (!mStartedIntentSenderFromFragment) {
            if (requestCode != -1) {
                checkForValidRequestCode(requestCode);
            }
        }
        super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues,
                extraFlags);
    }

    /**
     * Checks whether the given request code is a valid code by masking it with 0xffff0000. Throws
     * an {@link IllegalArgumentException} if the code is not valid.
     */
    static void checkForValidRequestCode(int requestCode) {
        if ((requestCode & 0xffff0000) != 0) {
            throw new IllegalArgumentException("Can only use lower 16 bits for requestCode");
        }
    }
}
