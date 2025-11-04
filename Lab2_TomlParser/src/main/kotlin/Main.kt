package org.example

import kotlinx.serialization.builtins.ListSerializer
import org.example.Entities.Person
import org.example.Entities.Address
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch

fun generatePeople(count: Int): List<Person> {
    val random = Random(42)
    val names = listOf("Dima", "Egor", "Ilya", "Alex", "Sonya")
    val cities = listOf("London", "Paris", "Berlin", "Madrid", "Rome")
    val countries = listOf("UK", "France", "Germany", "Spain", "Italy")

    return (1..count).map { i ->
        Person(
            id = i,
            name = "${names.random(random)}_$i",
            age = 18 + random.nextInt(60),
            isActive = random.nextBoolean(),
            score = random.nextDouble() * 100,
            tags = (1..3).map { "tag_${random.nextInt(100)}" },
            address = Address(
                street = "Street $i",
                city = cities.random(random),
                country = countries.random(random)
            )
        )
    }
}

fun benchmarkKotlinx() {
    val sizes = listOf(10_000, 100_000, 500_000)

    for (size in sizes) {
        println("=== kotlinx.serialization: $size records ===")

        val data = generatePeople(size)

        val jsonStr: String

        val serializationTime = measureTimeMillis {
            jsonStr = Json.encodeToString(ListSerializer(Person.serializer()), data)
        }

        val deserializationTime = measureTimeMillis {
            val restored = Json.decodeFromString(ListSerializer(Person.serializer()), jsonStr)
        }

        println("Serialization: ${serializationTime} ms")
        println("Deserialization: ${deserializationTime} ms")
        println()
    }
}

suspend fun benchmarkKotlinxParallelAsCoroutine() {
    val sizes = listOf(10_000, 100_000, 500_000)
    val json = Json { encodeDefaults = true }
    val parallelism = Runtime.getRuntime().availableProcessors()

    for (size in sizes) {
        println("=== kotlinx.serialization (JSON Lines) parallel: $size records ===")

        val data = generatePeople(size)
        val jsonLines: String

        val serializationTime = measureTimeMillis {
            coroutineScope {
                jsonLines = data
                    .chunked((data.size + parallelism - 1) / parallelism)
                    .map { chunk ->
                        async(Dispatchers.Default) {
                            chunk.joinToString("\n") { person ->
                                json.encodeToString(Person.serializer(), person)
                            }
                        }
                    }
                    .awaitAll()
                    .joinToString("\n")
            }
        }

        val deserializationTime = measureTimeMillis {
            val lines = jsonLines.split("\n").filter { it.isNotBlank() }
            val chunkSize = 10_000

            coroutineScope {
                lines
                    .chunked((lines.size + parallelism - 1) / parallelism)
                    .map { chunk ->
                        async(Dispatchers.Default) {
                            chunk.map { line ->
                                json.decodeFromString(Person.serializer(), line)
                            }
                        }
                    }
                    .awaitAll()
                    .flatten()
            }
        }

        println("Serialization: ${serializationTime} ms")
        println("Deserialization: ${deserializationTime} ms")
        println()
    }
}

fun benchmarkCustom() {
    val sizes = listOf(10_000, 100_000, 500_000)

    for(size in sizes) {
        println("=== Custom TomlSerializer: $size records ===")

        val data = generatePeople(size)

        val serialized: String

        val serializationTime = measureTimeMillis {
            serialized = data.map { TomlSerializer.serialize(it) }.joinToString("\n\n\n")
        }

        val deserializationTime = measureTimeMillis {
            val deserialized = serialized.split("\n\n\n")
                .filter { it.isNotBlank() }
                .map { TomlSerializer.deserialize<Person>(it) }
            println("Deserialized count: ${deserialized.size}")
        }

        println("Serialization: ${serializationTime} ms")
        println("Deserialization: ${deserializationTime} ms")
        println()
    }
}

