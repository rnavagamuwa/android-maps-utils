/*
 * Copyright 2014 Google Inc.
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

package com.google.maps.android.heatmaps;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;
import com.google.maps.android.geometry.Bounds;
import com.google.maps.android.quadtree.PointQuadTree;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Tile provider that creates heatmap tiles.
 */
public class WeightBasedHeatmapTileProvider implements TileProvider {

    /**
     * Default radius for convolution
     */
    private static final int DEFAULT_RADIUS = 20;

    /**
     * Default opacity of heatmap overlay
     */
    private static final double DEFAULT_OPACITY = 0.7;

    /**
     * Colors for default gradient.
     * Array of colors, represented by ints.
     */
    private static final int[] DEFAULT_GRADIENT_COLORS = {
            Color.rgb(102, 225, 0),
            Color.rgb(255, 0, 0)
    };

    /**
     * Starting fractions for default gradient.
     * This defines which percentages the above colors represent.
     * These should be a sorted array of floats in the interval [0, 1].
     */
    private static final float[] DEFAULT_GRADIENT_START_POINTS = {
            0.2f, 1f
    };

    /**
     * Default gradient for heatmap.
     */
    private static final Gradient DEFAULT_GRADIENT = new Gradient(DEFAULT_GRADIENT_COLORS, DEFAULT_GRADIENT_START_POINTS);

    /**
     * Size of the world (arbitrary).
     * Used to measure distances relative to the total world size.
     * Package access for WeightedLatLng.
     */
    private static final double WORLD_WIDTH = 1;

    /**
     * Tile dimension, in pixels.
     */
    private static final int TILE_DIM = 512;

    /**
     * Assumed screen size (pixels)
     */
    private static final int SCREEN_SIZE = 1280;

    /**
     * Default (and minimum possible) minimum zoom level at which to calculate maximum intensities
     */
    private static final int DEFAULT_MIN_ZOOM = 5;

    /**
     * Default (and maximum possible) maximum zoom level at which to calculate maximum intensities
     */
    private static final int DEFAULT_MAX_ZOOM = 11;

    /**
     * Maximum zoom level possible on a map.
     */
    private static final int MAX_ZOOM_LEVEL = 22;

    /**
     * Minimum radius value.
     */
    private static final int MIN_RADIUS = 10;

    /**
     * Default value for gradient smoothing.
     */
    private static final double DEFAULT_GRADIENT_SMOOTHING = 10;

    /**
     * Quad tree of all the points to display in the heatmap
     */
    private PointQuadTree<WeightedLatLng> mTree;

    /**
     * Collection of all the data.
     */
    private Collection<WeightedLatLng> mData;

    /**
     * Bounds of the quad tree
     */
    private Bounds mBounds;

    /**
     * Heatmap point radius.
     */
    private int mRadius;

    /**
     * Gradient of the color map
     */
    private Gradient mGradient;

    /**
     * Color map to use to color tiles
     */
    private int[] mColorMap;

    /**
     * Opacity of the overall heatmap overlay [0...1]
     */
    private double mOpacity;

    /**
     * Maximum intensity estimates for heatmap
     */
    private double[] mMaxIntensity;

    /**
     * Fixed maximum intensity for heatmap.
     */
    private double staticMaxIntensity;

    /**
     * Solid color gradient smoothing.
     */
    private double mGradientSmoothing;

    /**
     * Builder class for the HeatmapTileProvider.
     */
    public static class Builder {
        // Required parameters - not final, as there are 2 ways to set it
        private Collection<WeightedLatLng> data;

        // Optional, initialised to default values
        private int radius = DEFAULT_RADIUS;
        private Gradient gradient = DEFAULT_GRADIENT;
        private double opacity = DEFAULT_OPACITY;
        private double maxIntensity = 0;
        private double gradientSmoothing = DEFAULT_GRADIENT_SMOOTHING;

