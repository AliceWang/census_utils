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
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Class that represents an island in the shapefile.
 * 
 * @author aaron
 * 
 */
public class Island extends Graph {

	public Island(Collection<Block> blocks) {
		this.blocks = new Hashtable<Integer, Block>();
		addAllBlocks(blocks);
	}

	/**
	 * Returns the centroid of this island by taking the average of the
	 * centroids of all the blocks in the island.
	 * 
	 * @return
	 */
	public Coordinate getCenter() {
		double x = 0;
		double y = 0;

		for (Block b : blocks.values()) {
			x += b.getPolygon().getCentroid().getX();
			y += b.getPolygon().getCentroid().getY();
		}

		x = x / blocks.size();
		y = y / blocks.size();

		return new Coordinate(x, y);
	}

	/**
	 * Returns a list containing the blocks that are located on the boundary of
	 * this island
	 * 
	 * @return the list of blocks located on the boundary
	 */
	public HashSet<Block> findBoundaryBlocks() {
		HashSet<Block> boundary = new HashSet<Block>();

		for (Block b : blocks.values()) {
			Geometry bBoundary = b.getPolygon().getBoundary();

			int count = bBoundary.getNumPoints();

			for (Block n : b.neighbors) {
				bBoundary = bBoundary.difference(n.getPolygon().getBoundary());
			}

			count = bBoundary.getNumPoints();
			if (count > 0) {
				boundary.add(b);
			}
		}

		return boundary;
	}

	/**
	 * Returns the first block of this island to be used as a representative of
	 * the island to link it to the mainland.
	 * 
	 * @return
	 */
	public Block getRepresentative() {
		ArrayList<Block> allBlocks = new ArrayList<Block>(blocks.values());
		return allBlocks.get(0);
	}
}
