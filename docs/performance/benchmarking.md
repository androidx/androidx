# Benchmarking in AndroidX

[TOC]

The public documentation at
[d.android.com/benchmark](http://d.android.com/benchmark) explains how to use
the library - this page focuses on specifics to writing libraries in the
AndroidX repo, and our continuous testing / triage process.

There are two types of benchmarks, summarized below:

|          | Macrobenchmark                  | Microbenchmark                  |
| -------- | ------------------------------- | ------------------------------- |
| Function | Measure high-level entry points | Measure individual functions.   |
:          : or interactions, such as        :                                 :
:          : activity launch or scrolling a  :                                 :
:          : list.                           :                                 :
| Scope    | Out-of-process test of full     | In-process test of CPU work.    |
:          : app.                            :                                 :
| Speed    | Medium iteration speed. It can  | Fast iteration speed. Often     |
:          : exceed a minute.                : less than 10 seconds.           :
| Tracing  | Results come with system        | Results come with a minimal     |
:          : traces.                         : system trace and a method trace :
:          :                                 : by default.                     :

For continuous testing, triage, and regression tracking, see
[Monitoring Benchmarks](monitoring.md).
