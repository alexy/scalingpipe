package org.suffix.logreg

import java.util.Random

import scala.util.control.Breaks.{break, breakable}

import com.aliasi.dca.DiscreteChooser
import com.aliasi.io.{LogLevel, Reporters}
import com.aliasi.matrix.{DenseVector, Vector}
import com.aliasi.stats.{AnnealingSchedule, RegressionPrior}

object DcaDemo extends App {

  val simCoeffs = Array[Double]( 3.0, -2.0, 1.0 )
  val simCoeffVector = new DenseVector(simCoeffs)
  val simChooser = new DiscreteChooser(simCoeffVector)
  val numDims = simCoeffs.length
  val numSamples = 1000
  val random = new Random(42)
  
  val alternativess = new Array[Array[Vector]](numSamples)
  val choices = new Array[Int](numSamples)
  for (i <- 0 until numSamples) {
    val numChoices = 1 + random.nextInt(8)
    alternativess(i) = new Array[Vector](numChoices)
    for (j <- 0 until numChoices) {
      val xs = new Array[Double](numDims)
      for (k <- 0 until numDims) {
        xs(k) = 2.0 * random.nextGaussian()
      }
      alternativess(i)(j) = new DenseVector(xs)
    }
    val choiceProbs = simChooser.choiceProbs(alternativess(i))
    val choiceProb = random.nextDouble()
    var cumProb = 0.0
    breakable {
      for (j <- 0 until numChoices) {
        cumProb += choiceProbs(j)
        if (choiceProb < cumProb || j == (numChoices - 1)) {
          choices(i) = j
          break
        }
      }
    }
    Console.println("Sample " + i + " random choice prob=" + choiceProb)
    for (j <- 0 until numChoices) {
      Console.println((if (choices(i) == j) "* " else "  ") + j +
        " p=" + choiceProbs(j) + " xs=" + alternativess(i)(j))
    }
  }
  
  val priorVariance = 4.0
  val prior = RegressionPrior.gaussian(priorVariance, true)
  val priorBlockSize = 100
  val initialLearningRate = 0.1
  val decayBase = 0.99
  val annealingSchedule = AnnealingSchedule.exponential(
    initialLearningRate, decayBase)
  val minImprovement = 0.00001
  val minEpochs = 5
  val maxEpochs = 500
  val reporter = Reporters.stdOut().setLevel(LogLevel.DEBUG)
  val chooser = DiscreteChooser.estimate(
    alternativess, choices, prior, priorBlockSize, 
    annealingSchedule, minImprovement, minEpochs, 
    maxEpochs, reporter)
  val coeffVector = chooser.coefficients()
  Console.println("Actual coeffs: " + simCoeffVector)
  Console.println("Fit coeffs   : " + coeffVector)
  
}