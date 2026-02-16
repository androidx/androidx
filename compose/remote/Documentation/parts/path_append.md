### PathAppend Illustration

The `PathAppend` operation adds segments to an existing path.

#### LineTo
Simple straight line segment.
<div id="pathAppendLineContainer">
  <canvas id="pathAppendLineCanvas" width="500" height="120" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

#### QuadraticTo
A curve with a single control point. The dashed lines show the tangents from the endpoints.
<div id="pathAppendQuadContainer">
  <canvas id="pathAppendQuadCanvas" width="500" height="200" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

#### CubicTo
A curve with two control points, allowing for more complex shapes like "S" curves.
<div id="pathAppendCubicContainer">
  <canvas id="pathAppendCubicCanvas" width="500" height="250" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';

  function drawPoint(ctx, x, y, label, align) {
    ctx.beginPath();
    ctx.arc(x, y, 5, 0, Math.PI * 2);
    ctx.fillStyle = DARK_GRAY;
    ctx.fill();
    ctx.font = '14px Arial';
    if (align === 'right') {
      ctx.textAlign = 'right';
      ctx.fillText(label, x - 10, y + 5);
    } else {
      ctx.textAlign = 'left';
      ctx.fillText(label, x + 10, y + 5);
    }
  }

  function setupLine() {
    var canvas = document.getElementById('pathAppendLineCanvas');
    if (!canvas) { setTimeout(setupLine, 100); return; }
    var ctx = canvas.getContext('2d');
    var x1 = 100, y1 = 60, x2 = 400, y2 = 60;
    ctx.clearRect(0, 0, 500, 120);
    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);
    ctx.lineWidth = 5;
    ctx.strokeStyle = BLUE;
    ctx.stroke();
    drawPoint(ctx, x1, y1, 'Start', 'right');
    drawPoint(ctx, x2, y2, 'End');
  }

  function setupQuad() {
    var canvas = document.getElementById('pathAppendQuadCanvas');
    if (!canvas) { setTimeout(setupQuad, 100); return; }
    var ctx = canvas.getContext('2d');
    var x1 = 100, y1 = 150, cx = 250, cy = 30, x2 = 400, y2 = 150;
    ctx.clearRect(0, 0, 500, 200);
    
    // Tangents
    ctx.setLineDash([5, 5]);
    ctx.strokeStyle = GRAY;
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(x1, y1); ctx.lineTo(cx, cy); ctx.lineTo(x2, y2);
    ctx.stroke();
    ctx.setLineDash([]);

    // Curve
    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.quadraticCurveTo(cx, cy, x2, y2);
    ctx.lineWidth = 5;
    ctx.strokeStyle = BLUE;
    ctx.stroke();

    drawPoint(ctx, x1, y1, 'Start', 'right');
    drawPoint(ctx, x2, y2, 'End');
    drawPoint(ctx, cx, cy, 'Control Point');
  }

  function setupCubic() {
    var canvas = document.getElementById('pathAppendCubicCanvas');
    if (!canvas) { setTimeout(setupCubic, 100); return; }
    var ctx = canvas.getContext('2d');
    var x1 = 80, y1 = 180, c1x = 120, c1y = 20, c2x = 380, c2y = 230, x2 = 420, y2 = 70;
    ctx.clearRect(0, 0, 500, 250);

    // Tangents
    ctx.setLineDash([5, 5]);
    ctx.strokeStyle = GRAY;
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(x1, y1); ctx.lineTo(c1x, c1y);
    ctx.moveTo(x2, y2); ctx.lineTo(c2x, c2y);
    ctx.stroke();
    ctx.setLineDash([]);

    // Curve
    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.bezierCurveTo(c1x, c1y, c2x, c2y, x2, y2);
    ctx.lineWidth = 5;
    ctx.strokeStyle = BLUE;
    ctx.stroke();

    drawPoint(ctx, x1, y1, 'Start', 'right');
    drawPoint(ctx, x2, y2, 'End');
    drawPoint(ctx, c1x, c1y, 'CP1');
    drawPoint(ctx, c2x, c2y, 'CP2');
  }

  function run() { setupLine(); setupQuad(); setupCubic(); }
  if (document.readyState === 'complete') { run(); } 
  else { window.addEventListener('load', run); setTimeout(run, 500); }
})();
</script>