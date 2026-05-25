package com.noted

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

fun <T> LiveData<T>.getOrAwaitValue(timeoutSeconds: Long = 2): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(value: T) {
            data = value
            latch.countDown()
            removeObserver(this)
        }
    }
    observeForever(observer)
    if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
        removeObserver(observer)
        throw TimeoutException("LiveData value was never set within $timeoutSeconds seconds")
    }
    @Suppress("UNCHECKED_CAST")
    return data as T
}
