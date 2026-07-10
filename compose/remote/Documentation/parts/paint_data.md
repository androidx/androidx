### PaintData Illustration

The `PaintData` operation encodes properties like style (Fill/Stroke), color, and stroke width.

#### Fill vs Stroke
<div id="paintStylesContainer">
  <canvas id="paintStylesCanvas_v1" width="500" height="250" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('paintStylesCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 250);

    // STYLE_FILL
    ctx.beginPath();
    ctx.arc(125, 125, 65, 0, 2 * Math.PI);
    ctx.fillStyle = BLUE;
    ctx.fill();
    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 22px Arial';
    ctx.fillText('STYLE_FILL', 65, 220);

    // STYLE_STROKE
    ctx.beginPath();
    ctx.arc(375, 125, 65, 0, 2 * Math.PI);
    ctx.lineWidth = 7;
    ctx.strokeStyle = BLUE;
    ctx.stroke();
    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 22px Arial';
    ctx.fillText('STYLE_STROKE', 300, 220);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
