package alpine.util

/** The state of memoization of a computation, including an "in progress" to detect cycles. */
enum Memo[+T]:

  /** Computation is in progress. */
  case InProgres extends Memo[Nothing]

  /** Result has been computed and stored in the payload. */
  case Computed[T](value: T) extends Memo[T]

end Memo
