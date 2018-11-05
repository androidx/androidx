/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.content.pm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.collection.ArrayMap;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.content.pm.ShortcutsInfoSerialization.ShortcutContainer;
import androidx.core.graphics.drawable.IconCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Provides APIs to access and update a persistable list of {@link ShortcutInfoCompat}. This class
 * keeps an up-to-date cache of the complete list in memory for quick access, except shortcuts'
 * Icons, which are stored on the disk and only loaded from disk separately if necessary.
 */
@RequiresApi(19)
//TODO: we need Futures.addCallback and CallbackToFutureAdapter, update once they're available
class ShortcutInfoCompatSaver {

    static final String TAG = "ShortcutInfoCompatSaver";

    private static final String DIRECTORY_TARGETS = "ShortcutInfoCompatSaver_share_targets";
    private static final String DIRECTORY_BITMAPS = "ShortcutInfoCompatSaver_share_targets_bitmaps";
    private static final String FILENAME_XML = "targets.xml";
    // The maximum time background idle threads will wait for new tasks before terminating
    private static final int EXECUTOR_KEEP_ALIVE_TIME_SECS = 20;
    private static final Object GET_INSTANCE_LOCK = new Object();

    private static volatile ShortcutInfoCompatSaver sINSTANCE;

    @SuppressWarnings("WeakerAccess")
    final Context mContext;
    // mShortcutsMap is strictly only accessed by mCacheUpdateService
    @SuppressWarnings("WeakerAccess")
    final Map<String, ShortcutContainer> mShortcutsMap = new ArrayMap<>();
    @SuppressWarnings("WeakerAccess")
    final Map<String, ListenableFuture<?>> mScheduledBitmapTasks = new ArrayMap<>();

    @SuppressWarnings("WeakerAccess")
    final ExecutorService mCacheUpdateService;
    // Single threaded tasks queue for IO operations on disk
    private final ExecutorService mDiskIoService;

    @SuppressWarnings("WeakerAccess")
    final File mTargetsXmlFile;
    @SuppressWarnings("WeakerAccess")
    final File mBitmapsDir;

    @AnyThread
    static ShortcutInfoCompatSaver getInstance(Context context) {
        if (sINSTANCE == null) {
            synchronized (GET_INSTANCE_LOCK) {
                if (sINSTANCE == null) {
                    sINSTANCE = new ShortcutInfoCompatSaver(context,
                            createExecutorService(),
                            createExecutorService());
                }
            }
        }
        return sINSTANCE;
    }

    @AnyThread
    static ExecutorService createExecutorService() {
        return new ThreadPoolExecutor(
                // Set to 0 to avoid persistent background thread when idle
                0, /* core pool size */
                // Set to 1 to ensure tasks will run strictly in the submit order
                1, /* max pool size */
                EXECUTOR_KEEP_ALIVE_TIME_SECS, /* keep alive time */
                TimeUnit.SECONDS, /* keep alive time unit */
                new LinkedBlockingQueue<Runnable>() /* Not used */);
    }

