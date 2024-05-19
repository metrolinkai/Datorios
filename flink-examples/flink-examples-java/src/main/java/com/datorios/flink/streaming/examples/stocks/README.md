![](https://github.com/metrolinkai/Datorios/blob/main/resources/Horizontal%20Positive.png)

# STOCKS demo example
This program demonstrates the use of event-time processing, tumbling windows, and count windows in Apache Flink, showing how to handle and process real-time stock trade data.

The example consists on the java source code and the target jar file

### Package and Imports
```java
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
```

These imports include necessary Flink classes for creating data streams, defining windowing strategies, and processing functions. The package declaration organizes the code within a specific namespace.

### Class and Main Method
```java
public class StocksExample {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
```
- **StocksExample**: is the main class. Inside the _main_ method.
- **StreamExecutionEnvironment**: is a set up, which is the context in which the stream program is executed.

The parallelism is set to 1 for simplicity, ensuring single-threaded execution.

### Data Preparation
```java
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
```
A list of stock data is created using _Tuple4_, where each tuple represents a stock trade with the format: 
_(stock symbol, timestamp, volume, price)_.

### Creating the DataStream
```java
        DataStream<Tuple4<String, Integer, Integer, Double>> input = env.fromCollection(data);
        input.print();
```

The list is converted into a _DataStream_, which is the basic abstraction in Flink representing a stream of data. The _print_ method outputs the data to the console.

### Assigning Timestamps and Watermarks
```java
            input = input.assignTimestampsAndWatermarks(
                WatermarkStrategy
                        .<Tuple4<String, Integer, Integer, Double>>forMonotonousTimestamps()
                        .withTimestampAssigner((element, ts) -> element.f1)
        );
```
Timestamps and watermarks are assigned to handle event-time processing. The _WatermarkStrategy.forMonotonousTimestamps()_ is used because the timestamps in the data are monotonically increasing.

### Defining and Applying Windows
```java
        DataStream<Tuple4<String, Integer, Integer, Double>> output = input
                .keyBy(value -> value.f0)
                .window(TumblingEventTimeWindows.of(Time.seconds(60)))
                .process(new TimeSum())
                .keyBy(value -> value.f0)
                .countWindow(2)
                .process(new CountSum());
```
- **KeyBy:** The stream is partitioned by stock symbol using keyBy.
- **Window:** A tumbling event-time window of 60 seconds is defined.
- **Process:** The TimeSum process function is applied to aggregate data within each window.
- **KeyBy and CountWindow:** The result is re-partitioned by stock symbol and then grouped into windows of two elements each.
- **Process:** The CountSum process function is applied to the count-based windows.

### ProcessWindowFunction Implementations
```java
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
```
- **TimeSum:**: Aggregates the sum of trade volumes within each time window and outputs a new tuple with the sum.
- **CountSum:** Computes the average volume over two-element windows and outputs a new tuple with the average.

### Execution
```java
        output.print();
        env.execute("Stocks job");
    }
}
```
