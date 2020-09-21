/*
 * Copyright 2020 Esri
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

package com.esri.samples.show_location_history;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.PolylineBuilder;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.location.LocationDataSource;
import com.esri.arcgisruntime.location.SimulatedLocationDataSource;
import com.esri.arcgisruntime.location.SimulationParameters;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;

import org.apache.commons.io.IOUtils;

public class ShowLocationHistorySample extends Application {

  private MapView mapView;
  private boolean isTrackingEnabled = false;
  private Polyline routePolyline;

  @Override
  public void start(Stage stage) {

    try {
      // create stack pane and application scene
      StackPane stackPane = new StackPane();
      Scene scene = new Scene(stackPane);
      scene.getStylesheets().add(getClass().getResource("/show_location_history/style.css").toExternalForm());

      // set title, size, and add scene to stage
      stage.setTitle("Show Location History Sample");
      stage.setWidth(800);
      stage.setHeight(700);
      stage.setScene(scene);
      stage.show();

      // create a map with a dark gray canvas basemap
      ArcGISMap map = new ArcGISMap(Basemap.createDarkGrayCanvasVector());

      // create a map view and set its map
      mapView = new MapView();
      mapView.setMap(map);

      // set the map views's viewpoint centered on Los Angeles, California and scaled
      mapView.setViewpoint(new Viewpoint(new Point(-13185535.98, 4037766.28,
        SpatialReferences.getWebMercator()), 7000));

      // create a button that toggles the location tracking
      Button trackingButton = new Button("Start tracking");
      trackingButton.setDisable(true);

      // create a graphics overlay for the points and use a red circle for the symbols
      GraphicsOverlay locationHistoryOverlay = new GraphicsOverlay();
      SimpleMarkerSymbol locationSymbol = new SimpleMarkerSymbol(
        SimpleMarkerSymbol.Style.CIRCLE, 0xFFFF0000, 10f);
      SimpleRenderer locationHistoryRenderer = new SimpleRenderer(locationSymbol);
      locationHistoryOverlay.setRenderer(locationHistoryRenderer);

      // create a graphics overlay for the lines connecting the points and use a green line for the symbol
      GraphicsOverlay locationHistoryLineOverlay = new GraphicsOverlay();
      SimpleLineSymbol locationLineSymbol = new SimpleLineSymbol(
        SimpleLineSymbol.Style.SOLID, 0xFF00FF00, 2.0f);
      SimpleRenderer locationHistoryLineRenderer = new SimpleRenderer(locationLineSymbol);
      locationHistoryLineOverlay.setRenderer(locationHistoryLineRenderer);

      // add the graphics overlays to the map view
      mapView.getGraphicsOverlays().addAll(Arrays.asList(locationHistoryOverlay, locationHistoryLineOverlay));

      // create a polyline builder to connect the location points
      PolylineBuilder polylineBuilder = new PolylineBuilder(SpatialReferences.getWebMercator());

      // enable the button interactions when the map is loaded
      map.addDoneLoadingListener(() -> {

        if (map.getLoadStatus() == LoadStatus.LOADED) {
          trackingButton.setDisable(false);

          try {
            // access the json of the location points
            String polylineData = IOUtils.toString(getClass().getResourceAsStream("/show_location_history/polyline_data.json"),
              StandardCharsets.UTF_8);
            // create a polyline from the location points
            routePolyline = (Polyline) Geometry.fromJson(polylineData,
              SpatialReferences.getWebMercator());

          } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Error loading simulated data").show();
          }

          // create a simulated location data source
          SimulatedLocationDataSource simulatedLocationDataSource = new SimulatedLocationDataSource();
          // set the location of the simulated location data source with simulation parameters to set a consistent velocity
          simulatedLocationDataSource.setLocations(
            routePolyline, new SimulationParameters(Calendar.getInstance(), 30.0, 0.0, 0.0));

          // start the simulated location data source
          simulatedLocationDataSource.startAsync();
          simulatedLocationDataSource.addLocationChangedListener(locationChangedEvent -> {

            // if location tracking is turned off, do not draw points or extend the polyline
            if (!isTrackingEnabled) {
              return;
            }
            // get the position as a point from locationChangedEvent
            Point position = locationChangedEvent.getLocation().getPosition();

            // add the new point to the polyline
            polylineBuilder.addPoint(position);

            // add the new point to the graphics overlay
            locationHistoryOverlay.getGraphics().add(new Graphic(position));

            // reset the old polyline connecting the points
            locationHistoryLineOverlay.getGraphics().clear();

            // add the updated polyline to the graphics overlay
            locationHistoryLineOverlay.getGraphics().add(new Graphic(polylineBuilder.toGeometry()));
          });

          // configure the map view's location display to follow the simulated location data source
          LocationDisplay locationDisplay = mapView.getLocationDisplay();
          locationDisplay.setLocationDataSource(simulatedLocationDataSource);
          locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
          mapView.getLocationDisplay().setInitialZoomScale(7000);

          trackingButton.setOnAction(event -> {
            // if the user has panned away from the location display, turn it on again
            if (mapView.getLocationDisplay().getAutoPanMode() == LocationDisplay.AutoPanMode.OFF) {
              mapView.getLocationDisplay().setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
            }
            // toggle the location tracking when the button is clicked
            if (isTrackingEnabled) {
              trackingButton.setText("Start Tracking");
            } else {
              trackingButton.setText("Stop Tracking");
            }
            isTrackingEnabled = !isTrackingEnabled;
          });

        } else {
          new Alert(Alert.AlertType.ERROR, "Map failed to load").show();
        }
      });

      // add the map view and tracking button to the stack pane
      stackPane.getChildren().addAll(mapView, trackingButton);
      StackPane.setAlignment(trackingButton, Pos.TOP_LEFT);
      StackPane.setMargin(trackingButton, new Insets(10, 0, 0, 10));

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
