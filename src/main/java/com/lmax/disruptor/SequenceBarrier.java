/*
 * Copyright 2011 LMAX Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lmax.disruptor;


/**
 * 序号屏障（协调屏障）：
 * 通过跟踪生产者的cursor 和 当前事件处理器依赖的的sequence(dependentSequence/traceSequences)，来协调对共享数据结构的访问。
 *
 * 依赖的的sequence： 当前EventProcessor所属的消费者依赖的所有消费者的进度。
 *
 * Coordination barrier for tracking the cursor for publishers and sequence of
 * dependent {@link EventProcessor}s for processing a data structure
 */
public interface SequenceBarrier
{
    /**
	 * 在该屏障上等待，直到该序号的数据可以被消费。
	 * 是否可消费取决于生产者的cursor 和 当前事件处理器依赖的的sequence。
     * Wait for the given sequence to be available for consumption.
     *
     * @param sequence to wait for 事件处理器期望消费的下一个序号
     * @return the sequence up to which is available 可消费的最大序号
     * @throws AlertException       if a status change has occurred for the Disruptor
     * @throws InterruptedException if the thread needs awaking on a condition variable.
     * @throws TimeoutException     if a timeout occurs while waiting for the supplied sequence.
     */
    long waitFor(long sequence) throws AlertException, InterruptedException, TimeoutException;

    /**
	 * 获取生产者的光标(当前发布进度/序号)
     * Get the current cursor value that can be read.
     *
     * @return value of the cursor for entries that have been published.
     */
    long getCursor();

	// ------------------------------------这几个方法可类比为线程的中断状态操作---------------------------------
	// 因为EventProcessor并不是直接是线程对象，因此采用了这么一个方式？

	/**
	 * 查询状态标记是否被设置。
	 * The current alert status for the barrier.
	 *
	 * @return true if in alert otherwise false.
	 */
	boolean isAlerted();

	/**
	 * 通知事件处理器有状态发生了改变(有点像中断 {@link Thread#interrupt()})
     * Alert the {@link EventProcessor}s of a status change and stay in this status until cleared.
     */
    void alert();

    /**
	 * 清除上一个状态标记
     * Clear the current alert status.
     */
    void clearAlert();

    /**
	 * 检查标记，如果为true则抛出异常
     * Check if an alert has been raised and throw an {@link AlertException} if it has.
     *
     * @throws AlertException if alert has been raised.
     */
    void checkAlert() throws AlertException;
}
