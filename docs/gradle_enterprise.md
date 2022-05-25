# Gradle Enterprise

<!--* freshness: { owner: 'rahulrav' reviewed: '2022-05-02' } *-->

[TOC]

## Introduction

Gradle Enterprise is used to speed up workflows that use GitHub actions for
[playground](playground.md) projects.

The Gradle Enterprise Server is running on AWS (`us-east-1`). This service is
running on a Kubernetes cluster and can serve HTTP2 traffic. This service is
managed by the Gradle team.

The `FQHN` for the service is `hosted-ge-androidx.gradle.com` (port `443`).

Note: The service will always use a `Location` header `ge.androidx.dev`. That is
the only `hostname` it deems valid for a request origin.
