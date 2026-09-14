# `permissions: {}` kills the Gradle Build Scan PR comment

A tiny repro for a confusing one: `gradle/actions/setup-gradle` stops posting its
Job Summary (the table of Build Scan links) as a PR comment, with no obvious
error on the PR itself.

It looks like a Develocity/Build Scan problem. It isn't. It's a GitHub token
permissions problem.

## The symptom

PR comments used to appear, then quietly stopped. The workflow is still green
(or red) as normal, the Build Scans still publish fine, the Job Summary still
shows up on the run page — there's just no comment on the PR.

The only trace is a **warning**, buried in the `Post Setup Gradle` step:

```
Minimizing obsolete Job Summary comments on PR #123.
Adding Job Summary as comment to PR #123.
##[warning]Failed to generate PR comment.
HttpError: Resource not accessible by integration - https://docs.github.com/rest/issues/comments#create-an-issue-comment
```

It's a warning, not an error, so the job stays green and nobody notices.

## The cause

Somebody hardened the workflow:

```yaml
permissions: {}
```

That's a good default — but it zeroes out **every** scope on `GITHUB_TOKEN`,
including `pull-requests: write`. And that's exactly the scope
`add-job-summary-as-pr-comment` needs to POST the comment.

So `setup-gradle` does all the work — collects the builds, renders the table with
the scan links — and then GitHub returns 403 on the write.

## The fix

Keep the deny-by-default at the workflow level, grant the one scope back on the
job that posts the comment:

```yaml
permissions: {}

jobs:
  build:
    permissions:
      contents: read
      pull-requests: write
```

## What's in here

| File | What it does |
|---|---|
| [`.github/workflows/broken.yml`](.github/workflows/broken.yml) | `permissions: {}` only → comment fails with 403 |
| [`.github/workflows/fixed.yml`](.github/workflows/fixed.yml) | adds job-level `pull-requests: write` → comment posts |

Both run the same trivial build and publish a Build Scan to the free
`scans.gradle.com` service, so **no secrets are needed** — fork it and it works.

## How to see it

1. Fork or clone this repo.
2. Open a PR against `main` (any change will do).
3. Both workflows run on the PR:
   - **Fixed** posts the Job Summary comment with the Build Scan links.
   - **Broken** posts nothing. Open its run → `build` job → expand
     **`Post Setup Gradle`** to find the `Resource not accessible by integration`
     warning.

To exercise the failure path too, add `-Pfail=true` to the `Execute hello` step —
useful if your real workflow uses `add-job-summary-as-pr-comment: 'on-failure'`.

## Two things that make this harder to spot

**1. `on-failure` hides it.** If your workflow has:

```yaml
add-job-summary-as-pr-comment: 'on-failure'
```

then comments only appear when a step actually fails. A run of green PRs looks
exactly like a broken comment setup. Both workflows here use `'always'` so the
behaviour is unambiguous.

**2. Other bots keep commenting.** Tools that post via their own GitHub App
installation token (TestLens, Renovate, Danger, …) are unaffected by your
`permissions:` block, so the PR still gets bot comments — just not this one.
That makes it look selective and sends you hunting in the wrong place.

## Fork PRs are a separate problem

Even with the fix, PRs **from forks** still won't get a comment: GitHub hands
fork PRs a read-only `GITHUB_TOKEN` regardless of what your `permissions:` block
says. That's by design and there's no workflow-level fix — the usual workaround
is a separate `workflow_run`-triggered job that posts the comment with a
privileged token.

Same caveat for scans, if you publish to your own Develocity instance: secrets
aren't exposed to fork PRs, so an `onlyIf { it.isAuthenticated }` guard will skip
publishing there entirely. (This repo sidesteps that by using the free
`scans.gradle.com` service, which needs no access key.)

## Demo PRs

Three PRs are kept open against this repo as live evidence:

- a green build (this one)
- a red build forced from the workflow step (`-Pfail=true`)
- a red build caused by a source change

Cross them with the three workflows to see which combinations produce a comment.
