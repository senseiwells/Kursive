package me.senseiwells.essential_scripting.utils

import kotlin.script.experimental.api.ScriptDiagnostic

fun String.asWarningDiagnostics(): ScriptDiagnostic {
    return ScriptDiagnostic(ScriptDiagnostic.unspecifiedInfo, this, ScriptDiagnostic.Severity.WARNING)
}
