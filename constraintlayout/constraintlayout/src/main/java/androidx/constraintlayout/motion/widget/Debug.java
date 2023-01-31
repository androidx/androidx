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

package androidx.constraintlayout.motion.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.nio.CharBuffer;

// @TODO: add description

/**
 * Utilities useful for debugging
 *
 */
@SuppressLint("LogConditional")
public class Debug {

    /**
     * This logs n elements in the stack
     *
     * @param tag
     * @param msg
     * @param n
     *
     */
    public static void logStack(String tag, String msg, int n) {
        StackTraceElement[] st = new Throwable().getStackTrace();
        String s = " ";
        n = Math.min(n, st.length - 1);
        for (int i = 1; i <= n; i++) {
            @SuppressWarnings("unused") StackTraceElement ste = st[i];
            String stack = ".(" + st[i].getFileName() + ":" + st[i].getLineNumber()
                    + ") " + st[i].getMethodName();
            s += " ";
            Log.v(tag, msg + s + stack + s);
        }
    }

    /**
     * This logs n elements in the stack
     *
     * @param msg
     * @param n
     *
     */
    public static void printStack(String msg, int n) {
        StackTraceElement[] st = new Throwable().getStackTrace();
        String s = " ";
        n = Math.min(n, st.length - 1);
        for (int i = 1; i <= n; i++) {
            @SuppressWarnings("unused") StackTraceElement ste = st[i];
            String stack = ".(" + st[i].getFileName() + ":" + st[i].getLineNumber() + ") ";
            s += " ";
            System.out.println(msg + s + stack + s);
        }
    }

    /**
     * This provides return the name of a view
     *
     * @param view
     * @return name of view
     *
     */
    public static String getName(View view) {
        try {
            Context context = view.getContext();
            return context.getResources().getResourceEntryName(view.getId());
        } catch (Exception ex) {
            return "UNKNOWN";
        }
    }

    // @TODO: add description

    /**
     * @param obj
     */
    public static void dumpPoc(Object obj) {
        StackTraceElement s = new Throwable().getStackTrace()[1];
        String loc = ".(" + s.getFileName() + ":" + s.getLineNumber() + ")";
        Class c = obj.getClass();
        System.out.println(loc + "------------- " + c.getName() + " --------------------");
        Field[] declaredFields = c.getFields();
        for (int i = 0; i < declaredFields.length; i++) {
            Field declaredField = declaredFields[i];

            try {
                Object value = declaredField.get(obj);
                if (!declaredField.getName().startsWith("layout_constraint")) {
                    continue;
                }
                if (value instanceof Integer && value.toString().equals("-1")) {
                    continue;
                }
                if (value instanceof Integer && value.toString().equals("0")) {
                    continue;
                }
                if (value instanceof Float && value.toString().equals("1.0")) {
                    continue;
                }
                if (value instanceof Float && value.toString().equals("0.5")) {
                    continue;
                }
                System.out.println(loc + "    " + declaredField.getName() + " " + value);
            } catch (IllegalAccessException e) {

            }

        }
        System.out.println(loc + "------------- " + c.getSimpleName() + " --------------------");

    }

    /**
     * This returns the name of a view given its id
     *
     * @param context
     * @param id
     * @return name of view
     *
     */
    public static String getName(Context context, int id) {
        try {
            if (id != -1) {
                return context.getResources().getResourceEntryName(id);
            } else {
                return "UNKNOWN";
            }
        } catch (Exception ex) {
            return "?" + id;
        }
    }

    /**
     * This returns the name of a view given its id
     *
     * @param context
     * @param id
     * @return name of view
     *
     */
    public static String getName(Context context, int[] id) {
        try {
            String str = id.length + "[";
            for (int i = 0; i < id.length; i++) {
                str += (i == 0) ? "" : " ";
                String tmp = null;
                try {
                    tmp = context.getResources().getResourceEntryName(id[i]);
                } catch (Resources.NotFoundException e) {
                    tmp = "? " + id[i] + " ";
                }

                str += tmp;

            }
            return str + "]";
        } catch (Exception ex) {
            Log.v("DEBUG", ex.toString());
            return "UNKNOWN";
        }
    }

    /**
     * convert an id number to an id String useful in debugging
     *
     * @param layout
     * @param stateId
     * @return
     */
    public static String getState(MotionLayout layout, int stateId) {
        return getState(layout, stateId, -1);
    }

