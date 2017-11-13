/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.transition;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v17.leanback.R;

/**
 * Helper class to load Leanback specific transition.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class LeanbackTransitionHelper {

    interface LeanbackTransitionHelperVersion {
        Object loadTitleInTransition(Context context);
        Object loadTitleOutTransition(Context context);
    }

    /*
     * Kitkat does not allow load custom transition from resource, calling
     * LeanbackTransitionHelperKitKat to build custom transition in code.
     */
    @RequiresApi(19)
    static class LeanbackTransitionHelperKitKatImpl implements LeanbackTransitionHelperVersion {

        @Override
        public Object loadTitleInTransition(Context context) {
            return LeanbackTransitionHelperKitKat.loadTitleInTransition(context);
        }

        @Override
        public Object loadTitleOutTransition(Context context) {
            return LeanbackTransitionHelperKitKat.loadTitleOutTransition(context);
        }
    }

    /*
     * Load transition from resource or just return stub for API17.
     */
    static class LeanbackTransitionHelperDefault implements LeanbackTransitionHelperVersion {

        @Override
        public Object loadTitleInTransition(Context context) {
            return TransitionHelper.loadTransition(context, R.transition.lb_title_in);
        }

        @Override
        public Object loadTitleOutTransition(Context context) {
            return TransitionHelper.loadTransition(context, R.transition.lb_title_out);
        }
    }

    static LeanbackTransitionHelperVersion sImpl;

    static {
        if (Build.VERSION.SDK_INT >= 21) {
            sImpl = new LeanbackTransitionHelperDefault();
        } else if (Build.VERSION.SDK_INT >= 19) {
            sImpl = new LeanbackTransitionHelperKitKatImpl();
        } else {
            // Helper will create a stub object for transition in this case.
            sImpl = new LeanbackTransitionHelperDefault();
        }
    }

    static public Object loadTitleInTransition(Context context) {
        return sImpl.loadTitleInTransition(context);
    }

    static public Object loadTitleOutTransition(Context context) {
        return sImpl.loadTitleOutTransition(context);
    }
}
