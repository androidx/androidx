/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.core;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/** View model providing access to the camera */
public class CameraXViewModel extends AndroidViewModel {

    private MutableLiveData<ProcessCameraProvider> mProcessCameraProviderLiveData;

    public CameraXViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * Returns a {@link LiveData} containing CameraX's {@link ProcessCameraProvider} once it has
     * been initialized.
     */
    LiveData<ProcessCameraProvider> getCameraProvider() {
        if (mProcessCameraProviderLiveData == null) {
            mProcessCameraProviderLiveData = new MutableLiveData<>();
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                    ProcessCameraProvider.getInstance(getApplication());

            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    mProcessCameraProviderLiveData.setValue(cameraProvider);
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof CancellationException)) {
                        throw new IllegalStateException("Error occurred while initializing "
                                + "CameraX:", e.getCause());
                    }
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Unable to use CameraX", e);
                }
            }, ContextCompat.getMainExecutor(getApplication()));
        }
        return mProcessCameraProviderLiveData;
    }
}
