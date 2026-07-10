### MatrixFromPath Illustration

The `MatrixFromPath` operation calculates a matrix that aligns an object to a specific point and tangent along a path.

<div id="matrixFromPathContainer">
  <canvas id="matrixFromPathCanvas_v1" width="500" height="300" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('matrixFromPathCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 300);

    var p0 = {x: 50,  y: 200};
    var p1 = {x: 150, y: 50};
    var p2 = {x: 350, y: 250};
    var p3 = {x: 450, y: 100};

    // Reference Path
    ctx.beginPath();
    ctx.moveTo(p0.x, p0.y);
    ctx.bezierCurveTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
    ctx.strokeStyle = '#eee';
    ctx.lineWidth = 3;
    ctx.stroke();

    function getBezier(t) {
      var cx = 3 * (p1.x - p0.x), bx = 3 * (p2.x - p1.x) - cx, ax = p3.x - p0.x - cx - bx;
      var cy = 3 * (p1.y - p0.y), by = 3 * (p2.y - p1.y) - cy, ay = p3.y - p0.y - cy - by;
      var x = (ax*t*t*t) + (bx*t*t) + (cx*t) + p0.x;
      var y = (ay*t*t*t) + (by*t*t) + (cy*t) + p0.y;
      var dx = (3*ax*t*t) + (2*bx*t) + cx;
      var dy = (3*ay*t*t) + (2*by*t) + cy;
      return {x: x, y: y, angle: Math.atan2(dy, dx)};
    }

    var t = 0.6;
    var state = getBezier(t);

    // Tangent Construction Line
    ctx.beginPath();
    ctx.moveTo(state.x - 50*Math.cos(state.angle), state.y - 50*Math.sin(state.angle));
    ctx.lineTo(state.x + 50*Math.cos(state.angle), state.y + 50*Math.sin(state.angle));
    ctx.strokeStyle = GRAY;
    ctx.setLineDash([5, 5]);
    ctx.stroke();
    ctx.setLineDash([]);

    // Aligned Object (Arrow)
    ctx.save();
    ctx.translate(state.x, state.y);
    ctx.rotate(state.angle);
    
    ctx.beginPath();
    ctx.moveTo(-30, -15);
    ctx.lineTo(30, 0);
    ctx.lineTo(-30, 15);
    ctx.fillStyle = BLUE;
    ctx.fill();
    ctx.restore();

    // Anchor point
    ctx.beginPath();
    ctx.arc(state.x, state.y, 6, 0, 2 * Math.PI);
    ctx.fillStyle = DARK_GRAY;
    ctx.fill();

    ctx.font = 'bold 20px Arial';
    ctx.fillStyle = DARK_GRAY;
    ctx.textAlign = 'center';
    ctx.fillText('Matrix aligned to Path at t=' + t, 250, 40);
    ctx.font = '16px Arial';
    ctx.fillText('Object rotation = path tangent', state.x, state.y + 45);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
