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

import android.graphics.ImageFormat;
import android.util.Log;
import java.util.HashMap;

class TypeThunker extends Type {
    android.renderscript.Type mN;

    android.renderscript.Type getNObj() {
        return mN;
    }

    static HashMap<android.renderscript.Type, Type> mMap = new HashMap();

    void internalCalc() {
        mDimX = mN.getX();
        mDimY = mN.getY();
        mDimZ = mN.getZ();
        mDimFaces = mN.hasFaces();
        mDimMipmaps = mN.hasMipmaps();
        mDimYuv = mN.getYuv();
        calcElementCount();
    }

    TypeThunker(RenderScript rs, android.renderscript.Type t) {
        super(0, rs);
        mN = t;
        try {
            internalCalc();
            mElement = new ElementThunker(rs, t.getElement());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }

        synchronized(mMap) {
            mMap.put(mN, this);
        }
    }

    static Type find(android.renderscript.Type nt) {
        return mMap.get(nt);
    }

    static Type create(RenderScript rs, Element e,
                       int dx, int dy, int dz, boolean dmip, boolean dfaces, int yuv) {
        ElementThunker et = (ElementThunker)e;
        RenderScriptThunker rst = (RenderScriptThunker)rs;
        try {
            android.renderscript.Type.Builder tb =
                new android.renderscript.Type.Builder(rst.mN, et.mN);
            if (dx > 0) tb.setX(dx);
            if (dy > 0) tb.setY(dy);
            if (dz > 0) tb.setZ(dz);
            if (dmip) tb.setMipmaps(dmip);
            if (dfaces) tb.setFaces(dfaces);
            if (yuv > 0) tb.setYuvFormat(yuv);
            android.renderscript.Type nt = tb.create();
            TypeThunker tt = new TypeThunker(rs, nt);
            tt.internalCalc();

            return tt;
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }
}
