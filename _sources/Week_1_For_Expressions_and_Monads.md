# 1. For Expressions and Monads

## 1.1 Queries with For
- Consider this mini-database:
    ```scala
    case class Book(title: String, authors: List[String])

    val books: List[Book] = List(
        Book(title = "Structure and Interpretation of Computer Programs",
            authors = List("Abelson, Harald", "Sussman, Gerald J.")),
        Book(title = "Introduction to Functional Programming",
            authors = List("Bird, Richard", "Wadler, Phil")),
        Book(title = "Effective Java",
            authors = List("Bloch, Joshua")),
        Book(title = "Java Puzzlers",
            authors = List("Bloch, Joshua", "Gafter, Neal")),
        Book(title = "Programming in Scala",
            authors = List("Odersky, Martin", "Spoon, Lex", "Venners, Bill")))
    ```
- To find books whose author's name is Bird:
    ```scala
    for
        b <- books
        a <- b.authors
        if a.startsWith("Bird,")
    yield b.title
    ```
- To find books which have "Program" in the title:
    ```scala
    for b <- books if b.title.indexOf("Program") >= 0
    yield b.title
    ```
- Authors who have written at least two books?
    ```scala
    for
        b1 <- books
        b2 <- books
        if b1 != b2
        a1 <- b1.authors
        a2 <- b2.authors
        if a1 == a2
    yield a1
    ```
- To avoid double counting:
    ```scala
    for
        b1 <- books
        b2 <- books
        if b1.title < b2.title
        a1 <- b1.authors
        a2 <- b2.authors
        if a1 == a2
    yield a1
    ```
- What if authors have written 3 books? (Their names will appear 3 times)
    ```scala
    val repeated =
        for
            b1 <- books
            b2 <- books
            if b1.title < b2.title
            a1 <- b1.authors
            a2 <- b2.authors
            if a1 == a2
        yield a1
    repeated.distinct
    ```
    or make it yield a set
    ```scala
    val bookSet = books.toSet
    for
        b1 <- bookSet
        b2 <- bookSet
        if b1 != b2
        a1 <- b1.authors
        a2 <- b2.authors
        if a1 == a2
    yield a1
    ```

## 1.2 Translation of For

- Higher-order functions can be defined in terms of `for`:
    ```scala
    def mapFun[T, U](xs: List[T], f: T => U): List[U] =
        for x <- xs yield f(x)

    def flatMap[T, U](xs: List[T], f: T => Iterable[U]): List[U] =
        for x <- xs; y <- f(x) yield y

    def filter[T](xs: List[T], p: T => Boolean): List[T] =
        for x <- xs if p(x) yield x
    ```

- But `for` expressions are expressed in terms of `map`, `flatMap`, and a lazy variant `filter` by the Scala compiler
    1. A simple expression
        ```scala
        for x <- e1 yield e2
        ```
        is translated to
        ```scala
        e1.map(x => e2)
        ```
    2. An expression with a predicate
        ```scala
        for x <- e1 if f; s yield e2
        ```
        is translated to
        ```scala
        for x <- e1.withFilter(x => f); s yield e2
        ```
        `withFilter` is a variant of `filter` that doesn't produce an intermediate list but instead applies the following `map` or `flatMap` to only those elements that passed the test.
    3. A nested `for`-expression
         ```scala
        for x <- e1; y <- e2; s yield e2
        ```
        is translated to
        ```scala
        e1.flatMap(x => for y <- e2; s yield e3)
        ```

- Example:
    ```scala
    for
        i <- 1 until n
        j <- 1 until i
        if isPrime(i + j)
    yield (i, j)
    ```
    is also:
    ```scala
    (1 until n).flatMap(i =>
        (1 until i)
            .withFilter(j => isPrime(i+j))
            .map(j => (i, j)))
    ```

- Generalization of `for`
    - Translation of `for` is not limited to lists or sequences or collections as long as those types have an implementation of `map`, `flatMap`, and `withFilter`.
    - Example: arrays, databases, optional values, etc
    - This is the basis of _Spark_ :)

## 1.3 Functional Random Generators

- Random values:
    ```scala
    val rand = java.util.Random()
    rand.nextInt()
    ```
    But how would we get random values for other types such as booleans, strings, lists, sets, or even trees?

