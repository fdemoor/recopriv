package recoPrivResearch.recommenderBuilder;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.neighborhood.PPNSUserNeighborhood;
import org.apache.mahout.cf.taste.impl.similarity.TwoStepUncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import recoPrivResearch.recommenderBuilder.KNNRecommenderBuilder;
import recoPrivResearch.tools.ExceptHandler;

public abstract class KNNRecommenderBuilder implements NeighborhoodRecommenderBuilder {

	private static final Logger logger = LogManager
			.getLogger(KNNRecommenderBuilder.class);
	private Logger runTimeLogger = LogManager.getLogger(KNNRecommenderBuilder.class.getName() + "-runTime");
	private static HashSet<Double> similarityDistro=new HashSet<Double>();
	private static int numCalled=0;
	private final int k_; // of kNN
	private final boolean isPPNS_;
	private final int beta_;
	protected UserNeighborhood neighborhood;

	public KNNRecommenderBuilder(int k, boolean isPPNS, int beta) {
		k_ = k;
		isPPNS_ = isPPNS;
		beta_ = beta;
	}

	public abstract Recommender buildRecommender(DataModel model);

	protected abstract UserSimilarity getSimilarityMeasure(DataModel model);

	protected UserNeighborhood getKNNNeighborhood(UserSimilarity similarity,
			DataModel model) {

	long thTime = 0;
	long KNNTime = 0;

		UserNeighborhood neighborhood = null;
		try {

			if (isPPNS_) {
				neighborhood = new PPNSUserNeighborhood(k_, similarity, model, beta_);
				logger.debug("PPNSUserNeighborhood creation suceeded");
			} else {
				neighborhood = new NearestNUserNeighborhood(k_, similarity, model);
				logger.debug("NearestNUserNeighborhood creation suceeded");
			}

		} catch (TasteException e) {
			e.printStackTrace();
			logger.debug("NearestNUserNeighborhood creation failed w/ TasteException");
		}
		logger.debug("neighborhood is null: {}", neighborhood == null);

		// if (similarity instanceof TwoStepUncenteredCosineSimilarity){
		// 	//compute distribution
		// 	logger.info("Computing similarity distribution");
		// 	long start = System.nanoTime();
		// 	buildSimilarityDistribution(neighborhood, model);
		// 	long end = System.nanoTime();
		//  thTime = end - start;
		// 	System.out.format("Threshold computations takes %d ns%n", end - start);
		// 	logger.info("done with similarity distribution");
		// }

		if (logger.isDebugEnabled() || true) {
			long start = System.nanoTime();
			testNeighborhoods(neighborhood, model);
			long end = System.nanoTime();
			KNNTime = end - start;
			System.out.format("KNN computations takes %d ns%n", end - start);
		}

		runTimeLogger.info("{},{}", thTime, KNNTime);
		return neighborhood;
	}

	protected abstract UserBasedRecommender getRecommender(DataModel model,
			UserNeighborhood neighborhood, UserSimilarity similarity);

	/**
	 * Return the UserNeighborhood computed during the last call to
	 * buildRecommender.
	 */
	public UserNeighborhood getNeighborhood() {
		return neighborhood;
	}

	/**
	 * Debug function testing how many users have no neighbors in neighborhood.
	 * DEPRECATED METHOD.
	 */
	public static void testNeighborhoods(UserNeighborhood neighborhood,
			DataModel model) {
		int nonNullNeighborhoods = 0;
		LongPrimitiveIterator it = null;
		int nb = -1;
		try {
			it = model.getUserIDs();
			nb = model.getNumUsers();
		} catch (TasteException e) {
			e.printStackTrace();
		}
		while (it.hasNext()) {
			long[] neighbors = null;
			long user = it.nextLong();
			try {
				logger.trace("neighborhood is null: {}", neighborhood == null);
				// TODO: use Parameters.randomChoiceForEquallySimilarNeighors_
				neighbors = neighborhood.getUserNeighborhood(user, "random", new Random());
			} catch (TasteException e) {
				e.printStackTrace();
			}
			if (neighbors != null) {
				nonNullNeighborhoods++;
			}
		}
		logger.debug("{} non-null neighborhoods among {} users",
				nonNullNeighborhoods, nb);
	}

