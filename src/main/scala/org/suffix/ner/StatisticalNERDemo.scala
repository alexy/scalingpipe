package org.suffix.ner

import java.io.File

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.{asScalaSet, seqAsJavaList}
import scala.collection.mutable.ArrayBuffer

import com.aliasi.chunk.{BioTagChunkCodec, CharLmHmmChunker, Chunk, ChunkFactory, Chunker, ChunkerEvaluator, Chunking, ChunkingImpl, ConfidenceChunker, HmmChunker, NBestChunker}
import com.aliasi.corpus.{ObjectHandler, StringParser}
import com.aliasi.hmm.HmmCharLmEstimator
import com.aliasi.tag.StringTagging
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory
import com.aliasi.util.{AbstractExternalizable, Files, ObjectToSet, ScoredObject, Strings}

object StatisticalNERDemo extends App {

  /////////// Test /////////////
  val inputs = Array[String](
    "p53 regulates human insulin-like growth factor II gene expression through active P4 promoter in rhabdomyosarcoma cells."    
  )
  val MaxNBest = 8

//  val modelFile = new File("models/ne-en-bio-genetag.HmmChunker")
//  Console.println("==== Run Chunker ====")
//  test(modelFile, inputs)
//  Console.println("==== Run NBest Chunker ====")
//  testNBest(modelFile, inputs)
//  Console.println("==== Run Confidence Chunker ====")
//  testConf(modelFile, inputs)
  
  //////////// Train /////////////
  val MaxNGram = 8
  val NumChars = 256
  val LmInterpolation = MaxNGram
  
//  val trainFile = new File("data/medtag/genetag/genetag.tag")
//  val modelFile = new File("models/ne-en-bio-genetag.HmmChunker.local")
//  train(trainFile, modelFile)
  
  //////////// Eval //////////////

  val NumFolds = 10
  
  val goldFile = new File("data/medtag/genetag/Gold.format")
  val testFile = new File("data/medtag/genetag/genetag.sent")
  evaluate(goldFile, testFile, NumFolds)
  
  
  def test(modelFile: File, inputs: Array[String]): Unit = {
    val chunker = AbstractExternalizable.readObject(modelFile).
      asInstanceOf[Chunker]
    for (input <- inputs) {
      Console.println("TEXT: " + input)
      val chunking = chunker.chunk(input)
      for (chunk <- chunking.chunkSet()) {
        val start = chunk.start()
        val end = chunk.end()
        val phrase = input.substring(start, end)
        Console.println("  (" + start + "," + end + "): " + phrase)
      }
    }
  }
  
  def testNBest(modelFile: File, inputs: Array[String]): Unit = {
    val chunker = AbstractExternalizable.readObject(modelFile).
        asInstanceOf[NBestChunker]
    for (input <- inputs) {
      Console.println("TEXT: " + input)
      val cs = input.toCharArray()
      val nbit = chunker.nBest(cs, 0, cs.length, MaxNBest)
      var i = 0
      while (nbit.hasNext()) {
        val scoredObject = nbit.next()
        val jointProb = scoredObject.score()
        val chunking = scoredObject.getObject()
        for (chunk <- chunking.chunkSet()) {
          Console.println(i + " " + jointProb + 
            " (" + chunk.start() + "," + chunk.end + "): " + 
            new String(cs, chunk.start(), chunk.end() - 
            chunk.start()))
        }
        i += 1
      }
    }
  }
  
  def testConf(modelFile: File, inputs: Array[String]): Unit = {
    def chunker = AbstractExternalizable.readObject(modelFile).
        asInstanceOf[ConfidenceChunker]
    for (input <- inputs) {
      Console.println("TEXT: " + input)
      val cs = input.toCharArray()
      val nbit = chunker.nBestChunks(cs, 0, cs.length, MaxNBest)
      var i = 0
      while (nbit.hasNext()) {
        val chunk = nbit.next()
        val confidence = Math.pow(2.0, chunk.score())
        val start = chunk.start()
        val end = chunk.end()
        val phrase = input.substring(start, end)
        Console.println(i + " conf=" + confidence + " (" + 
            start + "," + end + "): " + phrase)
        i += 1
      }
    }
  }
  
