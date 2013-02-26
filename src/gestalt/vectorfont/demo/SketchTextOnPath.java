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


import gestalt.shape.Mesh;
import gestalt.vectorfont.TextOutlineCreator;
import gestalt.vectorfont.Util;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.util.Vector;
import mathematik.Vector3f;
import processing.core.PApplet;


public class SketchTextOnPath
        extends PApplet {

    private Shape mReturn;

    private Vector<Vector<Vector<Vector3f>>> mReturns;

    private TextOutlineCreator mPathCreator;

    public void setup() {
        size(640, 480, OPENGL);

        mPathCreator = new TextOutlineCreator("Helvetica", 32);
        mPathCreator.insideFlag(mathematik.Util.CLOCKWISE);
//        mPathCreator = new TextOutlineCreator("ReplicaMonoPro", 32);
//        mPathCreator.insideFlag(mathematik.Util.COUNTERCLOCKWISE);
        mPathCreator.outline_flatness(0.25f);
        mPathCreator.stretch_to_fit(false);
        mPathCreator.repeat(false);

        final Ellipse2D.Float e = new Ellipse2D.Float();
        e.x = width / 2 - 100;
        e.y = height / 2 - 100;
        e.width = 200;
        e.height = 200;
        mReturn = mPathCreator.getOutlineFromTextOnPathJAVA2D(e, "Since I was very young I realized ...");
    }

    public void draw() {
        background(255);
        drawTriangles();
        drawOutline(mReturn);
    }

    private void drawTriangles() {
        /* adjust flatness ( ie resolutions of curves ) */
        final float mFlatness = abs((float)mouseX / (float)width) * 5 + 0.1f;
        mPathCreator.outline_flatness(mFlatness);

        /* create ellipse */
        final Ellipse2D.Float e = new Ellipse2D.Float();
        e.width = 300 + (float)mouseX / (float)width * 100;
        e.height = e.width;
        e.x = width / 2 - e.width / 2;
        e.y = height / 2 - e.height / 2;

//        final Vector<Vector3f> mPoints = new Vector<Vector3f>();
//        mPoints.add(new Vector3f(0, 0));
//        mPoints.add(new Vector3f(200, 400));
//        mPoints.add(new Vector3f(100, 100));
//        mPoints.add(new Vector3f(400, 0));
//        final Shape e = mPathCreator.createShapeFromPoints2D(mPoints);

        /* create outlines */
        mReturns = mPathCreator.getOutlineFromTextOnPath(e, "MOUSENESS: " + mouseX + ", " + mouseY
                + " | FPSNESS: " + (int)frameRate
                + " | FLATNESS: " + mFlatness
                + " | RANDOMNESS: " + (int)random(9));

        /* toggle fill and wireframe */
        if (mousePressed) {
            stroke(0, 0, 255, 127);
            fill(0, 64);
        } else {
            noStroke();
            fill(0);
        }

        /* create and draw trinangles */
        final Vector<Vector3f> myTriangles = Util.convertToVertices(mReturns);
        beginShape(TRIANGLES);
        for (Vector3f myCharacters : myTriangles) {
            vertex(myCharacters.x, myCharacters.y, myCharacters.z);
        }
        endShape();
    }

    private void drawOutline(Shape mReturn) {
        stroke(0, 64);
        noFill();

        if (mReturn != null) {
            final PathIterator it = mReturn.getPathIterator(null, 1.0f);
            int type;
            float[] points = new float[6];
            beginShape(POLYGON);
            while (!it.isDone()) {
                type = it.currentSegment(points);
                vertex(points[0], points[1]);
                if (type == PathIterator.SEG_CLOSE) {
                    endShape(CLOSE);
                    beginShape();
                }
                it.next();
            }
            endShape();
        }
    }

    public static void main(String[] args) {
        PApplet.main(new String[] {SketchTextOnPath.class.getName()});
    }
}
