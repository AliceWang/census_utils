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

package edu.gatech.c4g.r4g;

import java.io.File;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.factory.GeoTools;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import edu.gatech.c4g.r4g.redistricting.*;
import edu.gatech.c4g.r4g.util.*;
import edu.gatech.c4g.r4g.view.MapView;

public class Redistrict {
	public static String POPULATION_FIELD;
	public static boolean GRAPHICS_ENABLED = true;
	
	
	private static final String USAGE = "[-h] -a <algorithm> -n <number_of_districts> -d <max_deviation> -i <input_file> [-nographics]";
	private static final String HEADER = "OpenRedistricting - A bunch of algorithms to (fairly?) redistrict (with GeoTools "
			+ GeoTools.getVersion() + ")";
	private static final String FOOTER = "\nCopyright 2009 - Aaron Ciaghi, Stephen Long, Joshua Justice";
	private static Options options;

	public static void main(String[] args) throws Exception {

		setup();

		// Create a Parser
		CommandLineParser parser = new BasicParser();

		// Parse the program arguments
		CommandLine commandLine = parser.parse(options, args);
		if (commandLine.hasOption('h')) {
			printUsage(options);
			System.exit(0);
		}

		if (commandLine.hasOption('a') && commandLine.hasOption('n')
				&& commandLine.hasOption('d')) {
			File file = null;
			
			if (commandLine.hasOption("nographics")){
				GRAPHICS_ENABLED = false;
			}

			if (!commandLine.hasOption('i')) {
				if (GRAPHICS_ENABLED) {
					file = JFileDataStoreChooser.showOpenFile("shp", null);
					if (file == null) {
						return;
					}
				}
			} else {
				file = new File(commandLine.getOptionValue('i'));
			}

			if (file == null) {
				System.err.println("A shapefile must be selected!");
				System.exit(1);
			}

			double maxDeviation = Double.parseDouble(commandLine
					.getOptionValue('d'));
			if ((maxDeviation > 1) || (maxDeviation < 0)) {
				System.err.println("Max Deviation must be between 0 and 1!");
				System.exit(1);
			}

			int ndis = Integer.parseInt(commandLine.getOptionValue('n'));
			String alg = commandLine.getOptionValue('a');

			FileDataStore store = FileDataStoreFinder.getDataStore(new File(
					commandLine.getOptionValue('i')));
			FeatureSource<SimpleFeatureType, SimpleFeature> source = store
					.getFeatureSource();

			String filename = file.getAbsolutePath();
			String galFile = filename.substring(0, filename.length() - 4)
					+ ".GAL";

			RedistrictingAlgorithm ra = null;

			if (alg.equals("australia")) {
				// run the algorithm
				POPULATION_FIELD = "TURPOP2006";
				ra = new AustralianRedistrictingAlgorithm(
						new AustralianLoader(), source, galFile);
			} else if (alg.equals("usa")) {
				POPULATION_FIELD = "POP100";
				ra = new AmericanRedistrictingAlgorithm(new AmericanLoader(),
						source, galFile);
			} else {
				System.err.println("Algorithm type not recognized!");
				System.exit(1);
			}

			System.out
					.println("Redistricting. You can have a coffee while you are waiting");
			ra.redistrict(ndis, maxDeviation);

			File districtedShapefile = Saver.save(source, ra.getBlockGraph(), filename.substring(0,
					filename.length() - 4));
			
			if (GRAPHICS_ENABLED){
				MapView mv = new MapView(districtedShapefile);
				mv.showShapefile();
			}

		} else {
			printUsage(options);
			System.exit(0);
		}

	}

	@SuppressWarnings("static-access")
	private static void setup() {
		Option help = new Option("h", "Print this message");
		Option algorithm = OptionBuilder
				.withArgName("algorithm")
				.hasArgs(1)
				.withDescription(
						"Specify the desired redistricting algorithm ('australia' or 'usa')")
				.create("a");
		Option ndists = OptionBuilder.withArgName("n_dist").hasArgs(1)
				.withDescription("Specify the number of districts to create")
				.create("n");
		Option deviation = OptionBuilder
				.withArgName("max_deviation")
				.hasArgs(1)
				.withDescription(
						"Max deviation allowed for the population of a district from the ideal population")
				.create("d");
		Option file = OptionBuilder
				.withArgName("input_file")
				.hasArgs(1)
				.withDescription("Specify the input file (with .shp extension)")
				.create("i");
		Option nographics = OptionBuilder.withDescription(
				"Disable graphic elements").create("nographics");

		options = new Options();
		options.addOption(help);
		options.addOption(algorithm);
		options.addOption(ndists);
		options.addOption(deviation);
		options.addOption(file);
		options.addOption(nographics);
	}

	private static void printUsage(Options options) {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.setWidth(80);
		helpFormatter.printHelp(USAGE, HEADER, options, FOOTER);
	}

}