"""
Stream Data Distribution Demo with PyFlink

This example demonstrates how to distribute streaming data into different paths based on their validity.
It showcases two distinct distribution methods:
- Exclusive Distribution: Invalid events are sent only through a separate stream.
- Inclusive Distribution: All events go through one stream, but invalid events are also sent through a separate stream.

Users can configure their desired settings at the beginning of the script to see how each distribution method works.
"""

from pyflink.common.typeinfo import Types
from pyflink.datastream import StreamExecutionEnvironment
import logging
import sys

# Set the method for stream distribution here. Use "Exclusive Distribution" to send invalid
# events only through their separate stream. Use "Inclusive Distribution" to send all events
# through one stream and invalid events through another as well.
# Change this to "Inclusive Distribution"/"Exclusive Distribution" as needed:
method = "Inclusive Distribution"


def data_distribution_demo(dist_method):
    # Initialize the stream execution environment
    env = StreamExecutionEnvironment.get_execution_environment()
    # Define the source data stream
    ds = env.from_collection(
        collection=[
            ('001', "in_stock", 3),
            ('002', "in_stock", 5),
            ('003', "in_stock", 0),
            ('004', "in_stock", 6),
            ('005', "in_stock", 8),
            ('006', "in_stock", 1),
            ('007', "in_stock", 0)
        ],
        type_info=Types.TUPLE([Types.STRING(), Types.STRING(), Types.INT()])
    )

    # Function to check the validity of each event
    def check_validity(event):
        validity = "valid" if event[2] != 0 else "invalid"
        yield event, validity

    # Process the stream to tag events as valid or invalid
    valid_check_stream = ds.flat_map(check_validity)

    # Distribute the stream based on the chosen method
    if dist_method == "Exclusive Distribution":
        # Filter and map the valid and invalid events to separate streams
        valid_stream = valid_check_stream.filter(lambda x: x[1] == "valid").map(lambda x: x[0])
        invalid_stream = valid_check_stream.filter(lambda x: x[1] == "invalid").map(lambda x: x[0])
        valid_stream.print()
        invalid_stream.print()

    elif dist_method == "Inclusive Distribution":
        # Map all events to one stream and invalid events to another
        all_stream = valid_check_stream.map(lambda x: x[0])
        invalid_stream = valid_check_stream.filter(lambda x: x[1] == "invalid").map(lambda x: x[0])
        all_stream.print()
        invalid_stream.print()

    # Execute the environment to begin processing
    env.execute()


if __name__ == '__main__':
    # Configure logging for INFO level messages and specify the message format
    logging.basicConfig(stream=sys.stdout, level=logging.INFO, format="%(message)s")
    # Call the data_distribution_demo function with the chosen distribution method
    data_distribution_demo(method)  # 'method' should be previously defined based on user's choice

# Output for "Exclusive Distribution"
# ('001', 'in_stock', 3)
# ('002', 'in_stock', 5)
# ('004', 'in_stock', 6)
# ('005', 'in_stock', 8)
# ('006', 'in_stock', 1)
# ('003', 'in_stock', 0)
# ('007', 'in_stock', 0)

# Output for "Inclusive Distribution"
# ('001', 'in_stock', 3)
# ('002', 'in_stock', 5)
# ('003', 'in_stock', 0)
# ('004', 'in_stock', 6)
# ('005', 'in_stock', 8)
# ('006', 'in_stock', 1)
# ('007', 'in_stock', 0)
# ('003', 'in_stock', 0)
# ('007', 'in_stock', 0)
