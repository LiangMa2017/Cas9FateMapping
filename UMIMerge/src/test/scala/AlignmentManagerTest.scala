import java.io.File

import aligner.{Aligner, MAFFT, AlignmentManager, AlignmentManager$, Alignment}
import org.scalatest.{Matchers, FlatSpec}
import utils.CutSites

import scala.collection.mutable
import scala.main.{ForwardReadOrientation, SequencingRead}

/**
 * Created by aaronmck on 11/18/15.
 */
class AlignmentManagerTest extends FlatSpec with Matchers {
  val readName = "TestRead1"


  "A MAFFT" should "find basic deletion correctly" in {
    val ref =     "AAATAAAAT"
    val readFwd = "AAAA-AAAA"
    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7)))
    val testCalls = AlignmentManager.callEdits(ref,readFwd,1,cutSites)
    testCalls._1.length should be (3)
    testCalls._1(0).cigarCharacter should be ("M")
    testCalls._1(0).readBase should be ("AAAA")
    testCalls._1(0).refBase should be ("AAAT")
    testCalls._1(0).refBase should be ("AAAT")

    testCalls._1(1).cigarCharacter should be ("D")
    testCalls._1(1).readBase should be ("-")
    testCalls._1(1).refBase should be ("A")

    testCalls._1(2).cigarCharacter should be ("M")
    testCalls._1(2).readBase should be ("AAAA")
    testCalls._1(2).refBase should be ("AAAT")
  }

  "A MAFFT" should "find multibase deletion correctly" in {
    val ref =     "AAATAAAAA"
    val readFwd = "AAAA---AA"
    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7)))
    val testCalls = AlignmentManager.callEdits(ref,readFwd,1,cutSites)
    testCalls._1.length should be (3)
    testCalls._1(0).cigarCharacter should be ("M")
    testCalls._1(0).readBase should be ("AAAA")
    testCalls._1(0).refBase should be ("AAAT")

    testCalls._1(1).cigarCharacter should be ("D")
    testCalls._1(1).readBase should be ("---")
    testCalls._1(1).refBase should be ("AAA")

    testCalls._1(2).cigarCharacter should be ("M")
    testCalls._1(2).readBase should be ("AA")
    testCalls._1(2).refBase should be ("AA")
  }

  "A MAFFT" should "find multi-deletions correctly" in {
    val ref =     "AAAAAAAAA"
    val readFwd = "--AA---AA"
    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7)))
    val testCalls = AlignmentManager.callEdits(ref,readFwd,1,cutSites)
    testCalls._1.length should be (3)

    testCalls._1(0).cigarCharacter should be ("M")
    testCalls._1(0).readBase should be ("AA")
    testCalls._1(0).refBase should be ("AA")
    testCalls._1(0).refPos should be (2)

    testCalls._1(1).cigarCharacter should be ("D")
    testCalls._1(1).readBase should be ("---")
    testCalls._1(1).refBase should be ("AAA")
    testCalls._1(1).refPos should be (4)

    testCalls._1(2).cigarCharacter should be ("M")
    testCalls._1(2).readBase should be ("AA")
    testCalls._1(2).refBase should be ("AA")
    testCalls._1(2).refPos should be (7)
  }

  "A MAFFT" should "find basic insertion correctly" in {
    val ref =     "AAAT-AAAA"
    val readFwd = "AAAATAAAA"
    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7)))
    val testCalls = AlignmentManager.callEdits(ref,readFwd,1,cutSites)
    testCalls._1.length should be (3)
    testCalls._1(0).cigarCharacter should be ("M")
    testCalls._1(0).readBase should be ("AAAA")
    testCalls._1(0).refBase should be ("AAAT")
    testCalls._1(0).refPos should be (0)

    testCalls._1(1).cigarCharacter should be ("I")
    testCalls._1(1).readBase should be ("T")
    testCalls._1(1).refBase should be ("-")
    testCalls._1(1).refPos should be (4)

    testCalls._1(2).cigarCharacter should be ("M")
    testCalls._1(2).readBase should be ("AAAA")
    testCalls._1(2).refBase should be ("AAAA")
    testCalls._1(2).refPos should be (4)
  }

  "A MAFFT" should "find offsets correctly" in {
    val ref =     "AAATAAAAA"
    val readFwd = "----TAAAA"

    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7)))
    val testCalls = AlignmentManager.callEdits(ref,readFwd,1,cutSites)
    testCalls._1.length should be (1)
    testCalls._1(0).cigarCharacter should be ("M")
    testCalls._1(0).readBase should be ("TAAAA")
    testCalls._1(0).refBase should be  ("AAAAA")
    testCalls._1(0).refPos should be (4)

  }

  "A MAFFT" should "merge an event and a non-event correctly" in {
    val ref1 =     "AAAATAAAA"
    val readFwd1 = "AAAATAAAA"

    val ref2 =     "AAAATAAAA"
    val readFwd2 = "AAAAT-AAA"

    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7)))
    val testCalls1 = AlignmentManager.callEdits(ref1,readFwd1,1,cutSites)
    val testCalls2 = AlignmentManager.callEdits(ref2,readFwd2,1,cutSites)

    testCalls1._1.length should be (1)
    testCalls2._1.length should be (3)

    val combined = AlignmentManager.editsToCutSiteCalls(List[List[Alignment]](testCalls1._1,testCalls2._1),List[List[String]](testCalls1._2,testCalls2._2),cutSites)
    combined._2.size should be (1)
    combined._2(0) should be ("WT_1D+5")
  }

  "A MAFFT" should "merge an dual-event and a non-event correctly" in {
    val ref1 =     "AAAATAAAAAAATAAAAA"
    val readFwd1 = "AAAATAAAAAAATTAAAA"

    //              012345678901234567
    val ref2 =     "AAAATAAAAAAAATAAAA"
    val readFwd2 = "AAAAT-AAAAAAAT--AA"

    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7),(12,14,16)))
    val testCalls1 = AlignmentManager.callEdits(ref1,readFwd1,1,cutSites)
    val testCalls2 = AlignmentManager.callEdits(ref2,readFwd2,1,cutSites)

    testCalls1._1.length should be (1)
    testCalls2._1.length should be (5)


    val combined = AlignmentManager.editsToCutSiteCalls(List[List[Alignment]](testCalls1._1,testCalls2._1),List[List[String]](testCalls1._2,testCalls2._2),cutSites)
    combined._2.size should be (2)
    println(combined._2.mkString(")("))
    combined._2(0) should be ("WT_1D+5")
    combined._2(1) should be ("WT_2D+14")
  }

  "A MAFFT" should "handle two reads with a shared, complex event correctly" in {
    val ref1 =     "AAATAAAAAAAATAAAAA"
    val readFwd1 = "AAAT-T-AAAAATAAAAA"

    //              012345678901234567
    val ref2 =     "AAATAAAAAAAATAAAAA"
    val readFwd2 = "AAAT-T-AAAAATAAAAA"

    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((2,5,8)))
    val testCalls1 = AlignmentManager.callEdits(ref1,readFwd1,1,cutSites)
    val testCalls2 = AlignmentManager.callEdits(ref2,readFwd2,1,cutSites)

    testCalls1._1.length should be (5)
    testCalls2._1.length should be (5)

    val combined = AlignmentManager.editsToCutSiteCalls(List[List[Alignment]](testCalls1._1,testCalls2._1),List[List[String]](testCalls1._2,testCalls2._2),cutSites)
    combined._2.size should be (1)
    combined._2(0) should be ("1D+4&1D+6")
  }


  "A MAFFT" should "merge an a collision correctly" in {
    val ref1 =     "AAAATAAAAAAAATAAAA"
    val readFwd1 = "AAAAT-AAAAAAAT--AA"

    //              012345678901234567
    val ref2 =     "AAAATAAAAAAAATAAAA"
    val readFwd2 = "AAAAT-AAAAAAAT--AA"

    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7),(12,14,16)))
    val testCalls1 = AlignmentManager.callEdits(ref1,readFwd1,1,cutSites)
    val testCalls2 = AlignmentManager.callEdits(ref2,readFwd2,1,cutSites)

    testCalls1._1.length should be (5)
    testCalls2._1.length should be (5)

    val combined = AlignmentManager.editsToCutSiteCalls(List[List[Alignment]](testCalls1._1,testCalls2._1),List[List[String]](testCalls1._2,testCalls2._2),cutSites)
    combined._2.size should be (2)
    println(combined._2.mkString(")("))
    combined._2(0) should be ("1D+5")
    combined._2(1) should be ("2D+14")
  }

  "A MAFFT" should "work with real data4" in {
    val ref =     "TCGTCGGCAGCGTCAGATGTGTATAAGAGACAGNNNNNNNNNNCTTCCTCCAGCTCTTCAGCTCGTCTCTCCAGCAGTTCCCCCGAGTCTGCACCTCCCCAGAAGTCCTCCAGTCCAAACGCTGCTGTCCAGTCTGGCCCGGCGACGGCTCTGTGTGCGGCGTCCAGTCAGGTCGAGGGTTCTGTCAGGACGTCCTGGTGTCCGACCTTCCCAACGGGCCGCAGTATCCTCACTCAGGAGTGGACGATCGAGAGCGATGGCCTTTAGTGTTTTACAACCAAACCTGCCAGTGCGCCGGAAACTACATGGGGTTTGATTGCGGCGAATGCAAGTTCGGCTTCTTCGGTGCCAACTGCGCAGAGAGACGCGAGTCTGTGCGCAGAAATATATTCCAGCTGTCCACTACCGAGAGGCAGAGGTTCATCTCGTACCTAAATCTGGCCAAAACCACCATAAGCCCCGATTATATGATCGTAACAGGAACGTACGCGCAGATGAACAACGGCTCCACGCCAATGTTCGCCAACATCAGTGTGTACGATTTATTCGTGTGGATGCATTATTACGTGTCCCGGGACGCTCTGCTCGGTGGGCCTGGGAATGTGTGGGCTGATAGATCGGAAGAGCACACGTCTGAACT"
    val readFwd =  "CTTCCTCCAGCTCTTCAGCTCGTCTCTCCAGCAGTTCCCCCGAGTCTGCACCACCCAAACGCTGCTGTCCAGTCTGGCCCGGCGACGGCTCCGTGTGCGGCGTCCAGTCAGGTCGAGGGTTCTGTCAGGACGTCCTGGTGTCCGACCTTCCCAACGGGCCGCAGTATCCTCACTCAGTGGACGATCGAGAGCGATGGCCTTTAGTGTTAACACC"

    val readRev = "ATCAGCCCACACATTCCCAGGCCCACCGAGCAGAGCGTCCCGGGACACGTAATAATGCATCCACATAAATCGTACACACTGATGTTGGCGAACATTGGCGTGGAGATTGTTC"

    val fRead = Aligner.SequencingReadFromNameBases("fwd",readFwd)
    val rRead = Aligner.SequencingReadFromNameBases("rev",readRev)

    val cutsSiteObj = CutSites.fromFile(new File("test_files/TYRFull.fasta.cutSites"), 3)

    rRead.reverseCompAlign = true
    //println(AlignmentManager.cutSiteEvents("testUMI", ref, fRead, rRead, cutsSiteObj, 10, true)._3.mkString("<->"))
  }


  "Alignment manager" should "recover the wild type sequence when there is no event called" in {
    val fakeReferenceBases1 = Alignment(/*val refPos: Int */ 5, /*refBase: String*/ "AAAAAAAAAA", /*readBase: String*/ "----------", /*cigarCharacter: String*/ "M")
    val fakeReferenceBases2 = Alignment(/*val refPos: Int */ 5, /*refBase: String*/ "AAAAAAAAAA", /*readBase: String*/ "AAAAAAAAAA", /*cigarCharacter: String*/ "M")

    val cutsites = new CutSites()
    cutsites.cutSites(0) = 10
    cutsites.startSites(0) = 10
    cutsites.fullSites :+= ("TestRef", 5, 10)
    cutsites.windows :+= (5, 10, 15)
    cutsites.size = 1

    val edits = AlignmentManager.editsToCutSiteCalls(List[List[Alignment]](List[Alignment](fakeReferenceBases1,fakeReferenceBases2)), List[List[String]](List[String]("AAAAAAAAAA")), cutsites)
    println("-------------------->>>>>>>>")
    println("-->>" + edits._2.mkString(",") + "<<--" + edits._3.mkString(",") + "<<--")
  }
}
