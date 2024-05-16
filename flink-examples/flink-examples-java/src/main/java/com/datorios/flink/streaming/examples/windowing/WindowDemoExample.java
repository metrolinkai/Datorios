package com.datorios.flink.streaming.examples.windowing;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

/**
 *  WindowDemoExample generates in memory streaming data as the jobs source in the form of
 *      (Name, Timestamp, Pulse)
 *  The job creates a tumbling time window and finds the maximal heart rate of each of the participants
 */
public class WindowDemoExample {

    public static void main(String[] args) throws Exception {
        final ParameterTool params = ParameterTool.fromArgs(args);
        final long allowedLateness = params.getLong("allowedLateness", 2000L);
        final long tumblingWindowSize = params.getLong("tumblingWindowSize", 5000L);
        final long limit = params.getLong("limit", 2000L);
        final long rate = params.getLong("rate", 20L);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<Tuple3<String, Long, Integer>> source = env.addSource(new WindowDemoSampleData(limit, rate));


        DataStream<Tuple2<String, Integer>> outputStream =
                source.keyBy(value -> value.f0)
                        .window(TumblingEventTimeWindows.of(Time.milliseconds(tumblingWindowSize)))
                        .allowedLateness(Time.milliseconds(allowedLateness))
                        .max(2)
                        .map(x-> new Tuple2<>(x.f0, x.f2)).name("Max Heart Rate Under Window")
                        .returns(Types.TUPLE(Types.STRING, Types.INT));

        outputStream.print();

        env.execute();
    }

}
