package ru.raspberry.launcher.models.repo

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.OS

@Serializable
class Rule(
    val action: RuleAction,
    val os: OSRestriction? = null,
    val features: Map<String, Boolean>? = null,
) {
    fun isApplicable(
        os: OS,
        features: Map<String, Boolean> = emptyMap()
    ): Boolean {
        if (this.os != null && this.os.name != null && this.os.name != os) return when(action) {
            RuleAction.Allow -> false
            RuleAction.Disallow -> true
        }
        if (this.features != null && !this.features.all { features[it.key] == it.value }) return  when(action) {
            RuleAction.Allow -> false
            RuleAction.Disallow -> true
        }
        return when(action) {
            RuleAction.Allow -> true
            RuleAction.Disallow -> false
        }
    }
}
