# 2. Lazy Evaluation

## 2.1 Structural Induction on Trees

skipped (recommended)

## 2.2 Lazy Lists

- Collections and Combinatorial Search: Find the second prime number between 1000 and 10000:
    - `(1000 to 10000).filter(isPrime)(1)`

- Recursive alternative:
    ```scala
    def secondPrime(from: Int, to: Int) = nthPrime(from, to, 2)
    def nthPrime(from: Int, to: Int, n: Int): Int =
        if from >= to then throw Error("no prime")
        else if isPrime(from) then
            if n == 1 then from else nthPrime(from + 1, to, n - 1)
        else nthPrime(from + 1, to, n)
    ```
    - complex and long but better performance as first computes all primes in the range.

- Can we avoid computing the elements of a sequence until they are needed for evaluation? This is the main idea behind a lazy list.

- Defining Lazy Lists:
    ```scala
    val xs = LazyList.cons(1, LazyList.cons(2, LazyList.empty))

    LazyList(1, 2, 3)

    (1 to 1000).to(LazyList)
    ```

- Function to return range (like `(lo until hi).to(LazyList)`):
    ```scala
    def lazyRange(lo: Int, hi: Int): LazyList[Int] =
        if lo >= hi then LazyList.empty
        else LazyList.cons(lo, lazyRange(lo + 1, hi))
    ```
    - returns a single object of type `LazyList`. Elements are computed only when needed (e.g calling `head` or `tail`)

- Similar to producing a list
    ```scala
    def listRange(lo: Int, hi: Int): List[Int] =
        if lo >= hi then Nil
        else lo :: listRange(lo + 1, hi)
    ```
    - returns a list with hi - lo elements

- Finding second prime number is now
    - `LazyList.range(1000, 10000).filter(isPrime)(1)`

- Easier `Cons` operator:
    - `x #:: xs == LazyList.cons(x, xs)`
    - `#::` can be used in expressions as well as patterns.

- Implementation of Lazy Lists: uses "by-name" parameter so expressions are not evaluated at the point of call.

- Exercise:
    ```scala
    def lazyRange(lo: Int, hi: Int): TailLazyList[Int] =
        print(lo + " ")
        if lo >= hi then TailLazyList.empty
        else TailLazyList.cons(lo, lazyRange(lo + 1, hi))
    ```
    - What's the output of lazyRange(1, 10).take(3).toList: `1 2 3`

## 2.3 Lazy Evaluation

- The proposed implementation (2.2; TailLazyList) suffers from a problem: if `tail` is called several times, the corresponding lazy list will be recomputed each time.
- Can be avoided by storing the result of the first evaluation of `tail` and re-using the stored result. This is "lazy evaluation" (as opposed to "by-name evaluation" or "strict evaluation")

- Specifying lazy evaluation
    ```scala
    lazy val x = expr
    ```

- Exercise: What's the output of:
    ```scala
    def expr =
        val x = { print("x"); 1 }
        lazy val y = { print("y"); 2 }
        def z = { print("z"); 3 }
        z + y + x + z + y + x
    expr
    ```
    - xzyz (`val` and `lazy val` evaluated once)

- The real world implementation of lazy list uses a lazy evaluation of `tail` (as well as `head` and `isEmpty`) 

## 2.4 Computing with infinite sequences

- Lazy lists open up the possibility to define infinite lists
    ```scala
    def from(n: Int): LazyList[Int] = n #:: from(n + 1)
    ```
    - All natural numbers: `val nats = from(0)`
    - Multiples of 4: `nats.map(_ * 4)`

- Sieve of Eratosthenes: to calculate prime numbers
    - Start with all integers from 2, the first prime number.
    - Eliminate all multiples of 2.
    - The first element of resulting list is 3, a prime number.
    - Eliminate all multiples of 3.
    - Iterate forever.

- Implementation
    ```scala
    def sieve(s: LazyList[Int]): LazyList[Int] = 
        s.head #:: sieve(s.tail.filter(_ % s.head != 0))

    val primes = sieve(from(2))

    primes.take(N).toList // first 'N' primes
    ```

- Back to Square Roots: to produce a converging sequence
    ```scala
    def sqrtSeq(x: Double): LazyList[Double] = 
        def improve(guess: Double) = (guess + x / guess) / 2
        lazy val guesses: LazyList[Double] = 1 #:: guesses.map(improve)
        guesses

    def isGoodEnough(guess: Double, x: Double) = 
        ((guess * guess - x) / x).abs < 0.0001

    sqrtSeq(2).filter(isGoodEnoug(_, 2))
    ```

## 2.5 Case Study: The Water Pouring Problem

- You are given glasses of different sizes.
- Your task is to produce a glass with a given amount of water in it.
- Only allowed moves are:
    - fill a glass
    - empty a glass
    - pour from one glass to another until first glass is empty or second glass is full.

```scala
type Glass = Int // index of glass
type State = Vector[Int] // levels of each glass

class Pouring(full: State): // full: max levels of each glass

    enum Move:
        case Empty(glass: Glass) // glass to empty
        case Fill(glass: Glass) // glass to fill
        case Pour(from: Glass, to: Glass)

        def apply(state: State): State = this match
            case Empty(glass) => state.updated(glass, 0)
            case Fill(glass) => state.updated(glass, full(glass))
            case Pour(from: Glass, to: Glass) =>
                val amount = state(from) min (full(to) - state(to))
                state.updated(from, state(from) - amount).updated(to, state(to) + amount)
    end Move

    val moves = // all possible moves at any state
        val glass: Range = 0 until full.length
        (for g <- glass yield Move.Empty(g))
        ++ (for g <- glasses yield Move.Fill(g))
        ++ (for g1 <- glasses; g2 <- glasses if g1 != g2 yield Move.Pour(g1, g2))

    class Path(history: List[Move], val endState: State):
        def extend(move: Move) = Path(move :: history, move(endState))
        override def toString = s"${history.reverse.mkString(" ")} --> $endState"
    end Path

    val empty: State = full.map(x => 0)
    val start = Path(Nil, empty)

    def pathsFrom(paths: List[Path], explored: Set[State]): LazyList[List[Path]] = 
        val frontier = 
            for 
                path <- paths
                move <- moves
                next = path.extend(move)
                if !explored.contains(next.endState)
            yield next
        paths #:: pathsFrom(frontier, explored ++ frontier.map(_.endState))

    def solutions(target: Int): LazyList[Path] = 
        for
            path <- pathsFrom(List(start), Set(empty))
            path <- paths
            if path.endState.contains(target)
        yield path

end Pouring

// example
val problem = Pouring(Vector(4, 7))
problem.solutions(6).head
```

- Design principles:
    - Name everything you can.
    - Put operations into natural scopes.
    - Keep degrees of freedom for future reginements.



