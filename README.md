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

## Word Sense Disambiguation - wordSense
## Expectation Maximization - em
## Singular Value Decomposition - svd
## Character Language Modeling - lm
## Spelling Correction - querySpellChecker
## Database Text Mining - db
## Interesting Phrase Detection - interestingPhrases
## Hypenation and Syllabification - hyphenation
## Chinese Word Segmentation - chineseTokens
## Logistic Regression - logistic-regression
## Conditional Random Fields - crf
## Topic Classification - classify
## Language Identification - langid
## Clustering - cluster
## Sentiment Analysis - sentiment

Additional data to run these examples:

