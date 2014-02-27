package org.suffix.cluster

import java.io.File
import java.util.Random

import scala.collection.JavaConversions.setAsJavaSet
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

import com.aliasi.cluster.LatentDirichletAllocation
import com.aliasi.symbol.MapSymbolTable
import com.aliasi.tokenizer.{EnglishStopTokenizerFactory, LowerCaseTokenizerFactory, ModifyTokenTokenizerFactory, RegExTokenizerFactory, StopTokenizerFactory, TokenizerFactory}

object LdaWormBaseDemo extends App {

  val MinTokenCount = 5
  val NumTopics: Short = 50
  val TopicPrior = 0.1
  val WordPrior = 0.01
  val BurnInEpochs = 0
  val SampleLag = 1
  val NumSamples = 2000
  val RandomSeed = 42
  val MaxWordsPerTopic = 200
  val MaxTopicsPerDoc = 10
  
  val inputFile = new File("data/2007-12-01-wormbase-literature.endnote")
  val texts = readCorpus(inputFile)
  val symbolTable = new MapSymbolTable()
  val tokFactory = wormbaseTokenizerFactory()
  val docTokens = LatentDirichletAllocation.tokenizeDocuments(
    texts, tokFactory, symbolTable, MinTokenCount)
  Console.println("Number of unique words above count threshold=" +
    symbolTable.numSymbols())
  val numTokens = docTokens.
    foldLeft(0)((sum, tokens) => sum + tokens.length)
  Console.println("Tokenized. #-Tokens after pruning=" + numTokens)
  val handler = new LdaReportingHandler(symbolTable)
  val sample = LatentDirichletAllocation.gibbsSampler(
    docTokens, NumTopics, TopicPrior, WordPrior, 
    BurnInEpochs, SampleLag, NumSamples, new Random(RandomSeed), 
    handler)
  handler.fullReport(sample, MaxWordsPerTopic, MaxTopicsPerDoc, false)
  
  def readCorpus(file: File): Array[CharSequence] = {
    val texts = ArrayBuffer[String]()
    Source.fromFile(file).getLines().foreach(line => {
      val buf = new StringBuilder()
      if (line.startsWith("%T")) 
        buf.append(line.substring(3))
      else if (line.startsWith("%X")) 
        buf.append(" ").append(line.substring(3))
      else if (line.trim().length() == 0)
        texts += buf.toString()
        buf.clear()
    })
    texts.toArray
  }
  
  def wormbaseTokenizerFactory(): TokenizerFactory = {
    var factory: TokenizerFactory =  // letter/digit/hyphen
      new RegExTokenizerFactory("[\\x2Da-zA-Z0-9]+")
    factory = new NonAlphaStopTokenizerFactory(factory)
    factory = new LowerCaseTokenizerFactory(factory)
    factory = new EnglishStopTokenizerFactory(factory)
    factory = new StopTokenizerFactory(factory, stopwordSet())
    factory = new StemTokenizerFactory(factory)
    factory
  }
  
  def stopwordSet(): Set[String] = Set[String](
    "these", "elegan", "caenorhabditi",
    "both", "may", "between", "our", "et", "al", "however", "many",
    "thu", "thus", "how", "while", "same", "here", "although", "those",
    "might", "see", "like", "likely", "where", "i", "ii", "iii", "iv",
    "v", "vi", "vii", "viii", "ix", "x", "zero", "one", "two", "three",
    "four", "five", "six", "seven", "eight", "nine", "ten", "eleven",
    "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
    "eighteen", "nineteen", "twenty", "thirty", "forty", "fifty", "sixty",
    "seventy", "eighty", "ninety", "hundred", "thousand", "million")
}

class NonAlphaStopTokenizerFactory(factory: TokenizerFactory) 
    extends ModifyTokenTokenizerFactory(factory) {
  
  override def modifyToken(token: String): String = {
    if (stop(token)) null else token
  }
  
  def stop(token: String): Boolean = {
    if (token.length() < 2) true
    else {
      val letters = token.toCharArray().
        filter(ch => Character.isLetter(ch))
      if (letters.length != token.length()) true
      else false
    }
  }
}

class StemTokenizerFactory(factory: TokenizerFactory)
    extends ModifyTokenTokenizerFactory(factory) {
  
  val Vowels = Set[Char]('a', 'e', 'i', 'o', 'u', 'y')
  val suffixes = Array[String]("ss", "ies", "sses", "s")
  
  override def modifyToken(token: String): String = {
    var stemmed = false
    var stem: String = null
    suffixes.foreach(suffix => {
      if ((! stemmed) && token.endsWith(suffix)) {
        stem = token.substring(0, token.length() - suffix.length())
        stem = if (validStem(stem)) stem else token
        stemmed = true
      }
    })
    if (stem == null) token else stem
  }
  
  def validStem(stem: String): Boolean = {
    if (stem.length() < 2) false
    else {
      val vowels = stem.toCharArray().
        filter(ch => Vowels.contains(ch))
      if (vowels.length > 0) true
      else false
    }
  }
}