package edu.gatech.c4g.r4g.redistricting;

import org.geotools.data.FeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import edu.gatech.c4g.r4g.util.AmericanLoader;

/**
 * Class that implements the redistricting algorithm for the US. Currently it
 * simply uses the generic algorithm.
 * 
 * @author aaron
 * 
 */
public class AmericanRedistrictingAlgorithm extends RedistrictingAlgorithm {

	public AmericanRedistrictingAlgorithm(AmericanLoader loader,
			FeatureSource<SimpleFeatureType, SimpleFeature> source,
			String galFile) {
		super(loader, source, galFile);
	}

}
