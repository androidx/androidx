### MatrixScale Illustration

The `MatrixScale` operation scales the coordinate system by `scaleX` and `scaleY` relative to a pivot point `(pivotX, pivotY)`.

<div id="matrixScaleContainer">
  <canvas id="matrixScaleCanvas_v1" width="500" height="330" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var phase = 0;
  function draw() {
    var canvas = document.getElementById('matrixScaleCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 330);

    var px = 100, py = 100;
    var sx = 1.2 + Math.sin(phase) * 0.8;
    var sy = 1.2 + Math.cos(phase * 0.7) * 0.4;
    phase += 0.02;

    function drawHouse(ctx, x, y, w, h, color, alpha) {
      ctx.save();
      ctx.globalAlpha = alpha || 1.0;
      ctx.translate(x, y);
      // Body
      ctx.fillStyle = color;
      ctx.fillRect(0, h * 0.4, w, h * 0.6);
      // Roof
      ctx.beginPath();
      ctx.moveTo(0, h * 0.4);
      ctx.lineTo(w / 2, 0);
      ctx.lineTo(w, h * 0.4);
      ctx.closePath();
      ctx.fill();
      // Door
      ctx.fillStyle = 'white';
      ctx.fillRect(w * 0.35, h * 0.75, w * 0.3, h * 0.25);
      ctx.restore();
    }

    // Original
    ctx.strokeStyle = '#eee';
    ctx.lineWidth = 1;
    ctx.setLineDash([4, 4]);
    ctx.strokeRect(px, py, 80, 80);
    ctx.setLineDash([]);
    drawHouse(ctx, px, py, 80, 80, '#ddd', 0.5);

    ctx.font = '16px Arial';
    ctx.fillStyle = '#bbb';
    ctx.fillText('Original (80x80)', px, py - 12);

    // Scaled
    ctx.save();
    ctx.translate(px, py);
    ctx.scale(sx, sy);
    ctx.translate(-px, -py);
    
    drawHouse(ctx, px, py, 80, 80, BLUE, 0.6);
    ctx.lineWidth = 2;
    ctx.strokeStyle = BLUE;
    ctx.strokeRect(px, py, 80, 80);
    ctx.restore();

    // Pivot Anchor
    ctx.beginPath();
    ctx.arc(px, py, 7, 0, 2 * Math.PI);
    ctx.fillStyle = DARK_GRAY;
    ctx.fill();
    
    // Labels
    ctx.font = 'bold 20px Arial';
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('Pivot (px, py)', px + 15, py - 8);
    
    ctx.font = 'bold 22px Arial';
    ctx.fillText('scaleX: ' + sx.toFixed(2), 280, 160);
    ctx.fillText('scaleY: ' + sy.toFixed(2), 280, 200);

    requestAnimationFrame(draw);
  }
  draw();
})();
</script>