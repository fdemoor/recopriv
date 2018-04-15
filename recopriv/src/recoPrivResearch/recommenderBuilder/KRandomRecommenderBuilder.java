package recoPrivResearch.recommenderBuilder;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.neighborhood.RandomNUserNeighborhood;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import recoPrivResearch.tools.ExceptHandler;

/**
 * RecommenderBuilder using k randomly chosen neighbors per user to generate recommendations.
 */
public abstract class KRandomRecommenderBuilder implements NeighborhoodRecommenderBuilder {

	private static final Logger logger = LogManager.getLogger(KRandomRecommenderBuilder.class);

	// Attributes
	private final int k_; // Number of neighbors per user
	private final long seed_; // Seed for peusdo-random numbers generation
	protected UserNeighborhood neighborhood; // Should only be initialised by buildRecommender

	// Constructor
	public KRandomRecommenderBuilder(int k, long seed) {
		k_ = k;
		seed_ = seed;
	}

	// Abstract methods
	public abstract Recommender buildRecommender(DataModel model);

	protected abstract UserSimilarity getSimilarityMeasure(DataModel model);

	protected abstract UserBasedRecommender getRecommender(DataModel model, UserNeighborhood neighborhood, UserSimilarity similarity);

	// Concrete methods
	protected UserNeighborhood getKRandomNeighborhood(UserSimilarity similarity, DataModel model) {
		UserNeighborhood neighborhood = null;
		try {
			neighborhood = new RandomNUserNeighborhood(k_, similarity, model, seed_);
		} catch (TasteException e) {
			e.printStackTrace();
		}
		return neighborhood;
	}

	/**
	 * Return the UserNeighborhood computed during the last call to
	 * buildRecommender.
	 */
	public UserNeighborhood getNeighborhood() {
		return neighborhood;
	}

}
