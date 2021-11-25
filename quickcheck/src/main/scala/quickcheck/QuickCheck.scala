package quickcheck

import org.scalacheck.*
import Arbitrary.*
import Gen.*
import Prop.forAll

abstract class QuickCheckHeap extends Properties("Heap") with IntHeap:
  lazy val genHeap: Gen[H] = oneOf(
    Gen.const(empty),
    for {
      x <- arbitrary[Int]
      h <- oneOf(Gen.const(empty), genHeap)
    } yield insert(x, h)
  )
  given Arbitrary[H] = Arbitrary(genHeap)

  property("gen1") = forAll { (h: H) =>
    val m = if isEmpty(h) then 0 else findMin(h)
    findMin(insert(m, h)) == m
  }

  property("min1") = forAll { (a: Int) =>
    val h = insert(a, empty)
    findMin(h) == a
  }

  property("min2") = forAll { (a: Int, b: Int) =>
    val h = insert(a, insert(b, empty))
    if a < b then findMin(h) == a
    else findMin(h) == b
  }

  property("del1") = forAll { (a: Int) =>
    val h = insert(a, empty)
    deleteMin(h) == empty
  }

  property("ord1") = forAll { (h: H) =>
    def checkOrdering(p: H): Boolean =
      val delHeap = deleteMin(p)
      if isEmpty(delHeap) then true
      else (findMin(p) < findMin(delHeap)) && checkOrdering(delHeap) 
    if isEmpty(h) then true else checkOrdering(h)
  }

  property("meld1") = forAll { (h1: H, h2: H) =>
    val m = meld(h1, h2)
    val minh1 = if isEmpty(h1) then 0 else findMin(h1)
    val minh2 = if isEmpty(h2) then 0 else findMin(h2)
    
    if isEmpty(m) then true else 
      val minm = findMin(m)
      minm == minh1 || minm == minh2
  }

  

