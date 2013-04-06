package org.suffix.postag

import java.io.{File, FileInputStream, ObjectInputStream}

import scala.collection.JavaConversions.{asScalaBuffer, seqAsJavaList}

import com.aliasi.hmm.{HiddenMarkovModel, HmmDecoder}
import com.aliasi.tag.ScoredTagging
import com.aliasi.tokenizer.RegExTokenizerFactory
import com.aliasi.util.Streams

object MedPostPOSRunDemo extends App {

  val TokFactory = new RegExTokenizerFactory("(-|'|\\d|\\p{L})+|\\S")
  val MaxNBest = 5
  
  val testSentences = Array[String](
    "A good correlation was found between the grade of Barrett's esophagus dysplasia and high p53 positivity.",
    "This correlation was also confirmed by detection of early carcinoma in patients with \"preventive\" extirpation of the esophagus due to a high-grade dysplasia."
  )
  
  val medPostModelFile = new File("models/pos-en-bio-medpost.HiddenMarkovModel")
  test(medPostModelFile, testSentences)

  def test(modelFile: File, inputs: Array[String]): Unit = {
    Console.println("Reading model from file: " + modelFile.getName())
    val oistream = new ObjectInputStream(new FileInputStream(modelFile))
    val hmm = oistream.readObject().asInstanceOf[HiddenMarkovModel]
    Streams.closeQuietly(oistream)
    val hmmDecoder = new HmmDecoder(hmm)
    for (testSentence <- testSentences) {
      Console.println("SENTENCE: " + testSentence)
      val tokenizer = TokFactory.tokenizer(
        testSentence.toCharArray(), 0, testSentence.length())
      val tokens = tokenizer.tokenize()
      firstBest(tokens, hmmDecoder)
      nBest(tokens, hmmDecoder)
      confidence(tokens, hmmDecoder)
    }
  }
  
  def firstBest(tokens: Array[String], decoder: HmmDecoder): Unit = {
    val tagging = decoder.tag(tokens.toList)
    Console.println("FIRST BEST: " + 
      tagging.tokens().zip(tagging.tags()).
      map(x => x._1 + "/" + x._2).
      foldLeft(" ")(_ + " " + _))
  }

  def nBest(tokens: Array[String], decoder: HmmDecoder): Unit = {
    Console.println("N-BEST:")
    val nbit = decoder.tagNBest(tokens.toList, MaxNBest)
    while (nbit.hasNext()) {
      val tagging: ScoredTagging[String] = nbit.next()
      Console.println(tagging.score() + ": " + 
        tagging.tokens().zip(tagging.tags()).
        map(x => x._1 + "/" + x._2).
        foldLeft(" ")(_ + " " + _))
    }
  }

  def confidence(tokens: Array[String], decoder: HmmDecoder): Unit = {
    Console.println("CONFIDENCE:")
    val lattice = decoder.tagMarginal(tokens.toList)
    for (i <- 0 until tokens.length) {
      val tagScores = lattice.tokenClassification(i)
      Console.println(tokens(i) + "/" + tagScores.category(i) + 
        " " + tagScores.score(i))
    }
  }
}

