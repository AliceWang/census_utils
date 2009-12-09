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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;

import org.geotools.data.FeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import edu.gatech.c4g.r4g.model.Block;
import edu.gatech.c4g.r4g.model.BlockGraph;
import edu.gatech.c4g.r4g.model.District;
import edu.gatech.c4g.r4g.model.Island;
import edu.gatech.c4g.r4g.util.Loader;

/**
 * Generic redistricting algorithm. This is the class that does the actual job
 * of redistricting. It is abstract, thus it must be extended in order to add
 * country/state-specific rules.
 * 
 * @author aaron
 * 
 */
public abstract class RedistrictingAlgorithm {

	/**
	 * Number of desired districts
	 */
	int ndis;
	/**
	 * Ideal population that a district should have
	 */
	double idealPopulation;
	/**
	 * Minimum allowed population for a district
	 */
	double minPopulation;
	/**
	 * Maximum allowed population for a district
	 */
	double maxPopulation;

	/**
	 * {@link BlockGraph} containing all the blocks in the shapefile
	 */
	BlockGraph bg;
	/**
	 * {@link Island}s in the shapefile (including the mainland).
	 */
	ArrayList<Island> islands;
	Loader loader;

	public RedistrictingAlgorithm(Loader loader,
			FeatureSource<SimpleFeatureType, SimpleFeature> source,
			String galFile) {
		this.loader = loader;

		System.out.println("Loading files");
		bg = loader.load(source, galFile);

		// islands = new ArrayList<Island>();
		System.out.println("Finding islands");
		islands = bg.toIslands();
		System.out.println("Found " + islands.size() + " islands");

	}

	/**
	 * This function calls executes the 3 stages of the redistricting algorithm
	 * sequentially.
	 * 
	 * @see #initialExpansion()
	 * @see #secondaryExpansion()
	 * @see #populationBalancing()
	 * 
	 * @param ndis
	 *            desired number of districts
	 * @param maxDeviation
	 *            max allowed deviation from the ideal population
	 */
	public void redistrict(int ndis, double maxDeviation) {
		// calculate ideal population
		this.ndis = ndis;
		idealPopulation = bg.getPopulation() / ndis;
		minPopulation = idealPopulation - idealPopulation * maxDeviation;
		maxPopulation = idealPopulation + idealPopulation * maxDeviation;

		// stage 1
		initialExpansion();

		// --------------------------------------
		// stage2
		secondaryExpansion();

		// LOG INFO
		System.out.println("\n=============\n" + "After Stage 2\n"
				+ "=============\n");
		System.out.println(bg.districtStatistics());

		// --------------------------------------
		// stage3
		// populationBalancing();
	}

	/**
	 * First stage of the algorithm. Each district is grown from the most
	 * densely populated block available up to 80% the minimum allowed size for
	 * a district. This is supposed to leave some space for the secondary stage
	 * and limit district choking.
	 */
	protected void initialExpansion() {
		ArrayList<Block> allBlocks = new ArrayList<Block>();
		allBlocks.addAll(bg.getAllBlocks());
		// sort the blocks by density
		Collections.sort(allBlocks, new BlockDensityComparator());

		for (int currentDistNo = 1; currentDistNo <= ndis; currentDistNo++) {
			System.out.println("Building district " + currentDistNo);

			Block firstBlock = findFirstUnassignedBlock(allBlocks);

			District dist = new District(currentDistNo);
			// add the most populated block
			ArrayList<Block> expandFrom = new ArrayList<Block>();
			dist.addBlock(firstBlock);
			expandFrom.add(firstBlock);

			while (!expandFrom.isEmpty()
					&& dist.getPopulation() <= minPopulation * .8) {

				ArrayList<Block> neighborsList = new ArrayList<Block>();

				for (Block b : expandFrom) {
					for (Block n : b.neighbors) {
						if (n.getDistNo() == Block.UNASSIGNED) {
							if (!neighborsList.contains(n)) {
								neighborsList.add(n);
							}
						}
					}
				}

				ArrayList<Block> blocksToAdd = chooseNeighborsToAdd(dist
						.getPopulation(), minPopulation, neighborsList);
				dist.addAllBlocks(blocksToAdd);
				expandFrom = blocksToAdd;
			}

			bg.addDistrict(dist);
		}

	}

	/**
	 * Second stage of the algorithm. Adds to each district all the closest
	 * unassigned blocks until the ideal population is reached.
	 */
	protected void secondaryExpansion() {
		ArrayList<Block> unassigned = bg.getUnassigned();

		// Argument: a SortedSet of unassigned blocks
		boolean ignorePopulation = false;
		for (int i = 0; i < 2; i++) {
			int oldsize = 0;
			int newsize = unassigned.size();

			while (oldsize != newsize) {
				for (Block current : unassigned) {
					int district = Block.UNASSIGNED;

					if (current.neighbors.isEmpty()) {
						System.out.println("Block " + current.getId()
								+ " has no neighbors!!");
					}

					for (Block b : current.neighbors) {
						if (b.getDistNo() != Block.UNASSIGNED) {
							District d = bg.getDistrict(b.getDistNo());
							int pop = d.getPopulation();
							if (pop <= idealPopulation || ignorePopulation) {
								if (district == Block.UNASSIGNED) {
									district = b.getDistNo();
								} else {
									District currentD = bg
											.getDistrict(district);
									int newPop = currentD.getPopulation();
									district = pop < newPop ? b.getDistNo()
											: district;
								}
							}
						}
					}

					if (district != Block.UNASSIGNED) {
						bg.getDistrict(district).addBlock(current);

						// System.out.println(unassigned.size());
					}
				}
				oldsize = newsize;
				unassigned = bg.getUnassigned();
				newsize = unassigned.size();
			}
			ignorePopulation = true;
		}

	}

