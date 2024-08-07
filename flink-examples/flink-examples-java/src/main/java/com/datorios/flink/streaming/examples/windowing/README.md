![](https://github.com/metrolinkai/Datorios/blob/main/resources/Horizontal%20Positive.png)

# Window demo example
This Apache Flink application sets up a streaming job that generates in-memory data representing heart rate readings with timestamps. It then processes this data using a tumbling window to find and output the maximum heart rate for each participant within each window period, allowing for some lateness in the data. The key components include setting up the execution environment, defining the data source, partitioning the data, applying windowing, computing the minimal value and producing a few sentences regarding that value.

The example consists on the java source code and the target jar file

### Package and Imports
```java
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
```

- **Package**: Defines the package org.apache.flink.streaming.examples.windowing.
- **Imports**: Imports necessary classes from Apache Flink libraries, including data types (tuples), utilities (ParameterTool), and various streaming and windowing classes.

### Class and Main Method
```java
public class WindowDemoExample {
   public static void main(String[] args) throws Exception {
```
- **Class Definition**: Defines the main class WindowDemoExample.
- **Main Method**: The entry point of the program.

### Parameter Initialization
```java
final ParameterTool params = ParameterTool.fromArgs(args);
final long allowedLateness = params.getLong("allowedLateness", 2000L);
final long tumblingWindowSize = params.getLong("tumblingWindowSize", 5000L);
final long limit = params.getLong("limit", 2000L);
final long rate = params.getLong("rate", 20L);
final long timeBetweenEvents = params.getLong("timeBetweenEvents", 500L);
```
- **ParameterTool**: Used to retrieve command-line arguments.
- **allowedLateness**: The maximum allowed lateness for elements in the window.
- **tumblingWindowSize**: Size of the tumbling window in milliseconds.
- **limit**: Maximum number of records to generate.
- **rate:** Rate at which records are generated.
- **timeBetweenEvents:** Time between events, event time based.

### Stream Execution Environment
```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
DataStream<Tuple3<String, Long, Integer>> source = env.addSource(new WindowDemoSampleData(limit, rate, timeBetweenEvents));
```

### Data Stream Processing
```java
        DataStream<Tuple3<String, Integer, String>> outputStream =
        source.keyBy(value -> value.f0)
                .window(TumblingEventTimeWindows.of(Time.milliseconds(tumblingWindowSize)))
                .allowedLateness(Time.milliseconds(allowedLateness))
                .min(2)
                .map(x-> new Tuple2<>(x.f0, x.f2)).name("Minimal Heart Rate Under Window")
                .returns(Types.TUPLE(Types.STRING, Types.INT))
                .keyBy(in -> in.f0)
                .flatMap(new PulseAdvisor());

```
- **keyBy**: Partitions the stream based on the participant's name (value.f0).
- **window**: Applies a tumbling event time window of specified size.
allowedLateness: Allows for late elements within a specified duration.
- **min:** Computes the minimal value in the third field (index 2) of the tuple, which is the pulse rate.
- **map:** Transforms the result to a new tuple containing the participant's name and their minimal pulse rate within the window.
- **name:** Sets a name for the operation.
- **returns**: Specifies the return type of the transformation.
- **keyBy**: Partitions the stream based on the participant's name (value.f0).
- **flatMap**: Transforms each record into 3 tuples of name, pulse, sentence where sentence is based on pulse value 

### Output and Execution
```java
        outputStream.print();
        env.execute();
    }
}
```
- **print:** Prints the resulting stream to the standard output.
- **execute:** Triggers the program execution in the Flink environment.
