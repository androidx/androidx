# Benchmarking in AndroidX

[TOC]

The public documentation at
[d.android.com/benchmark](http://d.android.com/benchmark) explains how to use
the library - this page focuses on specifics to writing libraries in the
AndroidX repo, and our continuous testing / triage process.

There are two types of benchmarks, summarized below:

|             | Macrobenchmark               | Microbenchmark                |
| ----------- | ---------------------------- | ----------------------------- |
| API version | 23 and later                 | 14 and later                  |
| Function    | Measure high-level entry     | Measure individual functions. |
:             : points or interactions, such :                               :
:             : as activity launch or        :                               :
:             : scrolling a list.            :                               :
| Scope       | Out-of-process test of full  | In-process test of CPU work.  |
:             : app.                         :                               :
| Speed       | Medium iteration speed. It   | Fast iteration speed. Often   |
:             : can exceed a minute.         : less than 10 seconds.         :
| Tracing     | Results come with System     | Results come with a minimal   |
:             : traces                       : System trace, and a method    :
:             :                              : trace by default.             :

For continuous testing, triage, and regression tracking, see
[Monitoring Benchmarks](monitoring.md).
