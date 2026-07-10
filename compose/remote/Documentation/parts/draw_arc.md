### DrawArc Illustration

The `DrawArc` operation renders an arc within a specified oval bounding box, starting from `startAngle` and sweeping through `sweepAngle` (degrees).

<div id="drawArcContainer">
  <canvas id="drawArcCanvas_v1" width="600" height="500" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('drawArcCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 600, 500);

    var cx = 300, cy = 250, rx = 175, ry = 125;
    var startAngle = 30;
    var sweepAngle = 240;

    // Bounding Oval
    ctx.beginPath();
    ctx.ellipse(cx, cy, rx, ry, 0, 0, 2 * Math.PI);
    ctx.strokeStyle = '#eee';
    ctx.setLineDash([10, 10]);
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.setLineDash([]);

    // Axes
    ctx.beginPath();
    ctx.moveTo(cx - rx - 20, cy);
    ctx.lineTo(cx + rx + 20, cy);
    ctx.moveTo(cx, cy - ry - 20);
    ctx.lineTo(cx, cy + ry + 20);
    ctx.strokeStyle = '#f8f8f8';
    ctx.stroke();

    // The Arc
    ctx.beginPath();
    ctx.ellipse(cx, cy, rx, ry, 0, (startAngle * Math.PI) / 180, ((startAngle + sweepAngle) * Math.PI) / 180, false);
    ctx.lineWidth = 8;
    ctx.strokeStyle = BLUE;
    ctx.stroke();

    // Start Angle Line
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    var sx = cx + rx * Math.cos(startAngle * Math.PI / 180);
    var sy = cy + ry * Math.sin(startAngle * Math.PI / 180);
    ctx.lineTo(sx, sy);
    ctx.strokeStyle = BLUE;
    ctx.lineWidth = 2;
    ctx.stroke();

    // End Angle Line
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    var ex = cx + rx * Math.cos((startAngle + sweepAngle) * Math.PI / 180);
    var ey = cy + ry * Math.sin((startAngle + sweepAngle) * Math.PI / 180);
    ctx.lineTo(ex, ey);
    ctx.strokeStyle = GRAY;
    ctx.stroke();

    // Labels
    ctx.font = 'bold 18px Arial';
    ctx.fillStyle = BLUE;
    ctx.textAlign = 'left';
    ctx.fillText('startAngle: ' + startAngle + '°', sx + (startAngle > 90 ? -150 : 10), sy + (sy > cy ? 20 : -10));
    
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('sweepAngle: ' + sweepAngle + '°', ex + (ex < cx ? -160 : 10), ey + (ey > cy ? 25 : -10));

    ctx.font = '14px Arial';
    ctx.fillStyle = GRAY;
    ctx.textAlign = 'right';
    ctx.fillText('0° (3 o\'clock)', cx + rx - 5, cy - 10);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
