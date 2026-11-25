### ClipPath Illustration

The `ClipPath` modifier restricts the drawing area of a component to an arbitrary path defined
earlier in the document. The path coordinates are relative to the component's content box (after
any preceding padding modifier), in the same space as canvas drawing operations, and are not scaled
by the document density behaviour. Everything painted after the modifier (subsequent modifiers,
click ripples and the component content) is clipped.

<div id="clipPathContainer">
  <canvas id="clipPathCanvas_v1" width="500" height="330" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('clipPathCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 330);

    var x = 100, y = 40, w = 300, h = 220;

    function wedge() {
      ctx.beginPath();
      ctx.moveTo(x + w / 2, y + h / 2);
      ctx.arc(x + w / 2, y + h / 2, h / 2, -Math.PI / 2, Math.PI / 3);
      ctx.closePath();
    }

    // Component content (dimmed): what would be drawn without the clip
    ctx.globalAlpha = 0.1;
    ctx.fillStyle = BLUE;
    ctx.fillRect(x, y, w, h);
    ctx.globalAlpha = 1.0;

    // Component bounds
    ctx.strokeStyle = GRAY;
    ctx.lineWidth = 2;
    ctx.setLineDash([8, 8]);
    ctx.strokeRect(x, y, w, h);
    ctx.setLineDash([]);

    // Clip to the path and paint the content again
    ctx.save();
    wedge();
    ctx.clip();
    ctx.fillStyle = BLUE;
    ctx.fillRect(x, y, w, h);
    ctx.fillStyle = 'white';
    ctx.font = 'bold 20px Arial';
    ctx.textAlign = 'center';
    ctx.fillText('Clipped', x + w / 2 + 40, y + h / 2 - 20);
    ctx.restore();

    // Path outline
    wedge();
    ctx.strokeStyle = DARK_GRAY;
    ctx.lineWidth = 2;
    ctx.stroke();

    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 18px Arial';
    ctx.textAlign = 'left';
    ctx.fillText('Component bounds (dashed) and clip path (solid)', x, y + h + 40);
  }
  if (document.readyState === 'complete') { draw(); }
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
