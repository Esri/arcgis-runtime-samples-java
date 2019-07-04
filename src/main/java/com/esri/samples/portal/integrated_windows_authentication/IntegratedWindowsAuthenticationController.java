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

package com.esri.samples.portal.integrated_windows_authentication;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.util.Pair;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.DrawStatus;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.portal.PortalQueryParameters;
import com.esri.arcgisruntime.portal.PortalQueryResultSet;
import com.esri.arcgisruntime.security.AuthenticationManager;

public class IntegratedWindowsAuthenticationController {

  @FXML
  private MapView mapView;
  @FXML
  private ListView<Pair<String, String>> resultsListView;
  @FXML
  private TextField portalUrlTextField;
  @FXML
  private ProgressIndicator progressIndicator;
  @FXML
  private Text loadStateTextView;
  @FXML
  private Text loadWebMapTextView;

  private Portal iwaSecuredPortal;
  private Portal publicPortal;
  private boolean usingPublicPortal;

  public void initialize() {
    try {

      // create a streets base map
      ArcGISMap map = new ArcGISMap(Basemap.createStreets());

      // set the map to be displayed in the map view
      mapView.setMap(map);

      // set authentication challenge handler
      AuthenticationManager.setAuthenticationChallengeHandler(new IWAChallengeHandler());

      // keep hold of the public portal the will be searched, to allow retrieving portal items later
      publicPortal = new Portal("http://www.arcgis.com");

      // add a listener to the map results list view that loads the map on selection
      resultsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
        if (resultsListView.getSelectionModel().getSelectedItem() != null) {
          // show progress indicator while map is drawing
          progressIndicator.setVisible(true);

          // get the portal item ID from the selected list view item
          String selectedItemId = resultsListView.getSelectionModel().getSelectedItem().getKey();

          // determine if using the public or secured portal; get the appropriate object reference
          Portal portal;
          if (usingPublicPortal) {
            portal = publicPortal;
          } else {
            portal = iwaSecuredPortal;
          }

          // use the item ID to create a PortalItem from the portal
          PortalItem portalItem = new PortalItem(portal, selectedItemId);

          if (portalItem != null) {
            // create a Map using the web map (portal item)
            ArcGISMap webMap = new ArcGISMap(portalItem);
            // set the map to the map view
            mapView.setMap(webMap);
          }

          loadWebMapTextView.setText("Loaded web map from item " + selectedItemId);
        }
      });

      // make the list view show a preview of the portal items' map area
      resultsListView.setCellFactory(c -> new PortalItemInfoListCell());

      // hide the progress indicator when the map is finished drawing
      mapView.addDrawStatusChangedListener(drawStatusChangedEvent -> {
        if (drawStatusChangedEvent.getDrawStatus() == DrawStatus.COMPLETED) {
          progressIndicator.setVisible(false);
        }
      });

    } catch (Exception e) {
      // on any error, display the stack trace.
      e.printStackTrace();
    }
  }

  /**
   * Handles searching a public portal.
   */
  @FXML
  private void handleSearchPublicPress() {
    // set a variable indicating that the portal is a public portal, to allow retrieving portal items later
    usingPublicPortal = true;
    // search the public portal
    searchPortal(publicPortal);
  }

  /**
   * Handles searching a secure portal.
   */
  @FXML
  private void handleSearchSecurePress() {
    // check for a url in the URL field
    if (!portalUrlTextField.getText().isEmpty()) {
      // keep hold of the portal we are searching and set a variable indicating that this is a secure portal, to allow retrieving portal items later
      iwaSecuredPortal = new Portal(portalUrlTextField.getText(), true);
      usingPublicPortal = false;
      // search the IWA-secured portal, the user may be challenged for access
      searchPortal(iwaSecuredPortal);
    } else {
      new Alert(Alert.AlertType.ERROR, "Portal URL is empty. Please enter a portal URL.").show();
    }
  }

  /**
   * Search the given portal for its portal items and display their titles in a list view.
   *
   * @param portal to search
   */
  private void searchPortal(Portal portal) {

    // check if the portal is null
    if (portal == null) {
      new Alert(Alert.AlertType.ERROR, "No portal provided").show();
      return;
    }

    // clear any existing items in the list view
    resultsListView.getItems().clear();

    // clear the information about the previously loaded map
    loadWebMapTextView.setText("");

    // show portal load state
    progressIndicator.setVisible(true);
    loadStateTextView.setText("Searching for web map items on the portal at " + portal.getUri());

    // load the portal items
    portal.loadAsync();
    portal.addDoneLoadingListener(() -> {
      if (portal.getLoadStatus() == LoadStatus.LOADED) {
        try {
          // update load state in UI with the portal URI
          loadStateTextView.setText("Connected to the portal on " + new URI(portal.getUri()).getHost());
        } catch (URISyntaxException e) {
          new Alert(Alert.AlertType.ERROR, "Error getting URI from portal: " + e.getMessage()).show();
        }

        // report the user name used for this connection
        if (portal.getUser() != null) {
          loadStateTextView.setText("Connected as: " + portal.getUser().getUsername());
        } else {
          // if connecting to an unsecured portal, no user credentials are needed to authenticate access
          loadStateTextView.setText("Connected as: Anonymous");
        }

        // search the portal for web maps
        ListenableFuture<PortalQueryResultSet<PortalItem>> portalItemResultFuture = portal.findItemsAsync(new PortalQueryParameters("type:(\"web map\" NOT \"web mapping application\")"));
        portalItemResultFuture.addDoneListener(() -> {
          try {
            // get the result
            PortalQueryResultSet<PortalItem> portalItemSet = portalItemResultFuture.get();
            List<PortalItem> portalItems = portalItemSet.getResults();
            // add the IDs and titles of the portal items to the list view
            portalItems.forEach(portalItem -> resultsListView.getItems().add(new Pair<>(portalItem.getItemId(), portalItem.getTitle())));

          } catch (ExecutionException | InterruptedException e) {
            new Alert(Alert.AlertType.ERROR, "Error getting portal item set from portal: " + e.getMessage()).show();
          }
          // hide the progress indicator
          progressIndicator.setVisible(false);
        });

      } else {
        // hide the progress indicator and reset the load state text
        progressIndicator.setVisible(false);
        loadStateTextView.setText("");
        // report error
        new Alert(Alert.AlertType.ERROR, "Portal sign in failed: " + portal.getLoadError().getCause().getMessage()).show();
      }
    });
  }

  /**
   * Stops and releases all resources used in the application.
   */
  void terminate() {

    if (mapView != null) {
      mapView.dispose();
    }
  }

  /**
   * Shows the title of the portal items in the selection list view.
   */
  class PortalItemInfoListCell extends ListCell<Pair<String, String>> {

    @Override
    protected void updateItem(Pair<String, String> portalItemInfo, boolean empty) {
      super.updateItem(portalItemInfo, empty);
      if (portalItemInfo != null) {
        // set the list cell's text to the map's index
        setText(portalItemInfo.getValue());
      } else {
        setText(null);
      }
    }
  }
}