        /**
         * Constructor for builder.
         * No required parameters here, but user must call either data() or weightedData().
         */
        public Builder() {
        }

        /**
         * Setter for data in builder. Must call this or weightedData
         *
         * @param val Collection of LatLngs to put into quadtree.
         *            Should be non-empty.
         * @return updated builder object
         */
        public Builder data(Collection<LatLng> val) {
            return weightedData(wrapData(val));
        }

        /**
         * Setter for data in builder. Must call this or data
         *
         * @param val Collection of WeightedLatLngs to put into quadtree.
         *            Should be non-empty.
         * @return updated builder object
         */
        public Builder weightedData(Collection<WeightedLatLng> val) {
            this.data = val;

            // Check that points is non empty
            if (this.data.isEmpty()) {
                throw new IllegalArgumentException("No input points.");
            }
            return this;
        }

        /**
         * Setter for radius in builder
         *
         * @param val Radius of convolution to use, in terms of pixels.
         *            Must be within minimum and maximum values of 10 to 50 inclusive.
         * @return updated builder object
         */
        public Builder radius(int val) {
            radius = val;
            // Check that radius is within bounds.
            if (radius < MIN_RADIUS) {
                throw new IllegalArgumentException("Radius not within bounds.");
            }
            return this;
        }

        /**
         * Setter for gradient in builder
         *
         * @param val Gradient to color heatmap with.
         * @return updated builder object
         */
        public Builder gradient(Gradient val) {
            gradient = val;
            return this;
        }

        /**
         * Setter for opacity in builder
         *
         * @param val Opacity of the entire heatmap in range [0, 1]
         * @return updated builder object
         */
        public Builder opacity(double val) {
            opacity = val;
            // Check that opacity is in range
            if (opacity < 0 || opacity > 1) {
                throw new IllegalArgumentException("Opacity must be in range [0, 1]");
            }
            return this;
        }

        public Builder maxIntensity(double val) {
            if (val < 0) {
                throw new IllegalArgumentException("Maximum intensity must be larger than 0.");
            }
            maxIntensity = val;
            return this;
        }

        public Builder gradientSmoothing(double val) {
            this.gradientSmoothing = val;
            return this;
        }

        /**
         * Call when all desired options have been set.
         * Note: you must set data using data or weightedData before this!
         *
         * @return HeatmapTileProvider created with desired options.
         */
        public WeightBasedHeatmapTileProvider build() {
            // Check if data or weightedData has been called
            if (data == null) {
                throw new IllegalStateException("No input data: you must use either .data or " +
                        ".weightedData before building");
            }

            return new WeightBasedHeatmapTileProvider(this);
        }
    }

    private WeightBasedHeatmapTileProvider(Builder builder) {
        // Get parameters from builder
        mData = builder.data;

        mRadius = builder.radius;
        mGradient = builder.gradient;
        mOpacity = builder.opacity;

        staticMaxIntensity = builder.maxIntensity;
        mGradientSmoothing = builder.gradientSmoothing;

        // Generate color map
        setGradient(mGradient);

        // Set the data
        setWeightedData(mData);
    }

    /**
     * Changes the dataset the heatmap is portraying. Weighted.
     * User should clear overlay's tile cache (using clearTileCache()) after calling this.
     *
     * @param data Data set of points to use in the heatmap, as LatLngs.
     *             Note: Editing data without calling setWeightedData again will not update the data
     *             displayed on the map, but will impact calculation of max intensity values,
     *             as the collection you pass in is stored.
     *             Outside of changing the data, max intensity values are calculated only upon
     *             changing the radius.
     */
    public void setWeightedData(Collection<WeightedLatLng> data) {
        // Change point set
        mData = data;

        // Check point set is OK
        if (mData.isEmpty()) {
            throw new IllegalArgumentException("No input points.");
        }

        // Because quadtree bounds are final once the quadtree is created, we cannot add
        // points outside of those bounds to the quadtree after creation.
        // As quadtree creation is actually quite lightweight/fast as compared to other functions
        // called in heatmap creation, re-creating the quadtree is an acceptable solution here.

        // Make the quad tree
        mBounds = getBounds(mData);

        mTree = new PointQuadTree<>(mBounds);

        // Add points to quad tree
        for (WeightedLatLng l : mData) {
            mTree.add(l);
        }

        // Calculate reasonable maximum intensity for color scale (user can also specify)
        // Get max intensities
        mMaxIntensity = getMaxIntensities(mRadius);
    }

