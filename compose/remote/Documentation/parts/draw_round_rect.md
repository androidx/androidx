### DrawRoundRect Illustration

The `DrawRoundRect` operation renders a rectangle with rounded corners, defined by its bounds and the radii `rx` and `ry`.

<div id="drawRoundRectContainer">
  <canvas id="drawRoundRectCanvas_v1" width="600" height="400" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('drawRoundRectCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 600, 400);

    var x = 100, y = 60, w = 300, h = 220, r = 50;

    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.quadraticCurveTo(x + w, y, x + w, y + r);
    ctx.lineTo(x + w, y + h - r);
    ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
    ctx.lineTo(x + r, y + h);
    ctx.quadraticCurveTo(x, y + h, x, y + h - r);
    ctx.lineTo(x, y + r);
    ctx.quadraticCurveTo(x, y, x + r, y);
    ctx.closePath();

    ctx.fillStyle = BLUE_TRANS;
    ctx.fill();
    ctx.lineWidth = 5;
    ctx.strokeStyle = BLUE;
    ctx.stroke();

    ctx.font = 'bold 22px Arial';
    ctx.fillStyle = DARK_GRAY;
    ctx.textAlign = 'left';
    ctx.fillText('rx, ry = ' + r, x + w + 15, y + r);
    
    ctx.font = '18px Arial';
    ctx.fillStyle = GRAY;
    ctx.textAlign = 'center';
    ctx.fillText('bounds (left, top, right, bottom)', x + w/2, y + h + 45);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>