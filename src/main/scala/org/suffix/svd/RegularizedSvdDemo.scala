package org.suffix.svd

import java.io.{OutputStreamWriter, PrintWriter}

import scala.collection.mutable.ArrayBuffer

import com.aliasi.io.{LogLevel, Reporters}
import com.aliasi.matrix.SvdMatrix

object RegularizedSvdDemo extends App {

  val Matrix = Array[Array[Double]](
    Array[Double]( 1, 2, 3, -4 ),
    Array[Double]( 1, 4, 9, -16 ),
    Array[Double]( 1, 8, 27, -64 ),
    Array[Double]( -1, -16, -81, 256 ))
  val Regularizations = Array[Double](0.0001, 0.001, 0.01, 0.1, 1.0) 
  val MinEpochs = 10
  val MaxEpochs = 10000
  val FeatureInit = 0.01
  val InitialLearningRate = 0.0005
  val AnnealingRate = 10000
  val MinImprovement = 0.0000
  val NumFactors = 4
  
  val reporter = Reporters.writer(
    new PrintWriter(new OutputStreamWriter(System.out))).
    setLevel(LogLevel.DEBUG)
  Regularizations.foreach(regularization => {
    Console.println("==== regularization: " + regularization + " ====")
    val svdMatrix = SvdMatrix.svd(Matrix, NumFactors, 
      FeatureInit, InitialLearningRate, AnnealingRate, 
      regularization, reporter, MinImprovement,
      MinEpochs, MaxEpochs)
     val singularValues = svdMatrix.singularValues()
     Console.println("Singular values: " + singularValues.toList)
     Console.println("Reconstructed Matrix")
     for (i <- 0 until svdMatrix.numRows()) {
       val buf = ArrayBuffer[Double]()
       for (j <- 0 until svdMatrix.numColumns()) {
         buf += svdMatrix.value(i, j)
       }
       Console.println(buf.toList)
     }    
  })

}