# 4. Functions and State

## 4.1 Functions and State

- Mutable states are constructed through "variables". They're written like a value definition but with `var`:
    ```scala
    var x: String = "abc"
    var count = 111
    ```

- However, associations for variables can be changed later though an assignment:
    ```scala
    x = "hi"
    count = count + 1
    ```

- State using Objects:
    ```scala
    class BankAccount:
        private var balance = 0 // cannot be accessed from outside the class

        def deposit(amount: Int): Unit =
            if amount > 0 then balance = balance + account

        def withdraw(amount: Int): Int = 
            if amount > 0 && amount <= balance then
                balance = balance - amount
                balance
            else throw Error("insufficient funds")
    ```

- Working with Mutable Objects:
    ```scala
    val account = BankAccount()
    account.deposit(50) // 
    account.withdraw(20) // balance = 30
    account.withdraw(20) // balance = 10
    account.withdraw(20) // Error: insufficient funds
    ```
    - `account.withdraw(20)` produces different results. Accounts are "stateful objects".

---

## 4.2 Identity and Change

- Operational equivalence: `x` and `y` are operationally equivalent if no possible test can distinguish between them.

- If `val x = E; val y = E`, `x` and `y` are the same. So we could have rewritten `val x = E; val y = x`

- Suppose:
    ```scala
    val x = BankAccount()
    val y = BankAccount()
    ```
    Are `x` and `y` the same here?

- To test if they are the same, execute the definitions followed by an arbitrary sequence of operations that involves x and y, observing the outcomes: say `S`.

- Then, execute the definitions with another sequence `S'` obtained by renaming all occurrences of `y` by `x` in `S`. If the results are differnet, `x` and `y` are different.

- Counterexample:
    ```scala
    val x = BankAccount()
    val y = BankAccount()
    x.deposit(30) // 30
    y.withdraw(20) // insufficient funds
    ```
    - replace `y` by `x`:
        ```scala
        val x = BankAccount()
        val y = BankAccount()
        x.deposit(30) // 30
        x.withdraw(20) // 10 (this is different)
        ```
        Conclude that `x` and `y` are not the same.

- On the otherhand
    ```scala
    val x = BankAccount()
    val y = x
    ```
    results in `x` and `y` being the same. So our original substitution model ceases to be valid when we add the assignment. 

---

## 4.3 Loops

- Scala program that uses a `while` loop:
    ```scala
    def power(x: Double, exp: Int): Double = 
        var r = 1.0
        var i = exp
        while i > 0 do { r = r * x; i = i - 1 }
        r
    ```
    - `while-do` is a built-in control construct, but we can do this with a function

```scala
def whileDo(condition: => Boolean)(command: => Unit): Unit = 
    if condition then
        command
        whileDo(condition)(command)
    else ()
```
- condition and command must be passed by name, so that they're reevaluated in each iteration
- `whileDo` is tail recursive.
- example call: `whileDo(x > 0)(y = y * y: x = x - 1)`

- Implement `repeatUntil` (similar to `doWhile`)
    ```scala
    def repeatUntil(command: => Unit)(condition: => Boolean): Unit = 
        command
        if !condition then
            repeatUntil(command)(condition)
        else ()
    ```

## 4.4 Advanced Example: Discrete Event Simulation

- Digital Circuits
    - components: logical gates
    - constraints: delays

- Digital Circuid Diagrams
    - complex circuits can be created from logical gates
    - how do we do this through code?

- A language for digital circuits
    ```scala
    val a, b, c = Wire()

    def inverter(input: Wire, output: Wire): Unit
    def andGate(in1: Wire, in2: Wire, output: Wire): Unit
    def orGate(in1: Wire, in2: Wire, output: Wire): Unit
    ```
    - read `andGate` as: place and and gate between the two input wires and the output wire.

- Half-adder:
    ```scala
    def halfAdder(a: Wire, b: Wire, s: Wire, c: Wire): Unit =
        val d = Wire()
        val e = Wire()
        orGate(a, b, d)
        andGate(a, b, c)
        inverter(c, e)
        andGate(d, e, s)
    ```
    - You could then use this in a full adder (recommend looking at a logic diagram)

- How do we implement these? We need an API for discrete event simulation.

- Overview: Simulation -> Gates -> Circuits -> Sim (test circuit)

```scala
trait Simulation:
    
    def currentTime: Int = ???
    
    def afterDelay(delay: Int)(block: => Unit): Unit = 
        val item = Event(currentTime + delay, () => block)
        agenda = insert(agenda, item)

    def run(): Unit = 
        afterDelay(0) {
            println(s"simulation started, time = $currentTime")
        }
        loop()

end Simulation

trait Gates extends Simulation:
    
    def InverterDelay: Int
    def AndGateDelay: Int
    def OrGateDelay: Int
    
    class Wire:
        private var sigVal = false
        private var actions: List[Action] = List()

        def getSignal(): Boolean = sigVal

        def setSignal(s: Boolean): Unit = 
            if s != sigVal then
                sigval = s
                actions.foreach(_())

        def addAction(a: Action): Unit = 
            actions = a :: actions
            a()

    def inverter(input: Wire, output: Wire): Unit = 
        def invertAction(): Unit = 
            val inputSig = input.getSignal()
            afterDelay(InverterDelay) { output.setSignal(!inputSig) }
        input.addAction(invertAction)

    def andGate(in1: Wire, in2: Wire, output: Wire): Unit = 
        def andAction(): Unit = 
            val in1Sig = in1.getSignal()
            val in2Sig = in2.getSignal()
            afterDelay(AndGateDelay) { output.setSignal(in1Sig & in2Sig) }
        in1.addAction(andAction)
        in2.addAction(andAction)

    def orGate(in1: Wire, in2: Wire, output: Wire): Unit = 
        def orAction(): Unit = 
            val in1Sig = in1.getSignal()
            val in2Sig = in2.getSignal()
            afterDelay(OrGateDelay) { output.setSignal(in1Sig | in2Sig) }
        in1.addAction(orAction)
        in2.addAction(orAction)
    
end Gates

trait Circuits extends Gates:
    
    def halfAdder(a: Wire, b: Wire, s: Wire, c: Wire): Unit =
        val d = Wire()
        val e = Wire()
        orGate(a, b, d)
        andGate(a, b, c)
        inverter(c, e)
        andGate(d, e, s)

end Circuits
```
