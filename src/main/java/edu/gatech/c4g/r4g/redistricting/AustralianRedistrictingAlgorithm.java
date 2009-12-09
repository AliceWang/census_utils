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

package edu.gatech.c4g.r4g.redistricting;

import org.geotools.data.FeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import edu.gatech.c4g.r4g.util.AustralianLoader;

/**
 * Class that implements the redistricting algorithm for Australia. Currently it
 * uses the generic algorithm after loading the input shapefile with rules
 * specific to Australia.
 * 
 * @author aaron
 * 
 */
public class AustralianRedistrictingAlgorithm extends RedistrictingAlgorithm {

	public AustralianRedistrictingAlgorithm(AustralianLoader loader,
			FeatureSource<SimpleFeatureType, SimpleFeature> source,
			String galFile) {
		super(loader, source, galFile);
	}

}
