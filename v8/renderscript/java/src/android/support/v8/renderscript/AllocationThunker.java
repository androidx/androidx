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

import java.io.IOException;
import java.io.InputStream;
import android.content.res.Resources;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Surface;
import android.util.Log;
import android.util.TypedValue;

class AllocationThunker extends Allocation {
    android.renderscript.Allocation mN;
    //Allocation mAdaptedAllocation;

    android.renderscript.Allocation getNObj() {
        return mN;
    }

    static android.renderscript.Allocation.MipmapControl
        convertMipmapControl(MipmapControl mc) {

        switch(mc) {
        case MIPMAP_NONE:
            return android.renderscript.Allocation.MipmapControl.MIPMAP_NONE;
        case MIPMAP_FULL:
            return android.renderscript.Allocation.MipmapControl.MIPMAP_FULL;
        case MIPMAP_ON_SYNC_TO_TEXTURE:
            return android.renderscript.Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE;
        }
        return null;
    }

    public Type getType() {
        return TypeThunker.find(mN.getType());
    }

    public Element getElement() {
        return getType().getElement();
    }

    public int getUsage() {
        try {
            return mN.getUsage();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public int getBytesSize() {
        try {
            return mN.getBytesSize();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    AllocationThunker(RenderScript rs, Type t, int usage, android.renderscript.Allocation na) {
        super(0, rs, t, usage);

        mType = t;
        mUsage = usage;
        mN = na;
    }

    public void syncAll(int srcLocation) {
        try {
            mN.syncAll(srcLocation);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void ioSend() {
        try {
            mN.ioSend();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void ioReceive() {
        try {
            mN.ioReceive();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void copyFrom(BaseObj[] d) {
        if (d == null) {
            return;
        }
        android.renderscript.BaseObj[] dN = new android.renderscript.BaseObj[d.length];
        for (int i = 0; i < d.length; i++) {
            dN[i] = d[i].getNObj();
        }
        try {
            mN.copyFrom(dN);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void copyFromUnchecked(int[] d) {
        try {
            mN.copyFromUnchecked(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copyFromUnchecked(short[] d) {
        try {
            mN.copyFromUnchecked(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copyFromUnchecked(byte[] d) {
        try {
            mN.copyFromUnchecked(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copyFromUnchecked(float[] d) {
        try {
            mN.copyFromUnchecked(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void copyFrom(int[] d) {
        try {
            mN.copyFrom(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copyFrom(short[] d) {
        try {
            mN.copyFrom(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copyFrom(byte[] d) {
        try {
            mN.copyFrom(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copyFrom(float[] d) {
        try {
            mN.copyFrom(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copyFrom(Bitmap b) {
        try {
            mN.copyFrom(b);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copyFrom(Allocation a) {
        AllocationThunker at = (AllocationThunker)a;
        try {
            mN.copyFrom(at.mN);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }


    public void setFromFieldPacker(int xoff, FieldPacker fp) {
        try {
            android.renderscript.FieldPacker nfp =
                new android.renderscript.FieldPacker(fp.getData());
            mN.setFromFieldPacker(xoff, nfp);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void setFromFieldPacker(int xoff, int component_number, FieldPacker fp) {
        try {
            android.renderscript.FieldPacker nfp =
                new android.renderscript.FieldPacker(fp.getData());
            mN.setFromFieldPacker(xoff, component_number, nfp);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void generateMipmaps() {
        try {
            mN.generateMipmaps();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void copy1DRangeFromUnchecked(int off, int count, int[] d) {
        try {
            mN.copy1DRangeFromUnchecked(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copy1DRangeFromUnchecked(int off, int count, short[] d) {
        try {
            mN.copy1DRangeFromUnchecked(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copy1DRangeFromUnchecked(int off, int count, byte[] d) {
        try {
            mN.copy1DRangeFromUnchecked(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copy1DRangeFromUnchecked(int off, int count, float[] d) {
        try {
            mN.copy1DRangeFromUnchecked(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void copy1DRangeFrom(int off, int count, int[] d) {
        try {
            mN.copy1DRangeFrom(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copy1DRangeFrom(int off, int count, short[] d) {
        try {
            mN.copy1DRangeFrom(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copy1DRangeFrom(int off, int count, byte[] d) {
        try {
            mN.copy1DRangeFrom(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copy1DRangeFrom(int off, int count, float[] d) {
        try {
            mN.copy1DRangeFrom(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void copy1DRangeFrom(int off, int count, Allocation data, int dataOff) {
        try {
            AllocationThunker at = (AllocationThunker)data;
            mN.copy1DRangeFrom(off, count, at.mN, dataOff);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, byte[] data) {
        try {
            mN.copy2DRangeFrom(xoff, yoff, w, h, data);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, short[] data) {
        try {
            mN.copy2DRangeFrom(xoff, yoff, w, h, data);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, int[] data) {
        try {
            mN.copy2DRangeFrom(xoff, yoff, w, h, data);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, float[] data) {
        try {
            mN.copy2DRangeFrom(xoff, yoff, w, h, data);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void copy2DRangeFrom(int xoff, int yoff, int w, int h,
                                Allocation data, int dataXoff, int dataYoff) {
        try {
            AllocationThunker at = (AllocationThunker)data;
            mN.copy2DRangeFrom(xoff, yoff, w, h, at.mN, dataXoff, dataYoff);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copy2DRangeFrom(int xoff, int yoff, Bitmap data) {
        try {
            mN.copy2DRangeFrom(xoff, yoff, data);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }


    public void copyTo(Bitmap b) {
        try {
            mN.copyTo(b);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copyTo(byte[] d) {
        try {
            mN.copyTo(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copyTo(short[] d) {
        try {
            mN.copyTo(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copyTo(int[] d) {
        try {
            mN.copyTo(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
    public void copyTo(float[] d) {
        try {
            mN.copyTo(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    // creation

    static BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
    static {
        mBitmapOptions.inScaled = false;
    }

    static public Allocation createTyped(RenderScript rs, Type type, MipmapControl mips, int usage) {
        RenderScriptThunker rst = (RenderScriptThunker)rs;
        TypeThunker tt = (TypeThunker)type;

        try {
            android.renderscript.Allocation a =
                android.renderscript.Allocation.createTyped(rst.mN, tt.mN,
                                                            convertMipmapControl(mips),
                                                            usage);
            return new AllocationThunker(rs, type, usage, a);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    static public Allocation createFromBitmap(RenderScript rs, Bitmap b,
                                              MipmapControl mips,
                                              int usage) {

        RenderScriptThunker rst = (RenderScriptThunker)rs;
        try {
            android.renderscript.Allocation a =
                android.renderscript.Allocation.createFromBitmap(rst.mN, b,
                                                                 convertMipmapControl(mips),
                                                                 usage);
            TypeThunker tt = new TypeThunker(rs, a.getType());
            return new AllocationThunker(rs, tt, usage, a);

        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    static public Allocation createCubemapFromBitmap(RenderScript rs, Bitmap b,
                                                     MipmapControl mips,
                                                     int usage) {
        RenderScriptThunker rst = (RenderScriptThunker)rs;
        try {
            android.renderscript.Allocation a =
                    android.renderscript.Allocation.createCubemapFromBitmap(
                    rst.mN, b, convertMipmapControl(mips), usage);
            TypeThunker tt = new TypeThunker(rs, a.getType());
            return new AllocationThunker(rs, tt, usage, a);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    static public Allocation createCubemapFromCubeFaces(RenderScript rs,
                                                        Bitmap xpos,
                                                        Bitmap xneg,
                                                        Bitmap ypos,
                                                        Bitmap yneg,
                                                        Bitmap zpos,
                                                        Bitmap zneg,
                                                        MipmapControl mips,
                                                        int usage) {
        RenderScriptThunker rst = (RenderScriptThunker)rs;
        try {
            android.renderscript.Allocation a =
                    android.renderscript.Allocation.createCubemapFromCubeFaces(
                    rst.mN, xpos, xneg, ypos, yneg, zpos, zneg,
                    convertMipmapControl(mips), usage);
            TypeThunker tt = new TypeThunker(rs, a.getType());
            return new AllocationThunker(rs, tt, usage, a);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    static public Allocation createFromBitmapResource(RenderScript rs,
                                                      Resources res,
                                                      int id,
                                                      MipmapControl mips,
                                                      int usage) {

        RenderScriptThunker rst = (RenderScriptThunker)rs;
        try {
            android.renderscript.Allocation a =
                    android.renderscript.Allocation.createFromBitmapResource(
                    rst.mN, res, id, convertMipmapControl(mips), usage);
            TypeThunker tt = new TypeThunker(rs, a.getType());
            return new AllocationThunker(rs, tt, usage, a);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    static public Allocation createFromString(RenderScript rs,
                                              String str,
                                              int usage) {
        RenderScriptThunker rst = (RenderScriptThunker)rs;
        try {
            android.renderscript.Allocation a =
                    android.renderscript.Allocation.createFromString(
                    rst.mN, str, usage);
            TypeThunker tt = new TypeThunker(rs, a.getType());
            return new AllocationThunker(rs, tt, usage, a);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    static public Allocation createSized(RenderScript rs, Element e,
                                         int count, int usage) {
        RenderScriptThunker rst = (RenderScriptThunker)rs;
        ElementThunker et = (ElementThunker) e;
        try {
            android.renderscript.Allocation a =
                android.renderscript.Allocation.createSized
                (rst.mN, (android.renderscript.Element)e.getNObj(), count, usage);
            TypeThunker tt = new TypeThunker(rs, a.getType());
            return new AllocationThunker(rs, tt, usage, a);
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }

}