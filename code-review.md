# Jetpack Code Review Etiquette

## Common Terms

*   **Code-Review levels**
    *   Give **Code-review +2** once you feel this change is submit-worthy assuming author fixed
     the minor shortcomings (no additional review needed)
    *   Give **Code-review +1** if you expect/need other reviewers to look at the change
    *   Give **Code-review -1** with a top level comment if the change is missing essential parts such as:
        *   missing or not enough tests (unless there is a good explanation why tests are not
         possible in the commit message)
        *   incomplete commit message
        *   change is too large and needs to be broken down into smaller changes
    *   Give **Code-review -2** sparingly in cases where the change as-is causes a risk to the project
*   **Author** - creator of a change looking for a code review
*   **Assignee** - the point person responsible for the given code review (who’s currently
 blocking). Each change should always have one active assignee.
*   **Reviewer** - supposed to review a given change, but currently is not the primary reviewer
 (assignee)
*   **CC** - Anyone else that you wish to be notified of the CL.  Does not need to review the given change (but can do so if they wish).
*   **Owner** (OWNERS file) - an expert in a given set of the code and is required to review any changes to that code. Higher level owners are responsible to make sure that any more direct owners are aware of the change or would not mind the given change.
*   **Testing** - every change should have an appropriate level of testing. Bug fixes should have regression tests, APIs change should have coverage of APIs and functionality. Manual testing is only acceptable if it is not easy to automate or requires new infrastructure.

## Reviewer Guidelines

*   Within 8 working hours, review every change you are assignee on (see go/rapidreview)
    *   Does not require resolution (ex. CR+2) of the change, but have a set of full
     comments or reassigning to a better reviewer
*   Within 16 working hours, review changes if you are not assignee on. It is appropriate to
 remove yourself from the review if you are not expecting to review the given change.
*   If already CR+2'ed by someone else, it is not required to give CR+2 again
*   Prioritize reviews from other timezones (_e.g. MTV reviewers should review LON changes before 11am if possible to expedite the change_)
*   Pay attention to the phrasing of your review comments
    *   Avoid using subjective statements such as “I don’t like it” or “This is bad”, use actionable ones like “Added APIs could cause performance issues, consider using Foo instead” or “Use more meaningful variable names”
    *   Do not attack the author, critique the code
    *   Keep in mind that you are helping somebody who asked for your help
*   Trust the author. Gerrit will not auto-submit changes with pending comments, thus eagerly give Code-review +2 if you only have nits left. This allows the author to correct the issues without an additional round of review.
*   If missing, request tests.
*   Request breaking down the changes into smaller ones if they are too large to be easily understood or contain multiple different fixes or improvements. Only acceptable large changes are simple renames
*   Set your calendar to OOO if you are OOO or the office name if you are travelling. Optionally, also set your [Gerrit status](https://android-review.googlesource.com/settings/) as OOO.

## Change Author Guidelines

*   Break down your changes to the smallest functional units (do not try to land giant changes without prior approval from relevant reviewers)
*   Only add reviewers to the first change if you have a stack of changes to allow reviewer to review in the logical order
*   Start with one reviewer that will be your primary
*   One reviewer should always be an assignee
*   Avoid adding unnecessarily large number of reviewers (e.g. do not add all OWNERS)
*   If you add more than one reviewer, send a PTAL (please take a look) message noting which
 reviewer should focus on what part of the change, e.g. _“aurimas@ PTAL at API changes, sumir
 @ PTAL at foo/bar/ changes, adamp@ for OWNERS review”_
*   Follow up on every comment left in the review either by following the suggestion and marking it done or stating why you chose not to (e.g. “this value can never be X, so the check is not required”). It is not acceptable to ignore a comment and simply mark it as resolved - discuss offline if needed.
*   Write complete commit messages
    *   explain why the change is needed
    *   have a bug in the commit message, unless the change is trivial. 
    *   explain how you tested the change
    *   use the imperative tense, (e.g. “Add …” instead of “Added …”) 
*   Do not shy away from scheduling an offline, GVC or IM conversation, especially if reviewers do not respond within the time stated above. Once something is resolved offline, add a comment about the decision and that the topic was discussed offline.
*   Share a design doc ahead of review for large features.
*   Avoid using googler-only links in a commit message or comments.
*   Keep in mind that code review comments are directed towards the code and not you. Do not take comments or Code-review -1/-2 personally, we use them as tools to progress the change towards landing successfully.
