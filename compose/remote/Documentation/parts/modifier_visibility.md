### ComponentVisibility Illustration

The `ComponentVisibility` modifier controls whether a component is visible, hidden but still taking up space, or completely removed from the layout.

<div id="visibilityModifierContainer">
  <canvas id="visibilityModifierCanvas_v1" width="500" height="250" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('visibilityModifierCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#BBBBBB', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 250);

    function drawState(x, y, label, sublabel, vis) {
      ctx.strokeStyle = GRAY;
      ctx.setLineDash([5, 5]);
      ctx.strokeRect(x, y, 120, 100);
      ctx.setLineDash([]);

      if (vis === 'VISIBLE') {
        ctx.fillStyle = BLUE;
        ctx.fillRect(x + 10, y + 10, 100, 80);
      } else if (vis === 'INVISIBLE') {
        ctx.fillStyle = BLUE_TRANS;
        ctx.fillRect(x + 10, y + 10, 100, 80);
        ctx.setLineDash([2, 2]);
        ctx.strokeStyle = BLUE;
        ctx.strokeRect(x + 10, y + 10, 100, 80);
        ctx.setLineDash([]);
      }
      
      ctx.fillStyle = DARK_GRAY;
      ctx.font = 'bold 18px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(label, x + 60, y + 130);
      ctx.font = '14px Arial';
      ctx.fillStyle = GRAY;
      ctx.fillText(sublabel, x + 60, y + 150);
    }

    drawState(50, 40, 'VISIBLE', 'Drawn & Sized', 'VISIBLE');
    drawState(190, 40, 'INVISIBLE', 'Sized only', 'INVISIBLE');
    drawState(330, 40, 'GONE', 'Not Sized', 'GONE');
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
