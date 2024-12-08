package me.senseiwells.scripting.script.annotation

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class Environment(val version: String, val env: String)
