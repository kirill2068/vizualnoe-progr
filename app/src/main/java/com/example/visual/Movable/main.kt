package com.example.visual.Movable

fun main() {
    val humans = listOf(
        Human("Фадеев Е.А", 19, 9),
        Human("Андропов Ф.С", 9, 3),
        Human("Топский П.Е", 21, 11),
        Human("Серегеев С.В", 54, 6)
    )
    val driver = Driver("Кириленко А.Г", 25, 30)

    val all = humans + driver

    all.forEach { it.move() }

    Thread.sleep(6000)
}