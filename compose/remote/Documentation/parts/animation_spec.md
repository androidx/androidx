### AnimationSpec Illustration

The `AnimationSpec` defines easing curves and durations for motion and visibility transitions.

#### Easing Curves (Cubic Bezier)
<div id="animationSpecContainer">
  <canvas id="animationSpecCanvas_v1" width="500" height="250" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('animationSpecCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 250);

    function drawCurve(x, y, w, h, cp1x, cp1y, cp2x, cp2y, label) {
      ctx.strokeStyle = '#eee';
      ctx.lineWidth = 2;
      ctx.strokeRect(x, y, w, h);
      
      ctx.beginPath();
      ctx.moveTo(x, y + h);
      ctx.bezierCurveTo(x + cp1x*w, y + h - cp1y*h, x + cp2x*w, y + h - cp2y*h, x + w, y);
      ctx.lineWidth = 5;
      ctx.strokeStyle = BLUE;
      ctx.stroke();
      
      ctx.fillStyle = DARK_GRAY;
      ctx.font = 'bold 18px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(label, x + w/2, y + h + 30);
    }

    drawCurve(35, 35, 100, 100, 0.4, 0.0, 0.2, 1.0, "Standard");
    drawCurve(185, 35, 100, 100, 0.4, 0.0, 1.0, 1.0, "Accelerate");
    drawCurve(335, 35, 100, 100, 0.0, 0.0, 0.2, 1.0, "Decelerate");
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
