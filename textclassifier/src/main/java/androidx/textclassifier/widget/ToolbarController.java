/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.textclassifier.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.collection.ArrayMap;
import androidx.core.app.RemoteActionCompat;
import androidx.core.internal.view.SupportMenu;
import androidx.core.util.Preconditions;
import androidx.textclassifier.R;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

/**
 * Controls displaying of actions in the floating toolbar.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
@RequiresApi(Build.VERSION_CODES.M)
@UiThread
public final class ToolbarController {

    private static final String LOG_TAG = "ToolbarController";
    private static final int ORDER_START = 50;
    private static final int ALPHA = 20;
    private static final int HIGHLIGHT_DELAY_MS = 80;

    private final TextView mTextView;
    private final Rect mContentRect;
    private final FloatingToolbar mToolbar;
    private final BackgroundSpan mHighlight;

    private static WeakReference<ToolbarController> sInstance = new WeakReference<>(null);

    /**
     * Returns the singleton instance of the toolbar controller and associates it with the specified
     * textView. If the toolbar was initially associated with a different textView, the toolbar will
     * be dismissed before associating it with the newly specified textView.
     */
    public static ToolbarController getInstance(TextView textView) {
        final ToolbarController controller = sInstance.get();
        if (controller == null) {
            sInstance = new WeakReference<>(new ToolbarController(textView));
        } else if (controller.mTextView != textView) {
            logv("New textView. Dismissing previous toolbar.");
            dismissImmediately(controller.mToolbar);
            sInstance = new WeakReference<>(new ToolbarController(textView));
        }
        return sInstance.get();
    }

    private ToolbarController(TextView textView) {
        mTextView = Preconditions.checkNotNull(textView);
        mContentRect = new Rect();
        mHighlight = new BackgroundSpan(withAlpha(mTextView.getHighlightColor()));
        mToolbar = new FloatingToolbar(textView);
        mToolbar.setOnMenuItemClickListener(new OnMenuItemClickListener(mToolbar));
        mToolbar.setDismissOnMenuItemClick(true);
    }

    /**
     * Shows the floating toolbar with the specified actions.
     *
     * <p>This controller also adds standard items (e.g. Copy, Share) to the toolbar in addition to
     * the specified actions.
     *
     * @param actions actions to show in the toolbar
     * @param start text start index for positioning the toolbar;
     *              must be less at least 0 and less than end index
     * @param end text end index for positioning the toolbar;
     *            the toolbar will not be shown this index is invalid for the associated textView
     */
    public void show(List<RemoteActionCompat> actions, int start, int end) {
        Preconditions.checkNotNull(actions);
        Preconditions.checkArgumentInRange(start, 0, end - 1, "start");

        final CharSequence text = mTextView.getText();
        if (text == null || end > text.length()) {
            Log.d(LOG_TAG, "Cannot show link toolbar. Invalid text indices");
            return;
        }

        logv("About to show new toolbar state. Dismissing old state");
        dismissImmediately(mToolbar);
        final SupportMenu menu = createMenu(mTextView, mHighlight, actions);
        if (canShowToolbar(mTextView, true) && menu.hasVisibleItems()) {
            setListeners(mTextView, start, end, mToolbar);
            setHighlight(mTextView, mHighlight, start, end, mToolbar);
            updateRectCoordinates(mContentRect, mTextView, start, end);
            mToolbar.setContentRect(mContentRect);
            mToolbar.setMenu(menu);
            mToolbar.show();
            logv("Showing toolbar");
        }
    }

    @VisibleForTesting
    boolean isToolbarShowing() {
        return mToolbar.isShowing();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void dismissImmediately(FloatingToolbar toolbar) {
        toolbar.hide();
        toolbar.dismiss();
    }

    /**
     * Returns true if the textView should be allowed to show a toolbar. Otherwise, returns false.
     *
     * @param textView the textView
     * @param assumeWindowFocus if true, this method assumes the window in which the textView is in
     *                          has focus. Should typically be set to {@code true} unless the caller
     *                          knows the window does not have focus.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean canShowToolbar(TextView textView, boolean assumeWindowFocus) {
        final boolean viewFocus = textView.hasFocus();
        final boolean viewAttached = textView.isAttachedToWindow();
        final boolean canShowToolbar = assumeWindowFocus && viewFocus && viewAttached;
        if (!canShowToolbar) {
            logv(String.format("canShowToolbar=false. "
                    + "Reason: windowFocus=%b, viewFocus=%b, viewAttached=%b",
                    assumeWindowFocus, viewFocus, viewAttached));
        }
        return canShowToolbar;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static int withAlpha(int color) {
        return Color.argb(ALPHA, Color.red(color), Color.green(color), Color.blue(color));
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable
    static String getHighlightedText(TextView textView, BackgroundSpan highlight) {
        final CharSequence text = textView.getText();
        if (text instanceof Spannable) {
            final Spannable spannable = (Spannable) text;
            final int start = spannable.getSpanStart(highlight);
            final int end = spannable.getSpanEnd(highlight);
            final int min = Math.max(0, Math.min(start, end));
            final int max = Math.max(0, Math.max(start, end));
            return textView.getText().subSequence(min, max).toString();
        }
        return null;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void removeHighlight(TextView textView) {
        final CharSequence text = textView.getText();
        if (text instanceof Spannable) {
            final Spannable spannable = (Spannable) text;
            final BackgroundSpan[] spans =
                    spannable.getSpans(0, text.length(), BackgroundSpan.class);
            for (BackgroundSpan span : spans) {
                spannable.removeSpan(span);
            }
        }
    }

    private static void setHighlight(
            final TextView textView, final BackgroundSpan highlight,
            final int start, final int end, final FloatingToolbar toolbar) {
        final CharSequence text = textView.getText();
        if (text instanceof Spannable) {
            removeHighlight(textView);
            final String originalText = text.toString();
            textView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (canShowToolbar(textView, true)
                            && originalText.equals(textView.getText().toString())
                            && toolbar.isShowing()) {
                        ((Spannable) text).setSpan(highlight, start, end, 0);
                    }
                }
            }, HIGHLIGHT_DELAY_MS);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void updateRectCoordinates(Rect rect, TextView textView, int start, int end) {
        final int[] startXY = getCoordinates(textView, start);
        final int[] endXY = getCoordinates(textView, end);
        rect.set(startXY[0], startXY[1], endXY[0], endXY[1]);
        rect.sort();
    }

    private static int[] getCoordinates(TextView textView, int index) {
        final Layout layout = textView.getLayout();
        final int line = layout.getLineForOffset(index);
        final int x = (int) layout.getPrimaryHorizontal(index);
        final int y = layout.getLineTop(line);
        final int[] xy = new int[2];
        textView.getLocationOnScreen(xy);
        return new int[]{
                x + textView.getTotalPaddingLeft() - textView.getScrollX() + xy[0],
                y + textView.getTotalPaddingTop() - textView.getScrollY() + xy[1]};
    }

    private static SupportMenu createMenu(
            final TextView textView,
            final BackgroundSpan highlight,
            List<RemoteActionCompat> actions) {
        final MenuBuilder menu = new MenuBuilder(textView.getContext());
        final Map<MenuItem, PendingIntent> menuActions = new ArrayMap<>();
        final int size = actions.size();
        for (int i = 0; i < size; i++) {
            final RemoteActionCompat action = actions.get(i);
            final MenuItem item = menu.add(
                    FloatingToolbar.MENU_ID_SMART_ACTION  /* groupId */,
                    i == 0 ? FloatingToolbar.MENU_ID_SMART_ACTION : i  /* itemId */,
                    i == 0 ? 0 : ORDER_START + i  /* order */,
                    action.getTitle()  /* title */);
            if (action.shouldShowIcon()) {
                item.setIcon(action.getIcon().loadDrawable(textView.getContext()));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                item.setContentDescription(action.getContentDescription());
            }
            item.setShowAsAction(i == 0
                    ? MenuItem.SHOW_AS_ACTION_ALWAYS
                    : MenuItem.SHOW_AS_ACTION_NEVER);
            menuActions.put(item, action.getActionIntent());
        }

        menu.add(Menu.NONE, android.R.id.copy, 1,
                android.R.string.copy)
                .setAlphabeticShortcut('c')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, android.R.id.shareText, 2,
                R.string.abc_share)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.setCallback(new MenuBuilder.Callback() {
            @Override
            public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                final PendingIntent intent = menuActions.get(item);
                if (intent != null) {
                    try {
                        intent.send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(LOG_TAG, "Error performing smart action", e);
                    }
                } else {
                    switch (item.getItemId()) {
                        case android.R.id.copy:
                            copyText();
                            break;
                        case android.R.id.shareText:
                            shareText();
                            break;
                    }
                }
                return true;
            }

            @Override
            public void onMenuModeChange(MenuBuilder menu) {}

            private void copyText() {
                final ClipboardManager clipboard =
                        textView.getContext().getSystemService(ClipboardManager.class);
                final String text = getHighlightedText(textView, highlight);
                if (clipboard != null && !TextUtils.isEmpty(text)) {
                    try {
                        clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
                    } catch (Throwable t) {
                        Log.d(LOG_TAG, "Error copying text: " + t.getMessage());
                    }
                }
            }

            private void shareText() {
                final String text = getHighlightedText(textView, highlight);
                if (!TextUtils.isEmpty(text)) {
                    final Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
                    textView.getContext().startActivity(Intent.createChooser(sharingIntent, null));
                }
            }
        });
        return menu;
    }

    /* To enable verbose logging. Run the following command:
     * adb shell setprop log.tag.ToolbarController VERBOSE && adb shell stop && adb shell start
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void logv(String message) {
        if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
            Log.v(LOG_TAG, message);
        }
    }

    private static void setListeners(
            TextView textView, int start, int end, FloatingToolbar toolbar) {
        toolbar.setOnDismissListener(
                new OnToolbarDismissListener(
                        textView,
                        new TextViewListener(toolbar, textView, start, end),
                        new ActionModeCallback(
                                toolbar,
                                textView.getCustomSelectionActionModeCallback(),
                                /* preferMe= */ false),
                        new ActionModeCallback(
                                toolbar,
                                textView.getCustomInsertionActionModeCallback(),
                                /* preferMe= */ true)));
    }

    /**
     * Listens for several TextView events to reposition or dismiss the toolbar.
     */
    private static final class TextViewListener implements
            ViewTreeObserver.OnPreDrawListener,
            ViewTreeObserver.OnWindowFocusChangeListener,
            ViewTreeObserver.OnGlobalFocusChangeListener,
            ViewTreeObserver.OnWindowAttachListener {

        private static final long THROTTLE_DELAY_MS = 300;

        private final FloatingToolbar mToolbar;
        private final TextView mTextView;
        private final Rect mContentRect;
        private final Rect mTempRect;
        private final int mStart;
        private final int mEnd;

        private long mLastUpdateTimeMs = System.currentTimeMillis() - THROTTLE_DELAY_MS;

        TextViewListener(FloatingToolbar toolbar, TextView textView, int start, int end) {
            mToolbar = Preconditions.checkNotNull(toolbar);
            mTextView = Preconditions.checkNotNull(textView);
            mContentRect = new Rect();
            mTempRect = new Rect();
            mStart = start;
            mEnd = end;
        }

        @Override
        public boolean onPreDraw() {
            final long now = System.currentTimeMillis();
            if (!maybeDismissToolbar(true, "onPreDraw")
                    && mToolbar.isShowing()
                    && now - mLastUpdateTimeMs >= THROTTLE_DELAY_MS) {
                updateRectCoordinates(mTempRect, mTextView, mStart, mEnd);
                if (!mTempRect.equals(mContentRect)) {
                    // View moved.
                    mContentRect.set(mTempRect);
                    mToolbar.setContentRect(mContentRect);
                    mToolbar.updateLayout();
                    mLastUpdateTimeMs = now;
                }
            }
            return true;
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            maybeDismissToolbar(hasFocus, "onWindowFocusChanged");
        }

        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
            maybeDismissToolbar(true, "onGlobalFocusChanged");
        }

        @Override
        public void onWindowAttached() {
            maybeDismissToolbar(true, "onWindowAttached");
        }

        @Override
        public void onWindowDetached() {
            maybeDismissToolbar(true, "onWindowDetached");
        }

        private boolean maybeDismissToolbar(boolean assumeWindowFocus, String caller) {
            if (canShowToolbar(mTextView, assumeWindowFocus)) {
                return false;
            }
            logv("TextViewListener." + caller + ": Dismissing toolbar.");
            dismissImmediately(mToolbar);
            return true;
        }
    }

    /**
     * Wraps a textView's action mode callback so the toolbar can react to action mode updates.
     */
    private static final class ActionModeCallback extends ActionMode.Callback2 {

        private final FloatingToolbar mToolbar;
        @Nullable final ActionMode.Callback mOriginalCallback;
        private final boolean mPreferMe;

        ActionModeCallback(
                FloatingToolbar toolbar,
                @Nullable ActionMode.Callback originalCallback,
                boolean preferMe) {
            mToolbar = Preconditions.checkNotNull(toolbar);
            mOriginalCallback = originalCallback;
            mPreferMe = preferMe;
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            if (actionMode.getType() == ActionMode.TYPE_FLOATING) {
                if (mPreferMe) {
                    // Don't start the original action mode if this action mode should be preferred.
                    return false;
                }

                // Dismiss the toolbar if the textView starts a floating action mode.
                // NOTE that TextView by default starts a selection/insertion action mode if no
                // custom callback is set.
                if (mOriginalCallback == null
                        || mOriginalCallback.onCreateActionMode(actionMode, menu)) {
                    logv("ActionModeCallback: Dismissing toolbar. hasCallback="
                            + (mOriginalCallback != null));
                    dismissImmediately(mToolbar);
                    return true;
                }
                return false;
            }
            return mOriginalCallback.onCreateActionMode(actionMode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            // If another toolbar is showing, this toolbar should not be showing.
            mToolbar.dismiss();

            return mOriginalCallback == null
                    || mOriginalCallback.onPrepareActionMode(actionMode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            return mOriginalCallback != null
                    && mOriginalCallback.onActionItemClicked(actionMode, menuItem);
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            if (mOriginalCallback != null) {
                mOriginalCallback.onDestroyActionMode(actionMode);
            }
        }

        @Override
        public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
            if (mOriginalCallback instanceof ActionMode.Callback2) {
                ((ActionMode.Callback2) mOriginalCallback).onGetContentRect(mode, view, outRect);
            }
        }
    }

    private static final class OnToolbarDismissListener implements PopupWindow.OnDismissListener {

        private final TextView mTextView;
        private final ViewTreeObserver mObserver;
        private final TextViewListener mTextViewListener;
        private final ActionModeCallback mSelectionCallback;
        private final ActionModeCallback mInsertionCallback;

        OnToolbarDismissListener(
                TextView textView,
                TextViewListener textViewListener,
                ActionModeCallback selectionCallback,
                ActionModeCallback insertionCallback) {
            mTextView = Preconditions.checkNotNull(textView);
            mObserver = mTextView.getViewTreeObserver();
            mTextViewListener = Preconditions.checkNotNull(textViewListener);
            registerListeners();
            mSelectionCallback = Preconditions.checkNotNull(selectionCallback);
            mInsertionCallback = Preconditions.checkNotNull(insertionCallback);
            setCallbacks();
        }

        private void registerListeners() {
            mObserver.addOnPreDrawListener(mTextViewListener);
            mObserver.addOnWindowFocusChangeListener(mTextViewListener);
            mObserver.addOnGlobalFocusChangeListener(mTextViewListener);
            mObserver.addOnWindowAttachListener(mTextViewListener);
        }

        private void unregisterListeners() {
            mObserver.removeOnPreDrawListener(mTextViewListener);
            mObserver.removeOnWindowFocusChangeListener(mTextViewListener);
            mObserver.removeOnGlobalFocusChangeListener(mTextViewListener);
            mObserver.removeOnWindowAttachListener(mTextViewListener);
        }

        private void setCallbacks() {
            mTextView.setCustomSelectionActionModeCallback(mSelectionCallback);
            mTextView.setCustomInsertionActionModeCallback(mInsertionCallback);
        }

        private void clearCallbacks() {
            if (mSelectionCallback == mTextView.getCustomSelectionActionModeCallback()) {
                mTextView.setCustomSelectionActionModeCallback(
                        mSelectionCallback.mOriginalCallback);
            }
            if (mInsertionCallback == mTextView.getCustomInsertionActionModeCallback()) {
                mTextView.setCustomInsertionActionModeCallback(
                        mInsertionCallback.mOriginalCallback);
            }
        }

        @Override
        public void onDismiss() {
            removeHighlight(mTextView);
            unregisterListeners();
            clearCallbacks();
        }
    }

    private static final class OnMenuItemClickListener implements MenuItem.OnMenuItemClickListener {

        private final FloatingToolbar mToolbar;

        OnMenuItemClickListener(FloatingToolbar toolbar) {
            mToolbar = Preconditions.checkNotNull(toolbar);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            final Menu menu = mToolbar.getMenu();
            if (menu != null) {
                return menu.performIdentifierAction(item.getItemId(), 0);
            }
            return false;
        }
    }

    /**
     * BackgroundColorSpan that is used to indicate the part of the text that is the subject of the
     * showing toolbar.
     */
    @VisibleForTesting
    static final class BackgroundSpan extends BackgroundColorSpan {

        private static final CharacterStyle NON_PARCELABLE_UNDERLYING = new CharacterStyle() {
            @Override
            public void updateDrawState(TextPaint textPaint) {}
        };

        BackgroundSpan(int color) {
            super(color);
        }

        @Override
        public CharacterStyle getUnderlying() {
            // Prevent this span from being parceled.
            return NON_PARCELABLE_UNDERLYING;
        }
    }
}
