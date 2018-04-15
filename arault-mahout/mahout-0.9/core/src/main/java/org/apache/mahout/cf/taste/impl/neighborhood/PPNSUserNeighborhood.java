package org.apache.mahout.cf.taste.impl.neighborhood;

import java.lang.Math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Random;

import org.apache.mahout.cf.taste.impl.neighborhood.AbstractUserNeighborhood;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.common.SamplingLongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.recommender.TopItems;
import org.apache.mahout.cf.taste.impl.similarity.TwoStepUncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import com.google.common.base.Preconditions;

/**
 * <p>
 * Computes a neighborhood according to PPNS method: "A Security-assured
 * Accuracy-maximised Privacy Preserving Collaborative Filtering Recommendation
 * Algorithm" Zhigang Lu, Hong Shen
 * </p>
 */
public final class PPNSUserNeighborhood extends AbstractUserNeighborhood {

	private final int n;
	private final double minSimilarity;
	private HashSet<Double> similarityDistro = new HashSet<Double>();
	private boolean perUserDistro;
	private HashMap<Long, Double> hashMap;
	private int beta;
	private int k;

	/**
	 * @param n
	 *            neighborhood size; capped at the number of users in the data
	 *            model
	 * @param beta
	 *            security metric in PPNS method
	 * @throws IllegalArgumentException
	 *             if {@code n < 1}, or userSimilarity or dataModel are
	 *             {@code null}
	 */
	public PPNSUserNeighborhood(int n, UserSimilarity userSimilarity, DataModel dataModel, int beta)
			throws TasteException {
		this(n, Double.NEGATIVE_INFINITY, userSimilarity, dataModel, 1.0, beta);
		int nbUsers = dataModel.getNumUsers();
		this.hashMap = new HashMap<Long, Double>(nbUsers);
		this.beta = beta;
	}

	/**
	 * @param n
	 *            neighborhood size; capped at the number of users in the data
	 *            model
	 * @param minSimilarity
	 *            minimal similarity required for neighbors
	 * @param beta
	 *            security metric in PPNS method
	 * @throws IllegalArgumentException
	 *             if {@code n < 1}, or userSimilarity or dataModel are
	 *             {@code null}
	 */
	public PPNSUserNeighborhood(int n, double minSimilarity, UserSimilarity userSimilarity, DataModel dataModel,
			int beta) throws TasteException {
		this(n, minSimilarity, userSimilarity, dataModel, 1.0, beta);
		int nbUsers = dataModel.getNumUsers();
		this.hashMap = new HashMap<Long, Double>(nbUsers);
		this.beta = beta;
	}

	/**
	 * @param n
	 *            neighborhood size; capped at the number of users in the data
	 *            model
	 * @param minSimilarity
	 *            minimal similarity required for neighbors
	 * @param samplingRate
	 *            percentage of users to consider when building neighborhood --
	 *            decrease to trade quality for performance
	 * @param beta
	 *            security metric in PPNS method
	 * @throws IllegalArgumentException
	 *             if {@code n < 1} or samplingRate is NaN or not in (0,1], or
	 *             userSimilarity or dataModel are {@code null}
	 */
	public PPNSUserNeighborhood(int n, double minSimilarity, UserSimilarity userSimilarity, DataModel dataModel,
			double samplingRate, int beta) throws TasteException {
		super(userSimilarity, dataModel, samplingRate);
		Preconditions.checkArgument(n >= 1, "n must be at least 1");
		int numUsers = dataModel.getNumUsers();
		this.n = n;
		this.k = (beta * n) > numUsers ? numUsers : (beta * n);
		this.minSimilarity = minSimilarity;
		int nbUsers = dataModel.getNumUsers();
		this.hashMap = new HashMap<Long, Double>(nbUsers);
		this.beta = beta;
	}

	@Override
	public long[] getUserNeighborhood(long userID) throws TasteException {
		DataModel dataModel = getDataModel();
		UserSimilarity userSimilarityImpl = getUserSimilarity();

		TopItems.Estimator<Long> estimator = new Estimator(userSimilarityImpl, userID, minSimilarity);

		LongPrimitiveIterator userIDs = SamplingLongPrimitiveIterator.maybeWrapIterator(dataModel.getUserIDs(),
				getSamplingRate());

    long[] neighbors = TopItems.getTopUsers(k, userIDs, null, estimator);
    Random rand = new Random();

		return PPNS(userID, neighbors, rand);
	}