  def train(trainFile: File, modelFile: File): Unit = {
    Console.println("Setting up Chunker Estimator")
    val tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE
    val hmmEstimator = 
      new HmmCharLmEstimator(MaxNGram, NumChars, LmInterpolation)
    val chunkerEstimator = 
      new CharLmHmmChunker(tokenizerFactory, hmmEstimator)
    Console.println("Setting up data parser")
    val geneTagParser = new GeneTagParser()
    geneTagParser.setHandler(chunkerEstimator)
    Console.println("Training with data from file: " + trainFile)
    geneTagParser.parse(trainFile)
    Console.println("Compiling and Writing model to " + modelFile)
    AbstractExternalizable.compileTo(chunkerEstimator, modelFile)
  }
  
  def evaluate(goldFile: File, testFile: File, numFolds: Int): Unit = {
    Console.println("Parsing Training Data")
    val accum = new ChunkingAccumulator();
    val parser = new GeneTagChunkParser(goldFile)
    parser.setHandler(accum)
    parser.parse(testFile)
    val chunkings = accum.getChunkings()
    Console.println("Found " + chunkings.length + " chunkings")
    val evalChunker = new EvalChunker()
    val evaluator = new ChunkerEvaluator(evalChunker)
    evaluator.setVerbose(false)
    evaluator.setMaxNBest(128)
    evaluator.setMaxNBestReport(8)
    evaluator.setMaxConfidenceChunks(16)
    for (i <- 0 until numFolds) {
      doEvaluate(evalChunker, evaluator, chunkings, i, numFolds)
    }
    Console.println("Base Evaluation: " + evaluator)
    val prCurve = evaluator.confidenceEvaluation().prCurve(true)
    Console.println("PR Curve")
    for (i <- 0 until prCurve.length) {
      Console.println(prCurve(i)(0) + " " + prCurve(i)(1))
    }
  }
  
  def doEvaluate(evalChunker: EvalChunker, evaluator: ChunkerEvaluator, 
      chunkings: Array[Chunking], fold: Int, numFolds: Int): Unit = {
    Console.println("Evaluating fold=" + (fold + 1) + " of " + numFolds)
    val tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE
    val hmmEstimator = new HmmCharLmEstimator(MaxNGram, NumChars, LmInterpolation)
    val chunkerEstimator = new CharLmHmmChunker(tokenizerFactory, hmmEstimator)
    for (i <- 0 until numFolds) {
      if (i != fold) {
        Console.println("    train, fold=" + (i + 1))
        visitFold(chunkings, i, numFolds, chunkerEstimator)
      }
    }
    Console.println("    compiling")
    evalChunker.chunker = AbstractExternalizable.compile(
      chunkerEstimator).asInstanceOf[HmmChunker]
    visitFold(chunkings, fold, numFolds, evaluator)
  }
  
  def visitFold(chunkings: Array[Chunking], fold: Int, numFolds: Int,
      handler: ObjectHandler[Chunking]): Unit = {
    val start = startFold(chunkings.length, fold, numFolds)
    val end = startFold(chunkings.length, fold + 1, numFolds)
    for (i <- start until end) {
      handler.handle(chunkings(i))
    }
  }
  
  def startFold(len: Int, fold: Int, numFolds: Int): Int = {
    (len * fold) / numFolds
  }
}

class GeneTagParser extends StringParser[ObjectHandler[Chunking]] {
  
  override def parseString(cs: Array[Char], start: Int, end: Int): Unit = {
    val text = new String(cs, start, end - start)
    val sentences = text.split("\n")
    for (sentence <- sentences) {
      if (!(Strings.allWhitespace(sentence) || 
          sentence.indexOf('_') < 0)) {
        val words = sentence.split(" ")
        val tokens = words.map(x => x.split("_")(0))
        val tags = normalizeTags(words.map(x => x.split("_")(1)))
        val whitespaces = new Array[String](tokens.length + 1)
        whitespaces.map(x => " ")
        whitespaces(0) = ""
        whitespaces(whitespaces.length - 1) = ""
        val codec = new BioTagChunkCodec()
        val starts = new Array[Int](tokens.length)
        val ends = new Array[Int](tokens.length)
        val buf = new StringBuilder()
        for (i <- 0 until tokens.length) {
          starts(i) = buf.length()
          buf.append(tokens(i))
          ends(i) = buf.length()
          if (i + 1 < tokens.length) {
            buf.append(" ")
          }
        }
        val tagging = new StringTagging(tokens.toList, tags, 
          buf.toString(), starts, ends)
        val chunking = codec.toChunking(tagging)
        getHandler().handle(chunking)
      }
    }
  }
  