    @AnyThread
    ShortcutInfoCompatSaver(Context context, ExecutorService cacheUpdateService,
            ExecutorService diskIoService) {
        mContext = context.getApplicationContext();
        mCacheUpdateService = cacheUpdateService;
        mDiskIoService = diskIoService;
        final File workingDirectory = new File(context.getFilesDir(), DIRECTORY_TARGETS);
        mBitmapsDir = new File(workingDirectory, DIRECTORY_BITMAPS);
        mTargetsXmlFile = new File(workingDirectory, FILENAME_XML);
        // we trying to recover from errors during following submit:
        // if xml was corrupted it is removed and saver is started clean
        // however it is still not great and there is chance to swallow an exception
        mCacheUpdateService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureDir(workingDirectory);
                    ensureDir(mBitmapsDir);
                    mShortcutsMap.putAll(ShortcutsInfoSerialization.loadFromXml(mTargetsXmlFile,
                            mContext));
                    deleteDanglingBitmaps(new ArrayList<>(mShortcutsMap.values()));
                } catch (Exception e) {
                    Log.w(TAG, "ShortcutInfoCompatSaver started with an exceptions ", e);
                }
            }
        });
    }

    @AnyThread
    ListenableFuture<Void> removeShortcuts(List<String> shortcutIds) {
        final List<String> idList = new ArrayList<>(shortcutIds);
        final ResolvableFuture<Void> result = ResolvableFuture.create();
        mCacheUpdateService.submit(new Runnable() {
            @Override
            public void run() {
                for (String id : idList) {
                    mShortcutsMap.remove(id);
                    ListenableFuture<?> removed = mScheduledBitmapTasks.remove(id);
                    if (removed != null) {
                        removed.cancel(false);
                    }
                }
                scheduleSyncCurrentState(result);
            }
        });
        return result;
    }

    @AnyThread
    ListenableFuture<Void> removeAllShortcuts() {
        final ResolvableFuture<Void> result =
                ResolvableFuture.create();
        mCacheUpdateService.submit(new Runnable() {
            @Override
            public void run() {
                mShortcutsMap.clear();
                for (ListenableFuture<?> task : mScheduledBitmapTasks.values()) {
                    task.cancel(false);
                }
                mScheduledBitmapTasks.clear();
                scheduleSyncCurrentState(result);
            }
        });

        return result;
    }

    @WorkerThread
    List<ShortcutInfoCompat> getShortcuts() throws Exception {
        return mCacheUpdateService.submit(new Callable<ArrayList<ShortcutInfoCompat>>() {
            @Override
            public ArrayList<ShortcutInfoCompat> call() {
                ArrayList<ShortcutInfoCompat> shortcuts = new ArrayList<>();
                for (ShortcutContainer item : mShortcutsMap.values()) {
                    shortcuts.add(new ShortcutInfoCompat.Builder(item.mShortcutInfo).build());
                }
                return shortcuts;
            }
        }).get();
    }

    @WorkerThread
    IconCompat getShortcutIcon(final String shortcutId) throws Exception {
        final ShortcutContainer container = mCacheUpdateService.submit(
                new Callable<ShortcutContainer>() {
                    @Override
                    public ShortcutContainer call() {
                        return mShortcutsMap.get(shortcutId);
                    }
                }).get();
        if (container == null) {
            return null;
        }
        if (!TextUtils.isEmpty(container.mResourceName)) {
            int id = 0;
            try {
                id = mContext.getResources().getIdentifier(container.mResourceName, null, null);
            } catch (Exception e) {
                /* Do nothing, continue and try mBitmapPath */
            }
            if (id != 0) {
                return IconCompat.createWithResource(mContext, id);
            }
        }
        if (!TextUtils.isEmpty(container.mBitmapPath)) {
            Bitmap bitmap = mDiskIoService.submit(new Callable<Bitmap>() {
                @Override
                public Bitmap call() {
                    return BitmapFactory.decodeFile(container.mBitmapPath);
                }
            }).get();
            // TODO: Re-create an adaptive icon if the original icon was adaptive
            return bitmap != null ? IconCompat.createWithBitmap(bitmap) : null;
        }
        return null;
    }

    /**
     * Delete bitmap files from the disk if they are not associated with any shortcuts in the list.
     *
     * Strictly called by mDiskIoService only
     */
    @SuppressWarnings("WeakerAccess")
    void deleteDanglingBitmaps(List<ShortcutContainer> shortcutsList) {
        List<String> bitmapPaths = new ArrayList<>();
        for (ShortcutContainer item : shortcutsList) {
            if (!TextUtils.isEmpty(item.mBitmapPath)) {
                bitmapPaths.add(item.mBitmapPath);
            }
        }
        for (File bitmap : mBitmapsDir.listFiles()) {
            if (!bitmapPaths.contains(bitmap.getAbsolutePath())) {
                bitmap.delete();
            }
        }
    }

    ListenableFuture<Void> addShortcuts(List<ShortcutInfoCompat> shortcuts) {
        final List<ShortcutInfoCompat> copy = new ArrayList<>(shortcuts.size());
        for (ShortcutInfoCompat infoCompat : shortcuts) {
            copy.add(new ShortcutInfoCompat.Builder(infoCompat).build());
        }
        final ResolvableFuture<Void> result = ResolvableFuture.create();
        mCacheUpdateService.submit(new Runnable() {
            @Override
            public void run() {
                for (final ShortcutInfoCompat info : copy) {
                    Set<String> categories = info.getCategories();
                    if (categories == null || categories.isEmpty()) {
                        continue;
                    }
                    ShortcutContainer container = containerFrom(info);
                    IconCompat icon = info.mIcon;
                    // not null only if it is safe to call getBitmap
                    Bitmap bitmap = container.mBitmapPath != null ? icon.getBitmap() : null;
                    final String id = info.getId();
                    mShortcutsMap.put(id, container);
                    if (bitmap != null) {
                        final ListenableFuture<Void> future = scheduleBitmapSaving(bitmap,
                                container.mBitmapPath);
                        ListenableFuture<?> old = mScheduledBitmapTasks.put(id, future);
                        if (old != null) {
                            old.cancel(false);
                        }
                        future.addListener(new Runnable() {
                            @Override
                            public void run() {
                                mScheduledBitmapTasks.remove(id);
                                // saving bitmap was skipped, but it is okay
                                if (future.isCancelled()) {
                                    return;
                                }
                                try {
                                    future.get();
                                } catch (Exception e) {
                                    // propagate an exception up to the chain.
                                    result.setException(e);
                                }
                            }
                        }, mCacheUpdateService);
                    }
                }
                scheduleSyncCurrentState(result);
            }
        });
        return result;
    }

    @SuppressWarnings("WeakerAccess")
    ListenableFuture<Void> scheduleBitmapSaving(final Bitmap bitmap, final String path) {
        return submitDiskOperation(new Runnable() {
            @Override
            public void run() {
                saveBitmap(bitmap, path);
            }
        });
    }

    private ListenableFuture<Void> submitDiskOperation(final Runnable runnable) {
        final ResolvableFuture<Void> result = ResolvableFuture.create();
        mDiskIoService.submit(new Runnable() {
            @Override
            public void run() {
                if (result.isCancelled()) {
                    return;
                }
                try {
                    runnable.run();
                    result.set(null);
                } catch (Exception e) {
                    result.setException(e);
                }
            }
        });
        return result;
    }

    // must be called on mCacheUpdateService
    @SuppressWarnings("WeakerAccess")
    void scheduleSyncCurrentState(final ResolvableFuture<Void> output) {
        final List<ShortcutContainer> containers = new ArrayList<>(mShortcutsMap.values());
        final ListenableFuture<Void> future = submitDiskOperation(new Runnable() {
            @Override
            public void run() {
                deleteDanglingBitmaps(containers);
                ShortcutsInfoSerialization.saveAsXml(containers, mTargetsXmlFile);
            }
        });
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    future.get();
                    output.set(null);
                } catch (Exception e) {
                    output.setException(e);
                }
            }
        }, mCacheUpdateService);
    }

    @SuppressWarnings("WeakerAccess")
    ShortcutContainer containerFrom(ShortcutInfoCompat shortcut) {
        String resourceName = null;
        String bitmapPath = null;
        if (shortcut.mIcon != null) {
            IconCompat icon = shortcut.mIcon;
            switch (icon.getType()) {
                case Icon.TYPE_RESOURCE:
                    resourceName = mContext.getResources().getResourceName(icon.getResId());
                    break;
                case Icon.TYPE_BITMAP:
                case Icon.TYPE_ADAPTIVE_BITMAP:
                    // Choose a unique file name to serialize the bitmap
                    bitmapPath = new File(mBitmapsDir, UUID.randomUUID().toString())
                            .getAbsolutePath();
                    break;
                case Icon.TYPE_DATA:
                case Icon.TYPE_URI:
                case IconCompat.TYPE_UNKNOWN:
                    break;
            }
        }
        ShortcutInfoCompat shortcutCopy = new ShortcutInfoCompat.Builder(shortcut)
                .setIcon(null).build();
        return new ShortcutContainer(shortcutCopy, resourceName, bitmapPath);
    }

    /*
     * Suppress wrong thread warning since Bitmap.compress() and saveBitmap() are both annotated
     * @WorkerThread, but from different packages.
     * androidx.annotation.WorkerThread vs android.annotation.WorkerThread
     */
    @WorkerThread
    @SuppressWarnings("WrongThread")
    void saveBitmap(Bitmap bitmap, String path) {
        if (bitmap == null) {
            throw new IllegalArgumentException("bitmap is null");
        }
        if (TextUtils.isEmpty(path)) {
            throw new IllegalArgumentException("path is empty");
        }

        try (FileOutputStream fileStream = new FileOutputStream(new File(path))) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100 /* quality */, fileStream)) {
                Log.wtf(TAG, "Unable to compress bitmap");
                throw new RuntimeException("Unable to compress bitmap for saving " + path);
            }
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            Log.wtf(TAG, "Unable to write bitmap to file", e);
            throw new RuntimeException("Unable to write bitmap to file " + path, e);
        }
    }

    static boolean ensureDir(File directory) {
        if (directory.exists() && !directory.isDirectory() && !directory.delete()) {
            return false;
        }
        if (!directory.exists()) {
            return directory.mkdirs();
        }
        return true;
    }
}
