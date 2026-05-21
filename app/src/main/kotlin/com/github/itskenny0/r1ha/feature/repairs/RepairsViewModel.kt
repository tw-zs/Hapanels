package com.github.itskenny0.r1ha.feature.repairs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.itskenny0.r1ha.core.ha.HaRepository
import com.github.itskenny0.r1ha.core.ha.RepairIssue
import com.github.itskenny0.r1ha.core.util.R1Log
import com.github.itskenny0.r1ha.core.util.Toaster
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Drives the Repairs surface. Calls `repairs/list_issues` on entry + every refresh, sorts
 * the result with severity-first / created-newest-second / ignored-last, and exposes an
 * ignore action that flips the server-side ignore bit + re-fetches.
 */
class RepairsViewModel(
    private val haRepository: HaRepository,
) : ViewModel() {

    @androidx.compose.runtime.Stable
    data class UiState(
        val loading: Boolean = true,
        val issues: List<RepairIssue> = emptyList(),
        val error: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            haRepository.listRepairs().fold(
                onSuccess = { issues ->
                    val sorted = issues.sortedWith(
                        compareBy<RepairIssue> { if (it.ignored) 1 else 0 }
                            .thenBy { severityRank(it.severity) }
                            .thenByDescending { it.createdAt ?: "" },
                    )
                    _ui.value = _ui.value.copy(loading = false, issues = sorted, error = null)
                    R1Log.i("Repairs", "fetched ${sorted.size} issue(s)")
                },
                onFailure = { t ->
                    R1Log.w("Repairs", "fetch failed: ${t.message}")
                    _ui.value = _ui.value.copy(loading = false, error = t.message)
                },
            )
        }
    }

    fun ignore(issue: RepairIssue) {
        viewModelScope.launch {
            haRepository.ignoreRepair(issue.domain, issue.issueId, ignore = !issue.ignored).fold(
                onSuccess = {
                    Toaster.show(
                        if (issue.ignored) "Restored ${issue.issueId}"
                        else "Ignored ${issue.issueId}",
                    )
                    refresh()
                },
                onFailure = { t ->
                    Toaster.errorExpandable(
                        shortText = "Ignore failed",
                        fullText = t.message ?: t.toString(),
                    )
                },
            )
        }
    }

    /** Sort key for severity — lower is shown first. critical → error → warning → unknown. */
    private fun severityRank(s: String): Int = when (s.lowercase()) {
        "critical" -> 0
        "error" -> 1
        "warning" -> 2
        else -> 3
    }

    companion object {
        fun factory(haRepository: HaRepository) = viewModelFactory {
            initializer { RepairsViewModel(haRepository) }
        }
    }
}
