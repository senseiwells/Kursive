package me.senseiwells.scripting.annotation

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class Mappings(val type: String)