	/**
	 * Compute:
	 * - the average profile size among all users.
	 * - for each user, the similarity distribution of all other users.
	 * - for each user, the threshold similarity above which twostep adds a richness bonus.
	 * Users refers to users in model.
	 */
	public static void buildSimilarityDistribution(
			UserNeighborhood neighborhood, DataModel model) {
		logger.debug("build Similarity Distribution called {} time", ++numCalled);
		if (neighborhood instanceof NearestNUserNeighborhood) {
			NearestNUserNeighborhood nneighborhood = (NearestNUserNeighborhood) neighborhood;
			UserSimilarity sim = nneighborhood.getUserSimilarity();
			if (sim instanceof TwoStepUncenteredCosineSimilarity) {
				TwoStepUncenteredCosineSimilarity twoStep = (TwoStepUncenteredCosineSimilarity) sim;
				double percentile = twoStep.getPercentileThreshold();
				boolean perUserDistro=twoStep.isPerUserDistro();
				LongPrimitiveIterator it = ExceptHandler.getModelUserIDs(model);
				double avgProfileSize=0;
				int numUsers=0;
				long similarityComputations = 0;
				long sortComputations = 0;
				long start = 0;

				// try {
				// 	int nbAboveThres = (int) (ExceptHandler.getModelNumUsers(model) * (1 - percentile));
				// 	NearestNUserNeighborhood thresNeighborhood = new NearestNUserNeighborhood(nbAboveThres, 0.0, twoStep, model, 1.0);
				// 	while (it.hasNext()) {
				// 		long user = it.nextLong();
				// 		double threshold = 1.0;
				// 		start = System.nanoTime();
				// 		if (perUserDistro) {
				// 			long[] userAboveThresNeighborhood = thresNeighborhood.getUserNeighborhood(user);
				// 			for (long neighbor : userAboveThresNeighborhood) {
				// 				double similarity = twoStep.userSimilarity(user, neighbor);
				// 				if (similarity < threshold) {
				// 					threshold = similarity;
				// 				}
				// 			}
				// 			twoStep.setSimilarityThresholdForUser(user, threshold);
				// 			logger.debug("User {}: threshold={}", user, threshold);
				// 		}
				// 	}
				// } catch (TasteException e) {
				// 	e.printStackTrace();
				// }

				while (it.hasNext()) {
					long user = it.nextLong();
					HashSet<Double> simValues = null;
					try {
						simValues = nneighborhood
								.getSimilarityDistributionForUser(user);
					} catch (TasteException e) {
						e.printStackTrace();
					}
					avgProfileSize+=ExceptHandler.getPreferences(model, user).length();
					numUsers++;
					if (perUserDistro) {
						ArrayList<Double> sortedValues = new ArrayList<Double>(simValues);
						Collections.sort(sortedValues);
						int maxPos = sortedValues.size() - 1;
						int position = (int) Math.round(maxPos * percentile);
						double threshold = sortedValues.get(position);
						logger.debug("User {}: {} percentile is at position {} among {} in similarity array, threshold={}", user, percentile, position, maxPos, threshold);
						twoStep.setSimilarityThresholdForUser(user,
								threshold);
					} else {
						similarityDistro.addAll(simValues);
					}
				}

				if (!perUserDistro) {
					ArrayList<Double> sortedValues = new ArrayList<Double>(
							similarityDistro);
					Collections.sort(sortedValues);
					int maxPos = sortedValues.size() - 1;
					int position = (int) Math.round(maxPos * percentile);
					double threshold = sortedValues.get(position);
					logger.debug("returning {} for all pos={} of {} simValue: {}", percentile, position, maxPos, threshold);
					twoStep.setSimilarityThresholdForAllUser(threshold);
				}
				avgProfileSize/=numUsers;
				twoStep.setAvgProfileSize(avgProfileSize);
			}
		}

	}
}