- Generator
    ```scala
    train Generator[+T]:
        def generate(): T
    ```
    generates random values of type `T`.
    - Instances:
        ```scala
        val integers = new Generator[Int]:
            val rand = java.util.Random()
            def generate() = rand.nextInt()

        val booleans = new Generator[Boolean]:
            def generate() = integers.generate() > 0

        val pairs = new Generator[(Int, Int)]:
            def generate() = (integers.generate(), integers.generate())
        ```

- How do we streamline this? We would _like_ to use `for`:
    ```scala
    val booleans = for x <- integers yield x > 0

    def pairs[T, U](t: Generator[T], u: Generator[U]) = 
        for x <- t; y <- u yield (x, y)
    ```
    which can be rewritten as:
    ```scala
    val booleans = integers.map(x => x > 0)

    def pairs[T, U](t: Generator[T], u: Generator[U]) = 
        t.flatMap(x => u.map(y => (x, y))) 
    ```
    but requires implementation of `map` and `flatMap`.

- Here's a fuller version of `Generator`:
    ```scala
    trait Generator[+T]:
        def generate(): T
        
        def map[S](f: T => S) = new Generator[S]:
            def generate() = f(Generator.this.generate())
        
        def flatMap[S](f: T => Generator[S]) = new Generator[S]:
            def generate() = f(Generator.this.generate()).generate()
    ```
    `Generator.this` refers to the `this` of the outer object of class `Generator`

- Does this work? We can verify:
    - booleans
        ```scala
        val booleans = for x <- integers yield x > 0

        val booleans = integers.map(x => x > 0)

        val booleans = new Generator[Boolean]:
            def generate() = ((x: Int) => x > 0)(integers.generate())

        val booleans = new Generator[Boolean]:
            def generate() = integers.generate() > 0
        ```
    - pairs
        ```scala
        def pairs[T, U](t: Generator[T], u: Generator[U]) = t.flatMap(
            x => u.map(y => (x, y)))

        def pairs[T, U](t: Generator[T], u: Generator[U]) = t.flatMap(
            x => new Generator[(T, U)] { def generate() = (x, u.generate()) })

        def pairs[T, U](t: Generator[T], u: Generator[U]) = new Generator[(T, U)]:
            def generate() = (new Generator[(T, U)]:
            def generate() = (t.generate(), u.generate())
            ).generate()

        def pairs[T, U](t: Generator[T], u: Generator[U]) = new Generator[(T, U)]:
            def generate() = (t.generate(), u.generate())
        ```

- More Generator Examples:
    ```scala
    def single[T](x: T): Generator[T] = new Generator[T]:
        def generate() = x

    def range(lo: Int, hi: Int): Generator[Int] =
        for x <- integers yield lo + x.abs % (hi - lo)

    def oneOf[T](xs: T*): Generator[T] =
        for idx <- range(0, xs.length) yield xs(idx)
    ```

- A `List` Generator: Lists are either empty or non-empty
    ```scala
    def lists: Generator[List[Int]] = 
        for
            isEmpty <- booleans
            list <- if isEmpty then emptyLists else nonEmptyLists
        yield list

    def emptyLists = single(Nil) // must be a `Generator`

    def nonEmptyLists = 
        for 
            head <- integers
            tail <- lists
        yield head :: tail
    ```

- A `Tree` Generator: either leaf or an inner tree
    ```scala
    enum Tree:
        case Inner(left: Tree, right: Tree)
        case Leaf(x: Int)

    def trees: Generator[Tree] =
        for
            isLeaf <- booleans
            tree <- if isLeaf then leafs else inners
        yield tree

    def leafs = for x <- integers yield Tree.Leaf(x)

    def inners = 
        for 
            x <- trees
            y <- trees
        yield Tree.Inner(x, y)
    ```

- Why do we care about this? Random generators allow for a powerful testing framework.
    ```scala
    def test[T](g: Generator[T], numTimes: Int = 100)
            (test: T => Boolean): Unit =
        for i <- 0 until numTimes do
            val value = g.generate()
            assert(test(value), s"test failed for $value")
        println(s"passed $numTimes tests")
    ```

