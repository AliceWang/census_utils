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
import java.util.Collections;
import java.util.Hashtable;

/**
 * Abstract graph of {@link Block}s that provides common functionalities.
 * 
 * @author aaron
 * 
 */
public abstract class Graph {

	protected int population = 0;
	protected Hashtable<Integer, Block> blocks;

	public Graph() {
		blocks = new Hashtable<Integer, Block>();
	}

	/**
	 * Adds a block to this graph and updates the population field
	 * 
	 * @param b
	 *            the block to add
	 */
	public void addBlock(Block b) {
		blocks.put(b.getId(), b);
		population += b.getPopulation();
	}

	/**
	 * Default function to remove a block. It does not remove all the
	 * connections to the removed block and it must be overridden in order to
	 * have such functionality. The population field is updated upon removal of
	 * the block.
	 * 
	 * @param b
	 *            the block to remove
	 */
	public void removeBlock(Block b) {
		blocks.remove(b.getId());
		population -= b.getPopulation();
	}

	/**
	 * Returns the block with the given Id
	 * 
	 * @param index
	 * @return
	 */
	public Block getBlock(int index) {
		return blocks.get(index);
	}

	/**
	 * Returns a {@link Collection} containing all the blocks of this graph.
	 * 
	 * @return
	 */
	public Collection<Block> getAllBlocks() {
		return blocks.values();
	}

	/**
	 * Adds all the blocks of the input {@link Collection}.
	 * 
	 * @see #addBlock(Block)
	 * 
	 * @param c
	 */
	public void addAllBlocks(Collection<Block> c) {
		for (Block b : c) {
			addBlock(b);
		}
	}

	/**
	 * Removes all the blocks in this graph that are also contained in the input
	 * {@link Collection}.
	 * 
	 * @see #removeBlock(Block)
	 * 
	 * @param c
	 */
	public void removeAllBlocks(Collection<Block> c) {
		for (Block b : c) {
			removeBlock(b);
		}
	}

	/**
	 * Returns the population of this graph.
	 * 
	 * @return
	 */
	public int getPopulation() {
		return population;
	}

	/**
	 * Checks if the input block is in this graph.
	 * 
	 * @param b
	 * @return true if the input block is in this graph. false otherwise
	 */
	public boolean hasBlock(Block b) {
		return blocks.contains(b);
	}

	/**
	 * Finds all the blocks in this graph that have not been assigned to a
	 * district.
	 * 
	 * @return and {@link ArrayList} of blocks such that for each block
	 *         {@link Block#getDistNo()} == {@link Block#UNASSIGNED}
	 */
	public ArrayList<Block> getUnassigned() {
		ArrayList<Block> unassigned = new ArrayList<Block>();

		for (Block b : blocks.values()) {

			if (b.getDistNo() == Block.UNASSIGNED)
				unassigned.add(b);
		}

		Collections.sort(unassigned);
		return unassigned;
	}

}
