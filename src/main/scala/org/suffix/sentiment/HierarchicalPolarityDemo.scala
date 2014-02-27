package org.suffix.sentiment

import java.io.{File, FileInputStream, ObjectInputStream}

import scala.collection.JavaConversions.asScalaIterator

import com.aliasi.classify.{BaseClassifierEvaluator, Classification, Classified, ConditionalClassification, DynamicLMClassifier, JointClassifier}
import com.aliasi.util.{BoundedPriorityQueue, Files, ScoredObject}

object HierarchicalPolarityDemo extends App {

  val MaxSents = 25
  val MinSents = 5
  val MinScore = 0.5
  
  val polarityDir = new File("data/sentiment/txt_sentoken")
  val categories = polarityDir.list()
  val ngram = 8
  val classifier = 
    DynamicLMClassifier.createNGramProcess(categories, ngram)
  val modelFile = new File("models/subjectivity.model")
  val objIn = new ObjectInputStream(new FileInputStream(modelFile))
  val subjectivityClassifier = 
    objIn.readObject().asInstanceOf[JointClassifier[CharSequence]]
  objIn.close()
  train()
  evaluate()
  
  def train(): Unit = {
    var numTrainingCases = 0
    var numTrainingChars = 0
    Console.println("Training...")
    categories.foreach(category => {
      val classification = new Classification(category)
      val trainFiles = new File(polarityDir, category).listFiles()
      trainFiles.foreach(trainFile => {
        if (isTrainingFile(trainFile)) {
          val review = Files.readFromFile(trainFile, "ISO-8859-1")
          numTrainingCases += 1
          numTrainingChars += review.length()
          val classified = 
            new Classified[CharSequence](review, classification)
          classifier.handle(classified)
        }
      })
    })
    Console.println("  # Training Cases: " + numTrainingCases)
    Console.println("  # Training Chars: " + numTrainingChars)
  }
  
  def evaluate(): Unit = {
    Console.println("Evaluating...")
    val evaluator = new BaseClassifierEvaluator[CharSequence](
      null, categories, false)
    categories.foreach(category => {
      val files = new File(polarityDir, category).listFiles()
      files.foreach(file => {
        if (! isTrainingFile(file)) {
          val review = Files.readFromFile(file, "ISO-8859-1")
          val subjReview = subjectiveSentences(review)
          val classification = classifier.classify(subjReview)
          evaluator.addClassification(category, classification, null)
        }
      })
    })
    Console.println(evaluator.toString())
  }
  
  def isTrainingFile(file: File): Boolean = {
    file.getName().charAt(2) != '9'
  }
  
  def subjectiveSentences(review: String): String = {
    val sentences = review.split("\n")
    val priQueue = new BoundedPriorityQueue[ScoredObject[String]](
      ScoredObject.comparator(), MaxSents)
    sentences.foreach(sentence => {
      val subjClassification = subjectivityClassifier.classify(sentence).
        asInstanceOf[ConditionalClassification]
      val subjProb = if (subjClassification.category(0).equals("quote"))
        subjClassification.conditionalProbability(0)
        else subjClassification.conditionalProbability(1)
      priQueue.offer(new ScoredObject[String](sentence, subjProb))
    })
    val scoredObjects = priQueue.iterator().toList
    var i = 0
    val reviewBuf = new StringBuilder()
    scoredObjects.foreach(scoredObject => {
      val skip = ((scoredObject.score() < MinScore) && (i > MinSents))
      if (! skip) reviewBuf.append(scoredObject.getObject() + "\n")
      i += 1
    })
    reviewBuf.toString().trim()
  }
}