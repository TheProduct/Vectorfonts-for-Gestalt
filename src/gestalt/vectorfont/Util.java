/*
 * Vectorfont Plugin for Gestalt
 *
 * * Copyright (C) 2011 The Product GbR Kochlik + Paul
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


package gestalt.vectorfont;

import gestalt.Gestalt;
import gestalt.shape.Mesh;

import mathematik.Vector3f;

import com.sun.j3d.utils.geometry.GeometryInfo;

import java.util.Vector;

import javax.media.j3d.GeometryArray;
import javax.vecmath.Point3f;


public class Util {

    public static Mesh createMesh(Vector<Vector3f> pTriangles, final boolean pCreateNormals) {
        final float[] mVertices = werkzeug.Util.toArray3f(pTriangles);
        final float[] mNormals;
        if (pCreateNormals) {
            mNormals = new float[mVertices.length];
            mathematik.Util.createNormals(mVertices, mNormals);
        } else {
            mNormals = null;
        }
        return new Mesh(mVertices, 3,
                            null, 4,
                            null, 2,
                            mNormals,
                            Gestalt.MESH_TRIANGLES);
    }

    public static Vector<Vector3f[]> convertToTriangles(final Vector<Vector<Vector<Vector3f>>> pVectors) {
        final Vector<Vector3f[]> myCharTriangles = new Vector<Vector3f[]>();
        for (int i = 0; i < pVectors.size(); i++) {
            final Vector<Vector3f> myVertices = new Vector<Vector3f>();
            final Vector<Integer> myVertivesPerShape = new Vector<Integer>();
            final Vector<Vector<Vector3f>> myCharacter = pVectors.get(i);
            for (int j = 0; j < myCharacter.size(); j++) {
                final Vector<Vector3f> myOutline = myCharacter.get(j);
                myVertivesPerShape.add(myOutline.size());
                for (Vector3f v : myOutline) {
                    myVertices.add(v);
                }
            }
            if (myCharacter.size() > 0) {
                myCharTriangles.add(triangulate(werkzeug.Util.toArray3f(myVertices),
                                                werkzeug.Util.toArray(myVertivesPerShape),
                                                new int[] {myCharacter.size()}));
            }
        }
        return myCharTriangles;
    }

    public static Vector<Vector3f> convertToVertices(final Vector<Vector<Vector<Vector3f>>> pVectors) {
        final Vector<Vector3f> myCharTriangles = new Vector<Vector3f>();
        for (int i = 0; i < pVectors.size(); i++) {
            final Vector<Vector3f> myVertices = new Vector<Vector3f>();
            final Vector<Integer> myVertivesPerShape = new Vector<Integer>();
            final Vector<Vector<Vector3f>> myCharacter = pVectors.get(i);
            for (int j = 0; j < myCharacter.size(); j++) {
                final Vector<Vector3f> myOutline = myCharacter.get(j);
                myVertivesPerShape.add(myOutline.size());
                for (Vector3f v : myOutline) {
                    myVertices.add(v);
                }
            }
            if (myCharacter.size() > 0) {
                final Vector3f[] mTriangle = triangulate(werkzeug.Util.toArray3f(myVertices),
                                                         werkzeug.Util.toArray(myVertivesPerShape),
                                                         new int[] {myCharacter.size()});
                for (int j = 0; j < mTriangle.length; j++) {
                    final Vector3f v = mTriangle[j];
                    myCharTriangles.add(v);
                }
            }
        }
        return myCharTriangles;
    }

    public static Vector3f[] triangulate(float[] theData,
                                         int[] theStripCount,
                                         int[] theContourCount) {
        final GeometryInfo myGeometryInfo = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
        myGeometryInfo.setCoordinates(theData);
        myGeometryInfo.setStripCounts(theStripCount);
        myGeometryInfo.setContourCounts(theContourCount);

        final GeometryArray myGeometryArray = myGeometryInfo.getGeometryArray();
        final Vector3f[] myPoints = new Vector3f[myGeometryArray.getValidVertexCount()];
        for (int i = 0; i < myGeometryArray.getValidVertexCount(); i++) {
            final Point3f p = new Point3f();
            myGeometryArray.getCoordinate(i, p);
            myPoints[i] = new Vector3f();
            myPoints[i].set(p.x, p.y, p.z);
        }

        return myPoints;
    }
}
