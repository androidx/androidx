### Theme Illustration

The `Theme` operation allows the document to adapt its visual appearance (colors, styles) based on the active theme (e.g., Light or Dark mode).

<div id="themeContainer">
  <canvas id="themeCanvas_v1" width="500" height="200" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var themeTimer = 0;
  function draw() {
    var canvas = document.getElementById('themeCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var isDark = (Math.floor(themeTimer / 100) % 2 === 1);
    themeTimer++;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    var bg = isDark ? '#333' : '#fff';
    var textCol = isDark ? '#eee' : '#444';
    var primaryCol = isDark ? '#8ab4f8' : '#0047AB';

    ctx.fillStyle = bg;
    ctx.fillRect(0, 0, 500, 200);

    // Component Representation
    ctx.fillStyle = primaryCol;
    ctx.fillRect(150, 50, 200, 100);
    ctx.strokeStyle = textCol;
    ctx.lineWidth = 2;
    ctx.strokeRect(150, 50, 200, 100);

    ctx.fillStyle = textCol;
    ctx.font = 'bold 24px Arial';
    ctx.textAlign = 'center';
    ctx.fillText(isDark ? 'DARK MODE' : 'LIGHT MODE', 250, 110);

    ctx.font = 'bold 18px Arial';
    ctx.fillStyle = GRAY;
    ctx.fillText('Theme switching automatically updates linked colors', 250, 180);

    requestAnimationFrame(draw);
  }
  draw();
})();
</script>
