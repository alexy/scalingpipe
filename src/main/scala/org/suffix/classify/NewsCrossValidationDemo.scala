package org.suffix.classify

import java.io.File
import com.aliasi.corpus.XValidatingObjectCorpus
import com.aliasi.classify.Classified
import com.aliasi.classify.Classification
import com.aliasi.util.Files
import java.util.Random
import com.aliasi.classify.DynamicLMClassifier
import com.aliasi.util.AbstractExternalizable
import com.aliasi.classify.JointClassifier
import com.aliasi.classify.JointClassifierEvaluator

object NewsCrossValidationDemo extends App {

  val TrainingDir = new File("data/fourNewsGroups/4news-train")
  val TestingDir = new File("data/fourNewsGroups/4news-test")
  val Categories = Array[String](
    "soc.religion.christian",
    "talk.religion.misc",
    "alt.atheism",
    "misc.forsale"    
  )
  val NGramSize = 6
  val NumFolds = 10
  
  val corpus = 
    new XValidatingObjectCorpus[Classified[CharSequence]](NumFolds)
  for (category <- Categories) {
    val classification = new Classification(category)
    val trainCatDir = new File(TrainingDir, category)
    for (trainingFile <- trainCatDir.listFiles()) {
      val text = Files.readFromFile(trainingFile, "ISO-8859-1")
      val classified = new Classified[CharSequence](text, classification)
      corpus.handle(classified)
    }
    val testCatDir = new File(TestingDir, category)
    for (testingFile <- testCatDir.listFiles()) {
      val text = Files.readFromFile(testingFile, "ISO-8859-1")
      val classified = new Classified[CharSequence](text, classification)
      corpus.handle(classified)
    }
  }
  Console.println("Num instances: " + corpus.size())
  Console.println("Permuting corpus...")
  corpus.permuteCorpus(new Random(42))
  Console.println("%5s  %10s".format("FOLD", "ACCU"))
  for (fold <- 0 until NumFolds) {
    corpus.setFold(fold)
    val classifier = 
      DynamicLMClassifier.createNGramProcess(Categories, NGramSize)
    corpus.visitTrain(classifier)
    val compiledClassifier = 
      AbstractExternalizable.compile(classifier).
      asInstanceOf[JointClassifier[CharSequence]]
    val evaluator = new JointClassifierEvaluator[CharSequence](
      compiledClassifier, Categories, false)
    corpus.visitTrain(evaluator)
    Console.println("%5d  %4.2f +/- %4.2f".format(
      fold,
      evaluator.confusionMatrix().totalAccuracy(),
      evaluator.confusionMatrix().confidence95()))
  }
}