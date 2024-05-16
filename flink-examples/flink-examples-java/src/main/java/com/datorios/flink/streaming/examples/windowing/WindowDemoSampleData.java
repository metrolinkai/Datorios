package com.datorios.flink.streaming.examples.windowing;

import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import com.datorios.flink.streaming.examples.utils.ThrottledIterator;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Random;

public class WindowDemoSampleData implements SourceFunction<Tuple3<String, Long, Integer>> {

    static final String[] NAMES = {"tom", "jerry", "alice", "bob", "john", "grace"};
    static final Long[] CLOCK_TICK = {1000L, 200L, 1200L, 2000L, 2100L, -900L, 800L, 700L, 920L, 3200L, 500L -9000L, -800L};

    private final ThrottledIterator<Tuple3<String, Long, Integer>> source;
    public WindowDemoSampleData(Long limit, Long rate){
        source = new ThrottledIterator<>(new PulseMeasurementSource(limit), rate);
    }

    @Override
    public void run(SourceContext<Tuple3<String, Long, Integer>> ctx) throws Exception {
        Tuple3<String, Long, Integer> nextRecord = null;
        while(source.hasNext()) {
            nextRecord = source.next();
            ctx.emitWatermark(new Watermark(nextRecord.f1-1));
            ctx.collectWithTimestamp(nextRecord, nextRecord.f1);
        }
        ctx.emitWatermark(new Watermark(Long.MAX_VALUE));
    }

    @Override
    public void cancel() {

    }


    /** Continuously generates (name, timestamp, pulse). */
    private static class PulseMeasurementSource implements Iterator<Tuple3<String, Long, Integer>>, Serializable {

        private final Random rnd = new Random(hashCode());

        private Long clock = 983000L;

        private Long eventsCounter;

        public PulseMeasurementSource(Long eventsCounter){
            this.eventsCounter = eventsCounter;
        }

        @Override
        public boolean hasNext() {
            return eventsCounter > 0;
        }

        @Override
        public Tuple3<String, Long, Integer> next() {
            clock += getNextTick();
            eventsCounter--;
            return new Tuple3<>(NAMES[rnd.nextInt(NAMES.length)], clock ,50 + rnd.nextInt(100) );
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Long getNextTick() {
            Long tick = CLOCK_TICK[rnd.nextInt(CLOCK_TICK.length)];
            while (clock + tick <= 0) {
                tick = CLOCK_TICK[rnd.nextInt(CLOCK_TICK.length)];
            }
            return tick;
        }
    }


}
