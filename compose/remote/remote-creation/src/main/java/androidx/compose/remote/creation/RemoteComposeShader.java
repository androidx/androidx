/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.creation;

import androidx.compose.remote.core.operations.ShaderData;

import java.util.HashMap;

/** Provides an API to create Shaders, setUniforms which is inserted into doc */
public class RemoteComposeShader {
    int mShaderID = 0; // allows shaders to be referenced by number
    int mShaderTextID = 0; // allows shaders to be referenced by number
    HashMap<String, float[]> mUniformFloatMap = null;
    HashMap<String, int[]> mUniformIntMap = null;
    HashMap<String, Integer> mUniformBitmapMap = null;
    String mShader;
    RemoteComposeWriter mWriter;

    public RemoteComposeShader(String shader, RemoteComposeWriter writer) {
        this.mShader = shader;
        this.mWriter = writer;
        mShaderTextID = writer.addText(shader);
    }

    public String getShader() {
        return mShader;
    }

    public int getShaderID() {
        return mShaderID;
    }

    public void setShaderID(int id) {
        mShaderID = id;
    }

    public int getShaderTextID() {
        return mShaderTextID;
    }

    // Int Uniforms

    /**
     * Sets a uniform of an array of 4 integers
     *
     * @param name The name of the uniform
     * @param v1 The first Variable (typically X, or A in Vector)
     * @param v2 The second Variable (typically Y or R )
     * @param v3 The Third Variable (Typically Z or G)
     * @param v4 The Third Variable (Typically W or B)
     * @return this
     */
    public RemoteComposeShader setIntUniform(String name, int v1, int v2, int v3, int v4) {
        return mySetIntUniform(name, v1, v2, v3, v4);
    }

    /**
     * Sets a uniform of an array of 3 integers
     *
     * @param name The name of the uniform
     * @param v1 The first Variable (typically X, or A in Vector)
     * @param v2 The second Variable (typically Y or R )
     * @param v3 The Third Variable (Typically Z or G)
     * @return this
     */
    public RemoteComposeShader setIntUniform(String name, int v1, int v2, int v3) {
        return mySetIntUniform(name, v1, v2, v3);
    }

    /**
     * Sets a uniform of an array of 2 integers
     *
     * @param name The name of the uniform
     * @param v1 The first Variable (typically X, or A in Vector)
     * @param v2 The second Variable (typically Y or R )
     * @return this
     */
    public RemoteComposeShader setIntUniform(String name, int v1, int v2) {
        return mySetIntUniform(name, v1, v2);
    }

    /**
     * Sets a uniform of an integer
     *
     * @param name The name of the uniform
     * @param v1 The first Variable (typically X, or A in Vector)
     * @return this
     */
    public RemoteComposeShader setIntUniform(String name, int v1) {
        return mySetIntUniform(name, v1);
    }

    private RemoteComposeShader mySetIntUniform(String name, int... value) {
        HashMap<String, int[]> map =
                mUniformIntMap == null ? new HashMap<String, int[]>() : mUniformIntMap;
        mUniformIntMap = map;
        map.put(name, value);
        return this;
    }

    // Float Uniforms

    /**
     * Sets a uniform of an array of 4 floats
     *
     * @param name The name of the uniform
     * @param v1 The first Variable (typically X, or A in Vector)
     * @param v2 The second Variable (typically Y or R )
     * @param v3 The Third Variable (Typically Z or G)
     * @param v4 The Third Variable (Typically W or B)
     * @return this
     */
    public RemoteComposeShader setFloatUniform(
            String name, float v1, float v2, float v3, float v4) {
        return mySetFloatUniform(name, v1, v2, v3, v4);
    }

    /**
     * Sets a uniform of an array of 3 floats
     *
     * @param name The name of the uniform
     * @param v1 The first Variable (typically X, or A in Vector)
     * @param v2 The second Variable (typically Y or R )
     * @param v3 The Third Variable (Typically Z or G)
     * @return this
     */
    public RemoteComposeShader setFloatUniform(String name, float v1, float v2, float v3) {
        return mySetFloatUniform(name, v1, v2, v3);
    }

    /**
     * Sets a uniform of an array of 2 floats
     *
     * @param name The name of the uniform
     * @param v1 The first Variable (typically X Vector)
     * @param v2 The second Variable (typically Y)
     * @return this
     */
    public RemoteComposeShader setFloatUniform(String name, float v1, float v2) {
        return mySetFloatUniform(name, v1, v2);
    }

    /**
     * Sets a uniform of a float
     *
     * @param name The name of the uniform
     * @param v1 The Variable
     * @return this
     */
    public RemoteComposeShader setFloatUniform(String name, float v1) {
        return mySetFloatUniform(name, v1);
    }

    private RemoteComposeShader mySetFloatUniform(String name, float... value) {
        HashMap<String, float[]> map =
                mUniformFloatMap == null ? new HashMap<String, float[]>() : mUniformFloatMap;
        mUniformFloatMap = map;
        map.put(name, value);
        return this;
    }

    /**
     * Set a uniform of a bitmap (the id)
     *
     * @param name The name of the uniform
     * @param id The id of the bitmap
     * @return this
     */
    public RemoteComposeShader setBitmapUniform(String name, int id) {
        HashMap<String, Integer> map =
                mUniformBitmapMap == null ? new HashMap<String, Integer>() : mUniformBitmapMap;
        mUniformBitmapMap = map;
        map.put(name, id);
        return this;
    }

    /**
     * Writes the shader to the buffer
     *
     * @return this
     */
    public int commit() {
        mShaderID = mWriter.mState.dataGetId(this);
        if (mShaderID == -1) {
            mShaderID = mWriter.mState.cacheData(this);
        }
        ShaderData.apply(
                mWriter.getBuffer().getBuffer(),
                mShaderID,
                mShaderTextID,
                mUniformFloatMap,
                mUniformIntMap,
                mUniformBitmapMap);
        return mShaderID;
    }
}
