package me.senseiwells.scripting.script.annotation

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class Mappings(val type: String)
