package org.suffix.spell

import java.io.{BufferedInputStream, BufferedOutputStream, File, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.NIOFSDirectory
import org.apache.lucene.util.Version

import com.aliasi.lm.NGramProcessLM
import com.aliasi.spell.{CompiledSpellChecker, FixedWeightEditDistance, TrainSpellChecker}
import com.aliasi.tokenizer.{IndoEuropeanTokenizerFactory, LowerCaseTokenizerFactory}
import com.aliasi.util.{Files, Streams}

object SpellCheckDemo extends App {

  val MatchWeight = -0.0
  val DeleteWeight = -4.0
  val InsertWeight = -1.0
  val SubstituteWeight = -2.0
  val TransposeWeight = -2.0
  val MaxHits = 100
  val NGramLength = 5
  
  val trainDir = new File("data/rec.sport.hockey/train")
  val luceneDir = new File("data/spellIndex")
  val modelFile = new File("models/spellChecker.bin")
  
  val editDist = new FixedWeightEditDistance(MatchWeight,
    DeleteWeight, InsertWeight, SubstituteWeight, TransposeWeight)
  val lm = new NGramProcessLM(NGramLength)
  val factory = new LowerCaseTokenizerFactory(
    IndoEuropeanTokenizerFactory.INSTANCE)
  val spellChecker = new TrainSpellChecker(lm, editDist, factory)
  val analyzer = new StandardAnalyzer(Version.LUCENE_42)

  Console.println("Writing Lucene index and spelling model...")
  writeIndexDocs(luceneDir, trainDir, analyzer)
  writeModel(spellChecker, modelFile)
  
  Console.println("Reading compiled spelling model...")
  val compiledSpellChecker = readModel(modelFile)
  compiledSpellChecker.setTokenizerFactory(factory)
  val searcher = new IndexSearcher(DirectoryReader.open(
    new NIOFSDirectory(luceneDir)))
  
  /////////////////// test model //////////////////
  val queryParser = new QueryParser(Version.LUCENE_42, 
    "text", analyzer)
  val testSpellings = Array[(String,String)](
    ("Canad dian", "Canadian"),
    ("CanadianHockey", "Canadian Hockey"),
    ("hokey", "hockey"),
    ("wayne gretski", "Wayne Gretzky"),
    ("Calgary", "Calgary"),
    ("hky", "hockey"),
    ("exuberation", "exuberation")
  )
  test(testSpellings, queryParser, compiledSpellChecker)
  
  def test(testSpellings: Array[(String,String)], 
      queryParser: QueryParser,
      model: CompiledSpellChecker): Unit = {
  testSpellings.foreach(testSpelling => {
    val query = queryParser.parse(testSpelling._1)
    val results = searcher.search(query, MaxHits)
    Console.println("Found " + results.totalHits + 
      " documents matching query: \"" + testSpelling._1 + "\"")
    val bestAlternative = 
      compiledSpellChecker.didYouMean(testSpelling._1)
    if (bestAlternative.equals(testSpelling._2)) {
      Console.println("No spelling mistake")
    } else {
      val altQuery = queryParser.parse(bestAlternative)
      val results = searcher.search(altQuery, MaxHits)
      Console.println("Found " + results.totalHits + 
        " documents matching query: \"" + bestAlternative + "\"")
    }
  })
    
  }
  
  def writeIndexDocs(luceneDir: File, trainDir: File, 
      analyzer: Analyzer): Unit = {
    if (! luceneDir.exists()) {
      Console.println("Writing Lucene index...")
      val iwconf = new IndexWriterConfig(Version.LUCENE_42, analyzer)
      iwconf.setOpenMode(IndexWriterConfig.OpenMode.CREATE)
      val indexDir = new NIOFSDirectory(luceneDir)
      val indexWriter = new IndexWriter(indexDir, iwconf)
      trainDir.listFiles().foreach(file => {
        val text = Files.readFromFile(file, "ISO-8859-1")
        spellChecker.handle(text)
        val luceneDoc = new Document()
        luceneDoc.add(new Field("text", text, 
          Field.Store.YES, Field.Index.ANALYZED))
        indexWriter.addDocument(luceneDoc)
      })
      indexWriter.commit()
      indexWriter.close()
    }
  }

  def writeModel(model: TrainSpellChecker, modelFile: File): Unit = {
    if (! modelFile.exists()) {
      val ostream = new ObjectOutputStream(
        new BufferedOutputStream(
        new FileOutputStream(modelFile)))
      model.compileTo(ostream)
      Streams.closeQuietly(ostream)
    }
  }

  def readModel(modelFile: File): CompiledSpellChecker = {
    val istream = new ObjectInputStream(
      new BufferedInputStream(
      new FileInputStream(modelFile)))
    val csc = istream.readObject().asInstanceOf[CompiledSpellChecker]
    Streams.closeQuietly(istream)
    csc
  }
}