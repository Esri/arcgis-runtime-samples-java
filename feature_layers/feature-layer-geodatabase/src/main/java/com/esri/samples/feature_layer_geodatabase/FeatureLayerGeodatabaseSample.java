/*
 * Copyright 2017 Esri.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.esri.samples.feature_layer_geodatabase;

import java.io.File;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.data.Geodatabase;
import com.esri.arcgisruntime.data.GeodatabaseFeatureTable;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.view.MapView;

public class FeatureLayerGeodatabaseSample extends Application {

  private MapView mapView;
  // keep loadables in scope to avoid garbage collection
  private Geodatabase geodatabase;
  private FeatureLayer featureLayer;

  @Override
  public void start(Stage stage) {

    try {

      // create stack pane and JavaFX app scene
      StackPane stackPane = new StackPane();
      Scene fxScene = new Scene(stackPane);

      // set title, size, and add JavaFX scene to stage
      stage.setTitle("Feature Layer Geodatabase Sample");
      stage.setWidth(800);
      stage.setHeight(700);
      stage.setScene(fxScene);
      stage.show();

      // authentication with an API key or named user is required to access basemaps and other location services
      String yourAPIKey = System.getProperty("apiKey");
      ArcGISRuntimeEnvironment.setApiKey(yourAPIKey);

      // create a map with the streets basemap style
      ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_STREETS);

      // create a map view and set the map to it
      mapView = new MapView();
      mapView.setMap(map);

      // create geodatabase from local resource
      File geodatabaseFile = new File(System.getProperty("data.dir"), "./samples-data/los_angeles/LA_Trails" +
              ".geodatabase");
      geodatabase = new Geodatabase(geodatabaseFile.getAbsolutePath());
      geodatabase.addDoneLoadingListener(() -> {
        if (geodatabase.getLoadStatus() == LoadStatus.LOADED) {
          // access the geodatabase's feature table Trailheads
          GeodatabaseFeatureTable geodatabaseFeatureTable = geodatabase.getGeodatabaseFeatureTable("Trailheads");
          geodatabaseFeatureTable.loadAsync();
          // create a layer from the geodatabase feature table above and add to map
          featureLayer = new FeatureLayer(geodatabaseFeatureTable);
          featureLayer.addDoneLoadingListener(() -> {
            if (featureLayer.getLoadStatus() == LoadStatus.LOADED) {
              // set viewpoint to the location of feature layer's features
              mapView.setViewpointCenterAsync(featureLayer.getFullExtent().getCenter(), 1000000);
            } else {
              Alert alert = new Alert(Alert.AlertType.ERROR, "Feature Layer Failed to Load!");
              alert.show();
            }
          });
          // display feature layer to the map view
          map.getOperationalLayers().add(featureLayer);
        } else {
          Alert alert = new Alert(Alert.AlertType.ERROR, "Geodatabase Failed to Load!");
          alert.show();
        }
      });
      // load geodatabase
      geodatabase.loadAsync();

      // add the map view to stack pane
      stackPane.getChildren().addAll(mapView);
    } catch (Exception e) {
      // on any error, display the stack trace.
      e.printStackTrace();
    }
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
