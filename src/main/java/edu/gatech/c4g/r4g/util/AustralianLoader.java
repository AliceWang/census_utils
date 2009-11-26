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
