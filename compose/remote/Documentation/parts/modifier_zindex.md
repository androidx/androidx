### ZIndex Illustration

The `ZIndex` modifier determines the stacking order of overlapping components. Components with a higher Z-Index are drawn on top of those with a lower value.

<div id="zIndexContainer">
  <canvas id="zIndexCanvas_v1" width="500" height="300" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('zIndexCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#BBBBBB', DARK_GRAY = '#444444', BLUE_LIGHT = 'rgba(0, 71, 171, 0.6)';
    ctx.clearRect(0, 0, 500, 300);

    function drawBox(x, y, label, color, z) {
      ctx.fillStyle = color;
      ctx.fillRect(x, y, 150, 100);
      ctx.strokeStyle = '#fff';
      ctx.lineWidth = 2;
      ctx.strokeRect(x, y, 150, 100);
      
      ctx.fillStyle = 'white';
      ctx.font = 'bold 20px Arial';
      ctx.fillText(label, x + 20, y + 40);
      ctx.font = '16px Arial';
      ctx.fillText('Z-Index: ' + z, x + 20, y + 70);
    }

    // Stacked boxes illustration
    drawBox(100, 100, 'Box A', '#888', 1);
    drawBox(150, 130, 'Box B', BLUE_LIGHT, 2);
    drawBox(200, 160, 'Box C', BLUE, 3);

    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 22px Arial';
    ctx.fillText('Higher Z drawn last (on top)', 120, 50);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
