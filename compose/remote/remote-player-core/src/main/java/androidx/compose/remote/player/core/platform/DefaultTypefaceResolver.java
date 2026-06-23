/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.core.platform;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.graphics.fonts.FontStyle;
import android.graphics.fonts.FontVariationAxis;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Limits;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.paint.PaintBundle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Default implementation of TypefaceResolver that preserves the original logic from
 * AndroidPaintContext.
 */
@RestrictTo(LIBRARY_GROUP)
public class DefaultTypefaceResolver implements TypefaceResolver {
    private static final String SYSTEM_FONTS_PATH = "/system/fonts/";
    private final RemoteContext mContext;

    private final LinkedHashMap<String, String> mPathCache =
            new LinkedHashMap<String, String>(Limits.MAX_CACHE_ENTRIES + 1, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > Limits.MAX_CACHE_ENTRIES;
                }
            };
    private final LinkedHashMap<String, Typeface> mTypefaceCache =
            new LinkedHashMap<String, Typeface>(Limits.MAX_CACHE_ENTRIES + 1, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Typeface> eldest) {
                    return size() > Limits.MAX_CACHE_ENTRIES;
                }
            };
    private final LinkedHashMap<String, Font.Builder> mFontBuilderCache =
            new LinkedHashMap<String, Font.Builder>(Limits.MAX_CACHE_ENTRIES + 1, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Font.Builder> eldest) {
                    return size() > Limits.MAX_CACHE_ENTRIES;
                }
            };

    public DefaultTypefaceResolver(@NonNull RemoteContext context) {
        this.mContext = context;
    }

    @Override
    public @NonNull FontInstance resolve(
            int fontType,
            int weight,
            boolean italic,
            @Nullable Typeface fallbackTypeface,
            int fallbackWeight,
            boolean fallbackItalic) {
        switch (fontType) {
            case PaintBundle.FONT_TYPE_DEFAULT:
                return new SimpleFontInstance(createTypeface(Typeface.DEFAULT, weight, italic));
            case PaintBundle.FONT_TYPE_SERIF:
                return new SimpleFontInstance(createTypeface(Typeface.SERIF, weight, italic));
            case PaintBundle.FONT_TYPE_SANS_SERIF:
                return new SimpleFontInstance(createTypeface(Typeface.SANS_SERIF, weight, italic));
            case PaintBundle.FONT_TYPE_MONOSPACE:
                return new SimpleFontInstance(createTypeface(Typeface.MONOSPACE, weight, italic));
            default: // font data
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    RemoteContext.FontInfo fi =
                            (RemoteContext.FontInfo) mContext.getObject(fontType);
                    Font.Builder builder = (Font.Builder) fi.fontBuilder;
                    if (builder == null) {
                        fi.fontBuilder = builder =
                                createFontBuilder(fi.mFontData, weight, italic);
                    }
                    return new BuilderFontInstance(builder);
                } else {
                    return new SimpleFontInstance(Typeface.DEFAULT);
                }
        }
    }

    private Typeface createTypeface(Typeface base, int weight, boolean italic) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (weight == 400 && !italic) {
                return base;
            } else {
                return Typeface.create(base, weight, italic);
            }
        } else {
            int style = (weight >= 600) ? Typeface.BOLD : Typeface.NORMAL;
            if (italic) {
                style |= Typeface.ITALIC;
            }
            return Typeface.create(base, style);
        }
    }

    @Override
    public @NonNull FontInstance resolve(
            @NonNull String fontName,
            int weight,
            boolean italic,
            @Nullable Typeface fallbackTypeface,
            int fallbackWeight,
            boolean fallbackItalic) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Font.Builder fontBuilder = fbFromString(fontName, weight, italic);
            if (fontBuilder != null) {
                try {
                    return new BuilderFontInstance(fontBuilder);
                } catch (Exception e) {
                    String key = fontName + weight + italic;
                    mFontBuilderCache.put(key, null); // block further lookups
                }
            }
        }
        Typeface tf =
                tfFromString(
                        fontName, weight, italic, fallbackTypeface, fallbackWeight, fallbackItalic);
        if (tf != null) {
            return new SimpleFontInstance(tf);
        }
        if (fallbackTypeface != null) {
            return new SimpleFontInstance(fallbackTypeface);
        }
        return new SimpleFontInstance(Typeface.DEFAULT);
    }

    private Typeface tfFromString(
            String fontType,
            int weight,
            boolean italic,
            Typeface fallbackTypeface,
            int fallbackWeight,
            boolean fallbackItalic) {
        String key = fontType + weight + italic;
        if (mTypefaceCache.containsKey(key)) {
            return mTypefaceCache.get(key);
        }

        Typeface typeface =
                createTypeface(
                        fontType, weight, italic, fallbackTypeface, fallbackWeight, fallbackItalic);
        mTypefaceCache.put(key, typeface);
        return typeface;
    }

    private Typeface createTypeface(
            String fontType,
            int weight,
            boolean italic,
            Typeface fallbackTypeface,
            int fallbackWeight,
            boolean fallbackItalic) {

        Typeface basePrimary = Typeface.create(fontType, Typeface.NORMAL);

        boolean primaryFound =
                !basePrimary.equals(Typeface.DEFAULT)
                        || (fontType != null && fontType.equalsIgnoreCase("sans-serif"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (primaryFound) {
                try {
                    return Typeface.create(basePrimary, weight, italic);
                } catch (Exception ignored) {
                }
            }

            try {
                return Typeface.create(fallbackTypeface, fallbackWeight, fallbackItalic);
            } catch (Exception e) {
                return fallbackTypeface;
            }
        } else {
            if (primaryFound) {
                int style = (weight >= 600) ? Typeface.BOLD : Typeface.NORMAL;
                if (italic) {
                    style |= Typeface.ITALIC;
                }
                return Typeface.create(basePrimary, style);
            }
            int fallbackStyle = (fallbackWeight >= 600) ? Typeface.BOLD : Typeface.NORMAL;
            if (fallbackItalic) {
                fallbackStyle |= Typeface.ITALIC;
            }
            return Typeface.create(fallbackTypeface, fallbackStyle);
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private Font.Builder fbFromString(String fontType, int weight, boolean italic) {
        String key = fontType + weight + italic;
        String path = getFontPath(fontType);
        if (path == null) {
            return null;
        }
        if (mFontBuilderCache.containsKey(key)) {
            return mFontBuilderCache.get(key);
        }

        Font.Builder fb = new Font.Builder(new File(path));
        fb.setWeight(weight);
        fb.setSlant(italic ? FontStyle.FONT_SLANT_ITALIC : FontStyle.FONT_SLANT_UPRIGHT);
        mFontBuilderCache.put(key, fb);
        return fb;
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private Font.Builder createFontBuilder(byte[] data, int weight, boolean italic) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
        buffer.put(data);
        buffer.rewind();
        Font.Builder builder = new Font.Builder(buffer);
        builder.setWeight(weight);
        builder.setSlant(italic ? FontStyle.FONT_SLANT_ITALIC : FontStyle.FONT_SLANT_UPRIGHT);
        return builder;
    }

    private String getFontPath(String fontName) {
        if (mPathCache.containsKey(fontName)) {
            return mPathCache.get(fontName);
        }
        File fontsDir = new File(SYSTEM_FONTS_PATH);
        if (!fontsDir.exists() || !fontsDir.isDirectory()) {
            System.err.println("System fonts directory not found");
            mPathCache.put(fontName, null);
            return null;
        }

        File[] fontFiles = fontsDir.listFiles();
        if (fontFiles == null) {
            System.err.println("Unable to list font files");
            mPathCache.put(fontName, null);
            return null;
        }
        String fontNameLower = fontName.toLowerCase(Locale.ROOT);
        for (File fontFile : fontFiles) {
            if (fontFile.getName().toLowerCase(Locale.ROOT).contains(fontNameLower)) {
                mPathCache.put(fontName, fontFile.getAbsolutePath());
                return fontFile.getAbsolutePath();
            }
        }
        mPathCache.put(fontName, null);
        return null;
    }

    private static class SimpleFontInstance implements FontInstance {
        private final Typeface mTypeface;

        SimpleFontInstance(Typeface typeface) {
            this.mTypeface = typeface;
        }

        @Override
        public @NonNull Typeface getTypeface() {
            return mTypeface;
        }

        @Override
        public @NonNull Typeface applyVariationSettings(
                @NonNull String[] tags, float @NonNull [] values) {
            return mTypeface;
        }

        @Override
        public void setOnLoadedListener(@NonNull Runnable listener) {
            // Do nothing, already loaded.
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static class BuilderFontInstance implements FontInstance {
        private final Font.Builder mBuilder;
        private Typeface mTypeface;

        BuilderFontInstance(Font.Builder builder) {
            this.mBuilder = builder;
            this.mTypeface = buildTypeface(null);
        }

        @Override
        public @NonNull Typeface getTypeface() {
            return mTypeface;
        }

        @Override
        public @NonNull Typeface applyVariationSettings(
                @NonNull String[] tags, float @NonNull [] values) {
            FontVariationAxis[] axes = new FontVariationAxis[tags.length];
            for (int i = 0; i < tags.length; i++) {
                axes[i] = new FontVariationAxis(tags[i], values[i]);
            }
            mTypeface = buildTypeface(axes);
            return mTypeface;
        }

        @Override
        public void setOnLoadedListener(@NonNull Runnable listener) {
            // Do nothing, already loaded.
        }

        private Typeface buildTypeface(FontVariationAxis[] axis) {
            try {
                if (axis != null) {
                    mBuilder.setFontVariationSettings(axis);
                }
                Font font = mBuilder.build();
                FontFamily.Builder fontFamilyBuilder = new FontFamily.Builder(font);
                FontFamily fontFamily = fontFamilyBuilder.build();
                return new Typeface.CustomFallbackBuilder(fontFamily)
                        .setSystemFallback("sans-serif")
                        .build();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}
