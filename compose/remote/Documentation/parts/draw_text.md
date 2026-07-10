### DrawText Illustration

The `DrawText` operation renders a run of text at a specific `(x, y)` coordinate.

<div id="drawTextContainer">
  <canvas id="drawTextCanvas_v1" width="500" height="250" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('drawTextCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 250);

    var x = 50, y = 150;
    ctx.font = '60px Arial';
    ctx.fillStyle = BLUE;
    ctx.fillText('Hello World', x, y);

    // Anchor
    ctx.beginPath();
    ctx.arc(x, y, 7, 0, 2 * Math.PI);
    ctx.fillStyle = DARK_GRAY;
    ctx.fill();
    
    ctx.font = 'bold 22px Arial';
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('Origin (x, y)', x, y + 50);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>