/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.view.insets;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.StyleRes;
import androidx.core.R;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A layout drawing views under system bars, called {@link Protection}s, to ensure enough contrast
 * between the system bars (such as status bar and navigation bar) and the app content.
 *
 * <p>
 * This layout is meant to be used in
 * <a href="{@docRoot}develop/ui/views/layout/edge-to-edge">edge-to-edge</a> scenarios. The
 * {@link Protection}s are set using the {@link #setProtections(List)} method or directly in the
 * view constructor.
 *
 * <p>If this view will be attached to a hierarchy owned by a {@link android.view.Window Window}, it
 * is strongly recommended to call {@link androidx.core.view.WindowCompat#enableEdgeToEdge(Window)}
 * to make sure the view can reach the edges of the window and the framework color views are gone.
 */
public class ProtectionLayout extends FrameLayout {

    private static final Object PROTECTION_VIEW = new Object();
    private final List<Protection> mProtections = new ArrayList<>();
    private ProtectionGroup mGroup;

    public ProtectionLayout(@NonNull Context context) {
        super(context);
    }

    public ProtectionLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProtectionLayout(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ProtectionLayout(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Constructs a view which draws protections contained in the specified list.
     *
     * @param context the Context the view is running in, through which it can access the current
     *                theme, resources, etc.
     * @param protections a list of protections associated with a local area.
     */
    public ProtectionLayout(@NonNull Context context, @NonNull List<Protection> protections) {
        super(context);
        setProtections(protections);
    }

    /**
     * Replaces existing {@link Protection}s with the given ones. Calling this with an empty list
     * will remove all the protections.
     *
     * @param protections a list of protections associated with a local area.
     */
    public void setProtections(@NonNull List<Protection> protections) {
        mProtections.clear();
        mProtections.addAll(protections);
        if (isAttachedToWindow()) {
            addProtectionViews();
            requestApplyInsets();
        }
    }

    @NonNull
    private SystemBarStateMonitor getOrInstallSystemBarStateMonitor() {
        final ViewGroup rootView = (ViewGroup) getRootView();
        final Object tag = rootView.getTag(R.id.tag_system_bar_state_monitor);
        if (tag instanceof SystemBarStateMonitor) {
            return (SystemBarStateMonitor) tag;
        }
        final SystemBarStateMonitor monitor = new SystemBarStateMonitor(rootView);
        rootView.setTag(R.id.tag_system_bar_state_monitor, monitor);
        return monitor;
    }

    private void maybeUninstallSystemBarStateMonitor() {
        final ViewGroup rootView = (ViewGroup) getRootView();
        final Object tag = rootView.getTag(R.id.tag_system_bar_state_monitor);
        if (!(tag instanceof SystemBarStateMonitor)) {
            // The monitor hasn't been installed.
            return;
        }
        final SystemBarStateMonitor monitor = (SystemBarStateMonitor) tag;
        if (monitor.hasCallback()) {
            // Don't uninstall the monitor because other ProtectionLayout still needs it.
            return;
        }
        monitor.detachFromWindow();
        rootView.setTag(R.id.tag_system_bar_state_monitor, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        addProtectionViews();
        requestApplyInsets();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeProtectionViews();
        maybeUninstallSystemBarStateMonitor();
    }

    private void addProtectionViews() {
        if (mProtections.isEmpty()) {
            removeProtectionViews();
            return;
        }
        final SystemBarStateMonitor monitor = getOrInstallSystemBarStateMonitor();
        removeProtectionViews();
        mGroup = new ProtectionGroup(monitor, mProtections);
        final int nonProtectionChildCount = getChildCount();
        for (int i = 0, size = mGroup.size(); i < size; i++) {
            final Protection protection = mGroup.getProtection(i);
            // Add protections on top of any existing child views.
            addProtectionView(getContext(), i + nonProtectionChildCount, protection);
        }
    }

    private void removeProtectionViews() {
        if (mGroup != null) {
            removeViews(getChildCount() - mGroup.size(), mGroup.size());
            for (int i = 0, size = mGroup.size(); i < size; i++) {
                mGroup.getProtection(i).getAttributes().setCallback(null);
            }
            mGroup.dispose();
            mGroup = null;
        }
    }

    private void addProtectionView(Context context, int index, Protection protection) {
        final int width;
        final int height;
        final int gravity;
        final Protection.Attributes attrs = protection.getAttributes();
        switch (protection.getSide()) {
            case WindowInsetsCompat.Side.LEFT:
                width = attrs.getWidth();
                height = MATCH_PARENT;
                gravity = Gravity.LEFT;
                break;
            case WindowInsetsCompat.Side.TOP:
                width = MATCH_PARENT;
                height = attrs.getHeight();
                gravity = Gravity.TOP;
                break;
            case WindowInsetsCompat.Side.RIGHT:
                width = attrs.getWidth();
                height = MATCH_PARENT;
                gravity = Gravity.RIGHT;
                break;
            case WindowInsetsCompat.Side.BOTTOM:
                width = MATCH_PARENT;
                height = attrs.getHeight();
                gravity = Gravity.BOTTOM;
                break;
            default:
                throw new IllegalArgumentException("Unexpected side: " + protection.getSide());
        }

        final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                width, height, gravity);
        final Insets margin = attrs.getMargin();
        params.leftMargin = margin.left;
        params.topMargin = margin.top;
        params.rightMargin = margin.right;
        params.bottomMargin = margin.bottom;
        final View view = new View(context);
        view.setTag(PROTECTION_VIEW);
        view.setTranslationX(attrs.getTranslationX());
        view.setTranslationY(attrs.getTranslationY());
        view.setAlpha(attrs.getAlpha());
        view.setVisibility(attrs.isVisible() ? View.VISIBLE : View.GONE);
        view.setBackground(attrs.getDrawable());
        final Protection.Attributes.Callback callback =
                new Protection.Attributes.Callback() {

                    @Override
                    public void onWidthChanged(int width) {
                        params.width = width;
                        view.setLayoutParams(params);
                    }

                    @Override
                    public void onHeightChanged(int height) {
                        params.height = height;
                        view.setLayoutParams(params);
                    }

                    @Override
                    public void onMarginChanged(@NonNull Insets margin) {
                        params.leftMargin = margin.left;
                        params.topMargin = margin.top;
                        params.rightMargin = margin.right;
                        params.bottomMargin = margin.bottom;
                        view.setLayoutParams(params);
                    }

                    @Override
                    public void onVisibilityChanged(boolean visible) {
                        view.setVisibility(visible ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onDrawableChanged(@NonNull Drawable drawable) {
                        view.setBackground(drawable);
                    }

                    @Override
                    public void onTranslationXChanged(float translationX) {
                        view.setTranslationX(translationX);
                    }

                    @Override
                    public void onTranslationYChanged(float translationY) {
                        view.setTranslationY(translationY);
                    }

                    @Override
                    public void onAlphaChanged(float alpha) {
                        view.setAlpha(alpha);
                    }
                };
        attrs.setCallback(callback);
        addView(view, index, params);
    }

    @Override
    public void addView(@Nullable View child, int index, ViewGroup.@Nullable LayoutParams params) {
        if (child != null && child.getTag() != PROTECTION_VIEW) {
            // Non-ProtectionView cannot be added on top of any ProtectionViews.
            final int protectionViewCount = mGroup != null ? mGroup.size() : 0;
            final int maxIndex = getChildCount() - protectionViewCount;
            if (index > maxIndex || index < 0) {
                index = maxIndex;
            }
        }
        super.addView(child, index, params);
    }
}
