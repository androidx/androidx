/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.remote.core.operations.ComponentValue;

class ComponentValueKeys {
    float mWidth;
    float mHeight;
    float mContentWidth;
    float mContentHeight;
    float mX;
    float mY;
    float mRootX;
    float mRootY;

    public void setWidth(float width) {
        mWidth = width;
    }

    public void setHeight(float height) {
        mHeight = height;
    }

    public void setContentWidth(float contentWidth) {
        mContentWidth = contentWidth;
    }

    public void setContentHeight(float contentHeight) {
        mContentHeight = contentHeight;
    }

    public void setX(float x) {
        mX = x;
    }

    public void setY(float y) {
        mY = y;
    }

    public void setRootX(float x) {
        mRootX = x;
    }

    public void setRootY(float y) {
        mRootY = y;
    }

    public float getWidth() {
        return mWidth;
    }

    public float getHeight() {
        return mHeight;
    }

    public float getContentWidth() {
        return mContentWidth;
    }

    public float getContentHeight() {
        return mContentHeight;
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    public float getRootX() {
        return mRootX;
    }

    public float getRootY() {
        return mRootY;
    }

    public float getValue(int type) {
        switch (type) {
            case ComponentValue.WIDTH:
                return getWidth();
            case ComponentValue.HEIGHT:
                return getHeight();
            case ComponentValue.CONTENT_WIDTH:
                return getContentWidth();
            case ComponentValue.CONTENT_HEIGHT:
                return getContentHeight();
            case ComponentValue.POS_X:
                return getX();
            case ComponentValue.POS_Y:
                return getY();
            case ComponentValue.POS_ROOT_X:
                return getRootX();
            case ComponentValue.POS_ROOT_Y:
                return getRootY();
        }
        return 0f;
    }

    public void setValue(int type, float value) {
        switch (type) {
            case ComponentValue.WIDTH:
                setWidth(value);
                break;
            case ComponentValue.HEIGHT:
                setHeight(value);
                break;
            case ComponentValue.CONTENT_WIDTH:
                setContentWidth(value);
                break;
            case ComponentValue.CONTENT_HEIGHT:
                setContentHeight(value);
                break;
            case ComponentValue.POS_X:
                setX(value);
                break;
            case ComponentValue.POS_Y:
                setY(value);
                break;
            case ComponentValue.POS_ROOT_X:
                setRootX(value);
                break;
            case ComponentValue.POS_ROOT_Y:
                setRootY(value);
                break;
        }
    }
}
