package me.senseiwells.kursive.annotation

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class Script(val id: String, val version: String)