  def normalizeTags(rawTags: Array[String]): List[String] = {
    val normalized = ArrayBuffer[String]()
    var i = 0
    while (i < rawTags.length) {
      if (rawTags(i).startsWith("GENE")) {
        val tag = rawTags(i)
        normalized += "B_GENE"
        i += 1
        while (i < rawTags.length && 
            tag.equals(rawTags(i))) {
          normalized += "I_GENE"
          i += 1
        }
      } else {
        normalized += "O"
        i += 1
      }
    }
    normalized.toList
  }
}

class ChunkingAccumulator extends ObjectHandler[Chunking] {
  
  val chunkingSet = scala.collection.mutable.Set[Chunking]()
  
  def handle(chunking: Chunking) {
    chunkingSet.add(chunking)
  }
  
  def getChunkings(): Array[Chunking] = {
    chunkingSet.toArray
  }
}

class EvalChunker extends Chunker with NBestChunker 
    with ConfidenceChunker {
  
  var chunker: HmmChunker = null
  
  override def chunk(cs: CharSequence): Chunking = {
    chunker.chunk(cs)
  }
  
  override def chunk(cs: Array[Char], start: Int, end: Int): 
      Chunking = {
    chunker.chunk(cs, start, end)
  }
  
  override def nBestChunks(cs: Array[Char], start: Int, end: Int, 
      max: Int): java.util.Iterator[Chunk] = {
    chunker.nBestChunks(cs, start, end, max)
  }
  
  override def nBest(cs: Array[Char], start: Int, end: Int, 
      max: Int): java.util.Iterator[ScoredObject[Chunking]] = {
    chunker.nBest(cs, start, end, max)
  }
}

class GeneTagChunkParser(goldFile: File, handler: ObjectHandler[Chunking]) 
    extends StringParser[ObjectHandler[Chunking]](handler) {
  
  val idToChunkSet = new ObjectToSet[String,Chunk]()
  readChunks(goldFile)
  
  def this(goldFile: File) { this(goldFile, null) }
  
  def getChunkHandler(): ObjectHandler[Chunking] = {
    getHandler()
  }
  
  def parseString(cs: Array[Char], start: Int, end: Int): Unit = {
    val text = new String(cs, start, end - start)
    val lines = text.split("\n")
    for (i <- 0 until (lines.length - 1) by 2) {
      val sentenceId = lines(i)
      val sentence = lines(i + 1)
      if (sentence.length() > 0) {
        val mapping = new Array[Int](sentence.length())
        var target = 0
        for (k <- 0 until mapping.length) {
          if (sentence.charAt(k) != ' ') {
            mapping(target) = k
            target += 1
          }
        }
        val chunking = new ChunkingImpl(sentence)
        for (nextChunk <- idToChunkSet.getSet(sentenceId)) {
          val chunkStart = mapping(nextChunk.start())
          val chunkEnd = mapping(nextChunk.end())
          val remappedChunk = ChunkFactory.createChunk(
            chunkStart, chunkEnd + 1, "GENE")
          chunking.add(remappedChunk)
        }
        getChunkHandler().handle(chunking)
      }
    }
  }
  
  def readChunks(goldFile: File): Unit = {
    val text = Files.readFromFile(goldFile, "ASCII")
    val lines = text.split("\n")
    for (line <- lines) {
      val cols = line.split("\\|")
      if (cols.length > 0) {
        val sentenceId = cols(0)
        val positions = cols(1).split(" ")
        val start = positions(0).toInt
        val end = positions(1).toInt
        val phrase = cols(2)
        val chunk = ChunkFactory.createChunk(start, end)
        idToChunkSet.addMember(sentenceId, chunk)
      }
    }
  }
}