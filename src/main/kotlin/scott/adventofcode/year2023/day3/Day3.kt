package scott.adventofcode.year2023.day3

import java.io.File
import java.lang.Math.max
import java.lang.Math.min

fun main() {
   //part1()
   part2()
}

fun part1() {
   File("src/main/resources/day-3-input.txt").useLines { lines ->
      val grid = Grid(lines.mapIndexed { y, line ->
         CellRow(line.toCharArray().asSequence().mapIndexed { x, character -> Cell(x, y, character) }.toList())
      }.toList())
      grid.numbers.filter(Number::isPartNumber).map(Number::getPartNumber).onEach(::println).sum().let(::println)
   }
}

fun part2() {
   File("src/main/resources/day-3-input.txt").useLines { lines ->
      val grid = Grid(lines.mapIndexed { y, line ->
         CellRow(line.toCharArray().asSequence().mapIndexed { x, character -> Cell(x, y, character) }.toList())
      }.toList())
      var gearRatioSum = 0
      val foundCombinations = mutableSetOf<Combination>()
      for (number in grid.numbers) {
         for (other in grid.numbers) {
            if (number == other) continue
            if (Combination(number, other) in foundCombinations) continue
            if (number.isConnectedByGear(other)) {
               gearRatioSum += (number.getPartNumber() * other.getPartNumber())
               foundCombinations.add(Combination(number, other))
            }
         }
      }
      println("Total gear ratios: $gearRatioSum")
   }
}

class Combination(val numberA: Number, val numberB: Number) {
   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      other as Combination
      if (numberA != other.numberA && numberA != other.numberB) return false
      if (numberB != other.numberB && numberB != other.numberA) return false

      return true
   }

   override fun hashCode(): Int {
      return numberA.hashCode() + numberB.hashCode()
   }
}


class Grid(val rows: List<CellRow>) {
   fun getCells(leftX: Int, rightX: Int, y: Int): List<Cell> {
      val cells = rows.getOrNull(y)?.cells
      return cells?.subList(max(0, leftX), min(rightX + 1, cells.size)) ?: emptyList()
   }

   fun getCell(x: Int, y: Int): Cell {
      return rows.getOrNull(y)?.cells?.getOrNull(x) ?: Cell(x, y, '.')
   }

   val numbers = mutableListOf<Number>()

   init {
      for (row in rows) {
         var currentNumber: Number? = null
         for (cell in row.cells) {
            if (currentNumber == null && cell.isNumber()) {
               currentNumber = Number(this, cell).also { numbers.add(it) }
            } else if (currentNumber != null && cell.isNumber()) {
               currentNumber.addCell(cell)
            } else if (cell.isNumber().not()) {
               currentNumber = null
            }
         }
      }
   }
}

class CellRow(val cells: List<Cell>)
class Cell(val x: Int, val y: Int, val character: Char) {
   fun isNumber(): Boolean {
      return character in '0'..'9'
   }

   fun isSymbol(): Boolean {
      return character.isDigit().not() && character != '.'
   }

   override fun toString(): String {
      return "Cell($x, $y, '$character')"
   }

   fun isGear(): Boolean {
      return character == '*'
   }

}

class Number(private val grid: Grid, cell: Cell) {
   private val cells = mutableListOf<Cell>()

   init {
      cells.add(cell)
   }

   fun addCell(cell: Cell) {
      cells.add(cell)
   }

   fun isConnectedByGear(number: Number): Boolean {
      val myGearCells = getSurroundingGearCells()
      val otherGearCells = number.getSurroundingGearCells()
      if (myGearCells.isEmpty() || otherGearCells.isEmpty()) return false
      val overlap = myGearCells.intersect(otherGearCells)
      require(overlap.size <= 1) { "only expect 1 gear to link 2 numbers" }
      return overlap.size == 1
   }

   fun getPartNumber(): Int {
      return cells.map { cell -> cell.character }.joinToString("").toInt()
   }

   fun isPartNumber(): Boolean {
      return getSurroundingCells { it.isSymbol() }.isNotEmpty()
   }

   private fun getSurroundingGearCells(): Set<Cell> {
      return getSurroundingCells { it.isGear() }
   }

   private fun getSurroundingCells(cellTest: (Cell) -> Boolean): Set<Cell> {
      val y = cells.first().y
      val leftX = cells[0].x
      val rightX = cells.last().x
      val cellsAbove = grid.getCells(leftX - 1, rightX + 1, y - 1)
      val cellsBeside = listOf(grid.getCell(leftX - 1, y), grid.getCell(rightX + 1, y))
      val cellsBelow = grid.getCells(leftX - 1, rightX + 1, y + 1)
      return (cellsAbove + cellsBelow + cellsBeside).filter(cellTest).toSet()
   }

   override fun toString(): String {
      return cells.map { it.character }.joinToString("")
   }
}
