/*
 * Copyright 2019 Esri.
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

package com.esri.samples.na.closest_facility_static;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.esri.arcgisruntime.tasks.networkanalysis.ClosestFacilityParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.ClosestFacilityResult;
import com.esri.arcgisruntime.tasks.networkanalysis.ClosestFacilityRoute;
import com.esri.arcgisruntime.tasks.networkanalysis.ClosestFacilityTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Facility;
import com.esri.arcgisruntime.tasks.networkanalysis.Incident;

public class ClosestFacilityStaticSample extends Application {

  private MapView mapView;
  ClosestFacilityTask closestFacilityTask;
  ArrayList<Incident> incidentsList = new ArrayList<>();
  ArrayList<Facility> facilitiesList = new ArrayList<>();
  SimpleLineSymbol simpleLineSymbol;
  Button solveRoutesButton;
  Button resetButton;

  @Override
  public void start(Stage stage) throws Exception {

    try {
      // create stack pane and application scene
      StackPane stackPane = new StackPane();
      Scene scene = new Scene(stackPane);

      // set title, size, and add scene to stage
      stage.setTitle("Closest Facility (Static) Sample");
      stage.setWidth(800);
      stage.setHeight(700);
      stage.setScene(scene);
      stage.show();

      VBox controlsVBox = new VBox(6);
      controlsVBox.setBackground(new Background(new BackgroundFill(Paint.valueOf("rgba(0,0,0,0.3)"), CornerRadii.EMPTY,
              Insets.EMPTY)));
      controlsVBox.setPadding(new Insets(10.0));
      controlsVBox.setMaxSize(150, 50);
      controlsVBox.getStyleClass().add("panel-region");

      // create buttons
      solveRoutesButton = new Button("Solve Routes");
      solveRoutesButton.setMaxWidth(Double.MAX_VALUE);
      solveRoutesButton.setDisable(true);
      resetButton = new Button("Reset");
      resetButton.setMaxWidth(Double.MAX_VALUE);
      resetButton.setDisable(true);

      // add buttons to the control panel
      controlsVBox.getChildren().addAll(solveRoutesButton, resetButton);

      // create a ArcGISMap with a Basemap instance with an Imagery base layer
      ArcGISMap map = new ArcGISMap(Basemap.createStreetsWithReliefVector());

      // set the map to be displayed in this view
      mapView = new MapView();
      mapView.setMap(map);

      // Add a graphics overlay to the map (will be used later to display routes)
      mapView.getGraphicsOverlays().add(new GraphicsOverlay());

      // create Symbols for displaying facilities
      PictureMarkerSymbol facilitySymbol = createSymbol("https://static.arcgis.com/images/Symbols/SafetyHealth/FireStation.png");
      PictureMarkerSymbol incidentSymbol = createSymbol("https://static.arcgis.com/images/Symbols/SafetyHealth/esriCrimeMarker_56_Gradient.png");

      // create a line symbol to mark the route
      simpleLineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0x4D0000FF, 5.0f);

      // create a ClosestFacilityTask
      closestFacilityTask = new ClosestFacilityTask("https://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/ClosestFacility");

      // Create a table for facilities using the FeatureServer
      FeatureTable facilitiesFeatureTable = new ServiceFeatureTable("https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/ArcGIS/rest/services/San_Diego_Facilities/FeatureServer/0");

      // Create a feature layer from the table, apply facilities icon
      FeatureLayer facilitiesFeatureLayer = new FeatureLayer(facilitiesFeatureTable);
      facilitiesFeatureLayer.setRenderer(new SimpleRenderer(facilitySymbol));

      // Create a table for incidents using the FeatureServer
      FeatureTable incidentsFeatureTable = new ServiceFeatureTable("https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/ArcGIS/rest/services/San_Diego_Incidents/FeatureServer/0");

      // Create a feature layer from the table, apply incident icon
      FeatureLayer incidentsFeatureLayer = new FeatureLayer(incidentsFeatureTable);
      incidentsFeatureLayer.setRenderer(new SimpleRenderer(incidentSymbol));

      // Add the layers to the map
      map.getOperationalLayers().addAll(Arrays.asList(facilitiesFeatureLayer, incidentsFeatureLayer));

      // Wait for both layers to load
      facilitiesFeatureLayer.loadAsync();
      incidentsFeatureLayer.loadAsync();

      // TODO: Is there any possibility to combine the two Tasks and only use one listener?
      facilitiesFeatureLayer.addDoneLoadingListener(() -> {
        incidentsFeatureLayer.addDoneLoadingListener(() -> {
          if (facilitiesFeatureLayer.getLoadStatus() == LoadStatus.LOADED && incidentsFeatureLayer.getLoadStatus() == LoadStatus.LOADED) {

            Envelope fullFeatureLayerExtent = GeometryEngine.combineExtents(facilitiesFeatureLayer.getFullExtent(), incidentsFeatureLayer.getFullExtent());
            mapView.setViewpointGeometryAsync(fullFeatureLayerExtent, 90);

            // Create query parameters to select all features
            QueryParameters queryParameters = new QueryParameters();
            queryParameters.setWhereClause("1=1");

            // Retrieve a list of all facilities
            ListenableFuture<FeatureQueryResult> result = facilitiesFeatureTable.queryFeaturesAsync(queryParameters);
            result.addDoneListener(() -> {
              try {
                FeatureQueryResult facilitiesResult = result.get();
                for (Feature facilityFeature : facilitiesResult) {
                  facilitiesList.add(new Facility(facilityFeature.getGeometry().getExtent().getCenter()));
                }

              } catch (InterruptedException | ExecutionException e) {
                displayMessage("Error retrieving list of facilities", e.getMessage());
              }
            });

            // Retrieve a list of all incidents
            ListenableFuture<FeatureQueryResult> incidentsQueryResult = incidentsFeatureTable.queryFeaturesAsync(queryParameters);
            incidentsQueryResult.addDoneListener(() -> {
              try {
                FeatureQueryResult incidentsResult = incidentsQueryResult.get();

                for (Feature incidentFeature : incidentsResult) {
                  incidentsList.add(new Incident(incidentFeature.getGeometry().getExtent().getCenter()));
                }

              } catch (InterruptedException | ExecutionException e) {
                displayMessage("Error retrieving list of incidents", e.getMessage());
              }
            });

            // enable button and resolve button press
            solveRoutesButton.setDisable(false);
            solveRoutesButton.setOnAction(e -> {
              try {
                solveRoutes();
              } catch (Exception ex) {
                ex.printStackTrace();
              }
            });

            // handle reset button presses
            resetButton.setOnAction(e -> {
              try {
                resetRoutes();
              } catch (Exception ex) {
                ex.printStackTrace();
              }
            });
          }
        });
      });

      // add the map view and control panel to stack pane
      stackPane.getChildren().addAll(mapView, controlsVBox);
      StackPane.setAlignment(controlsVBox, Pos.TOP_LEFT);
      StackPane.setMargin(controlsVBox, new Insets(10, 0, 0, 10));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // task to find the closes route between an incident and a facility
  private void solveRoutes() {
    try {
      closestFacilityTask.addDoneLoadingListener(() -> {
        if (closestFacilityTask.getLoadStatus() == LoadStatus.LOADED) {
          try {
            ClosestFacilityParameters closestFacilityParameters = closestFacilityTask.createDefaultParametersAsync().get();
            closestFacilityParameters.setFacilities(facilitiesList);
            closestFacilityParameters.setIncidents(incidentsList);

            // solve closest facilities
            try {
              // use the task to solve for the closest facility
              ListenableFuture<ClosestFacilityResult> closestFacilityTaskResult = closestFacilityTask.solveClosestFacilityAsync(closestFacilityParameters);
              closestFacilityTaskResult.addDoneListener(() -> {
                try {
                  ClosestFacilityResult closestFacilityResult = closestFacilityTaskResult.get();

                  // find the closest facility for each incident
                  for (int i = 0; i < incidentsList.size(); i++) {

                    // get the index of the closest facility to incident. (i) is the index of the incident, [0] is the index of the closest facility.
                    Integer closestFacilityIndex = closestFacilityResult.getRankedFacilityIndexes(i).get(0);

                    // get the route to the closest facility.
                    ClosestFacilityRoute closestFacilityRoute = closestFacilityResult.getRoute(closestFacilityIndex, i);

                    // display the route on the graphics overlay
                    mapView.getGraphicsOverlays().get(0).getGraphics().add(new Graphic(closestFacilityRoute.getRouteGeometry(), simpleLineSymbol));

                    // disable the solve button
                    solveRoutesButton.setDisable(true);

                    // enable the reset button
                    resetButton.setDisable(false);
                  }

                } catch (ExecutionException | InterruptedException e) {
                  e.printStackTrace();
                }
              });

            } catch (Exception e) {
              e.printStackTrace();
            }

          } catch (InterruptedException | ExecutionException e) {
            displayMessage("Error getting default route parameters", e.getMessage());
          }

        } else {
          displayMessage("Error loading route task", closestFacilityTask.getLoadError().getMessage());
        }
      });

      closestFacilityTask.loadAsync();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Creates a PictureMarkerSymbol from a URI and sizes it appropriately
   *
   * @param uri the URI of the picture to be used for the symbol
   */
  private PictureMarkerSymbol createSymbol(String uri) {
    PictureMarkerSymbol symbol = new PictureMarkerSymbol(uri);
    symbol.setHeight(30);
    symbol.setWidth(30);
    return symbol;
  }

  private void resetRoutes() {
    // clear the route graphics.
    mapView.getGraphicsOverlays().get(0).getGraphics().clear();

    // Reset the buttons
    solveRoutesButton.setDisable(false);
    resetButton.setDisable(true);
  }

  /**
   * Shows a message in an alert dialog.
   *
   * @param title   title of alert
   * @param message message to display
   */
  private void displayMessage(String title, String message) {

    Platform.runLater(() -> {
      Alert dialog = new Alert(Alert.AlertType.INFORMATION);
      dialog.setHeaderText(title);
      dialog.setContentText(message);
      dialog.showAndWait();
    });
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
