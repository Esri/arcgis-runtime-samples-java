package com.esri.samples.group_layers;

import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TreeCell;

import com.esri.arcgisruntime.layers.Layer;

/**
 * A custom tree cell for displaying a layer in a tree view. Includes a check box for toggling the layer's visibility.
 */
public class LayerTreeCell extends TreeCell<Layer> {

  @Override
  public void updateItem(Layer layer, boolean empty) {
    super.updateItem(layer, empty);
    if (!empty) {

      // use the layer's name for the text
      setText(layer.getName());

      if (layer.getName().equals("DevA_BuildingShells") || layer.getName().equals("DevB_BuildingShells")) {

        // if the layer is a building shell create a radio button and assign to the toggle group
        RadioButton radioButton = new RadioButton();
        setGraphic(radioButton);
        radioButton.setToggleGroup(GroupLayersSample.buildingsToggleGroup);

        // toggle the layer's visibility when the radio button is selected
        radioButton.setSelected(layer.isVisible());
        radioButton.selectedProperty().addListener(e -> layer.setVisible(radioButton.isSelected()));
      } else {

        // if the layer is not a building shell, create a checkbox
        CheckBox checkBox = new CheckBox();
        setGraphic(checkBox);

        // toggle the layer's visibility when the check box is toggled
        checkBox.setSelected(layer.isVisible());
        checkBox.selectedProperty().addListener(e -> layer.setVisible(checkBox.isSelected()));
      }
    } else {
      setText(null);
      setGraphic(null);
    }
  }
}
