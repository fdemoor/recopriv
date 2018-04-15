package recoPrivResearch.recommenderBuilder;

import java.util.Random;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.cf.taste.similarity.PreferenceInferrer;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.similarity.AbstractSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.AsymmetricUncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.ScalarProductSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TwoStepUncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.AveragingPreferenceInferrer;
import org.apache.mahout.cf.taste.impl.similarity.ZeroingPreferenceInferrer;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import recoPrivResearch.recommenderBuilder.KNNRecommenderBuilder;
import recoPrivResearch.tools.ExceptHandler;

public class VariousSimmRecommenderBuilder extends KNNRecommenderBuilder {

	private static final Logger logger = LogManager
			.getLogger(VariousSimmRecommenderBuilder.class);

	private final String similarityType;
	private final boolean twostepUseThreshold;
	private final int nbIdealItems;
	private final String twostepFirstStepType;
	private final boolean isGlobalIdealNbItems;
	private final double percentIdealNbItems;
	private final boolean randomizeIdealNbItems;
	private final String variantIdealNbItems;
	private final Random rand;

	private UserSimilarity sim;

	public VariousSimmRecommenderBuilder(int k, String type, boolean useThreshold, int nbItems, String firstStepType, boolean isGlobal, double per, boolean randomize, Random r, String variantIdeal, boolean isPPNS, int beta) {
		super(k, isPPNS, beta);
		similarityType = type;
		twostepUseThreshold = useThreshold;
		nbIdealItems = nbItems;
		twostepFirstStepType = firstStepType;
		isGlobalIdealNbItems = isGlobal;
		percentIdealNbItems = per;
		randomizeIdealNbItems = randomize;
		rand = r;
		variantIdealNbItems = variantIdeal;
	}

	public Recommender buildRecommender(DataModel model) {

		UserSimilarity similarity = getSimilarityMeasure(model);
		sim = similarity;

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
			logger.debug("checking similarityType ");
			if (similarityType.contains("step")) {
				logger.debug("contains twostep or onestep");
				similarity = new TwoStepUncenteredCosineSimilarity(model);
				TwoStepUncenteredCosineSimilarity twostepSimil = (TwoStepUncenteredCosineSimilarity) similarity;
				twostepSimil.setUseThreshold(twostepUseThreshold);
				twostepSimil.setIdealNbItems(nbIdealItems);
				twostepSimil.setIsGlobalIdealNbItems(isGlobalIdealNbItems);
				twostepSimil.setPercentIdealNbItems(percentIdealNbItems);
				twostepSimil.setRandomizeIdealNbItems(randomizeIdealNbItems);
				twostepSimil.setRand(rand);
				if (twostepFirstStepType.contains("cosine-average")) {
					similarity.setPreferenceInferrer(ExceptHandler.getAveragingPrefInferrer(model));
					similarity.setEnableXInferrer(true);
					similarity.setEnableYInferrer(true);
					logger.debug("first step used cosine-average similarity metric");
				} else { // Default
					similarity.setPreferenceInferrer(new ZeroingPreferenceInferrer());
					logger.debug("first step used cosine similarity metric");
				}
				int startPctile = similarityType.indexOf("(") + 1;
				int endPctile = similarityType.indexOf(')');
				String percentileThreshold = similarityType.substring(
						startPctile, endPctile);
				logger.debug("got percentileThreshold string {}", percentileThreshold);
				twostepSimil.setPercentileThreshold(Double.parseDouble(percentileThreshold));
				if (similarityType.contains("global")) {
					twostepSimil.setPerUserDistro(false);
					logger.debug("global distro");
				} else {
					twostepSimil.setPerUserDistro(true);
					logger.debug("per-user distro");
				}
				if (similarityType.contains("one")) {
					twostepSimil.setUseRichness(false);
					logger.debug("no richness -> one-step");
				} else {
					twostepSimil.setUseRichness(true);
					logger.debug("richness -> two-step ");
				}

			} else if (similarityType.contains("scalarProduct")) {
				logger.debug("contains scalarProduct");
				similarity = new ScalarProductSimilarity(model);
			} else if (similarityType.contains("pearson")) {
				logger.debug("contains pearson");
				similarity = new PearsonCorrelationSimilarity(model);
			} else if (similarityType.contains("asymcosX")) {
				logger.debug("contains asymcosX");
				similarity = new AsymmetricUncenteredCosineSimilarity(model);
				logger.debug("instantiated asymcosX");
				((AsymmetricUncenteredCosineSimilarity) similarity)
						.setDivideByX(true);
				((AsymmetricUncenteredCosineSimilarity) similarity)
						.setDivideByY(false);
			} else if (similarityType.contains("asymcosY")) {
				logger.debug("contains asymcosY");
				similarity = new AsymmetricUncenteredCosineSimilarity(model);
				((AsymmetricUncenteredCosineSimilarity) similarity)
						.setDivideByX(false);
				((AsymmetricUncenteredCosineSimilarity) similarity)
						.setDivideByY(true);
			} else {
				logger.debug("contains nothing");
				similarity = new UncenteredCosineSimilarity(model);
			}
		} catch (TasteException e) {
			e.printStackTrace();
		}

		if (!similarityType.contains("mahout") && !similarityType.contains("step")) {
			similarity.setPreferenceInferrer(getPrefInferrer(model));
			logger.debug("Setting preference inferrer for similarityType {}", similarityType);
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
		int indexOfDash = similarityType.indexOf('_');
		if (indexOfDash < 0)
			indexOfDash = similarityType.length();
		if (similarityType.contains("wupXontoY")) {
			similarity.setEnableXInferrer(true);
			similarity.setEnableYInferrer(false);
		} else if (similarityType.contains("wupYontoX")) {
			similarity.setEnableXInferrer(false);
			similarity.setEnableYInferrer(true);
		} else if (similarityType.contains("wupSimmFalse")) {
			// This should be equal to cosine and not cosine-mahout
			similarity.setEnableXInferrer(false);
			similarity.setEnableYInferrer(false);
		} else if (similarityType.contains("wupSimmTrue")) {
			// This should be equal to cosine and not cosine-mahout
			similarity.setEnableXInferrer(true);
			similarity.setEnableYInferrer(true);
		}

		return similarity;
	}

	/**
	 * buildRecommender method must have been called before
	 */
	public UserSimilarity getUserSim() {
		return sim;
	}

	private PreferenceInferrer getPrefInferrer(DataModel model) {
		PreferenceInferrer result;

		if (similarityType.contains("average")) {
			result = ExceptHandler.getAveragingPrefInferrer(model);
			logger.debug("returning averagingPrefInferrer");
		} else {
			result = new ZeroingPreferenceInferrer();
			logger.debug("returning zeroingPreferenceInferrer");
		}
		return result;
	}

	protected UserBasedRecommender getRecommender(DataModel model,
			UserNeighborhood neighborhood, UserSimilarity similarity) {
		return new GenericUserBasedRecommender(model, neighborhood, similarity);
	}
}
