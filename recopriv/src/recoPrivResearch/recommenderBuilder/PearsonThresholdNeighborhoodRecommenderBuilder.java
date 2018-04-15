package recoPrivResearch.recommenderBuilder;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;

import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;

public class PearsonThresholdNeighborhoodRecommenderBuilder implements RecommenderBuilder {

	private final double threshold_; // Neighborhood threshold similarity

	public PearsonThresholdNeighborhoodRecommenderBuilder(double threshold) {
		threshold_ = threshold;
	}

	public Recommender buildRecommender(DataModel model) {

		UserSimilarity similarity = null;
		try{
			similarity = new PearsonCorrelationSimilarity(model);
		} catch(TasteException e) {
			e.printStackTrace();
		}

		// Make neighborhoods with users of similarity at least 0.1
		UserNeighborhood neighborhood = new ThresholdUserNeighborhood(threshold_, similarity, model);

		UserBasedRecommender recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);

		return recommender;
	}
}
