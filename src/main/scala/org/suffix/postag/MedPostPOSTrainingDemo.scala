package org.suffix.postag

import java.io.File

import com.aliasi.hmm.HmmCharLmEstimator
import com.aliasi.util.AbstractExternalizable

object MedPostPOSTrainingDemo extends App {

  val medPostDataDir = new File("data/medtag/medpost")
  val medPostCorpus = new MedPostPosCorpus(medPostDataDir)
  val medPostModelFile = new File("models/pos-en-bio-medpost.HiddenMarkovModel")
  train(medPostCorpus, medPostModelFile)
  
//  val brownZipDir = new File("data/brown.zip")
//  val brownCorpus = new BrownPosCorpus(brownZipDir)
//  val brownModelFile = new File("models/pos-en-bio-brown.HiddenMarkovModel")
//  train(brownCorpus, brownModelFile)

//  val geniaPosFile = new File("data/GENIAcorpus3.02.pos.txt")
//  val geniaCorpus = new GeniaPosCorpus(geniaPosFile)
//  val geniaModelFile = new File("models/pos-en-bio-genia.HiddenMarkovModel")
//  train(geniaCorpus, geniaModelFile)

  def train(corpus: PosCorpus, modelFile: File): Unit = {
    val NGram = 8
    val NumChars = 256
    val LambdaFactor = 8.0
    // set up parser with estimator as handler
    val parser = corpus.parser()
    val estimator = new HmmCharLmEstimator(NGram, NumChars, LambdaFactor)
    parser.setHandler(estimator)
    // train on files in data directory ending in .inc
    val it = corpus.sourceIterator()
    var i = 0
    while (it.hasNext) {
      val inputSource = it.next()
      parser.parse(inputSource)
      i = i + 1
    }
    Console.println("files read: " + i)
    // write model to file
    AbstractExternalizable.compileTo(estimator, modelFile)
  }
}

