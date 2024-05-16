package com.datorios.flink.streaming.examples.stocks;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StocksExample {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        List<Tuple4<String, Integer, Integer, Double>> data = new ArrayList<>();
        data.add(new Tuple4<>("Google", 39797000, 562, 832.58));
        data.add(new Tuple4<>("Google", 39612000, 261, 834.68));
        data.add(new Tuple4<>("Apple", 39642000, 821, 368.18));
        data.add(new Tuple4<>("Facebook", 39653000, 67, 209.25));
        data.add(new Tuple4<>("Apple", 39629000, 703, 238.13));
        data.add(new Tuple4<>("Amazon", 39888000, 937, 676.41));
        data.add(new Tuple4<>("Facebook", 39859000, 190, 712.95));
        data.add(new Tuple4<>("Apple", 39863000, 626, 517.24));
        data.add(new Tuple4<>("Google", 39656000, 303, 821.14));
        data.add(new Tuple4<>("Facebook", 39642000, 20, 606.77));

        DataStream<Tuple4<String, Integer, Integer, Double>> input = env.fromCollection(data);
        input.print();

        input = input.assignTimestampsAndWatermarks(
                WatermarkStrategy
                        .<Tuple4<String, Integer, Integer, Double>>forMonotonousTimestamps()
                        .withTimestampAssigner((element, ts) -> element.f1)
        );

        DataStream<Tuple4<String, Integer, Integer, Double>> output = input
                .keyBy(value -> value.f0)
                .window(TumblingEventTimeWindows.of(Time.seconds(60)))
                .process(new TimeSum())
                .keyBy(value -> value.f0)
                .countWindow(2)
                .process(new CountSum());

        output.print();

        env.execute("Stocks job");
    }

    static class TimeSum extends ProcessWindowFunction<
            Tuple4<String, Integer, Integer, Double>,
            Tuple4<String, Integer, Integer, Double>,
            String,
            TimeWindow> {

        @Override
        public void process(
                String key,
                Context context,
                Iterable<Tuple4<String, Integer, Integer, Double>> values,
                Collector<Tuple4<String, Integer, Integer, Double>> out
        ) throws Exception {
            Iterator<Tuple4<String, Integer, Integer, Double>> input = values.iterator();
            Tuple4<String, Integer, Integer, Double> ret = input.next();
            Integer sum = 0;
            for (Tuple4<String, Integer, Integer, Double> value : values) {
                sum += value.f2;
            }

            out.collect(new Tuple4<>(key, ret.f1, sum, ret.f3));
        }
    }

    static class CountSum extends ProcessWindowFunction<
            Tuple4<String, Integer, Integer, Double>,
            Tuple4<String, Integer, Integer, Double>,
            String,
            GlobalWindow> {

        @Override
        public void process(
                String key,
                Context context,
                Iterable<Tuple4<String, Integer, Integer, Double>> values,
                Collector<Tuple4<String, Integer, Integer, Double>> out
        ) throws Exception {
            Iterator<Tuple4<String, Integer, Integer, Double>> input = values.iterator();
            Tuple4<String, Integer, Integer, Double> ret = input.next();
            Integer sum = 0;
            Integer count = 0;
            for (Tuple4<String, Integer, Integer, Double> value : values) {
                sum += value.f2;
                count += 1;
            }

            out.collect(new Tuple4<>(key, ret.f1, sum / count, ret.f3));
        }
    }
}
