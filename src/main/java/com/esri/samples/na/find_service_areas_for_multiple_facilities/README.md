# Find service areas for multiple facilities

Find the service areas of several facilities from a feature service.

![](FindServiceAreasForMultipleFacilities.png)

## Use case

A service area is a region which can be accessed from a facility as limited by one or more factors, such as travel time, distance, or cost. When analyzing the service area of multiple facilities, this workflow can be used to identify gaps in service area coverage, or significant overlaps, helping to optimize the distribution of facilities. For example, a city's health service may identify areas of a city that can be effectively accessed from particular hospitals, and with this optimize distribution of staff and resources.

## How to use the sample

Click the 'find service areas' button to determine and display the service area of each facility on the map. The polygons displayed around each facility represent the service area, with the cutoff dictated by travel time.

## How it works

1. Create a new `ServiceAreaTask` from a network service.
2. Create default `ServiceAreaParameters` from the service area task.
3. Set the parameters `ServiceAreaParameters.setReturnPolygons(true)` to return polygons of all service areas.
4. Add facilities of the `ServiceAreaParameters`. For this, use a set of `QueryParameters` to select features from an `ArcGISFeatureTable`: `serviceAreaParameters.setFacilities(facilitiesArcGISFeatureTable, queryParameters)`.
5. Get the `ServiceAreaResult` by solving the service area task using the parameters.
6. For each facility, get any `ServiceAreaPolygons` that were returned, `serviceAreaResult.getResultPolygons(facilityIndex)`.
7. Display the service area polygons as `Graphics` in a `GraphicsOverlay` on the `MapView`.

## About the data

This sample uses a street map of San Diego, in combination with a feature service with facilities (used here to represent hospitals).

## Relevant API

* ServiceAreaFacility
* ServiceAreaParameters
* ServiceAreaPolygon
* ServiceAreaResult
* ServiceAreaTask

## Tags

facilities, feature service, impedance, network analysis, service area, travel time