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