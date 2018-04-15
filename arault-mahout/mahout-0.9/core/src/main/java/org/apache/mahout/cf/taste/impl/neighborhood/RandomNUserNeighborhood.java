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

import java.util.Set;
import java.util.HashSet;
import java.util.Random;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.common.SamplingLongPrimitiveIterator;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * <p>
 * Computes a neighborhood consisting of n randomly chosen users to a given user.
 * </p>
 */
public final class RandomNUserNeighborhood extends AbstractUserNeighborhood {

  	private static final Logger log = LoggerFactory.getLogger(RandomNUserNeighborhood.class);

	// Attributes
	private final int n;
	private final long seed;

	// Constructors
	/**
	 * @param n
	 *            neighborhood size; capped at the number of users in the data
	 *            model
	 * @throws IllegalArgumentException
	 *             if {@code n < 1}, or userSimilarity or dataModel are
	 *             {@code null}
	 */
	public RandomNUserNeighborhood(int n, UserSimilarity userSimilarity, DataModel dataModel, long seed) throws TasteException {
		this(n, userSimilarity, dataModel, 1.0, seed);
	}

	/**
	 * @param n
	 *            neighborhood size; capped at the number of users in the data
	 *            model
	 * @param samplingRate
	 *            percentage of users to consider when building neighborhood --
	 *            decrease to trade quality for performance
	 * @throws IllegalArgumentException
	 *             if {@code n < 1} or samplingRate is NaN or not in (0,1], or
	 *             userSimilarity or dataModel are {@code null}
	 */
	public RandomNUserNeighborhood(int n, UserSimilarity userSimilarity, DataModel dataModel, double samplingRate, long seed) throws TasteException {
		super(userSimilarity, dataModel, samplingRate);
		Preconditions.checkArgument(n >= 1, "n must be at least 1");
		int numUsers = dataModel.getNumUsers();
		this.n = n > numUsers ? numUsers : n;
		this.seed = seed;
	}

	// Methods
	@Override
	/**
	 * Version of getUserNeighborhood for compatibility with Mahout's original arguments.
	 * Uses the seed attribute to create a Random PRNG object.
	 */
	public long[] getUserNeighborhood(long userID) throws TasteException {
		return getUserNeighborhood(userID, new Random(seed));
	}

	@Override
	/**
	 * Version of getUserNeighborhood for compatibility with recopriv's arguments.
	 * choiceBehavior is ignored.
	 */
	public long[] getUserNeighborhood(long userID, String choiceBehavior, Random rand) throws TasteException {
		return getUserNeighborhood(userID, rand);
	}

	/**
	 * Return an array of n randomly chosen user IDs.
	 * The result contains neither duplicates or userID.
	 */
	public long[] getUserNeighborhood(long userID, Random rand) throws TasteException {
		long[] result = new long[n];

		long[] userIDs = getUserIDsArray(getDataModel());
		Set<Long> alreadyChosenNeighbors = new HashSet<Long>(n);
		int i = 0;
		do {
			long tentativeID = rand.nextInt(userIDs.length);
			if(!alreadyChosenNeighbors.contains(tentativeID) && tentativeID != userID) {
				result[i] = tentativeID;
				alreadyChosenNeighbors.add(tentativeID);
				i++;
			}
		} while(i < result.length);

		return result;
	}

	/**
	 * Get all the user IDs contained in dataModel as an array of long integers.
	 */
	private long[] getUserIDsArray(DataModel dataModel) throws TasteException {
		long[] result = new long[dataModel.getNumUsers()];
		
		LongPrimitiveIterator userIDs = SamplingLongPrimitiveIterator.maybeWrapIterator(getDataModel().getUserIDs(), getSamplingRate());
		for(int i=0; i<result.length; i++) {
			result[i] = userIDs.next();
		}

		return result;
	}

	@Override
	public String toString() {
		return "RandomNUserNeighborhood";
	}

}
