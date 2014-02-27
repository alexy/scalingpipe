package org.suffix.em

import java.io.File
import java.util.regex.Pattern

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.{asScalaSet, collectionAsScalaIterable, mapAsJavaMap, setAsJavaSet}
import scala.collection.mutable.ArrayBuffer

import com.aliasi.classify.{Classification, Classified, JointClassifier, JointClassifierEvaluator, TradNaiveBayesClassifier}
import com.aliasi.corpus.{Corpus, ObjectHandler}
import com.aliasi.io.{FileLineReader, LogLevel, Reporters}
import com.aliasi.stats.Statistics
import com.aliasi.tokenizer.{EnglishStopTokenizerFactory, IndoEuropeanTokenizerFactory, LowerCaseTokenizerFactory, RegExFilteredTokenizerFactory, TokenizerFactory}
import com.aliasi.util.{AbstractExternalizable, Factory, ObjectToSet, Strings}

object Em20NewsgroupsDemo extends App {
  
  val CategoryPrior = 0.005D
  val TokenInCategoryPrior = 0.001D
  val InitialTokenInCategoryPrior = 0.001D
  val DocLengthNorm = 9.0D
  val MinCounts = 0.0001D
  val MinImprovement = 0.0001D
  val NumReplications = 10
  val MaxEpochs = 20
  
  val startTime = System.currentTimeMillis()
  val tokenizerFactory = {
    var factory: TokenizerFactory = 
      IndoEuropeanTokenizerFactory.INSTANCE
    factory = new RegExFilteredTokenizerFactory(factory, 
      Pattern.compile("\\p{Alpha}+"))
    factory = new LowerCaseTokenizerFactory(factory)
    factory = new EnglishStopTokenizerFactory(factory)
    factory
  }
  val corpusPath = new File("data/20newsgroups")
  val corpus = new TwentyNewsGroupsCorpus(corpusPath, tokenizerFactory)
  val unlabeledCorpus = corpus.unlabeledCorpus()
  val reporter = Reporters.stream(System.out, "ISO-8859-1").
    setLevel(LogLevel.DEBUG)
  val NumSupervisedItems = Array[Int](
      1, 2, 4, 8, 16, 32, 64, 128, 256, 512)
  for (numSupervisedItems <- NumSupervisedItems) {
    val accs = ArrayBuffer[Double]()
    val accsEm = ArrayBuffer[Double]()
    for (trial <- 0 until NumReplications) {
      corpus.setMaxSupervisedInstancesPerCategory(numSupervisedItems)
      val initialClassifier = new TradNaiveBayesClassifier(
          corpus.categorySet(), tokenizerFactory,
          CategoryPrior, InitialTokenInCategoryPrior, DocLengthNorm)
      val classifierFactory = new Factory[TradNaiveBayesClassifier] {
        override def create(): TradNaiveBayesClassifier = {
          new TradNaiveBayesClassifier(corpus.categorySet(), 
            tokenizerFactory, CategoryPrior, 
            TokenInCategoryPrior, DocLengthNorm)
        }
      }
      val emClassifier = TradNaiveBayesClassifier.emTrain(
        initialClassifier, classifierFactory, 
        corpus, unlabeledCorpus, MinCounts, 
        MaxEpochs, MinImprovement, reporter)
      val acc = eval(initialClassifier, corpus)
      val accEm = eval(emClassifier, corpus)
      Console.println("ACC=%5.3f   EM ACC=%5.3f\n".format(acc, accEm))
      accs += acc
      accsEm += accEm
    }
    Console.println("--------")
    val endTime = System.currentTimeMillis()
    Console.println("#Sup=%4d  Supervised mean(acc)=%5.3f sd(acc)=%5.3f   EM mean(acc)=%5.3f sd(acc)=%5.3f     %10s\n\n".
      format(numSupervisedItems, 
      Statistics.mean(accs.toArray), 
      Statistics.standardDeviation(accs.toArray), 
      Statistics.mean(accsEm.toArray), 
      Statistics.standardDeviation(accsEm.toArray),
      Strings.msToString(endTime - startTime)))
  }
  reporter.close()
  
  def eval(classifier: TradNaiveBayesClassifier, 
      corpus: Corpus[ObjectHandler[Classified[CharSequence]]]): 
      Double = {
    val categories = classifier.categorySet().
      map(x => x.asInstanceOf[String]).
      toArray.sortWith(_ < _)
    val compiledClassifier = AbstractExternalizable.compile(classifier).
      asInstanceOf[JointClassifier[CharSequence]]
    val evaluator = new JointClassifierEvaluator[CharSequence](
      compiledClassifier, categories, false)
    corpus.visitTest(evaluator)
    evaluator.confusionMatrix().totalAccuracy()
  }
}

