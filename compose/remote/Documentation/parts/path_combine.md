### PathCombine Illustration

The `PathCombine` operation performs boolean logic between two paths to create a new complex shape.

<div id="pathCombineContainer">
  <canvas id="pathCombineCanvas_v1" width="500" height="220" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var phase = 0;
  function draw() {
    var canvas = document.getElementById('pathCombineCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#BBBBBB', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 220);

    // Offset between circles oscillates
    var circleOffset = 20 + (Math.sin(phase) + 1) / 2 * 40;
    phase += 0.03;

    function drawCircles(x, label, mode) {
      ctx.save();
      ctx.translate(x, 60);
      
      var r = 40;
      var c1x = 40 - circleOffset/2;
      var c2x = 40 + circleOffset/2;

      // Combined Result (Logic)
      ctx.save();
      if (mode === 'INTERSECT') {
          ctx.beginPath(); ctx.arc(c1x, 40, r, 0, Math.PI*2); ctx.clip();
          ctx.beginPath(); ctx.arc(c2x, 40, r, 0, Math.PI*2);
          ctx.fillStyle = BLUE; ctx.fill();
      } else if (mode === 'UNION') {
          ctx.beginPath(); ctx.arc(c1x, 40, r, 0, Math.PI*2);
          ctx.arc(c2x, 40, r, 0, Math.PI*2);
          ctx.fillStyle = BLUE; ctx.fill();
      } else if (mode === 'DIFFERENCE') {
          ctx.beginPath(); ctx.arc(c1x, 40, r, 0, Math.PI*2);
          ctx.fillStyle = BLUE; ctx.fill();
          ctx.globalCompositeOperation = 'destination-out';
          ctx.beginPath(); ctx.arc(c2x, 40, r, 0, Math.PI*2); ctx.fill();
      }
      ctx.restore();

      // Overlays (Original Outlines)
      ctx.strokeStyle = '#eee';
      ctx.lineWidth = 1;
      ctx.setLineDash([2, 2]);
      ctx.beginPath(); ctx.arc(c1x, 40, r, 0, Math.PI*2); ctx.stroke();
      ctx.beginPath(); ctx.arc(c2x, 40, r, 0, Math.PI*2); ctx.stroke();
      ctx.setLineDash([]);

      ctx.fillStyle = DARK_GRAY;
      ctx.font = 'bold 18px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(label, 40, 130);
      ctx.restore();
    }

    drawCircles(40, 'UNION', 'UNION');
    drawCircles(190, 'INTERSECT', 'INTERSECT');
    drawCircles(340, 'DIFFERENCE', 'DIFFERENCE');

    requestAnimationFrame(draw);
  }
  draw();
})();
</script>