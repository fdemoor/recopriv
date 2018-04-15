package recoPrivResearch.recommenderBuilder;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.cf.taste.similarity.PreferenceInferrer;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.AveragingPreferenceInferrer;
import org.apache.mahout.cf.taste.impl.similarity.ZeroingPreferenceInferrer;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import recoPrivResearch.recommenderBuilder.KRandomRecommenderBuilder;
import recoPrivResearch.tools.ExceptHandler;

/**
 * User-based collaborative filtering RecommenderBuilder using k randomly chosen neighbors per user, and cosine similarity to generate recommendations.
 */
public class CosKRandomRecommenderBuilder extends KRandomRecommenderBuilder {

	private static final Logger logger = LogManager.getLogger(CosKRandomRecommenderBuilder.class);

	// Attributes
	private final String similarityType;

	// Constructor
	public CosKRandomRecommenderBuilder(int k, String type, long seed) {
		super(k, seed);
		similarityType = type;
	}

	// Methods
	public Recommender buildRecommender(DataModel model) {

		UserSimilarity similarity = getSimilarityMeasure(model);

		neighborhood = getKRandomNeighborhood(similarity, model);

		UserBasedRecommender recommender = getRecommender(model, neighborhood, similarity);

		return recommender;
	}

	protected UserSimilarity getSimilarityMeasure(DataModel model) {
		UserSimilarity similarity = null;
		try {
			similarity = new UncenteredCosineSimilarity(model);
		} catch (TasteException e) {
			e.printStackTrace();
		}

		if (!similarityType.equals("cosine-mahout")) {
			similarity.setPreferenceInferrer(getPrefInferrer(model));
		}

		return similarity;
	}

	private PreferenceInferrer getPrefInferrer(DataModel model) {
		PreferenceInferrer result;

		int indexOfDash = similarityType.indexOf('-');
		String stringAfterDash = "";
		if (indexOfDash >= 0) {
			stringAfterDash = similarityType.substring(indexOfDash + 1);
		}

		switch (stringAfterDash) {
		case "":
			result = new ZeroingPreferenceInferrer();
			logger.debug("returning zeroingPreferenceInferrer");
			break;
		case "average":
			result = ExceptHandler.getAveragingPrefInferrer(model);
			logger.debug("returning averagingPrefInferrer");
			break;
		default:
			result = null;
			break;
		}
		return result;
	}

	protected UserBasedRecommender getRecommender(DataModel model, UserNeighborhood neighborhood, UserSimilarity similarity) {
		return new GenericUserBasedRecommender(model, neighborhood, similarity);
	}
}
