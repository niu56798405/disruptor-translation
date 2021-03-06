/*
 * Copyright 2012 LMAX Ltd.
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
 * 序号生成器，为生产者们提供序号。
 *
 * 同时追踪消费序号的消费链末端们的消费者进度{@link Sequence},以协调序号生成器和消费者们之间的速度。
 *
 * Coordinates claiming sequences for access to a data structure while tracking dependent {@link Sequence}s
 */
public interface Sequencer extends Cursored, Sequenced
{
    /**
	 * 将-1作为默认序号
     * Set to -1 as sequence starting point
     */
    long INITIAL_CURSOR_VALUE = -1L;

    /**
	 * 将生产者的序号(光标)移动到指定位置.（仅在初始化RingBuffer时使用）
	 *
     * Claim a specific sequence.  Only used if initialising the ring buffer to
     * a specific value.
     *
     * @param sequence The sequence to initialise too.
     */
    void claim(long sequence);

    /**
	 * 指定序号的数据是否可用(是否已发布)
     * Confirms if a sequence is published and the event is available for use; non-blocking.
     *
     * @param sequence of the buffer to check
     * @return true if the sequence is available for use, false if not
     */
    boolean isAvailable(long sequence);

    /**
	 * 添加序号生成器需要追踪的网关Sequence（新增的末端消费者消费序列/进度），
	 * Sequencer会持续跟踪它们的进度信息，以协调生产者和消费者之间的速度。
	 * 即生产者想使用一个序号时必须等待所有的网关Sequence处理完该序号。
	 *
	 * Add the specified gating sequences to this instance of the Disruptor.  They will
     * safely and atomically added to the list of gating sequences.
     *
     * @param gatingSequences The sequences to add.
     */
    void addGatingSequences(Sequence... gatingSequences);

    /**
	 * 移除这些网关Sequence(消费者消费序列/进度)，不再跟踪它们的进度信息
     * Remove the specified sequence from this sequencer.
     *
     * @param sequence to be removed.
     * @return <code>true</code> if this sequence was found, <code>false</code> otherwise.
     */
    boolean removeGatingSequence(Sequence sequence);

    /**
	 * 为事件处理器创建一个序号屏障，追踪这些Sequence的信息，用于从RingBuffer中获取可用的数据。
	 *
	 * 为啥放在Sequencer接口中？ Barrier需要知道序号生成器(Sequencer)的生产进度，需要持有Sequencer对象引用。
     *
	 *  Create a new SequenceBarrier to be used by an EventProcessor to track which messages
     * are available to be read from the ring buffer given a list of sequences to track.
     *
     * @param sequencesToTrack All of the sequences that the newly constructed barrier will wait on.
	 *                         所有需要追踪的序列，其实也是所有要追踪的前置消费者。
	 *                         即消费者只能消费被这些Sequence代表的消费者们已经消费的序列
     * @return A sequence barrier that will track the specified sequences.
	 * 			一个新创建的用于追踪给定序列的屏障
     * @see SequenceBarrier
     */
    SequenceBarrier newBarrier(Sequence... sequencesToTrack);

    /**
	 * 获取序号生成器(Sequencer自身)和 所有追踪的消费者们的进度信息中的最小序号
     * Get the minimum sequence value from all of the gating sequences
     * added to this ringBuffer.
     *
     * @return The minimum gating sequence or the cursor sequence if
     * no sequences have been added.
     */
    long getMinimumSequence();

    /**
	 * 查询 nextSequence-availableSequence 区间段之间连续发布的最大序号。多生产者模式下可能是不连续的
	 * 多生产者模式下{@link Sequencer#next(int)} next是预分配的，因此可能部分数据还未被填充
	 *
     * Get the highest sequence number that can be safely read from the ring buffer.  Depending
     * on the implementation of the Sequencer this call may need to scan a number of values
     * in the Sequencer.  The scan will range from nextSequence to availableSequence.  If
     * there are no available values <code>&gt;= nextSequence</code> the return value will be
     * <code>nextSequence - 1</code>.  To work correctly a consumer should pass a value that
     * is 1 higher than the last sequence that was successfully processed.
     *
     * @param nextSequence      The sequence to start scanning from.
	 *                          事件处理器期望的下一个消费的序号
     * @param availableSequence The sequence to scan to.看见的已发布的最大序号
	 *                          多生产者模式下，已发布的数据可能是不连续的，因此不能直接该序号进行消费。
	 *                          必须顺序的消费，不能跳跃
	 *
     * @return The highest value that can be safely read, will be at least <code>nextSequence - 1</code>.
	 * 			返回的值可以安全的读(必须是连续的)，最小返回 nextSequence - 1，即我消费的最后一个序号，即返回时事件处理器什么也不做
     */
    long getHighestPublishedSequence(long nextSequence, long availableSequence);


	/**
	 * 创建一个数据轮询器
	 * 好像是支持测试用的
	 * @param provider
	 * @param gatingSequences
	 * @param <T>
	 * @return
	 */
	<T> EventPoller<T> newPoller(DataProvider<T> provider, Sequence... gatingSequences);
}