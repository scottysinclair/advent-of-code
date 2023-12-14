package scott.adventofcode.year2023.day5

import java.io.File
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicLong

fun main() {
   //part1()
   part2()
}

fun part1() {
   File("src/main/resources/day-5-input.txt").useLines { linesSeq ->
      val lines = linesSeq.toList()
      val resourceFactory = ResourceFactory()
      val initialSeeds = parseInitialSeedsForPart1(lines)
      val resourceMappings = parseResourceMappings(lines, resourceFactory)
      resourceMappings.forEach { println(it) }

      val seed = resourceFactory.get("seed")
      val location = resourceFactory.get("location")
      val resourceChain = buildResourceChain(seed, location, resourceMappings)

      initialSeeds.minOf { seed ->
         println("Processing seed $seed -----------------")
         resourceChain.fold(seed) { number, nextResourceMapping ->
            nextResourceMapping.map(number).also {
               println("Mapping $number from ${nextResourceMapping.source.name} to ${nextResourceMapping.destination.name} => $it")
            }
         }
      }.let(::println)
   }
}

fun part2() {
   File("src/main/resources/day-5-input.txt").useLines { linesSeq ->
      val lines = linesSeq.toList()
      val resourceFactory = ResourceFactory()
      val initialSeedRanges = parseInitialSeedsForPart2(lines)
      initialSeedRanges.forEach { println(it) }
      val resourceMappings = parseResourceMappings(lines, resourceFactory)
      resourceMappings.forEach { println(it) }

      val seed = resourceFactory.get("seed")
      val location = resourceFactory.get("location")
      val resourceChain = buildResourceChain(seed, location, resourceMappings)

      val totalSeeds = initialSeedRanges.sumOf { it.length }
      val totalLocations = resourceMappings.first { it.destination == location }.length()

      println("total seeds: $totalSeeds")
      println("total locations: $totalLocations")

      val count = AtomicLong(0)
      initialSeedRanges.parallelStream().map { seedRange ->
         seedRange.minOf { seed ->
            printProgress(count, totalSeeds)
            resourceChain.fold(seed) { number, nextResourceMapping -> nextResourceMapping.map(number)  }
         }
      }.toList().min().let(::println)
   }
}

fun printProgress(count: AtomicLong, totalSeeds: Long) {
   val myCount = count.incrementAndGet()
   if (myCount % (1000000 * 30) == 0L) {
      val percentComplete = ((myCount / totalSeeds.toDouble()) * 100).toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
      println("Count $myCount from $totalSeeds  => $percentComplete%")
   }
}




fun buildResourceChain(from: Resource, to: Resource, resourceMappings: List<ResourceMappings>) : List<ResourceMappings> {
   val chain = mutableListOf<ResourceMappings>()
   var currentResource = from
   while(currentResource != to) {
      val chainLink = resourceMappings.first { it.source == currentResource }
      chain.add(chainLink)
      currentResource = chainLink.destination
   }
   return chain
}

fun parseInitialSeedsForPart1(lines: List<String>) : List<Long>  = lines[0].substring("seeds: ".length).split(" ").map { it.trim().toLong() }
fun parseInitialSeedsForPart2(lines: List<String>) : List<SeedRange> {
   val numbers = parseInitialSeedsForPart1(lines)
   return numbers.mapIndexedNotNull { index, number ->
      if (index % 2 == 0) SeedRange(number, numbers[index + 1])
      else null
   }
}

data class SeedRange(val startSeed: Long, val length: Long) : Iterable<Long> {
   override fun iterator(): Iterator<Long> {
      return LongRange(startSeed, startSeed + length - 1).iterator()
   }
}

class ResourceFactory {
   private val resources = mutableMapOf<String, Resource>()
   fun get(name: String) : Resource {
      return resources[name]!!
   }
   fun getOrCreate(name: String) : Resource {
      return resources.getOrPut(name) { Resource(name) }
   }
}

val mapping_line_regex = Regex("(\\d+) (\\d+) (\\d+)")
fun parseResourceMappings(lines: List<String>, resourceFactory: ResourceFactory) : List<ResourceMappings> {
   var currentResourceMappings : ResourceMappings? = null
   val resourceMappings = mutableListOf<ResourceMappings>()
   for (line in lines) {
      try {
         if (line.contains("map:")) {
            val (source, destination) = parseResourcesInLine(line, resourceFactory)
            currentResourceMappings = ResourceMappings(source, destination).also { resourceMappings.add(it) }
         } else if (currentResourceMappings != null) {
            val result = mapping_line_regex.find(line)
            if (result != null) {
               val (destinationStart, sourceStart, length) = result.groupValues.toMutableList().drop(1).map(String::toLong)
               currentResourceMappings.addMappedRange(sourceStart, destinationStart, length)
            }
         } else if (line.trim().isEmpty()) {
            currentResourceMappings = null
         }
      }
      catch(x: Exception) {
         throw IllegalStateException("Could not parse line '$line'", x)
      }
   }
   return resourceMappings
}

val from_to_regex = Regex("(.+)-to-(.+) map:")
fun parseResourcesInLine(line: String, fac: ResourceFactory): Pair<Resource, Resource> {
   return from_to_regex.find(line)!!.let {
      fac.getOrCreate(it.groupValues[1]) to fac.getOrCreate(it.groupValues[2])
   }
}


data class Resource(val name: String)

class ResourceMappings(val source: Resource, val destination: Resource) {
   private val ranges = mutableListOf<MappedRange>()
   fun addMappedRange(sourceStart: Long, destinationStart: Long, length: Long) {
      val sourceRange = Range(sourceStart, length)
      val destinationRange = Range(destinationStart, length)
      ranges.add(MappedRange(sourceRange, destinationRange))
   }
   fun map(input: Long) : Long {
      return ranges.firstNotNullOfOrNull { r -> r.map(input) } ?: input
   }
   fun length() : Long {
      return ranges.sumOf { it.length() }
   }

   override fun toString(): String {
      return "ResourceMappings(source=$source, destination=$destination, #ranges=${ranges.size})"
   }

}


class MappedRange(private val sourceRange: Range, private val destination: Range) {
   fun map(input: Long) : Long? {
      return sourceRange.indexOf(input)?.let { destination[it] }
   }
   fun length() : Long = sourceRange.length
}

class Range(val from: Long, val length: Long) {
   fun indexOf(input: Long) : Long? {
      if (input < from) return null
      val endExclusive = from + length
      if (input >= endExclusive) return null
      return input - from
   }

   operator fun get(index: Long) : Long? {
      if (index < 0) return null
      if (index >= length) return null
      return from + index
   }
}