    /**
     * convert an id number to an id String useful in debugging
     *
     * @param layout
     * @param stateId
     * @param len     trim if string > len
     * @return
     */
    public static String getState(MotionLayout layout, int stateId, int len) {
        if (stateId == -1) {
            return "UNDEFINED";
        }
        Context context = layout.getContext();
        String str = context.getResources().getResourceEntryName(stateId);
        if (len != -1) {
            if (str.length() > len) {
                str = str.replaceAll("([^_])[aeiou]+", "$1"); // del vowels ! at start
            }
            if (str.length() > len) {
                int n = str.replaceAll("[^_]", "").length(); // count number of "_"
                if (n > 0) {
                    int extra = (str.length() - len) / n;
                    String reg = CharBuffer.allocate(extra).toString().replace('\0', '.') + "_";
                    str = str.replaceAll(reg, "_");
                }
            }
        }
        return str;
    }

    /**
     * Convert a motion event action to a string
     *
     * @param event
     * @return
     */
    public static String getActionType(MotionEvent event) {
        int type = event.getAction();
        Field[] fields = MotionEvent.class.getFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            try {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        && field.getType().equals(Integer.TYPE)
                        && field.getInt(null) == type) {
                    return field.getName();
                }
            } catch (IllegalAccessException e) {
            }
        }
        return "---";
    }

    /**
     * Get file name and location where this method is called.
     * Formatting it such that it is clickable by Intellij
     *
     * @return (filename : line_no)
     */
    public static String getLocation() {
        StackTraceElement s = new Throwable().getStackTrace()[1];
        return ".(" + s.getFileName() + ":" + s.getLineNumber() + ")";
    }

    /**
     * Get file name and location where this method is called.
     * Formatting it such that it is clickable by Intellij
     *
     * @return (filename : line_no)
     */
    public static String getLoc() {
        StackTraceElement s = new Throwable().getStackTrace()[1];
        return ".(" + s.getFileName() + ":" + s.getLineNumber() + ") " + s.getMethodName() + "()";
    }

    /**
     * Get file name and location where this method is called.
     * Formatting it such that it is clickable by Intellij
     *
     * @return (filename : line_no)
     */
    public static String getLocation2() {
        StackTraceElement s = new Throwable().getStackTrace()[2];
        return ".(" + s.getFileName() + ":" + s.getLineNumber() + ")";
    }

    /**
     * Get file name and location where this method is called.
     * Formatting it such that it is clickable by Intellij
     *
     * @return (filename : line_no)
     */
    public static String getCallFrom(int n) {
        StackTraceElement s = new Throwable().getStackTrace()[2 + n];
        return ".(" + s.getFileName() + ":" + s.getLineNumber() + ")";
    }

    // @TODO: add description

    /**
     *
     * @param layout
     * @param str
     */
    public static void dumpLayoutParams(ViewGroup layout, String str) {
        StackTraceElement s = new Throwable().getStackTrace()[1];
        String loc = ".(" + s.getFileName() + ":" + s.getLineNumber() + ") " + str + "  ";
        int n = layout.getChildCount();
        System.out.println(str + " children " + n);
        for (int i = 0; i < n; i++) {
            View v = layout.getChildAt(i);
            System.out.println(loc + "     " + getName(v));
            ViewGroup.LayoutParams param = v.getLayoutParams();
            Field[] declaredFields = param.getClass().getFields();
            for (int k = 0; k < declaredFields.length; k++) {
                Field declaredField = declaredFields[k];

                try {
                    Object value = declaredField.get(param);
                    String name = declaredField.getName();
                    if (!name.contains("To")) {
                        continue;
                    }
                    if (value.toString().equals("-1")) {
                        continue;
                    }

                    System.out.println(loc + "       " + declaredField.getName() + " " + value);
                } catch (IllegalAccessException e) {

                }

            }

        }
    }

    // @TODO: add description

    /**
     *
     * @param param
     * @param str
     */
    public static void dumpLayoutParams(ViewGroup.LayoutParams param, String str) {
        StackTraceElement s = new Throwable().getStackTrace()[1];
        String loc = ".(" + s.getFileName() + ":" + s.getLineNumber() + ") " + str + "  ";
        System.out.println(" >>>>>>>>>>>>>>>>>>. dump " + loc + "  " + param.getClass().getName());

        Field[] declaredFields = param.getClass().getFields();
        for (int k = 0; k < declaredFields.length; k++) {
            Field declaredField = declaredFields[k];

            try {
                Object value = declaredField.get(param);
                String name = declaredField.getName();
                if (!name.contains("To")) {
                    continue;
                }
                if (value.toString().equals("-1")) {
                    continue;
                }
//                    if (value instanceof  Integer && value.toString().equals("-1")) {
//                        continue;
//                    }

                System.out.println(loc + "       " + name + " " + value);
            } catch (IllegalAccessException e) {

            }

        }
        System.out.println(" <<<<<<<<<<<<<<<<< dump " + loc);

    }
}