    /**
     * Changes the dataset the heatmap is portraying. Unweighted.
     * User should clear overlay's tile cache (using clearTileCache()) after calling this.
     *
     * @param data Data set of points to use in the heatmap, as LatLngs.
     */
    public void setData(Collection<LatLng> data) {
        // Turn them into WeightedLatLngs and delegate.
        setWeightedData(wrapData(data));
    }

    /**
     * Helper function - wraps LatLngs into WeightedLatLngs.
     *
     * @param data Data to wrap (LatLng)
     * @return Data, in WeightedLatLng form
     */
    private static Collection<WeightedLatLng> wrapData(Collection<LatLng> data) {
        // Use an ArrayList as it is a nice collection
        ArrayList<WeightedLatLng> weightedData = new ArrayList<WeightedLatLng>();

        for (LatLng l : data) {
            weightedData.add(new WeightedLatLng(l));
        }

        return weightedData;
    }

    /**
     * Creates tile.
     *
     * @param x    X coordinate of tile.
     * @param y    Y coordinate of tile.
     * @param zoom Zoom level.
     * @return image in Tile format
     */
    public Tile getTile(int x, int y, int zoom) {
        double tileWidth = WORLD_WIDTH / Math.pow(2, zoom);

        // Bounds of the current tile
        double minX = x * tileWidth;
        double minY = y * tileWidth;

        // Padding in terms of world width units
        double padding = mRadius * tileWidth / TILE_DIM;

        // Bound of the extended tile to search for points that can influence current tile because of the radius
        double minXextended = x * tileWidth - padding;
        double maxXextended = (x + 1) * tileWidth + padding;
        double minYextended = y * tileWidth - padding;
        double maxYextended = (y + 1) * tileWidth + padding;

        Bounds tileExtendedBounds = new Bounds(minXextended, maxXextended, minYextended, maxYextended);

        // Check if points bound with padding intersects with this bound
        Bounds paddedBounds = new Bounds(mBounds.minX - padding, mBounds.maxX + padding, mBounds.minY - padding, mBounds.maxY + padding);

        if (!tileExtendedBounds.intersects(paddedBounds)) {
            return TileProvider.NO_TILE;
        }

        // Search for all points within tile bounds
        Collection<WeightedLatLng> points = mTree.search(tileExtendedBounds);

        // Deal with the extended search overlap across lat = 180
        double xOffset = 0;
        Collection<WeightedLatLng> wrappedPoints = new ArrayList<>();
        if (minXextended < 0) {
            Bounds overlapBounds = new Bounds(minXextended + WORLD_WIDTH, WORLD_WIDTH, minYextended, maxYextended);
            xOffset = -WORLD_WIDTH;
            wrappedPoints = mTree.search(overlapBounds);
        } else if (maxXextended > WORLD_WIDTH) {
            Bounds overlapBounds = new Bounds(0, maxXextended - WORLD_WIDTH, minYextended, maxYextended);
            xOffset = WORLD_WIDTH;
            wrappedPoints = mTree.search(overlapBounds);
        }

        // If no points, return blank tile
        if (points.isEmpty() && wrappedPoints.isEmpty()) {
            return TileProvider.NO_TILE;
        }

        // Final tile grid
        HeatmapPoint[][] tileGrid = new HeatmapPoint[TILE_DIM][TILE_DIM];

        // Middle of the square around point, so the position of the point inside this square
        int middle = mRadius - 1;

        // Add points to the tile grid taking radius into account
        for (WeightedLatLng point : points) {
            double pointWeight = point.getIntensity();

            // Point indexes inside tileGrid array. Might be outside of the array because it can belong to padding!
            int pointGridX = (int) (((point.getPoint().x - minX) * TILE_DIM) / tileWidth);
            int pointGridY = (int) (((point.getPoint().y - minY) * TILE_DIM) / tileWidth);

            // Size of point's radius square inside one tile would be 2 * mRadius, but we need shorten this
            // if radius square doesn't fit all inside this tile
            int iStart = 0;
            int iEnd = 2 * mRadius;
            int jStart = 0;
            int jEnd = 2 * mRadius;

            if (pointGridX - mRadius < 0) {
                iStart = mRadius - pointGridX;
            }

            if (pointGridX + mRadius > TILE_DIM) {
                iEnd = mRadius - (pointGridX - TILE_DIM);
            }

            if (pointGridY - mRadius < 0) {
                jStart = mRadius - pointGridY;
            }

            if (pointGridY + mRadius > TILE_DIM) {
                jEnd = mRadius - (pointGridY - TILE_DIM);
            }

            try {

                for (int i = iStart; i < iEnd; i++) {
                    for (int j = jStart; j < jEnd; j++) {
                        double distanceToPoint = calculateDistance(i, j, middle, middle);
                        // intensity based on the distance from point calculated using exponential function
                        double intensity = calculateIntensity(distanceToPoint);
                        // weight goes down based on intensity and smoothing but can't be lower than 1
                        double weight = Math.max((intensity * mGradientSmoothing) + (pointWeight - mGradientSmoothing), 1);
                        int tileXIndex = (i - middle) + pointGridX - 1;
                        int tileYIndex = (j - middle) + pointGridY - 1;

                        if (intensity > 0.01) {
                            HeatmapPoint newPoint = new HeatmapPoint(intensity, weight);
                            if (tileGrid[tileXIndex][tileYIndex] != null) {
                                tileGrid[tileXIndex][tileYIndex] = mergeHeatmapPoints(tileGrid[tileXIndex][tileYIndex], newPoint);
                            } else {
                                tileGrid[tileXIndex][tileYIndex] = newPoint;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.d(WeightBasedHeatmapTileProvider.class.getSimpleName(), "Exception while drawing the heatmap.", e);
            }
        }

        for (WeightedLatLng point : wrappedPoints) {

        }

        // Color tileGrid into bitmap
        Bitmap bitmap;
        if (staticMaxIntensity > 0) {
            bitmap = colorize(tileGrid, mColorMap, staticMaxIntensity);
        } else {
            bitmap = colorize(tileGrid, mColorMap, mMaxIntensity[zoom]);
        }

        return convertBitmap(bitmap);
    }

    private double calculateDistance(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double calculateIntensity(double distance) {
        return Math.exp((-distance * distance) / (mRadius * mRadius / 3));
    }

    /**
     * Setter for gradient/color map.
     * User should clear overlay's tile cache (using clearTileCache()) after calling this.
     *
     * @param gradient Gradient to set
     */
    public void setGradient(Gradient gradient) {
        mGradient = gradient;
        mColorMap = gradient.generateColorMap(mOpacity);
    }

    /**
     * Setter for radius.
     * User should clear overlay's tile cache (using clearTileCache()) after calling this.
     *
     * @param radius Radius to set
     */
    public void setRadius(int radius) {
        mRadius = radius;
        // need to recalculate max intensity
        mMaxIntensity = getMaxIntensities(mRadius);
    }

    /**
     * Setter for opacity
     * User should clear overlay's tile cache (using clearTileCache()) after calling this.
     *
     * @param opacity opacity to set
     */
    public void setOpacity(double opacity) {
        mOpacity = opacity;
        // need to recompute kernel color map
        setGradient(mGradient);
    }


    public void setMaxIntensity(double maxIntensity) {
        staticMaxIntensity = maxIntensity;
    }


    /**
     * Gets array of maximum intensity values to use with the heatmap for each zoom level
     * This is the value that the highest color on the color map corresponds to
     *
     * @param radius radius of the heatmap
     * @return array of maximum intensities
     */
    private double[] getMaxIntensities(int radius) {
        // Can go from zoom level 3 to zoom level 22
        double[] maxIntensityArray = new double[MAX_ZOOM_LEVEL];

        // Calculate only if there is no static max intensity set
        if (staticMaxIntensity == 0) {
            // Calculate max intensity for each zoom level
            for (int i = DEFAULT_MIN_ZOOM; i < DEFAULT_MAX_ZOOM; i++) {
                // Each zoom level multiplies viewable size by 2
                maxIntensityArray[i] = getMaxValue(mData, mBounds, radius,
                        (int) (SCREEN_SIZE * Math.pow(2, i - 3)));
                if (i == DEFAULT_MIN_ZOOM) {
                    for (int j = 0; j < i; j++) maxIntensityArray[j] = maxIntensityArray[i];
                }
            }
            for (int i = DEFAULT_MAX_ZOOM; i < MAX_ZOOM_LEVEL; i++) {
                maxIntensityArray[i] = maxIntensityArray[DEFAULT_MAX_ZOOM - 1];
            }
        }
        return maxIntensityArray;
    }

    /**
     * helper function - convert a bitmap into a tile
     *
     * @param bitmap bitmap to convert into a tile
     * @return the tile
     */
    private static Tile convertBitmap(Bitmap bitmap) {
        // Convert it into byte array (required for tile creation)
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] bitmapdata = stream.toByteArray();
        return new Tile(TILE_DIM, TILE_DIM, bitmapdata);
    }

    /* Utility functions below */

    /**
     * Helper function for quadtree creation
     *
     * @param points Collection of WeightedLatLng to calculate bounds for
     * @return Bounds that enclose the listed WeightedLatLng points
     */
    static Bounds getBounds(Collection<WeightedLatLng> points) {

        // Use an iterator, need to access any one point of the collection for starting bounds
        Iterator<WeightedLatLng> iter = points.iterator();

        WeightedLatLng first = iter.next();

        double minX = first.getPoint().x;
        double maxX = first.getPoint().x;
        double minY = first.getPoint().y;
        double maxY = first.getPoint().y;

        while (iter.hasNext()) {
            WeightedLatLng l = iter.next();
            double x = l.getPoint().x;
            double y = l.getPoint().y;
            // Extend bounds if necessary
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }

        return new Bounds(minX, maxX, minY, maxY);
    }

    private HeatmapPoint mergeHeatmapPoints(HeatmapPoint heatmapPoint1, HeatmapPoint heatmapPoint2) {
        double newWeight = weightedAverage(heatmapPoint1.weight, heatmapPoint1.intensity, heatmapPoint2.weight, heatmapPoint2.intensity);
        double newIntensity = Math.min(heatmapPoint1.intensity + heatmapPoint2.intensity, 1);
        return new HeatmapPoint(newIntensity, newWeight);
    }

    private static double weightedAverage(double pointWeight1, double pointIntensity1, double pointWeight2, double pointIntensity2) {
        return ((pointIntensity1 * pointWeight1) + (pointIntensity2 * pointWeight2)) / (pointIntensity1 + pointIntensity2);
    }

    /**
     * Converts a grid of intensity values to a colored Bitmap, using a given color map
     *
     * @param grid     the input grid (assumed to be square)
     * @param colorMap color map (created by generateColorMap)
     * @param max      Maximum intensity value: maps to 100% on gradient
     * @return the colorized grid in Bitmap form, with same dimensions as grid
     */
    private static Bitmap colorize(HeatmapPoint[][] grid, int[] colorMap, double max) {
        // Maximum color value
        int maxColor = colorMap[colorMap.length - 1];
        // Multiplier to "scale" intensity values with, to map to appropriate color
        double colorMapScaling = (colorMap.length - 1) / max;
        // Dimension of the input grid (and dimension of output bitmap)
        int dim = grid.length;

        int i, j, index, col;
        HeatmapPoint point;
        // Array of colors
        int colors[] = new int[dim * dim];
        for (i = 0; i < dim; i++) {
            for (j = 0; j < dim; j++) {
                // [x][y]
                // need to enter each row of x coordinates sequentially (x first)
                // -> [j][i]
                point = grid[j][i];
                index = i * dim + j;

                if (point != null) {
                    col = (int) (point.weight * colorMapScaling);
                    int fromColorMap = colorMap[col];
                    int transparency = (int) (point.intensity * Color.alpha(fromColorMap));
                    // Make it more resilient: cant go outside colorMap
                    if (col < colorMap.length)
                        colors[index] = ColorUtils.setAlphaComponent(fromColorMap, transparency);
                    else colors[index] = ColorUtils.setAlphaComponent(maxColor, transparency);
                } else {
                    colors[index] = Color.TRANSPARENT;
                }
            }
        }

        // Now turn these colors into a bitmap
        Bitmap tile = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);
        // (int[] pixels, int offset, int stride, int x, int y, int width, int height)
        tile.setPixels(colors, 0, dim, 0, 0, dim, dim);
        return tile;
    }

    /**
     * Calculate a reasonable maximum intensity value to map to maximum color intensity
     *
     * @param points    Collection of LatLngs to put into buckets
     * @param bounds    Bucket boundaries
     * @param radius    radius of convolution
     * @param screenDim larger dimension of screen in pixels (for scale)
     * @return Approximate max value
     */
    private static double getMaxValue(Collection<WeightedLatLng> points, Bounds bounds, int radius,
                                      int screenDim) {
        // Approximate scale as if entire heatmap is on the screen
        // ie scale dimensions to larger of width or height (screenDim)
        double minX = bounds.minX;
        double maxX = bounds.maxX;
        double minY = bounds.minY;
        double maxY = bounds.maxY;
        double boundsDim = (maxX - minX > maxY - minY) ? maxX - minX : maxY - minY;

        // Number of buckets: have diameter sized buckets
        int nBuckets = (int) (screenDim / (2 * radius) + 0.5);
        // Scaling factor to convert width in terms of point distance, to which bucket
        double scale = nBuckets / boundsDim;

        // Make buckets
        // Use a sparse array - use LongSparseArray just in case
        LongSparseArray<LongSparseArray<Double>> buckets = new LongSparseArray<LongSparseArray<Double>>();
        //double[][] buckets = new double[nBuckets][nBuckets];

        // Assign into buckets + find max value as we go along
        double x, y;
        double max = 0;
        for (WeightedLatLng l : points) {
            x = l.getPoint().x;
            y = l.getPoint().y;

            int xBucket = (int) ((x - minX) * scale);
            int yBucket = (int) ((y - minY) * scale);

            // Check if x bucket exists, if not make it
            LongSparseArray<Double> column = buckets.get(xBucket);
            if (column == null) {
                column = new LongSparseArray<Double>();
                buckets.put(xBucket, column);
            }
            // Check if there is already a y value there
            Double value = column.get(yBucket);
            if (value == null) {
                value = 0.0;
            }
            value += l.getIntensity();
            // Yes, do need to update it, despite it being a Double.
            column.put(yBucket, value);

            if (value > max) max = value;
        }

        return max;
    }

    private class HeatmapPoint {
        double intensity;
        double weight;

        HeatmapPoint(double intensity, double weight) {
            this.intensity = intensity;
            this.weight = weight;
        }
    }
}
