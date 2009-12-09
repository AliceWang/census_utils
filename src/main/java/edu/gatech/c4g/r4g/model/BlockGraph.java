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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Main data structure that contains all the blocks in a shapefile. It also
 * contain a reference to all the districts built from its blocks.
 * 
 * @author aaron
 * 
 */
public class BlockGraph extends Graph {
	/**
	 * Districts build from blocks in this graph.
	 */
	private Hashtable<Integer, District> districts;

	/**
	 * Builds a graph from an input {@link FeatureSource} (usually a shapefile).
	 * 
	 * @param source
	 */
	public BlockGraph(FeatureSource<SimpleFeatureType, SimpleFeature> source) {
		this.blocks = new Hashtable<Integer, Block>();
		this.districts = new Hashtable<Integer, District>();

		try {
			FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source
					.getFeatures();

			Iterator<SimpleFeature> iterator = collection.iterator();
			try {
				for (Iterator<SimpleFeature> i = collection.iterator(); i
						.hasNext();) {
					SimpleFeature feature = i.next();

					Block b = new Block(feature);
					addBlock(b);
				}
			} finally {
				collection.close(iterator);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Removes a block from this graph and removes all the edges that contain
	 * the block.
	 */
	public void removeBlock(Block b) {
		for (Block neighbor : b.neighbors) {
			neighbor.neighbors.remove(b);
		}

		blocks.remove(b.getId());
	}

	/**
	 * Adds a new district to the districts hashtable {@link #districts}
	 * 
	 * @param d
	 */
	public void addDistrict(District d) {
		districts.put(d.getDistrictNo(), d);
	}

	/**
	 * Returns the number of districts.
	 * 
	 * @return
	 */
	public int getDistrictCount() {
		return districts.size();
	}

	/**
	 * Returns a {@link Collection} containing all the districts.
	 * 
	 * @return
	 */
	public Collection<District> getAllDistricts() {
		return districts.values();
	}

	/**
	 * Returns the district with the input district number.
	 * 
	 * @param distNo
	 * @return
	 */
	public District getDistrict(int distNo) {
		if (distNo == Block.UNASSIGNED) {
			return null;
		}
		return districts.get(new Integer(distNo));
	}

	/**
	 * Returns a list containing all the islands in the graph sorted by number
	 * of blocks.
	 */
	public ArrayList<Island> toIslands() {
		ArrayList<Block> allBlocks = new ArrayList<Block>(blocks.values());
		ArrayList<Island> islands = new ArrayList<Island>();

		while (!allBlocks.isEmpty()) {
			int start = (int) (Math.random() * Integer.MAX_VALUE)
					% allBlocks.size();

			Block firstBlock = allBlocks.get(start);
			HashSet<Block> islandBlocks = new HashSet<Block>();
			addToIsland(islandBlocks, firstBlock);
			Island island = new Island(islandBlocks);

			islands.add(island);

			allBlocks.removeAll(islandBlocks);
		}

		Collections.sort(islands, new Comparator<Island>() {

			public int compare(Island o1, Island o2) {
				if (o1.getAllBlocks().size() > o2.getAllBlocks().size()) {
					return -1;
				} else if (o1.getAllBlocks().size() < o2.getAllBlocks().size()) {
					return 1;
				}
				return 0;
			}

		});

		if (islands.size() > 1) {
			Island mainland = islands.get(0);
			System.out.println("\tFinding coastline");
			HashSet<Block> coastLine = mainland.findBoundaryBlocks();
			System.out.println("\tLinking islands with mainland");
			for (Island i : islands) {
				if (i != mainland) {
					linkIsland(i, coastLine);
				}
			}
		}
		return islands;
	}

	/**
	 * Recursively adds to an {@link Island} a block and all the blocks
	 * contained in the connected subgraph that contains the input block.
	 * 
	 * @param island
	 * @param b
	 */
	private void addToIsland(HashSet<Block> island, Block b) {
		if (!island.contains(b) && b.getDistNo() == Block.UNASSIGNED) {
			island.add(b);
			for (Block bl : b.neighbors) {
				if (!island.contains(bl)) {
					addToIsland(island, bl);
				}
			}
		}
	}

	/**
	 * Links an island to its geographically closest block on the coastline.
	 * 
	 * @param island
	 *            the island to connect
	 * @param coastLine
	 *            a {@link HashSet} containing the blocks on the coast of the
	 *            area to redistrict that has been previously computed
	 *            {@link Island#findBoundaryBlocks()}.
	 */
	private void linkIsland(Island island, HashSet<Block> coastLine) {
		Coordinate islandCenter = island.getCenter();
		Block islandRepresentative = island.getRepresentative();
		Block coastBlock = null;

		double dist = Double.MAX_VALUE;
		for (Block b : coastLine) {
			double newDist = b.calculateDistance(islandCenter);
			if (newDist < dist) {
				dist = newDist;
				coastBlock = b;
			}
		}

		if (coastBlock != null) {
			coastBlock.neighbors.add(islandRepresentative);
			islandRepresentative.neighbors.add(coastBlock);
		}
	}

	/**
	 * Returns some useful stats about the districts.
	 * 
	 * @return
	 */
	public String districtStatistics() {
		int usedblocks = 0;
		String stat = "";

		for (District d : districts.values()) {
			double totArea = 0;

			for (Block b : d.getAllBlocks()) {
				totArea += b.getArea();
			}

			stat += "District "
					+ d.getDistrictNo()
					+ ": population "
					+ d.getPopulation()
					+ "("
					+ ((double) d.getPopulation() / (double) this
							.getPopulation()) * 100 + "%) ("
					+ d.getAllBlocks().size() + " blocks, area= " + totArea
					+ ")\n";
			usedblocks += d.getAllBlocks().size();
		}

		stat += "Unassigned blocks: " + (blocks.size() - usedblocks);

		return stat;
	}

}
