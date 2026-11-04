package com.example.voicecompanion.data.vehicle

import com.example.voicecompanion.domain.model.VehicleAvailability
import com.example.voicecompanion.domain.model.VehicleDataDefinition

/** Keeps async capability callbacks alive until they finish so Vehicle.release() cannot race them. */
internal class VehicleCapabilityRequests(
    private val begin: () -> Unit,
    private val finish: () -> Unit,
    private val isStopped: () -> Boolean,
    private val markUnavailable: (VehicleDataDefinition, VehicleAvailability, String) -> Unit,
    private val onError: (VehicleDataDefinition, Exception) -> Unit,
) {
    fun <T> request(
        fallback: VehicleDataDefinition,
        call: (onComplete: (T) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
        onComplete: (T) -> Unit,
    ) = requestInternal(fallback, call, onComplete, null)

    fun <T> request(
        fallback: VehicleDataDefinition,
        call: (onComplete: (T) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
        onComplete: (T) -> Unit,
        onFailure: (Exception) -> Unit,
    ) = requestInternal(fallback, call, onComplete, onFailure)

    private fun <T> requestInternal(
        fallback: VehicleDataDefinition,
        call: (onComplete: (T) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
        onComplete: (T) -> Unit,
        onFailure: ((Exception) -> Unit)?,
    ) {
        begin()
        val completed = java.util.concurrent.atomic.AtomicBoolean(false)
        fun completeOnce(block: () -> Unit) {
            if (!completed.compareAndSet(false, true)) return
            try {
                block()
            } catch (e: Exception) {
                if (!isStopped()) onError(fallback, e)
            } finally { finish() }
        }
        try {
            call(
                { result ->
                    completeOnce {
                        if (!isStopped()) onComplete(result)
                    }
                },
                { error ->
                    completeOnce {
                        if (!isStopped()) {
                            onError(fallback, error)
                            onFailure?.invoke(error)
                        }
                    }
                },
            )
        } catch (e: Exception) {
            completeOnce {
                if (!isStopped()) {
                    onError(fallback, e)
                    onFailure?.invoke(e)
                }
            }
        }
    }

    fun unsupported(definition: VehicleDataDefinition, detail: String) =
        markUnavailable(definition, VehicleAvailability.UNSUPPORTED, detail)
}
