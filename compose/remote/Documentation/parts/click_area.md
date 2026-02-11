### ClickArea Illustration

The `ClickArea` operation defines an interactive rectangular region. This region can be larger than the visible component to make it easier to hit on touch devices.

<div id="clickAreaContainer">
  <canvas id="clickAreaCanvas_v1" width="500" height="250" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('clickAreaCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 250);

    var cx=250, cy=125, cw=80, ch=40;
    var hitW=160, hitH=100;

    // Visible Component
    ctx.fillStyle = BLUE;
    ctx.fillRect(cx - cw/2, cy - ch/2, cw, ch);
    ctx.fillStyle = 'white';
    ctx.font = '14px Arial';
    ctx.textAlign = 'center';
    ctx.fillText('Visible', cx, cy + 5);

    // Hit Area (ClickArea)
    ctx.strokeStyle = DARK_GRAY;
    ctx.lineWidth = 2;
    ctx.setLineDash([8, 4]);
    ctx.strokeRect(cx - hitW/2, cy - hitH/2, hitW, hitH);
    ctx.setLineDash([]);
    
    // Labels
    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 20px Arial';
    ctx.fillText('ClickArea (Interactive zone)', cx, cy + hitH/2 + 30);
    
    ctx.fillStyle = GRAY;
    ctx.font = '16px Arial';
    ctx.fillText('Actual component bounds', cx, cy - ch/2 - 15);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
