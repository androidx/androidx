/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.constraintlayout.core.widgets;

/**
 * Simple rect class
 */
public class Rectangle {
    public int x;
    public int y;
    public int width;
    public int height;

    // @TODO: add description
    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    void grow(int w, int h) {
        x -= w;
        y -= h;
        width += 2 * w;
        height += 2 * h;
    }

    boolean intersects(Rectangle bounds) {
        return x >= bounds.x && x < bounds.x + bounds.width
                && y >= bounds.y && y < bounds.y + bounds.height;
    }

    // @TODO: add description
    public boolean contains(int x, int y) {
        return x >= this.x && x < this.x + this.width
                && y >= this.y && y < this.y + this.height;
    }

    public int getCenterX() {
        return (x + width) / 2;
    }

    public int getCenterY() {
        return (y + height) / 2;
    }
}
