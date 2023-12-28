package scott.adventofcode.year2023.day8

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sun.source.tree.Tree
import scott.adventofcode.year2023.day8.Direction.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.time.Duration
import java.util.TreeMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

fun main() {
   //part1()
   part2()
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


private fun part2() {

   BufferedOutputStream(FileOutputStream("day-8-report.json")).use { reportStream  ->
      File("src/main/resources/day-8-input.txt").useLines { linesSeq ->
         val lines = linesSeq.toList()
         val directions = parseDirections(lines)
         val nodeFactory = NodeFactory()
         parseInstructions(nodeFactory, lines)
         val ruleOutEvery = 1000L
         val reportEvery = Duration.ofMinutes(30)
         val currentPositions = nodeFactory.findAll(Regex("..A")).mapIndexed { i, node -> Position(i, node, 0, ruleOutEvery, directions, 0L) }
         val checkQueue = CheckQueue(currentPositions.size, 10000, reportEvery)
         println("${currentPositions.size} starting positions and threads")

         currentPositions.parallelStream().forEach { position ->
            var lastReportTime = System.currentTimeMillis()
            var finished = false
            while (!finished) {
               if ((System.currentTimeMillis() - lastReportTime) > reportEvery.toMillis()) {
                  checkQueue.report(reportStream)
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


         checkQueue.report(reportStream, true)
         println(checkQueue.getFinishedCheck()!!.stepNumber)
      }
   }
}

class CheckQueue(private val numberOfPositions: Int, private val maxQueueSize: Int, private val reportEvery: Duration) {
   private val stepsToCheck = TreeMap<Long, Check>()
   private val lock = ReentrantLock()
   private val canProceeed = lock.newCondition()
   private val lastPositions = TreeMap<Int,PositionData>()
   private var finishedCheck: Check? = null
   private var lastReport = 0L
   private val mapper = jacksonObjectMapper().apply {
      enable(SerializationFeature.INDENT_OUTPUT)
      factory.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
   }

   fun report(stream: OutputStream,  override: Boolean = false) = lock.withLock{
      if (override || System.currentTimeMillis() - lastReport > reportEvery.toMillis()) {
         println("Reporting.....")
         mapOf(
            "numberOfStepsToCheck" to stepsToCheck.size,
            "stepsToCheck" to stepsToCheck,
            "lastPositions" to lastPositions,
            "finishedCheck" to finishedCheck).let {
            mapper.writeValue(stream, it)
         }
         lastReport = System.currentTimeMillis()
      }
   }

   fun ruleOut(positionData: PositionData) = lock.withLock {
      lastPositions[positionData.positionId] = positionData
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
//         report(true)
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
      require(positionIds.add(positionData.positionId)) { "step ${stepNumber } already had position ${positionData.positionId} in the check "}
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

   @JsonIgnore
   fun getLeft() = left
   @JsonIgnore
   fun getRight() = right
   override fun toString(): String {
      return "Node(text='$text', left=${left.text}, right=${right.text})"
   }

}
