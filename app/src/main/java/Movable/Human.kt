package Movable

import kotlinx.coroutines.*

open class Human(
    val FIO: String,
    var age: Int,
    override var speed: Double) : Movable {

    override var x: Double = 0.0
    override var y: Double = 0.0

    override fun move() {
        val dx = (-1..1).random()
        val dy = (-1..1).random()
        x += dx * speed
        y += dy * speed
    }

    open suspend fun run() {
        repeat(10) {
            move()
            println("$FIO - ($x, $y)")
            delay(500)
        }
    }
}
