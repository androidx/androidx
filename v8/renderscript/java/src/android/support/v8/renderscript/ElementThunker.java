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

import java.lang.reflect.Field;

import android.util.Log;

class ElementThunker extends Element {
    android.renderscript.Element mN;

    android.renderscript.Element getNObj() {
        return mN;
    }

    public int getBytesSize() {
        try {
            return mN.getBytesSize();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public int getVectorSize() {
        try {
            return mN.getVectorSize();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    static android.renderscript.Element.DataKind convertKind(DataKind cdk) {
        switch(cdk) {
        case USER:
            return android.renderscript.Element.DataKind.USER;
        case PIXEL_L:
            return android.renderscript.Element.DataKind.PIXEL_L;
        case PIXEL_A:
            return android.renderscript.Element.DataKind.PIXEL_A;
        case PIXEL_LA:
            return android.renderscript.Element.DataKind.PIXEL_LA;
        case PIXEL_RGB:
            return android.renderscript.Element.DataKind.PIXEL_RGB;
        case PIXEL_RGBA:
            return android.renderscript.Element.DataKind.PIXEL_RGBA;
        }
        return null;
    }

    static android.renderscript.Element.DataType convertType(DataType cdt) {
        switch(cdt) {
        case NONE:
            return android.renderscript.Element.DataType.NONE;
            //case DataType.FLOAT_16:
        case FLOAT_32:
            return android.renderscript.Element.DataType.FLOAT_32;
        case FLOAT_64:
            return android.renderscript.Element.DataType.FLOAT_64;
        case SIGNED_8:
            return android.renderscript.Element.DataType.SIGNED_8;
        case SIGNED_16:
            return android.renderscript.Element.DataType.SIGNED_16;
        case SIGNED_32:
            return android.renderscript.Element.DataType.SIGNED_32;
        case SIGNED_64:
            return android.renderscript.Element.DataType.SIGNED_64;
        case UNSIGNED_8:
            return android.renderscript.Element.DataType.UNSIGNED_8;
        case UNSIGNED_16:
            return android.renderscript.Element.DataType.UNSIGNED_16;
        case UNSIGNED_32:
            return android.renderscript.Element.DataType.UNSIGNED_32;
        case UNSIGNED_64:
            return android.renderscript.Element.DataType.UNSIGNED_64;

        case BOOLEAN:
            return android.renderscript.Element.DataType.BOOLEAN;

        case MATRIX_4X4:
            return android.renderscript.Element.DataType.MATRIX_4X4;
        case MATRIX_3X3:
            return android.renderscript.Element.DataType.MATRIX_3X3;
        case MATRIX_2X2:
            return android.renderscript.Element.DataType.MATRIX_2X2;

        case RS_ELEMENT:
            return android.renderscript.Element.DataType.RS_ELEMENT;
        case RS_TYPE:
            return android.renderscript.Element.DataType.RS_TYPE;
        case RS_ALLOCATION:
            return android.renderscript.Element.DataType.RS_ALLOCATION;
        case RS_SAMPLER:
            return android.renderscript.Element.DataType.RS_SAMPLER;
        case RS_SCRIPT:
            return android.renderscript.Element.DataType.RS_SCRIPT;
        }
        return null;
    }

    public boolean isComplex() {
        try {
            return mN.isComplex();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public int getSubElementCount() {
        try {
            return mN.getSubElementCount();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Element getSubElement(int index) {
        try {
            return new ElementThunker(mRS, mN.getSubElement(index));
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public String getSubElementName(int index) {
        try {
            return mN.getSubElementName(index);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public int getSubElementArraySize(int index) {
        try {
            return mN.getSubElementArraySize(index);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public int getSubElementOffsetBytes(int index) {
        try {
            return mN.getSubElementOffsetBytes(index);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public DataType getDataType() {
        return mType;
    }

    public DataKind getDataKind() {
        return mKind;
    }


    ElementThunker(RenderScript rs, android.renderscript.Element e) {
        super(0, rs);
        mN = e;
    }


    static Element create(RenderScript rs, DataType dt) {
        RenderScriptThunker rst = (RenderScriptThunker)rs;
        try {
            android.renderscript.Element e = null;
            switch(dt) {
            case FLOAT_32:
                e = android.renderscript.Element.F32(rst.mN);
                break;
            case FLOAT_64:
                e = android.renderscript.Element.F64(rst.mN);
                break;
            case SIGNED_8:
                e = android.renderscript.Element.I8(rst.mN);
                break;
            case SIGNED_16:
                e = android.renderscript.Element.I16(rst.mN);
                break;
            case SIGNED_32:
                e = android.renderscript.Element.I32(rst.mN);
                break;
            case SIGNED_64:
                e = android.renderscript.Element.I64(rst.mN);
                break;
            case UNSIGNED_8:
                e = android.renderscript.Element.U8(rst.mN);
                break;
            case UNSIGNED_16:
                e = android.renderscript.Element.U16(rst.mN);
                break;
            case UNSIGNED_32:
                e = android.renderscript.Element.U32(rst.mN);
                break;
            case UNSIGNED_64:
                e = android.renderscript.Element.U64(rst.mN);
                break;

            case BOOLEAN:
                e = android.renderscript.Element.BOOLEAN(rst.mN);
                break;

            case MATRIX_4X4:
                e = android.renderscript.Element.MATRIX_4X4(rst.mN);
                break;
            case MATRIX_3X3:
                e = android.renderscript.Element.MATRIX_3X3(rst.mN);
                break;
            case MATRIX_2X2:
                e = android.renderscript.Element.MATRIX_2X2(rst.mN);
                break;

            case RS_ELEMENT:
                e = android.renderscript.Element.ELEMENT(rst.mN);
                break;
            case RS_TYPE:
                e = android.renderscript.Element.TYPE(rst.mN);
                break;
            case RS_ALLOCATION:
                e = android.renderscript.Element.ALLOCATION(rst.mN);
                break;
            case RS_SAMPLER:
                e = android.renderscript.Element.SAMPLER(rst.mN);
                break;
            case RS_SCRIPT:
                e = android.renderscript.Element.SCRIPT(rst.mN);
                break;
            }

            return new ElementThunker(rs, e);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public static Element createVector(RenderScript rs, DataType dt, int size) {
        RenderScriptThunker rst = (RenderScriptThunker)rs;
        android.renderscript.Element e;
        try {
            e = android.renderscript.Element.createVector(rst.mN, convertType(dt), size);
            return new ElementThunker(rs, e);
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }

    public static Element createPixel(RenderScript rs, DataType dt, DataKind dk) {
        RenderScriptThunker rst = (RenderScriptThunker)rs;
        android.renderscript.Element e;
        try {
            e = android.renderscript.Element.createPixel(rst.mN, convertType(dt), convertKind(dk));
        return new ElementThunker(rs, e);
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }

    public boolean isCompatible(Element e) {
        ElementThunker et = (ElementThunker)e;
        try {
            return et.mN.isCompatible(mN);
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }

    static class BuilderThunker {
        android.renderscript.Element.Builder mN;

        public BuilderThunker(RenderScript rs) {
            RenderScriptThunker rst = (RenderScriptThunker)rs;
            try {
                mN = new android.renderscript.Element.Builder(rst.mN);
            } catch (android.renderscript.RSRuntimeException e) {
                throw ExceptionThunker.convertException(e);
            }
        }

        public void add(Element e, String name, int arraySize) {
            ElementThunker et = (ElementThunker)e;
            try {
                mN.add(et.mN, name, arraySize);
            } catch (android.renderscript.RSRuntimeException exc) {
                throw ExceptionThunker.convertException(exc);
            }
        }

        public Element create(RenderScript rs) {
            try {
                android.renderscript.Element e = mN.create();
                return new ElementThunker(rs, e);
            } catch (android.renderscript.RSRuntimeException exc) {
                throw ExceptionThunker.convertException(exc);
            }
        }
    }
}


