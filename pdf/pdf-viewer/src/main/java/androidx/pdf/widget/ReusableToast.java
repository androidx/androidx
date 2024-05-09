/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.widget;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * A single toast that can be shown and hidden any number of times. Showing it when it is already
 * shown just makes it stay for longer. It hides automatically.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ReusableToast {

    private final View mView;

    private int mAutoHideDelayMs = 1000;

    public ReusableToast(@NonNull View view) {
        this.mView = view;
    }

    @NonNull
    public View getView() {
        return mView;
    }

    public void setAutoHideDelayMs(int ms) {
        this.mAutoHideDelayMs = ms;
    }

    /** Show view by setting visibility to VISIBLE. */
    public void show() {
        mView.animate().cancel();
        mView.setVisibility(View.VISIBLE);
        mView.requestLayout(); // This text view will not update the text when View.GONE.
        mView.setAlpha(1);
        mView.bringToFront();
        makeAutoHide();
    }

    /** Hide view by setting visibility to GONE. */
    public void hide() {
        mView.setVisibility(View.GONE);
    }

    private void makeAutoHide() {
        mView.animate().setStartDelay(mAutoHideDelayMs).alpha(0).withEndAction(this::hide);
    }
}
