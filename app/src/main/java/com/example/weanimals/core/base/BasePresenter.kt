package com.example.weanimals.core.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

/**
 * Base for presenters that can be detached from an Activity without retaining it.
 * All presenter callbacks run on the main dispatcher and are guarded by [withView].
 */
abstract class BasePresenter<V : Any> {

    private var view: V? = null
    private val presenterJob: Job = SupervisorJob()

    protected val presenterScope = CoroutineScope(
        presenterJob + Dispatchers.Main.immediate
    )

    open fun attachView(view: V) {
        this.view = view
    }

    open fun detachView() {
        view = null
    }

    fun destroy() {
        detachView()
        presenterJob.cancel()
    }

    protected fun withView(action: (V) -> Unit) {
        view?.let(action)
    }
}
