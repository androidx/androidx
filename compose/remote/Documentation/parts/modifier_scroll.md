### Scroll Modifier Illustration

The `Scroll` modifier allows a component to have a larger internal content area than its physical viewport bounds.

<div id="scrollModifierContainer">
  <canvas id="scrollModifierCanvas_v1" width="500" height="300" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var phase = 0;
  function draw() {
    var canvas = document.getElementById('scrollModifierCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.1)';
    ctx.clearRect(0, 0, 500, 300);

    var viewportX=150, viewportY=80, viewportW=200, viewportH=180;
    var contentW=200, contentH=500;
    
    // Animate scroll position
    var maxScroll = contentH - viewportH;
    var scrollY = (Math.sin(phase) + 1) / 2 * maxScroll;
    phase += 0.02;

    // Content Area (Full) - Drawn behind/around
    ctx.strokeStyle = GRAY;
    ctx.setLineDash([5, 5]);
    ctx.strokeRect(viewportX, viewportY - scrollY, contentW, contentH);
    ctx.setLineDash([]);
    ctx.font = '16px Arial';
    ctx.fillStyle = GRAY;
    ctx.textAlign = 'left';
    ctx.fillText('Content Area (500px)', viewportX + 10, viewportY - scrollY + 25);

    // Some content items
    for(var i=0; i<7; i++) {
      ctx.fillStyle = BLUE_TRANS;
      ctx.fillRect(viewportX + 20, viewportY - scrollY + 50 + i*70, contentW - 40, 50);
    }

    // Viewport (Clip bounds)
    ctx.lineWidth = 4;
    ctx.strokeStyle = DARK_GRAY;
    ctx.strokeRect(viewportX, viewportY, viewportW, viewportH);
    
    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 22px Arial';
    ctx.fillText('Viewport (180px)', viewportX, viewportY - 15);
    
    // Scroll Indicator
    ctx.fillStyle = BLUE;
    var thumbH = (viewportH / contentH) * viewportH;
    var thumbY = viewportY + (scrollY / contentH) * viewportH;
    ctx.fillRect(viewportX + viewportW + 5, thumbY, 6, thumbH);
    
    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 16px Arial';
    ctx.fillText('scroll position', viewportX + viewportW + 15, thumbY + thumbH/2 + 5);

    requestAnimationFrame(draw);
  }
  draw();
})();
</script>
