/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.integration.testapp.vo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jspecify.annotations.NonNull;

@Entity(tableName = "products")
public class Product {

    @PrimaryKey(autoGenerate = true)
    public final long id;

    // This project is tested against a version of the room compiler that doesn't recognize JSpecify
    @SuppressWarnings("JSpecifyNullness")
    @androidx.annotation.NonNull
    public final String name;

    public Product(long id, @NonNull String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Product product = (Product) o;
        if (id != product.id) return false;
        return name == null ? product.name == null : name.equals(product.name);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(id);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
