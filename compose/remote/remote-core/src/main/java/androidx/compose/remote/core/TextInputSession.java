/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.managers.CoreText;
import androidx.compose.remote.core.operations.layout.managers.TextInputLayout;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An active text editing session, handed to the {@link TextInputSupport} when a {@link
 * TextInputLayout} is clicked.
 *
 * <p>The session gives the host what it needs to present an editor: the layout (keyboard type, IME
 * action, flags, limits, hint), the text component displaying the edited string when there is one
 * (to place and style the editor), the window bounds to cover, and the current text. The
 * host writes edits back with {@link #setText(String)} and calls {@link #end()} when editing is
 * over.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class TextInputSession {
    private final @NonNull RemoteContext mContext;
    private final @NonNull TextInputLayout mLayout;
    private final @Nullable CoreText mTextComponent;
    private final float @NonNull [] mBounds = new float[4];
    private boolean mActive = true;

    /**
     * @param layout the edited layout
     */
    public TextInputSession(@NonNull RemoteContext context, @NonNull TextInputLayout layout) {
        mContext = context;
        mLayout = layout;
        mTextComponent = layout.getTextComponent();
        updateBounds();
    }

    /** Returns the id of the edited layout. */
    public int getComponentId() {
        return mLayout.getComponentId();
    }

    /** Returns the edited layout. */
    public @NonNull Component getComponent() {
        return mLayout;
    }

    /**
     * Returns the edited layout, carrying the editor configuration (keyboard type, IME action,
     * flags, limits, hint).
     */
    public @NonNull TextInputLayout getLayout() {
        return mLayout;
    }

    /**
     * Returns the text component displaying the edited string in the layout's content, if any, so
     * the host can match its editor's text style (size, weight, color, alignment) to it. {@link
     * #getBounds()} covers this component when present, the whole layout otherwise.
     */
    public @Nullable CoreText getTextComponent() {
        return mTextComponent;
    }

    /**
     * Returns the bounds the editor should cover relative to the document root, as {@code [x, y,
     * width, height]} in document pixels: the text component's when there is one, else the
     * layout's. The array is owned by the session and refreshed on every
     * {@link TextInputSupport#updateTextInputBounds(TextInputSession)}.
     */
    public float @NonNull [] getBounds() {
        return mBounds;
    }

    /** Returns the current value of the edited string. */
    public @NonNull String getText() {
        String text = mContext.getText(mLayout.getTextId());
        return text != null ? text : "";
    }

    /**
     * Writes a new value for the edited string into the document. Every binding on that string
     * updates through the usual listener mechanism, and a repaint is requested.
     *
     * @param text the new value
     */
    public void setText(@NonNull String text) {
        if (!mActive) {
            return;
        }
        mContext.loadText(mLayout.getTextId(), text);
        mContext.needsRepaint();
    }

    /** Returns the placeholder to show when the text is empty, or null if none was declared. */
    public @Nullable String getHint() {
        int hintId = mLayout.getHintId();
        return hintId != 0 ? mContext.getText(hintId) : null;
    }

    /** Returns true until {@link #end()} is called or the document stops the session. */
    public boolean isActive() {
        return mActive;
    }

    /**
     * Ends the session: the component renders its own text again. Called by the host once the
     * editor is dismissed. Calling it more than once is a no-op.
     */
    public void end() {
        if (!mActive) {
            return;
        }
        mActive = false;
        mLayout.onSessionEnded(mContext, this);
    }

    /**
     * Recomputes {@link #getBounds()} from the current layout.
     *
     * @return true if the bounds changed
     */
    public boolean updateBounds() {
        float x = mBounds[0];
        float y = mBounds[1];
        float w = mBounds[2];
        float h = mBounds[3];
        Component target = mTextComponent != null ? mTextComponent : mLayout;
        mBounds[0] = 0f;
        mBounds[1] = 0f;
        target.getLocationInWindow(mContext, mBounds);
        mBounds[2] = target.getWidth();
        mBounds[3] = target.getHeight();
        return x != mBounds[0] || y != mBounds[1] || w != mBounds[2] || h != mBounds[3];
    }
}
