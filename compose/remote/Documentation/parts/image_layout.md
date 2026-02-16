### Image Layout Algorithm

Image Layout handles the display and scaling of bitmap resources.

#### Visual Illustration (Scale Modes)
<div id="imageLayoutContainer">
  <canvas id="imageLayoutCanvas_v1" width="500" height="280" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('imageLayoutCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 280);

    function drawScaleDemo(x, y, label, mode) {
      var vw = 120, vh = 120; // Viewport
      var iw = 160, ih = 80;  // Image (wider than high)

      ctx.save();
      ctx.translate(x, y);

      // Label
      ctx.fillStyle = DARK_GRAY;
      ctx.font = 'bold 16px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(label, vw/2, -15);

      // Viewport bounds
      ctx.strokeStyle = '#eee';
      ctx.setLineDash([4, 4]);
      ctx.strokeRect(0, 0, vw, vh);
      ctx.setLineDash([]);

      ctx.save();
      ctx.beginPath();
      ctx.rect(0, 0, vw, vh);
      ctx.clip();

      var dx, dy, dw, dh;

      if (mode === 'FIT') {
        var scale = Math.min(vw / iw, vh / ih);
        dw = iw * scale;
        dh = ih * scale;
        dx = (vw - dw) / 2;
        dy = (vh - dh) / 2;
      } else if (mode === 'FILL') {
        dx = 0; dy = 0; dw = vw; dh = vh;
      } else if (mode === 'CENTER_CROP') {
        var scale = Math.max(vw / iw, vh / ih);
        dw = iw * scale;
        dh = ih * scale;
        dx = (vw - dw) / 2;
        dy = (vh - dh) / 2;
      }

      // Draw Placeholder Image
      ctx.fillStyle = BLUE;
      ctx.fillRect(dx, dy, dw, dh);
      ctx.strokeStyle = 'white';
      ctx.lineWidth = 2;
      ctx.strokeRect(dx + 5, dy + 5, dw - 10, dh - 10);
      ctx.beginPath();
      ctx.moveTo(dx, dy); ctx.lineTo(dx + dw, dy + dh);
      ctx.moveTo(dx + dw, dy); ctx.lineTo(dx, dy + dh);
      ctx.stroke();

      ctx.restore();

      // Border
      ctx.strokeStyle = DARK_GRAY;
      ctx.lineWidth = 2;
      ctx.strokeRect(0, 0, vw, vh);

      ctx.restore();
    }

    drawScaleDemo(40, 60, 'FIT', 'FIT');
    drawScaleDemo(190, 60, 'CENTER_CROP', 'CENTER_CROP');
    drawScaleDemo(340, 60, 'FILL', 'FILL');
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>

#### Sizing

1.  **Wrap Content**:
    -   If sizing is not constrained, the component takes the intrinsic width and height of the source bitmap.

2.  **Scaling**:
    -   Calculates a destination rectangle within the component's bounds based on the `scaleType`.
    -   Supports typical scaling modes like `Fit`, `CenterCrop`, and `Fill`.

#### Rendering

-   Applies alpha transparency.
-   Clips the bitmap to the component's bounds if the scaled image exceeds them.
