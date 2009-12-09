/*
  Redistricting application
  Copyright (C) <2009>  <Aaron Ciaghi, Stephen Long, Joshua Justice>
  
  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
  
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU General Public License along
  with this program; if not, write to the Free Software Foundation, Inc.,
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package edu.gatech.c4g.r4g.view;

import java.io.File;
import java.io.IOException;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.SLD;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.geotools.swing.JMapFrame;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Provides a graphical representation of the result of the redistricting
 * algorithm.
 * 
 * @author aaron
 * 
 */
public class MapView {

	private FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;

	public MapView(File f) {
		// Set look and feel
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} catch (UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			System.err
					.println("System Look and Feel not supported! Using Cross Platform L&F");
			try {
				UIManager.setLookAndFeel(UIManager
						.getCrossPlatformLookAndFeelClassName());
			} catch (Exception e2) {
				e2.printStackTrace();
				System.exit(1);
			}
		}

		FileDataStore store;
		try {
			store = FileDataStoreFinder.getDataStore(f);
	        FeatureSource<SimpleFeatureType,SimpleFeature> source = store.getFeatureSource();
			featureSource = source;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void showShapefile() {
		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);

		MapContext map = new DefaultMapContext();
		map.setTitle("Redistricting Algorithm Result Viewer");
		// map.addLayer(featureSource, null);

		/**
		 * Create the Style and display the shapefile
		 */
		Style style;
		try {
			style = createStyle(featureSource, "DISTRICT");
			map.addLayer(featureSource, style);

			// Now display the map
			JMapFrame.showMap(map);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// public void displayDistricts(MapContext map, Collection<District>
	// districts) {
	// // decide the colors
	// int districtCount = districts.size();
	// Color[] districtColors = new Color[districtCount];
	//
	// for (int i = 0; i < districtCount; i++) {
	// int r = (int) (Math.random() * 255);
	// int g = (int) (Math.random() * 255);
	// int b = (int) (Math.random() * 255);
	//
	// districtColors[i] = new Color(r, g, b);
	// }
	//
	// for (District d : districts) {
	// for (Block b : d.getAllBlocks()) {
	// // b.getFeature().
	// }
	// }
	//
	// // map.addLayer(arg0); TODO
	// }

	/**
	 * Create a rendering style to display features from the given feature
	 * source by matching unique values of the specified feature attribute to
	 * colours
	 * 
	 * @param featureSource
	 *            the feature source
	 * @return a new Style instance
	 */
	private Style createStyle(
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource,
			String attributeName) throws Exception {

		FilterFactory2 filterFactory = CommonFactoryFinder
				.getFilterFactory2(null);
		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);

		ColorLookUpFunction colourFn = new ColorLookUpFunction(featureSource
				.getFeatures(), filterFactory.property(attributeName));

		Stroke stroke = styleFactory.createStroke(colourFn, // function to
				// choose feature
				// colour
				filterFactory.literal(1.0f), // line width
				filterFactory.literal(1.0f)); // opacity

		Fill fill = styleFactory.createFill(colourFn, // function to choose
				// feature colour
				filterFactory.literal(1.0f)); // opacity

		Class<?> geomClass = featureSource.getSchema().getGeometryDescriptor()
				.getType().getBinding();
		Symbolizer sym = null;
		if (Polygon.class.isAssignableFrom(geomClass)
				|| MultiPolygon.class.isAssignableFrom(geomClass)) {

			sym = styleFactory.createPolygonSymbolizer(stroke, fill, null);

		} else if (LineString.class.isAssignableFrom(geomClass)
				|| MultiLineString.class.isAssignableFrom(geomClass)) {
			sym = styleFactory.createLineSymbolizer(stroke, null);

		} else {
			Graphic gr = styleFactory.createDefaultGraphic();
			gr.graphicalSymbols().clear();
			Mark mark = styleFactory.getCircleMark();
			mark.setFill(fill);
			mark.setStroke(stroke);
			gr.graphicalSymbols().add(mark);
			gr.setSize(filterFactory.literal(10.0f));
			sym = styleFactory.createPointSymbolizer(gr, null);
		}

		Style style = SLD.wrapSymbolizers(sym);
		return style;
	}

}
