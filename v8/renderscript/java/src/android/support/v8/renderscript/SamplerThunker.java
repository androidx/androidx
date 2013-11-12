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
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 *
 **/
class SamplerThunker extends Sampler {
    android.renderscript.Sampler mN;

    protected SamplerThunker(int id, RenderScript rs) {
        super(id, rs);
    }

    android.renderscript.BaseObj getNObj() {
        return mN;
    }

    static android.renderscript.Sampler.Value convertValue (Value v) {
        switch (v) {
        case NEAREST:
            return android.renderscript.Sampler.Value.NEAREST;
        case LINEAR:
            return android.renderscript.Sampler.Value.LINEAR;
        case LINEAR_MIP_LINEAR:
            return android.renderscript.Sampler.Value.LINEAR_MIP_LINEAR;
        case LINEAR_MIP_NEAREST:
            return android.renderscript.Sampler.Value.LINEAR_MIP_NEAREST;
        case WRAP:
            return android.renderscript.Sampler.Value.WRAP;
        case CLAMP:
            return android.renderscript.Sampler.Value.CLAMP;
        case MIRRORED_REPEAT:
            return android.renderscript.Sampler.Value.MIRRORED_REPEAT;
        }
        return null;
    }

    /**
     * Builder for creating non-standard samplers.  Useful if mix and match of
     * wrap modes is necesary or if anisotropic filtering is desired.
     *
     */
    public static class Builder {
        RenderScriptThunker mRS;
        Value mMin;
        Value mMag;
        Value mWrapS;
        Value mWrapT;
        Value mWrapR;
        float mAniso;

        public Builder(RenderScriptThunker rs) {
            mRS = rs;
            mMin = Value.NEAREST;
            mMag = Value.NEAREST;
            mWrapS = Value.WRAP;
            mWrapT = Value.WRAP;
            mWrapR = Value.WRAP;
        }

        public void setMinification(Value v) {
            if (v == Value.NEAREST ||
                v == Value.LINEAR ||
                v == Value.LINEAR_MIP_LINEAR ||
                v == Value.LINEAR_MIP_NEAREST) {
                mMin = v;
            } else {
                throw new IllegalArgumentException("Invalid value");
            }
        }

        public void setMagnification(Value v) {
            if (v == Value.NEAREST || v == Value.LINEAR) {
                mMag = v;
            } else {
                throw new IllegalArgumentException("Invalid value");
            }
        }

        public void setWrapS(Value v) {
            if (v == Value.WRAP || v == Value.CLAMP || v == Value.MIRRORED_REPEAT) {
                mWrapS = v;
            } else {
                throw new IllegalArgumentException("Invalid value");
            }
        }

        public void setWrapT(Value v) {
            if (v == Value.WRAP || v == Value.CLAMP || v == Value.MIRRORED_REPEAT) {
                mWrapT = v;
            } else {
                throw new IllegalArgumentException("Invalid value");
            }
        }

        public void setAnisotropy(float v) {
            if(v >= 0.0f) {
                mAniso = v;
            } else {
                throw new IllegalArgumentException("Invalid value");
            }
        }

        public Sampler create() {
            mRS.validate();
            try {
                android.renderscript.Sampler.Builder b = new android.renderscript.Sampler.Builder(mRS.mN);
                b.setMinification(convertValue(mMin));
                b.setMagnification(convertValue(mMag));
                b.setWrapS(convertValue(mWrapS));
                b.setWrapT(convertValue(mWrapT));
                b.setAnisotropy(mAniso);
                android.renderscript.Sampler s = b.create();

                SamplerThunker sampler = new SamplerThunker(0, mRS);
                sampler.mMin = mMin;
                sampler.mMag = mMag;
                sampler.mWrapS = mWrapS;
                sampler.mWrapT = mWrapT;
                sampler.mWrapR = mWrapR;
                sampler.mAniso = mAniso;
                sampler.mN = s;

                return sampler;
            } catch (android.renderscript.RSRuntimeException e) {
                throw ExceptionThunker.convertException(e);
            }
        }
    }


}