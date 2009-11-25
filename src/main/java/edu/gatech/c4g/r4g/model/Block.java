package edu.gatech.c4g.r4g.model;

import java.util.HashSet;

import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

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
