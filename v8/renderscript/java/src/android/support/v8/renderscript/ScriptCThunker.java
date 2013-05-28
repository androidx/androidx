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
import android.content.res.Resources;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.HashMap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 *
 **/
class ScriptCThunker extends android.renderscript.ScriptC {
    private static final String TAG = "ScriptC";

    protected ScriptCThunker(RenderScriptThunker rs, Resources resources, int resourceID) {
        super(rs.mN, resources, resourceID);
    }

    android.renderscript.Script.KernelID thunkCreateKernelID(
            int slot, int sig, Element ein, Element eout) {

        android.renderscript.Element nein = null;
        android.renderscript.Element neout = null;
        if (ein != null) {
            nein = ((ElementThunker)ein).mN;
        }
        if (eout != null) {
            neout = ((ElementThunker)eout).mN;
        }
        return createKernelID(slot, sig, nein, neout);
    }


    void thunkInvoke(int slot) {
        invoke(slot);
    }

    void thunkBindAllocation(Allocation va, int slot) {
        android.renderscript.Allocation nva = null;
        if (va != null) {
            nva = ((AllocationThunker)va).mN;
        }
        bindAllocation(nva, slot);
    }

    void thunkSetTimeZone(String timeZone) {
        setTimeZone(timeZone);
    }

    void thunkInvoke(int slot, FieldPacker v) {
        android.renderscript.FieldPacker nfp =
                new android.renderscript.FieldPacker(v.getData());
        invoke(slot, nfp);
    }

    void thunkForEach(int slot, Allocation ain, Allocation aout, FieldPacker v) {
        android.renderscript.Allocation nin = null;
        android.renderscript.Allocation nout = null;
        android.renderscript.FieldPacker nfp = null;
        if (ain != null) {
            nin = ((AllocationThunker)ain).mN;
        }
        if (aout != null) {
            nout = ((AllocationThunker)aout).mN;
        }
        if (v != null) {
            nfp = new android.renderscript.FieldPacker(v.getData());
        }
        forEach(slot, nin, nout, nfp);
    }

    void thunkForEach(int slot, Allocation ain, Allocation aout, FieldPacker v,
                      android.support.v8.renderscript.Script.LaunchOptions sc) {
        android.renderscript.Script.LaunchOptions lo = null;
        if (sc != null) {
            lo = new android.renderscript.Script.LaunchOptions();
            if (sc.getXEnd() > 0) lo.setX(sc.getXStart(), sc.getXEnd());
            if (sc.getYEnd() > 0) lo.setY(sc.getYStart(), sc.getYEnd());
            if (sc.getZEnd() > 0) lo.setZ(sc.getZStart(), sc.getZEnd());
        }

        android.renderscript.Allocation nin = null;
        android.renderscript.Allocation nout = null;
        android.renderscript.FieldPacker nfp = null;
        if (ain != null) {
            nin = ((AllocationThunker)ain).mN;
        }
        if (aout != null) {
            nout = ((AllocationThunker)aout).mN;
        }
        if (v != null) {
            nfp = new android.renderscript.FieldPacker(v.getData());
        }
        forEach(slot, nin, nout, nfp, lo);
    }

    void thunkSetVar(int index, float v) {
        setVar(index, v);
    }
    void thunkSetVar(int index, double v) {
        setVar(index, v);
    }
    void thunkSetVar(int index, int v) {
        setVar(index, v);
    }
    void thunkSetVar(int index, long v) {
        setVar(index, v);
    }
    void thunkSetVar(int index, boolean v) {
        setVar(index, v);
    }

    void thunkSetVar(int index, BaseObj o) {
        if (o == null) {
            setVar(index, 0);
            return;
        }
        setVar(index, o.getNObj());
    }
    void thunkSetVar(int index, FieldPacker v) {
        android.renderscript.FieldPacker nfp =
                new android.renderscript.FieldPacker(v.getData());
        setVar(index, nfp);
    }

    void thunkSetVar(int index, FieldPacker v, Element e, int[] dims) {
        android.renderscript.FieldPacker nfp =
                new android.renderscript.FieldPacker(v.getData());
        ElementThunker et = (ElementThunker)e;
        setVar(index, nfp, et.mN, dims);
    }

    android.renderscript.Script.FieldID thunkCreateFieldID(int slot, Element e) {
        ElementThunker et = (ElementThunker) e;
        return createFieldID(slot, et.getNObj());
    }

}
