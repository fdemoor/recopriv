package recoPrivResearch;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import java.lang.management.MemoryUsage;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

// Trove
import gnu.trove.iterator.TLongIterator;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;

// Log4j
import org.apache.log4j.PropertyConfigurator;
/*import org.apache.log4j.BasicConfigurator;*/
//import org.apache.log4j.ConsoleAppender;
//import org.apache.log4j.Level;
/*import org.apache.log4j.EnhancedPatternLayout;*/

// Log4j2
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Mahout
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.eval.RecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.eval.IRStatistics;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderSequentialEvaluator;
import org.apache.mahout.cf.taste.impl.eval.RMSRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.RMSRecommenderEvaluatorKFold;
import org.apache.mahout.cf.taste.impl.eval.RMSRecommenderSequentialEvaluator;
import org.apache.mahout.cf.taste.impl.eval.GenericRecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.eval.IRStatisticsImpl;

import recoPrivResearch.attackEvaluator.AttackStats;
import recoPrivResearch.attackEvaluator.AttackEvaluator;
import recoPrivResearch.attackEvaluator.NearestNeighborSybilAttackEvaluator;
import recoPrivResearch.dataModelBuilder.SybilModelBuilder;
import recoPrivResearch.recommenderBuilder.CosKNNRecommenderBuilder;
import recoPrivResearch.recommenderBuilder.CosKRandomRecommenderBuilder;
import org.apache.mahout.cf.taste.impl.similarity.TwoStepUncenteredCosineSimilarity;
import recoPrivResearch.recommenderBuilder.ItemAvgRatingRecommenderBuilder;
import recoPrivResearch.recommenderBuilder.UserAvgRatingRecommenderBuilder;
import recoPrivResearch.recommenderBuilder.VariousSimmRecommenderBuilder;
import recoPrivResearch.recommenderBuilder.JaccardKNNRecommenderBuilder;
import recoPrivResearch.tools.Parameters;
import recoPrivResearch.tools.ExceptHandler;

import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.model.Preference;

public class EvaluateRecommender {

	private static final Logger logger = LogManager.getLogger(EvaluateRecommender.class);
	private final Random rand;
	private final Parameters params;

	public static void main(String[] args) {
		EvaluateRecommender eval = new EvaluateRecommender(new Parameters(args));
		eval.evaluate();
	}

	private EvaluateRecommender(Parameters p) {
		params = p;

		// Initialise log4j's configuration
		// Pass an Appender obj. to configure() to change configuration
		/*ConsoleAppender a = new ConsoleAppender(new EnhancedPatternLayout(EnhancedPatternLayout.TTCC_CONVERSION_PATTERN));*/
		//a.setThreshold(Level.INFO);
		////a.setThreshold(Level.DEBUG);
		//// Default conf (log to the console w/ DEBUG level)
		////BasicConfigurator.configure();
		//// Personalised conf
		/*BasicConfigurator.configure(a);*/

		// Initialise log4j's configuration from file
		PropertyConfigurator.configure(params.log4jConfFile_);

		rand = new Random(params.seed_);
	}

	private void evaluate() {

		logger.info("Starting evaluation");
		ExceptHandler.logParameters(params);

		// 1) Load dataset
		logger.info("Creating models, builders, and evaluators...");
		DataModel model = ExceptHandler.createDataModel(params.datasetPath_);
		// WARNING: FileDataModel throws a NoSuchElementException if there is a text file
		// with the same name prefix (until .csv) in the same dir as the dataset
		logger.info("Dataset containing ratings by {} users on {} items", ExceptHandler.getModelNumUsers(model), ExceptHandler.getModelNumItems(model));

		// 2) Create a RecommenderBuilder
		RecommenderBuilder builder = getRecoBuilder();

		// 3) Run the relevant evaluators with the aforementionned dataset and RecommenderBuilder
		for (int index = 0; index < params.nbRuns_; index++) {
			runEvaluators(model, builder);
		}

		logger.info("Done");
	}


