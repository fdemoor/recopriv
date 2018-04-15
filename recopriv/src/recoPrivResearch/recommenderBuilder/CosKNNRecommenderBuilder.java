package recoPrivResearch.recommenderBuilder;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.cf.taste.similarity.PreferenceInferrer;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.similarity.AbstractSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.ScalarProductSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.AveragingPreferenceInferrer;
import org.apache.mahout.cf.taste.impl.similarity.ZeroingPreferenceInferrer;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import recoPrivResearch.recommenderBuilder.KNNRecommenderBuilder;
import recoPrivResearch.tools.ExceptHandler;

public class CosKNNRecommenderBuilder extends KNNRecommenderBuilder {

	private static final Logger logger = LogManager
			.getLogger(CosKNNRecommenderBuilder.class);

	private final String similarityType;

	public CosKNNRecommenderBuilder(int k, String type, boolean isPPNS, int beta) {
		super(k, isPPNS, beta);
		similarityType = type;
	}

	public Recommender buildRecommender(DataModel model) {

		UserSimilarity similarity = getSimilarityMeasure(model);

		// Neighborhood consisting of the nearest n users
		neighborhood = getKNNNeighborhood(similarity, model);
		logger.debug("neighborhood is null: {}", neighborhood == null);

		UserBasedRecommender recommender = getRecommender(model, neighborhood,
				similarity);

		return recommender;
	}

	protected UserSimilarity getSimilarityMeasure(DataModel model) {
		AbstractSimilarity similarity = null;
		try {
			if (similarityType.substring(0,
					Math.min(13, similarityType.length())).equals(
					"scalarProduct")) {
				similarity = new ScalarProductSimilarity(model);
			} else {
				similarity = new UncenteredCosineSimilarity(model);
			}
		} catch (TasteException e) {
			e.printStackTrace();
		}

		if (!similarityType.equals("cosine-mahout")) {
			similarity.setPreferenceInferrer(getPrefInferrer(model));
			// This sets inferrer for all the wup below.
		}
		/*
		 * For WUP similarity: to project X onto Y: - look at all the items of Y
		 * even those that are not in X - and only those of X that appear in Y
		 * so we must use the X inferrer, but no Y inferrer
		 *
		 * to project Y onto X: - look at all the items of X even those that are
		 * not in Y - and only those in Y that appear in X so we must use the Y
		 * inferrer, but no X inferrer
		 */
		// the following code relies on the fact that the above if sets the
		// inferrer for all similarityType except cosine-mahout
		int indexOfDash = similarityType.indexOf('-');
		if (indexOfDash < 0)
			indexOfDash = similarityType.length();
		if (similarityType.substring(0, indexOfDash).equals("wupXontoY")) {
			similarity.setEnableXInferrer(true);
			similarity.setEnableYInferrer(false);
		} else if (similarityType.substring(0, indexOfDash).equals("wupYontoX")) {
			similarity.setEnableXInferrer(false);
			similarity.setEnableYInferrer(true);
		} else if (similarityType.substring(0, indexOfDash).equals(
				"wupSimmFalse")) {
			// This should be equal to cosine and not cosine-mahout
			similarity.setEnableXInferrer(false);
			similarity.setEnableYInferrer(false);
		} else if (similarityType.substring(0, indexOfDash).equals(
				"wupSimmTrue")) {
			// This should be equal to cosine and not cosine-mahout
			similarity.setEnableXInferrer(true);
			similarity.setEnableYInferrer(true);
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

	protected UserBasedRecommender getRecommender(DataModel model,
			UserNeighborhood neighborhood, UserSimilarity similarity) {
		return new GenericUserBasedRecommender(model, neighborhood, similarity);
	}
}
