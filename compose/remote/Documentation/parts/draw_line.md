### DrawLine Illustration

The `DrawLine` operation renders a single straight line segment between `(startX, startY)` and `(endX, endY)`.

<div id="drawLineContainer">
  <canvas id="drawLineCanvas_v1" width="500" height="500" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('drawLineCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 500);

    var x1 = 100, y1 = 100, x2 = 400, y2 = 400;

    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);
    ctx.lineWidth = 10;
    ctx.strokeStyle = BLUE;
    ctx.lineCap = 'round';
    ctx.stroke();

    ctx.font = 'bold 22px Arial';
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('Start (' + x1 + ', ' + y1 + ')', x1 - 40, y1 - 30);
    ctx.fillText('End (' + x2 + ', ' + y2 + ')', x2 - 120, y2 + 50);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
