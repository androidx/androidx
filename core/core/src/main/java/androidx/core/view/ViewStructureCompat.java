/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.view;

import android.view.ViewStructure;

import org.jspecify.annotations.NonNull;

/**
 * Helper for accessing features in {@link ViewStructure}.
 * <p>
 * Currently this helper class only has features for content capture usage. Other features for
 * Autofill are not available.
 */
public class ViewStructureCompat {

    private final ViewStructure mWrappedObj;

    /**
     * Provides a backward-compatible wrapper for {@link ViewStructure}.
     *
     * @param contentCaptureSession platform class to wrap
     * @return wrapped class
     */
    public static @NonNull ViewStructureCompat toViewStructureCompat(
            @NonNull ViewStructure contentCaptureSession) {
        return new ViewStructureCompat(contentCaptureSession);
    }

    /**
     * Provides the {@link ViewStructure} represented by this object.
     *
     * @return platform class object
     * @see ViewStructureCompat#toViewStructureCompat(ViewStructure)
     */
    public @NonNull ViewStructure toViewStructure() {
        return mWrappedObj;
    }

    private ViewStructureCompat(@NonNull ViewStructure viewStructure) {
        this.mWrappedObj = viewStructure;
    }

    /**
     * Set the text that is associated with this view.  There is no selection
     * associated with the text.  The text may have style spans to supply additional
     * display and semantic information.
     */
    public void setText(@NonNull CharSequence charSequence) {
        mWrappedObj.setText(charSequence);
    }

    /**
     * Set the class name of the view, as per
     * {@link android.view.View#getAccessibilityClassName View.getAccessibilityClassName()}.
     */
    public void setClassName(@NonNull String string) {
        mWrappedObj.setClassName(string);
    }

    /**
     * Set the content description of the view, as per
     * {@link android.view.View#getContentDescription View.getContentDescription()}.
     */
    public void setContentDescription(@NonNull CharSequence charSequence) {
        mWrappedObj.setContentDescription(charSequence);
    }

    /**
     * Set the basic dimensions of this view.
     *
     * @param left The view's left position, in pixels relative to its parent's left edge.
     * @param top The view's top position, in pixels relative to its parent's top edge.
     * @param scrollX How much the view's x coordinate space has been scrolled, in pixels.
     * @param scrollY How much the view's y coordinate space has been scrolled, in pixels.
     * @param width The view's visible width, in pixels.  This is the width visible on screen,
     * not the total data width of a scrollable view.
     * @param height The view's visible height, in pixels.  This is the height visible on
     * screen, not the total data height of a scrollable view.
     */
    public void setDimens(int left, int top, int scrollX, int scrollY, int width, int height) {
        mWrappedObj.setDimens(left, top, scrollX, scrollY, width, height);
    }
}
