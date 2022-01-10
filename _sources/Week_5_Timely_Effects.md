# 5. Timely Effects

## 5.1 Imperative Event Handling: The Observer Pattern

- The Observer Pattern is used when views need to react to changes in a model.

- Example: Publisher and Subscriber Traits
    ```scala
    trait Subscriber:
        def handler(pub: Publisher): Unit

    trait Publisher:
        private var subscribers: Set[Subscriber] = Set()

        def subscribe(subscriber: Subscriber): Unit =
            subscribers += subscriber
        
        def unsubscribe(subscriber: Subscriber): Unit =
            subscribers -= subscriber
        
        def publish(): Unit =
            subscribers.foreach(_.handler(this))
            
    end Publisher
    ```
    - Using this, `BankAccount`s can be made a `Publisher`.

## 5.2 Functional Reactive Programming

- Reactive Programming: Reacting to sequences of events that happen in time.

- Functional view: aggregate an event squence into a signal.
    - A signal is a value that changes over time.
    - Represented as a function from time to the value domain
    - Instad of propogating updates to mutable state, we define new signals in terms of existing ones.

- Example: Mouse Positions
    - Event-based View: When the mouse moves, an event
        ```
        MouseMoved(toPos: Position)
        ```
        is fired.

    - FRP View: A signal, 
        ```
        mousePosition: Signal[Position]
        ```
        which at any point in time represents the current moust position.

- Fundamental Signal Operations
    1. Obtain value at the current time. Expressed by `()` application (e.g: `mousePosition()`)
    2. Define a signal in terms of other signals
        ```scala
        def inRectangle(LL: Position, UR: Position): Signal[Boolean] = 
            Signal {
                val pos = mousePosition()
                LL <= pos && pos <= UR
            }
        ```

- How is the above definition different from a function that just returns a Boolean?
    - This creates a signal that reflects whether `mousePosition` at that point of time is in the box.
    - Each time this signal is queried will give the state at that point of time.

- Possibilities:
    - A signal can be evaluated on demand, when needed.
    - Sample at certain points
    - If discrete, propogate automatically to dependent to required systems

- Time-varying signals
    - Expressions of type `Signal` cannot be updated. But `Signal.Var` can be.
        ```scala
        val sig = Signal.Var(3)
        sig.update(5)
        ```
    - There's an easier way to update. `sig()` is derefrencing, and `sig() = newValue` is update.
    - Another crucial difference: we can apply functions to signals, which gives us a relation between two signals that is maintained automatically, at all future points in time.
    - No such mechanism exists for mutable variables, we have to propogate all updates manually.

- Example
    ```scala
    var x = 1
    var y = x*2
    x = 3 // y = 2

    val x = Signal.Var(1)
    val y = Signal(x * 2)
    x() = 3 // y() = 6!
    ```

- Apply to BankAccount
```scala
class BankAccount:
    def balance: Signal[Int] = myBalance

    private val myBalance = Signal.Var[Int](0)

    def deposit(amount: Int): Unit =
        if amount > 0 then 
            val b = myBalance()
            myBalance() = b + account

    def withdraw(amount: Int): Int = 
        if amount > 0 && amount <= balance() then
            val b = myBalance()
            myBalance() = b - amount // myBalance() = myBalance() - amount: won't work!
            myBalance()
        else throw Error("insufficient funds")
end BankAccount

def consolidated(accts: List[BankAccount]): Signal[Int] = 
    Signal(accts.map(_.balance()).sum)
```

## 5.3 A Simple FRP Implementation
- Went through an implementation, I skipped notes for this as it was mostly code.
