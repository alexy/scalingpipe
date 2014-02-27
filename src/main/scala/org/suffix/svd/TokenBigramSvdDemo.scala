package org.suffix.svd

import java.io.{File, OutputStreamWriter, PrintWriter}

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asScalaBuffer

import com.aliasi.io.{LogLevel, Reporters}
import com.aliasi.matrix.SvdMatrix
import com.aliasi.symbol.MapSymbolTable
import com.aliasi.tokenizer.{IndoEuropeanTokenizerFactory, TokenizerFactory}
import com.aliasi.util.{Files, ObjectToDoubleMap, Strings}

object TokenBigramSvdDemo extends App {

  val MaxFactors = 3
  val FeatureInit = 0.1
  val InitialLearningRate = 0.001
  val AnnealingRate = 100
  val Regularization = 0.00
  val MinImprovement = 0.0000
  val MinEpochs = 10
  val MaxEpochs = 200

  val inputFile = new File("data/alice-gutenberg.txt")
  
  val symbolTable = new MapSymbolTable()
  val factory = IndoEuropeanTokenizerFactory.INSTANCE
  val bigrams = extractBigrams(inputFile, symbolTable, factory)
  val reporter = Reporters.writer(
    new PrintWriter(new OutputStreamWriter(System.out, "ASCII"))).
    setLevel(LogLevel.DEBUG)
  val matrix = SvdMatrix.svd(bigrams, 
    MaxFactors, FeatureInit, InitialLearningRate, 
    AnnealingRate, Regularization, reporter, 
    MinImprovement, MinEpochs, MaxEpochs)
  reportSvd(bigrams, matrix, symbolTable)
  
  def extractBigrams(file: File, symbolTable: MapSymbolTable,
      factory: TokenizerFactory): Array[Array[Double]] = {
    val chars = Files.readCharsFromFile(file, "ASCII")
    val tokens = factory.tokenizer(chars, 0, chars.length).tokenize()
    val symbols = tokens.map(token => {
      if (Strings.allLetters(token.toCharArray()))
        symbolTable.getOrAddSymbol(token)
      else -1
    })
    val numSymbols = symbolTable.numSymbols()
    Console.println("  # Distinct tokens=" + numSymbols)
    Console.println("  # Matrix entries=" + (numSymbols * numSymbols))
    val bigrams = Array.ofDim[Double](numSymbols, numSymbols)
    for (i <- 0 until numSymbols) {
      val left = symbols(i)
      val right = symbols(i + 1)
      if (left >= 0 && right >= 0)
        bigrams(left)(right) += 1.0
    }
    bigrams
  }
  
  def reportSvd(bigrams: Array[Array[Double]], 
      matrix: SvdMatrix, symbolTable: MapSymbolTable): Unit = {
    val singularVals = matrix.singularValues()
    val leftSingularVecs = matrix.leftSingularVectors()
    val rightSingularVecs = matrix.rightSingularVectors()
    for (i <- 0 until singularVals.length) {
      Console.println("singularValues(" + i + 
          ")=" + singularVals.toList)
      Console.println("Extreme Left Values")
      extremeValues(leftSingularVecs, i, symbolTable)
      Console.println("Extreme RIght Values")
      extremeValues(rightSingularVecs, i, symbolTable)
    }
    val topPairCounts = new ObjectToDoubleMap[String]()
    val numSymbols = symbolTable.numSymbols()
    for (i <- 0 until numSymbols;
         j <- 0 until numSymbols) {
      if (bigrams(i)(j) != 0)
        topPairCounts.set(symbolTable.idToSymbol(i) + "," + 
          symbolTable.idToSymbol(j), bigrams(i)(j))
    }
    val numPairs = topPairCounts.size()
    Console.println("# Unique pairs=" + numPairs)
    val pairsByCount = topPairCounts.keysOrderedByValueList()
    pairsByCount.subList(0, 25).foreach(pair => {
      Console.println("    " + pair + " count=" + 
        topPairCounts.getValue(pair))
    })
    Console.println("Reconstructed top counts")
    Console.println("LeftToken,RightToken OriginalValue SvdValue")
    pairsByCount.subList(0, 25).foreach(pair => {
      val tokPair = pair.split(",")
      val leftId = symbolTable.symbolToID(tokPair(0))
      val rightId = symbolTable.symbolToID(tokPair(1))
      val originalVal = topPairCounts.getValue(pair)
      val reconstructedVal = matrix.value(leftId, rightId)
      Console.println(pair + " " + originalVal + " " + reconstructedVal)
    })
  }
  
  def extremeValues(bigrams: Array[Array[Double]], 
      order: Int, symbolTable: MapSymbolTable): Unit = {
    val topBigrams = new ObjectToDoubleMap[String]()
    for (i <- 0 until bigrams.length) {
      val token = symbolTable.idToSymbol(i)
      topBigrams.set(token, bigrams(i)(order))
    }
    val tokensByValue = topBigrams.keysOrderedByValueList()
    val size = tokensByValue.size()
    var curr = 0
    tokensByValue.subList(0, 10).foreach(token => {
      Console.println("    %6d %-15s %5.3f".
        format(curr, token, topBigrams.getValue(token)))
      curr += 1
    })
    Console.println("...")
    curr = size - 10
    tokensByValue.subList(size - 10, size).foreach(token => {
      Console.println("    %6d %-15s %5.3f".
        format(curr, token, topBigrams.getValue(token)))
      curr += 1
    })
  }
}