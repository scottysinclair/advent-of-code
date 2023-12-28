package scott.adventofcode.year2023.day8

import scott.adventofcode.year2023.day8.Direction.*
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.time.Duration
import java.util.TreeMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

fun main() {
   val nodeFactory = NodeFactory()
   val lastPositions = loadLastPositions("day-8.log", nodeFactory)


   System.setOut(PrintStream(FileOutputStream("day-8.log", true)))
   //
   //part1()
   part2(lastPositions, nodeFactory)
}

val regexpLastPos = Regex("Position: (\\d) Step: (\\d+)  Node: (\\S\\S\\S) di: (\\d+)")
fun loadLastPositions(fileName: String, nodeFactory: NodeFactory): Set<PositionData> {
   return runCatching {
      File(fileName).useLines { lines ->
         lines.filter { it.contains("Position:") }
            .map {
             val (pos, step, node, index) = regexpLastPos.find(it)!!.groupValues.drop(1)
            PositionData(pos.toInt(), step.toLong(), nodeFactory.create(node), index.toLong())
         }.map { mapOf(it.positionId to it) }
            .reduce { a, b ->  a + b }
            .values.toSet()
      }
   }.onFailure { it.printStackTrace() }
      .getOrDefault(emptySet())
}

private fun part1() {
   File("src/main/resources/day-8-input.txt").useLines { linesSeq ->
      val lines = linesSeq.toList()
      val directions = parseDirections(lines)
      val nodeFactory = NodeFactory()
      val instructions = parseInstructions(nodeFactory, lines)
      var currentNode = nodeFactory.get("AAA")
      val target = nodeFactory.get("ZZZ")
      val directionsIterator = directions.infiniteIterator()
      var steps = 0L
      while (currentNode != target) {
         currentNode = currentNode.follow(directionsIterator.next())
         steps++;
         if (steps % (1000 * 1000 * 1000) == 0L) println(steps)
      }
      println(steps)
   }
}


private fun part2(lastPositions: Set<PositionData>, nodeFactory: NodeFactory) {

   File("src/main/resources/day-8-input.txt").useLines { linesSeq ->
      val lines = linesSeq.toList()
      val directions = parseDirections(lines)
      parseInstructions(nodeFactory, lines)
      val ruleOutEvery = 1000L
      val reportEvery = Duration.ofMinutes(1)
      var currentPositions = if (lastPositions.size == 6) {
         lastPositions.map { Position(it.positionId, it.node, it.stepNumber, ruleOutEvery, directions, it.directionIndex) }
      }
      else {
         nodeFactory.findAll(Regex("..A")).mapIndexed { i, node -> Position(i, node, 0, ruleOutEvery, directions, 0L) }
      }
      val checkQueue = CheckQueue(currentPositions.size, 10000, reportEvery)
      println("${currentPositions.size} starting positions and threads")
      while(!checkQueue.isFinished()) {
         currentPositions.forEach { it.nextStep(checkQueue) }
      }

      /*
      currentPositions.parallelStream().forEach { position ->
         var lastReportTime = System.currentTimeMillis()
         var finished = false
         while (!finished) {
            if ((System.currentTimeMillis() - lastReportTime) > reportEvery.toMillis()) {
               checkQueue.report()
               lastReportTime = System.currentTimeMillis()
            }
            if (position.nextStep(checkQueue)) {
               finished = true
            }
            if (position.stepsTaken() % 1000 == 0L) {
               finished = checkQueue.isFinished()
            }
         }
      }

       */
      checkQueue.report(true)
      println(checkQueue.getFinishedCheck()!!.stepNumber)
   }
}

class CheckQueue(private val numberOfPositions: Int, private val maxQueueSize: Int, private val reportEvery: Duration) {
   private val stepsToCheck = TreeMap<Long, Check>()
   private val lock = ReentrantLock()
   private val canProceeed = lock.newCondition()
   private val lastPositions = mutableMapOf<Int,PositionData>()
   private var finishedCheck: Check? = null
   private var lastReport = 0L

