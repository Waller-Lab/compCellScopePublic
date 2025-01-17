/*
 * Developed as part of the Computational CellScope Project
 * Waller Lab, EECS Dept., The University of California at Berkeley
 *
 * Licensed under the 3-Clause BSD License:
 *
 * Copyright © 2015 Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the owner nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package com.wallerlab.compcellscope.viewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class ZoomableImageView extends View {
    private static final int INVALID_POINTER_ID = -1;
    private static final float MIN_SCALE = 0.05f;
    private static final float MAX_SCALE = 20.0f;

    private Drawable image;
    private float xPos;
    private float yPos;
    private float lastXPos;
    private float lastYPos;
    private int activePointerId = INVALID_POINTER_ID;

    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.f;

    public ZoomableImageView(Context context) {
        this(context, null, 0);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void setImage(String file) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
    	Bitmap opt = BitmapFactory.decodeFile(file, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, image.getIntrinsicWidth(), image.getIntrinsicHeight());

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap bmp = BitmapFactory.decodeFile(file, options);
        image = new BitmapDrawable(getContext().getResources(), bmp);
        image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
        invalidate();
    }
    
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

        final int halfHeight = height / 2;
        final int halfWidth = width / 2;

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while ((halfHeight / inSampleSize) > reqHeight
                && (halfWidth / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }
    }

    return inSampleSize;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // pass all events through ScaleGestureDetector
        scaleDetector.onTouchEvent(ev);

        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                lastXPos = ev.getX();
                lastYPos = ev.getY();
                activePointerId = ev.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                final int pointerIndex = ev.findPointerIndex(activePointerId);
                float x = ev.getX(pointerIndex);
                float y = ev.getY(pointerIndex);

                if (!scaleDetector.isInProgress()) {
                    xPos += x - lastXPos;
                    yPos += y - lastYPos;

                    invalidate();
                }

                lastXPos = x;
                lastYPos = y;

                break;
            case MotionEvent.ACTION_POINTER_UP:
                final int idx = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                int pointerId = ev.getPointerId(idx);
                if (pointerId == activePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = idx == 0 ? 1 : 0;
                    lastXPos = ev.getX(newPointerIndex);
                    lastYPos = ev.getY(newPointerIndex);
                    activePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            case MotionEvent.ACTION_UP:
                activePointerId = INVALID_POINTER_ID;
                break;
            case MotionEvent.ACTION_CANCEL:
                activePointerId = INVALID_POINTER_ID;
                break;
        }

        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (image != null) {
            canvas.save();
            canvas.translate(xPos, yPos);
            canvas.scale(scaleFactor, scaleFactor);
            canvas.translate(-image.getIntrinsicWidth()/2, -image.getIntrinsicHeight()/2);
            image.draw(canvas);
            canvas.restore();
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_SCALE, Math.min(scaleFactor, MAX_SCALE));

            invalidate();
            return true;
        }
    }
}
