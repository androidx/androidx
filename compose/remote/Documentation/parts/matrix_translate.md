### MatrixTranslate Illustration

The `MatrixTranslate` operation moves the coordinate system by `dx` and `dy`.

<div id="matrixTranslateContainer">
  <canvas id="matrixTranslateCanvas_v1" width="500" height="250" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var phase = 0;
  function draw() {
    var canvas = document.getElementById('matrixTranslateCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 250);

    // Dynamic DX/DY
    var dx = 100 + Math.sin(phase) * 100;
    var dy = 40 + Math.cos(phase * 0.5) * 30;
    phase += 0.02;

    var x = 30, y = 30, w = 80, h = 80;

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

    // Original (Reference)
    ctx.strokeStyle = '#eee';
    ctx.lineWidth = 1;
    ctx.setLineDash([4, 4]);
    ctx.strokeRect(x, y, w, h);
    ctx.setLineDash([]);
    drawHouse(ctx, x, y, w, h, '#ddd', 0.5);
    
    ctx.font = 'bold 18px Arial';
    ctx.fillStyle = '#bbb';
    ctx.fillText('Original', x, y - 10);

    // Transformed
    ctx.save();
    ctx.translate(dx, dy);
    drawHouse(ctx, x, y, w, h, BLUE, 0.6);
    ctx.lineWidth = 2;
    ctx.strokeStyle = BLUE;
    ctx.strokeRect(x, y, w, h);
    ctx.restore();

    // Translation Arrow
    ctx.beginPath();
    ctx.moveTo(x + w/2, y + h/2);
    ctx.lineTo(x + w/2 + dx, y + h/2 + dy);
    ctx.strokeStyle = GRAY;
    ctx.setLineDash([8, 5]);
    ctx.stroke();
    ctx.setLineDash([]);
    
    ctx.fillStyle = 'red';
    ctx.beginPath(); ctx.arc(x + w/2 + dx, y + h/2 + dy, 5, 0, Math.PI*2); ctx.fill();

    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 22px Arial';
    ctx.fillText('dx: ' + Math.round(dx) + ', dy: ' + Math.round(dy), 250, 40);
    
    requestAnimationFrame(draw);
  }
  draw();
})();
</script>