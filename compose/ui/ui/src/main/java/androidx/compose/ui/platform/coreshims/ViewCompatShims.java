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

import android.os.Build;
import android.view.View;
import android.view.autofill.AutofillId;
import android.view.contentcapture.ContentCaptureSession;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Helper for accessing features in {@link View}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ViewCompatShims {
    private ViewCompatShims() {
        // This class is not instantiable.
    }

    @IntDef({
            IMPORTANT_FOR_CONTENT_CAPTURE_AUTO,
            IMPORTANT_FOR_CONTENT_CAPTURE_YES,
            IMPORTANT_FOR_CONTENT_CAPTURE_NO,
            IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS,
            IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS,
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface ImportantForContentCapture {}

    /**
     * Automatically determine whether a view is important for content capture.
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_AUTO = 0x0;

    /**
     * The view is important for content capture, and its children (if any) will be traversed.
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_YES = 0x1;

    /**
     * The view is not important for content capture, but its children (if any) will be traversed.
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_NO = 0x2;

    /**
     * The view is important for content capture, but its children (if any) will not be traversed.
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS = 0x4;

    /**
     * The view is not important for content capture, and its children (if any) will not be
     * traversed.
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS = 0x8;

    /**
     * Sets the mode for determining whether this view is considered important for content capture.
     *
     * <p>The platform determines the importance for autofill automatically but you
     * can use this method to customize the behavior. Typically, a view that provides text should
     * be marked as {@link #IMPORTANT_FOR_CONTENT_CAPTURE_YES}.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 30 and above, this method matches platform behavior.
     * <li>SDK 29 and below, this method does nothing.
     * </ul>
     *
     * @param v The View against which to invoke the method.
     * @param mode {@link #IMPORTANT_FOR_CONTENT_CAPTURE_AUTO},
     * {@link #IMPORTANT_FOR_CONTENT_CAPTURE_YES}, {@link #IMPORTANT_FOR_CONTENT_CAPTURE_NO},
     * {@link #IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS},
     * or {@link #IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS}.
     *
     * @attr ref android.R.styleable#View_importantForContentCapture
     */
    public static void setImportantForContentCapture(@NonNull View v,
            @ImportantForContentCapture int mode) {
        if (Build.VERSION.SDK_INT >= 30) {
            Api30Impl.setImportantForContentCapture(v, mode);
        }
    }

    /**
     * Gets the session used to notify content capture events.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 29 and above, this method matches platform behavior.
     * <li>SDK 28 and below, this method always return null.
     * </ul>
     *
     * @param v The View against which to invoke the method.
     * @return session explicitly set by {@link #setContentCaptureSession(ContentCaptureSession)},
     * inherited by ancestors, default session or {@code null} if content capture is disabled for
     * this view.
     */
    @Nullable
    public static ContentCaptureSessionCompat getContentCaptureSession(@NonNull View v) {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentCaptureSession session = Api29Impl.getContentCaptureSession(v);
            if (session == null) {
                return null;
            }
            return ContentCaptureSessionCompat.toContentCaptureSessionCompat(session, v);
        }
        return null;
    }

    /**
     * Gets the unique, logical identifier of this view in the activity, for autofill purposes.
     *
     * <p>The autofill id is created on demand, unless it is explicitly set by
     * {@link #setAutofillId(AutofillId)}.
     *
     * <p>See {@link #setAutofillId(AutofillId)} for more info.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 26 and above, this method matches platform behavior.
     * <li>SDK 25 and below, this method always return null.
     * </ul>
     *
     * @param v The View against which to invoke the method.
     * @return The View's autofill id.
     */
    @Nullable
    public static AutofillIdCompat getAutofillId(@NonNull View v) {
        if (Build.VERSION.SDK_INT >= 26) {
            return AutofillIdCompat.toAutofillIdCompat(Api26Impl.getAutofillId(v));
        }
        return null;
    }

    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() {
            // This class is not instantiable.
        }
        @DoNotInline
        public static AutofillId getAutofillId(View view) {
            return view.getAutofillId();
        }
    }

    @RequiresApi(29)
    private static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static ContentCaptureSession getContentCaptureSession(View view) {
            return view.getContentCaptureSession();
        }
    }

    @RequiresApi(30)
    private static class Api30Impl {
        private Api30Impl() {
            // This class is not instantiable.
        }
        @DoNotInline
        static void setImportantForContentCapture(View view, int mode) {
            view.setImportantForContentCapture(mode);
        }
    }
}
