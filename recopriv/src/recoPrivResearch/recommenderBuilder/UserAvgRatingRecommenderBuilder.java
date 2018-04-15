package recoPrivResearch.recommenderBuilder;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.recommender.ItemUserAverageRecommender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserAvgRatingRecommenderBuilder implements RecommenderBuilder {

	private static final Logger logger = LogManager.getLogger(UserAvgRatingRecommenderBuilder.class);

	public Recommender buildRecommender(DataModel model) {
		logger.debug("Using user-mean rating as baseline predictor");

		return getBuilder(model);
	}

	private Recommender getBuilder(DataModel model) {
		Recommender result = null;
		try {
			result = new ItemUserAverageRecommender(model);
		} catch(TasteException e) {
			e.printStackTrace();
		}
		return result;
	}

}
