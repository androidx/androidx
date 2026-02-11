### Collapsible Column Layout Algorithm

Collapsible Column extends Column Layout by hiding children that exceed the available vertical space.

#### Basic Overflow Animation
<div id="collapsibleColumnContainer">
  <canvas id="collapsibleColumnCanvas_v1" width="500" height="400" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

#### Priority-Based Hiding Animation
In this example, **Item 2** has a higher priority value (lower importance), so it is hidden **first**. Notice how Item 3 shifts up to fill the gap when Item 2 is removed.

<div id="collapsibleColumnPriorityContainer">
  <canvas id="collapsibleColumnPriorityCanvas_v1" width="500" height="400" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
  var itemW = 200, itemH = 80, spacing = 10, startX = 150, startY = 50;

  function drawItem(ctx, idx, x, y, fits, label) {
    if (!fits) return;
    ctx.fillStyle = BLUE_TRANS;
    ctx.fillRect(x, y, itemW, itemH);
    ctx.strokeStyle = BLUE;
    ctx.lineWidth = 2;
    ctx.strokeRect(x, y, itemW, itemH);
    ctx.fillStyle = BLUE;
    ctx.textAlign = 'center';
    ctx.font = 'bold 14px Arial';
    ctx.fillText(label, x + itemW/2, y + itemH/2 + 5);
  }

  var phase1 = 0, phase2 = 0;
  function animate() {
    var c1 = document.getElementById('collapsibleColumnCanvas_v1');
    var c2 = document.getElementById('collapsibleColumnPriorityCanvas_v1');
    
    if (c1) {
      var ctx = c1.getContext('2d');
      ctx.clearRect(0, 0, 500, 400);
      var h = 50 + (Math.sin(phase1) + 1) / 2 * 300;
      phase1 += 0.015;
      ctx.strokeStyle = GRAY; ctx.setLineDash([5, 5]); ctx.strokeRect(startX, startY, itemW, h); ctx.setLineDash([]);
      ctx.fillStyle = GRAY; ctx.font = 'bold 14px Arial'; ctx.textAlign='left'; ctx.fillText('Height: ' + Math.round(h) + 'px', startX, startY - 10);
      
      var currentY = startY;
      for (var i=0; i<3; i++) {
        var fits = ( (currentY + itemH - startY) <= h );
        if (fits) {
          drawItem(ctx, i+1, startX, currentY, true, 'ITEM ' + (i+1));
          currentY += itemH + spacing;
        }
      }
    }

    if (c2) {
      var ctx = c2.getContext('2d');
      ctx.clearRect(0, 0, 500, 400);
      var h = 50 + (Math.sin(phase2) + 1) / 2 * 300;
      phase2 += 0.015;
      ctx.strokeStyle = GRAY; ctx.setLineDash([5, 5]); ctx.strokeRect(startX, startY, itemW, h); ctx.setLineDash([]);
      ctx.fillStyle = GRAY; ctx.font = 'bold 14px Arial'; ctx.textAlign='left'; ctx.fillText('Height: ' + Math.round(h) + 'px', startX, startY - 10);
      
      var items = [
        {id: 1, p: 10},
        {id: 2, p: 100},
        {id: 3, p: 20}
      ];
      
      var visibility = {};
      var accumulated = 0;
      var priorityProcess = [...items].sort((a,b) => a.p - b.p);
      for (var itm of priorityProcess) {
         if (accumulated + itemH <= h) {
            visibility[itm.id] = true;
            accumulated += itemH + spacing;
         } else {
            visibility[itm.id] = false;
         }
      }

      var currentY = startY;
      for (var itm of items) {
        if (visibility[itm.id]) {
          drawItem(ctx, itm.id, startX, currentY, true, 'ITEM ' + itm.id + ' (P:' + itm.p + ')');
          currentY += itemH + spacing;
        }
      }
    }
    requestAnimationFrame(animate);
  }
  animate();
})();
</script>

#### Measurement Phase

1.  **Priority Sorting**:
    -   Children are sorted based on their `CollapsiblePriority` modifier.

2.  **Overflow Detection**:
    -   The layout measures children and accumulates their heights.
    -   Any child that would cause the total height to exceed the Column's maximum height is marked as `GONE`.

#### Layout Phase

-   Visible children are positioned vertically according to standard Column Layout rules (Top, Center, Bottom, etc.).
