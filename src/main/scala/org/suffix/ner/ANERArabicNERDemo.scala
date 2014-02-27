package org.suffix.ner

import java.io.File

import scala.collection.JavaConversions.{asScalaSet, bufferAsJavaList}
import scala.collection.mutable.ArrayBuffer

import com.aliasi.chunk.{BioTagChunkCodec, CharLmRescoringChunker, Chunker, ChunkerEvaluator, Chunking}
import com.aliasi.io.FileLineReader
import com.aliasi.tag.StringTagging
import com.aliasi.tokenizer.RegExTokenizerFactory
import com.aliasi.util.{AbstractExternalizable, Strings}

object ANERArabicNERDemo extends App {

  val NumFolds = 6
  val LocTag = "LOC"
  val PersTag = "PERS"
  val OrgTag = "ORG"
  val MiscTag = "MISC"
  val ReportTypes = Array[String](LocTag, PersTag, OrgTag, MiscTag)
  val TokenizerRegex = "\\S+"

  val UseDictionary = false
  val IncludeMisc = true
  val HmmNGramLength = 9
  val HmmInterpolationRatio = 8
  val NumChars = 1024
  val NumAnalysesRescored = 512
  
  val corpusFile = new File("data/ANER/ANERCorp")
  val locGazFile = new File("data/ANER/ANERGazet/AlignedLocGazetteer")
  val orgGazFile = new File("data/ANER/ANERGazet/AlignedOrgGazetteer")
  val perGazFile = new File("data/ANER/ANERGazet/AlignedPersGazetteer")
  
  val tokenizerFactory = new RegExTokenizerFactory(TokenizerRegex)
  val locDict = FileLineReader.readLineArray(locGazFile, Strings.UTF8)
  val orgDict = FileLineReader.readLineArray(orgGazFile, Strings.UTF8)
  val perDict = FileLineReader.readLineArray(perGazFile, Strings.UTF8)
  Console.println("Corpus Statistics:" +
    "\n    Location Dict Entries: " + locDict.length +
    "\n    Organization Dict Entries: " + orgDict.length + 
    "\n    Person Dict Entries: " + perDict.length)
  
  val sentences = parseANER(corpusFile)
  val chunkTypeSet = scala.collection.mutable.Set[String]()
  for (chunking <- sentences) {
    for (chunk <- chunking.chunkSet()) {
      chunkTypeSet += chunk.`type`()
    }
  }
  val numSentences = sentences.size()
  val evaluator = new ChunkerEvaluator(null)
  evaluator.setMaxConfidenceChunks(1)
  evaluator.setMaxNBest(1)
  evaluator.setVerbose(false)
  for (fold <- NumFolds to 0 by -1) {
    val startTestFold = (fold * numSentences) / NumFolds
    val endTestFold = if (fold == NumFolds) numSentences 
      else ((fold + 1) * numSentences) / NumFolds
    Console.println("FOLD=" + fold + 
      "  start sent=" + startTestFold + 
      "  end sent=" + endTestFold)
    val foldEvaluator = new ChunkerEvaluator(null)
    foldEvaluator.setMaxConfidenceChunks(1)
    foldEvaluator.setMaxNBest(1)
    foldEvaluator.setVerbose(false)
    
    val chunker = new CharLmRescoringChunker(
      tokenizerFactory, NumAnalysesRescored, 
      HmmNGramLength, NumChars, 
      HmmInterpolationRatio, true)
    Console.println("Training Labeled Data")
    for (i <- 0 until startTestFold) {
      chunker.handle(sentences(i))
    }
    for (i <- endTestFold until numSentences) {
      chunker.handle(sentences(i))
    }
    if (UseDictionary) {
      Console.println("Training Dictionary")
      locDict.foreach(chunker.trainDictionary(_, LocTag))
      orgDict.foreach(chunker.trainDictionary(_, OrgTag))
      perDict.foreach(chunker.trainDictionary(_, PersTag))
    }
    Console.println("Compiling")
    val compiledChunker = AbstractExternalizable.compile(chunker).
      asInstanceOf[Chunker]
    evaluator.setChunker(compiledChunker)
    foldEvaluator.setChunker(compiledChunker)
    Console.println("Evaluating")
    for (i <- startTestFold until endTestFold) {
      try {
        evaluator.handle(sentences(i))
        foldEvaluator.handle(sentences(i))
      } catch {
        case e: Exception => {
          Console.println(sentences(i))
          e.printStackTrace();
        }
      }
    }
    printEval("FOLD=" + fold, foldEvaluator)
  }
  Console.println("Combined Cross-validation results")
  printEval("X-Val", evaluator)
  
  def parseANER(corpusFile: File): ArrayBuffer[Chunking] = {
    val lines = FileLineReader.readLineArray(corpusFile, Strings.UTF8)
    val sentences = ArrayBuffer[Chunking]()
    var tokens = ArrayBuffer[String]()
    var tags = ArrayBuffer[String]()
    var numTokens = 0
    for (line <- lines) {
      val pos = line.lastIndexOf(' ')
      var token: String = null
      var tag: String = null
      if (pos < 0) {
        if (line.equals(".O")) {
          token = "."
          tag = "O"
        }
      } else {
        token = line.substring(0, pos)
        tag = line.substring(pos + 1)
      }
      if (!IncludeMisc && tag.indexOf(MiscTag) >= 0) {
        tag = "O"
      }
      tokens += token
      tags += tag
      numTokens += 1
      if (isEos(token, tag)) {
        sentences += toChunking(tokens, tags)
        tokens.clear()
        tags.clear()
      }
    }
    sentences += toChunking(tokens, tags)
    Console.println("  # sentences=" + sentences.size() + ", # tokens=" + numTokens)
    sentences
  }

  def toChunking(tokens: ArrayBuffer[String], tags: ArrayBuffer[String]): Chunking = {
    val codec = new BioTagChunkCodec(null, false, "B-", "I-", "O")
    val buf = new StringBuilder()
    val tokenStarts = new Array[Int](tokens.size)
    val tokenEnds = new Array[Int](tokens.size)
    for (i <- 0 until tokens.size) {
      if (i > 0) buf.append(" ")
      tokenStarts(i) = buf.length()
      buf.append(tokens(i))
      tokenEnds(i) = buf.length()
    }
    val tagging = new StringTagging(tokens, tags, buf, 
      tokenStarts, tokenEnds)
    val chunking = codec.toChunking(tagging)
    chunking
  }
  
  def isEos(token: String, tag: String): Boolean = {
    "O".equals(tag) &&
        (".".equals(token) || "!".equals(token))
  }
  
  def printEval(msg: String, evaluator: ChunkerEvaluator): Unit = {
    val chunkingEval = evaluator.evaluation()
    for (tag <- ReportTypes) {
      val prEvalByType = chunkingEval.perTypeEvaluation(tag).
        precisionRecallEvaluation()
      Console.println("%10s    %6s P=%5.3f R=%5.3f F=%5.3f".
        format(msg, tag, prEvalByType.precision(), 
        prEvalByType.recall(), prEvalByType.fMeasure()))
    }
    val prEval = chunkingEval.precisionRecallEvaluation()
    Console.println("%10s  COMBINED P=%5.3f R=%5.3f F=%5.3f".
      format(msg, prEval.precision(), prEval.recall(),
      prEval.fMeasure()))
  }
}