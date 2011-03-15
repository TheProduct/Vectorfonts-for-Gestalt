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



/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package gestalt.vectorfont;


import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Vector;
import mathematik.Vector2f;
import mathematik.Vector3f;


public class TextOutlineCreator {

    private final Font mFont;

    private boolean mStretchToFit = false;

    private boolean mRepeat = false;

    private float mPathFlatness = 1.0f;

    private float mOutlineFlatness = 1.0f;

    private final FontRenderContext mFRC;

    public TextOutlineCreator(final String pFontName, final float mFontSize) {
        this(Font.decode(pFontName).deriveFont(Font.PLAIN, mFontSize));
    }

    public TextOutlineCreator(final Font pFont) {
        mFont = pFont;
        mFRC = new FontRenderContext(null, true, true);
    }

    public static Vector<Vector3f[]> convertToTriangles(Vector<Vector<Vector<Vector3f>>> mWordOutlines) {
        return gestalt.vectorfont.Util.convertToTriangles(mWordOutlines);
    }

    public void stretch_to_fit(final boolean pStretchToFit) {
        mStretchToFit = pStretchToFit;
    }

    public void repeat(final boolean pRepeat) {
        mRepeat = pRepeat;
    }

    public Vector<Vector<Vector<Vector3f>>> getOutlineFromText(String text) {
        final Vector<Vector<Vector<Vector3f>>> myOutlines = new Vector<Vector<Vector<Vector3f>>>();
        final GlyphVector mVector = mFont.createGlyphVector(mFRC, text);
        for (int j = 0; j < mVector.getNumGlyphs(); j++) {
            final Shape mGlyph = mVector.getGlyphOutline(j);
            final Vector<Vector<Vector<Vector3f>>> mNewCharacters = TextOutlineCreator.extractOutlineFromComplexGlyph(mGlyph, mOutlineFlatness);
            myOutlines.addAll(mNewCharacters);
        }
        return myOutlines;
    }

    public GeneralPath getOutlineFromTextOnPathJAVA2D(final Shape mPath, final String pText) {

        final GlyphVector mGlyphVector = mFont.createGlyphVector(mFRC, pText);

        final GeneralPath mResult = new GeneralPath();
        final PathIterator it = new FlatteningPathIterator(mPath.getPathIterator(null), mPathFlatness);
        float mPoints[] = new float[6];
        float moveX = 0, moveY = 0;
        float lastX = 0, lastY = 0;
        float thisX = 0, thisY = 0;
        int type = 0;
        float next = 0;
        int currentChar = 0;
        int length = mGlyphVector.getNumGlyphs();

        if (length == 0) {
            return mResult;
        }

        float factor = mStretchToFit ? measurePathLength(mPath, mPathFlatness) / (float)mGlyphVector.getLogicalBounds().getWidth() : 1.0f;
        float nextAdvance = 0;

        while (currentChar < length && !it.isDone()) {
            type = it.currentSegment(mPoints);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    moveX = lastX = mPoints[0];
                    moveY = lastY = mPoints[1];
                    mResult.moveTo(moveX, moveY);
                    nextAdvance = mGlyphVector.getGlyphMetrics(currentChar).getAdvance() * 0.5f;
                    next = nextAdvance;
                    break;

                case PathIterator.SEG_CLOSE:
                    mPoints[0] = moveX;
                    mPoints[1] = moveY;
                // Fall into....

                case PathIterator.SEG_LINETO:
                    thisX = mPoints[0];
                    thisY = mPoints[1];
                    float dx = thisX - lastX;
                    float dy = thisY - lastY;
                    float distance = (float)Math.sqrt(dx * dx + dy * dy);
                    if (distance >= next) {
                        float r = 1.0f / distance;
                        float angle = (float)Math.atan2(dy, dx);
                        while (currentChar < length && distance >= next) {
                            Shape glyph = mGlyphVector.getGlyphOutline(currentChar);
                            Point2D p = mGlyphVector.getGlyphPosition(currentChar);
                            float px = (float)p.getX();
                            float py = (float)p.getY();
                            float x = lastX + next * dx * r;
                            float y = lastY + next * dy * r;
                            float advance = nextAdvance;
                            nextAdvance = currentChar < length - 1 ? mGlyphVector.getGlyphMetrics(currentChar + 1).getAdvance() * 0.5f : 0;
                            final AffineTransform mTransform = new AffineTransform();
                            mTransform.setToTranslation(x, y);
                            mTransform.rotate(angle);
                            mTransform.translate(-px - advance, -py);
                            mResult.append(mTransform.createTransformedShape(glyph), false);
                            next += (advance + nextAdvance) * factor;
                            currentChar++;
                            if (mRepeat) {
                                currentChar %= length;
                            }
                        }
                    }
                    next -= distance;
                    lastX = thisX;
                    lastY = thisY;
                    break;
            }
            it.next();
        }

