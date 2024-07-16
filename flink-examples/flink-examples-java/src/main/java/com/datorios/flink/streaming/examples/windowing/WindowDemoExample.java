package com.datorios.flink.streaming.examples.windowing;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

public class WindowDemoExample {

    public static void main(String[] args) throws Exception {
        final ParameterTool params = ParameterTool.fromArgs(args);
        final long allowedLateness = params.getLong("allowedLateness", 7000L);
        final long tumblingWindowSize = params.getLong("tumblingWindowSize", 10000L);
        final long limit = params.getLong("limit", 2000L);
        final long rate = params.getLong("rate", 40L);
        final long timeBetweenEvents = params.getLong("timeBetweenEvents", 500L);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<Tuple3<String, Long, Integer>> source = env.addSource(new WindowDemoSampleData(limit, rate, timeBetweenEvents));


        DataStream<Tuple3<String, Integer, String>> outputStream =
                source.keyBy(value -> value.f0)
                        .window(TumblingEventTimeWindows.of(Time.milliseconds(tumblingWindowSize)))
                        .allowedLateness(Time.milliseconds(allowedLateness))
                        .min(2)
                        .map(x-> new Tuple2<>(x.f0, x.f2)).name("Minimal Heart Rate Under Window")
                        .returns(Types.TUPLE(Types.STRING, Types.INT))
                        .keyBy(in -> in.f0)
                        .flatMap(new PulseAdvisor());

        outputStream.print();

        env.execute();
    }

    private static class PulseAdvisor implements FlatMapFunction<Tuple2<String, Integer>, Tuple3<String, Integer, String>> {
        @Override
        public void flatMap(
                Tuple2<String, Integer> value,
                Collector<Tuple3<String, Integer, String>> out) throws Exception {
                if (value.f1 > 100) {
                    out.collect(new Tuple3<>(value.f0,value.f1," pulse is very high"));
                    out.collect(new Tuple3<>(value.f0,value.f1,value.f0 + " is working out?"));
                    out.collect(new Tuple3<>(value.f0,value.f1,"hey " + value.f0 + "! Great job!"));
                }
            if (value.f1 < 70) {
                out.collect(new Tuple3<>(value.f0,value.f1," is very low"));
                out.collect(new Tuple3<>(value.f0,value.f1,value.f0 + " is sleeping?"));
                out.collect(new Tuple3<>(value.f0,value.f1,"good night "+ value.f0));
            }
            if (value.f1 >= 70 && value.f1 <= 100){
                out.collect(new Tuple3<>(value.f0,value.f1," is normal"));
                out.collect(new Tuple3<>(value.f0,value.f1,value.f0 + " is probably watching TV"));
                out.collect(new Tuple3<>(value.f0,value.f1,value.f0 + "! how about a workout?"));
            }
        }
    }

}
