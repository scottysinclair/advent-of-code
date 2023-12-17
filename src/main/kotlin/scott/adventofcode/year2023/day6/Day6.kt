package scott.adventofcode.year2023.day6

import java.io.File
import java.time.Duration

fun main() {
   //part1()
   part2()
}

fun part1() {
   File("src/main/resources/day-6-input.txt").useLines { lines ->
      val races = parseRacePart1(lines)
      races.map { race ->
         race.getPossibleHoldDurations().count { it.getSpeed().beatsRecord() }
      }.reduce { a, b -> a * b}
         .let(::println)
   }
}

fun part2() {
   File("src/main/resources/day-6-input.txt").useLines { lines ->
      val race = parseRacePart2(lines)
      race.getPossibleHoldDurations().count { it.getSpeed().beatsRecord() }
         .let(::println)
   }
}

private fun parseTimes(line: String): List<Int> {
   return line.split(Regex("\\s+")).drop(1).map { it.toInt() }
}

private fun parseDistances(line: String): List<Int> {
   return line.split(Regex("\\s+")).drop(1).map { it.toInt() }
}

private fun parseRacePart1(linesSeq: Sequence<String>) : List<Race> {
   return linesSeq.toList().let { (firstLine, secondLine) ->
      val times = parseTimes(firstLine)
      val distances = parseDistances(secondLine)
      times.indices.map { i ->
         Race(times[i].toLong(), distances[i].toLong())
      }
   }
}

private fun parseRacePart2(linesSeq: Sequence<String>) : Race {
   return linesSeq.toList().let { (firstLine, secondLine) ->
      val times = parseTimes(firstLine)
      val distances = parseDistances(secondLine)
      val mergedTimes = times.map { it.toString() }.joinToString("").toLong()
      val mergedDistances = distances.map { it.toString() }.joinToString("").toLong()
      return Race(mergedTimes,  mergedDistances)
   }
}

data class Race(val time: Long, val distanceRecord: Long) {
   fun getPossibleHoldDurations(): List<HoldDuration> {
      return (0..time).toList().map { HoldDuration(this, it) }
   }
}

data class HoldDuration(val race: Race, val millis: Long) {
   fun getSpeed(): Speed {
      val millimetersPerMillisecond = millis * 1
      return Speed(this, millimetersPerMillisecond)
   }
   fun getTimeLeft() : Long = race.time - millis
}

data class Speed(val holdDuration: HoldDuration, val millimetersPerMillisecond: Long) {
   val race = holdDuration.race
   fun beatsRecord() : Boolean = getDistanceAchieved() > race.distanceRecord
   fun getDistanceAchieved() : Long = millimetersPerMillisecond * holdDuration.getTimeLeft()
}
