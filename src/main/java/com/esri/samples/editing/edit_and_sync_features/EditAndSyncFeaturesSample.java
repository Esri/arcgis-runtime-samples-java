/* Copyright 2017 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.esri.samples.editing.edit_and_sync_features;

import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.samples.tiledlayers.tile_cache.TileCacheSample;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class EditAndSyncFeaturesSample extends Application {

    private Button geodatabaseButton;

    private MapView mapView;
    private GraphicsOverlay graphicsOverlay;

    @Override
    public void start(Stage stage) {

        try{
            // create stack pane and application scene
            StackPane stackPane = new StackPane();
            Scene scene = new Scene(stackPane);

            // set title, size and add scene to stage
            stage.setTitle("Edit and Sync Features Sample");
            stage.setWidth(800);
            stage.setHeight(700);
            stage.setScene(scene);
            stage.show();

            // create a map view and add a map
            mapView = new MapView();
            // create a graphics overlay and symbol to mark the extent
            graphicsOverlay = new GraphicsOverlay();
            mapView.getGraphicsOverlays().add(graphicsOverlay);

            // load cached tiles
            loadTileCache();

//            1. Create a `GeodatabaseSyncTask` from a URL.
//            1. Use `createDefaultGenerateGeodatabaseParametersAsync(...)` to create `GenerateGeodatabaseParameters` from the `GeodatabaseSyncTask`, passing in an `Envelope` argument.
//            1. Create a `GenerateGeodatabaseJob` from the `GeodatabaseSyncTask` using `generateGeodatabaseAsync(...)` passing in parameters and a path to the local geodatabase.
//            1. Start the `GenerateGeodatabaseJob` and, on success, load the `Geodatabase`.
//            1. On successful loading, call `getGeodatabaseFeatureTables()` on the `Geodatabase` and add it to the `ArcGISMap`'s operational layers.
//            1. To sync changes between the local and web geodatabases:
//            1. Define `SyncGeodatabaseParameters` including setting the `SyncGeodatabaseParameters.SyncDirection`.
//            1. Create a `SyncGeodatabaseJob` from `GeodatabaseSyncTask` using `.syncGeodatabaseAsync(...)` passing the `SyncGeodatabaseParameters` and `Geodatabase` as arguments.
//            1. Start the `SyncGeodatabaseJob`.

            //



            // add map view to stack pane
            stackPane.getChildren().add(mapView);

        } catch (Exception e) {
            // on any error, display the stack trace
            e.printStackTrace();
        }
    }

    /*
     * Load local tile cache.
     */
    private void loadTileCache(){
        // use local tile package for the base map
        TileCache sanFranciscoTileCache = new TileCache("samples-data/sanfrancisco/SanFrancisco.tpk");
        ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer(sanFranciscoTileCache);
        Basemap basemap = new Basemap(tiledLayer);
        final ArcGISMap map = new ArcGISMap(basemap);
        mapView.setMap(map);
    }


    /**
     * Stops and releases all resources used in application.
     */
    @Override
    public void stop() {

        if (mapView != null) {
            mapView.dispose();
        }
    }

    /**
     * Opens and runs application.
     *
     * @param args arguments passed to this application
     */
    public static void main(String[] args) {

        Application.launch(args);
    }
}
