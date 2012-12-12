/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.support.v13.app;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * This is the service that provides the factory to be bound to the collection service. This class
 * needs to be declared in your manifest. For more details, refer to the docs for
 * {@link RemoteViewsCompat} for proper usage.
 */
public class RemoteViewsServiceCompat extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        RemoteViewsFactory rvf = RemoteViewsListFactory.getFactory(intent);
        return rvf;
    }
}
