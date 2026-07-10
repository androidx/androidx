### Background Modifier Illustration

The `Background` modifier applies a color and shape behind a component.

#### Rectangle Shape
<div id="bgRectContainer">
  <canvas id="bgRectCanvas_v1" width="240" height="240" style="border:1px solid #ccc; background: #fff; display: inline-block; margin: 10px;"></canvas>
</div>

#### Circle Shape
<div id="bgCircleContainer">
  <canvas id="bgCircleCanvas_v1" width="240" height="240" style="border:1px solid #ccc; background: #fff; display: inline-block; margin: 10px;"></canvas>
</div>

<script>
(function() {
  function setup(id, type) {
    function draw() {
      var canvas = document.getElementById(id);
      if (!canvas) { setTimeout(draw, 100); return; }
      var ctx = canvas.getContext('2d');
      if (!ctx) return;

      var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
      ctx.clearRect(0, 0, 240, 240);
      ctx.fillStyle = BLUE;

      if (type === 'rect') {
        ctx.fillRect(30, 30, 180, 180);
      } else {
        ctx.beginPath();
        ctx.arc(120, 120, 90, 0, 2 * Math.PI);
        ctx.fill();
      }
      
      ctx.fillStyle = DARK_GRAY;
      ctx.font = 'bold 22px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(type.toUpperCase(), 120, 230);
    }
    if (document.readyState === 'complete') { draw(); } 
    else { window.addEventListener('load', draw); setTimeout(draw, 500); }
  }
  setup('bgRectCanvas_v1', 'rect');
  setup('bgCircleCanvas_v1', 'circle');
})();
</script>
