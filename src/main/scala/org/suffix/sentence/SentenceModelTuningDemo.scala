package org.suffix.sentence

import com.aliasi.sentences.HeuristicSentenceModel
import collection.JavaConversions._

object SentenceModelTuningDemo extends App {

  val possibleStops = Set[String](".")
  val impossiblePenultimates = Set[String](
      "Bros", "Mr", "Mme", "Dr", "Jr", "Co")
  val impossibleStarts = Set[String](
      ")", "}", "]", ">", ".", "!", "?", ":", 
      ",", "-", "--", "---", "%")
  
  val sentenceModel = new DemoSentenceModel(possibleStops, 
      impossiblePenultimates, impossibleStarts, false, false)
  val sentenceModelEvaluator = new SentenceModelEvaluator(sentenceModel)
  sentenceModelEvaluator.evaluate(true)
}

class DemoSentenceModel(possibleStops: Set[String], 
    impossiblePenultimates: Set[String],
    impossibleStarts: Set[String],
    forceFinalStop: Boolean,
    balanceParens: Boolean) 
    extends HeuristicSentenceModel(possibleStops, 
    impossiblePenultimates, impossibleStarts, 
    forceFinalStop, balanceParens) {
}