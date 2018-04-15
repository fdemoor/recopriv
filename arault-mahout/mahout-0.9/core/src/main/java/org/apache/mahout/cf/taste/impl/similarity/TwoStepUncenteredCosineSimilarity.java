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

package org.apache.mahout.cf.taste.impl.similarity;

import java.util.HashMap;
import java.util.Random;

import org.apache.commons.collections.map.HashedMap;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.common.Weighting;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.model.Preference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * <p>
 * An implementation of the cosine similarity. The result is the cosine of the
 * angle formed between the two preference vectors.
 * </p>
 *
 * <p>
 * Note that this similarity does not "center" its data, shifts the user's
 * preference values so that each of their means is 0. For this behavior, use
 * {@link PearsonCorrelationSimilarity}, which actually is mathematically
 * equivalent for centered data.
 * </p>
 */
public final class TwoStepUncenteredCosineSimilarity extends AbstractSimilarity {

  	private static final Logger log = LoggerFactory.getLogger(TwoStepUncenteredCosineSimilarity.class);

	private HashMap<Long, Double> similarityThresholds = new HashMap<Long, Double>();
	private double percentileThreshold;
	private double globalSimilarityThreshold = -1;
	private boolean perUserDistro;
	private double avgProfileSize;
	private boolean useRichness = true;
	private boolean useThreshold = true;
	private int idealNbItems = 400;
	private boolean isGlobalIdealNbItems = true;
	private double percentIdealNbItems = 1.5;
	private boolean randomizeIdealNbItems = false;
	private String variantIdealNbItems = "u";
	private Random rand;


	/**
	 * @throws IllegalArgumentException
	 *             if {@link DataModel} does not have preference values
	 */
	public TwoStepUncenteredCosineSimilarity(DataModel dataModel)
			throws TasteException {
		this(dataModel, Weighting.UNWEIGHTED);
	}

	/**
	 * @throws IllegalArgumentException
	 *             if {@link DataModel} does not have preference values
	 */
	public TwoStepUncenteredCosineSimilarity(DataModel dataModel,
			Weighting weighting) throws TasteException {
		super(dataModel, weighting, false);
		Preconditions.checkArgument(dataModel.hasPreferenceValues(),
				"DataModel doesn't have preference values");
	}

	public boolean isUseRichness() {
		return useRichness;
	}

	public void setUseRichness(boolean useRichness) {
		this.useRichness = useRichness;
	}

	public void setIdealNbItems(int nb) {
		idealNbItems = nb;
	}

	public void setPercentileThreshold(double percentileThreshold) {
		this.percentileThreshold = percentileThreshold;
	}

