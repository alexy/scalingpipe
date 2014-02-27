package org.suffix.logreg

import java.io.{File, PrintWriter}
import java.util.Random

import com.aliasi.classify.{Classification, Classified, ConditionalClassifierEvaluator, LogisticRegressionClassifier}
import com.aliasi.corpus.XValidatingObjectCorpus
import com.aliasi.io.Reporters
import com.aliasi.stats.{AnnealingSchedule, RegressionPrior}
import com.aliasi.tokenizer.{RegExTokenizerFactory, TokenFeatureExtractor}
import com.aliasi.util.Files

object TextClassificationDemo extends App {
  
  val trainDir = new File("data/fourNewsGroups/4news-train")
  val testDir = new File("data/fourNewsGroups/4news-test")
  val categories = Array[String](
    "soc.religion.christian", "talk.religion.misc",
    "alt.atheism", "misc.forsale")
  
  val progressWriter = new PrintWriter(System.out, true)
  val numFolds = 10
  val corpus = 
    new XValidatingObjectCorpus[Classified[CharSequence]](numFolds)
  categories.foreach(category => {
    val classification = new Classification(category)
    new File(trainDir, category).listFiles().foreach(trainFile => {
      val text = Files.readFromFile(trainFile, "ISO-8859-1")
      val classified = new Classified[CharSequence](text, classification)
      corpus.handle(classified)
    })
    new File(testDir, category).listFiles().foreach(trainFile => {
      val text = Files.readFromFile(trainFile, "ISO-8859-1")
      val classified = new Classified[CharSequence](text, classification)
      corpus.handle(classified)
    })
  })
  progressWriter.println("Num instances=" + corpus.size())
  progressWriter.println("Permuting corpus")
  corpus.permuteCorpus(new Random(7117))
  progressWriter.println("Evaluating folds...")

  val factory = new RegExTokenizerFactory("\\p{L}+|\\d+") // letters|digits
  val featureExtractor = new TokenFeatureExtractor(factory)
  val minFeatureCount = 2
  val addInterceptFeature = true
  val nonInformativeIntercept = true
  val priorVariance = 0.0
  val prior = RegressionPrior.gaussian(1.0, nonInformativeIntercept)
  val annealingSchedule = 
    AnnealingSchedule.exponential(0.00025, 0.999)
  val minImprovement = 0.000000001
  val minEpochs = 100
  val maxEpochs = 20000
  val blocksize = corpus.size()
  val rollingAvgSize = 10
  
  for (fold <- 0 until numFolds) {
    corpus.setFold(fold)
    val reporter = Reporters.writer(progressWriter)
    val classifier: LogisticRegressionClassifier[CharSequence] = 
      LogisticRegressionClassifier.train(corpus, 
        featureExtractor, minFeatureCount, addInterceptFeature, 
        prior, blocksize, null, annealingSchedule, 
        minImprovement, rollingAvgSize, minEpochs, maxEpochs, 
        null, reporter)
    progressWriter.println("CLASSIFIER & FEATURES")
    progressWriter.println(classifier)
    progressWriter.println("EVALUATION")
    val evaluator = new ConditionalClassifierEvaluator[CharSequence](
      classifier, categories, false)
    corpus.visitTest(evaluator)
    progressWriter.println("FOLD=%5d  ACC=%4.2f  +/-%4.2f".format(
      fold,
      evaluator.confusionMatrix().totalAccuracy(),
      evaluator.confusionMatrix().confidence95()))
  }
}