### MatrixSkew Illustration

The `MatrixSkew` operation applies a skew (shear) transformation to the coordinate system.

<div id="matrixSkewContainer">
  <canvas id="matrixSkewCanvas_v1" width="500" height="330" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var phase = 0;
  function draw() {
    var canvas = document.getElementById('matrixSkewCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 330);

    var skX = Math.sin(phase) * 0.8;
    var skY = Math.cos(phase * 0.6) * 0.3;
    phase += 0.02;

    var x = 120, y = 120, w = 80, h = 80;

    function drawHouse(ctx, x, y, w, h, color, alpha) {
      ctx.save();
      ctx.globalAlpha = alpha || 1.0;
      ctx.translate(x, y);
      // Body
      ctx.fillStyle = color;
      ctx.fillRect(0, 0, w, h);
      // Roof
      ctx.beginPath();
      ctx.moveTo(0, 0);
      ctx.lineTo(w / 2, -h * 0.5);
      ctx.lineTo(w, 0);
      ctx.closePath();
      ctx.fill();
      // Door
      ctx.fillStyle = 'white';
      ctx.fillRect(w * 0.35, h * 0.6, w * 0.3, h * 0.4);
      ctx.restore();
    }

    // Original
    ctx.strokeStyle = '#eee';
    ctx.lineWidth = 1;
    ctx.setLineDash([4, 4]);
    ctx.strokeRect(x, y, w, h);
    ctx.setLineDash([]);
    drawHouse(ctx, x, y, w, h, '#ddd', 0.5);
    
    ctx.font = '16px Arial';
    ctx.fillStyle = '#bbb';
    ctx.fillText('Original', x, y - 10);

    // Skewed
    ctx.save();
    ctx.translate(x, y);
    ctx.transform(1, skY, skX, 1, 0, 0);
    drawHouse(ctx, 0, 0, w, h, BLUE, 0.6);
    ctx.lineWidth = 2;
    ctx.strokeStyle = BLUE;
    ctx.strokeRect(0, 0, w, h);
    ctx.restore();

    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 22px Arial';
    ctx.fillText('skewX: ' + skX.toFixed(2), 35, 50);
    ctx.fillText('skewY: ' + skY.toFixed(2), 35, 80);

    requestAnimationFrame(draw);
  }
  draw();
})();
</script>