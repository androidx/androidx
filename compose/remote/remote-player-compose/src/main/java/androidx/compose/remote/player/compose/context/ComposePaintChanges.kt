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
package androidx.compose.remote.player.compose.context

import android.annotation.SuppressLint
import android.graphics.*
import android.graphics.Typeface.CustomFallbackBuilder
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.graphics.fonts.FontStyle
import android.graphics.fonts.FontVariationAxis
import android.os.Build
import android.util.Log
import androidx.compose.remote.core.MatrixAccess
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.paint.PaintChanges
import androidx.compose.remote.player.compose.utils.remoteToBlendMode
import androidx.compose.remote.player.compose.utils.remoteToPorterDuffMode
import androidx.compose.remote.player.compose.utils.toPaintingStyle
import androidx.compose.remote.player.compose.utils.toStrokeCap
import androidx.compose.remote.player.compose.utils.toStrokeJoin
import androidx.compose.remote.player.view.platform.AndroidRemoteContext
import androidx.compose.ui.graphics.NativePaint
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/** A [PaintChanges] implementation for [ComposePaintContext]. */
internal class ComposePaintChanges(
    private val remoteContext: RemoteContext,
    private val getPaint: () -> androidx.compose.ui.graphics.Paint,
) : PaintChanges {
    private var fontBuilder: Font.Builder? = null
    val tmpMatrix: Matrix = Matrix()
    var tileModes: Array<Shader.TileMode> =
        arrayOf(Shader.TileMode.CLAMP, Shader.TileMode.REPEAT, Shader.TileMode.MIRROR)

    override fun setTextSize(size: Float) {
        getNativePaint().textSize = size
    }

    override fun setTypeFace(fontType: Int, weight: Int, italic: Boolean) {
        when (fontType) {
            PaintBundle.FONT_TYPE_DEFAULT ->
                if (weight == 400 && !italic) { // for normal case
                    getNativePaint().setTypeface(Typeface.DEFAULT)
                } else {
                    getNativePaint().setTypeface(Typeface.create(Typeface.DEFAULT, weight, italic))
                }
            PaintBundle.FONT_TYPE_SERIF ->
                if (weight == 400 && !italic) { // for normal case
                    getNativePaint().setTypeface(Typeface.SERIF)
                } else {
                    getNativePaint().setTypeface(Typeface.create(Typeface.SERIF, weight, italic))
                }
            PaintBundle.FONT_TYPE_SANS_SERIF ->
                if (weight == 400 && !italic) { //  for normal case
                    getNativePaint().setTypeface(Typeface.SANS_SERIF)
                } else {
                    getNativePaint()
                        .setTypeface(Typeface.create(Typeface.SANS_SERIF, weight, italic))
                }
            PaintBundle.FONT_TYPE_MONOSPACE ->
                if (weight == 400 && !italic) { //  for normal case
                    getNativePaint().setTypeface(Typeface.MONOSPACE)
                } else {
                    getNativePaint()
                        .setTypeface(Typeface.create(Typeface.MONOSPACE, weight, italic))
                }
            else -> {
                val fi = remoteContext.getObject(fontType) as RemoteContext.FontInfo?
                var builder = fi!!.fontBuilder as Font.Builder?
                if (builder == null) {
                    builder = createFontBuilder(fi.mFontData, weight, italic)
                    fi.fontBuilder = builder
                }
                fontBuilder = builder
                setAxis(null)
            }
        }
    }

    override fun setShaderMatrix(matrixId: Float) {
        val id = Utils.idFromNan(matrixId)
        if (id == 0) {
            getNativePaint().shader.setLocalMatrix(null)
            return
        }
        val matAccess = remoteContext.getObject(id) as MatrixAccess?
        tmpMatrix.setValues(MatrixAccess.to3x3(matAccess!!.get()))
        val s: Shader = getNativePaint().shader
        s.setLocalMatrix(tmpMatrix)
    }

    /**
     * @param fontType String to be looked up in system
     * @param weight the weight of the font
     * @param italic if the font is italic
     */
    override fun setTypeFace(fontType: String, weight: Int, italic: Boolean) {
        val path = getFontPath(fontType)
        fontBuilder = Font.Builder(File(path!!))
        fontBuilder!!.setWeight(weight)
        fontBuilder!!.setSlant(
            if (italic) FontStyle.FONT_SLANT_ITALIC else FontStyle.FONT_SLANT_UPRIGHT
        )
        setAxis(null)
    }

    private fun createFontBuilder(data: ByteArray, weight: Int, italic: Boolean): Font.Builder {
        val buffer = ByteBuffer.allocateDirect(data.size)

        // 2. Put the fontBytes into the direct buffer.
        buffer.put(data)
        buffer.rewind()
        fontBuilder = Font.Builder(buffer)
        fontBuilder!!.setWeight(weight)
        fontBuilder!!.setSlant(
            if (italic) FontStyle.FONT_SLANT_ITALIC else FontStyle.FONT_SLANT_UPRIGHT
        )
        setAxis(null)
        return fontBuilder!!
    }

    private fun setAxis(axis: Array<FontVariationAxis?>?) {
        var font: Font?
        try {
            if (axis != null) {
                fontBuilder!!.setFontVariationSettings(axis)
            }
            font = fontBuilder!!.build()
        } catch (e: IOException) {
            e.printStackTrace()
            throw RuntimeException(e)
        }

        val fontFamilyBuilder = FontFamily.Builder(font)
        val fontFamily = fontFamilyBuilder.build()
        val typeface = CustomFallbackBuilder(fontFamily).setSystemFallback("sans-serif").build()
        getNativePaint().setTypeface(typeface)
    }

    private fun getFontPath(fontName: String): String? {
        var fontName = fontName
        val fontsDir = File(SYSTEM_FONTS_PATH)
        if (!fontsDir.exists() || !fontsDir.isDirectory()) {
            Log.i(TAG, "System fonts directory not found")
            return null
        }

        val fontFiles = fontsDir.listFiles()
        if (fontFiles == null) {
            Log.i(TAG, "Unable to list font files")
            return null
        }
        fontName = fontName.lowercase()
        for (fontFile in fontFiles) {
            if (fontFile.getName().lowercase().contains(fontName)) {
                return fontFile.absolutePath
            }
        }
        Log.i(TAG, "font \"$fontName\" not found")
        return null
    }

    /**
     * Set the font variation axes
     *
     * @param tags tags
     * @param values values
     */
    override fun setFontVariationAxes(tags: Array<String>, values: FloatArray) {
        val axes = arrayOfNulls<FontVariationAxis>(tags.size)
        for (i in tags.indices) {
            axes[i] = FontVariationAxis(tags[i], values[i])
        }
        setAxis(axes)
    }

    /**
     * Set the texture shader
     *
     * @param bitmapId the id of the bitmap to use
     * @param tileX The tiling mode for x to draw the bitmap in.
     * @param tileY The tiling mode for y to draw the bitmap in.
     * @param filterMode the filter mode to be used when sampling from this shader.
     * @param maxAnisotropy The Anisotropy value to use for filtering. Must be greater than 0.
     */
    override fun setTextureShader(
        bitmapId: Int,
        tileX: Short,
        tileY: Short,
        filterMode: Short,
        maxAnisotropy: Short,
    ) {

        // TODO implement getBitmap(bitmapId)
        val bitmap = remoteContext.mRemoteComposeState.getFromId(bitmapId) as Bitmap ?: return
        val bs =
            BitmapShader(
                bitmap,
                Shader.TileMode.entries[tileX.toInt()],
                Shader.TileMode.entries[tileY.toInt()],
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (filterMode > 0) {
                bs.filterMode = filterMode.toInt()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    bs.maxAnisotropy = filterMode.toInt()
                }
            }
        }
        getPaint().shader = bs
        // Todo cache shader once created limit cache to 10 shaders
    }

    override fun setStrokeWidth(width: Float) {
        getPaint().strokeWidth = width
    }

    override fun setColor(color: Int) {
        getNativePaint().setColor(color)
    }

    override fun setStrokeCap(cap: Int) {
        getPaint().strokeCap = Paint.Cap.entries[cap].toStrokeCap()
    }

    override fun setStyle(style: Int) {
        getPaint().style = Paint.Style.entries[style].toPaintingStyle()
    }

    @SuppressLint("NewApi")
    override fun setShader(shaderId: Int) {
        // TODO this stuff should check the shader creation
        if (shaderId == 0) {
            getNativePaint().setShader(null)
            return
        }
        val data: ShaderData? = getShaderData(shaderId)
        if (data == null) {
            return
        }
        val shader = RuntimeShader(remoteContext.getText(data.shaderTextId)!!)
        var names = data.getUniformFloatNames()
        for (i in names.indices) {
            val name = names[i]
            val `val` = data.getUniformFloats(name)
            shader.setFloatUniform(name, `val`)
        }
        names = data.getUniformIntegerNames()
        for (i in names.indices) {
            val name = names[i]
            val `val` = data.getUniformInts(name)
            shader.setIntUniform(name, `val`)
        }
        names = data.getUniformBitmapNames()
        for (i in names.indices) {
            val name = names[i]
            val `val` = data.getUniformBitmapId(name)
            val androidContext = remoteContext as AndroidRemoteContext
            val bitmap = androidContext.mRemoteComposeState.getFromId(`val`) as Bitmap?
            val bitmapShader = BitmapShader(bitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            shader.setInputShader(name, bitmapShader)
        }
        getNativePaint().setShader(shader)
    }

    override fun setImageFilterQuality(quality: Int) {
        getNativePaint().isFilterBitmap = quality == 1
    }

    override fun setBlendMode(mode: Int) {
        getPaint().blendMode = remoteToBlendMode(mode)!!
    }

    override fun setAlpha(a: Float) {
        getNativePaint().setAlpha((255 * a).toInt())
    }

    override fun setStrokeMiter(miter: Float) {
        getNativePaint().strokeMiter = miter
    }

    override fun setStrokeJoin(join: Int) {
        getPaint().strokeJoin = Paint.Join.entries[join].toStrokeJoin()
    }

    override fun setFilterBitmap(filter: Boolean) {
        getNativePaint().isFilterBitmap = filter
    }

    override fun setAntiAlias(aa: Boolean) {
        getPaint().isAntiAlias = aa
    }

    override fun clear(mask: Long) {
        if ((mask and (1L shl PaintBundle.COLOR_FILTER)) != 0L) {
            getNativePaint().setColorFilter(null)
        }
    }

    override fun setLinearGradient(
        colors: IntArray,
        stops: FloatArray?,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        tileMode: Int,
    ) {
        getNativePaint()
            .setShader(
                LinearGradient(startX, startY, endX, endY, colors, stops, tileModes[tileMode])
            )
    }

    override fun setRadialGradient(
        colors: IntArray,
        stops: FloatArray?,
        centerX: Float,
        centerY: Float,
        radius: Float,
        tileMode: Int,
    ) {
        getNativePaint()
            .setShader(RadialGradient(centerX, centerY, radius, colors, stops, tileModes[tileMode]))
    }

    override fun setSweepGradient(
        colors: IntArray,
        stops: FloatArray?,
        centerX: Float,
        centerY: Float,
    ) {
        getNativePaint().setShader(SweepGradient(centerX, centerY, colors, stops))
    }

    override fun setColorFilter(color: Int, mode: Int) {
        val porterDuffMode = remoteToPorterDuffMode(mode)
        getNativePaint().setColorFilter(PorterDuffColorFilter(color, porterDuffMode))
    }

    private fun getNativePaint(): NativePaint = getPaint().asFrameworkPaint()

    private fun getShaderData(id: Int): ShaderData? {
        return remoteContext.mRemoteComposeState.getFromId(id) as ShaderData?
    }

    companion object {
        private const val TAG = "ComposePaintContext"
        private const val SYSTEM_FONTS_PATH: String = "/system/fonts/"
    }
}
