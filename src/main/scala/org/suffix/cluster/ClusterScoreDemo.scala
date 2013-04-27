package org.suffix.cluster

import java.util.HashSet

import com.aliasi.cluster.ClusterScore

object ClusterScoreDemo extends App {

  val refCluster1 = new HashSet[Int]()
  List(1, 2, 3).foreach(refCluster1.add(_))
  val refCluster2 = new HashSet[Int]()
  List(4, 5).foreach(refCluster2.add(_))
  val refCluster3 = new HashSet[Int]()
  List(6).foreach(refCluster3.add(_))
  val refPartition = new HashSet[HashSet[Int]]()
  List(refCluster1, refCluster2, refCluster3).foreach(
    refPartition.add(_))
  
  val resCluster1 = new HashSet[Int]()
  List(1, 2).foreach(resCluster1.add(_))
  val resCluster2 = new HashSet[Int]()
  List(3, 4, 5, 6).foreach(resCluster2.add(_))
  val resPartition = new HashSet[HashSet[Int]]()
  List(resCluster1, resCluster2).foreach(resPartition.add(_))
  
  val score = new ClusterScore[Int](refPartition, resPartition)
  val prEval = score.equivalenceEvaluation()
  Console.println("Equivalence Relation Evaluation\n" + 
    prEval.toString())
  Console.println("MUC P=" + score.mucPrecision() + ", R=" + 
    score.mucRecall() + ", F=" + score.mucF())
  Console.println("B-Cubed P=" + score.b3ClusterPrecision() + 
    ", R=" + score.b3ClusterRecall() + ", F=" + score.b3ClusterF())
  Console.println("Element Averaged P=" + score.b3ElementPrecision() +
    ", R=" + score.b3ElementRecall() + ", F=" + score.b3ElementF())
}