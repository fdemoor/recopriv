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

import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.common.Weighting;
import org.apache.mahout.cf.taste.impl.common.RefreshHelper;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.similarity.PreferenceInferrer;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import com.google.common.base.Preconditions;

/**
 * Abstract superclass encapsulating functionality that is common to most
 * implementations in this package.
 */
public abstract class AbstractSimilarity extends AbstractItemSimilarity
		implements UserSimilarity {

	protected PreferenceInferrer inferrer;
	protected boolean enableXInferrer = true;
	protected boolean enableYInferrer = true;
	private final boolean weighted;
	protected final boolean centerData;
	protected int cachedNumItems;
	private int cachedNumUsers;
	private final RefreshHelper refreshHelper;
	// the following code is super dirty, but it does the trick
	protected int itemsInXNotInY = 0;
	protected int itemsInYNotInX = 0;
	protected int itemsInX = 0;
	protected int itemsInY = 0;
	protected double totScoresInX = 0;
	protected double totScoresInY = 0;
	protected double totX2NotInY = 0;
	protected double totY2NotInX = 0;
	protected double totScoresInXNotInY = 0;
	protected double totScoresInYNotInX = 0;

	/**
	 * <p>
	 * Creates a possibly weighted {@link AbstractSimilarity}.
	 * </p>
	 */
	AbstractSimilarity(final DataModel dataModel, Weighting weighting,
			boolean centerData) throws TasteException {
		super(dataModel);
		this.weighted = weighting == Weighting.WEIGHTED;
		this.centerData = centerData;
		this.cachedNumItems = dataModel.getNumItems();
		this.cachedNumUsers = dataModel.getNumUsers();
		this.refreshHelper = new RefreshHelper(new Callable<Object>() {
			@Override
			public Object call() throws TasteException {
				cachedNumItems = dataModel.getNumItems();
				cachedNumUsers = dataModel.getNumUsers();
				return null;
			}
		});
	}

	final PreferenceInferrer getPreferenceInferrer() {
		return inferrer;
	}

	public final void setEnableXInferrer(boolean enable) {
		enableXInferrer = enable;
	}

	public final void setEnableYInferrer(boolean enable) {
		enableYInferrer = enable;
	}

	@Override
	public final void setPreferenceInferrer(PreferenceInferrer inferrer) {
		Preconditions.checkArgument(inferrer != null, "inferrer is null");
		refreshHelper.addDependency(inferrer);
		refreshHelper.removeDependency(this.inferrer);
		this.inferrer = inferrer;
	}

	final boolean isWeighted() {
		return weighted;
	}

	/**
	 * <p>
	 * Several subclasses in this package implement this method to actually
	 * compute the similarity from figures computed over users or items. Note
	 * that the computations in this class "center" the data, such that X and
	 * Y's mean are 0.
	 * </p>
	 * 
	 * <p>
	 * Note that the sum of all X and Y values must then be 0. This value isn't
	 * passed down into the standard similarity computations as a result.
	 * </p>
	 * 
	 * @param n
	 *            total number of users or items
	 * @param sumXY
	 *            sum of product of user/item preference values, over all
	 *            items/users preferred by both users/items
	 * @param sumX2
	 *            sum of the square of user/item preference values, over the
	 *            first item/user
	 * @param sumY2
	 *            sum of the square of the user/item preference values, over the
	 *            second item/user
	 * @param sumXYdiff2
	 *            sum of squares of differences in X and Y values
	 * @return similarity value between -1.0 and 1.0, inclusive, or
	 *         {@link Double#NaN} if no similarity can be computed (e.g. when no
	 *         items have been rated by both users
	 */
	abstract double computeResult(int n, double sumXY, double sumX2,
			double sumY2, double sumXYdiff2);

	@Override
	public double userSimilarity(long userID1, long userID2)
			throws TasteException {
		DataModel dataModel = getDataModel();
		PreferenceArray xPrefs = dataModel.getPreferencesFromUser(userID1);
		PreferenceArray yPrefs = dataModel.getPreferencesFromUser(userID2);
		itemsInXNotInY = 0;
		itemsInYNotInX = 0;
		totScoresInXNotInY = 0;
		totScoresInYNotInX = 0;
		totScoresInX=0;
		totScoresInY=0;
		totX2NotInY=0;
		totY2NotInX=0;
		itemsInX=0;
		itemsInY=0;
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
		double sumXYdiff2 = 0.0;
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
				double diff = x - y;
				sumXYdiff2 += diff * diff;
				count++;
			}
			if (compare <= 0) {// X has a value
				double x = xPrefs.getValue(xPrefIndex);
				itemsInX++;
				totScoresInX += x;
				totX2NotInY += x * x;
				itemsInXNotInY++;
				totScoresInXNotInY += x;
				
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
				totY2NotInX+= y * y;
				itemsInYNotInX++;
				totScoresInYNotInX += y;
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

		// "Center" the data. If my math is correct, this'll do it.
		double result;
		if (centerData) {
			double meanX = sumX / count;
			double meanY = sumY / count;
			// double centeredSumXY = sumXY - meanY * sumX - meanX * sumY + n *
			// meanX * meanY;
			double centeredSumXY = sumXY - meanY * sumX;
			// double centeredSumX2 = sumX2 - 2.0 * meanX * sumX + n * meanX *
			// meanX;
			double centeredSumX2 = sumX2 - meanX * sumX;
			// double centeredSumY2 = sumY2 - 2.0 * meanY * sumY + n * meanY *
			// meanY;
			double centeredSumY2 = sumY2 - meanY * sumY;
			result = computeResult(count, centeredSumXY, centeredSumX2,
					centeredSumY2, sumXYdiff2);
		} else {
			result = computeResult(count, sumXY, sumX2, sumY2, sumXYdiff2);
		}

		if (!Double.isNaN(result)) {
			result = normalizeWeightResult(result, count, cachedNumItems);
		}
		return result;
	}

	@Override
	public final double itemSimilarity(long itemID1, long itemID2)
			throws TasteException {
		DataModel dataModel = getDataModel();
		PreferenceArray xPrefs = dataModel.getPreferencesForItem(itemID1);
		PreferenceArray yPrefs = dataModel.getPreferencesForItem(itemID2);
		int xLength = xPrefs.length();
		int yLength = yPrefs.length();

		if (xLength == 0 || yLength == 0) {
			return Double.NaN;
		}

		long xIndex = xPrefs.getUserID(0);
		long yIndex = yPrefs.getUserID(0);
		int xPrefIndex = 0;
		int yPrefIndex = 0;

		double sumX = 0.0;
		double sumX2 = 0.0;
		double sumY = 0.0;
		double sumY2 = 0.0;
		double sumXY = 0.0;
		double sumXYdiff2 = 0.0;
		int count = 0;

		// No, pref inferrers and transforms don't apply here. I think.

		while (true) {
			int compare = xIndex < yIndex ? -1 : xIndex > yIndex ? 1 : 0;
			if (compare == 0) {
				// Both users expressed a preference for the item
				double x = xPrefs.getValue(xPrefIndex);
				double y = yPrefs.getValue(yPrefIndex);
				sumXY += x * y;
				sumX += x;
				sumX2 += x * x;
				sumY += y;
				sumY2 += y * y;
				double diff = x - y;
				sumXYdiff2 += diff * diff;
				count++;
			}
			if (compare <= 0) {
				if (++xPrefIndex == xLength) {
					break;
				}
				xIndex = xPrefs.getUserID(xPrefIndex);
			}
			if (compare >= 0) {
				if (++yPrefIndex == yLength) {
					break;
				}
				yIndex = yPrefs.getUserID(yPrefIndex);
			}
		}

		double result;
		if (centerData) {
			// See comments above on these computations
			double n = (double) count;
			double meanX = sumX / n;
			double meanY = sumY / n;
			// double centeredSumXY = sumXY - meanY * sumX - meanX * sumY + n *
			// meanX * meanY;
			double centeredSumXY = sumXY - meanY * sumX;
			// double centeredSumX2 = sumX2 - 2.0 * meanX * sumX + n * meanX *
			// meanX;
			double centeredSumX2 = sumX2 - meanX * sumX;
			// double centeredSumY2 = sumY2 - 2.0 * meanY * sumY + n * meanY *
			// meanY;
			double centeredSumY2 = sumY2 - meanY * sumY;
			result = computeResult(count, centeredSumXY, centeredSumX2,
					centeredSumY2, sumXYdiff2);
		} else {
			result = computeResult(count, sumXY, sumX2, sumY2, sumXYdiff2);
		}

		if (!Double.isNaN(result)) {
			result = normalizeWeightResult(result, count, cachedNumUsers);
		}
		return result;
	}

	@Override
	public double[] itemSimilarities(long itemID1, long[] itemID2s)
			throws TasteException {
		int length = itemID2s.length;
		double[] result = new double[length];
		for (int i = 0; i < length; i++) {
			result[i] = itemSimilarity(itemID1, itemID2s[i]);
		}
		return result;
	}

	final double normalizeWeightResult(double result, int count, int num) {
		double normalizedResult = result;
		if (weighted) {
			double scaleFactor = 1.0 - (double) count / (double) (num + 1);
			if (normalizedResult < 0.0) {
				normalizedResult = -1.0 + scaleFactor
						* (1.0 + normalizedResult);
			} else {
				normalizedResult = 1.0 - scaleFactor * (1.0 - normalizedResult);
			}
		}
		// Make sure the result is not accidentally a little outside [-1.0, 1.0]
		// due to rounding:
		if (normalizedResult < -1.0) {
			normalizedResult = -1.0;
		} else if (normalizedResult > 1.0) {
			normalizedResult = 1.0;
		}
		return normalizedResult;
	}

	@Override
	public final void refresh(Collection<Refreshable> alreadyRefreshed) {
		super.refresh(alreadyRefreshed);
		refreshHelper.refresh(alreadyRefreshed);
	}

	@Override
	public final String toString() {
		return this.getClass().getSimpleName() + "[dataModel:" + getDataModel()
				+ ",inferrer:" + inferrer + ']';
	}

}
