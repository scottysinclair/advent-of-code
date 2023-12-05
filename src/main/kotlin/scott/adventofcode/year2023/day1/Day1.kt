package scott.adventofcode.year2023.day1

import java.io.File
import java.lang.Integer.min
import java.util.regex.Pattern

fun main() {
   File("src/main/resources/day-1-input.txt").useLines { lines ->
     lines.map { line ->
        runCatching {
           val number = calculateNumber(line)
           println("$line  => $number")
           number
        }.onFailure { println("Error processing line $line") }
           .getOrThrow()
     }.sum()
   }.let(::println)
}

fun calculateNumber(line: String) : Int {
   val first = getFirstDigit(line, numberText)
   val last = getLastDigit(line, numberText)
   return "$first$last".toInt()
}

fun getFirstDigit(line: String, numberText: List<String>): Int {
   /*
    * find the first digit from the left
    */
   return line.indices.firstNotNullOf { i ->
      /*
       * either it really is a digit
       */
      if (line[i] in '0'..'9') {
         line[i].toString().toInt()
      }
      else {
         /*
          * or it's the name of a digit
          */
         numberText.indices.firstNotNullOfOrNull { nti ->
            val linePart = line.substring(i, min(i + numberText[nti].length, line.length))
            if (linePart == numberText[nti]) nti+1
            else null
         }
      }
   }
}
fun getLastDigit(line: String, numberText: List<String>): Int {
   return getFirstDigit(line.reversed(), numberText.map { it.reversed() })
}


private val numberText = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine")


