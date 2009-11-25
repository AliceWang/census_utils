package edu.gatech.c4g.r4g.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Copyright (C) 2009
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Joshua Justice
 * 
 * 
 */
public class District extends Graph {
	private int districtNo;

	public District(int districtNo) {
		blocks = new Hashtable<Integer, Block>();
		this.districtNo = districtNo;
	}

	public int getDistrictNo() {
		return districtNo;
	}

	public void addBlock(Block b) {
		if (!blocks.containsKey(b.getId())) {
			super.addBlock(b);
			b.setDistNo(districtNo);
		}
	}

	public Hashtable<Integer, Block> getBorderingBlocks() {
		Hashtable<Integer, Block> neighbors = new Hashtable<Integer, Block>();
		for (Block b : blocks.values()) {
			Iterator<Block> i = b.neighbors.iterator();
			while (i.hasNext()) {
				Block current = i.next();
				if (current.getDistNo() != b.getDistNo()) {
					neighbors.put(current.getDistNo(), current);
				}
			}
		}
		return neighbors;
	}
	
	public Hashtable<Integer, Block> getBorderingBlocks(int DistNo){
		Hashtable<Integer,Block> neighbors = new Hashtable<Integer, Block>();
		for (Block b:blocks.values()){
			Iterator<Block> i = b.neighbors.iterator();
			while(i.hasNext()) {
				Block a = i.next();
				if (a.getDistNo() == DistNo){
					neighbors.put(a.getDistNo(), a);
				}
			}
		}
		return neighbors;
	}

	public ArrayList<Block> getUnassigned() {
		ArrayList<Block> unassigned = new ArrayList<Block>();

		for (Block b : blocks.values()) {

			if (b.getDistNo() == Block.UNASSIGNED)
				unassigned.add(b);
		}

		Collections.sort(unassigned);
		return unassigned;
	}

	public ArrayList<Integer> getNeighboringDistricts() {
		ArrayList<Integer> neighbors = new ArrayList<Integer>();
		Hashtable<Integer, Block> neighborBlks = new Hashtable<Integer, Block>();
		for (Block b : blocks.values()) {
			Iterator<Block> i = b.neighbors.iterator();
			while (i.hasNext()) {
				Block a = i.next();
				if (a.getDistNo() != b.getDistNo()) {
					neighborBlks.put(a.getDistNo(), a);
				}
			}
		}
		Enumeration<Integer> DistNo = neighborBlks.keys();
		while (DistNo.hasMoreElements()) {
			neighbors.add(DistNo.nextElement());
		}
		return neighbors;
	}

	public void removeBlock(Block b) {
		super.removeBlock(b);
		b.setDistNo(Block.UNASSIGNED);
	}

	public boolean isInRange(double min, double max) {
		return (population > min) && (population <= max);
	}

	/**
	 * Returns the inverse of difference of the convex hull of this district
	 * with its area. This value is a metric for compactness.
	 * WARNING! This function is very expensive.
	 * 
	 * @return
	 */
	public double getCompactness() {
		Geometry distPoly = null;

		for (Block b : blocks.values()) {
			if (distPoly == null) {
				distPoly = b.getPolygon();
			} else {
				distPoly = distPoly.union(b.getPolygon());
			}
		}

		Geometry convexHull = distPoly.convexHull();
		
		//System.out.println("Compactness of " + districtNo + ": " + (1/(convexHull.getArea() - distPoly.getArea())));

		return 1 / (convexHull.getArea() - distPoly.getArea());
	}

}
