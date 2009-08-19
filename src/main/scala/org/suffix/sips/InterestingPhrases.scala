package org.suffix.sips

import com.aliasi.tokenizer.{IndoEuropeanTokenizerFactory,TokenizerFactory}
import com.aliasi.lm.TokenizedLM
import com.aliasi.util.{Files,ScoredObject,AbstractExternalizable}

import java.io.{File,IOException}

import java.util.SortedSet
import scala.collection.jcl.Conversions._

object InterestingPhrases {

    val NGRAM = 3
    val MIN_COUNT = 5
    val MAX_NGRAM_REPORTING_LENGTH = 2
    val NGRAM_REPORTING_LENGTH = 2
    val MAX_COUNT = 100
    
    type ScoredStrings = List[ScoredObject[Array[String]]]

    val hockeyDataDir = "/s/src/java/lingpipe/lingpipe-3.8.2/demos/data/rec.sport.hockey/"
    val BACKGROUND_DIR = new File(hockeyDataDir+"train")
    val FOREGROUND_DIR = new File(hockeyDataDir+"test")


    def main(args: Array[String]) /*:Unit*/ {
	    val tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE

	    println("Training background model")
    	val backgroundModel = buildModel(tokenizerFactory,
    						 NGRAM,
    						 BACKGROUND_DIR)
	
	    backgroundModel.sequenceCounter.prune(3)

	    println("\nAssembling collocations in Training")
	    val coll: ScoredStrings
	      = backgroundModel.collocationSet(NGRAM_REPORTING_LENGTH,
                                             MIN_COUNT,MAX_COUNT).toList

	    println("\nCollocations in Order of Significance:")
	    report(coll)

	    println("Training foreground model")
	    val foregroundModel = buildModel(tokenizerFactory,
						 NGRAM,
						 FOREGROUND_DIR);
	    foregroundModel.sequenceCounter.prune(3)

	    println("\nAssembling New Terms in Test vs. Training")
	    val newTerms 
	      = foregroundModel.newTermSet(NGRAM_REPORTING_LENGTH,
				       MIN_COUNT,
				       MAX_COUNT,
				       backgroundModel).toList

	    println("\nNew Terms in Order of Signficance:")
	    report(newTerms)
		

	    println("\nDone.")
    } 

    def buildModel(tokenizerFactory: TokenizerFactory,
					  ngram: Int,
					  directory: File): TokenizedLM = {

	    val trainingFiles = directory.list
	    val model = new TokenizedLM(tokenizerFactory, ngram)
	    println("Training on "+directory)
		    
	    trainingFiles foreach { (fileName:String) =>
	        val text = Files.readFromFile(new File(directory,
						      fileName), "ISO-8859-1")
	        model.train(text)
      }
		  model
    }

    def report(nGrams: ScoredStrings): Unit = {
      nGrams foreach { nGram =>
  	    val score: Double = nGram.score
  	    val toks: List[String] = nGram.getObject.toList
  	    report_filter(score,toks)
      }
	  }
    
    def report_filter(score: Double, toks: List[String]) /* :Unit */ {
      if (toks forall capWord)
		    println("Score: %5.3e with : %s" format (score, toks.mkString(" ")))
		  else ()
    }

    def capWord(s: String) = s.toList match {
      // case h :: t if ('A' to 'Z' contains h) && (t forall ('a' to 'z' contains _)) => true
      case h :: t if (h.isUpperCase) && (t forall (_.isLowerCase)) => true
      case _ => false
    }

    // no head and tail in String in 2.7
    // def capWord1(s: String) = !s.isEmpty && s(0).isUpperCase && (s.tail forall (_.isLowerCase))
    def capWord1(s: String) = !s.isEmpty && s.first.isUpperCase && (s.drop(1) forall (_.isLowerCase))
}
