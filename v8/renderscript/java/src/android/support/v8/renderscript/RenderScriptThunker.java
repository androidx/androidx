/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v8.renderscript;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Process;
import android.util.Log;
import android.view.Surface;



class RenderScriptThunker extends RenderScript {
    android.renderscript.RenderScript mN;

    void validate() {
        if (mN == null) {
            throw new RSInvalidStateException("Calling RS with no Context active.");
        }
    }

    public void setPriority(Priority p) {
        try {
            if (p == Priority.LOW) mN.setPriority(android.renderscript.RenderScript.Priority.LOW);
            if (p == Priority.NORMAL) mN.setPriority(android.renderscript.RenderScript.Priority.NORMAL);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    RenderScriptThunker(Context ctx) {
        super(ctx);
        isNative = true;
    }

    public static RenderScript create(Context ctx, int sdkVersion) {
        try {
            RenderScriptThunker rs = new RenderScriptThunker(ctx);
            rs.mN = android.renderscript.RenderScript.create(ctx, sdkVersion);
            return rs;
        }
        catch(android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void contextDump() {
        try {
            mN.contextDump();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void finish() {
        try {
            mN.finish();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void destroy() {
        try {
            mN.destroy();
            mN = null;
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }

    }

    public void setMessageHandler(RSMessageHandler msg) {
        mMessageCallback = msg;
        try {
            android.renderscript.RenderScript.RSMessageHandler handler =
                new android.renderscript.RenderScript.RSMessageHandler() {
                    public void run() {
                        mMessageCallback.mData = mData;
                        mMessageCallback.mID = mID;
                        mMessageCallback.mLength = mLength;
                        mMessageCallback.run();
                    }
                };
            mN.setMessageHandler(handler);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void setErrorHandler(RSErrorHandler msg) {
        mErrorCallback = msg;
        try {
            android.renderscript.RenderScript.RSErrorHandler handler =
                new android.renderscript.RenderScript.RSErrorHandler() {
                    public void run() {
                        mErrorCallback.mErrorMessage = mErrorMessage;
                        mErrorCallback.mErrorNum = mErrorNum;
                        mErrorCallback.run();
                    }
                };
            mN.setErrorHandler(handler);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }


    boolean equals(Object obj1, Object obj2) {
        if (obj2 instanceof android.support.v8.renderscript.BaseObj) {
            return ((android.renderscript.BaseObj)obj1).equals(((android.support.v8.renderscript.BaseObj)obj2).getNObj());
        }
        return false;
    }
}