	/**
	 * Third stage of the algorithm. Balances the population of the districts
	 * where necessary.
	 * 
	 * @see #finalizeDistricts()
	 */
	protected void populationBalancing() {
		// TODO
		finalizeDistricts();
	}

	/**
	 * First part of stage 3.
	 */
	protected void finalizeDistricts() {
		// Get the under-apportioned Districts
		ArrayList<District> undAppDists = new ArrayList<District>();
		for (District d : bg.getAllDistricts()) {
			if (d.getPopulation() < minPopulation) {
				undAppDists.add(d);
			}
		}
		// Get their neighboring districts
		for (District d : undAppDists) {
			ArrayList<Integer> n = d.getNeighboringDistricts();
			ArrayList<District> nDists = new ArrayList<District>();
			for (District t : bg.getAllDistricts()) {
				for (int i : n) {
					if (t.getDistrictNo() == n.get(i)) {
						nDists.add(t);
					}
				}
			}
			// find out which neighboring districts have too many people and get
			// the bordering blocks from those districts one at a time.
			for (int i = 0; i < nDists.size(); i++) {
				while ((d.getPopulation() < minPopulation)
						&& (nDists.get(i).getPopulation() > maxPopulation)) {
					District nDist = nDists.get(i);
					// The bordering blocks to be moved over.
					Hashtable<Integer, Block> bBlocks = d
							.getBorderingBlocks(nDist.getDistrictNo());
					while ((d.getPopulation() < minPopulation)
							&& (nDists.get(i).getPopulation() > maxPopulation)
							&& (!bBlocks.isEmpty())) {
						// get Enumeration and remove value based off of the
						// enumerated values
						Block b;
						Enumeration<Integer> enume = bBlocks.keys();
						if (enume.hasMoreElements()) {
							b = bBlocks.remove(enume.nextElement());
							bg.getDistrict(b.getDistNo()).removeBlock(b);
							b.setDistNo(d.getDistrictNo());
							d.addBlock(b);
						}
					}
				}

			}
		}
		undAppDists = new ArrayList<District>();
		for (District d : bg.getAllDistricts()) {
			if (d.getPopulation() < minPopulation) {
				undAppDists.add(d);
			}
		}
		// Get their neighboring districts
		for (District d : undAppDists) {
			ArrayList<Integer> n = d.getNeighboringDistricts();
			ArrayList<District> nDists = new ArrayList<District>();
			for (District t : bg.getAllDistricts()) {
				for (int i : n) {
					if (t.getDistrictNo() == n.get(i)) {
						nDists.add(t);
					}
				}
			}
			// find out which neighboring districts have too many people and get
			// the bordering blocks from those districts one at a time.
			for (int i = 0; i < nDists.size(); i++) {
				while ((d.getPopulation() < minPopulation)
						&& (nDists.get(i).getPopulation() > idealPopulation)) {
					District nDist = nDists.get(i);
					// The bordering blocks to be moved over.
					Hashtable<Integer, Block> bBlocks = d
							.getBorderingBlocks(nDist.getDistrictNo());
					while ((d.getPopulation() < minPopulation)
							&& (nDists.get(i).getPopulation() > idealPopulation)
							&& (!bBlocks.isEmpty())) {
						// get Enumeration and remove value based off of the
						// enumerated values
						Block b;
						Enumeration<Integer> enume = bBlocks.keys();
						if (enume.hasMoreElements()) {
							b = bBlocks.remove(enume.nextElement());
							bg.getDistrict(b.getDistNo()).removeBlock(b);
							b.setDistNo(d.getDistrictNo());
							d.addBlock(b);
						}
					}
				}

			}
		}
	}

	/**
	 * Returns the first unassigned block. The block list should be ordered by
	 * density using the {@link BlockDensityComparator}.
	 * 
	 * @return
	 */
	private Block findFirstUnassignedBlock(ArrayList<Block> list) {
		for (Block b : list) {
			if (b.getDistNo() == Block.UNASSIGNED) {
				int countAssigned = 0;
				for (Block n : b.neighbors) {
					if (n.getDistNo() != Block.UNASSIGNED) {
						countAssigned++;
					}
				}

				if (countAssigned < b.neighbors.size()) {
					return b;
				}
			}
		}

		return null;
	}

	/**
	 * Function called by the {@link #initialExpansion()} to choose which blocks
	 * have to be added to a district.
	 * 
	 * @param basePop
	 * @param upperBound
	 * @param blocks
	 * @return
	 */
	protected ArrayList<Block> chooseNeighborsToAdd(int basePop,
			double upperBound, ArrayList<Block> blocks) {

		ArrayList<Block> returnList = new ArrayList<Block>();
		int[] population = new int[blocks.size()];
		int totalPop = basePop;

		// populate the population array
		for (int n = 0; n < blocks.size(); n++) {
			population[n] = blocks.get(n).getPopulation();
			totalPop += blocks.get(n).getPopulation();
		}

		if (totalPop <= upperBound) {
			// add all blocks
			return blocks;
		} else {
			Collections.sort(blocks);
			int position = blocks.size() - 1;
			totalPop = basePop;
			totalPop += blocks.get(position).getPopulation();

			while (totalPop <= upperBound) {
				returnList.add(blocks.get(position));
				position--;
				totalPop += blocks.get(position).getPopulation();
			}

			return returnList;
		}
	}

	public BlockGraph getBlockGraph() {
		return bg;
	}

	protected class BlockDensityComparator implements Comparator<Block> {

		public int compare(Block o1, Block o2) {
			if (o1.getDensity() > o2.getDensity()) {
				return -1;
			} else if (o1.getDensity() < o2.getDensity()) {
				return 1;
			}
			return 0;
		}

	}

}