   fun report(override: Boolean) = lock.withLock{
      if (override || System.currentTimeMillis() - lastReport > reportEvery.toMillis()) {
         println("Reporting.....")
         println("Queue size ${stepsToCheck.size}")
         lastPositions.values.forEach { lastPos ->
            println("Position: ${lastPos.positionId} Step: ${lastPos.stepNumber}  Node: ${lastPos.node.text} di: ${lastPos.directionIndex}")
         }
         lastReport = System.currentTimeMillis()
      }
   }

   fun ruleOut(positionData: PositionData) = lock.withLock {
//      println("Ruling out ${positionData.stepNumber} for pos ${positionData.positionId}, qz: ${stepsToCheck.size}")
      //remove checks if this positionData jumped it
      val i = stepsToCheck.values.iterator()
      while(i.hasNext()) {
         val currentCheck = i.next()
         if (currentCheck.stepNumber <= positionData.stepNumber) {
            if (positionData.positionId !in currentCheck.positionIds) {
//               println("Processing step ${positionData.stepNumber} removing check $currentCheck because position ${positionData.positionId} was not in it")
               i.remove()
            }
         }
         else break
      }
   }


   fun add(positionData: PositionData) : Boolean = lock.withLock {
//      println("Processing step ${positionData.stepNumber} for position ${positionData.positionId}, qz: ${stepsToCheck.size}")
      if (finishedCheck != null) return@withLock true
      lastPositions[positionData.positionId] = positionData

      val theCheck = stepsToCheck[positionData.stepNumber] ?: Check(positionData.stepNumber, mutableSetOf(), mutableSetOf())
      /*
       * if there is a higher step with a positionId which is missing from the current step check then
       * we don't need to add a new check for the step.
       */
      if (stepsToCheck.values.count { it.stepNumber > positionData.stepNumber &&  (it.positionIds - theCheck.positionIds).isNotEmpty() } == 0) {
         theCheck.add(positionData)
//         println("Processing step ${positionData.stepNumber} Adding check for position ${positionData.positionId} at step ${positionData.stepNumber}, node ${positionData.node.text}")
         if (theCheck.positionIds.size == 1) {
            stepsToCheck[positionData.stepNumber] = theCheck
         }
      }
      else {
//         println("Processing step ${positionData.stepNumber} Not adding check for position ${positionData.positionId} at step ${theCheck.stepNumber}, it would be redundant")
      }
      if (theCheck.positionIds.size == numberOfPositions) {
         println("Processing step ${positionData.stepNumber} Found Step ${theCheck.stepNumber} has all $numberOfPositions positions")
         finishedCheck = theCheck
         canProceeed.signalAll()
         return@withLock true
      }
      else {
         //remove earlier checks if this position jumped it
         val i = stepsToCheck.values.iterator()
         while(i.hasNext()) {
            val currentCheck = i.next()
            if (currentCheck.stepNumber < positionData.stepNumber) {
               if (positionData.positionId !in currentCheck.positionIds) {
    //              println("Processing step ${positionData.stepNumber} removing check $currentCheck because position ${positionData.positionId} was not in it")
                  i.remove()
               }
            }
            else break
         }
         false
      }.also {
      //   println("Finishing processing step ${positionData.stepNumber} for position ${positionData.positionId}, qz: ${stepsToCheck.size}")
         /*
          * have some wait logic at the end. so that our positionData had the chance to remove checks
          */
         if (stepsToCheck.size > maxQueueSize) {
            println("Processing step ${positionData.stepNumber} Position ${positionData.positionId} is waiting")
            canProceeed.awaitUninterruptibly()
            println("Processing step ${positionData.stepNumber} Position ${positionData.positionId} is resuming")
            if (finishedCheck != null) return@withLock true
         }
         else if (stepsToCheck.size < 100) {
            canProceeed.signalAll()
         }
         report(true)
      }
   }
   fun getFinishedCheck(): Check? = lock.withLock { finishedCheck }
   fun isFinished() = getFinishedCheck() != null
}

data class PositionData(val positionId: Int, val stepNumber: Long, val node: Node, val directionIndex: Long) {
   override fun toString(): String {
      return "PositionData(positionId: ${positionId}, stepNumber: $stepNumber, node: ${node.text}  directionIndex: $directionIndex)"
   }
}

