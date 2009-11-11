package edu.gatech.c4g.r4g.view;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.geotools.data.FeatureSource;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.swing.JMapFrame;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class MapView {

	private FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;

	public MapView(FeatureSource<SimpleFeatureType, SimpleFeature> source) {
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
		
		featureSource = source;

	}

	public void showShapefile() {
		MapContext map = new DefaultMapContext();
		map.setTitle(featureSource.getInfo().getTitle()); 
		map.addLayer(featureSource, null);

		// Now display the map
		JMapFrame.showMap(map);
	}
}
