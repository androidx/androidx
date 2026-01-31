### GraphicsLayer Illustration

The `GraphicsLayer` modifier applies advanced transformations and effects like rotation, scaling, and transparency to a component.

<div id="graphicsLayerContainer">
  <canvas id="graphicsLayerCanvas_v1" width="500" height="330" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('graphicsLayerCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 330);

    var rotation = 15;
    var scale = 1.2;
    var alpha = 0.7;

    ctx.save();
    ctx.translate(250, 165);
    ctx.rotate(rotation * Math.PI / 180);
    ctx.scale(scale, scale);
    ctx.globalAlpha = alpha;

    ctx.fillStyle = BLUE;
    ctx.fillRect(-85, -85, 170, 170);
    ctx.strokeStyle = BLUE;
    ctx.lineWidth = 3;
    ctx.strokeRect(-85, -85, 170, 170);
    ctx.restore();

    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 22px Arial';
    ctx.fillText('rotationZ: ' + rotation + '°', 15, 35);
    ctx.fillText('scaleX/Y: ' + scale, 15, 70);
    ctx.fillText('alpha: ' + alpha, 15, 105);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
