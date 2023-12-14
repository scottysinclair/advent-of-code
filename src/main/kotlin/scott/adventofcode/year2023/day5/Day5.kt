package scott.adventofcode.year2023.day5

import java.io.File

fun main() {
   part1()
   //part2()
}

fun part1() {
   File("src/main/resources/day-5-input.txt").useLines { linesSeq ->
      val lines = linesSeq.toList()
      val resourceFactory = ResourceFactory()
      val initialSeeds = parseInitialSeeds(lines)
      val resourceMappings = parseResourceMappings(lines, resourceFactory)
      resourceMappings.forEach { println(it) }

      val seed = resourceFactory.get("seed")
      val location = resourceFactory.get("location")
      val resourceChain = buildResourceChain(seed, location, resourceMappings)

      initialSeeds.map { seed ->
         println("Processing seed $seed -----------------")
         resourceChain.fold(seed) { number, nextMapping ->
            nextMapping.map(number).also {
               println("Mapping $number from ${nextMapping.source.name} to ${nextMapping.destination.name} => $it")
            }
         }
      }.min().let(::println)

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

fun parseInitialSeeds(lines: List<String>) : List<Long>  = lines[0].substring("seeds: ".length).split(" ").map { it.trim().toLong() }

class ResourceFactory {
   private val resources = mutableMapOf<String, Resource>()
   fun get(name: String) : Resource {
      return resources.get(name)!!
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

   override fun toString(): String {
      return "ResourceMappings(source=$source, destination=$destination, #ranges=${ranges.size})"
   }

}


class MappedRange(private val sourceRange: Range, private val destination: Range) {
   fun map(input: Long) : Long? {
      return sourceRange.indexOf(input)?.let { destination[it] }
   }
}

class Range(val from: Long, val length: Long) {
   fun indexOf(input: Long) : Long? {
      if (input < from) return null
      val endExlusive = from + length
      if (input >= endExlusive) return null
      return input - from
   }

   operator fun get(index: Long) : Long? {
      if (index < 0) return null
      if (index >= length) return null
      return from + index
   }
}