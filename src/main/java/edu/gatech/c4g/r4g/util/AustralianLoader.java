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

package edu.gatech.c4g.r4g.util;

import java.util.ArrayList;

import org.geotools.data.FeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import edu.gatech.c4g.r4g.model.Block;
import edu.gatech.c4g.r4g.model.BlockGraph;

/**
 * Loader for the Australian redistricting algorithm. It takes care of removing
 * all the blocks that represent natural borders or that are useless for
 * redistricting (i.e. Water and Shipping)
 * 
 * @author aaron
 * 
 */
public class AustralianLoader extends Loader {

	@Override
	public BlockGraph load(
			FeatureSource<SimpleFeatureType, SimpleFeature> source,
			String galFile) {

		BlockGraph bg = super.load(source, galFile);

		return removeNaturalBorders(bg);
	}

	/**
	 * Removes all the blocks in the input {@link BlockGraph} that represent
	 * natural borders.
	 * 
	 * @param bg
	 * @return
	 */
	private BlockGraph removeNaturalBorders(BlockGraph bg) {
		ArrayList<Block> toRemove = new ArrayList<Block>();

		for (Block b : bg.getAllBlocks()) {
			SimpleFeature f = b.getFeature();
			String cat = (String) f.getProperty("CATEGORY").getValue();
			if (cat.equals(Block.CATEGORY_WATER)
					|| cat.equals(Block.CATEGORY_SHIPPING)) {
				toRemove.add(b);
			}
		}

		bg.removeAllBlocks(toRemove);

		return bg;
	}

}
