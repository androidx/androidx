### ShaderData Illustration

The `ShaderData` operation defines complex color effects like gradients or image-based patterns.

#### Linear Gradient Example
<div id="shaderGradientContainer">
  <canvas id="shaderGradientCanvas_v1" width="500" height="160" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('shaderGradientCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 160);

    var gradient = ctx.createLinearGradient(80, 0, 420, 0);
    gradient.addColorStop(0, BLUE);
    gradient.addColorStop(1, GRAY);

    ctx.fillStyle = gradient;
    ctx.fillRect(80, 30, 340, 100);
    
    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 20px Arial';
    ctx.fillText('Start Color (Blue)', 60, 150);
    ctx.fillText('End Color (Gray)', 320, 150);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
