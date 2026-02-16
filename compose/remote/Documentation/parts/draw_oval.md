### DrawOval Illustration

The `DrawOval` operation renders an oval that fits within the specified bounding box.

<div id="drawOvalContainer">
  <canvas id="drawOvalCanvas_v1" width="500" height="500" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('drawOvalCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 500);

    var cx = 250, cy = 250, rx = 175, ry = 100;

    // Main Oval
    ctx.beginPath();
    ctx.ellipse(cx, cy, rx, ry, 0, 0, 2 * Math.PI);
    ctx.fillStyle = BLUE_TRANS;
    ctx.fill();
    ctx.lineWidth = 5;
    ctx.strokeStyle = BLUE;
    ctx.stroke();

    // Bounding Box
    ctx.setLineDash([12, 12]);
    ctx.strokeStyle = GRAY;
    ctx.lineWidth = 2;
    ctx.strokeRect(cx - rx, cy - ry, rx * 2, ry * 2);
    ctx.setLineDash([]);
    
    // Labels
    ctx.font = 'bold 22px Arial';
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('left, top', cx - rx, cy - ry - 15);
    ctx.fillText('right, bottom', cx + rx - 140, cy + ry + 40);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>