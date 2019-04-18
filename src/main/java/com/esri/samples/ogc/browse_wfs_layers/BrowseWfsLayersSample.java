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

package com.esri.samples.ogc.browse_wfs_layers;

import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.ogc.wfs.OgcAxisOrder;
import com.esri.arcgisruntime.ogc.wfs.WfsFeatureTable;
import com.esri.arcgisruntime.ogc.wfs.WfsLayerInfo;
import com.esri.arcgisruntime.ogc.wfs.WfsService;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BrowseWfsLayersSample extends Application {

  private MapView mapView;
  private CheckBox checkBox;
  private ArcGISMap map;
  private ProgressIndicator progressIndicator;

  @Override
  public void start(Stage stage) throws Exception {

    // create stack pane and JavaFX app scene
    StackPane stackPane = new StackPane();
    Scene scene = new Scene(stackPane);
    scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

    // set title, size, and add JavaFX scene to stage
    stage.setTitle("Browse WFS Layers");
    stage.setWidth(800);
    stage.setHeight(700);
    stage.setScene(scene);
    stage.show();

    // create a control panel
    VBox controlsVBox = new VBox(6);
    controlsVBox.setBackground(new Background(new BackgroundFill(Paint.valueOf("rgba(0,0,0,0.3)"), CornerRadii.EMPTY, Insets.EMPTY)));
    controlsVBox.setPadding(new Insets(10.0));
    controlsVBox.setMaxSize(170, 200);
    controlsVBox.getStyleClass().add("panel-region");

    // create a check box to swap coordinate order
    checkBox = new CheckBox("Swap coordinate order");
    checkBox.setMaxWidth(Double.MAX_VALUE);
    checkBox.setWrapText(true);

    // create a label to display when checkbox is active: the WFSLayerInfos in this sample do not require coordinate swapping to display
    Label label = new Label("Data in this sample doesn't require coord swapping to display. Uncheck to display data.");
    label.setStyle("-fx-font-size:14px; -fx-text-fill: white;");
    label.setVisible(false);

    checkBox.setOnAction(event -> {
      label.setVisible(checkBox.isSelected());
    });

    // create a list to hold the names of the bookmarks
    ListView<WfsLayerInfo> wfsLayerNamesListView = new ListView<>();
    wfsLayerNamesListView.setMaxHeight(160);

    // create a progress indicator
    progressIndicator = new ProgressIndicator();
    progressIndicator.setVisible(false);

    // create an ArcGISMap with topographic basemap and set it to the map view
    map = new ArcGISMap(Basemap.createImagery());
    mapView = new MapView();
    mapView.setMap(map);

    // create a WFS service with a URL and load it
    WfsService wfsService = new WfsService("https://dservices2.arcgis.com/ZQgQTuoyBrtmoGdP/arcgis/services/Seattle_Downtown_Features/WFSServer?service=wfs&request=getcapabilities");
    wfsService.loadAsync();

    // when the WFS service has loaded, add its layer information to the list view for browsing
    wfsService.addDoneLoadingListener(() -> {
      if (wfsService.getLoadStatus() == LoadStatus.LOADED) {
        // add the list of WFS layers to the list view
        List<WfsLayerInfo> wfsLayerInfos = wfsService.getServiceInfo().getLayerInfos();
        wfsLayerNamesListView.getItems().addAll(wfsLayerInfos);
      } else {
        Alert alert = new Alert(Alert.AlertType.ERROR, "WFS Service Failed to Load!");
        alert.show();
      }
    });

    // populate the list view with layer names
    wfsLayerNamesListView.setCellFactory(list -> new ListCell<>() {

      @Override
      protected void updateItem(WfsLayerInfo wfsLayerInfo, boolean bln) {
        super.updateItem(wfsLayerInfo, bln);
        if (wfsLayerInfo != null) {
          String fullNameOfWfsLayer = wfsLayerInfo.getName();
          String[] split = fullNameOfWfsLayer.split(":");
          String smallName = split[1];
          setText(smallName);
        }
      }
    });

    // load the selected layer from the list view when the layer is selected
    wfsLayerNamesListView.getSelectionModel().selectedItemProperty().addListener(observable -> {
      updateMap(wfsLayerNamesListView.getSelectionModel().getSelectedItem());
    });

    // add the list view, button and check box to the control panel
    controlsVBox.getChildren().addAll(wfsLayerNamesListView, checkBox);
    // add the mapview to the stackpane
    stackPane.getChildren().addAll(mapView, controlsVBox, progressIndicator, label);
    StackPane.setAlignment(controlsVBox, Pos.TOP_LEFT);
    StackPane.setMargin(controlsVBox, new Insets(10, 0, 0, 10));
  }

  /**
   * Adds a WfsLayerInfo to the map's operational layers, with a random color renderer.
   * @param wfsLayerInfo the WfsLayerInfo that the map will display
   */
  private void updateMap(WfsLayerInfo wfsLayerInfo){

    progressIndicator.setVisible(true);

    // clear the map's operational layers
    map.getOperationalLayers().clear();

    // create a WFSFeatureTable from the WFSLayerInfo
    WfsFeatureTable wfsFeatureTable = new WfsFeatureTable(wfsLayerInfo);

    // set the feature request mode to manual. The table must be manually populated as panning and zooming won't request features automatically.
    wfsFeatureTable.setFeatureRequestMode(ServiceFeatureTable.FeatureRequestMode.MANUAL_CACHE);
    // define the coordinate order for the WFS service. NO_SWAP will keep the co-ordinates in the order they are retrieved from the WFS service; SWAP will reverse the order.
    // note: the data in this sample does not need swapped, however, other WFS service data may require it.
    wfsFeatureTable.setAxisOrder(checkBox.isSelected() ? OgcAxisOrder.SWAP : OgcAxisOrder.NO_SWAP);

    // create a feature layer to visualize the WFS features
    FeatureLayer wfsFeatureLayer = new FeatureLayer(wfsFeatureTable);

    // populate the table and then remove progress indicator and set the viewpoint to that of the layer's full extent when done.
    wfsFeatureTable.populateFromServiceAsync(new QueryParameters(), false, null ).addDoneListener(()->{
      progressIndicator.setVisible(false);
      mapView.setViewpointGeometryAsync(wfsFeatureLayer.getFullExtent(), 50);

    });

    // apply a renderer to the feature layer once the table is loaded (the renderer is based on the table's geometry type)
    wfsFeatureTable.addDoneLoadingListener(()->{
      switch (wfsFeatureTable.getGeometryType()) {
        case POINT:
          wfsFeatureLayer.setRenderer(new SimpleRenderer(new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, randomColor(), 4)));
          break;
        case POLYGON:
          wfsFeatureLayer.setRenderer(new SimpleRenderer(new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, randomColor(), null)));
          break;
        case POLYLINE:
          wfsFeatureLayer.setRenderer(new SimpleRenderer(new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, randomColor(), 2)));
          break;
      }

    });

    // add the layer to the map's operational layers
    map.getOperationalLayers().add(wfsFeatureLayer);
  }

  /**
   * Returns a random hex color code value from a preset list.
   */
  private Integer randomColor() {

    ArrayList<Integer> colorList = new ArrayList<>();
    colorList.add(0xff0000ff); // blue
    colorList.add(0xff00f5ff); // light blue
    colorList.add(0xff00ff00); // green
    colorList.add(0xfff8ff00); // yellow
    colorList.add(0xffffa600); // orange
    colorList.add(0xffff0000); // red
    colorList.add(0xffff00bd); // magenta
    colorList.add(0xff9400ff); // purple

    Random random = new Random();
    return colorList.get(random.nextInt(colorList.size()));
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