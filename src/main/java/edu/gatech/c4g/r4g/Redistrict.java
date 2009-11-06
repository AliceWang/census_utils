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

import edu.gatech.c4g.r4g.redistricting.AustralianRedistrictingAlgorithm;
import edu.gatech.c4g.r4g.redistricting.RedistrictingAlgorithm;
import edu.gatech.c4g.r4g.util.AustralianLoader;

public class Redistrict {

	private static final String USAGE = "[-h] -a <algorithm> -n <number_of_districts> -d <max_deviation> -i <input_file>";
	private static final String HEADER = "Redistricting4Good - A bunch of algorithms to (fairly?) redistrict Australia (with GeoTools "
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

			if (!commandLine.hasOption('i')) {
				file = JFileDataStoreChooser.showOpenFile("shp", null);
				if (file == null) {
					return;
				}
			} else {
				file = new File(commandLine.getOptionValue('i'));
			}

			if (file == null) {
				System.err.println("A shapefile must be selected!");
				System.exit(1);
			}
			
			double maxDeviation = Double.parseDouble(commandLine.getOptionValue('d')); 
			if ((maxDeviation > 1) || (maxDeviation < 0)){
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

			//MapView mv = new MapView(source);
			//mv.showShapefile();

			RedistrictingAlgorithm ra = null;

			if (alg.equals("australia")) {
				// run the algorithm
				ra = new AustralianRedistrictingAlgorithm(
						new AustralianLoader(), source, galFile);
			} else if (alg.equals("usa")) {
				// ra = american algorithm
			} else {
				System.err.println("Algorithm type not recognized!");
				System.exit(1);
			}

			System.out.println("Redistricting. You can have a coffee while you are waiting");
			ra.redistrict(ndis, maxDeviation);

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
		Option file = OptionBuilder.withArgName("input_file").hasArgs(1)
				.withDescription("Specify the input file (without extension)")
				.create("i");

		options = new Options();
		options.addOption(help);
		options.addOption(algorithm);
		options.addOption(ndists);
		options.addOption(deviation);
		options.addOption(file);
	}

	private static void printUsage(Options options) {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.setWidth(80);
		helpFormatter.printHelp(USAGE, HEADER, options, FOOTER);
	}

}