	/**
	 * Returns the right type of RecommenderBuilder depending on parameter similarityType_.
	 * The default is cosine-based RecommenderBuilder.
	 */
	private RecommenderBuilder getRecoBuilder() {
		RecommenderBuilder result = null;

		boolean isPPNS = false;
		if (params.neighborhoodType_.equals("ppns")) {
			isPPNS = true;
		}

		if(params.neighborhoodType_.equals("random")) {
			result = new CosKRandomRecommenderBuilder(params.k_, params.similarityType_, params.seed_);
		} else if (params.similarityType_.contains("jaccard")) {
			result = new JaccardKNNRecommenderBuilder(params.k_, isPPNS, params.beta_);
		} else if (params.similarityType_.contains("cosine")){
			result = new CosKNNRecommenderBuilder(params.k_, params.similarityType_, isPPNS, params.beta_);
		} else if (params.similarityType_.equals("user-mean")) {
			result = new UserAvgRatingRecommenderBuilder();
		} else if (params.similarityType_.equals("item-mean")) {
			result = new ItemAvgRatingRecommenderBuilder();
		} else {
			result = new VariousSimmRecommenderBuilder(params.k_, params.similarityType_, params.twostepUseThreshold_, params.twostepIdealNbItems_, params.twostepFirstStepType_, params.isGlobalIdealNbItems_, params.percentIdealNbItems_, params.randomizeIdealNbItems_, rand, params.variantIdealNbItems_, isPPNS, params.beta_);
		}
		return result;
	}

	// Run from 0 to all evaluators among: MAE, RMSE, Information Retrieval stats, Sybil Attack
	private void runEvaluators(DataModel model, RecommenderBuilder builder) {
		logger.info("Running evaluators...");

		double mae = runMAEEval(model, builder);
		double rmse = runRMSEEval(model, builder);
		if(params.computeMae_ || params.computeRmse_) { // Log MAE and RMSE
			Logger recoQualityLogger = LogManager.getLogger(EvaluateRecommender.class.getName() + "-recoQuality");
			recoQualityLogger.info("{},{},{},{},{}", params.k_, params.trainingPercentage_, params.validationPercentage_, mae, rmse);
		}

		IRStatistics[] irStats = runIRStatsEvaluator(model, builder);

		runAboveThresEval(model, builder);
		runBuildRunTimeEval(model, builder);
		runBuildMemoryEval(model, builder);
		runHypothesesEval(model);
		runSizeDistribEval(model, builder);

		runSybilAttack(model, builder);
	}

