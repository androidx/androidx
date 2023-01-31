/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.widget;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.constraintlayout.core.Metrics;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * This provide metrics of the complexity of the layout that is being solved.
 * The intent is for developers using the too to track the evolution of their UI
 * Typically the developer will monitor the computations on every callback of
 * mConstraintLayout.addOnLayoutChangeListener(this::callback);
 *
 */
public class ConstraintLayoutStatistics {
    public final static int NUMBER_OF_LAYOUTS = 1;
    public final static int NUMBER_OF_ON_MEASURES= 2;
    public final static int NUMBER_OF_CHILD_VIEWS= 3;
    public final static int NUMBER_OF_CHILD_MEASURES= 4;
    public final static int DURATION_OF_CHILD_MEASURES= 5;
    public final static int DURATION_OF_MEASURES = 6;
    public final static int DURATION_OF_LAYOUT = 7;
    public final static int NUMBER_OF_VARIABLES = 8;
    public final static int NUMBER_OF_EQUATIONS = 9;
    public final static int NUMBER_OF_SIMPLE_EQUATIONS = 10;

    private final Metrics mMetrics = new Metrics();
    ConstraintLayout mConstraintLayout;
    private static int MAX_WORD = 25;
    private static final String WORD_PAD = new String(new char[MAX_WORD]).replace('\0', ' ');

    /**
     * Measure performance information about ConstraintLayout
     *
     * @param constraintLayout
     */
    public ConstraintLayoutStatistics(ConstraintLayout constraintLayout) {
        attach(constraintLayout);
    }

    /**
     * Copy a layout Stats useful for comparing two stats
     * @param copy
     */
    public ConstraintLayoutStatistics(ConstraintLayoutStatistics copy) {
        mMetrics.copy(copy.mMetrics);
    }


    /**
     * Attach to a ConstraintLayout to gather statistics on its layout performance
     * @param constraintLayout
     */
    public void attach(ConstraintLayout constraintLayout) {
        constraintLayout.fillMetrics(mMetrics);
        mConstraintLayout = constraintLayout;
    }

    /**
     * Detach from a ConstraintLayout
     */
    public void detach() {
        if (mConstraintLayout != null) {
            mConstraintLayout.fillMetrics(null);
        }
    }

    /**
     * Clear the current metrics
     */
    public void reset() {
        mMetrics.reset();
    }

    /**
     * Create a copy of the statistics
     * @return a copy
     */
    @Override
    public ConstraintLayoutStatistics clone() {
        return new ConstraintLayoutStatistics(this);
    }

    /**
     * Format a float value outputting a string of fixed length
     * @param df format to use
     * @param val
     * @param length
     * @return
     */
    private String fmt(DecimalFormat df, float val, int length) {
        String s = new String(new char[length]).replace('\0', ' ');
        s = (s + df.format(val));
        return s.substring(s.length() - length);
    }

    /**
     * Log a summary of the statistics
     * @param tag
     */
    public void logSummary(String tag) {
        log(tag);
    }

    @SuppressLint({"LogConditional"})
    private void log(String tag) {
        StackTraceElement s = new Throwable().getStackTrace()[2];

        Log.v(tag, "CL Perf: --------  Performance .(" +
                s.getFileName() + ":" + s.getLineNumber() + ")  ------ ");

        DecimalFormat df = new DecimalFormat("###.000");

        Log.v(tag, log(df, DURATION_OF_CHILD_MEASURES));
        Log.v(tag, log(df, DURATION_OF_LAYOUT));
        Log.v(tag, log(df,  DURATION_OF_MEASURES));
        Log.v(tag, log(NUMBER_OF_LAYOUTS));
        Log.v(tag, log( NUMBER_OF_ON_MEASURES));
        Log.v(tag, log(NUMBER_OF_CHILD_VIEWS));
        Log.v(tag, log(NUMBER_OF_CHILD_MEASURES));
        Log.v(tag, log(NUMBER_OF_VARIABLES));
        Log.v(tag, log(NUMBER_OF_EQUATIONS));
        Log.v(tag, log( NUMBER_OF_SIMPLE_EQUATIONS));
    }

    /**
     * Generate a formatted String for the parameter formatting as a float
     * @param df
     * @param param
     * @return
     */
    private String log(DecimalFormat df, int param) {
        String value = fmt(df, getValue(param) * 1E-6f, 7);

        String title = geName(param);
        title = WORD_PAD + title;
        title = title.substring(title.length() - MAX_WORD);
        title += " = ";
        return "CL Perf: " + title + value;
    }

    /**
     * Generate a formatted String for the parameter
     * @param param
     * @return
     */
    private String log(int param) {
        String value = Long.toString(this.getValue(param));
        String title = geName(param);
        title = WORD_PAD + title;
        title = title.substring(title.length() - MAX_WORD);
        title += " = ";
        return "CL Perf: " + title + value;
    }

