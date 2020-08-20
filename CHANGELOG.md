# 1.0.1

Fixes:

* Closing process when terminating to stdin or stdout
* Multiline adding multiple args instead of a single one

# 1.0.0

Features:

* Read process output, error and write output in a single thread (excluding the ripper thread)
* Read process output and error in a single stream
* Allow to execute processes in chain using streams
* Allow to build with java version 1.8 and 1.9+
