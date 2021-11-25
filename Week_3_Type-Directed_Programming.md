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

- Using Clasuses matched explicitly
    ```scala
    def sort[T](xs: List[T])(using ord: Ordering[T]): List[T] = ...

    sort(strings)(using Ordering.String)
    ```

- Using clasuses syntax:
    ```scala
    def f(x: Int)(using a: A, b: B) = ...
    f(x)(using a, b)

    // or

    def f(x: Int)(using a: A)(using b: B) = ...

    // or mixed

    def f(x: Int)(using a: A)(y: Boolean)(using b: B) = ...
    f(x)(using a)(y)(using b) // called explicitly
    ```

- Parameters of a `using` clause can be anonymous. Useful when the body of the function doesn't mention the parameter at all (simply passes it implicitly)
    ```scala
    def sort[T](xs: List[T])(using Ordering[T]): List[T] =
        ...
        ... merge(sort(fst), sort(snd))

    def merge[T](xs: List[T])(using Ordering[T]): List[T] = ...
    ```

- Using clause with context bounds:
    ```scala
    // instead of
    def printSorted[T](as: List[T])(using Ordering[T]) =
        println(sort(as))

    // with context bound
    def printSorted[T: Ordering](as: List[T]) =
        println(sort(as))
    ```

- `Given` instances.
    - For the above example to work, `Ordering.Int` definition must be a `given` instance.
        ```scala
        object Ordering:
            given Int: Ordering[Int] with
                def compare(x: Int, y: Int): Int =
                    if x < y then -1 else if x > y then 1 else 0
        ```

- Given instances can also be anonymous
    ```scala
    given Ordering[Double] with
        def compare(x: Int, y: Int): Int = ...

    // will be synthesized to
    given given_Ordering_Double: Ordering[Double] with
        def compare(x: Int, y: Int): Int = ...
    ```

- Summoning an Instance: we can refer to both named and anonymous instances by its type:
    ```scala
    summon[Ordering[Int]]
    summon[Ordering[Double]]

    // which expand to:
    Ordering.Int
    Ordering.given_Ordering_Double
    ```

- Implicit Parameter Resolution: for an implicit parameter of type `T`, what `given` instances will the complier search for?
    - has a type compatible with `T`,
    - is visible at the point of the function call, or is defined in a companion object associated with `T`.

- The search for a given instance of type `T` includes
    - all the given instances that are visible (inherited, imported, or defined in an enclosing scope),
    - the given instances found in a companion object _associated_ with T.

- What does _associated_ mean? Besides the companion object of a class itself, the compiler will also consider:
    - companion objects associated with any of T’s inherited types
    - companion objects associated with any type argument in T
    - if T is an inner class, the outer objects in which it is embedded

- Example:
    ```scala
    trait Foo[T]
    trait Bar[T] extends Foo[T]
    trait Baz[T] extends Bar[T]
    trait X
    trait Y extends X
    ```
    - if a `given` instance of type `Bar[Y]` is required, compiler will look into the companion objects:
        - `Bar`
        - `Y`
        - `X`
        - `Foo` (not `Baz`)

- Ambiguous given isntances:
    ```scala
    trait C:
    val x: Int
    given c1: C with
    val x = 1
    given c2: C with
    val x = 2
    f(using c: C) = ()
    f
    ^
    error: ambiguous implicit arguments:
    both value c1 and value c2
    match type C of parameter c of method f
    ```

- Priorities: several given instances don't always generate ambiguity if one is more specific than the other
    - `given a: A` is more specific than `given b: B` if:
        - a is in a closer lexical scope than b, or
        - a is defined in a class or object which is a subclass of the class defining b, or
        - type A is a generic instance of type B, or
        - type A is a subtype of type B

- Example 1: which given instance is summoned?
    ```scala
    class A[T](x: T)
    given universal[T](using x: T): A[T](x)
    given specific: A[Int](2)

    summon[A[Int]]
    ```
    - `specific`

- Example 2:
    ```scala
    trait A:
        given ac: C
    trait B extends A:
        given bc: C
    object O extends B:
        val x = summon[C]
    ```
    - `bc`

- Example 3:
    ```scala
    given ac: C
    def f() =
        given b: C
        def g(using c: C) = ()
        
        g
    ```
    - `b`

## 3.3 Type Classes
