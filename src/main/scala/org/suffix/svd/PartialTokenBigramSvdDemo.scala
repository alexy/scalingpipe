package org.suffix.svd

import java.io.{File, OutputStreamWriter, PrintWriter}

import com.aliasi.io.{LogLevel, Reporters}
import com.aliasi.matrix.SvdMatrix
import com.aliasi.symbol.MapSymbolTable
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory

object PartialTokenBigramSvdDemo extends App {

  val MaxFactors = 3
  val FeatureInit = 1.0
  val InitialLearningRate = 0.002
  val AnnealingRate = 100
  val Regularization = 0.0
  val MinImprovement = 0.0000
  val MinEpochs = 10
  val MaxEpochs = 10000
  
  val inputFile = new File("data/alice-gutenberg.txt")
  val symbolTable = new MapSymbolTable()
  val factory = IndoEuropeanTokenizerFactory.INSTANCE
  val bigrams = TokenBigramSvdDemo.extractBigrams(
    inputFile, symbolTable, factory)
  val colIds = columnIds(bigrams)
  val partValues = partialValues(bigrams)
  
  val reporter = Reporters.writer(new PrintWriter(
    new OutputStreamWriter(System.out))).
    setLevel(LogLevel.DEBUG)
  val svdMatrix = SvdMatrix.partialSvd(
    colIds, partValues, MaxFactors, 
    FeatureInit, InitialLearningRate, AnnealingRate,
    Regularization, reporter, MinImprovement, 
    MinEpochs, MaxEpochs)
  TokenBigramSvdDemo.reportSvd(bigrams, svdMatrix, symbolTable)
  
  def columnIds(bigrams: Array[Array[Double]]): Array[Array[Int]] = {
    var columnIds = new Array[Array[Int]](bigrams.length)
    for (i <- 0 until columnIds.length)
      columnIds(i) = columnIdsRow(bigrams(i))
    columnIds
  }
  
  def columnIdsRow(bigram: Array[Double]): Array[Int] = {
    var count = 0
    for (i <- 0 until bigram.length)
      if (bigram(i) != 0) count += 1
    var columnIdsRow = new Array[Int](count)
    count = 0
    for (i <- 0 until bigram.length) { 
      if (bigram(i) != 0) {
        columnIdsRow(count) = i
        count += 1
      }
    }
    columnIdsRow
  }
  
  def partialValues(bigrams: Array[Array[Double]]): 
      Array[Array[Double]] = {
    var partialValues = new Array[Array[Double]](bigrams.length)
    for (i <- 0 until bigrams.length) 
      partialValues(i) = partialValuesRow(bigrams(i))
    partialValues
  }
  
  def partialValuesRow(bigram: Array[Double]): Array[Double] = {
    var count = 0
    for (i <- 0 until bigram.length) 
      if (bigram(i) != 0) count += 1
    var partialValuesRow = new Array[Double](count)
    count = 0
    for (i <- 0 until bigram.length) {
      if (bigram(i) != 0) {
        partialValuesRow(count) = bigram(i)
        count += 1
      }
    }
    partialValuesRow
  }
}