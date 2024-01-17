# Issue Lifecycle and Reporting Guidelines

[TOC]

This document provides an external-facing explanation of how to file a bug
against a Jetpack library or infrastructure component and how to interpret the
various statuses and priorities displayed in the issue tracker.

## Issue tracker {#tracker}

The public-facing issue tracker URL is
[issuetracker.google.com](https://issuetracker.google.com).

The top-level Jetpack component is
[`Android Public Tracker > App Development > Jetpack (androidx)`](https://issuetracker.google.com/components/192731/manage#basic).
**Do not** file bugs against the top-level component. Individual library
components are organized by group and can be searched by entering the Maven
group in the issue tracker's component field, e.g. `androidx.core`.

Issues with tools, processes, or infrastructure should be reported under the
[`Jetpack (androidx) > Infrastructure`](https://issuetracker.google.com/components/705292/manage#basic)
component.

## Reporting guidelines {#reporting}

Issue Tracker isn't a developer support forum. For support information, consider
[StackOverflow](http://stackoverflow.com).

Support for Google apps is through
[Google's support site](http://support.google.com/). Support for third-party
apps is provided by the app's developer, for example through the contact
information provided on Google Play.

1.  Search for your bug to see if anyone has already reported it. Don't forget
    to search for all issues, not just open ones, as your issue might already
    have been reported and closed. To help you find the most popular results,
    sort the result by number of stars.

1.  If you find your issue and it's important to you, star it! The number of
    stars on a bug helps us know which bugs are most important to fix.

1.  If no one has reported your bug, file the bug. First, browse for the correct
    component -- typically this has a 1:1 correspondence with Maven group ID --
    and fill out the provided template.

1.  Include as much information in the bug as you can, following the
    instructions for the bug queue that you're targeting. A bug that simply says
    something isn't working doesn't help much, and will probably be closed
    without any action. The amount of detail that you provide, such as a minimal
    sample project, log files, repro steps, and even a patch set, helps us
    address your issue.

## Status definitions {#status}

| Status   | Description                                                       |
| -------- | ----------------------------------------------------------------- |
| New      | The default for public bugs. Waiting for someone to validate,     |
:          : reproduce, or otherwise confirm that this is actionable. Bugs in  :
:          : this state can be either Untriaged or Triaged. Untriaged state    :
:          : refers to a status where an issue has been filed against our      :
:          : team, but the team/person responsible for fixing the issue has    :
:          : not looked at it yet. Triaged state refers to a status where an   :
:          : issue filed against the team has been reviewed by the             :
:          : team/person, and they have accurately updated the priority of the :
:          : issue.                                                            :
| Assigned | In this state, the issue is ready to be added to an iteration     |
:          : with a level of certainty that it will be completed within the    :
:          : upcoming team iteration. Issues here should be >=P3               :
| Accepted | Actively being worked on by the assignee. Do not reassign.        |
| Fixed    | Fixed in the development branch. Do not re-open unless the fix is |
:          : reverted.                                                         :
| WontFix  | Covers all the reasons we chose to close the issue without taking |
:          : action (can't repro, working as intended, obsolete).              :

## Priority criteria and SLOs {#priority}

| Priority | Criteria                        | Resolution time                |
| -------- | ------------------------------- | ------------------------------ |
| P0       | This issue is preventing        | Less than 1 day. Don't go home |
:          : someone from getting work done  : until this is fixed.           :
:          : and doesn’t have a workaround.  :                                :
:          : Examples include service        :                                :
:          : outages, work-stopping issues,  :                                :
:          : and build breakages             :                                :
| P1       | This issue requires rapid       | Within the next 7 days         |
:          : resolution, but can be dealt    :                                :
:          : with on a slightly longer       :                                :
:          : timeline than P0. Examples      :                                :
:          : include issues that frequently  :                                :
:          : hinder workflow, serious        :                                :
:          : regressions, and ship-blocking  :                                :
:          : issues                          :                                :
| P2       | This issue is important to      | Within the next month          |
:          : resolve and may block releases. :                                :
:          : Examples include non-OKR        :                                :
:          : feature requests and infrequent :                                :
:          : workflow issues.                :                                :
| P3       | This issue would be nice to     | Less than 365 days             |
:          : resolve, but it's not going to  :                                :
:          : block any releases. Examples    :                                :
:          : include nice-to-have feature    :                                :
:          : requests, bugs that only        :                                :
:          : affects a small set of use      :                                :
:          : cases, and occasional issues.   :                                :
| P4       | Issue has not yet been          | N/A (must triage in under 14   |
:          : prioritized.                    : days                           :

## Issue lifecycle

1.  When an issue is reported, it is set to `Assigned` status for default
    assignee (typically the [library owner](/docs/owners.md))
    with a priority of **P4**.
1.  Once an issue has been triaged by the assignee, its priority will be raised
    from **P4** according to severity.
1.  The issue may still be reassigned at this point.
    [Bug bounty](/docs/onboarding.md#bug-bounty) issues are
    likely to change assignees.
1.  A status of **Accepted** means the assignee is actively working on the
    issue.
1.  A status of **Fixed** means that the issue has been resolved in the
    development branch. Please note that it may take some time for the fix to
    propagate into various release channels (internal repositories, Google
    Maven, etc.). **Do not** re-open an issue because the fix has not yet
    propagated into a specific release channel. **Do not** re-open an issue that
    has been fixed unless the fix was reverted or the exact reported issue is
    still occurring.
1.  A status of **WontFix** means that the issue cannot be resolved within a
    year due to prioritization, staffing, etc. **Do not** re-open an issue that
    has been marked as WontFix or file identical issues. Issues that are open to
    reconsideration will be added to an internal hotlist and revisited when the
    circumstances surrounding their closure have changed.