        return mResult;
    }

    public GeneralPath createShapeFromPoints2D(final Vector<Vector3f> pPoints) {
        final GeneralPath mPath = new GeneralPath();
        if (pPoints.isEmpty()) {
            return mPath;
        }
        mPath.moveTo(pPoints.firstElement().x, pPoints.firstElement().y);
        if (pPoints.size() > 1) {
            for (int i = 1; i < pPoints.size(); i++) {
                final Vector3f v = pPoints.get(i);
                mPath.lineTo(v.x, v.y);
            }
        }
        return mPath;
    }

    public GeneralPath createShapeFromPoints(final Vector<Vector2f> pPoints) {
        final GeneralPath mPath = new GeneralPath();
        if (pPoints.isEmpty()) {
            return mPath;
        }
        mPath.moveTo(pPoints.firstElement().x, pPoints.firstElement().y);
        if (pPoints.size() > 1) {
            for (int i = 1; i < pPoints.size(); i++) {
                final Vector2f v = pPoints.get(i);
                mPath.lineTo(v.x, v.y);
            }
        }
        return mPath;
    }

    public Vector<Vector<Vector<Vector3f>>> getOutlineFromTextOnPath(final Shape mPath, final String pText) {
        final Vector<Vector<Vector<Vector3f>>> mAllCharacters = new Vector<Vector<Vector<Vector3f>>>();
        final GlyphVector mGlyphVector = mFont.createGlyphVector(mFRC, pText);
        final PathIterator it = new FlatteningPathIterator(mPath.getPathIterator(null), mPathFlatness);
        float mPoints[] = new float[6];
        float moveX = 0, moveY = 0;
        float lastX = 0, lastY = 0;
        float mThisX = 0, mThisY = 0;
        int type = 0;
        float mNext = 0;
        int mCurrentCharID = 0;
        int mNumberOfGlyphs = mGlyphVector.getNumGlyphs();

        if (mNumberOfGlyphs == 0) {
            return mAllCharacters;
        }

        float factor = mStretchToFit ? measurePathLength(mPath, mPathFlatness) / (float)mGlyphVector.getLogicalBounds().getWidth() : 1.0f;
        float nextAdvance = 0;

        while (mCurrentCharID < mNumberOfGlyphs && !it.isDone()) {
            type = it.currentSegment(mPoints);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    moveX = lastX = mPoints[0];
                    moveY = lastY = mPoints[1];
                    nextAdvance = mGlyphVector.getGlyphMetrics(mCurrentCharID).getAdvance() * 0.5f;
                    mNext = nextAdvance;
                    break;

                case PathIterator.SEG_CLOSE:
                    mPoints[0] = moveX;
                    mPoints[1] = moveY;
                // Fall into....

                case PathIterator.SEG_LINETO:
                    mThisX = mPoints[0];
                    mThisY = mPoints[1];
                    final float dx = mThisX - lastX;
                    final float dy = mThisY - lastY;
                    float distance = (float)Math.sqrt(dx * dx + dy * dy);
                    if (distance >= mNext) {
                        float r = 1.0f / distance;
                        float angle = (float)Math.atan2(dy, dx);
                        while (mCurrentCharID < mNumberOfGlyphs && distance >= mNext) {
                            final Shape mGlyph = mGlyphVector.getGlyphOutline(mCurrentCharID);
                            final Point2D p = mGlyphVector.getGlyphPosition(mCurrentCharID);
                            final float px = (float)p.getX();
                            final float py = (float)p.getY();
                            final float x = lastX + mNext * dx * r;
                            final float y = lastY + mNext * dy * r;
                            final float advance = nextAdvance;
                            nextAdvance = mCurrentCharID < mNumberOfGlyphs - 1 ? mGlyphVector.getGlyphMetrics(mCurrentCharID + 1).getAdvance() * 0.5f : 0;
                            final AffineTransform mTransform = new AffineTransform();
                            mTransform.setToTranslation(x, y);
                            mTransform.rotate(angle);
                            mTransform.translate(-px - advance, -py);

                            /* extract outlines */
                            final Shape mShape = mTransform.createTransformedShape(mGlyph);
                            final Vector<Vector<Vector<Vector3f>>> mNewCharacters = extractOutlineFromComplexGlyph(mShape, mOutlineFlatness);
                            mAllCharacters.addAll(mNewCharacters);
                            mNext += (advance + nextAdvance) * factor;
                            mCurrentCharID++;
                            if (mRepeat) {
                                mCurrentCharID %= mNumberOfGlyphs;
                            }
                        }
                    }
                    mNext -= distance;
                    lastX = mThisX;
                    lastY = mThisY;
                    break;
            }
            it.next();
        }
        return mAllCharacters;
    }

    public static Vector<Vector<Vector<Vector3f>>> extractOutlineFromComplexGlyph(final Shape pShape,
                                                                                  final float pOutlineFlatness) {
        final Vector<Vector<Vector<Vector3f>>> pAllCharacters = new Vector<Vector<Vector<Vector3f>>>();
        final Vector<Vector<Vector3f>> mSingleCharacter = extractOutlineFromSimpleShape(pShape, pOutlineFlatness);
        /* add simple character or handle and split complex glyphs */
        if (mSingleCharacter.size() <= 1) {
            pAllCharacters.add(mSingleCharacter);
        } else {
            handleComplexGlyphs(mSingleCharacter, pAllCharacters);
        }
        return pAllCharacters;
    }

    public static Vector<Vector<Vector3f>> extractOutlineFromSimpleShape(final Shape pShape,
                                                                         final float pFlatness) {
        final PathIterator mIt = new FlatteningPathIterator(pShape.getPathIterator(null), pFlatness);
        final Vector<Vector<Vector3f>> myCharacter = new Vector<Vector<Vector3f>>();
        final float[] mSegment = new float[6];
        float x = 0;
        float y = 0;
        float mx = 0;
        float my = 0;
        Vector<Vector3f> mOutline = new Vector<Vector3f>();
        while (!mIt.isDone()) {
            final int mSegmentType = mIt.currentSegment(mSegment);
            switch (mSegmentType) {
                case PathIterator.SEG_MOVETO:
                    x = mx = mSegment[0];
                    y = my = mSegment[1];
                    mOutline.add(new Vector3f(x, y, 0.0f));
                    break;
                case PathIterator.SEG_LINETO:
                    x = mSegment[0];
                    y = mSegment[1];
                    mOutline.add(new Vector3f(x, y, 0.0f));
                    break;
                case PathIterator.SEG_CLOSE:
                    x = mx;
                    y = my;
                    mOutline.add(new Vector3f(x, y, 0.0f));
                    myCharacter.add(mOutline);
                    mOutline = new Vector<Vector3f>();
                    break;
            }
            mIt.next();
        }
        return myCharacter;
    }

    private static void handleComplexGlyphs(final Vector<Vector<Vector3f>> mSingleCharacter, final Vector<Vector<Vector<Vector3f>>> mAllCharacters) {
        /*  sort inside and outside shapes */
        final Vector<MShape> mInsideShapes = new Vector<MShape>();
        final Vector<MShape> mOutsideShapes = new Vector<MShape>();
        for (int i = 0; i < mSingleCharacter.size(); i++) {
            final Vector<Vector3f> mSingleShape = mSingleCharacter.get(i);
            final boolean mIsInside = mathematik.Util.isClockWise2D(mSingleShape) == mathematik.Util.CLOCKWISE;
            if (mIsInside) {
                mInsideShapes.add(new MShape(mIsInside, mSingleShape));
            } else {
                mOutsideShapes.add(new MShape(mIsInside, mSingleShape));
            }
        }
        /* add shapes as individual 'characters' if there is no insides */
        if (mInsideShapes.isEmpty()) {
            addMShapes(mOutsideShapes, mAllCharacters);
        } else {
            /* asign inside shapes to outside shapes */
            final Iterator<MShape> mIterator = mInsideShapes.iterator();
            while (mIterator.hasNext()) {
                final MShape mInsideShape = mIterator.next();
                mIterator.remove();
                /* check if inside shape is contained by any outside shape */
                boolean mAddedShape = false;
                for (final MShape mOutsideShape : mOutsideShapes) {
                    /* we just query the first point of the inside shape /( hopefully this will do ) */
                    if (mathematik.Util.inside2DPolygon(mInsideShape.shape.firstElement(), mOutsideShape.shape)) {
                        mOutsideShape.inside_shape.add(mInsideShape);
                        mAddedShape = true;
                        break;
                    }
                }
                /* if we couldn t asign inside shape, turn it into an outside shape */
                if (!mAddedShape) {
                    mOutsideShapes.add(mInsideShape);
                }
            }
            /* add all outside shapes */
            addMShapes(mOutsideShapes, mAllCharacters);
        }
    }

    private static void addMShapes(final Vector<MShape> pShapes, final Vector<Vector<Vector<Vector3f>>> pAllCharacters) {
        for (final MShape mMasterShape : pShapes) {
            final Vector<Vector<Vector3f>> mSimpleCharacter = new Vector<Vector<Vector3f>>();
            mSimpleCharacter.add(mMasterShape.shape);
            /* add inside shapes */
            if (!mMasterShape.inside_shape.isEmpty()) {
                for (final MShape mInsideShape : mMasterShape.inside_shape) {
                    mSimpleCharacter.add(mInsideShape.shape);
                }
            }
            pAllCharacters.add(mSimpleCharacter);
        }
    }

    private static class MShape {

        final boolean inside;

        final Vector<Vector3f> shape;

        final Vector<MShape> inside_shape;

        private MShape(final boolean pInside, final Vector<Vector3f> pShape) {
            inside = pInside;
            shape = pShape;
            inside_shape = new Vector<MShape>();
        }
    }

    private static float measurePathLength(final Shape shape, final float pPathFlatness) {
        PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), pPathFlatness);
        float points[] = new float[6];
        float moveX = 0, moveY = 0;
        float lastX = 0, lastY = 0;
        float thisX = 0, thisY = 0;
        int type = 0;
        float total = 0;

        while (!it.isDone()) {
            type = it.currentSegment(points);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    moveX = lastX = points[0];
                    moveY = lastY = points[1];
                    break;

                case PathIterator.SEG_CLOSE:
                    points[0] = moveX;
                    points[1] = moveY;
                // Fall into....

                case PathIterator.SEG_LINETO:
                    thisX = points[0];
                    thisY = points[1];
                    float dx = thisX - lastX;
                    float dy = thisY - lastY;
                    total += (float)Math.sqrt(dx * dx + dy * dy);
                    lastX = thisX;
                    lastY = thisY;
                    break;
            }
            it.next();
        }

        return total;
    }

    public void outline_flatness(float pOutlineFlatness) {
        mOutlineFlatness = pOutlineFlatness;
    }

    public void path_flatness(float pPathFlatness) {
        mPathFlatness = pPathFlatness;
    }
}
