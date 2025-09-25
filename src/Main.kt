import kotlinx.coroutines.*

fun main() = runBlocking {
    val humans = List(4) { i ->
        Human("Human${i + 1}", 18 + i, 2.0 + i * 0.2)
    }

    val drivers = List(1) { i ->
        Driver("Driver${i + 1}", 19 + i, 19.0 + i * 0.2, "Hyundai Solaris ${i + 1}")
    }

    val jobs = (humans + drivers).map { obj ->
        launch { obj.run() }
    }
    jobs.forEach { it.join() }

}