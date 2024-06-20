package com.rosan.xposed.util

fun <E> MutableCollection<E>.addAll(vararg elements: E): Boolean = addAll(elements.asList())

fun <E> Collection<E>.containsAll(vararg elements: E): Boolean = containsAll(elements.asList())
