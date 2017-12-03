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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.WeightBasedHeatmapTileProvider;
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
    private static final int ALT_HEATMAP_RADIUS = 50;

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

    private WeightBasedHeatmapTileProvider mProvider;
    private TileOverlay mOverlay;
    private Collection<WeightedLatLng> mData;

    @Override
    protected int getLayoutId() {
        return R.layout.heatmaps_demo;
    }

    @Override
    protected void startDemo() {
        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(50.067959, 19.91266), 14));

        getMap().addMarker(new MarkerOptions().position(new LatLng(49.986111, 20.061667)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.193139, 20.288717)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.740278, 19.588611)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.061389, 19.938333)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.174722, 20.986389)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.064507, 19.920777)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.3, 19.95)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.833333, 19.940556)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.477778, 20.03)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.975, 19.828333)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.357778, 20.0325)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.0125, 20.988333)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.067959, 19.91266)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.418588, 20.323788)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.62113, 20.710777)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.039167, 19.220833)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.970495, 19.837214)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.701667, 20.425556)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.078429, 20.050861)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.895, 21.054167)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.27722, 19.569658)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.968889, 20.606389)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.51232, 19.63755)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.018077, 20.989849)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.081698, 19.895629)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.968889, 20.43)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.279167, 19.559722)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.067947, 19.912865)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.654444, 21.159167)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.099606, 20.016707)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.357778, 20.0325)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.296628, 19.959694)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.019014, 21.002474)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.056829, 19.926414)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.616667, 20.7)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(49.883333, 19.5)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.054217, 19.943289)));
        getMap().addMarker(new MarkerOptions().position(new LatLng(50.133333, 19.4)));

        mData = new ArrayList<>(38);
        mData.add(new WeightedLatLng(new LatLng(49.986111, 20.061667), 1));
        mData.add(new WeightedLatLng(new LatLng(50.193139, 20.288717), 1));
        mData.add(new WeightedLatLng(new LatLng(49.740278, 19.588611), 1));
        mData.add(new WeightedLatLng(new LatLng(50.061389, 19.938333), 1));
        mData.add(new WeightedLatLng(new LatLng(50.174722, 20.986389), 1));
        mData.add(new WeightedLatLng(new LatLng(50.064507, 19.920777), 23));
        mData.add(new WeightedLatLng(new LatLng(49.3, 19.95), 1));
        mData.add(new WeightedLatLng(new LatLng(49.833333, 19.940556), 1));
        mData.add(new WeightedLatLng(new LatLng(49.477778, 20.03), 1));
        mData.add(new WeightedLatLng(new LatLng(49.975, 19.828333), 1));
        mData.add(new WeightedLatLng(new LatLng(50.357778, 20.0325), 1));
        mData.add(new WeightedLatLng(new LatLng(50.0125, 20.988333), 1));
        mData.add(new WeightedLatLng(new LatLng(50.067959, 19.91266), 76));
        mData.add(new WeightedLatLng(new LatLng(49.418588, 20.323788), 63));
        mData.add(new WeightedLatLng(new LatLng(49.62113, 20.710777), 25));
        mData.add(new WeightedLatLng(new LatLng(50.039167, 19.220833), 1));
        mData.add(new WeightedLatLng(new LatLng(49.970495, 19.837214), 48));
        mData.add(new WeightedLatLng(new LatLng(49.701667, 20.425556), 1));
        mData.add(new WeightedLatLng(new LatLng(50.078429, 20.050861), 43));
        mData.add(new WeightedLatLng(new LatLng(49.895, 21.054167), 1));
        mData.add(new WeightedLatLng(new LatLng(50.27722, 19.569658), 50));
        mData.add(new WeightedLatLng(new LatLng(49.968889, 20.606389), 1));
        mData.add(new WeightedLatLng(new LatLng(49.51232, 19.63755), 29));
        mData.add(new WeightedLatLng(new LatLng(50.018077, 20.989849), 50));
        mData.add(new WeightedLatLng(new LatLng(50.081698, 19.895629), 32));
        mData.add(new WeightedLatLng(new LatLng(49.968889, 20.43), 1));
        mData.add(new WeightedLatLng(new LatLng(50.279167, 19.559722), 1));
        mData.add(new WeightedLatLng(new LatLng(50.067947, 19.912865), 52));
        mData.add(new WeightedLatLng(new LatLng(49.654444, 21.159167), 1));
        mData.add(new WeightedLatLng(new LatLng(50.099606, 20.016707), 27));
        mData.add(new WeightedLatLng(new LatLng(50.357778, 20.0325), 41));
        mData.add(new WeightedLatLng(new LatLng(49.296628, 19.959694), 15));
        mData.add(new WeightedLatLng(new LatLng(50.019014, 21.002474), 57));
        mData.add(new WeightedLatLng(new LatLng(50.056829, 19.926414), 51));
        mData.add(new WeightedLatLng(new LatLng(49.616667, 20.7), 1));
        mData.add(new WeightedLatLng(new LatLng(49.883333, 19.5), 1));
        mData.add(new WeightedLatLng(new LatLng(50.054217, 19.943289), 41));
        mData.add(new WeightedLatLng(new LatLng(50.133333, 19.4), 1));

        if(mProvider == null) {
            mProvider = new WeightBasedHeatmapTileProvider.Builder()
                    .weightedData(mData)
                    .gradient(ALT_HEATMAP_GRADIENT)
                    .maxIntensity(100)
                    .radius(ALT_HEATMAP_RADIUS)
                    .opacity(ALT_HEATMAP_OPACITY)
                    .gradientSmoothing(10)
                    .build();
            mOverlay = getMap().addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
        }

        mOverlay.clearTileCache();
    }

}