data class Check(val stepNumber: Long, val positionIds: MutableSet<Int>, val nodes: MutableSet<Node>) {
   fun add(positionData: PositionData) {
      require(positionIds.add(positionData.positionId)) { "step ${stepNumber }already had position ${positionData.positionId} in the check "}
      this.nodes.add(positionData.node)
   }

   override fun toString(): String {
      return "Check(stepNumber=$stepNumber, positionsIds=${positionIds} nodes=${nodes.map { it.text }})"
   }
}


class Position(private val id: Int, private var node: Node, private var stepsTaken: Long, private val ruleOutEvery: Long, directions: List<Direction>, directionIndex: Long) {
   private val directionsIterator = directions.infiniteIterator(directionIndex)
   fun reachedGoal(): Boolean = node.text.endsWith('Z')

   /**
    * @return true if all positions are finished
    */
   fun nextStep(checkQueue: CheckQueue): Boolean {
      node = node.follow(directionsIterator.next())
      stepsTaken++
      return if (reachedGoal()) {
         //println("$id  adding check for step $stepsTaken  at node ${node.text}")
         checkQueue.add(PositionData(id, stepsTaken, node, directionsIterator.directionIndex()))
      }
      else {
         if (stepsTaken % ruleOutEvery == 0L) {
            checkQueue.ruleOut(PositionData(id, stepsTaken, node, directionsIterator.directionIndex()))
         }
         false
      }
   }

   fun node(): Node = node
   fun stepsTaken(): Long = stepsTaken

   override fun toString(): String {
      return "Position(id: $id, ${node.text}  stepsTaken: $stepsTaken)"
   }

}

fun allEndInZ(nodes: Collection<Node>): Boolean {
   return nodes.all { it.text.endsWith("Z") }
}

fun <T> List<T>.infiniteIterator(directionIndex: Long = 0) = InfiniteIterator(this, directionIndex)

class InfiniteIterator<T>(private val source: List<T>, private var nextIndex: Long=0) : Iterator<T> {
   private var i = source.listIterator()
   init {
      //go to the start position
      (0 until (nextIndex % source.size)).forEach { i.next() }
   }

   override fun hasNext(): Boolean = true

   override fun next(): T {
      if (!i.hasNext()) {
         i = source.listIterator()
      }
      return i.next().also { nextIndex = i.nextIndex().toLong() }
   }

   fun directionIndex(): Long = nextIndex
}

fun parseDirections(lines: List<String>): List<Direction> {
   return lines.first().map { Direction.parse(it) }
}

val instruction_regex = Regex("(\\S\\S\\S) = .(\\S\\S\\S), (\\S\\S\\S).")
fun parseInstructions(nodeFactory: NodeFactory, lines: List<String>): List<Node> {
   return lines.mapNotNull {
      instruction_regex.find(it)?.let {
         val (text, left, right) = it.groupValues.drop(1)
         nodeFactory.create(text, left, right)
      }
   }
}


enum class Direction(private val char: Char) {
   LEFT('L'),
   RIGHT('R');

   companion object {
      fun parse(char: Char): Direction {
         return entries.first { it.char == char }
      }
   }
}

class NodeFactory {
   private val nodes = mutableMapOf<String, Node>()
   fun create(meText: String, leftText: String, rightText: String): Node {
      val node = nodes.computeIfAbsent(meText) { Node(it) }
      val left = nodes.computeIfAbsent(leftText) { Node(it) }
      val right = nodes.computeIfAbsent(rightText) { Node(it) }
      return node.apply { set(left, right) }
   }
   fun create(meText: String): Node {
      return nodes.computeIfAbsent(meText) { Node(it) }
   }

   fun get(text: String): Node = nodes[text]!!
   fun findAll(regex: Regex): List<Node> {
      return nodes.values.filter { it.text.matches(regex) }
   }
}

class Node(val text: String) {
   private lateinit var left: Node;
   private lateinit var right: Node;
   fun set(left: Node, right: Node) {
      this.left = left;
      this.right = right;
   }

   fun follow(direction: Direction): Node {
      return when (direction) {
         LEFT -> left
         RIGHT -> right
      }
   }

   fun getLeft() = left
   fun getRight() = right
   override fun toString(): String {
      return "Node(text='$text', left=${left.text}, right=${right.text})"
   }

}
