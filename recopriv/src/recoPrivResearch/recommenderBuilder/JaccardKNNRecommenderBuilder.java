package recoPrivResearch.recommenderBuilder;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.cf.taste.similarity.PreferenceInferrer;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;

import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import recoPrivResearch.recommenderBuilder.KNNRecommenderBuilder;
import recoPrivResearch.tools.ExceptHandler;

public class JaccardKNNRecommenderBuilder extends KNNRecommenderBuilder {

	private static final Logger logger = LogManager.getLogger(JaccardKNNRecommenderBuilder.class);

	public JaccardKNNRecommenderBuilder(int k, boolean isPPNS, int beta) {
		super(k, isPPNS, beta);
	}

	public Recommender buildRecommender(DataModel model) {

		UserSimilarity similarity = getSimilarityMeasure(model);

		// Neighborhood consisting of the nearest n users
		neighborhood = getKNNNeighborhood(similarity, model);
		logger.debug("neighborhood is null: {}", neighborhood==null);

		UserBasedRecommender recommender = getRecommender(model, neighborhood, similarity);

		return recommender;
	}

	protected UserSimilarity getSimilarityMeasure(DataModel model) {
		return new TanimotoCoefficientSimilarity(model);
	}

	protected UserBasedRecommender getRecommender(DataModel model, UserNeighborhood neighborhood, UserSimilarity similarity) {
		return new GenericUserBasedRecommender(model, neighborhood, similarity);
	}
}
