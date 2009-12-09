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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Graph of blocks that represents a district.
 * 
 * @author aaron
 * 
 */
public class District extends Graph {
	/**
	 * District identifier
	 */
	private int districtNo;

	public District(int districtNo) {
		blocks = new Hashtable<Integer, Block>();
		this.districtNo = districtNo;
	}

	/**
	 * Returns this district's identifier
	 * 
	 * @return
	 */
	public int getDistrictNo() {
		return districtNo;
	}

	public void addBlock(Block b) {
		if (!blocks.containsKey(b.getId())) {
			super.addBlock(b);
			b.setDistNo(districtNo);
		}
	}

	/**
	 * Finds all the blocks on the border of this district, namely all the
	 * blocks that have one or more neighbors in another district.
	 * 
	 * @return
	 */
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

	/**
	 * Returns all the blocks bordering with the district with the input
	 * district identifier.
	 * 
	 * @param DistNo
	 * @return
	 */
	public Hashtable<Integer, Block> getBorderingBlocks(int DistNo) {
		Hashtable<Integer, Block> neighbors = new Hashtable<Integer, Block>();
		for (Block b : blocks.values()) {
			Iterator<Block> i = b.neighbors.iterator();
			while (i.hasNext()) {
				Block a = i.next();
				if (a.getDistNo() == DistNo) {
					neighbors.put(a.getDistNo(), a);
				}
			}
		}
		return neighbors;
	}

	/**
	 * Returns an {@link ArrayList} containing the district identifiers of the
	 * neighboring districts.
	 * 
	 * @return
	 */
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

	/**
	 * Removes the input block from this district.
	 */
	public void removeBlock(Block b) {
		super.removeBlock(b);
		b.setDistNo(Block.UNASSIGNED);
	}

	/**
	 * Checks if this district's population is within the required range.
	 * 
	 * @param min
	 *            minimum allowed population
	 * @param max
	 *            maximum allowed population
	 * @return
	 */
	public boolean isInRange(double min, double max) {
		return (population > min) && (population <= max);
	}

	/**
	 * Calculates a compactness score for this district. The score is the
	 * percentage of the convex hull of this district covered by the actual
	 * district. The higher the better. WARNING! This function is very
	 * expensive!
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

		// System.out.println("Compactness of " + districtNo + ": " +
		// (1/(convexHull.getArea() - distPoly.getArea())));

		return distPoly.getArea() / convexHull.getArea();
	}

}
