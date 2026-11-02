# Java prerequisites — study with this repository

Do these in order. Write small programs before consulting reference solutions, explain their output,
and then compare your work with the relevant repository component. Request tutoring mode
for one lesson at a time; submit your attempt before receiving the solution.

1. Variables, primitive values, references, loops and methods: count words in a string.
   Explain why `==` and `.equals()` behave differently for strings.
2. Classes, constructors, records and validation: model a Document with a positive ID.
   Compare your result with `index/Document.java`. Explain record immutability limits when
   a record contains a mutable collection.
3. Interfaces and polymorphism: define a tokenizer interface and two implementations.
   Explain which callers depend on behavior and which require a concrete implementation.
4. Collections and generics: build Map<String,List<Long>>; contrast HashMap, TreeMap,
   HashSet and PriorityQueue. Predict iteration order and explain comparator direction.
5. Exceptions and resources: reject malformed input; write/read a file with try-with-resources.
   Explain checked IOException, unchecked validation errors and why silently catching either
   can hide data loss. Study the uncertain-commit handling in `node/SearchEngine.java`.
6. Unicode: compare UTF-16 units, code points and tokens. Normalize decomposed accents,
   full-width Latin characters and non-Latin words. Read `analyzer/Analyzer.java`.
7. Files and bytes: encode an integer, a UTF-8 string and a length prefix. Truncate each
   encoding and write a decoder that fails clearly rather than returning partial data.
8. Threads and visibility: implement two writers incrementing a counter. Demonstrate a
   lost update; fix it with synchronization. Explain why immutable data plus an AtomicReference
   is useful. Read `node/SearchEngine.java` and `ConcurrencyTest.java`.
9. Executors and cancellation: use a pool with a queue capacity of one; show rejection,
   interruption and shutdown. Reproduce why clearing queued work before canceling the
   active task fixed the shutdown race in `node/QueryExecutor.java`.
10. Networking: send an HTTP JSON request, handle non-200 responses, timeouts and invalid
    payloads. Explain why "no acknowledgment" does not prove a write failed.

You are ready for a subsystem lesson when you can implement its prerequisite exercise,
debug a deliberately broken variant, and explain the relevant invariant in your own words.
