package recoPrivResearch.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.StringBuilder;

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

//import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
//import gnu.trove.iterator.TFloatIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TLongIterator;

/**
 * Compute the lower half of the user-user similarity matrix, and put the result in a file to plot the distribution of similarities.
 * The dataset used must have less than Integer.MAX_VALUE users.
 * Otherwise, modifications are required starting at [1] in the code.
 */
public class PerfectlySimilarCounterpartsComputer {

	private static final Logger logger = LogManager.getLogger(PerfectlySimilarCounterpartsComputer.class);

	/*public static TLongObjectMap<TLongArrayList> usersPSCAsCosine;*/
	//public static TLongObjectMap<TLongArrayList> usersPSCAsJaccard;
	//public static TLongObjectMap<TLongArrayList> usersPSCAsCosineMahout;
	//public static TLongObjectMap<TLongArrayList> usersPSCAsCosineAverage;
	//public static TLongObjectMap<TLongArrayList> usersPSCAsPearson;
	//public static TLongObjectMap<TLongArrayList> usersPSCAsWupXontoY;
	/*public static TLongObjectMap<TLongArrayList> usersPSCAsWupYontoX;*/
	
	public static void main(String[] args) {
		
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
		boolean logPSCIDs = args[4].equals("true");
		


		int nbUsers = ExceptHandler.getModelNumUsers(model);

		TLongObjectMap<TLongArrayList> usersPSCAsCosine = new TLongObjectHashMap<TLongArrayList>(nbUsers);
		TLongObjectMap<TLongArrayList> usersPSCAsJaccard = new TLongObjectHashMap<TLongArrayList>(nbUsers);
		TLongObjectMap<TLongArrayList> usersPSCAsCosineMahout = new TLongObjectHashMap<TLongArrayList>(nbUsers);
		TLongObjectMap<TLongArrayList> usersPSCAsCosineAverage = new TLongObjectHashMap<TLongArrayList>(nbUsers);
		TLongObjectMap<TLongArrayList> usersPSCAsPearson = new TLongObjectHashMap<TLongArrayList>(nbUsers);
		TLongObjectMap<TLongArrayList> usersPSCAsWupXontoY = new TLongObjectHashMap<TLongArrayList>(nbUsers);
		TLongObjectMap<TLongArrayList> usersPSCAsWupYontoX = new TLongObjectHashMap<TLongArrayList>(nbUsers);

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

		// [1] Gather all user IDs in an array, to deal with non-consecutive IDs
		long[] userIDs = new long[nbUsers];
		int i = 0;
		for(LongPrimitiveIterator userIter = ExceptHandler.getModelUserIDs(model); userIter.hasNext();) {
			userIDs[i] = userIter.next(); // No need for bound checks because userIter will no return more than nbUsers

			usersPSCAsCosineMahout.put(userIDs[i], new TLongArrayList());
			usersPSCAsCosine.put(userIDs[i], new TLongArrayList());
			usersPSCAsJaccard.put(userIDs[i], new TLongArrayList());
			usersPSCAsCosineMahout.put(userIDs[i], new TLongArrayList());
			usersPSCAsCosineAverage.put(userIDs[i], new TLongArrayList());
			usersPSCAsPearson.put(userIDs[i], new TLongArrayList());
			usersPSCAsWupXontoY.put(userIDs[i], new TLongArrayList());
			usersPSCAsWupYontoX.put(userIDs[i], new TLongArrayList());

			i++;
		}

		/*BufferedWriter cosBw = null;*/
		//BufferedWriter cosAvgBw = null;
		//BufferedWriter cosMahoutBw = null;
		//BufferedWriter jacBw = null;
		//BufferedWriter pearsBw = null;
		//BufferedWriter wupXYBw = null;
		//BufferedWriter wupYXBw = null;

		//if(logPSCIDs) {
			//cosBw = getBufferedWriter(path, dataset, "cos");
			//cosAvgBw = getBufferedWriter(path, dataset, "cos-avg");
			//cosMahoutBw = getBufferedWriter(path, dataset, "cos-mahout");
			//jacBw = getBufferedWriter(path, dataset, "jaccard");
			//pearsBw = getBufferedWriter(path, dataset, "pearson");
			//wupXYBw = getBufferedWriter(path, dataset, "wupXontoY");
			//wupYXBw = getBufferedWriter(path, dataset, "wupYontoX");
		/*}*/

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
					/*if(!Float.isNaN(similarity)) {*/
						//usersPSCAsCosineMahout.add(similarity);
					//}
					//if(logPSCIDs) {
						//writeLineTo(cosMahoutBw, uid, fid, similarity);
					/*}*/
					if(similarity == 1.0) {
						recordPerfectlySimilarCounterpartsIDs(uid, fid, usersPSCAsCosineMahout);
					}

					// Cosine
					similarity = (float) -1.0;
					try {
						similarity = (float) cos.userSimilarity(uid, fid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					/*if(!Float.isNaN(similarity)) {*/
						//usersPSCAsCosine.add(similarity);
					//}
					//if(logPSCIDs) {
						//writeLineTo(cosBw, uid, fid, similarity);
					/*}*/
					if(similarity == 1.0) {
						recordPerfectlySimilarCounterpartsIDs(uid, fid, usersPSCAsCosine);
					}
					
					// Jaccard
					similarity = (float) -1.0;
					try {
						similarity = (float) jac.userSimilarity(uid, fid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					/*if(!Float.isNaN(similarity)) {*/
						//usersPSCAsJaccard.add(similarity);
					//}
					//if(logPSCIDs) {
						//writeLineTo(jacBw, uid, fid, similarity);
					/*}*/
					if(similarity == 1.0) {
						recordPerfectlySimilarCounterpartsIDs(uid, fid, usersPSCAsJaccard);
					}

					// Cosine-average
					similarity = (float) -1.0;
					try {
						similarity = (float) cosAvg.userSimilarity(uid, fid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					/*if(!Float.isNaN(similarity)) {*/
						//usersPSCAsCosineAverage.add(similarity);
					//}
					//if(logPSCIDs) {
						//writeLineTo(cosAvgBw, uid, fid, similarity);
					/*}*/
					if(similarity == 1.0) {
						recordPerfectlySimilarCounterpartsIDs(uid, fid, usersPSCAsCosineAverage);
					}

					// Pearson
					similarity = (float) -1.0;
					try {
						similarity = (float) pearson.userSimilarity(uid, fid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					/*if(!Float.isNaN(similarity)) {*/
						//usersPSCAsPearson.add(similarity);
					//}
					//if(logPSCIDs) {
						//writeLineTo(pearsBw, uid, fid, similarity);
					/*}*/
					if(similarity == 1.0) {
						recordPerfectlySimilarCounterpartsIDs(uid, fid, usersPSCAsPearson);
					}
					
					// WUP, X projected onto Y
					// Above assumption is false here, but meh.
					similarity = (float) -1.0;
					try {
						similarity = (float) wupXontoY.userSimilarity(uid, fid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					/*if(!Float.isNaN(similarity)) {*/
						//usersPSCAsWupXontoY.add(similarity);
					//}
					//if(logPSCIDs) {
						//writeLineTo(wupXYBw, uid, fid, similarity);
					/*}*/
					if(similarity == 1.0) {
						recordPerfectlySimilarCounterpartsForFirstUser(uid, fid, usersPSCAsWupXontoY);
					}

					// WUP, Y projected onto X
					// Above assumption is false here, but meh.
					similarity = (float) -1.0;
					try {
						similarity = (float) wupYontoX.userSimilarity(uid, fid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					/*if(!Float.isNaN(similarity)) {*/
						//usersPSCAsWupYontoX.add(similarity);
					//}
					//if(logPSCIDs) {
						//writeLineTo(wupYXBw, uid, fid, similarity);
					/*}*/
					if(similarity == 1.0) {
						recordPerfectlySimilarCounterpartsForFirstUser(uid, fid, usersPSCAsWupYontoX);
					}

				} else if(j < k) { // Compute the upper half of the user-user similarity matrix, w/ asymmetric measures only
					// WUP, X projected onto Y
					float similarity = (float) -1.0;
					try {
						similarity = (float) wupXontoY.userSimilarity(fid, uid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					/*if(!Float.isNaN(similarity)) {*/
						//usersPSCAsWupXontoY.add(similarity);
					//}
					//if(logPSCIDs) {
						//writeLineTo(wupXYBw, fid, uid, similarity);
					/*}*/
					if(similarity == 1.0) {
						recordPerfectlySimilarCounterpartsForFirstUser(fid, uid, usersPSCAsWupXontoY);
					}

					// WUP, Y projected onto X
					similarity = (float) -1.0;
					try {
						similarity = (float) wupYontoX.userSimilarity(fid, uid);
					} catch(TasteException e) {
						e.printStackTrace();
					}
					/*if(!Float.isNaN(similarity)) {*/
						//usersPSCAsWupYontoX.add(similarity);
					//}
					//if(logPSCIDs) {
						//writeLineTo(wupYXBw, fid, uid, similarity);
					/*}*/
					if(similarity == 1.0) {
						recordPerfectlySimilarCounterpartsForFirstUser(fid, uid, usersPSCAsWupYontoX);
					}

				}
			}
		}
		
		try {
			/*cosBw.close();*/
			//cosAvgBw.close();
			//cosMahoutBw.close();
			//jacBw.close();
			//pearsBw.close();
			//wupXYBw.close();
			//wupYXBw.close();

			cdfInt(path+dataset+"_PSCdistribution-cos.csv", usersPSCAsCosine);
			cdfInt(path+dataset+"_PSCdistribution-jaccard.csv", usersPSCAsJaccard);
			cdfInt(path+dataset+"_PSCdistribution-cos-mahout.csv", usersPSCAsCosineMahout);
			cdfInt(path+dataset+"_PSCdistribution-cos-avg.csv", usersPSCAsCosineAverage);
			cdfInt(path+dataset+"_PSCdistribution-pearson.csv", usersPSCAsPearson);
			cdfInt(path+dataset+"_PSCdistribution-wupXontoY.csv", usersPSCAsWupXontoY);
			cdfInt(path+dataset+"_PSCdistribution-wupYontoX.csv", usersPSCAsWupYontoX);

			if(logPSCIDs) {
				writePSCIDs(path, dataset, "cos", usersPSCAsCosine);
				writePSCIDs(path, dataset, "cos-mahout", usersPSCAsCosineMahout);
				writePSCIDs(path, dataset, "cos-avg", usersPSCAsCosineAverage);
				writePSCIDs(path, dataset, "jaccard", usersPSCAsJaccard);
				writePSCIDs(path, dataset, "pearson", usersPSCAsPearson);
				writePSCIDs(path, dataset, "wupXontoY", usersPSCAsWupXontoY);
				writePSCIDs(path, dataset, "wupYontoX", usersPSCAsWupYontoX);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Add user1 and user2 to each other's list of PSC.
	 */
	private static void recordPerfectlySimilarCounterpartsIDs(long user1, long user2, TLongObjectMap<TLongArrayList> map) {
		recordPerfectlySimilarCounterpartsForFirstUser(user1, user2, map);
		recordPerfectlySimilarCounterpartsForFirstUser(user2, user1, map);
	}

	/**
	 * Add user2 to user1's list of PSC.
	 */
	private static void recordPerfectlySimilarCounterpartsForFirstUser(long user1, long user2, TLongObjectMap<TLongArrayList> map) {
		TLongArrayList user1sPSC = map.get(user1);

		// Create the list if necessary
		/*if(user1sPSC == null) {*/
			//user1sPSC = new TLongArrayList();
			//map.put(user1, user1sPSC);
		/*}*/
		
		user1sPSC.add(user2);
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
	private static void cdfInt(String path, TLongObjectMap<TLongArrayList> map) throws IOException {
		
		logger.info("Writing number of PSC distribution in {}", path);

		TIntArrayList allNbPSC = gatherAllNbPSC(map);

		allNbPSC.sort();
		
		int totalNbVal = allNbPSC.size();
		int nbVal = 0;
		int currentVal = -1;
		double cumulativePercent;
		long counter = 0;
		
		FileWriter fw = new FileWriter(path);
		BufferedWriter bw = new BufferedWriter(fw);
		StringBuilder builder = new StringBuilder(20000000); // Arbitrary capacity, see [2]
		builder.append("#similarity,cumulativePercentage\n");
		
		for(TIntIterator it=allNbPSC.iterator(); it.hasNext(); ) {
			int nbPSC = it.next();
			counter++;

			if(currentVal != nbPSC) {
				cumulativePercent = nbVal;
				cumulativePercent /= (double) totalNbVal;
				cumulativePercent = 1 - cumulativePercent;
				currentVal = nbPSC;

				builder.append(nbPSC).append(",").append(cumulativePercent).append("\n");
			}
			nbVal++;

			// [2] Write data to the result file every 1M lines (arbitrary number) because big 
			// datasets like Jester produce so much data it cannot be written all at once. This 
			// limitation comes from StringBuilder.toString copying the internal buffer (how 
			// many copies?).
			if(counter == 1000000) {
				counter = 0;
				bw.write(builder.toString());
				builder = new StringBuilder(20000000);
			}
		}
		
		bw.write(builder.toString());
		bw.close();
	}
		
	private static TIntArrayList gatherAllNbPSC(TLongObjectMap<TLongArrayList> map) {
		TIntArrayList result = new TIntArrayList();

		for(TLongIterator it=map.keySet().iterator(); it.hasNext(); ) {
			result.add(map.get(it.next()).size());
		}
		
		return result;
	}

	private static void writePSCIDs(String path, String prefix, String suffix, TLongObjectMap<TLongArrayList> map) throws IOException {
		logger.info("Writing all users' PSC IDs in {}{}_PSC{}.csv", path, prefix, suffix);

		FileWriter fw = fw = new FileWriter(path+prefix+"_PSC-"+suffix+".csv");
		BufferedWriter bw = new BufferedWriter(fw);

		long counter = 0;
		StringBuilder builder = new StringBuilder(20000); // Arbitrary capacity, see [2]
		builder.append("# user,nbPSC,PSCiD1;PSCiD2;PSCiD3;...\n");

		for(TLongIterator it=map.keySet().iterator(); it.hasNext(); ) {
			counter++;
			long user = it.next();
			builder.append(user).append(",").append(map.get(user).size()).append(",");

			for(TLongIterator iter=map.get(user).iterator(); iter.hasNext(); ) {
				builder.append(iter.next()).append(";");
			}
			builder.replace(builder.length()-1, builder.length(), "\n");

			if(counter == 1000) {
				counter = 0;
				bw.write(builder.toString());
				builder = new StringBuilder(20000);
			}
		}

		bw.write(builder.toString());
		bw.close();
	}

	/*private static void writeLineTo(BufferedWriter bw, long uid, long fid, float similarity) {*/

		//StringBuilder builder = new StringBuilder();

		//builder.append(uid).append(",").append(fid).append(",").append(similarity);

		//try {
			//bw.write(builder.toString());
			//bw.newLine();
		//} catch(IOException e) {
			//e.printStackTrace();
		//}
	/*}*/
}
