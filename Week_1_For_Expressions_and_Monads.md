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

