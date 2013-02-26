/*
 * Vectorfont Plugin for Gestalt
 *
 * Copyright (C) 2011 The Product GbR Kochlik + Paul
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * {@link http://www.gnu.org/licenses/lgpl.html}
 *
 */


package gestalt.vectorfont.demo;


import gestalt.render.SketchRenderer;
import gestalt.shape.Mesh;
import gestalt.vectorfont.TextOutlineCreator;
import gestalt.vectorfont.Util;

import java.awt.geom.Ellipse2D;
import java.util.Vector;
import mathematik.Vector3f;


public class SketchTextAsMesh
        extends SketchRenderer {

    private Vector<Vector<Vector<Vector3f>>> mReturns;

    private TextOutlineCreator mPathCreator;

    private Mesh mMesh;

    public void setup() {
        setupDefaults();
        backgroundcolor().set(0.2f);

        mPathCreator = new TextOutlineCreator("Helvetica", 32);
        mPathCreator.insideFlag(mathematik.Util.CLOCKWISE);
        mPathCreator.outline_flatness(0.25f);
        mPathCreator.stretch_to_fit(false);
        mPathCreator.repeat(false);
    }

    public void loop(final float pDeltaTime) {
        drawTriangles();
    }

    private void drawTriangles() {
        /* create ellipse */
        final Ellipse2D.Float e = new Ellipse2D.Float();
        e.width = 300 + (float)mouseX / (float)width * 100;
        e.height = e.width;
        e.x = -e.width / 2;
        e.y = -e.height / 2;

        /* create outlines */
        mReturns = mPathCreator.getOutlineFromTextOnPath(e, "MOUSENESS: " + mouseX + ", " + mouseY
                + " | FPSNESS: " + (int)frameRate
                + " | RANDOMNESS: " + (int)random(0, 9));

        /* create and draw trinangles */
        bin(BIN_3D).remove(mMesh);
        mMesh = Util.createMesh(Util.convertToVertices(mReturns), true);
        bin(BIN_3D).add(mMesh);
    }

    public static void main(String[] args) {
        SketchTextAsMesh.init(SketchTextAsMesh.class);
    }
}
