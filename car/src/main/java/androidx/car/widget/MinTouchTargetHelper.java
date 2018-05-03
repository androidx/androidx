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

package androidx.car.widget;

import android.graphics.Rect;
import android.view.TouchDelegate;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * Helper class that will ensure that a given view meets a minimum touch target size that is
 * specified.
 */
class MinTouchTargetHelper {
    private MinTouchTargetHelper() {}

    /**
     * Sets up the view that will be checked and ensured to meet a minimum touch target size.
     *
     * @param view The view that to be checked; cannot be {@code null}.
     * @return A {@link TouchTargetSubject} that can be customized with touch target information.
     */
    static TouchTargetSubject ensureThat(View view) {
        if (view == null) {
            throw new IllegalArgumentException("View cannot be null.");
        }
        return new TouchTargetSubject(view);
    }

    /**
     * A class to encapsulate information concerning a view whose target target needs to be
     * expanded to meet a minimum touch size.
     *
     * <p>This class should not be created directly. Instead use
     * {@link MinTouchTargetHelper#ensureThat(View)} to get an instance of this class.
     */
    static class TouchTargetSubject {
        private View mSubjectView;

        private TouchTargetSubject(@NonNull View subject) {
            mSubjectView = subject;
        }

        /**
         * Sets the minimum touch target size in pixels for the subject view. Calling this method
         * will also set up the framework to do this assurance.
         *
         * <p>Under the hood, this method uses a {@link TouchDelegate} to handle the delegation,
         * meaning that only one child view can be set per parent.
         *
         * @param size The minimum touch target size in pixels.
         */
        void hasMinTouchSize(int size) {
            if (size <= 0) {
                throw new IllegalStateException(
                        "Minimum touch target size must be greater than 0.");
            }

            if (!(mSubjectView.getParent() instanceof View)) {
                throw new IllegalStateException("Subject view does not have a valid parent of type"
                        + " View. Parent is: " + mSubjectView.getParent());
            }

            View parentView = (View) mSubjectView.getParent();

            // The TouchDelegate needs to be set after the subject view has been laid out in
            // order to get the hit Rect. Use a post() to ensure this.
            parentView.post(() -> {
                Rect rect = new Rect();
                mSubjectView.getHitRect(rect);

                // Ensure that the touch target for the icon is the minimum touch size.
                int hitWidth = Math.abs(rect.right - rect.left);
                if (hitWidth < size) {
                    int amountToIncrease = (size - hitWidth) / 2;
                    rect.left -= amountToIncrease;
                    rect.right += amountToIncrease;
                }

                int hitHeight = Math.abs(rect.top - rect.bottom);
                if (hitHeight < size) {
                    int amountToIncrease = (size - hitHeight) / 2;
                    rect.top -= amountToIncrease;
                    rect.bottom += amountToIncrease;
                }

                parentView.setTouchDelegate(new TouchDelegate(rect, mSubjectView));
            });
        }
    }
}
