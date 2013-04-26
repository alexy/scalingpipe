ScalingPipe is a sample of Scala drivers for LingPipe, the famous Machine Learning Natural Language Processing Library, developed by Bob Carpenter:

http://lingpipe.com/

We use SBT (Simple Build Tool) to install Scala and other dependencies. Since LingPipe is not open-source, you need to agree to its license and then manually install the lingpipe jar file into the lib directory as shown below.

	cd $PROJECT_ROOT (this directory)
	mkdir -p lib
	cp $LINGPIPE_DIST_DIR/lingpipe-*.jar lib

The code expects to see the demo data that is provided along with the LingPipe 4.1.0 distribution in the data directory, so you should link it in like so. Some of the code also expects to see the pre-built models supplied (the demos dont produce all the models they consume, so you may want to copy the models over to your local (to your project) models directory. Alternatively, if you are feeling more confident (ie, you know you can regenerate the models), then you can just link it in.

	cd $PROJECT_ROOT (this directory)
	ln -s $LINGPIPE_DIST_DIR/demos/data .
	ln -s $LINGPIPE_DIST_DIR/demos/models .

The code is based on the demos code from the LingPipe 4.1.0 distribution and consists of the following packages.

## String Comparison

Code demonstrating various string distance measures available in LingPipe. The LingPipe demo code is in demos/stringCompare, the scalingpipe equivalent is in the package o.s.strcmp Read the LingPipe [String Comparison Tutorial](http://alias-i.com/lingpipe/demos/tutorial/stringCompare/read-me.html) for more details.

## Sentence Detection

Code demonstrating sentence splitting and chunking, evaluating and tuning sentence models. The LingPipe demo code is in demos/sentences, the scalingpipe equivalent is in the package o.s.sentence. Read the LingPipe [Sentences Tutorial](http://alias-i.com/lingpipe/demos/tutorial/sentences/read-me.html) for more details.

The evaluation and tuning demos require the [GENIA Corpus (TGZ)](http://www.nactem.ac.uk/genia/genia-corpus/term-corpus) to be downloaded - the link in the original LingPipe docs is outdated. The TGZ file can be expanded into the demos/data directory of the LingPipe distribution.

## Part of Speech Tagging

Code demonstrating part of speech tagging and phrase chunking. The LingPipe demo code is in demos/posTags, the scalingpipe equivalent is in the package o.s.postag. Read the LingPipe [Part of Speech Tutorial](http://alias-i.com/lingpipe/demos/tutorial/posTags/read-me.html) for more details.

You will need to download the Brown Corpus from NLTK, the GENIA POS corpus from GENIA and the MedPost corpus from Medpost. The GENIA URL is outdated and should be [this one](http://www.nactem.ac.uk/genia/genia-corpus/pos-annotation). All of the corpora should be downloaded into the LingPipe demos/data directory.

## Named Entity Recognition

Code demonstrating building and evaluating Named Entiy Recognizers. The LingPipe demo code is in demos/ne, the scalingpipe equivalent is in the package o.s.ner. Read the LingPipe [Named Entity Tutorial](http://alias-i.com/lingpipe/demos/tutorial/ne/read-me.html) for more details.

The Gene NER (StatisticalNERDemo) uses the genetag data from the MedPost dataset that was needed for the POS Tagging code. Additionally you need to download the CoNLL Spanish NER (CoNLLSpanishNERDemo), gunzip and manually fix 3 of the of I tags to B tags (see the tutorial for files and line numbers). For the Arabic NER you have to download the ANER corpus and gazetteer and gunzip the gazetteers.

## Word Sense Disambiguation

Code for parsing Senseval training and test files. Multiple senses for each ambiguous word is provided, along with test sentences containing the usage. The LingPipe code is in demos/wordSense, the scalingpipe equivalent is in the package o.s.wsd. Read the LingPipe [Word Sense Tutorial](http://alias-i.com/lingpipe/demos/tutorial/wordSense/read-me.html) for more details. You will need to download the Senseval files, links are on the LingPipe tutorial page. The scalingpipe code expects them in the data/senseval-en directory.

## Expectation Maximization 

Implements the Expectation Maximization Algorithm to classify 20 newsgroup text. LingPipe code is in demos/em, the scalingpipe equivalent is in the package o.s.em. Read the LingPipe [Expectation Maximization Tutorial](http://alias-i.com/lingpipe/demos/tutorial/em/read-me.html) for more details. You will need to download the 20 newsgroup data (links on the LingPipe tutorial page), and the scalingpipe code expects it under the directory data/20newsgroups. Note that the code takes an extremely long time to complete - this is expected, the Java version takes about 4 hours to complete per the tutorial page.

## Character Language Modeling 

Demo code demonstrating usage of a Character and Token Language model. Both models are trained on the medline/medsamp2010.xml file provided with the LingPipe distribution, and are used to calculate probability of unseen sentences. The code differs from that in the LingPipe demos, but the information in [LingPipe Character LM Tutorial](http://alias-i.com/lingpipe/demos/tutorial/lm/read-me.html) should still prove useful. LingPipe code is in demos/lm, scalingpipe code is in o.s.langmodel.

## Singular Value Decomposition

Demo code for SVD and LSI functionality in LingPipe. LingPipe demo code is in demos/svd and corresponding scalingpipe code is in o.s.svd. More information in the [LingPipe SVD Tutorial](http://alias-i.com/lingpipe/demos/tutorial/svd/read-me.html). Token Bigram SVD Demos use a single file from the Gutenberg Project - LingPipe tutorial uses Pride and Prejudice, scalingpipe demo uses Alice in Wonderland (already had that lying around).

## Conditional Random Fields - crf

## Database Text Mining

Contains two demo programs to load up Medline abstracts into a MySQL database, then annotate sentences from it using a LingPipe chunking model. The annotations are also written out to the database, at which point you can use SQL to count and group them, etc. LingPipe code is in demos/db, corresponding scalingpipe code is in o.s.dbmining. Read the [LingPipe DB Mining Tutorial](http://alias-i.com/lingpipe/demos/tutorial/db/read-me.html) for more information.

You will need a MySQL server to run this example. The SQL to build the database and tables can be found in src/main/sql/dbmining_schema.sql. The code also needs a new JAR file lingmed-1.3.jar provided in the LingPipe demos/lib directory - this needs to be copied to the lib directory of the scalingpipe project. It also needs the MySQL client JAR file (loaded automatically by build.sbt). It also needs a TokenShapeChunker model that is not available from the LingPipe distribution, you can get it from the [ScoobyDoobyDoo project](https://github.com/kjyv/ScoobyDoobyDoo). Finally the code looks for demos/data/medsamp2006.xml but the data shipped with the LingPipe distribution is in demos/data/medline/medsamp2010.xml and the code (both Java and Scala) fails with MalformedURLException when trying to parse it - to fix, remove the DOCTYPE line from the XML file.

## Interesting Phrase Detection

Finds interesting phrases based on collocation frequency of a word pair being higher than expected. The LingPipe code is in demos/interestingPhrases, the correspnding scalingpipe code is in the package o.s.sips. Read the [LingPipe Phrases Tutorial](http://alias-i.com/lingpipe/demos/tutorial/interestingPhrases/read-me.html) for more details. The data for this demo is included in the LingPipe distribution.

## Spelling Correction

Demo for query spell checking using a 5-gram character language model over tokens in a Lucene index. The LingPipe code is in demos/querySpellCheck, the corresponding scalingpipe code is in package o.s.spell. Read the [LingPipe Spelling Tutorial](http://alias-i.com/lingpipe/demos/tutorial/querySpellChecker/read-me.html). Data for this demo is included with the LingPipe distribution.

## Hyphenation and Syllabification

Demo for hyphenation using the Moby dataset (needs to be downloaded) only, did not do the demos against the Webster and Celex datasets. LingPipe code is in demos/hyphenation and corresponding scalingpipe code is in package o.s.hyphen. Read teh [LingPipe Hyphenation & Syllabification Tutorial](http://alias-i.com/lingpipe/demos/tutorial/hyphenation/read-me.html) for more information.

## Chinese Word Segmentation - chineseTokens

## Logistic Regression

Demonstrates the use of the LingPipe Logistic regression module to do multi-category classification of structured and text data, regularization using priors and discrete choice analysis. LingPipe code is under demos/logistic-regression, scalingpipe code is in package o.s.logreg. Data is either embedded inside the code or uses the 4-newsgroup data supplied with the LingPipe distribution. More information in the [LingPipe Logistic Regression Tutorial](http://alias-i.com/lingpipe/demos/tutorial/logistic-regression/read-me.html).

## Topic Classification

Demo of classifying topic for 4 newsgroup (data supplied with LingPipe distribution) and cross validating and evaluating the classifiers. LingPipe code is in demps/classify, and corresponding scalingpipe code is in o.s.classify. More details on the [LingPipe Classification Tutorial](http://alias-i.com/lingpipe/demos/tutorial/classify/read-me.html).

## Language Identification - langid

Demo for training, cross-validating and using a language detection classifier using the Leipzig multi-language corpus. LingPipe code is in demos/langid and corrsponding scalingpipe code is in o.s.langdetect. See the [LingPipe Language ID Tutorial](http://alias-i.com/lingpipe/demos/tutorial/langid/read-me.html) for more information.

Data needs to be downloaded manually, and the sentences.txt from each language distribution needs to be extracted into the data/leipzip directory as ${langid}-sentences.txt files. The scalingpipe code was run using sentences from Catalan (cat), English (eng), Finnish (fin), French (fra), Hindi (hin), Italian (ita), Japanese (jpn), Korean (kor), Polish (pol), Russian (rus), Spanish (spa), Turkish (tur) and Chinese (zho).

## Clustering - cluster

## Sentiment Analysis - sentiment

Demo for doing polarity and subjectivity analysis. LingPipe code is in demos/sentiment and corresponding scalingpipe code is in o.s.sentiment. See the [LingPipe Sentiment Tutorial](http://alias-i.com/lingpipe/demos/tutorial/sentiment/read-me.html) for more details. You will need external data for this demo, links are on the LingPipe tutorial page, the data needs to be expanded into data/sentiment.
