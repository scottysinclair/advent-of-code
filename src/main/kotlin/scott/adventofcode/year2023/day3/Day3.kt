package scott.adventofcode.year2023.day3

import java.io.File
import java.lang.Math.max
import java.lang.Math.min

fun main() {
   part1()
}

fun part1() {
   File("src/main/resources/day-3-input.txt").useLines { lines ->
      val grid = Grid(lines.mapIndexed { y, line ->
         CellRow(line.toCharArray().asSequence().mapIndexed { x, character ->  Cell(x, y, character) }.toList())
      }.toList())
      //println(grid.rows.size)
      //grid.numbers.forEach(::println)

      grid.numbers.filter(Number::isPartNumber).map(Number::getPartNumber).onEach(::println).sum().let(::println)
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

   val numbers =  mutableListOf<Number>()
   init {
      for (row in rows) {
         var currentNumber :Number? = null
         for (cell in row.cells) {
            if (currentNumber == null && cell.isNumber() ) {
               currentNumber = Number(this, cell).also { numbers.add(it) }
            }
            else if (currentNumber != null && cell.isNumber()) {
               currentNumber.addCell(cell)
            }
            else if (cell.isNumber().not()) {
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


}

class Number(private val grid: Grid, cell: Cell) {
   private val cells = mutableListOf<Cell>()
   init {
      cells.add(cell)
   }

   fun addCell(cell: Cell) {
      cells.add(cell)
   }

   fun getPartNumber() : Int {
      return cells.map { cell -> cell.character }.joinToString("").toInt()
   }

   fun isPartNumber() : Boolean {
      val y = cells.first().y
      val leftX = cells[0].x
      val rightX = cells.last().x
      val cellsAbove = grid.getCells(leftX - 1, rightX + 1, y - 1)
      val cellsBeside = listOf(grid.getCell(leftX - 1, y), grid.getCell(rightX + 1, y))
      val cellsBelow = grid.getCells(leftX - 1, rightX + 1, y + 1)
      return (cellsAbove + cellsBelow + cellsBeside).any(Cell::isSymbol)
   }

   override fun toString(): String {
      return cells.map { it.character }.joinToString("")
   }
}