class TwentyNewsGroupsCorpus(path: File, factory: TokenizerFactory) 
    extends Corpus[ObjectHandler[Classified[CharSequence]]] {

  val HeaderRegex = Pattern.compile("^\\w+: ")
  var maxSupervisedInstancesPerCategory = 0
  
  val trainDir = new File(path, "20news-bydate-train")
  val testDir = new File(path, "20news-bydate-test")
  
  val trainCatToText = readToMap(trainDir)
  val testCatToText = readToMap(testDir)

  def readToMap(dir: File): Map[String,Array[String]] = {
    val catToText = new ObjectToSet[String,String]()
    for (catDir <- dir.listFiles()) {
      val category = catDir.getName()
      for (file <- catDir.listFiles()) {
        val text = extractText(
          FileLineReader.readLineArray(file, "ISO-8859-1"))
        if (text != null) catToText.addMember(category, text)
      }
    }
    Map() ++ 
      catToText.entrySet().map(x => x.getKey()).zip(
      catToText.entrySet().map(x => x.getValue().toArray().
      map(x => x.asInstanceOf[String])))
  }

  def extractText(lines: Array[String]): String = {
    val buf = new StringBuilder()
    for (line <- lines) {
      if (! HeaderRegex.matcher(line).find()) {
        buf.append(line).append(" ")
      }
    }
    val text = buf.toString().trim()
    if (atLeastThreeTokens(text)) text else null
  }
  
  def atLeastThreeTokens(text: String): Boolean = {
    val cs = text.toCharArray()
    val tokenizer = factory.tokenizer(cs, 0, cs.length)
    if (tokenizer.nextToken() == null) false
    else {
      if (tokenizer.nextToken() == null) false
      else true
    }
  }
  
  def categorySet(): Set[String] = {
    trainCatToText.keySet
  }
  
  def setMaxSupervisedInstancesPerCategory(max: Int): Unit = {
    maxSupervisedInstancesPerCategory = max
  }
  
  def unlabeledCorpus(): Corpus[ObjectHandler[CharSequence]] = {
    new Corpus[ObjectHandler[CharSequence]]() {
      
      override def visitTest(handler: ObjectHandler[CharSequence]): Unit = {
        throw new UnsupportedOperationException()
      }
      
      override def visitTrain(handler: ObjectHandler[CharSequence]): Unit = {
        for (texts <- trainCatToText.values()) {
          for (i <- maxSupervisedInstancesPerCategory until texts.length) {
            handler.handle(texts(i))
          }
        }
      }
    }  
  }
  
  override def visitTrain(
      handler: ObjectHandler[Classified[CharSequence]]): Unit = {
    visit(trainCatToText, handler, maxSupervisedInstancesPerCategory)
  }
  
  override def visitTest(
      handler: ObjectHandler[Classified[CharSequence]]): Unit = {
    visit(testCatToText, handler, Int.MaxValue)
  }
  
  def visit(catToItems: Map[String,Array[String]],
      handler: ObjectHandler[Classified[CharSequence]],
      maxItems: Int): Unit = {
    for (entry <- catToItems.entrySet()) {
      val category = entry.getKey()
      val classification = new Classification(category)
      val texts = entry.getValue()
      var i = 0
      while (i < maxItems && i < texts.length) {
        val classifiedText = new Classified[CharSequence](
          texts(i), classification)
        handler.handle(classifiedText)
        i += 1
      }
    }
  }
  
  override def toString(): String = {
    val buf = new StringBuilder()
    var totalTrain: Int = 0
    var totalTest: Int = 0
    val categories = trainCatToText.keySet.toArray.sortWith(_ < _)
    for (category <- categories) {
      buf.append(category)
      val numTrain = trainCatToText(category).length
      val numTest = testCatToText(category).length
      totalTrain += numTrain
      totalTest += numTest
      buf.append(" #train=").
        append(numTrain).
        append(" #test=").
        append(numTest).
        append("\n")
    }
    buf.append("Totals: #train=").
      append(totalTrain).
      append(" #test=").
      append(totalTest).
      append(" #combined=").
      append(totalTrain + totalTest).
      append("\n")
    buf.toString()
  }
}