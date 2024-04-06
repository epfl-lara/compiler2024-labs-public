package alpine_rt
import scala.reflect._

def panic: Nothing = { println("panic"); sys.exit(-1); }

object builtin:
  def print(s: Any): Unit = System.out.print(s)
  def iadd(a: Int, b: Int) = a+b
  def isub(a: Int, b: Int) = a-b
  def imul(a: Int, b: Int) = a*b
  def idiv(a: Int, b: Int) = a/b
  def irem(a: Int, b: Int) = a%b
  def exit(a: Int) = sys.exit(a)
  def lnot(a: Boolean): Boolean = !a
  def land(a: Boolean, b: Boolean): Boolean = a && b
  def lor(a: Boolean, b: Boolean): Boolean = a || b
  def ineg(a: Int): Int = -a
  def ishl(a: Int, b: Int): Int = a << b
  def ishr(a: Int, b: Int): Int = a >> b
  def ilt(a: Int, b: Int): Boolean = a < b
  def ile(a: Int, b: Int): Boolean = a <= b
  def igt(a: Int, b: Int): Boolean = a > b
  def ige(a: Int, b: Int): Boolean = a >= b
  def iinv(a: Int): Int = ~a
  def iand(a: Int, b: Int): Int = a & b
  def ior(a: Int, b: Int): Int = a | b
  def ixor(a: Int, b: Int): Int = a ^ b
  def fneg(a: Float): Float = -a
  def fadd(a: Float, b: Float): Float = a + b
  def fsub(a: Float, b: Float): Float = a - b
  def fmul(a: Float, b: Float): Float = a * b
  def fdiv(a: Float, b: Float): Float = a / b
  def flt(a: Float, b: Float): Boolean = a < b
  def fle(a: Float, b: Float): Boolean = a <= b
  def fgt(a: Float, b: Float): Boolean = a > b
  def fge(a: Float, b: Float): Boolean = a >= b
  
// T is the type in which we want to narrow
// V is the returned type (option)
def narrow[T: ClassTag,V](a: Any, someConstructor: T => V, none: V): V =
  a match
    case t: T => someConstructor(t)
    case _ => none

def narrowUnconditionally[T: ClassTag](a: Any): T =
  a match
    case t: T => t
    case _ => panic