### PathCreate Illustration

The `PathCreate` operation begins the definition of a path, followed by `PathAppend` operations to add segments like lines or curves.

<div id="pathCreateContainer">
  <canvas id="pathCreateCanvas_v1" width="500" height="300" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('pathCreateCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 300);

    var startX = 100, startY = 200;
    var p1x = 200, p1y = 50;
    var p2x = 300, p2y = 250;
    var endX = 400, endY = 100;

    // The Path
    ctx.beginPath();
    ctx.moveTo(startX, startY);
    ctx.lineTo(p1x, p1y);
    ctx.bezierCurveTo(p2x, p2y, p2x, p2y, endX, endY);
    ctx.lineWidth = 5;
    ctx.strokeStyle = BLUE;
    ctx.stroke();

    // Start point (PathCreate)
    ctx.beginPath();
    ctx.arc(startX, startY, 8, 0, 2 * Math.PI);
    ctx.fillStyle = DARK_GRAY;
    ctx.fill();

    ctx.font = 'bold 20px Arial';
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('PathCreate (startX, startY)', startX - 20, startY + 35);
    ctx.font = '18px Arial';
    ctx.fillStyle = GRAY;
    ctx.fillText('followed by PathAppend segments...', 150, 280);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