	@Override
	public double userSimilarity(long userID1, long userID2)
			throws TasteException {
		log.trace("computing similarity between {} and {}", userID1, userID2);
		DataModel dataModel = getDataModel();
		PreferenceArray xPrefs = dataModel.getPreferencesFromUser(userID1);
		PreferenceArray yPrefs = dataModel.getPreferencesFromUser(userID2);
		itemsInXNotInY = 0;
		itemsInYNotInX = 0;
		totScoresInXNotInY = 0;
		totScoresInYNotInX = 0;
		totScoresInX = 0;
		totScoresInY = 0;
		totX2NotInY = 0;
		totY2NotInX = 0;
		itemsInX = 0;
		itemsInY = 0;
		int xLength = xPrefs.length();
		int yLength = yPrefs.length();

		if (xLength == 0 || yLength == 0) {
			return Double.NaN;
		}
		// Y is the neighbor. X is the current node
		long xIndex = xPrefs.getItemID(0);
		long yIndex = yPrefs.getItemID(0);
		int xPrefIndex = 0;
		int yPrefIndex = 0;

		double sumX = 0.0;
		double sumX2 = 0.0;
		double sumY = 0.0;
		double sumY2 = 0.0;
		double sumXY = 0.0;
		int count = 0;

		boolean hasInferrer = (inferrer != null)
				&& !(enableXInferrer ^ enableYInferrer);
		boolean hasXInferrer = (inferrer != null) && enableXInferrer;
		boolean hasYInferrer = (inferrer != null) && enableYInferrer;
		/*
		 * For WUP similarity: to project X onto Y: - look at all the items of Y
		 * even those that are not in X - and only those of X that appear in Y
		 * so we must use the X inferrer, but no Y inferrer
		 *
		 * to project Y onto X: - look at all the items of X even those that are
		 * not in Y - and only those in Y that appear in X so we must use the Y
		 * inferrer, but no X inferrer
		 */
		while (true) {
			// compare is -1 if x has an item that y doesn't have; it is 1 if y has an item that x does not have.
			int compare = xIndex < yIndex ? -1 : xIndex > yIndex ? 1 : 0;
			if (hasInferrer || compare == 0 || hasYInferrer && compare < 0
					|| hasXInferrer && compare > 0) {
				double x;
				double y;
				if (xIndex == yIndex) {
					// Both users expressed a preference for the item
					x = xPrefs.getValue(xPrefIndex);
					y = yPrefs.getValue(yPrefIndex);

				} else {
					// Only one user expressed a preference, but infer the other
					// one's preference and tally
					// as if the other user expressed that preference
					if (compare < 0) {
						// X has a value; infer Y's
						x = xPrefs.getValue(xPrefIndex);
						y = inferrer.inferPreference(userID2, xIndex);

					} else {
						// compare > 0
						// Y has a value; infer X's
						x = inferrer.inferPreference(userID1, yIndex);
						y = yPrefs.getValue(yPrefIndex);

					}
				}
				sumXY += x * y;
				sumX += x;
				sumX2 += x * x;
				sumY += y;
				sumY2 += y * y;
				count++;
			}
			if (compare <= 0) {// X has a value
				double x = xPrefs.getValue(xPrefIndex);
				itemsInX++;
				totScoresInX += x;
				if (compare < 0) {
					totX2NotInY += x * x;
					itemsInXNotInY++;
					totScoresInXNotInY += x;
				}
				if (++xPrefIndex >= xLength) {
					if (hasInferrer || hasXInferrer) {// We use the inferrer for X to count the remaining Ys
						// Must count other Ys; pretend next X is far away
						if (yIndex == Long.MAX_VALUE) {
							// ... but stop if both are done!
							break;
						}
						xIndex = Long.MAX_VALUE; // This will yield compare = 1 and thus use the xInferrer
					} else {
						break;
					}
				} else {
					xIndex = xPrefs.getItemID(xPrefIndex);
				}
			}
			if (compare >= 0) { // Y has a value
				double y = yPrefs.getValue(yPrefIndex);
				itemsInY++;
				totScoresInY += y;
				if (compare > 0) {
					totY2NotInX += y * y;
					itemsInYNotInX++;
					totScoresInYNotInX += y;
				}
				if (++yPrefIndex >= yLength) {
					if (hasInferrer || hasYInferrer) {// We use the inferrer for Y to count the remaining Xs
						// Must count other Xs; pretend next Y is far away
						if (xIndex == Long.MAX_VALUE) {
							// ... but stop if both are done!
							break;
						}
						yIndex = Long.MAX_VALUE; // This will yield compare =-1 and thus use the yInferrer
					} else {
						break;
					}
				} else {
					yIndex = yPrefs.getItemID(yPrefIndex);
				}
			}
		}

		double result = computeResult(count, sumXY, sumX2, sumY2, userID1, userID2);

		if (!Double.isNaN(result)) {
			result = normalizeWeightResult(result, count, cachedNumItems);
		}
		return result;

	}

	/**
	 * Compute the cosine similarity of currentUser and neighbor using intermediary values
	 * computed by userSimilarity(), which is either returned as is if below a threshold,
	 * or replaced by the threshold similarity value + an optional bonus computed by computeRichnessMeasure().
	 */
	private double computeResult(int n, double sumXY, double sumX2, double sumY2,
			long currentUser, long neighbor) {
		if (n == 0) {
			return Double.NaN;
		}
		double denominator = Math.sqrt(sumX2) * Math.sqrt(sumY2);
		if (denominator == 0.0) {
			// One or both parties has -all- the same ratings;
			// can't really say much similarity under this measure
			return Double.NaN;
		}
		double result = sumXY / denominator; // Cosine similarity
		if (currentUser < 0) {
			log.error("Something's wrong. currentUser has an unexpected value: {}", currentUser);
			throw new IllegalArgumentException();
		}

		Double resultThreshold = getSimilarityThresholdForUser(currentUser);
		log.trace("using threshold {} for user {}", resultThreshold, currentUser);
		if (resultThreshold >= 0 && result >= resultThreshold) {
			if (useThreshold) {
				result = resultThreshold
					+ computeRichnessMeasure(resultThreshold, currentUser,
							neighbor);
			} else {
				result = result
						+ computeRichnessMeasure(resultThreshold, currentUser,
								neighbor);
			}
		} else {
			log.trace("not doing richness because th={} and res={}", resultThreshold, result);
		}
		log.trace("two-step returning {}", result);

		// Rounding the final similarity value to 1 if it is really close to 1.
		if ((1.0 - result) <= 0.00000000000001) {
			result = 1.0;
		}
		log.trace("sim ({}, {})={}", currentUser, neighbor, result);

		return result;

	}

	public double getSimilarityThresholdForUser(long currentUser) {
		if (!perUserDistro) {
			return globalSimilarityThreshold;
		} else {
			double toRet;
		       	if(similarityThresholds.containsKey(currentUser)) {
				toRet = similarityThresholds.get(currentUser);
			} else {
				toRet = -1.0;
			}
			return toRet;
		}
	}