	// Evaluate with MAE
	private double runMAEEval(DataModel model, RecommenderBuilder builder) {
		double result = -1.0;
		if(params.computeMae_) {
			logger.info("Computing MAE...");
			RecommenderEvaluator maeEvaluator = null;
			if(params.useSequentialEvaluators_) {
				maeEvaluator = new AverageAbsoluteDifferenceRecommenderSequentialEvaluator();
			} else {
				maeEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
			}
			maeEvaluator.setReproducibilitySeed(params.seed_);
			try {
				result = maeEvaluator.evaluate(builder, null, model, params.trainingPercentage_, params.validationPercentage_);
			} catch(TasteException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	// Evaluate with RMSE
	private double runRMSEEval(DataModel model, RecommenderBuilder builder) {
		double result = -1.0;
		if(params.computeRmse_) {
			logger.info("Computing RMSE...");
			RecommenderEvaluator rmseEvaluator = null;
			if(params.useSequentialEvaluators_) {
        if (params.trainingPercentage_ > 1) {
          // K-fold cross validation, trainingPercentage is used as K
          logger.info("Using {}-fold cross validation", params.trainingPercentage_);
          rmseEvaluator = new RMSRecommenderEvaluatorKFold();
        } else {
          rmseEvaluator = new RMSRecommenderSequentialEvaluator();
        }
			} else {
          if (params.trainingPercentage_ > 1) {
            // K-fold cross validation, trainingPercentage is used as K
            logger.info("Using {}-fold cross validation", params.trainingPercentage_);
            rmseEvaluator = new RMSRecommenderEvaluatorKFold();
          } else {
            rmseEvaluator = new RMSRecommenderEvaluator();
          }

			}
			rmseEvaluator.setReproducibilitySeed(params.seed_);
			try {
				result = rmseEvaluator.evaluate(builder, null, model, params.trainingPercentage_, params.validationPercentage_);

			} catch(TasteException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	// Evaluate with information retrieval measures (precision, recall, etc)
	private IRStatistics[] runIRStatsEvaluator(DataModel model, RecommenderBuilder builder) {
		//IRStatistics[] result = new IRStatistics[params.r_];
		IRStatistics[] result = new IRStatistics[16];
		if(params.computeIRStats_) {
			RecommenderIRStatsEvaluator irsEvaluator = new GenericRecommenderIRStatsEvaluator();
			for(int i=0; i < 8; i++) {
				logger.info("Computing Recall, Precision, etc for {} recommendations...", i+1);
				try {
					result[i] = irsEvaluator.evaluate(builder, null, model, null, 10, 2*i+2+16, params.likeThreshold_, params.validationPercentage_);
				} catch(TasteException e) {
					e.printStackTrace();
				}
			}
      for(int i=0; i < 8; i++) {
				logger.info("Computing Recall, Precision, etc for {} recommendations...", i+1);
				try {
					result[i+8] = irsEvaluator.evaluate(builder, null, model, null, 20, 4*i+4+32, params.likeThreshold_, params.validationPercentage_);
				} catch(TasteException e) {
					e.printStackTrace();
				}
			}
			logIRStats(result);
		}
		return result;
	}

	// If enabled, run the Sybil attack on users' privacy
	private AttackStats runSybilAttack(DataModel model, RecommenderBuilder builder) {
		AttackStats result = null;
		if(params.doSybilAttack_) { // Evaluate Sybil attack
			logger.info("Running Sybil attack...");

			AttackEvaluator sybilAttackEvaluator = new NearestNeighborSybilAttackEvaluator(params, rand);
			DataModelBuilder modelBuilder = new SybilModelBuilder(params, rand);

			if(params.doOnlyPrecomputations_) {
				runAttackPrecomputations(params, model, modelBuilder);
			} else {
				result = sybilAttackEvaluator.evaluate(builder, modelBuilder, model);
			}
		}
		return result;
	}

	private void logIRStats(IRStatistics[] stats) {
		Logger recoIRStatsLogger = LogManager.getLogger(EvaluateRecommender.class.getName() + "-recoIRStats");
		for(int i=0; i<stats.length; i++) {
			recoIRStatsLogger.info("{},{},{},{},{},{},{},{},{}", i+1, params.validationPercentage_, stats[i].getF1Measure(), stats[i].getFallOut(),
					stats[i].getFNMeasure(0.5), stats[i].getNormalizedDiscountedCumulativeGain(),
					stats[i].getPrecision(), stats[i].getReach(), stats[i].getRecall());
		}
	}

	private void runAttackPrecomputations(Parameters p, DataModel model, DataModelBuilder builder) {
		logger.info("Doing only the precomputations for the attack...");

		Logger precomputationsLogger = LogManager.getLogger(EvaluateRecommender.class.getName() + "-precomputations");
		precomputationsLogger.info("# percentAuxItems, targetID, NbNonAuxItems, targetProfileSize");

		DataModel attackedModel = ExceptHandler.injectSybils(builder, model);

		double percentAuxItems = p.percentAuxiliaryItems_;
		SybilModelBuilder sybilBuilder = (SybilModelBuilder) builder;
		TLongIntMap nbNonAuxItems = sybilBuilder.getNbNonAuxiliaryItems();
		for(TLongIterator it=nbNonAuxItems.keySet().iterator(); it.hasNext(); ) {
			long target = it.next();
			PreferenceArray prefs = ExceptHandler.getPreferences(model, target);
			precomputationsLogger.info("{},{},{},{}", percentAuxItems, target, nbNonAuxItems.get(target), prefs.length());
		}
	}

	private void runAboveThresEval(DataModel model, RecommenderBuilder builder) {
		if (params.computeAboveThres_ && builder instanceof VariousSimmRecommenderBuilder) {
			logger.info("Computing aboveThreshold information");
			Logger aboveThresLogger = LogManager.getLogger(EvaluateRecommender.class.getName() + "-aboveThres");
			VariousSimmRecommenderBuilder builder_ = (VariousSimmRecommenderBuilder) builder;
			builder_.buildRecommender(model);
			UserSimilarity sim = builder_.getUserSim();
			if (sim instanceof TwoStepUncenteredCosineSimilarity) {
				TwoStepUncenteredCosineSimilarity sim_ = (TwoStepUncenteredCosineSimilarity) sim;
				long user = 0;
				long neighbor = 0;
				try {
					for (LongPrimitiveIterator itU = model.getUserIDs(); itU.hasNext(); ) {
						user = itU.nextLong();
						double th = sim_.getSimilarityThresholdForUser(user);
						PreferenceArray UPrefs = model.getPreferencesFromUser(user);
						for (LongPrimitiveIterator itN = model.getUserIDs(); itN.hasNext(); ) {
							neighbor = itN.nextLong();
							double result = sim_.userSimilarity(user, neighbor);
							if (result >= th) {
								PreferenceArray NPrefs = model.getPreferencesFromUser(neighbor);
								int itemsInNNotInU = 0;
								for (Preference pref : NPrefs) {
									if (!UPrefs.hasPrefWithItemID(pref.getItemID())) {
										itemsInNNotInU += 1;
									}
								}
								aboveThresLogger.info("{},{},{},{}", user, neighbor, itemsInNNotInU, UPrefs.length());

							}
						}
					}
				} catch (TasteException ex) {}
			}
		}
	}

	private void runSizeDistribEval(DataModel model, RecommenderBuilder builder) {
		if (params.computeSizeDistrib_) {
			logger.info("Computing sizeDistrib information");
			Logger sizeDistribLogger = LogManager.getLogger(EvaluateRecommender.class.getName() + "-sizeDistrib");
			try {
				long user = 0;
				for (LongPrimitiveIterator itU = model.getUserIDs(); itU.hasNext(); ) {
					user = itU.nextLong();
					PreferenceArray UPrefs = model.getPreferencesFromUser(user);
					sizeDistribLogger.info("{}", UPrefs.length());
				}
			} catch (TasteException ex) {}
		}
	}

	private void runBuildRunTimeEval(DataModel model, RecommenderBuilder builder) {
		if (params.computeBuildRunTime_) {
				if (builder instanceof CosKNNRecommenderBuilder) {
					CosKNNRecommenderBuilder builder_ = (CosKNNRecommenderBuilder) builder;
					builder_.buildRecommender(model);
				} else if (builder instanceof VariousSimmRecommenderBuilder) {
					VariousSimmRecommenderBuilder builder_ = (VariousSimmRecommenderBuilder) builder;
					builder_.buildRecommender(model);
				}
		}
	}

	private void runBuildMemoryEval(DataModel model, RecommenderBuilder builder) {
		if (params.computeBuildMemory_) {
			Logger buildRecommenderLogger = LogManager.getLogger(EvaluateRecommender.class.getName() + "-buildMemory");

			MemoryMXBean memory = ManagementFactory.getMemoryMXBean();

			if (builder instanceof CosKNNRecommenderBuilder) {
				CosKNNRecommenderBuilder builder_ = (CosKNNRecommenderBuilder) builder;
				builder_.buildRecommender(model);
			} else if (builder instanceof VariousSimmRecommenderBuilder) {
				VariousSimmRecommenderBuilder builder_ = (VariousSimmRecommenderBuilder) builder;
				builder_.buildRecommender(model);
			}

			memory.gc();
			MemoryUsage heap = memory.getHeapMemoryUsage();
			MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();

			long heapUsage = heap.getUsed();
			long nonHeapUsage = nonHeap.getUsed();

			buildRecommenderLogger.info("{},{}", heapUsage, nonHeapUsage);
		}
	}

	private void runHypothesesEval(DataModel model) {
		if (params.computeHypotheses_) {
			logger.info("Running hypotheses evaluation");
			Logger hypoLogger = LogManager.getLogger(EvaluateRecommender.class.getName() + "-hypotheses");
			int nbHypoI = 0, nbHypoII = 0, nbHypoIII = 0;
			int nbInUAndN = 0;
			int sizeU, sizeN;
			long u, n;

			LongPrimitiveIterator itU = ExceptHandler.getModelUserIDs(model);
			for (; itU.hasNext(); ) {
				u = itU.nextLong();
				nbHypoI = 0;
				nbHypoII = 0;
				nbHypoIII = 0;
				sizeU = 0;
				PreferenceArray UPrefs = ExceptHandler.getPreferences(model, u);
				for (Preference pref : UPrefs) {
					if (pref.getValue() >= params.likeThreshold_) {
						sizeU += 1;
					}
				}
				LongPrimitiveIterator itN = ExceptHandler.getModelUserIDs(model);
				for (; itN.hasNext(); ) {
					n = itN.nextLong();
					if (n != u) {
						nbInUAndN = 0;
						sizeN = 0;
						PreferenceArray NPrefs = ExceptHandler.getPreferences(model, n);
						for (Preference pref : NPrefs) {
							if (pref.getValue() >= params.likeThreshold_) {
								sizeN += 1;
								if (ExceptHandler.getItemValue(model, u, pref.getItemID()) >= params.likeThreshold_) {
									nbInUAndN += 1;
								}
							}
						}
						if (nbInUAndN == sizeN) {
							if (nbInUAndN == sizeU) {
								nbHypoI += 1;
							} else if ((sizeN + 1) == sizeU) {
								nbHypoII += 1;
							}
						} else if ((nbInUAndN + 1) == sizeU) {
							if (sizeN == sizeU) {
								nbHypoIII += 1;
							}
						}
					}
				}
				hypoLogger.info("{},{},{},{},{}", u, nbHypoI, nbHypoII, nbHypoIII, sizeU);
			}
		}
	}
}
