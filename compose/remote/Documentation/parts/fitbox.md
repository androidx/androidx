### FitBox Layout Algorithm

FitBox is a specialized container that only renders the **first** child component that completely fits within the available space.

#### Visual Illustration (Animated Fitting Logic)
<div id="fitboxLayoutContainer">
  <canvas id="fitboxLayoutCanvas_v1" width="500" height="300" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var phase = 0;
  function draw() {
    var canvas = document.getElementById('fitboxLayoutCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#BBBBBB', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 300);

    // Container size oscillates
    var baseW = 350, baseH = 180;
    var minW = 80, minH = 40;
    var containerW = minW + (Math.sin(phase) + 1) / 2 * (baseW - minW);
    var containerH = minH + (Math.sin(phase) + 1) / 2 * (baseH - minH);
    phase += 0.015;

    var startX = 25, startY = 80;

    // Available Space
    ctx.strokeStyle = DARK_GRAY;
    ctx.setLineDash([5, 5]);
    ctx.strokeRect(startX, startY, containerW, containerH);
    ctx.setLineDash([]);
    ctx.font = 'bold 16px Arial';
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('FitBox Size: ' + Math.round(containerW) + 'x' + Math.round(containerH), startX, startY - 15);

    // Define 3 potential children with different sizes
    var children = [
      { id: 1, w: 250, h: 120, label: 'Child 1 (Large)' },
      { id: 2, w: 150, h: 80,  label: 'Child 2 (Medium)' },
      { id: 3, w: 60,  h: 30,  label: 'Child 3 (Small)' }
    ];

    var selectedIdx = -1;
    for (var i = 0; i < children.length; i++) {
      if (children[i].w <= containerW && children[i].h <= containerH) {
        selectedIdx = i;
        break;
      }
    }

    // Draw the selection indicator
    ctx.fillStyle = BLUE;
    ctx.font = 'bold 18px Arial';
    ctx.textAlign = 'left';
    if (selectedIdx !== -1) {
      ctx.fillText('Selected: ' + children[selectedIdx].label, startX + 250, 40);
      
      // Draw the selected child inside the container
      ctx.fillStyle = 'rgba(0, 71, 171, 0.5)';
      ctx.fillRect(startX, startY, children[selectedIdx].w, children[selectedIdx].h);
      ctx.strokeStyle = BLUE;
      ctx.lineWidth = 3;
      ctx.strokeRect(startX, startY, children[selectedIdx].w, children[selectedIdx].h);

      // Draw big white number in center
      ctx.fillStyle = 'white';
      ctx.font = 'bold 50px Arial';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(children[selectedIdx].id, startX + children[selectedIdx].w / 2, startY + children[selectedIdx].h / 2);
    } else {
      ctx.fillStyle = 'red';
      ctx.fillText('Selected: NONE (Too small)', startX + 250, 40);
    }

    // Draw the list of candidates
    ctx.textAlign = 'left';
    ctx.font = '14px Arial';
    for (var i = 0; i < children.length; i++) {
      var active = (i === selectedIdx);
      var tooBig = (children[i].w > containerW || children[i].h > containerH);
      
      ctx.fillStyle = active ? BLUE : (tooBig ? '#ccc' : DARK_GRAY);
      var prefix = active ? '▶ ' : '  ';
      ctx.fillText(prefix + children[i].label + ' [' + children[i].w + 'x' + children[i].h + ']', 30, 230 + i * 20);
    }

    requestAnimationFrame(draw);
  }
  draw();
})();
</script>

#### Measurement and Selection

1.  **Fitting Logic**:
    -   The layout iterates through its children in order.
    -   For each child, it checks its minimum required dimensions (defined by `WidthIn` or `HeightIn` modifiers).
    -   The first child whose minimum width and height are less than or equal to the FitBox's available constraints is selected.

2.  **Visibility Control**:
    -   The selected child is marked as `VISIBLE`.
    -   All other children are marked as `GONE`.
    -   If no child fits, the FitBox itself may become `GONE`.

3.  **Sizing**:
    -   The FitBox takes the measured size of the single selected child.
