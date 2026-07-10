### DrawRect Illustration

The `DrawRect` operation renders a rectangle defined by its `left`, `top`, `right`, and `bottom` coordinates.

<div id="drawRectContainer">
  <canvas id="drawRectCanvas_v1" width="500" height="500" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('drawRectCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 500);

    var left = 100, top = 100, right = 400, bottom = 350;
    var width = right - left;
    var height = bottom - top;

    ctx.fillStyle = BLUE_TRANS;
    ctx.fillRect(left, top, width, height);
    ctx.lineWidth = 5;
    ctx.strokeStyle = BLUE;
    ctx.strokeRect(left, top, width, height);

    ctx.font = 'bold 20px Arial';
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('left, top (' + left + ', ' + top + ')', left - 80, top - 25);
    ctx.fillText('right, bottom (' + right + ', ' + bottom + ')', right - 180, bottom + 50);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
