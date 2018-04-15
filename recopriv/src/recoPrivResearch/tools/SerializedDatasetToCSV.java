package recoPrivResearch.tools;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ClassNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Iterator;

import threadedSim.util.ScoreCount;

import com.opencsv.CSVWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SerializedDatasetToCSV {

	private static final Logger logger = LogManager.getLogger(SerializedDatasetToCSV.class);

	/**
	 * Convert a serialized Hashtable<Int,ScoreCount> object to a CSV file.
	 * The CSV file is written in the same directory as the original serialized object.
	 */
	public static void convertHashtableIntScoreCountToCSV(String serializedObj) {
		// Deserialize the dataset
		Hashtable<Integer, ScoreCount<Integer>> dataset = deserializeHastableIntScoreCount(serializedObj);
		
		String dir = getDirname(serializedObj);
		String resultFilename = getResultFilename(serializedObj);
		logger.debug("The resulting CSV file '{}' will be written to '{}'", resultFilename, dir);

		writeHashtableIntScoreCountToCSV(dataset, dir+resultFilename);
	}

	/**
	 * Deserialize a Hashtable<Int,ScoreCount> object from path.
	 */
	private static Hashtable<Integer, ScoreCount<Integer>> deserializeHastableIntScoreCount(String path) {
		Hashtable<Integer, ScoreCount<Integer>> result = null;

		FileInputStream file = ExceptHandler.createFileInputStream(path);
		ObjectInputStream objStream = ExceptHandler.createObjectInputStream(file);
		result = readHashtableObject(objStream);

		return result;
	}

	/**
	 * Equivalent to bash util dirname.
	 * Returns null if pathToFile has no parent dir.
	 */
	private static String getDirname(String pathToFile) {
		String result = null;
		Path originalFile = Paths.get(pathToFile);
		Path resultDir = originalFile.getParent();
		if(resultDir != null) {
			logger.debug("A parent path exists: {}", resultDir.toString());
			resultDir.normalize(); // remove redundant elements from the path
			result = resultDir.toString() + "/";
			logger.debug("Normalized parent path: {}", result);
		}
		return result;
	}

	/**
	 * Returns the same filename as the file designated by pathToFile with the last extension replaced by ".csv".
	 * Returns null if pathToFile has 0 elements.
	 */
	private static String getResultFilename(String pathToFile) {
		String result = null;
		Path originalFilename = Paths.get(pathToFile).getFileName();
		if(originalFilename != null) {
			logger.debug("A file name exists: {}", originalFilename.toString());
			String[] nameParts = originalFilename.toString().split("[.]");
			logger.debug("The {} detected part(s) of the file name are:", nameParts.length);
			result = "";
			for(int i=0; i<nameParts.length-1; i++) {
				logger.debug("Part {}: {}", i+1, nameParts[i]);
				result += nameParts[i];
			}
			logger.debug("Part {}: {}", nameParts.length, nameParts[nameParts.length-1]);
			result += ".csv";
		}
		return result;
	}

	/**
	 * Write to path the user profiles from dataset in the following CSV format (i.e. 1 rating/line):
	 * userID, itemID, rating
	 * userID, itemID2, rating
	 * etc.
	 */
	private static void writeHashtableIntScoreCountToCSV(Hashtable<Integer, ScoreCount<Integer>> dataset, String path) {
		CSVWriter writer = new CSVWriter(new BufferedWriter(ExceptHandler.createFileWriter(path)));
		logger.trace("Content of the CSV file");
		long nbLines = 0;
		boolean quoteElements = false;
		for(Iterator<Integer> it=dataset.keySet().iterator(); it.hasNext();) {
			Integer userID = it.next();
			ScoreCount<Integer> profile = dataset.get(userID);
			for(Iterator<Integer> iter=profile.getItems().iterator(); iter.hasNext();) {
				Integer itemID = iter.next();
				Double rating = profile.getValue(itemID);

				String[] csvLine = {userID.toString(), itemID.toString(), rating.toString()};
				logger.trace("csvLine: {},{},{}", userID, itemID, rating);
				writer.writeNext(csvLine, quoteElements);
				nbLines++;
			}
		}
		ExceptHandler.closeCSVWriter(writer);
		logger.info("{} lines written", nbLines);
	}

	/**
	 * Simple wrapper around ObjectInputStream.readObject() to improve code readability.
	 */
	@SuppressWarnings("unchecked")
	public static Hashtable<Integer, ScoreCount<Integer>> readHashtableObject(ObjectInputStream stream) {
		Hashtable<Integer, ScoreCount<Integer>> result = null;
		try {
			result = (Hashtable<Integer, ScoreCount<Integer>>) stream.readObject();
		} catch(IOException|ClassNotFoundException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Converting all files given as arguments to CSV files.
	 * Requirements: each argument should be the path to a serialized Hashtable<Int,ScoreCount<Int>>.
	 */
	public static void main(String[] args) {
		for(int i=0; i<args.length; i++) {
			logger.info("Converting {}", args[i]);
			convertHashtableIntScoreCountToCSV(args[i]);
		}
	}

}
