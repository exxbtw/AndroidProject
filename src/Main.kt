import kotlinx.coroutines.*

open class Human(val FIO: String, var age: Int, var speed: Double) {
    var x: Double = 0.0
    var y: Double = 0.0

    open fun move() {
        var dx = (-1..1).random()
        var dy = (-1..1).random()
        x = x + dx * speed
        y = y + dy * speed
    }

    open suspend fun run() {
        repeat(10) {
            move()
            println("$FIO - ($x, $y)")
            delay(500)
        }
    }
}

class Driver(FIO: String, age: Int, speed: Double, val car: String) : Human(FIO, age, speed) {
    val dx = (-1..1).random() //1 раз выбираем направление для прямолинейности
    val dy = (-1..1).random()

    override fun move() {
        x = x + dx * speed
        y = y + dy * speed
    }
    override suspend fun run() {
        repeat(10) {
            move()
            println("$car - ($x, $y)")
            delay(500)
        }
    }
}


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

//    val simulationTime = 10
//
//    for (t in 1..simulationTime) {
//        println("$t-ая секунда")
//        for (h in humans) {
//            h.move()
//            println("${h.FIO} - (${h.x}, ${h.y})")
//        }
//        for (d in drivers) {
//            d.move()
//            println("${d.car} - (${d.x}, ${d.y})")
//        }
//    }


}
