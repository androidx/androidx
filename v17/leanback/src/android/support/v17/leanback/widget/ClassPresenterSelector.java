/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import java.util.HashMap;

/**
 * A ClassPresenterSelector selects a {@link Presenter} based on the item's
 * Java class.
 */
public final class ClassPresenterSelector extends PresenterSelector {

    private final HashMap<Class<?>, Presenter> mClassMap = new HashMap<Class<?>, Presenter>();

    public void addClassPresenter(Class<?> cls, Presenter presenter) {
        mClassMap.put(cls, presenter);
    }

    @Override
    public Presenter getPresenter(Object item) {
        Class<?> cls = item.getClass();
        Presenter presenter = null;

        do {
            presenter = mClassMap.get(cls);
            cls = cls.getSuperclass();
        } while (presenter == null && cls != null);

        return presenter;
    }
}
