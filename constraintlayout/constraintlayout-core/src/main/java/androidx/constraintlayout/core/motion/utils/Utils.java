/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.constraintlayout.core.motion.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class Utils {
    // @TODO: add description
    public static void log(String tag, String value) {
        System.out.println(tag + " : " + value);
    }

    // @TODO: add description
    public static void loge(String tag, String value) {
        System.err.println(tag + " : " + value);
    }

    // @TODO: add description
    public static void socketSend(String str) {
        try {
            Socket socket = new Socket("127.0.0.1", 5327);
            OutputStream out = socket.getOutputStream();
            out.write(str.getBytes());
            out.close();
        } catch (IOException e) {
            //TODO replace with something not equal to printStackTrace();
            System.err.println(e.toString()+"\n"+ Arrays.toString(e.getStackTrace())
                    .replace("[","   at ")
                    .replace(",","\n   at")
                    .replace("]",""));
        }
    }

    private static int clamp(int c) {
        int n = 255;
        c &= ~(c >> 31);
        c -= n;
        c &= (c >> 31);
        c += n;
        return c;
    }

    // @TODO: add description
    public int getInterpolatedColor(float[] value) {
        int r = clamp((int) ((float) Math.pow(value[0], 1.0 / 2.2) * 255.0f));
        int g = clamp((int) ((float) Math.pow(value[1], 1.0 / 2.2) * 255.0f));
        int b = clamp((int) ((float) Math.pow(value[2], 1.0 / 2.2) * 255.0f));
        int a = clamp((int) (value[3] * 255.0f));
        int color = (a << 24) | (r << 16) | (g << 8) | b;
        return color;
    }

    // @TODO: add description
    public static int rgbaTocColor(float r, float g, float b, float a) {
        int ir = clamp((int) (r * 255f));
        int ig = clamp((int) (g * 255f));
        int ib = clamp((int) (b * 255f));
        int ia = clamp((int) (a * 255f));
        int color = (ia << 24) | (ir << 16) | (ig << 8) | ib;
        return color;
    }

    public interface DebugHandle {
        // @TODO: add description
        void message(String str);
    }

    static DebugHandle sOurHandle;

    public static void setDebugHandle(DebugHandle handle) {
        sOurHandle = handle;
    }

    // @TODO: add description
    public static void logStack(String msg, int n) {
        StackTraceElement[] st = new Throwable().getStackTrace();
        String s = " ";
        n = Math.min(n, st.length - 1);
        for (int i = 1; i <= n; i++) {
            StackTraceElement ste = st[i];
            String stack = ".(" + ste.getFileName() + ":"
                    + ste.getLineNumber() + ") " + ste.getMethodName();
            s += " ";
            System.out.println(msg + s + stack + s);
        }
    }

    // @TODO: add description
    public static void log(String str) {
        StackTraceElement s = new Throwable().getStackTrace()[1];
        String methodName = s.getMethodName();
        methodName = (methodName + "                  ").substring(0, 17);
        String npad = "    ".substring(Integer.toString(s.getLineNumber()).length());
        String ss = ".(" + s.getFileName() + ":" + s.getLineNumber() + ")" + npad + methodName;
        System.out.println(ss + " " + str);
        if (sOurHandle != null) {
            sOurHandle.message(ss + " " + str);
        }
    }

}