- Usage:
    ```scala
    test(pairs(lists, lists)) {
        (xs, ys) => (xs ++ ys).length >= xs.length
    }
    ```

- What are we doing here exactly? Instead of writing unit tests, write _properties_ that are assumed to hold.

- ScalaCheck implementation example:
    ```scala
    forAll { (l1: List[Int], l2: List[Int]) => 
        l1.size + l2.size == (l1 ++ l2).size
    }
    ```

## 1.4 Monads

- What is a Monad? It's a parametric type `M[T]` with two operations
    - `flatMap`
    - `unit`
    - Implementation:
        ```scala
        extension [T, U](m: M[T])
            def flatMap(f: T => M[U]): M[U]

        def unit[T](x: T): M[T]
        ```

- Have we seen Monads before? Yes!
    - `List` is a monad with `unit(x) = List(x)`
    - `Set` with `unit(x) = Set(x)`
    - `Option` with `unit(x) = Some(x)`
    - `Generator` with `unit(x) = single(x)`

- `map` can be defined using `flatMap` and `unit`
    ```scala
    m.map(f) == m.flatMap(x => unit(f(x)))
            == m.flatMap(f andThen unit)
    ```
    - `andThen` is used for function composition

- Monad laws
    - Associativity:
        ```scala
        m.flatMap(f).flatMap(g) == m.flatMap(f(_).flatMap(g))
        ```
    - Left unit:
        ```scala
        unit(x).flatMap(f) == f(x)
        ```
    - Right unit
        ```scala
        m.flatMap(unit) == m
        ```

- Checking laws: skipped

- Signficance of laws for `for`-expressions
    1. Associativity enables "inline" nested for expressions
        ```scala
        for
            y <- for x <- m; y <- f(x) yield y
            z <- g(y)
        yield z
        ```
        `==`
        ```scala
        for x <- m; y <- f(x); z <- g(y)
        yield z
        ```
    2. Right unit says:
        ```scala
        for x <- m yield x
        ```
        `== m`
    
## 1.5 Exceptional Monads

- Handling exceptions in Scala
    ```scala
    class BadInput(msg: String) extends Exception(msg)
    throw BadInput("missing data")
    ```

- Using `try / catch`
    ```scala
    def validatedInput(): String =
        try getInput()
        catch
            case BadInput(msg) => println(msg); validatedInput()
            case ex: Exception => println("fatal error; aborting"); throw ex
    ```

- Critique:
    - Exceptions don't show up in the types of functions that throw them
    - They don't work in parallel computations where we want to communicate an exception from one thread to another
    - Scala has a `Try` type which can be used to treat an exception as a normal function result value.

- `Try`:
    ```scala
    abstract class Try[+T]
    case class Success[+T](x: T) extends Try[T]
    case class Failure(ex: Exception) extends Try[Nothing]
    ```

- We can wrap any computation within a `Try`
    ```scala
    Try(expr) // gives Success(someValue) or Failure(someException)
    ```

- Implementation of `Try.apply`:
    ```scala
    import scala.util.control.NonFatal

    object Try:
        def apply[T](expr: => T): Try[T] =
            try Success(expr)
            catch case NonFatal(ex) => Failure(ex)
    ```

- Composing `Try` with `for`
    ```scala
    for
        x <- computeX
        y <- computeY
    yield f(x, y)
    ```
    - if `computeX` and `computeY` succeed, this returns `Success(f(x, y))`
    - else `Failure(ex)`

- So what are the implementations of `flatMap` and `map`?
    ```scala
    extension [T](xt: Try[T])
        def flatMap[U](f: T => Try[U]): Try[U] = xt match
            case Success(x) => try f(x) catch case NonFatal(ex) => Failure(ex)
            case fail: Failure => fail

        def map[U](f: T => U): Try[U] = xt match
            case Success(x) => Try(f(x))
            case fail: Failure => fail
    ```

- Is `Try` a monad with `unit = Try`? No, `Try(expr).flatMap(f) != f(expr)` as RHS might throw an exception.

- _Bullet-proof_ principle: An expression composed from `Try`, `map`, `flatMap` will never throw a non-fatal exception.
