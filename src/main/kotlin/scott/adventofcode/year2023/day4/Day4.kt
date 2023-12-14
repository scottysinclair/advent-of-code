package scott.adventofcode.year2023.day4

import java.io.File
import kotlin.math.pow

fun main() {
 //  part1()
   part2()
}

fun part1() {
   File("src/main/resources/day-4-input.txt").useLines { lines ->
      lines.map(::parse)
         .map(Card::getPoints)
         .sum()
         .let(::println)
   }
}

fun part2() {
   File("src/main/resources/day-4-input.txt").useLines { lines ->
      lines.map(::parse)
         .map { Copies(it, 1) }
         .toList()
         .also { allCopies ->
            getCopies(allCopies.iterator(), allCopies)
         }
         .map { it.copies }
         .sum()
         .let(::println)
   }
}

fun getCopies(iterator: Iterator<Copies>, allCopies: List<Copies>) {
   val cardCopies = iterator.next()
   /*
    * each copy of a card is processed as a normal card
    */
   repeat(cardCopies.copies) {
      /*
       * get the range from the match count
       */
      val matchCount = cardCopies.card.getMatchCount()
      val cardCopyStartIndex = cardCopies.card.id + 1
      val cardCopyEndIndexIncl = cardCopies.card.id + matchCount
      /*
       * for each card in the range add a copy
       */
      (cardCopyStartIndex .. cardCopyEndIndexIncl).mapNotNull { cardIdToCopy ->
         allCopies.find { copies -> copies.card.id == cardIdToCopy }?.incremement()
      }
   }
   /*
    * repeat for the next card
    */
   if (iterator.hasNext()) {
      getCopies(iterator, allCopies)
   }
}

fun parse(line: String) : Card {
   return runCatching {
      val result = Regex("Card\\W+(\\d+):(.+)\\|(.+)").find(line)!!
      val id = result.groupValues[1].toInt()
      val winningNumbers = result.groupValues[2].split(" ").map { it.trim() }.filter { it.isNotEmpty() }.map { it.toInt() }.toSet()
      val actualNumbers = result.groupValues[3].split(" ").map { it.trim() }.filter { it.isNotEmpty() }.map { it.toInt() }.toSet()
      Card(id, winningNumbers, actualNumbers)
   }
      .getOrElse {x ->
         throw IllegalStateException("Could not parse line $line", x)
      }
}

data class Copies(val card: Card, var copies: Int) {
   fun incremement() {
      copies++
   }
}

data class Card(val id: Int, val winningNumbers: Set<Int>, val actualNumbers: Set<Int>) {
   fun getMatchCount() : Int = actualNumbers.intersect(winningNumbers).size
   fun getPoints() : Int {
      val matchCount = getMatchCount()
      return when {
         matchCount == 0 -> 0
         else -> (1 * (2f.pow(matchCount  - 1))).toInt()
      }
   }
}