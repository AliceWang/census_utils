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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import edu.gatech.c4g.r4g.Redistrict;
import edu.gatech.c4g.r4g.model.Block;
import edu.gatech.c4g.r4g.model.BlockGraph;

/**
 * Saver utility class that provides the functions to output the result of the
 * redistricting algorithm to a shapefile and to a DST file.
 * 
 * @author aaron
 * 
 */
public class Saver {

	/**
	 * Saves the input {@link BlockGraph} with district information to a
	 * shapefile and to a DST file.
	 * 
	 * @param source the original {@link FeatureSource} from which bg was created
	 * @param bg
	 * @param outputFile the name of the output file (without extension)
	 * 
	 * @return the file that contains the new shapefile (for display purposes)
	 */
	public static File save(
			FeatureSource<SimpleFeatureType, SimpleFeature> source,
			BlockGraph bg, String outputFile) {
		// part 1 - save new shapefile
		System.out.println("Saving new Shapefile");
		
		File newShapefile = new File(outputFile + "_" + bg.getDistrictCount());	
		File redistrictedShapefile = saveShapefile(source, bg, newShapefile);

		// part 2 - write which blocks are in which district
		System.out.println("Saving districting data to " + outputFile + "_" + bg.getDistrictCount() + ".dst");
		saveDST(bg, new File(outputFile + "_" + bg.getDistrictCount() + ".dst"));
		return redistrictedShapefile; 

	}

	/**
	 * Saves a {@link BlockGraph} to a shapefile, adding district information
	 * @param source
	 * @param bg
	 * @param outFile
	 */
	private static File saveShapefile(
			FeatureSource<SimpleFeatureType, SimpleFeature> source,
			BlockGraph bg, File outFile) {
		SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
		sftb.init(source.getSchema());
		sftb.add("DISTRICT", Integer.class);
		SimpleFeatureType withDistrict = sftb.buildFeatureType();

		SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(withDistrict);

		FeatureCollection<SimpleFeatureType, SimpleFeature> newCollection = FeatureCollections
				.newCollection();

		for (Block b : bg.getAllBlocks()) {
			sfb.init(b.getFeature());
			sfb.set("DISTRICT", b.getDistNo());
			SimpleFeature sf = sfb.buildFeature("" + b.getId());
			newCollection.add(sf);
		}

		/*
		 * Get an output file name and create the new shapefile
		 */
		File newFile;
		if (Redistrict.GRAPHICS_ENABLED) {
			newFile = getNewShapeFile(outFile);
		} else {
			String path = outFile.getAbsolutePath();
			String newPath = path + ".shp";
			newFile = new File(newPath);
		}

		DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();

		Map<String, Serializable> params = new HashMap<String, Serializable>();
		try {
			params.put("url", newFile.toURI().toURL());
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		params.put("create spatial index", Boolean.TRUE);

		ShapefileDataStore newDataStore;
		try {
			newDataStore = (ShapefileDataStore) dataStoreFactory
					.createNewDataStore(params);
			newDataStore.createSchema(withDistrict);
			newDataStore.forceSchemaCRS(source.getSchema()
					.getCoordinateReferenceSystem());

			/*
			 * Write the features to the shapefile
			 */
			Transaction transaction = new DefaultTransaction("create");

			String typeName = newDataStore.getTypeNames()[0];
			FeatureStore<SimpleFeatureType, SimpleFeature> featureStore;
			featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) newDataStore
					.getFeatureSource(typeName);

			featureStore.setTransaction(transaction);
			try {
				featureStore.addFeatures(newCollection);
				transaction.commit();

			} catch (Exception problem) {
				problem.printStackTrace();
				transaction.rollback();

			} finally {
				transaction.close();
			}

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return newFile;
	}

	private static void saveDST(BlockGraph bg, File dstFile) {
		try {
			// Create file
			FileWriter fstream = new FileWriter(dstFile);
			BufferedWriter out = new BufferedWriter(fstream);

			// write block count and district count
			out.write(bg.getAllBlocks().size() + " " + bg.getDistrictCount()
					+ "\n");

			ArrayList<Block> blocks = new ArrayList<Block>(bg.getAllBlocks());
			Collections.sort(blocks, new Comparator<Block>() {
				public int compare(Block o1, Block o2) {
					Integer id1 = o1.getId();
					Integer id2 = o2.getId();
					return id1.compareTo(id2);
				};
			});

			for (Block b : bg.getAllBlocks()) {
				out.write(b.getId() + " " + b.getDistNo() + "\n");
			}

			out.close();

			System.out.println("DST info written to "
					+ dstFile.getAbsolutePath());
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

	/**
	 * Prompt the user for the name and path to use for the output shapefile
	 */
	private static File getNewShapeFile(File f) {
		String path = f.getAbsolutePath();
		String newPath = path + ".shp";

		JFileDataStoreChooser chooser = new JFileDataStoreChooser("shp");
		chooser.setDialogTitle("Save shapefile");
		chooser.setSelectedFile(new File(newPath));

		int returnVal = chooser.showSaveDialog(null);

		if (returnVal != JFileDataStoreChooser.APPROVE_OPTION) {
			// the user cancelled the dialog
			System.exit(0);
		}

		File newFile = chooser.getSelectedFile();
		if (newFile.equals(f)) {
			System.out.println("Error: cannot replace " + f);
			System.exit(0);
		}

		return newFile;
	}

}