    /**
     * Generate a float formatted String for the parameter comparing
     * current value with value in relative
     * @param df Format the float using this
     * @param relative compare against
     * @param param the parameter to compare
     * @return
     */
    private String compare(DecimalFormat df,  ConstraintLayoutStatistics relative, int param) {
        String value = fmt(df, getValue(param) * 1E-6f, 7);
        value += " -> " + fmt(df, relative.getValue(param) * 1E-6f, 7) + "ms";
        String title = geName(param);
        title = WORD_PAD + title;
        title = title.substring(title.length() - MAX_WORD);
        title += " = ";
        return "CL Perf: " + title + value;
    }

    /**
     * Generate a formatted String for the parameter comparing current value with value in relative
     * @param relative compare against
     * @param param the parameter to compare
     * @return
     */
    private String compare(ConstraintLayoutStatistics relative, int param) {
        String value = this.getValue(param) + " -> " + relative.getValue(param);
        String title = geName(param);

        title = WORD_PAD + title;
        title = title.substring(title.length() - MAX_WORD);
        title += " = ";
        return "CL Perf: " + title + value;
    }

    /**
     * log a summary of the stats compared to another statics
     * @param tag used in Log.v(tag, ...)
     * @param prev the previous stats to compare to
     */
    @SuppressLint("LogConditional")
    public void logSummary(String tag, ConstraintLayoutStatistics prev) {
        if (prev == null) {
            log(tag);
            return;
        }
        DecimalFormat df = new DecimalFormat("###.000");
        StackTraceElement s = new Throwable().getStackTrace()[1];

        Log.v(tag, "CL Perf: -=  Performance .(" +
                s.getFileName() + ":" + s.getLineNumber() + ")  =- ");
        Log.v(tag, compare(df, prev, DURATION_OF_CHILD_MEASURES));
        Log.v(tag, compare(df, prev, DURATION_OF_LAYOUT));
        Log.v(tag, compare(df, prev, DURATION_OF_MEASURES));
        Log.v(tag, compare(prev, NUMBER_OF_LAYOUTS));
        Log.v(tag, compare(prev, NUMBER_OF_ON_MEASURES));
        Log.v(tag, compare(prev, NUMBER_OF_CHILD_VIEWS));
        Log.v(tag, compare(prev, NUMBER_OF_CHILD_MEASURES));
        Log.v(tag, compare(prev, NUMBER_OF_VARIABLES));
        Log.v(tag, compare(prev, NUMBER_OF_EQUATIONS));
        Log.v(tag, compare(prev, NUMBER_OF_SIMPLE_EQUATIONS));
    }

    /**
     * get the value of a statistic
     * @param type
     * @return
     */
    public long getValue(int type) {
        switch (type) {
            case NUMBER_OF_LAYOUTS:
                return mMetrics.mNumberOfLayouts;
            case NUMBER_OF_ON_MEASURES:
                return mMetrics.mMeasureCalls;
            case NUMBER_OF_CHILD_VIEWS:
                return mMetrics.mChildCount;
            case NUMBER_OF_CHILD_MEASURES:
                return mMetrics.mNumberOfMeasures;
            case DURATION_OF_CHILD_MEASURES:
                return mMetrics.measuresWidgetsDuration ;
            case DURATION_OF_MEASURES:
                return mMetrics.mMeasureDuration;
            case DURATION_OF_LAYOUT:
                return mMetrics.measuresLayoutDuration;
            case  NUMBER_OF_VARIABLES:
                return mMetrics.mVariables;
            case  NUMBER_OF_EQUATIONS:
                return mMetrics.mEquations;
            case  NUMBER_OF_SIMPLE_EQUATIONS:
                return mMetrics.mSimpleEquations;
        }
        return 0;
    }

    /** get a simple name for a statistic
     *
     * @param type type of statistic
     * @return a camel case
     */
    String geName(int type) {
        switch (type) {
            case NUMBER_OF_LAYOUTS:
                return "NumberOfLayouts";
            case NUMBER_OF_ON_MEASURES:
                return "MeasureCalls";
            case NUMBER_OF_CHILD_VIEWS:
                return "ChildCount";
            case NUMBER_OF_CHILD_MEASURES:
                return "ChildrenMeasures";
            case DURATION_OF_CHILD_MEASURES:
                return "MeasuresWidgetsDuration ";
            case DURATION_OF_MEASURES:
                return "MeasureDuration";
            case DURATION_OF_LAYOUT:
                return "MeasuresLayoutDuration";
            case  NUMBER_OF_VARIABLES:
                return "SolverVariables";
            case  NUMBER_OF_EQUATIONS:
                return "SolverEquations";
            case  NUMBER_OF_SIMPLE_EQUATIONS:
                return  "SimpleEquations";
        }
        return "";
    }

}
