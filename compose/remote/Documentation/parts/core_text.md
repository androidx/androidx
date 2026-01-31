### Core Text Layout Algorithm

Core Text is a leaf layout component responsible for high-fidelity text rendering and measurement.

#### Visual Illustration (Autosize and Alignment)
<div id="coreTextContainer">
  <canvas id="coreTextCanvas_v1" width="500" height="280" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var phase = 0;
  function draw() {
    var canvas = document.getElementById('coreTextCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 280);

    // Box size oscillates for Autosize demo
    var boxW = 150 + Math.sin(phase) * 50;
    var boxH = 100 + Math.cos(phase) * 30;
    phase += 0.02;

    var text = "DYNAMIC TEXT";

    // 1. Autosize Demo
    ctx.save();
    ctx.translate(50, 60);
    ctx.strokeStyle = GRAY;
    ctx.setLineDash([4, 4]);
    ctx.strokeRect(0, 0, boxW, boxH);
    ctx.setLineDash([]);
    
    // Simple heuristic for autosize illustration
    var fontSize = Math.min(boxW / 7, boxH / 1.5);
    ctx.font = 'bold ' + fontSize + 'px Arial';
    ctx.fillStyle = BLUE;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(text, boxW/2, boxH/2);
    
    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 16px Arial';
    ctx.textAlign = 'left';
    ctx.fillText('Autosize: ' + Math.round(fontSize) + 'px', 0, -15);
    ctx.restore();

    // 2. Alignment Demo
    ctx.save();
    ctx.translate(300, 60);
    var alignBoxW = 160, alignBoxH = 150;
    ctx.strokeStyle = '#eee';
    ctx.strokeRect(0, 0, alignBoxW, alignBoxH);
    
    ctx.font = '18px Arial';
    ctx.fillStyle = DARK_GRAY;
    
    // Left
    ctx.textAlign = 'left';
    ctx.fillText('Align: Left', 10, 30);
    ctx.fillStyle = BLUE;
    ctx.fillRect(10, 35, 100, 4);
    
    // Center
    ctx.fillStyle = DARK_GRAY;
    ctx.textAlign = 'center';
    ctx.fillText('Align: Center', alignBoxW/2, 80);
    ctx.fillStyle = BLUE;
    ctx.fillRect(alignBoxW/2 - 50, 85, 100, 4);
    
    // Right
    ctx.fillStyle = DARK_GRAY;
    ctx.textAlign = 'right';
    ctx.fillText('Align: Right', alignBoxW - 10, 130);
    ctx.fillStyle = BLUE;
    ctx.fillRect(alignBoxW - 110, 135, 100, 4);
    
    ctx.restore();

    requestAnimationFrame(draw);
  }
  draw();
})();
</script>

#### Measurement Phase

1.  **Complex Text Analysis**:
    -   The algorithm checks for features requiring complex layout: line breaks (\n), tabs (\t), letter spacing, varied line heights, or overflow ellipsis.
    -   If these are present, it uses a complex layout engine to calculate line breaks and word wrapping.

2.  **Autosize**:
    -   If `autosize` is enabled, the layout performs a binary search between `minFontSize` and `maxFontSize`.
    -   It finds the largest font size that allows the text to fit within the provided `maxWidth` and `maxHeight` without causing unexpected hyphenation or overflow.

3.  **Intrinsic Size**:
    -   The measured size is the bounding box of the rendered text glyphs.

#### Styling and Alignment

-   Supports ARGB colors (static or dynamic via ID).
-   Supports font weights, styles (italic), and variable font axes.
-   Horizontal alignment (`Left`, `Center`, `Right`, `Justify`) is applied during the text layout process.
