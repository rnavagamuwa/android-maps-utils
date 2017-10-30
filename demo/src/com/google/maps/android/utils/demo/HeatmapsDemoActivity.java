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

package com.google.maps.android.utils.demo;

import android.graphics.Color;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapMode;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A demo of the Heatmaps library. Demonstrates how the HeatmapTileProvider can be used to create
 * a colored map overlay that visualises many points of weighted importance/intensity, with
 * different colors representing areas of high and low concentration/combined intensity of points.
 */
public class HeatmapsDemoActivity extends BaseDemoActivity {

    /**
     * Alternative radius for convolution
     */
    private static final int ALT_HEATMAP_RADIUS = 30;

    /**
     * Alternative opacity of heatmap overlay
     */
    private static final double ALT_HEATMAP_OPACITY = 0.7;

    /**
     * Alternative heatmap gradient (green -> red)
     */
    private static final int[] ALT_HEATMAP_GRADIENT_COLORS = {
            Color.parseColor("#79BC6A"),
            Color.parseColor("#BBCF4C"),
            Color.parseColor("#EEC20B"),
            Color.parseColor("#F29305"),
            Color.parseColor("#E50000"),

    };

    public static final float[] ALT_HEATMAP_GRADIENT_START_POINTS = {
            0.0f, 0.25f, 0.50f, 0.75f, 1.0f
    };

    public static final Gradient ALT_HEATMAP_GRADIENT = new Gradient(ALT_HEATMAP_GRADIENT_COLORS,
            ALT_HEATMAP_GRADIENT_START_POINTS);

    private HeatmapTileProvider mProvider;
    private TileOverlay mOverlay;
    private Collection<WeightedLatLng> mData;

    @Override
    protected int getLayoutId() {
        return R.layout.heatmaps_demo;
    }

    @Override
    protected void startDemo() {
        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(50.067959, 19.91266), 17));

        mData = new ArrayList<>(4);
        mData.add(new WeightedLatLng(new LatLng(50.067959, 19.91266), 99));
        mData.add(new WeightedLatLng(new LatLng(50.064507, 19.920777), 15));
        mData.add(new WeightedLatLng(new LatLng(50.067951, 19.91289), 1));
        mData.add(new WeightedLatLng(new LatLng(50.067873, 19.912719), 10));

        if(mProvider == null) {
            mProvider = new HeatmapTileProvider.Builder()
                    .weightedData(mData)
                    .setHeatmapMode(HeatmapMode.POINTS_WEIGHT)
                    .gradient(ALT_HEATMAP_GRADIENT)
                    .maxIntensity(100)
                    .radius(ALT_HEATMAP_RADIUS)
                    .opacity(ALT_HEATMAP_OPACITY)
                    .gradientSmoothing(10)
                    .build();
            mOverlay = getMap().addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
        }

        mOverlay.clearTileCache();
        getMap().setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                LatLng latLng = getMap().getCameraPosition().target;
                double metersPerPixel = 156543.03392 * Math.cos(latLng.latitude * Math.PI / 180) / Math.pow(2, getMap().getCameraPosition().zoom);
                final double result = Math.min(ALT_HEATMAP_RADIUS / metersPerPixel, ALT_HEATMAP_RADIUS);
                        mProvider.setRadius((int) result);
                        mOverlay.clearTileCache();
            }
        });
    }


}
