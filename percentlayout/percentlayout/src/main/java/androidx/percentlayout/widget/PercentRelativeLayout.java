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

package androidx.percentlayout.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * Subclass of {@link android.widget.RelativeLayout} that supports percentage based dimensions and
 * margins.
 *
 * You can specify dimension or a margin of child by using attributes with "Percent" suffix. Follow
 * this example:
 *
 * <pre class="prettyprint">
 * &lt;androidx.percentlayout.widget.PercentRelativeLayout
 *         xmlns:android="http://schemas.android.com/apk/res/android"
 *         xmlns:app="http://schemas.android.com/apk/res-auto"
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent"&gt
 *     &lt;ImageView
 *         app:layout_widthPercent="50%"
 *         app:layout_heightPercent="50%"
 *         app:layout_marginTopPercent="25%"
 *         app:layout_marginLeftPercent="25%"/&gt
 * &lt;/androidx.percentlayout.widget.PercentRelativeLayout&gt
 * </pre>
 *
 * The attributes that you can use are:
 * <ul>
 *     <li>{@code layout_widthPercent}
 *     <li>{@code layout_heightPercent}
 *     <li>{@code layout_marginPercent}
 *     <li>{@code layout_marginLeftPercent}
 *     <li>{@code layout_marginTopPercent}
 *     <li>{@code layout_marginRightPercent}
 *     <li>{@code layout_marginBottomPercent}
 *     <li>{@code layout_marginStartPercent}
 *     <li>{@code layout_marginEndPercent}
 *     <li>{@code layout_aspectRatio}
 * </ul>
 *
 * It is not necessary to specify {@code layout_width/height} if you specify {@code
 * layout_widthPercent.} However, if you want the view to be able to take up more space than what
 * percentage value permits, you can add {@code layout_width/height="wrap_content"}. In that case
 * if the percentage size is too small for the View's content, it will be resized using
 * {@code wrap_content} rule.
 *
 * <p>
 * You can also make one dimension be a fraction of the other by setting only width or height and
 * using {@code layout_aspectRatio} for the second one to be calculated automatically. For
 * example, if you would like to achieve 16:9 aspect ratio, you can write:
 * <pre class="prettyprint">
 *     android:layout_width="300dp"
 *     app:layout_aspectRatio="178%"
 * </pre>
 * This will make the aspect ratio 16:9 (1.78:1) with the width fixed at 300dp and height adjusted
 * accordingly.
 *
 * @deprecated consider using ConstraintLayout and associated layouts instead. The following shows
 * how to replicate the functionality of percentage layouts with a ConstraintLayout. The Guidelines
 * are used to define each percentage break point, and then a Button view is stretched to fill
 * the gap:
 *
 * <pre class="prettyprint">
 * &lt;androidx.constraintlayout.widget.ConstraintLayout
 *         xmlns:android="http://schemas.android.com/apk/res/android"
 *         xmlns:app="http://schemas.android.com/apk/res-auto"
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent"&gt
 *
 *     &lt;androidx.constraintlayout.widget.Guideline
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:id="@+id/left_guideline"
 *         app:layout_constraintGuide_percent=".15"
 *         android:orientation="vertical"/&gt
 *
 *     &lt;androidx.constraintlayout.widget.Guideline
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:id="@+id/right_guideline"
 *         app:layout_constraintGuide_percent=".85"
 *         android:orientation="vertical"/&gt
 *
 *     &lt;androidx.constraintlayout.widget.Guideline
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:id="@+id/top_guideline"
 *         app:layout_constraintGuide_percent=".15"
 *         android:orientation="horizontal"/&gt
 *
 *     &lt;androidx.constraintlayout.widget.Guideline
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:id="@+id/bottom_guideline"
 *         app:layout_constraintGuide_percent=".85"
 *         android:orientation="horizontal"/&gt
 *
 *     &lt;Button
 *         android:text="Button"
 *         android:layout_width="0dp"
 *         android:layout_height="0dp"
 *         android:id="@+id/button"
 *         app:layout_constraintLeft_toLeftOf="@+id/left_guideline"
 *         app:layout_constraintRight_toRightOf="@+id/right_guideline"
 *         app:layout_constraintTop_toTopOf="@+id/top_guideline"
 *         app:layout_constraintBottom_toBottomOf="@+id/bottom_guideline" /&gt
 *
 * &lt;/androidx.constraintlayout.widget.ConstraintLayout&gt
 */
@Deprecated
public class PercentRelativeLayout extends RelativeLayout {
    private final PercentLayoutHelper mHelper = new PercentLayoutHelper(this);

    public PercentRelativeLayout(Context context) {
        super(context);
    }

    public PercentRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PercentRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mHelper.adjustChildren(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mHelper.handleMeasuredStateTooSmall()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mHelper.restoreOriginalParams();
    }

    /**
     * @deprecated this class is deprecated along with its parent class.
     */
    @Deprecated
    public static class LayoutParams extends RelativeLayout.LayoutParams
            implements PercentLayoutHelper.PercentLayoutParams {
        private PercentLayoutHelper.PercentLayoutInfo mPercentLayoutInfo;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            mPercentLayoutInfo = PercentLayoutHelper.getPercentLayoutInfo(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        @Override
        public PercentLayoutHelper.PercentLayoutInfo getPercentLayoutInfo() {
            if (mPercentLayoutInfo == null) {
                mPercentLayoutInfo = new PercentLayoutHelper.PercentLayoutInfo();
            }

            return mPercentLayoutInfo;
        }

        @Override
        protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
            PercentLayoutHelper.fetchWidthAndHeight(this, a, widthAttr, heightAttr);
        }
    }
}
