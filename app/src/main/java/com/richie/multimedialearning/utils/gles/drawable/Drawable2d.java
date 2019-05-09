/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.richie.multimedialearning.utils.gles.drawable;

import com.richie.multimedialearning.utils.gles.GlUtil;

import java.nio.FloatBuffer;

/**
 * Base class for stuff we like to draw.
 */
public class Drawable2d {
    public static final int COORDS_PER_VERTEX = 2;
    private static final int SIZEOF_FLOAT = 4;
    public static final int VERTEX_STRIDE = COORDS_PER_VERTEX * SIZEOF_FLOAT;
    public static final int TEX_COORD_STRIDE = 2 * SIZEOF_FLOAT;

    private FloatBuffer mVertexArray;
    private FloatBuffer mTexCoordArray;
    private int mVertexCount;

    public Drawable2d() {
    }

    /**
     * Prepares a drawable from a "pre-fabricated" shape definition.
     * <p>
     * Does no EGL/GL operations, so this can be done at any time.
     */
    public Drawable2d(float[] vertexArray, float[] texCoordArray) {
        updateVertexArray(vertexArray);
        updateTexCoordArray(texCoordArray);
    }

    /**
     * update vertex array and create buffer
     *
     * @param vertexArray
     */
    public void updateVertexArray(float[] vertexArray) {
        mVertexArray = GlUtil.createFloatBuffer(vertexArray);
        mVertexCount = vertexArray.length / COORDS_PER_VERTEX;
    }

    /**
     * update texture coordinate array and create buffer
     *
     * @param texCoordArray
     */
    public void updateTexCoordArray(float[] texCoordArray) {
        mTexCoordArray = GlUtil.createFloatBuffer(texCoordArray);
    }

    /**
     * Returns the array of vertices.
     * <p>
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    public FloatBuffer getVertexArray() {
        return mVertexArray;
    }

    /**
     * Returns the array of texture coordinates.
     * <p>
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    public FloatBuffer getTexCoordArray() {
        return mTexCoordArray;
    }

    /**
     * Returns the number of vertices stored in the vertex array.
     */
    public int getVertexCount() {
        return mVertexCount;
    }
}