suspend fun benchmarkCustomParallelAsCoroutine() {
    val sizes = listOf(10_000, 100_000, 500_000)

    for(size in sizes) {
        println("=== Custom TomlSerializer as parallel with coroutine: $size records ===")

        val data = generatePeople(size)

        val serialized: String
        val parallelism = Runtime.getRuntime().availableProcessors()

        val serializationTime = measureTimeMillis {
            coroutineScope {
                serialized = data
                    .chunked((data.size + parallelism - 1) / parallelism)
                    .map { chunk ->
                        async(Dispatchers.Default) {
                            chunk.joinToString("\n\n\n") { TomlSerializer.serialize(it) }
                        }
                    }
                    .awaitAll()
                    .joinToString("\n\n\n")
            }
        }

        val deserializationTime = measureTimeMillis {
            val blocks = serialized.split("\n\n\n").filter { it.isNotBlank() }

            coroutineScope {
                val deserialized = blocks
                    .chunked((blocks.size + parallelism - 1) / parallelism)
                    .map { chunk ->
                        async(Dispatchers.Default) {
                            chunk.map { TomlSerializer.deserialize<Person>(it) }
                        }
                    }
                    .awaitAll()
                    .flatten()
            }
        }

        println("Serialization: ${serializationTime} ms")
        println("Deserialization: ${deserializationTime} ms")
        println()
    }
}

fun benchmarkCustomParallelAsThread() {
    val sizes = listOf(10_000, 100_000, 500_000)

    val parallelism = Runtime.getRuntime().availableProcessors()

    for(size in sizes) {
        println("=== Custom TomlSerializer as parallel with threads: $size records ===")

        val data = generatePeople(size)

        var serialized: String

        val chunkSizeSerialize = (data.size + parallelism - 1) / parallelism
        val chunksSerialize = data.chunked(chunkSizeSerialize)

        val resultsSerialize = Array<String?>(chunksSerialize.size) { null }
        val latchSerialize = CountDownLatch(chunksSerialize.size)

        val serializationTime = measureTimeMillis {
            for (i in chunksSerialize.indices) {
                val chunk = chunksSerialize[i]

                Thread {
                    resultsSerialize[i] = chunk.joinToString("\n\n\n") { TomlSerializer.serialize(it) }
                    latchSerialize.countDown()
                }.start()
            }

            latchSerialize.await()
        }

        serialized = resultsSerialize.joinToString("\n\n\n") { it ?: "" }



        val blocks = serialized.split("\n\n\n").filter { it.isNotBlank() }

        val chunkSizeDeserialize = (blocks.size + parallelism - 1) / parallelism
        val chunksDeserialize = blocks.chunked(chunkSizeDeserialize)

        val resultsDeserialize = Array<List<Person>?>(chunksDeserialize.size) { null }
        val latchDeserialize = CountDownLatch(chunksDeserialize.size)

        val deserializationTime = measureTimeMillis {
            for (i in chunksDeserialize.indices) {
                val chunk = chunksDeserialize[i]

                Thread {
                    resultsDeserialize[i] = chunk.map { TomlSerializer.deserialize<Person>(it) }
                    latchDeserialize.countDown()
                }.start()
            }

            latchDeserialize.await()
        }

        println("Serialization: ${serializationTime} ms")
        println("Deserialization: ${deserializationTime} ms")
        println()
    }
}

suspend fun main() {
    run {
        val warmup = generatePeople(10_000)
        TomlSerializer.serialize(warmup[0])
        warmup.map { TomlSerializer.serialize(it) }.joinToString("\n\n")
    }

    benchmarkCustomParallelAsThread()

//    benchmarkCustom()
//    benchmarkKotlinx()
//
//    benchmarkCustomParallelAsCoroutine()
//    benchmarkKotlinxParallelAsCoroutine()



    /*val data1 = Data("important string")
    val data2 = Data("other important string")

    val user1 = User("Ivan", 20, false, 1.333, listOf("Tom", "Sonya"), null, data1)
    val user2 = User("Dima", 22, true, 2.444, listOf("Ivan", "Alex"), user1, data2)
    val user3 = User("Alex", 24, true, 3.555, listOf("Egor", "Rus"), null, data1)

    val toml = TomlSerializer.serialize(user2)

    println(toml)

    val restored = TomlSerializer.deserialize<User>(toml)
    println("\nRestored: $restored")

    println("Equal? ${user2 == restored}")*/
}