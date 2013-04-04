ScalingPipe is a sample of Scala drivers for LingPipe, the famous Machine Learning Natural Language Processing Library, developed by Bob Carpenter:

http://lingpipe.com/

We use SBT (Simple Build Tool) to install Scala and other dependencies. Since LingPipe is not open-source, you need to agree to its license and then manually install the lingpipe jar file into the lib directory as shown below.

	cd $PROJECT_ROOT (this directory)
	mkdir -p lib
	cp $LINGPIPE_DIST_DIR/lingpipe-*.jar lib

As a starting point, all the demo code from the Lingpipe 4.1.0 distribution has been re-written using Scala 2.9.2. This consists of the following packages.

String Comparison - stringCompare (strcmp)
Sentence Detection - sentences (sentence)
Part of Speech Tagging - posTags
Named Entity Recognition - ne
Word Sense Disambiguation - wordSense
Expectation Maximization - em
Singular Value Decomposition - svd
Character Language Modeling - lm
Spelling Correction - querySpellChecker
Database Text Mining - db
Interesting Phrase Detection - interestingPhrases
Hypenation and Syllabification - hyphenation
Chinese Word Segmentation - chineseTokens
Logistic Regression - logistic-regression
Conditional Random Fields - crf
Topic Classification - classify
Language Identification - langid
Clustering - cluster
Sentiment Analysis - sentiment

The code uses the demo data that is provided along with the LingPipe 4.1.0 distribution. It expects the data to be rooted under $PROJECT_ROOT/data, so you should link it appropriately, like so:

	cd $PROJECT_ROOT (this directory)
	ln -s $LINGPIPE_DIST_DIR/demos/data .

Additional data to run these examples:

For sentence model evaluation:
GENIA Corpus: the link in the LingPipe docs are outdated, use this one instead:
http://www.nactem.ac.uk/genia/genia-corpus/term-corpus
