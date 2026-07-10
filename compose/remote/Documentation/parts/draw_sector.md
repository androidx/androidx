### DrawSector Illustration

The `DrawSector` operation renders a "pie" shape within a specified oval bounding box, starting from `startAngle` and sweeping through `sweepAngle`.

<div id="drawSectorContainer">
  <canvas id="drawSectorCanvas_v1" width="600" height="500" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('drawSectorCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 600, 500);

    var cx = 300, cy = 250, rx = 175, ry = 125;
    var startAngle = 45;
    var sweepAngle = 110;

    // Bounding Oval
    ctx.beginPath();
    ctx.ellipse(cx, cy, rx, ry, 0, 0, 2 * Math.PI);
    ctx.strokeStyle = '#eee';
    ctx.setLineDash([10, 10]);
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.setLineDash([]);

    // The Sector
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.ellipse(cx, cy, rx, ry, 0, (startAngle * Math.PI) / 180, ((startAngle + sweepAngle) * Math.PI) / 180, false);
    ctx.closePath();
    ctx.fillStyle = BLUE_TRANS;
    ctx.fill();
    ctx.lineWidth = 4;
    ctx.strokeStyle = BLUE;
    ctx.stroke();

    // Start Angle Indicator
    var sx = cx + rx * Math.cos(startAngle * Math.PI / 180);
    var sy = cy + ry * Math.sin(startAngle * Math.PI / 180);
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(sx, sy);
    ctx.strokeStyle = BLUE;
    ctx.lineWidth = 2;
    ctx.stroke();

    // End Angle Indicator
    var ex = cx + rx * Math.cos((startAngle + sweepAngle) * Math.PI / 180);
    var ey = cy + ry * Math.sin((startAngle + sweepAngle) * Math.PI / 180);
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(ex, ey);
    ctx.strokeStyle = GRAY;
    ctx.stroke();

    // Center point
    ctx.beginPath();
    ctx.arc(cx, cy, 5, 0, 2 * Math.PI);
    ctx.fillStyle = DARK_GRAY;
    ctx.fill();

    // Labels
    ctx.font = 'bold 18px Arial';
    ctx.textAlign = 'left';
    ctx.fillStyle = BLUE;
    ctx.fillText('startAngle: ' + startAngle + '°', sx + 10, sy + 15);
    
    ctx.textAlign = 'left';
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('sweepAngle: ' + sweepAngle + '°', ex - 120, ey + 65);

    ctx.textAlign = 'center';
    ctx.font = '14px Arial';
    ctx.fillStyle = GRAY;
    ctx.fillText('Center (cx, cy)', cx, cy - 15);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>