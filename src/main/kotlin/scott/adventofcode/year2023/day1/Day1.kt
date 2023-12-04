package scott.adventofcode.year2023.day1

import java.io.File
import java.util.regex.Pattern

fun main() {
   File("src/main/resources/day-1-input.txt").useLines { lines ->
     lines.map { line ->
        val processedLine = preprocess(line.lowercase())
        val number = calculateNumber(processedLine)
        println("$line  =>  $processedLine  => $number")
        number
     }.sum()
   }.let(::println)
}

fun calculateNumber(line: String) : Long {
   val first = line.first { it.isDigit() }
   val last = line.last { it.isDigit() }
   return "$first$last".toLong()
}

class Result(val index: Int, val numberText: String, val digit: Char)
fun preprocess(line: String) : String {
   /*
    * for each index in the line (from left to right)
    */
   val result = (0..line.length).firstNotNullOfOrNull { index ->
      /*
       * check if we can  replace at that point a number word with a digit
       */
      replaceWithDigit(line, index)?.let {
         /*
          * if so return the details
          */
         Result(index, it.first, it.second)
      }
   }
   /*
    * If no result then the line is not modified
    */
   return if (result == null) line
   else {
      /*
       * otherwise swap out the number for the digit and recursively process
       */
      val before = line.substring(0, result.index)
      val after = line.substring(result.index + result.numberText.length, line.length)
      preprocess("$before${result.digit}$after")
   }
}


val numbers = listOf(
   "one" to '1',
   "two" to '2',
   "three" to '3',
   "four" to '4',
   "five" to '5',
   "six" to '6',
   "seven" to '7',
   "eight" to '8',
   "nine" to '9'
)

/**
 * Returns the digit that can be replaced at that character
 */
fun replaceWithDigit(line: String, index: Int) : Pair<String,Char>? {
   return numbers.firstOrNull {(text, digit) ->
       line.regionMatches(index, text, 0, text.length)
   }
}