	private double computeRichnessMeasure(double resultThreshold,
			long currentUser, long neighbor) {

		if (useRichness) {
			double toRet;
			double maxRichness = 1 - resultThreshold; // max similarity should be 1.

			int userIdealNbItems = idealNbItems;
			if (!isGlobalIdealNbItems) {
				try {
					DataModel dataModel = getDataModel();
					PreferenceArray userPrefs = null;
          if (variantIdealNbItems.contains("n")) {
            userPrefs = dataModel.getPreferencesFromUser(neighbor);
          } else { // Default : "u"
            userPrefs = dataModel.getPreferencesFromUser(currentUser);
          }
					int nbUserItems = userPrefs.length();
					double weight = percentIdealNbItems;
					if (randomizeIdealNbItems) {
						double randomDouble = rand.nextDouble();
						weight = weight + randomDouble - 0.5;
					}
					userIdealNbItems = (int) (nbUserItems * weight);
				} catch (TasteException ex) {}
			}

			if (itemsInYNotInX == userIdealNbItems) {
				toRet = maxRichness;
			} else if (itemsInYNotInX < userIdealNbItems) {
				toRet = maxRichness * itemsInYNotInX / userIdealNbItems;
			} else { // (itemsInYNotInX > idealNbItems)
				toRet = maxRichness * (2 * userIdealNbItems - itemsInYNotInX)
						/ userIdealNbItems;
			}
			if (toRet < 0) {
				toRet = 0;
			}

			log.trace("sim ({}, {}): computed richness={} for threshold={}, maxRichness={}, idealNbItems={}, itemsInYNotInX={}",
				       	currentUser, neighbor, toRet, resultThreshold, maxRichness, userIdealNbItems , itemsInYNotInX);
			return toRet;
		} else {
			return 0;
		}
	}

	public boolean hasSimilarityThresholdForUser(long userID) {
		if (perUserDistro) {
			return similarityThresholds.containsKey(userID);
		} else {
			return globalSimilarityThreshold >= 0;
		}
	}

	public void setSimilarityThresholdForUser(long userID, double threshold) {
		similarityThresholds.put(userID, threshold);
	}

	/**
	 * @return percentile threshold in [0,1]
	 */
	public double getPercentileThreshold() {
		return percentileThreshold;
	}

	/**
	 * Not used but pseudo-implementation required by parent class AbstractSimilarity.
	 */
	@Override
	protected double computeResult(int n, double sumXY, double sumX2, double sumY2,
			double sumXYdiff2) {
		log.error("Not supported! Use computeResult(int,double,double,double,double,long,long) instead");
		throw new UnsupportedOperationException();
	}

	public void setSimilarityThresholdForAllUser(double threshold) {
		globalSimilarityThreshold = threshold;
	}

	public void setUseThreshold(boolean useTh) {
		useThreshold = useTh;
	}

	public void setIsGlobalIdealNbItems(boolean isGlobal) {
		isGlobalIdealNbItems = isGlobal;
	}

	public void setPercentIdealNbItems(double per) {
		percentIdealNbItems = per;
	}

	public void setRandomizeIdealNbItems(boolean randomize) {
		randomizeIdealNbItems = randomize;
	}

	public void setVariantIdealNbItems(String s) {
		variantIdealNbItems = s;
	}

	public void setRand(Random r) {
		rand = r;
	}

	public boolean isPerUserDistro() {
		return perUserDistro;
	}

	public void setPerUserDistro(boolean perUserDistro) {
		this.perUserDistro = perUserDistro;
	}

	public void setAvgProfileSize(double avgProfileSize) {
		this.avgProfileSize = avgProfileSize;
		log.debug("set avgProfileSize={}", avgProfileSize);
	}

	public double computeSecondStep(double resultThreshold, long xUser, long yUser) {
		log.debug("Computing second step with threshold={} bewteen user {} and potential neighbor {}", resultThreshold, xUser, yUser);
		DataModel model = getDataModel();
		int itemsInYNotInX = 0;
		try {
			PreferenceArray yPrefs = model.getPreferencesFromUser(yUser);
			PreferenceArray xPrefs = model.getPreferencesFromUser(xUser);
			for (Preference pref : yPrefs) {
				if (!xPrefs.hasPrefWithItemID(pref.getItemID())) {
					itemsInYNotInX += 1;
				}
			}
		} catch (TasteException ex) {}

		if (useRichness) {
			double toRet;
			double maxRichness = 1 - resultThreshold;

			int userIdealNbItems = idealNbItems;
			if (!isGlobalIdealNbItems) {
				try {
					PreferenceArray xPrefs = model.getPreferencesFromUser(xUser);
					int nbUserItems = xPrefs.length();
					double weight = percentIdealNbItems;
					if (randomizeIdealNbItems) {
						double randomDouble = rand.nextDouble();
						weight = weight + randomDouble - 0.5;
					}
					userIdealNbItems = (int) (nbUserItems * weight);
				} catch (TasteException ex) {}
			}

			if (itemsInYNotInX == userIdealNbItems) {
				toRet = maxRichness;
			} else if (itemsInYNotInX < userIdealNbItems) {
				toRet = maxRichness * itemsInYNotInX / userIdealNbItems;
			} else { // (itemsInYNotInX > idealNbItems)
				toRet = maxRichness * (2 * userIdealNbItems - itemsInYNotInX) / userIdealNbItems;
			}

			if (toRet < 0) {
				toRet = 0;
			}

			return toRet;

		} else {
			return 0;
		}
	}

}
