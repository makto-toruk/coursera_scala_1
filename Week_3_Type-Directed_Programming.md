# 3. Type-Directed Programming

## 3.1 Contextual Abstractions

- Origins of "con-text"
    - "what comes with the text, but is not in the text"

- How is context represented in other languages?
    - Global values,
    - Global mutable variables
    - Monkey Patching
    - Dependency injection frameworks

- Functional Context Representation
    - Context is represented with function parameters. 
    - Advantages:
        - Flexible
        - Types are checked
        - Not relying on side effects
    - Disadvantages:
        - Many function arguments
        - Which hardly ever change
        - Repetitive, errors are hard to spot.
    - But Scala can overcome some of these disadvantages.

- Example: Sorting
    ```scala
    def sort(xs: List[Int]): List[Int] = 
        ...
            if x < y then ...
        ...
    ```

- How can this be generalized to all types?
    ```scala
    def sort[T](xs: List[T]): List[T] = ...
    ```
    - But we don't have a single comparison function that works for all `T`

- We can pass a function
    ```scala
    def sort[T](xs: List[T])(lessThan: (T, T) => Boolean): List[T] = 
        ...
            if lessThan(x, y) then ...
        ...
    ```

- Examples:
    ```scala
    val ints = List(-5, 6, 3, 2, 7)
    val strings = List("apple", "pear", "orange", "pineapple")

    sort(ints)((x, y) => x < y)
    sort(strings)((s1, s2) => s1.compareTo(s2) < 0)
    ```

- Parametrization with Ordering:
    ```scala
    def sort[T](xs: List[T])(ord: Ordering[T]): List[T] = 
        ...
            if ord.lt(x, y) then ...
        ...
    ```
    - There already exists a class in the scala standard lib that represents orderings: `scala.math.Ordering[A]`

- Examples:
    ```scala
    import scala.math.Ordering

    sort(ints)(Ordering.Int)
    sort(strings)(Ordering.String)
    ```
    - But can the compiler simply infer the ordering from the type of the input? Yes.

- With Implicit Parameters
    ```scala
    def sort[T](xs: List[T])(using ord: Ordering[T]): List[T] = ...

    sort(ints)
    sort(strings)
    ```

- Type Inference: We have seen severalexamples where the compiler infers _types_ from _values_.

- Term Inference: But the Scala compiler is capable of the opposite as well. To infer _expressions_ (or _terms_) from _types_.
    ```scala
    sort(ints) // sort[Int](ints)(using Ordering.Int)
    ```

## 3.2 Using clauses and given instances