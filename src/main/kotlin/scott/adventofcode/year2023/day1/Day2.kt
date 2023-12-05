package scott.adventofcode.year2023.day1

import java.io.File
import kotlin.math.min

fun main() {
   part2()
}
fun part1() {
   File("src/main/resources/day-2-input.txt").useLines { lines ->
      lines
         .map(::parseGame)
         .filter { game ->
            filterPossible(game, 12, 13, 14)
         }
         .map { game -> game.id }
         .sum()
         .let(::println)
   }
}

fun part2() {
   File("src/main/resources/day-2-input.txt").useLines { lines ->
      lines
         .map(::parseGame)
         .map(::calculateMinimumCubes)
         .map { minumumRequirement ->  minumumRequirement.redCount * minumumRequirement.blueCount * minumumRequirement.greenCount }
         .sum()
         .let(::println)
   }
}

fun calculateMinimumCubes(game: Game) : MinumumRequirement {
   return game.sets.fold(MinumumRequirement(0, 0, 0)) { minimumRequirement, set ->
      MinumumRequirement(
         redCount = if (set.getCount(Color.RED) > minimumRequirement.redCount) set.getCount(Color.RED) else minimumRequirement.redCount,
         greenCount = if (set.getCount(Color.GREEN) > minimumRequirement.greenCount) set.getCount(Color.GREEN) else minimumRequirement.greenCount,
         blueCount = if (set.getCount(Color.BLUE) > minimumRequirement.blueCount) set.getCount(Color.BLUE) else minimumRequirement.blueCount,
      )
   }
}

fun filterPossible(game: Game, redCount: Int, greenCount: Int, blueCount: Int): Boolean {
   return game.sets.all { set ->
      set.colorCounts.all { colorCount ->
         when {
            colorCount.color == Color.RED && colorCount.count > redCount -> false
            colorCount.color == Color.GREEN && colorCount.count > greenCount -> false
            colorCount.color == Color.BLUE && colorCount.count > blueCount -> false
            else -> true
         }
      }
   }
}

fun parseGame(line: String): Game {
   val matchGame = Regex("Game\\s+(\\d+):").find(line)!!
   val gameId = matchGame.groupValues[1].toInt()
   val setStrings = line.substring(matchGame.range.last + 1).split(";")
   val sets = setStrings.map { setString ->
      Set(setString.trim().split(",").map { repString ->
         val (count, color) = repString.trim().split(" ")
         ColorCount(count.toInt(), Color.valueOf(color.uppercase()))
      })
   }
   return Game(gameId, sets)
}

data class Game(val id: Int, val sets: List<Set>)
data class Set(val colorCounts: List<ColorCount>) {
   fun getCount(color: Color) : Int = colorCounts.firstOrNull { it.color == color }?.count ?: 0
}
data class ColorCount(val count: Int, val color: Color)
enum class Color { RED, GREEN, BLUE }
data class MinumumRequirement(val redCount: Int, val blueCount: Int, val greenCount: Int)