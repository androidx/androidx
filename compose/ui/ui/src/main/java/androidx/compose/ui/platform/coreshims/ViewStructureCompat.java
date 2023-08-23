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

package androidx.compose.ui.platform.coreshims;

import static android.os.Build.VERSION.SDK_INT;

import android.view.ViewStructure;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * Helper for accessing features in {@link ViewStructure}.
 * <p>
 * Currently this helper class only has features for content capture usage. Other features for
 * Autofill are not available.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ViewStructureCompat {

    // Only guaranteed to be non-null on SDK_INT >= 23.
    private final Object mWrappedObj;

    /**
     * Provides a backward-compatible wrapper for {@link ViewStructure}.
     * <p>
     * This method is not supported on devices running SDK < 23 since the platform
     * class will not be available.
     *
     * @param contentCaptureSession platform class to wrap
     * @return wrapped class
     */
    @RequiresApi(23)
    @NonNull
    public static ViewStructureCompat toViewStructureCompat(
            @NonNull ViewStructure contentCaptureSession) {
        return new ViewStructureCompat(contentCaptureSession);
    }

    /**
     * Provides the {@link ViewStructure} represented by this object.
     * <p>
     * This method is not supported on devices running SDK < 23 since the platform
     * class will not be available.
     *
     * @return platform class object
     * @see ViewStructureCompat#toViewStructureCompat(ViewStructure)
     */
    @RequiresApi(23)
    @NonNull
    public ViewStructure toViewStructure() {
        return (ViewStructure) mWrappedObj;
    }

    private ViewStructureCompat(@NonNull ViewStructure viewStructure) {
        this.mWrappedObj = viewStructure;
    }

    /**
     * Set the text that is associated with this view.  There is no selection
     * associated with the text.  The text may have style spans to supply additional
     * display and semantic information.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 23 and above, this method matches platform behavior.
     * <li>SDK 22 and below, this method does nothing.
     * </ul>
     */
    public void setText(@NonNull CharSequence charSequence) {
        if (SDK_INT >= 23) {
            Api23Impl.setText((ViewStructure) mWrappedObj, charSequence);
        }
    }

    /**
     * Set the class name of the view, as per
     * {@link android.view.View#getAccessibilityClassName View.getAccessibilityClassName()}.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 23 and above, this method matches platform behavior.
     * <li>SDK 22 and below, this method does nothing.
     * </ul>
     */
    public void setClassName(@NonNull String string) {
        if (SDK_INT >= 23) {
            Api23Impl.setClassName((ViewStructure) mWrappedObj, string);
        }
    }

    /**
     * Explicitly set default global style information for text that was previously set with
     * {@link #setText}.
     *
     * @param size The size, in pixels, of the text.
     * @param fgColor The foreground color, packed as 0xAARRGGBB.
     * @param bgColor The background color, packed as 0xAARRGGBB.
     * @param style Style flags, as defined by {@link android.app.assist.AssistStructure.ViewNode}.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 23 and above, this method matches platform behavior.
     * <li>SDK 22 and below, this method does nothing.
     * </ul>
     */
    public void setTextStyle(float size, int fgColor, int bgColor, int style) {
        if (SDK_INT >= 23) {
            Api23Impl.setTextStyle((ViewStructure) mWrappedObj, size, fgColor, bgColor, style);
        }
    }

    /**
     * Set the content description of the view, as per
     * {@link android.view.View#getContentDescription View.getContentDescription()}.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 23 and above, this method matches platform behavior.
     * <li>SDK 22 and below, this method does nothing.
     * </ul>
     */
    public void setContentDescription(@NonNull CharSequence charSequence) {
        if (SDK_INT >= 23) {
            Api23Impl.setContentDescription((ViewStructure) mWrappedObj, charSequence);
        }
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
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 23 and above, this method matches platform behavior.
     * <li>SDK 22 and below, this method does nothing.
     * </ul>
     */
    public void setDimens(int left, int top, int scrollX, int scrollY, int width, int height) {
        if (SDK_INT >= 23) {
            Api23Impl.setDimens(
                    (ViewStructure) mWrappedObj, left, top, scrollX, scrollY, width, height);
        }
    }

    @RequiresApi(23)
    private static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void setDimens(ViewStructure viewStructure, int left, int top, int scrollX,
                int scrollY, int width, int height) {
            viewStructure.setDimens(left, top, scrollX, scrollY, width, height);
        }

        @DoNotInline
        static void setText(ViewStructure viewStructure, CharSequence charSequence) {
            viewStructure.setText(charSequence);
        }

        @DoNotInline
        static void setClassName(ViewStructure viewStructure, String string) {
            viewStructure.setClassName(string);
        }

        @DoNotInline
        static void setContentDescription(ViewStructure viewStructure, CharSequence charSequence) {
            viewStructure.setContentDescription(charSequence);
        }

        @DoNotInline
        static void setTextStyle(
                ViewStructure viewStructure, float size, int fgColor, int bgColor, int style) {
            viewStructure.setTextStyle(size, fgColor, bgColor, style);
        }
    }
}
