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
package androidx.recyclerview.widget;

import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

abstract public class BaseWrapContentWithAspectRatioTest extends BaseRecyclerViewInstrumentationTest {
    final BaseWrapContentTest.WrapContentConfig mWrapContentConfig;

    protected BaseWrapContentWithAspectRatioTest(
            BaseWrapContentTest.WrapContentConfig wrapContentConfig) {
        mWrapContentConfig = wrapContentConfig;
    }

    int getSize(View view, int orientation) {
        if (orientation == VERTICAL) {
            return view.getHeight();
        }
        return view.getWidth();
    }

    static class LoggingView extends View {

        MeasureBehavior mBehavior;

        public void setBehavior(MeasureBehavior behavior) {
            mBehavior = behavior;
        }

        public LoggingView(Context context) {
            super(context);
        }

        public LoggingView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public LoggingView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        public LoggingView(Context context, AttributeSet attrs, int defStyleAttr,
                int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            mBehavior.onMeasure(this, widthMeasureSpec, heightMeasureSpec);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            mBehavior.onLayout(changed, left, top, right, bottom);
        }

        public void setMeasured(int w, int h) {
            setMeasuredDimension(w, h);
        }

        public void prepareLayoutParams() {
            mBehavior.setLayoutParams(this);
        }
    }

    static class AspectRatioMeasureBehavior extends MeasureBehavior {

        Float ratio;
        int control;

        public AspectRatioMeasureBehavior(int desiredW, int desiredH, int wMode, int hMode) {
            super(desiredW, desiredH, wMode, hMode);
        }

        public AspectRatioMeasureBehavior aspectRatio(int control, float ratio) {
            this.control = control;
            this.ratio = ratio;
            return this;
        }

        @Override
        public void onMeasure(LoggingView view, int wSpec,
                int hSpec) {
            super.onMeasure(view, wSpec, hSpec);
            if (control == VERTICAL) {
                view.setMeasured(getSecondary(view.getMeasuredHeight()),
                        view.getMeasuredHeight());
            } else if (control == HORIZONTAL) {
                view.setMeasured(view.getMeasuredWidth(),
                        getSecondary(view.getMeasuredWidth()));
            }
        }

        public int getSecondary(int controlSize) {
            return (int) (controlSize * ratio);
        }
    }

    static class MeasureBehavior {
        private static final AtomicLong idCounter = new AtomicLong(0);
        public List<Pair<Integer, Integer>> measureSpecs = new ArrayList<>();
        public List<Pair<Integer, Integer>> layouts = new ArrayList<>();
        int desiredW, desiredH;
        final long mId = idCounter.incrementAndGet();

        ViewGroup.MarginLayoutParams layoutParams;

        public MeasureBehavior(int desiredW, int desiredH, int wMode, int hMode) {
            this.desiredW = desiredW;
            this.desiredH = desiredH;
            layoutParams = new ViewGroup.MarginLayoutParams(
                    wMode, hMode
            );
        }

        public MeasureBehavior withMargins(int left, int top, int right, int bottom) {
            layoutParams.leftMargin = left;
            layoutParams.topMargin = top;
            layoutParams.rightMargin = right;
            layoutParams.bottomMargin = bottom;
            return this;
        }

        public long getId() {
            return mId;
        }

        public void onMeasure(LoggingView view, int wSpec, int hSpec) {
            measureSpecs.add(new Pair<>(wSpec, hSpec));
            view.setMeasured(
                    RecyclerView.LayoutManager.chooseSize(wSpec, desiredW, 0),
                    RecyclerView.LayoutManager.chooseSize(hSpec, desiredH, 0));
        }

        public int getSpec(int position, int orientation) {
            if (orientation == VERTICAL) {
                return measureSpecs.get(position).second;
            } else {
                return measureSpecs.get(position).first;
            }
        }

        public void setLayoutParams(LoggingView view) {
            view.setLayoutParams(layoutParams);
        }

        public void onLayout(boolean changed, int left, int top, int right, int bottom) {
            if (changed) {
                layouts.add(new Pair<>(right - left, bottom - top));
            }
        }
    }


    static class WrapContentViewHolder extends RecyclerView.ViewHolder {

        LoggingView mView;

        public WrapContentViewHolder(ViewGroup parent) {
            super(new LoggingView(parent.getContext()));
            mView = (LoggingView) itemView;
            mView.setBackgroundColor(Color.GREEN);
        }
    }

    static class WrapContentAdapter extends RecyclerView.Adapter<WrapContentViewHolder> {

        List<MeasureBehavior> behaviors = new ArrayList<>();

        public WrapContentAdapter(MeasureBehavior... behaviors) {
            Collections.addAll(this.behaviors, behaviors);
        }

        @Override
        public WrapContentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new WrapContentViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull WrapContentViewHolder holder, int position) {
            holder.mView.setBehavior(behaviors.get(position));
            holder.mView.prepareLayoutParams();
        }

        @Override
        public int getItemCount() {
            return behaviors.size();
        }
    }

    static class MeasureSpecMatcher extends BaseMatcher<Integer> {

        private boolean checkSize = false;
        private boolean checkMode = false;
        private int mSize;
        private int mMode;

        public static MeasureSpecMatcher is(int size, int mode) {
            MeasureSpecMatcher matcher = new MeasureSpecMatcher(size, mode);
            matcher.checkSize = true;
            matcher.checkMode = true;
            return matcher;
        }

        public static MeasureSpecMatcher size(int size) {
            MeasureSpecMatcher matcher = new MeasureSpecMatcher(size, 0);
            matcher.checkSize = true;
            matcher.checkMode = false;
            return matcher;
        }

        public static MeasureSpecMatcher mode(int mode) {
            MeasureSpecMatcher matcher = new MeasureSpecMatcher(0, mode);
            matcher.checkSize = false;
            matcher.checkMode = true;
            return matcher;
        }

        private MeasureSpecMatcher(int size, int mode) {
            mSize = size;
            mMode = mode;

        }

        @Override
        public boolean matches(Object item) {
            if (item == null) {
                return false;
            }
            Integer intValue = (Integer) item;
            final int size = View.MeasureSpec.getSize(intValue);
            final int mode = View.MeasureSpec.getMode(intValue);
            if (checkSize && size != mSize) {
                return false;
            }
            if (checkMode && mode != mMode) {
                return false;
            }
            return true;
        }

        @Override
        public void describeMismatch(Object item, Description description) {
            Integer intValue = (Integer) item;
            final int size = View.MeasureSpec.getSize(intValue);
            final int mode = View.MeasureSpec.getMode(intValue);
            if (checkSize && size != mSize) {
                description.appendText(" Expected size was ").appendValue(mSize)
                        .appendText(" but received size is ").appendValue(size);
            }
            if (checkMode && mode != mMode) {
                description.appendText(" Expected mode was ").appendValue(modeName(mMode))
                        .appendText(" but received mode is ").appendValue(modeName(mode));
            }
        }

        @Override
        public void describeTo(Description description) {
            if (checkSize) {
                description.appendText(" Measure spec size:").appendValue(mSize);
            }
            if (checkMode) {
                description.appendText(" Measure spec mode:").appendValue(modeName(mMode));
            }
        }

        private static String modeName(int mode) {
            switch (mode) {
                case View.MeasureSpec.AT_MOST:
                    return "at most";
                case View.MeasureSpec.EXACTLY:
                    return "exactly";
                default:
                    return "unspecified";
            }
        }
    }
}
