### DrawCircle Illustration

The `DrawCircle` operation renders a circle centered at `(centerX, centerY)` with a given `radius`.

<div id="drawCircleContainer">
  <canvas id="drawCircleCanvas_v1" width="500" height="500" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('drawCircleCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 500);
    
    var cx = 250, cy = 250, radius = 125;

    // Main Circle
    ctx.beginPath();
    ctx.arc(cx, cy, radius, 0, 2 * Math.PI, false);
    ctx.fillStyle = BLUE_TRANS;
    ctx.fill();
    ctx.lineWidth = 5;
    ctx.strokeStyle = BLUE;
    ctx.stroke();

    // Center Anchor
    ctx.beginPath();
    ctx.arc(cx, cy, 7, 0, 2 * Math.PI, false);
    ctx.fillStyle = DARK_GRAY;
    ctx.fill();

    // Construction Line
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(cx + radius, cy);
    ctx.strokeStyle = GRAY;
    ctx.setLineDash([12, 8]);
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.setLineDash([]);

    // Text
    ctx.font = 'bold 22px Arial';
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('Center (cx, cy)', cx + 15, cy - 15);
    ctx.font = '20px Arial';
    ctx.fillText('radius: ' + radius, cx + 20, cy + 35);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
