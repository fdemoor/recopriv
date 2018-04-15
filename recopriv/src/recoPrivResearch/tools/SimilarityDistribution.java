package recoPrivResearch.tools;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.AbstractSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.AveragingPreferenceInferrer;
import org.apache.mahout.cf.taste.impl.similarity.ZeroingPreferenceInferrer;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;

// Log4J1
import org.apache.log4j.PropertyConfigurator;

// Log4J2
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.iterator.TFloatIterator;

/**
 * Compute the lower half of the user-user similarity matrix, and put the result in a file to plot the distribution of similarities.
 * The dataset used must have less than Integer.MAX_VALUE users.
 * Otherwise, modifications are required starting at [1] in the code.
 */
public class SimilarityDistribution {

	private static final Logger logger = LogManager.getLogger(SimilarityDistribution.class);

	public static TFloatArrayList listCosine;
	public static TFloatArrayList listJaccard;
	public static TFloatArrayList listCosMahout;
	public static TFloatArrayList listCosAvg;
	public static TFloatArrayList listPearson;
	public static TFloatArrayList listWupXontoY;
	public static TFloatArrayList listWupYontoX;
	
	public static void main(String[] args) {
		
		listCosine = new TFloatArrayList();
		listJaccard = new TFloatArrayList();
		listCosMahout = new TFloatArrayList();
		listCosAvg = new TFloatArrayList();
		listPearson = new TFloatArrayList();
		listWupXontoY = new TFloatArrayList();
		listWupYontoX = new TFloatArrayList();

		
		logger.debug("Log4j1 config file path (as parsed from CLI): {}", args[0]);
		logger.debug("Dataset path (as parsed from CLI): {}", args[1]);
		logger.debug("Result files name prefix (as parsed from CLI): {}", args[2]);
		logger.debug("Path where to write result files (as parsed from CLI): {}", args[3]);
		logger.debug("Write similarity values to some files? (as parsed from CLI): {}", args[4]);

		// A Log4J1 conf file is required for Mahout's classes WARN/INFO/DEBUG/etc. outputs.
		PropertyConfigurator.configure(args[0]);

		logger.info("Loading profiles");
		DataModel model = ExceptHandler.createDataModel(args[1]);

		String dataset = args[2];
		String path = args[3];
		boolean logSimilarityValues = args[4].equals("true");
		


		// Creating similarity computers
		UserSimilarity cosMahout = null;
		try {
			cosMahout = new UncenteredCosineSimilarity(model);
		} catch(TasteException e) {
			e.printStackTrace();
		}

		UserSimilarity cos = null;
		try {
			cos = new UncenteredCosineSimilarity(model);
		} catch(TasteException e) {
			e.printStackTrace();
		}
		cos.setPreferenceInferrer(new ZeroingPreferenceInferrer());

		UserSimilarity jac = null;
		jac = new TanimotoCoefficientSimilarity(model);

		UserSimilarity cosAvg = null;
		try {
			cosAvg = new UncenteredCosineSimilarity(model);
		} catch(TasteException e) {
			e.printStackTrace();
		}
		cosAvg.setPreferenceInferrer(ExceptHandler.getAveragingPrefInferrer(model)); 

		UserSimilarity pearson = null;
		try {
			pearson = new PearsonCorrelationSimilarity(model);
		} catch(TasteException e) {
			e.printStackTrace();
		}
		pearson.setPreferenceInferrer(new ZeroingPreferenceInferrer());
		
		UserSimilarity wupXontoY = null;
		try {
			wupXontoY = new UncenteredCosineSimilarity(model);
		} catch(TasteException e) {
			e.printStackTrace();
		}
		wupXontoY.setPreferenceInferrer(new ZeroingPreferenceInferrer()); 
		((AbstractSimilarity) wupXontoY).setEnableXInferrer(true);
		((AbstractSimilarity) wupXontoY).setEnableYInferrer(false);

		UserSimilarity wupYontoX = null;
		try {
			wupYontoX = new UncenteredCosineSimilarity(model);
		} catch(TasteException e) {
			e.printStackTrace();
		}
		wupYontoX.setPreferenceInferrer(new ZeroingPreferenceInferrer()); 
		((AbstractSimilarity) wupYontoX).setEnableXInferrer(false);
		((AbstractSimilarity) wupYontoX).setEnableYInferrer(true);



		// For each user, compute her similarity with all the other users

		int nbUsers = ExceptHandler.getModelNumUsers(model);
		// [1] Gather all user IDs in an array, to deal with non-consecutive IDs
		long[] userIDs = new long[nbUsers];
		int i = 0;
		for(LongPrimitiveIterator userIter = ExceptHandler.getModelUserIDs(model); userIter.hasNext();) {
			userIDs[i] = userIter.next(); // No need for bound checks because userIter will no return more than nbUsers
			i++;
		}

		BufferedWriter cosBw = null;
		BufferedWriter cosAvgBw = null;
		BufferedWriter cosMahoutBw = null;
		BufferedWriter jacBw = null;
		BufferedWriter pearsBw = null;
		BufferedWriter wupXYBw = null;
		BufferedWriter wupYXBw = null;

		if(logSimilarityValues) {
			cosBw = getBufferedWriter(path, dataset, "cos");
			cosAvgBw = getBufferedWriter(path, dataset, "cos-avg");
			cosMahoutBw = getBufferedWriter(path, dataset, "cos-mahout");
			jacBw = getBufferedWriter(path, dataset, "jaccard");
			pearsBw = getBufferedWriter(path, dataset, "pearson");
			wupXYBw = getBufferedWriter(path, dataset, "wupXontoY");
			wupYXBw = getBufferedWriter(path, dataset, "wupYontoX");
		}

		// Actual similarity computations
		for(int j=0; j<nbUsers; j++) {
			long uid = userIDs[j];
			logger.info("Processing user {}/{}, ID: {}", j+1, nbUsers, uid);

			for(int k=0; k<nbUsers; k++) {
				long fid = userIDs[k];
				
				if(j > k) { // Compute the lower half of the user-user similarity matrix, w/ all measures
					// Cosine-mahout
					float similarity = (float) -1.0;
					try {
						similarity = (float) cosMahout.userSimilarity(uid, fid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					// UserSimilarity may return NaN, when users have no common item
					// or when the denominator in the similarity formula is 0.
					if(!Float.isNaN(similarity)) {
						listCosMahout.add(similarity);
					}
					if(logSimilarityValues) {
						writeLineTo(cosMahoutBw, uid, fid, similarity);
					}

					// Cosine
					similarity = (float) -1.0;
					try {
						similarity = (float) cos.userSimilarity(uid, fid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					if(!Float.isNaN(similarity)) {
						listCosine.add(similarity);
					}
					if(logSimilarityValues) {
						writeLineTo(cosBw, uid, fid, similarity);
					}
					
					// Jaccard
					similarity = (float) -1.0;
					try {
						similarity = (float) jac.userSimilarity(uid, fid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					if(!Float.isNaN(similarity)) {
						listJaccard.add(similarity);
					}
					if(logSimilarityValues) {
						writeLineTo(jacBw, uid, fid, similarity);
					}

					// Cosine-average
					similarity = (float) -1.0;
					try {
						similarity = (float) cosAvg.userSimilarity(uid, fid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					if(!Float.isNaN(similarity)) {
						listCosAvg.add(similarity);
					}
					if(logSimilarityValues) {
						writeLineTo(cosAvgBw, uid, fid, similarity);
					}

					// Pearson
					similarity = (float) -1.0;
					try {
						similarity = (float) pearson.userSimilarity(uid, fid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					if(!Float.isNaN(similarity)) {
						listPearson.add(similarity);
					}
					if(logSimilarityValues) {
						writeLineTo(pearsBw, uid, fid, similarity);
					}
					
					// WUP, X projected onto Y
					// Above assumption is false here, but meh.
					similarity = (float) -1.0;
					try {
						similarity = (float) wupXontoY.userSimilarity(uid, fid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					if(!Float.isNaN(similarity)) {
						listWupXontoY.add(similarity);
					}
					if(logSimilarityValues) {
						writeLineTo(wupXYBw, uid, fid, similarity);
					}

					// WUP, Y projected onto X
					// Above assumption is false here, but meh.
					similarity = (float) -1.0;
					try {
						similarity = (float) wupYontoX.userSimilarity(uid, fid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					if(!Float.isNaN(similarity)) {
						listWupYontoX.add(similarity);
					}
					if(logSimilarityValues) {
						writeLineTo(wupYXBw, uid, fid, similarity);
					}

				} else if(j < k) { // Compute the upper half of the user-user similarity matrix, w/ asymmetric measures only
					// WUP, X projected onto Y
					float similarity = (float) -1.0;
					try {
						similarity = (float) wupXontoY.userSimilarity(fid, uid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					if(!Float.isNaN(similarity)) {
						listWupXontoY.add(similarity);
					}
					if(logSimilarityValues) {
						writeLineTo(wupXYBw, fid, uid, similarity);
					}

					// WUP, Y projected onto X
					similarity = (float) -1.0;
					try {
						similarity = (float) wupYontoX.userSimilarity(fid, uid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					if(!Float.isNaN(similarity)) {
						listWupYontoX.add(similarity);
					}
					if(logSimilarityValues) {
						writeLineTo(wupYXBw, fid, uid, similarity);
					}

				}
			}
		}
		
		try {
			cosBw.close();
			cosAvgBw.close();
			cosMahoutBw.close();
			jacBw.close();
			pearsBw.close();
			wupXYBw.close();
			wupYXBw.close();

			cdfDouble(path+dataset+"_distribution-cos.csv", listCosine);
			cdfDouble(path+dataset+"_distribution-jaccard.csv", listJaccard);
			cdfDouble(path+dataset+"_distribution-cos-mahout.csv", listCosMahout);
			cdfDouble(path+dataset+"_distribution-cos-avg.csv", listCosAvg);
			cdfDouble(path+dataset+"_distribution-pearson.csv", listPearson);
			cdfDouble(path+dataset+"_distribution-wupXontoY.csv", listWupXontoY);
			cdfDouble(path+dataset+"_distribution-wupYontoX.csv", listWupYontoX);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	

	/**
	 * Generate the sorted data points needed to plot a CDF, from a list of similarity values.
	 * Result is written in the file designated by path.
	 * Will overwrite any file already present.
	 * The result is a 2 columns CSV file.
	 * Each line represents a data point.
	 * The first column is similarity values.
	 * The second column is cumulative percentages, expressed within [0:1].
	 */
	private static void cdfDouble(String path, TFloatArrayList list) throws IOException {
		
		logger.info("Writing similarity distribution in {}", path);

		list.sort();
		
		int totalNbVal = list.size();
		double nbVal = 0;
		double currentVal = -1;
		double cumulativePercent;
		long counter = 0;
		
		FileWriter fw = new FileWriter(path);
		BufferedWriter bw = new BufferedWriter(fw);
		StringBuilder builder = new StringBuilder(1000000); // Abritrary capacity, see [2]
		builder.append("# similarity, cumulativePercentage\n");
		
		for(TFloatIterator it=list.iterator(); it.hasNext(); ) {
			float simil = it.next();
			counter++;

			if(currentVal != simil) {
				cumulativePercent = nbVal;
				cumulativePercent /= totalNbVal;
				cumulativePercent = 1 - cumulativePercent;
				currentVal = simil;

				builder.append(simil).append(",").append(cumulativePercent).append("\n");
			}
			nbVal++;

			// [2] Write data to the result file every 1M lines (arbitrary number) because big 
			// datasets like Jester produce so much data it cannot be written all at once. This 
			// limitation comes from StringBuilder.toString copying the internal buffer (how 
			// many copies?).
			if(counter == 1000000) {
				counter = 0;
				bw.write(builder.toString());
				builder = new StringBuilder(1000000);
			}
		}
		
		bw.write(builder.toString());
		bw.close();
	}
		
	private static BufferedWriter getBufferedWriter(String path, String prefix, String suffix) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(path+prefix+"_similValues-"+suffix+".csv");
		} catch(IOException e) {
			e.printStackTrace();
		}

		BufferedWriter bw = new BufferedWriter(fw);
		try {
			bw.write("# userX, userY, similarity");
			bw.newLine();
		} catch(IOException e) {
			e.printStackTrace();
		}

		return bw;
	}

	private static void writeLineTo(BufferedWriter bw, long uid, long fid, float similarity) {

		StringBuilder builder = new StringBuilder();

		builder.append(uid).append(",").append(fid).append(",").append(similarity);

		try {
			bw.write(builder.toString());
			bw.newLine();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
