package scott.adventofcode.year2023.day7

import java.io.File


fun main() {
   //part1()
   part2()
}


private fun part1() {
   File("src/main/resources/day-7-input.txt").useLines { lines ->
      val handsAndBids = parsePart1(lines)
      handsAndBids.sortedBy { it.hand }
         .onEach(::println)
         .mapIndexed { index, (hand, bid) ->
         val rank = index + 1
         bid * rank
      }
         .sum()
         .let(::println)
   }
}

fun parsePart1(lines: Sequence<String>): List<HandAndBid> {
   val regex = Regex("(\\S+)\\s(\\S+)")
   return lines.map {
      regex.find(it)!!.groupValues.let { v ->
         val hand = parseHandPart1(v[1])
         val bid = v[2].toInt()
         HandAndBid(hand, bid)
      }
   }.toList()
}

fun parseHandPart1(handText: String): Hand {
   return handText.asSequence().map(Card::toCardPart1).fold(EmptyHand as Hand) { hand, card -> hand + card }
}

private fun part2() {
   File("src/main/resources/day-7-input.txt").useLines { lines ->
      val handsAndBids = parsePart2(lines)
      handsAndBids.map { handAndBid ->
            tryUpgrade(handAndBid).also { upgraded ->
               if (upgraded.hand != handAndBid.hand) println("$handAndBid     ==>    $upgraded")
            }
         }
         .sortedBy { it.hand }
         .onEach(::println)
         .mapIndexed { index, (hand, bid) ->
            val rank = index + 1
            bid * rank
         }
         .sum()
         .let(::println)
   }
}

fun parsePart2(lines: Sequence<String>): List<HandAndBid> {
   val regex = Regex("(\\S+)\\s(\\S+)")
   return lines.map {
      regex.find(it)!!.groupValues.let { v ->
         val hand = parseHandPart2(v[1])
         val bid = v[2].toInt()
         HandAndBid(hand, bid)
      }
   }.toList()
}


fun parseHandPart2(handText: String): Hand {
   return handText.asSequence().map(Card::toCardPart2).fold(EmptyHand as Hand) { hand, card -> hand + card }
}

fun tryUpgrade(handAndBid: HandAndBid) : HandAndBid = HandAndBid(tryUpgrade(handAndBid.hand), handAndBid.bid)
fun tryUpgrade(hand: Hand) : Hand {
   return when (hand) {
      is FiveOfAKind -> hand
      is FourOfAKind -> {
         when (hand.countJokers()) {
            1 -> FiveOfAKind(hand.cards)
            4 -> FiveOfAKind(hand.cards) //the four of a kind are jokers
            else -> hand
         }
      }
      is FullHouse -> {
         when (hand.countJokers()) {
            2, 3 -> FiveOfAKind(hand.cards)
            else -> hand
         }
      }
      is ThreeOfAKind -> {
         when (hand.countJokers()) {
            1 -> FourOfAKind(hand.cards)
            3 -> FourOfAKind(hand.cards) //the three of a kind are jokers
            else -> hand
         }
      }
      is TwoPair -> {
         when (hand.countJokers()) {
            1 -> FullHouse(hand.cards)
            2 -> FourOfAKind(hand.cards) //one of the pair must be 2 jokers
            else -> hand
         }
      }
      is OnePair -> {
         when (hand.countJokers()) {
            1 -> ThreeOfAKind(hand.cards)
            2 -> ThreeOfAKind(hand.cards) //the pair is from jokers
            else -> hand
         }
      }
      is HighCard -> {
         when (hand.countJokers()) {
            1 -> OnePair(hand.cards) //all cards are different only 1 joker is possible
            else -> hand
         }
      }
      is EmptyHand -> hand
   }
}

enum class Card(val char: Char) {
   JOKER('J'),
   TWO('2'),
   THREE('3'),
   FOUR('4'),
   FIVE('5'),
   SIX('6'),
   SEVEN('7'),
   EIGHT('8'),
   NINE('9'),
   TEN('T'),
   JACK('J'),
   QUEEN('Q'),
   KING('K'),
   ACE('A');

   companion object {
      fun toCardPart1(char: Char): Card {
         return entries.filterNot { it == JOKER }.first { it.char == char }
      }
      fun toCardPart2(char: Char): Card {
         return entries.filterNot { it == JACK }.first { it.char == char }
      }
   }
}


data class HandAndBid(val hand: Hand, val bid: Int) {
   override fun toString(): String {
      return " ${hand.javaClass.simpleName}   $hand  $bid"
   }
}


sealed class Hand(val cards: List<Card>, private val strength: Int) : Comparable<Hand> {

   abstract operator fun plus(card: Card): Hand

   protected fun countMatches(card: Card): Int = cards.count { c -> c == card }

   fun countJokers(): Int = cards.count { c -> c == Card.JOKER }

   override fun compareTo(other: Hand): Int {
     return compareByHandStrength(other).takeIf { it != 0 } ?: compareByCardStrength(other)
   }
   private fun compareByHandStrength(other: Hand) : Int = strength.compareTo(other.strength)
   private fun compareByCardStrength(other: Hand) : Int = cards.indices.firstNotNullOf { i -> cards[i].compareTo(other.cards[i]).takeUnless { cmp -> cmp == 0 } }

   override fun toString(): String {
      return "[${cards.joinToString("") { it.char.toString() }}]"
   }
}

data object EmptyHand : Hand(emptyList(), 0) {
   override fun plus(card: Card): Hand {
      return HighCard(listOf(card))
   }
}


class HighCard(cards: List<Card>) : Hand(cards, 1) {
   override fun plus(card: Card): Hand {
      return when (countMatches(card)) {
         0 -> HighCard(cards + card)
         else -> OnePair(cards + card)
      }
   }
}

class OnePair(cards: List<Card>) : Hand(cards, 2) {
   override fun plus(card: Card): Hand {
      return when (val count = countMatches(card)) {
         0 -> OnePair(cards + card)
         1 -> TwoPair(cards + card)
         2 -> ThreeOfAKind(cards + card)
         else -> throw IllegalStateException("Count of $count is not possible")
      }
   }
}

class TwoPair(cards: List<Card>) : Hand(cards, 3) {
   override fun plus(card: Card): Hand {
      return when (val count = countMatches(card)) {
         0 -> TwoPair(cards + card)
         1 -> FullHouse(cards + card)
         2 -> FullHouse(cards + card)
         else -> throw IllegalStateException("Count of $count is not possible")
      }
   }

}

class ThreeOfAKind(cards: List<Card>) : Hand(cards, 4) {
   override fun plus(card: Card): Hand {
      return when (val count = countMatches(card)) {
         0 -> ThreeOfAKind(cards + card)
         1 -> FullHouse(cards + card)
         3 -> FourOfAKind(cards + card)
         else -> throw IllegalStateException("Count of $count is not possible")
      }
   }
}

class FullHouse(cards: List<Card>) : Hand(cards, 5) {
   override fun plus(card: Card): Hand {
      throw IllegalStateException("Adding a card is not possible")
   }
}

class FourOfAKind(cards: List<Card>) : Hand(cards, 6) {
   override fun plus(card: Card): Hand {
      return when (val count = countMatches(card)) {
         0 -> FourOfAKind(cards + card)
         4 -> FiveOfAKind(cards + card)
         else -> throw IllegalStateException("Count of $count is not possible")
      }
   }
}

class FiveOfAKind(cards: List<Card>) : Hand(cards, 7) {
   override fun plus(card: Card): Hand {
      throw IllegalStateException("Adding a card is not possible")
   }
}