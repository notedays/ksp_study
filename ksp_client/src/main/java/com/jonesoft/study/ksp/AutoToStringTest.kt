package com.jonesoft.study.ksp

import com.jonesoft.study.annotations.AutoToString

fun main() {
	val person = PersonImpl(
		name = "홍길동",
		age = 23,
		actions = MoveAction.values().toList()
	)
	println(person)
}

@AutoToString
open class Person(
	var name: String = "",
	var age: Int = 0,
	var actions: List<Action> = emptyList(),
)

interface Action {
	fun doAction()
}

enum class MoveAction(
	private val actionText: String,
) : Action {
	WALK("걷는다"),
	RUN("뛰다"),
	SLIDE("미끄러지다")
	;

	override fun doAction() = println(actionText)
}