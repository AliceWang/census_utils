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

package edu.gatech.c4g.r4g.model;

import java.util.HashSet;

import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;

import edu.gatech.c4g.r4g.Redistrict;

public class Block implements Comparable<Block> {
	public static final int UNASSIGNED = 0;
	public static final String CATEGORY_WATER = "Water";
	public static final String CATEGORY_SHIPPING = "Shipping";

	private int id;

	private int distNo = UNASSIGNED;

	private SimpleFeature feature;
	private MultiPolygon polygon;
	public HashSet<Block> neighbors;

	public Block(SimpleFeature sf) {
		feature = sf;
		polygon = (MultiPolygon) sf.getDefaultGeometry();

		// use only the integer part of the feature ID for compatibility with
		// GAL files
		String fId = sf.getID();
		id = Integer.parseInt(fId.substring(fId.lastIndexOf('.') + 1));
		neighbors = new HashSet<Block>();
	}

	public int getId() {
		return id;
	}

	public int getPopulation() {
		Property population = feature.getProperty(Redistrict.POPULATION_FIELD);
		return (int)Double.parseDouble(population.getValue().toString());
	}

	public double getArea() {
		return polygon.getArea();
	}

	public double getDensity() {
		if (getArea() > 0) {
			return getPopulation() / getArea();
		}
		return 0;
	}

	public MultiPolygon getPolygon() {
		return polygon;
	}

	public int getDistNo() {
		return distNo;
	}

	public void setDistNo(int distNo) {
		this.distNo = distNo;
	}
	
	public SimpleFeature getFeature(){
		return feature;
	}
	
	public double calculateDistance(Coordinate c){
		return c.distance(polygon.getCentroid().getCoordinate());
	}

	public int compareTo(Block other) {
		//uses population to compare
		Integer selfPop = this.getPopulation();
		Integer otherPop = other.getPopulation();
		return selfPop.compareTo(otherPop);		
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Block){
			return ((Block)obj).getId() == id;
		}
		return false;
	}

}
