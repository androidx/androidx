### Border Modifier Illustration

The `Border` modifier draws a stroke around the component's edge with a specific width and corner radius.

<div id="borderModifierContainer">
  <canvas id="borderModifierCanvas_v1" width="500" height="250" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('borderModifierCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    var x=80, y=50, w=340, h=150, r=35, bw=8;

    ctx.clearRect(0, 0, 500, 250);

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

    ctx.lineWidth = bw;
    ctx.strokeStyle = BLUE;
    ctx.stroke();

    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 22px Arial';
    ctx.fillText('borderWidth: ' + bw, x + w/2 - 75, y + h/2);
    ctx.fillText('roundedCorner: ' + r, x + w - 50, y + 15);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
