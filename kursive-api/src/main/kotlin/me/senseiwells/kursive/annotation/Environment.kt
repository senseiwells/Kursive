package me.senseiwells.kursive.annotation

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class Environment(val version: String, val env: String)
