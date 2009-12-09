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

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.filter.FunctionExpressionImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.PropertyName;

/**
 * A function to dynamically allocate colours to features. It works with a
 * lookup table where the key is a user-specified feature attribute. Colours are
 * generated using a simple colour ramp algorithm.
 */
public class ColorLookUpFunction extends FunctionExpressionImpl {

	private static final float INITIAL_HUE = 0.1f;
	private final FeatureCollection<SimpleFeatureType, SimpleFeature> collection;

	Map<Object, Color> lookup;
	private int numColours;

	private float hue;
	private float hueIncr;
	private float saturation = 0.7f;
	private float brightness = 0.7f;

	/**
	 * Creates an instance of the function for the given feature collection.
	 * Features will be assigned fill colours by matching the value of the
	 * specified feature attribute in a lookup table of unique attribute values
	 * with associated colours.
	 * 
	 * @param collection
	 *            the feature collection
	 * 
	 * @param colourAttribute
	 *            a literal expression that specifies the feature attribute to
	 *            use for colour lookup
	 */
	public ColorLookUpFunction(FeatureCollection<SimpleFeatureType, SimpleFeature> collection,
			PropertyName colourAttribute) {
		super("UniqueColour");
		this.collection = collection;

		this.params.add(colourAttribute);
		this.fallback = CommonFactoryFinder.getFilterFactory2(null).literal(
				Color.WHITE);
	}

	@Override
	public int getArgCount() {
		return 1;
	}

	/**
	 * Evalute this function for a given feature and return a Color.
	 * 
	 * @param object
	 *            the feature for which a colour is being requested
	 * 
	 * @return the colour for this feature
	 */
	@Override
	public Object evaluate(Object feature) {
		if (lookup == null) {
			createLookup();
		}

		Object key = ((PropertyName) params.get(0)).evaluate(feature);
		Color color = lookup.get(key);
		if (color == null) {
			color = addColor(key);
		}

		return color;
	}

	/**
	 * Creates the lookup table and initializes variables used in colour
	 * generation
	 */
	private void createLookup() {
		lookup = new HashMap<Object, Color>();
		try {
			UniqueVisitor visitor = new UniqueVisitor((PropertyName) params
					.get(0));
			collection.accepts(visitor, null);
			numColours = visitor.getUnique().size();
			hue = INITIAL_HUE;
			hueIncr = (1.0f - hue) / numColours;

		} catch (Exception ex) {
			throw new IllegalStateException("Problem creating colour lookup",
					ex);
		}
	}

	/*
	 * Generates a new colour for the colour ramp and adds it to the lookup
	 * table
	 */
	private Color addColor(Object key) {
		Color c = new Color(Color.HSBtoRGB(hue, saturation, brightness));
		hue += hueIncr;
		lookup.put(key, c);
		return c;
	}
}
