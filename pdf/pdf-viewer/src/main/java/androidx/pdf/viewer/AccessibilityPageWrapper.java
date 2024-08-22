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

package androidx.pdf.viewer;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.LinkRects;

import java.util.List;

/**
 * Container to hold a {@link PageMosaicView}, a {@link PageLinksView} and sometimes a {@link
 * FormAccessibilityView}.
 *
 * <p>{@link PageLinksView} and {@link FormAccessibilityView} are parents for accessible virtual
 * views, and are always on top of {@link PageMosaicView}. When present, {@link
 * FormAccessibilityView} is on top of {@link PageLinksView}. This means all page link views will
 * appear before form widget views when user scrolls through virtual views using left/right swipes.
 * If a link virtual view and a form widget virtual view overlap, the form widget view will receive
 * focus on direct tap.
 *
 * <p>{@link FormAccessibilityView} is only present when the document is a fillable form.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AccessibilityPageWrapper extends FrameLayout implements PageViewFactory.PageView {

    private final PageMosaicView mPageView;
    private final PageLinksView mPageLinksView;
    private final int mPageNum;

    public AccessibilityPageWrapper(
            @NonNull Context context,
            int pageNum,
            @NonNull PageMosaicView pageView,
            @Nullable PageLinksView pageLinksView) {
        super(context);
        this.mPageNum = pageNum;
        this.mPageView = pageView;
        this.mPageLinksView = pageLinksView;
        this.addView(pageView, 0);
        this.addView(pageLinksView, 1);
    }

    @Override
    public void clearAll() {
        mPageView.clearAll();
        mPageLinksView.clearAll();
    }

    @NonNull
    @Override
    public PageMosaicView getPageView() {
        return mPageView;
    }

    @Override
    public int getPageNum() {
        return mPageNum;
    }

    @Override
    public void setPageUrlLinks(@Nullable LinkRects links) {
        mPageView.setPageUrlLinks(links);
        mPageLinksView.setPageUrlLinks(links);
    }

    @Override
    public void setPageGotoLinks(@Nullable List<GotoLink> links) {
        mPageView.setPageGotoLinks(links);
        mPageLinksView.setPageGotoLinks(links);
    }

    @NonNull
    @Override
    public View asView() {
        return this;
    }
}
