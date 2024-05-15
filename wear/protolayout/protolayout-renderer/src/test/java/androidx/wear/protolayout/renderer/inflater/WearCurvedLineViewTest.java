package androidx.wear.protolayout.renderer.inflater;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.Shader;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.proto.ColorProto.ColorProp;
import androidx.wear.protolayout.proto.ColorProto.ColorStop;
import androidx.wear.protolayout.proto.ColorProto.SweepGradient;
import androidx.wear.protolayout.proto.DimensionProto.DegreesProp;
import androidx.wear.protolayout.proto.TypesProto.FloatProp;
import androidx.wear.protolayout.renderer.inflater.WearCurvedLineView.ArcSegment.CapPosition;
import androidx.wear.protolayout.renderer.inflater.WearCurvedLineView.SweepGradientHelper;

import com.google.common.truth.Expect;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WearCurvedLineViewTest {
    @Rule public final Expect expect = Expect.create();

    @NonNull RectF testBounds = new RectF(0f, 10f, 100f, 200f);

    @Test
    public void sweepGradientHelper_tooFewColors_throws() {
        SweepGradient sgProto =
                SweepGradient.newBuilder().addColorStops(colorStop(Color.RED, 0f)).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    SweepGradientHelper unused = new SweepGradientHelper(sgProto);
                });
    }

    @Test
    public void sweepGradientHelper_tooManyColors_throws() {
        int numColors = 50;
        SweepGradient.Builder sgBuilder = SweepGradient.newBuilder();
        for (int i = 0; i < numColors; i++) {
            sgBuilder.addColorStops(colorStop(Color.RED + i, (float) i / numColors));
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    SweepGradientHelper unused = new SweepGradientHelper(sgBuilder.build());
                });
    }

    @Test
    public void sweepGradientHelper_missingOffsets_throws() {
        SweepGradient sgProto =
                SweepGradient.newBuilder()
                        .addColorStops(colorStop(Color.RED, 0f))
                        .addColorStops(colorStop(Color.BLUE))
                        .addColorStops(colorStop(Color.GREEN, 1f))
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    SweepGradientHelper unused = new SweepGradientHelper(sgProto);
                });
    }

    @Test
    public void sweepGradientHelper_getShader_invalidAngleSpan_throws() {
        SweepGradientHelper sgHelper = new SweepGradientHelper(basicSweepGradientProto());

        float startAngle = 10f;
        float endAngle = startAngle + 400f;

        assertThrows(
                IllegalArgumentException.class,
                () -> sgHelper.getShader(testBounds, startAngle, endAngle, 0f, CapPosition.NONE));
    }

    @Test
    public void sweepGradientHelper_getColorAtSetOffsets() {
        // Gradient with colors [Red, Blue, Green] at offsets [0, 0.5, 1]
        SweepGradientHelper sgHelper = new SweepGradientHelper(basicSweepGradientProto());

        expect.that(sgHelper.getColor(0f)).isEqualTo(Color.RED);
        expect.that(sgHelper.getColor(180f)).isEqualTo(Color.BLUE);
        expect.that(sgHelper.getColor(360f)).isEqualTo(Color.GREEN);
    }

    @Test
    public void sweepGradientHelper_unsortedStops_getColorAtSetOffsets() {
        // Gradient with colors [Red, Green, Blue] at offsets [0, 1, 0.5]
        SweepGradient gradProto =
                SweepGradient.newBuilder()
                        .addColorStops(colorStop(Color.RED, 0f))
                        .addColorStops(colorStop(Color.GREEN, 1f))
                        .addColorStops(colorStop(Color.BLUE, 0.5f))
                        .build();
        SweepGradientHelper sgHelper = new SweepGradientHelper(gradProto);

        expect.that(sgHelper.getColor(0f)).isEqualTo(Color.RED);
        expect.that(sgHelper.getColor(180f)).isEqualTo(Color.BLUE);
        expect.that(sgHelper.getColor(360f)).isEqualTo(Color.GREEN);
    }

    @Test
    public void sweepGradientHelper_getColor_customStartAndEndAngles() {
        float startAngle = 180f;
        float endAngle = 720f;

        // Gradient with colors [Red, Blue, Green] at offsets [0, 0.5, 1]
        SweepGradientHelper sgHelper =
                new SweepGradientHelper(basicSweepGradientProto(startAngle, endAngle));

        // RED before and at startAngle.
        expect.that(sgHelper.getColor(0f)).isEqualTo(Color.RED);
        expect.that(sgHelper.getColor(startAngle)).isEqualTo(Color.RED);
        // BLUE in the middle angle.
        expect.that(sgHelper.getColor((startAngle + endAngle) / 2f)).isEqualTo(Color.BLUE);
        // GREEN at the endAngle and after.
        expect.that(sgHelper.getColor(endAngle)).isEqualTo(Color.GREEN);
        expect.that(sgHelper.getColor(888f)).isEqualTo(Color.GREEN);
    }

    @Test
    public void sweepGradientHelper_getInterpolatedColor() {
        SweepGradientHelper sgHelper = new SweepGradientHelper(basicSweepGradientProto());

        float angle1 = 90f;
        expect.that(sgHelper.getColor(angle1))
                .isEqualTo(sgHelper.interpolateColors(Color.RED, 0f, Color.BLUE, 180f, angle1));

        float angle2 = 213f;
        expect.that(sgHelper.getColor(angle2))
                .isEqualTo(sgHelper.interpolateColors(Color.BLUE, 180f, Color.GREEN, 360f, angle2));
    }

    @Test
    public void sweepGradientHelper_getInterpolatedColor_noOffsets() {
        SweepGradient sgProto =
                SweepGradient.newBuilder()
                        .addColorStops(colorStop(Color.RED))
                        .addColorStops(colorStop(Color.BLUE))
                        .addColorStops(colorStop(Color.GREEN))
                        .build();
        SweepGradientHelper sgHelper = new SweepGradientHelper(sgProto);

        float angle1 = 90f;
        expect.that(sgHelper.getColor(angle1))
                .isEqualTo(sgHelper.interpolateColors(Color.RED, 0f, Color.BLUE, 180f, angle1));

        float angle2 = 213f;
        expect.that(sgHelper.getColor(angle2))
                .isEqualTo(sgHelper.interpolateColors(Color.BLUE, 180f, Color.GREEN, 360f, angle2));
    }

    @Test
    public void sweepGradientHelper_shaderIsRotated() {
        SweepGradientHelper sgHelper = new SweepGradientHelper(basicSweepGradientProto());
        float rotationAngle = 63f;
        Matrix rotatedMatrix = new Matrix();
        rotatedMatrix.postRotate(
                rotationAngle,
                (testBounds.left + testBounds.right) / 2f,
                (testBounds.top + testBounds.bottom) / 2f);

        Shader generatedShader =
                sgHelper.getShader(testBounds, 180f, 360f, rotationAngle, CapPosition.NONE);
        assertThat(generatedShader).isInstanceOf(android.graphics.SweepGradient.class);
        Matrix generatedMatrix = new Matrix();
        generatedShader.getLocalMatrix(generatedMatrix);
        assertThat(rotatedMatrix).isEqualTo(generatedMatrix);
    }

    @Test
    public void sweepGradientHelper_colorSetToOpaque() {
        final int color0 = 0x12666666;
        final int color90 = 0xFD123456;
        final int color180 = 0xFF654321;
        final int noAlphaMask = 0x00FFFFFF;
        SweepGradient sgProto =
                SweepGradient.newBuilder()
                        .addColorStops(colorStop(color0))
                        .addColorStops(colorStop(color90))
                        .addColorStops(colorStop(color180))
                        .setEndAngle(degrees(180f))
                        .build();
        SweepGradientHelper sgHelper = new SweepGradientHelper(sgProto);

        int resolvedColor0 = sgHelper.getColor(0f);
        expect.that(Color.alpha(resolvedColor0)).isEqualTo(0xFF);
        expect.that(resolvedColor0 & noAlphaMask).isEqualTo(color0 & noAlphaMask);

        int resolvedColor90 = sgHelper.getColor(90f);
        expect.that(Color.alpha(resolvedColor90)).isEqualTo(0xFF);
        expect.that(resolvedColor90 & noAlphaMask).isEqualTo(color90 & noAlphaMask);

        int resolvedColor180 = sgHelper.getColor(180f);
        expect.that(Color.alpha(resolvedColor180)).isEqualTo(0xFF);
        expect.that(resolvedColor180 & noAlphaMask).isEqualTo(color180 & noAlphaMask);
    }

    /** Gradient with colors [Red, Blue, Green] at offsets [0, 0.5, 1] and given angles. */
    private SweepGradient basicSweepGradientProto(float startAngle, float endAngle) {
        return SweepGradient.newBuilder()
                .addColorStops(colorStop(Color.RED, 0f))
                .addColorStops(colorStop(Color.BLUE, 0.5f))
                .addColorStops(colorStop(Color.GREEN, 1f))
                .setStartAngle(degrees(startAngle))
                .setEndAngle(degrees(endAngle))
                .build();
    }

    /** Gradient with colors [Red, Blue, Green] at offsets [0, 0.5, 1]. */
    private SweepGradient basicSweepGradientProto() {
        return SweepGradient.newBuilder()
                .addColorStops(colorStop(Color.RED, 0f))
                .addColorStops(colorStop(Color.BLUE, 0.5f))
                .addColorStops(colorStop(Color.GREEN, 1f))
                .build();
    }

    private ColorProp staticColor(int value) {
        return ColorProp.newBuilder().setArgb(value).build();
    }

    private FloatProp staticFloat(float value) {
        return FloatProp.newBuilder().setValue(value).build();
    }

    private DegreesProp degrees(float value) {
        return DegreesProp.newBuilder().setValue(value).build();
    }

    private ColorStop colorStop(int color, float offset) {
        return ColorStop.newBuilder()
                .setColor(staticColor(color))
                .setOffset(staticFloat(offset))
                .build();
    }

    private ColorStop colorStop(int color) {
        return ColorStop.newBuilder().setColor(staticColor(color)).build();
    }
}