	/**
	 * Compute the set of (standard) cosine similarity values between userID and
	 * all the other users. Assumptions: this method is called only within
	 * recopriv's KNNRecommenderBuilder.buildSimilarityDistribution(), and only
	 * once per userID.
	 */
	public HashSet<Double> getSimilarityDistributionForUser(long userID) throws TasteException {
		DataModel dataModel = getDataModel();

		UserSimilarity userSimilarityImpl = getUserSimilarity();
		TopItems.Estimator<Long> estimator = new Estimator(userSimilarityImpl, userID, minSimilarity);

		LongPrimitiveIterator userIDs = SamplingLongPrimitiveIterator.maybeWrapIterator(dataModel.getUserIDs(),
				getSamplingRate());

		if (userSimilarityImpl instanceof TwoStepUncenteredCosineSimilarity) {
			// This should be written better but it would require adding stuff
			// to the estimator / similarity parts.
			TwoStepUncenteredCosineSimilarity twoStep = (TwoStepUncenteredCosineSimilarity) userSimilarityImpl;
			if (!twoStep.hasSimilarityThresholdForUser(userID)) {
				double percentile = twoStep.getPercentileThreshold();
				// HashSet<Double> simValues = TopItems.computeSimilarityValues(
				// userIDs, estimator, twoStep.getPercentileThreshold());

				HashSet<Double> simValues = new HashSet<Double>();
				while (userIDs.hasNext()) {
					long otherUserID = userIDs.next();

					double similarity;
					try {
						similarity = userSimilarityImpl.userSimilarity(userID, otherUserID);
						similarity *= 100;
						similarity = Math.round(similarity);
						similarity /= 100;
						simValues.add(similarity);
					} catch (NoSuchUserException nsue) {
						System.err.println("No such user " + otherUserID);
						continue;
					}
				}

				return simValues;

			} else {
				System.err.println("returning already computed similarity distribution. this should never happen");
				System.exit(-1);
				return null;
			}
		} else {
			System.err.println("This should never happen. Distribution without two step");
			System.exit(-1);
			return null;
		}

	}

	@Override
	public long[] getUserNeighborhood(long userID, String choiceBehavior, Random rand) throws TasteException {
		if (choiceBehavior.equals("lower")) {
			return getUserNeighborhood(userID);
		}

		DataModel dataModel = getDataModel();
		UserSimilarity userSimilarityImpl = getUserSimilarity();

		TopItems.Estimator<Long> estimator = new Estimator(userSimilarityImpl, userID, minSimilarity);

		LongPrimitiveIterator userIDs = SamplingLongPrimitiveIterator.maybeWrapIterator(dataModel.getUserIDs(),
				getSamplingRate());

		long[] neighbors = null;
//		System.out.println(
//				"[DAVIDE] in PPNS before if that will call getTopUsersRandom with obtained iterator samplingoriginal userIds "
//						+ dataModel.getNumUsers() + " and sampling rate " + getSamplingRate());

		if (choiceBehavior.equals("higher")) {
			neighbors = TopItems.getTopUsersHigher(k, userIDs, null, estimator);
		} else if (choiceBehavior.equals("random")) {
			neighbors = TopItems.getTopUsersRandom(k, userIDs, null, estimator, rand);
		} else {
			System.out.println("WARNING: choiceBehavior string has an incorrect value: " + choiceBehavior);
			return null;
		}

    return PPNS(userID, neighbors, rand);
	}

  private long[] PPNS(long userID, long[] candidates, Random rand) {
    
    DataModel dataModel = getDataModel();
    UserSimilarity userSimilarityImpl = getUserSimilarity();

		long[] neighbors = new long[n];

		for (int i = 0; i < n - 1; i++) {
			neighbors[i] = candidates[i];
		}
    
    double[] cumulativeSimilarities = new double[n];
    /* Create cumulative similarities vector */
    try {
      for (int i = 0; i < n; i++) {
        cumulativeSimilarities[i] = userSimilarityImpl.userSimilarity(userID, candidates[(beta - 1) * n + i]);
        if (i != 0) {
          cumulativeSimilarities[i] += cumulativeSimilarities[i - 1];
        }
      }
    } catch (TasteException ex) {}
    /* Take exponential */
    for (int i = 0; i < n; i++) {
      cumulativeSimilarities[i] = Math.exp(cumulativeSimilarities[i]);
    }
    /* Normalize to have vector ending with 1 */
    for (int i = 0; i < n; i++) {
      cumulativeSimilarities[i] /= cumulativeSimilarities[n-1];
    }
    double choice = rand.nextDouble();
    int j = 0;
    while (cumulativeSimilarities[j] < choice) {
      j++;
    }
    
		neighbors[n-1] = candidates[(beta - 1) * n + j];

		return neighbors;
	}

	@Override
	public String toString() {
		return "PPNSUserNeighborhood";
	}

	/**
	 * A wrapper around a UserSimilarity.userSimilarity() where the first user
	 * is fixed. The main method is estimate(userID), which is equivalent to
	 * userSimilarity(fixedUser, userID).
	 */
	private static final class Estimator implements TopItems.Estimator<Long> {
		private final UserSimilarity userSimilarityImpl;
		private final long theUserID;
		private final double minSim;

		private Estimator(UserSimilarity userSimilarityImpl, long theUserID, double minSim) {
			this.userSimilarityImpl = userSimilarityImpl;
			this.theUserID = theUserID;
			this.minSim = minSim;
		}

		@Override
		public double estimate(Long userID) throws TasteException {
			if (userID == theUserID) {
				return Double.NaN;
			}
			double sim = userSimilarityImpl.userSimilarity(theUserID, userID);
			return sim >= minSim ? sim : Double.NaN;
		}

		public String toString() {
			return "EST: " + theUserID + " syb: " + (theUserID < TopItems.firstSybilID ? "no" : "yes");
		}
	}
}
