/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.cf.taste.impl.neighborhood;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Random;

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
 * Computes a neighborhood consisting of the nearest n users to a given user.
 * "Nearest" is defined by the given {@link UserSimilarity}.
 * </p>
 */
public final class NearestNUserNeighborhood extends AbstractUserNeighborhood {

	private final int n;
	private final double minSimilarity;
	private HashSet<Double> similarityDistro = new HashSet<Double>();
	private boolean perUserDistro;
	private HashMap<Long, Double> hashMap;

	/**
	 * @param n
	 *            neighborhood size; capped at the number of users in the data
	 *            model
	 * @throws IllegalArgumentException
	 *             if {@code n < 1}, or userSimilarity or dataModel are
	 *             {@code null}
	 */
	public NearestNUserNeighborhood(int n, UserSimilarity userSimilarity,
			DataModel dataModel) throws TasteException {
		this(n, Double.NEGATIVE_INFINITY, userSimilarity, dataModel, 1.0);
		int nbUsers = dataModel.getNumUsers();
		this.hashMap = new HashMap<Long, Double>(nbUsers);
	}

	/**
	 * @param n
	 *            neighborhood size; capped at the number of users in the data
	 *            model
	 * @param minSimilarity
	 *            minimal similarity required for neighbors
	 * @throws IllegalArgumentException
	 *             if {@code n < 1}, or userSimilarity or dataModel are
	 *             {@code null}
	 */
	public NearestNUserNeighborhood(int n, double minSimilarity,
			UserSimilarity userSimilarity, DataModel dataModel)
			throws TasteException {
		this(n, minSimilarity, userSimilarity, dataModel, 1.0);
		int nbUsers = dataModel.getNumUsers();
		this.hashMap = new HashMap<Long, Double>(nbUsers);
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
	 * @throws IllegalArgumentException
	 *             if {@code n < 1} or samplingRate is NaN or not in (0,1], or
	 *             userSimilarity or dataModel are {@code null}
	 */
	public NearestNUserNeighborhood(int n, double minSimilarity,
			UserSimilarity userSimilarity, DataModel dataModel,
			double samplingRate) throws TasteException {
		super(userSimilarity, dataModel, samplingRate);
		Preconditions.checkArgument(n >= 1, "n must be at least 1");
		int numUsers = dataModel.getNumUsers();
		this.n = n > numUsers ? numUsers : n;
		this.minSimilarity = minSimilarity;
		int nbUsers = dataModel.getNumUsers();
		this.hashMap = new HashMap<Long, Double>(nbUsers);
	}

	@Override
	public long[] getUserNeighborhood(long userID) throws TasteException {
		DataModel dataModel = getDataModel();
		UserSimilarity userSimilarityImpl = getUserSimilarity();

		TopItems.Estimator<Long> estimator = new Estimator(userSimilarityImpl,
				userID, minSimilarity);

		LongPrimitiveIterator userIDs = SamplingLongPrimitiveIterator
				.maybeWrapIterator(dataModel.getUserIDs(), getSamplingRate());
		return TopItems.getTopUsers(n, userIDs, null, estimator);
	}


	/**
	 * Compute the set of (standard) cosine similarity values between userID and all the other users.
	 * Assumptions: this method is called only within recopriv's KNNRecommenderBuilder.buildSimilarityDistribution(), and only once per userID.
	 */
	public HashSet<Double> getSimilarityDistributionForUser(long userID)
			throws TasteException {
		DataModel dataModel = getDataModel();

		UserSimilarity userSimilarityImpl = getUserSimilarity();
		TopItems.Estimator<Long> estimator = new Estimator(userSimilarityImpl,
				userID, minSimilarity);

		LongPrimitiveIterator userIDs = SamplingLongPrimitiveIterator
				.maybeWrapIterator(dataModel.getUserIDs(), getSamplingRate());

		if (userSimilarityImpl instanceof TwoStepUncenteredCosineSimilarity) {
			// This should be written better but it would require adding stuff
			// to the estimator / similarity parts.
			TwoStepUncenteredCosineSimilarity twoStep = (TwoStepUncenteredCosineSimilarity) userSimilarityImpl;
			if (!twoStep.hasSimilarityThresholdForUser(userID)) {
				double percentile = twoStep.getPercentileThreshold();
//				HashSet<Double> simValues = TopItems.computeSimilarityValues(
//						userIDs, estimator, twoStep.getPercentileThreshold());

				HashSet<Double> simValues = new HashSet<Double>();
				while (userIDs.hasNext()) {
					long otherUserID = userIDs.next();

					double similarity;
					try {
						similarity = userSimilarityImpl.userSimilarity(userID, otherUserID);
						similarity *=100;
						similarity=Math.round(similarity);
						similarity /=100;
						simValues.add(similarity);
					} catch (NoSuchUserException nsue) {
						System.err.println("No such user "+otherUserID);
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
	public long[] getUserNeighborhood(long userID, String choiceBehavior,
			Random rand) throws TasteException {
		if (choiceBehavior.equals("lower")) {
			return getUserNeighborhood(userID);
		}

		DataModel dataModel = getDataModel();
		UserSimilarity userSimilarityImpl = getUserSimilarity();

		TopItems.Estimator<Long> estimator = new Estimator(userSimilarityImpl,
				userID, minSimilarity);

		LongPrimitiveIterator userIDs = SamplingLongPrimitiveIterator
				.maybeWrapIterator(dataModel.getUserIDs(), getSamplingRate());
//		System.out.println("[DAVIDE] in NearestNUserNeigh for uid="+userID+" with similarity "+userSimilarityImpl.getClass().getCanonicalName()+" before if that will call getTopUsersRandom with obtained iterator samplingoriginal userIds "+dataModel.getNumUsers()+" and sampling rate "+getSamplingRate());
		if (choiceBehavior.equals("higher")) {
			return TopItems.getTopUsersHigher(n, userIDs, null, estimator);
		} else if (choiceBehavior.equals("random")) {
			if (userSimilarityImpl instanceof TwoStepUncenteredCosineSimilarity) {
				TwoStepUncenteredCosineSimilarity twoStep = (TwoStepUncenteredCosineSimilarity) userSimilarityImpl;
				if (!twoStep.hasSimilarityThresholdForUser(userID) && twoStep.isPerUserDistro()) {
					hashMap.clear();
					//return TopItems.getTopUsersRandom(n, userIDs, null, estimator, rand);
					
					return TopItems.getTwoStepTopUsersRandom(n, userIDs, null, estimator, twoStep, rand, userID, hashMap);
				} else {
					return TopItems.getTopUsersRandom(n, userIDs, null, estimator, rand);
				}
			} else {
				return TopItems.getTopUsersRandom(n, userIDs, null, estimator, rand);
			}
		} else {
			System.out
					.println("WARNING: choiceBehavior string has an incorrect value: "
							+ choiceBehavior);
			return null;
		}
	}

	@Override
	public String toString() {
		return "NearestNUserNeighborhood";
	}

	/**
	 * A wrapper around a UserSimilarity.userSimilarity() where the first user is fixed.
	 * The main method is estimate(userID), which is equivalent to userSimilarity(fixedUser, userID).
	 */
	private static final class Estimator implements TopItems.Estimator<Long> {
		private final UserSimilarity userSimilarityImpl;
		private final long theUserID;
		private final double minSim;

		private Estimator(UserSimilarity userSimilarityImpl, long theUserID,
				double minSim) {
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
			return "EST: " + theUserID + " syb: "
					+ (theUserID < TopItems.firstSybilID ? "no" : "yes");
		}
	}
}
