package Movable

import kotlinx.coroutines.*

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