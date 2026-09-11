/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.testutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.xr.runtime.math.Ray;
import androidx.xr.runtime.math.Vector3;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SpatialInteractionHelperJavaTest {


    @Test
    public void enumsAndConstants_accessibleFromJava() {
        assertEquals(0, SpatialRayAction.Up.getValue());
        assertEquals(1, SpatialRayAction.Down.getValue());
        assertEquals(0, SpatialDeviceType.Unknown.getValue());
        assertEquals(1, SpatialDeviceType.Controller.getValue());
        assertEquals(2, SpatialDeviceType.Eyes.getValue());
        assertEquals(3, SpatialDeviceType.Hands.getValue());
        assertEquals(4, SpatialDeviceType.Head.getValue());
        assertEquals(0, SpatialPointerType.Left.getValue());
        assertEquals(1, SpatialPointerType.Right.getValue());

        assertEquals(2.0f, SpatialInteractionHelper.DEFAULT_RAY_DISTANCE, 1e-4f);
        assertEquals(100L, SpatialInteractionHelper.DEFAULT_DELAY_MS);
        assertEquals(500L, SpatialInteractionHelper.DEFAULT_DRAG_DURATION_MS);
        assertEquals(0.05f, SpatialInteractionHelper.DEFAULT_MOVE_HANDLE_MARGIN, 1e-4f);
    }

    @Test
    public void spatialAlignment_accessibleFromJava() {
        assertEquals(0f, SpatialAlignment.Center.getX(), 1e-4f);
        assertEquals(1f, SpatialAlignment.TopCenter.getY(), 1e-4f);
        assertEquals(-1f, SpatialAlignment.BottomCenter.getY(), 1e-4f);
        assertEquals(-1f, SpatialAlignment.CenterLeft.getX(), 1e-4f);
        assertEquals(1f, SpatialAlignment.CenterRight.getX(), 1e-4f);

        // Constructor overloads
        SpatialAlignment defaultAlign = new SpatialAlignment();
        assertEquals(0f, defaultAlign.getX(), 1e-4f);
        SpatialAlignment customAlign = new SpatialAlignment(0.5f, -0.5f, 0.2f);
        assertEquals(0.5f, customAlign.getX(), 1e-4f);
        assertEquals(-0.5f, customAlign.getY(), 1e-4f);
        assertEquals(0.2f, customAlign.getZ(), 1e-4f);
    }


    @Test
    public void spatialNode_constructorsAndPropertiesFromJava() {
        SpatialNode node = new SpatialNode("Header", "CustomName", 42);
        assertEquals("Header", node.getHeader());
        assertEquals("CustomName", node.getName());
        assertEquals(42, node.getId());
        assertEquals(0, node.getDepth());
        assertEquals(Vector3.Zero, node.getPosition());
        assertEquals(Vector3.One, node.getScale());
    }

    @Test
    public void ray_accessibleFromJava() {
        Ray ray = new Ray(new Vector3(1f, 2f, 3f), new Vector3(0f, 1f, 0f));
        assertEquals(new Vector3(1f, 2f, 3f), ray.getOrigin());
        assertEquals(new Vector3(0f, 1f, 0f), ray.getDirection());
    }
}
