# Monzo Backend Engineer: Master Interview Guide

*Compiled 20 November 2026 from Monzo-published material, candidate reports and engineering judgement.*

**Sourcing.** This guide draws on three tiers of public material: (1) Monzo-published: the live job ad and Greenhouse form, the "Demystifying the Backend Engineering interview process" post, the AI-use guidance, the Engineering Progression Framework v4.1 and the Engineering Principles (March 2025); (2) candidate reports from Glassdoor snippets and a few prep sites; (3) my own engineering judgement, labelled *(my guidance)* or *(inference)*. It is not inside knowledge of Monzo's scorecards.

**Correction.** "Be Proud, Help People, Dream Big" comes from a third-party prep site. It does not appear in any Monzo material I found. Section 5 has the values Monzo actually publishes.

## 1. The application and CV screening

### 1.1 What the CV needs to show

Monzo publishes no CV rubric. The best evidence is the live Senior Backend ad and the interview design: the initial call is built around your CV, so interviewers pick projects from it. The ad's "You should apply if" list:

- Strong backend experience on a technology product
- A product mindset (customer outcomes, data-informed decisions)
- Comfort with ambiguity
- Interest in distributed systems and resilient software
- Some experience with a strongly-typed language (Go, Java, C, Scala etc.)

The ad says Monzo does not require degrees or formal qualifications, and hires technology-agnostic, so prior Go, Cassandra or Kafka is not required. The listed stack: Go, Cassandra, Kafka, Kubernetes, Docker, Envoy, AWS (production), GCP (data), React, plus some physical data centres.

**Level matters.** The current ad targets Senior (L50) at £105–135k plus incentive awards, and the process interviews across the whole framework. Older ads listed Engineer II at £60–80k and Engineer III at £78–110k. Apply at the level your evidence supports.

| Level | Scope Monzo expects | CV evidence |
| --- | --- | --- |
| Engineer II | Tasks and small problem spaces | Owned features end to end, tests, PR reviews, fixed own defects |
| Engineer III | Whole project or feature lifecycle | Designed and delivered medium projects, mentoring, observability and runbooks, incident ownership |
| Senior (L50) | Large or ambiguous projects | Led complex projects and incidents, debt vs perfection trade-offs, explained technical risk to non-engineers, raised others' standards |
| Staff (L60) | Widespread impact across an area | Multi-team technical direction, safe rollouts, representing the company externally |

**CV structure (1–2 pages)** *(my guidance)*:

1. Header with GitHub, LinkedIn and any talks or blog posts.
2. A 2–3 line summary: years of backend work, domain, primary languages, the kind of system you have owned.
3. Experience bullets in the form **verb, mechanism, measurable outcome, your role**.
   - Weak: "Worked on payment microservices."
   - Strong: "Designed idempotent retry handling for a Kafka-based payment-event consumer (Go), eliminating duplicate side effects during broker rebalances. Reduced customer-visible duplicates from \~0.3% to 0 over 6 months."
4. Skills grouped by category (languages, datastores, messaging, infrastructure, observability). List only what you can defend for 10 minutes, since Monzo says it probes anything you name.
5. Education and certifications in one line.

**Keywords worth including, if true:** Go, Java/Kotlin/Scala, distributed systems, microservices, Kafka or other queues, Cassandra or another SQL/NoSQL store (and the trade-offs you weighed), Kubernetes, AWS, idempotency, eventual consistency, on-call and incident response, SLOs, testing strategy, payments or ledger work, security-sensitive systems.

**Metrics that read well:** throughput and latency (p99), reliability (error rate, incident reduction, SLO attainment), cost, migration scale (services, tables, rows moved with no downtime), adoption, and team multipliers (engineers onboarded, RFCs written).

### 1.2 What the application form evaluates

The live Greenhouse form has these fields:

- **Required:** CV; "What attracted you to Monzo?"; "What's the most interesting project you've worked on recently?" (objective, why it was interesting, what you learned, links welcome); location; right to work; privacy confirmation; a US-person question.
- **Optional:** cover letter, LinkedIn, and an anonymous diversity survey that Monzo says it cannot tie to you and that does not affect the outcome.

*(Inference)* The two free-text answers are probably the main filter beyond the CV. Strong answers:

- **Why Monzo:** a specific engineering angle, such as building a bank on microservices, the engineering blog's account of avoiding Cassandra counters for the crowdfunding limit, or Go with static analysis for error handling. Generic "innovative fintech" answers carry no signal.
- **Interesting project:** the same project you will defend in the initial call. State the goal, your decisions, one trade-off you would revisit and a measurable result. Link a repo or write-up if possible.

**AI rules.** Monzo says it will not mark you down for using AI. It asks you to "refine, not replace", and not to invent examples. AI is fine for polishing, not for live interviews. If you need AI for accessibility, tell your recruiter beforehand.

**Other facts:** the ad has no closing date; Monzo can sponsor visas for this role per the ad; referrals exist (a tiny Glassdoor sample showed about 80% applying online and 20% via referral).

### 1.3 Common reasons for rejection at screening

Monzo does not publish rejection reasons, so these are inferred from the ad, the blog and candidate reports.

- **Weak backend depth.** One reviewer from a front-end-heavy background said they were rejected for lacking depth in multithreading and concurrency. A full-stack CV needs real backend evidence.
- **Generic or AI-boilerplate free-text answers** that could apply to any company. Monzo explicitly warns against invented AI examples.
- **"We" with no "I".** Monzo says it interviews you, not your team.
- **Technology lists without outcomes:** no scale, no trade-offs, no results.
- **No evidence of operating systems in production.** The framework weights observability, incidents, runbooks and ownership.
- **Level mismatch** in either direction.
- **No strongly-typed language experience** (flagged in the ad, though gaps can be offset).
- **Right-to-work or location mismatches.**

## 2. Complete interview process, stage by stage

Monzo's own post describes a recruiter call and four stages. Candidate reports from 2018 to 2026 broadly match, with older loops including an on-site.

### Stage 0: recruiter call (\~30 min)

- **What happens:** The recruiter learns your background, explains the mission and process, and answers questions. One candidate was briefed on how to prepare for the behavioural stage.
- **Scored on:** Not formally. Motivation, communication and level fit.
- **Do:** Ask what coding format options exist, how long each stage takes and what level you are being considered for. Ask about reasonable adjustments if relevant.

### Stage 1: initial call, technical deep dive (1 hr)

- **What happens:** An engineer who has read your CV asks about recent projects: how it came about, implementation, trade-offs, what you would do differently. Typical topics are a big technical challenge, customer-facing apps and cross-functional work. No coding or whiteboarding.
- **Scored on** *(inferred)*: technical depth and accuracy; personal contribution vs team; quality of trade-off reasoning; self-awareness; product thinking.
- **Do:** Prepare 2–3 projects. For each write: problem, constraints, alternatives rejected, failure modes, numbers and a retrospective.

### Stage 2: coding (your choice)

**Option A: take-home plus review call**

- Asynchronous, in your own IDE and language. Submit a README with structure, run instructions and trade-offs.
- Monzo checks the program functions as described. There are no bonus points for extras.
- The review call is a walkthrough on a shared screen covering language choice, design, data structures and performance. The interviewer reads your code and tries to run it beforehand.
- Reports consistently say the task is a **web crawler**. Review calls then went into multithreading choices, scaling across machines, queues, distributed locks and a distributed rate limiter. A 2018 candidate recalls a 45-minute discussion.
- Timebox: Monzo's backend post gives none. A prep site quotes four hours from Monzo's *mobile* guide, so treat that as indicative only.

**Option B: live pairing (45 min)**

- You implement functions to satisfy an interface, with pre-written unit tests. Languages include Go, Python and Java. It is not a speed test. You can use docs, Google and StackOverflow.
- **Scored on:** idiomatic code, structure, data-structure understanding, iterative approach, communication and handling ambiguity.

**Which to pick** *(my guidance)*: Choose the take-home if you write cleaner code alone, and accept that the review is where you will be judged hardest. One London candidate in March 2026 passed earlier stages but was rejected at the take-home review. Choose pairing if you narrate well and want the problem constrained.

### Stage 3: systems design (1 hr)

- **What happens:** You design a hypothetical, non-Monzo system with an engineer in Excalidraw. They probe trade-offs, challenge assumptions and add information that forces changes. Diagram neatness does not matter; shared understanding of flow and of performance, consistency and availability trade-offs does.
- **Scored on:** requirement scoping; first-principles reasoning instead of reciting textbook designs; failure-mode analysis; depth on any technology you name (including AWS/GCP services); adapting when requirements change.
- **Reported:** you may receive a partially designed system to complete. One candidate was rejected largely on system design after about 40 hours of dedicated prep. An Engineering Manager candidate reported only one engineer present who decided the outcome.

### Stage 4: behavioural (1 hr, two interviewers)

- **What happens:** Four areas: communication, learning, teamwork and delivering projects. Monzo says collaboration and delivery matter as much as technical excellence.
- **Scored on:** stakeholder alignment, incremental delivery, handling complexity, level of leadership vs participation, and values alignment.

### Stage 5: decision

A Hiring Committee reviews all feedback, checks for bias and matches you to a team. The decision is holistic, not stage-by-stage.

**Timing.** The live ad says an average of about 4 weeks. Glassdoor lists roughly 30 days to hire for backend roles. Senior Staff roles add a leadership interview.

## 3. Real interview questions, solutions and trade-offs

### 3.1 Questions reported for Monzo

| # | Question or topic | Stage | Origin |
| --- | --- | --- | --- |
| 1 | Describe a past project in great technical detail and how you handled stakeholders | Initial call | Glassdoor, UK (2025–26) |
| 2 | Hypothetical follow-ups on your own project | Initial call | Glassdoor, UK |
| 3 | Experience designing distributed systems; coding languages | Phone screen | Glassdoor, UK (older) |
| 4 | How would a system deadlock? | Phone screen | Glassdoor, London (2017) |
| 5 | What is the difference between a thread and a process? | Technical | Glassdoor, UK |
| 6 | How does the internet work? | Technical | Glassdoor, London (2018) |
| 7 | General knowledge: networks, security, data structures | On-site | Glassdoor, London (2018) |
| 8 | Implement a web crawler | Take-home | Glassdoor, UK (multiple reports) |
| 9 | Ways to create and manage threads in Java; pros and cons | Take-home review | Glassdoor, UK |
| 10 | Why should a thread pool not be too large or too small? | Take-home review | Glassdoor, UK |
| 11 | How to batch or distribute work across threads | Take-home review | Glassdoor, UK |
| 12 | Scaling with queues and databases; distributed locks | Take-home review | Glassdoor, UK |
| 13 | Implement a distributed rate limiter | Take-home review | Glassdoor, UK |
| 14 | Complete a partially designed system | Systems design | Glassdoor, UK |
| 15 | Spotify top-k problem (as prep advice) | Systems design | Glassdoor, UK (Engineering Manager loop) |
| 16 | Notification system / payment processor (outbox, saga, idempotency probed) | Systems design | Third-party prep site (TechPrep), unverified |
| 17 | Payment processing, real-time notifications, multi-currency ledger | Systems design | Third-party prep site (FinalRound), unverified |
| 18 | Implement a rate limiter; unique combinations summing to a target | Coding | Third-party prep site, unverified; contradicts Monzo's "no leetcode" statement |
| 19 | Two requests update the same account balance; what goes wrong? | Design concept | Third-party prep site (prepare.sh), a generic backend question |

**International and analogous questions.** I found no verifiable non-UK Monzo backend loop reports; Glassdoor's regional domains mirror the same Monzo reviews. These Blind (US) reports are not Monzo's but closely resemble Monzo's topics: eBay, design a web crawler; Uber, a Robinhood-style backend with push notifications when a stock moves more than 5%; LinkedIn, tiny URL across multiple data centres. Use them as practice, not as predicted Monzo questions.

### 3.2 Model solutions

*All of this is my engineering guidance, not Monzo's marking scheme.*

#### A. Concurrent web crawler

**Design:**

- Define a `Fetcher` interface so the crawler is testable with a fake and no network.
- One coordinator goroutine owns the **frontier** (queue) and the **visited set**. Workers fetch and send results back over a channel. This removes shared-memory races.
- Normalise URLs: resolve relative links, drop fragments, lower-case the host, and consider trailing slashes and query parameters.
- Restrict to the seed host unless told otherwise.
- Terminate with a **pending counter** (+1 per enqueued URL, −1 per processed result). Stop when it reaches zero.
- Use `context` for cancellation and timeouts. Add retries with exponential backoff plus jitter, a max depth or page budget, and a response size cap.

```go
type Fetcher interface {
    Fetch(ctx context.Context, url string) (links []string, err error)
}

func Crawl(ctx context.Context, f Fetcher, seed string, workers int) []string {
    type result struct{ url string; links []string; err error }
    jobs, results := make(chan string), make(chan result)
    var wg sync.WaitGroup
    for i := 0; i < workers; i++ {
        wg.Add(1)
        go func() { defer wg.Done()
            for u := range jobs { l, err := f.Fetch(ctx, u); results <- result{u, l, err} }
        }()
    }
    seen := map[string]bool{seed: true}
    queue, pending := []string{seed}, 0
    var order []string
    for len(queue) > 0 || pending > 0 {
        var out chan string; var next string
        if len(queue) > 0 { out, next = jobs, queue[0] }
        select {
        case out <- next: queue = queue[1:]; pending++
        case r := <-results:
            pending--; order = append(order, r.url)
            for _, l := range r.links { if !seen[l] { seen[l] = true; queue = append(queue, l) } }
        case <-ctx.Done(): close(jobs); wg.Wait(); return order
        }
    }
    close(jobs); wg.Wait(); return order
}
```

**Trade-offs to articulate:**

- Coordinator-owned state (simple, no locks, one serial point) vs a mutex-protected shared set (more parallel, easier to get wrong).
- BFS vs DFS: BFS gives shallow pages first and bounded depth; DFS uses less frontier memory.
- Pool size: for I/O-bound work, higher than core count but bounded to avoid exhausting file descriptors and hammering hosts. For CPU-bound parsing, around core count. Too few leaves latency unused; too many causes context switching, memory pressure and downstream overload.
- Memory growth of the `seen` set: cap it, or use a Bloom filter at scale and accept false positives.
- Error policy: retry transient errors; never retry 4xx except 429.

**Scaling to multiple machines:**

- **Partition the frontier by host** (hash of hostname) across Kafka partitions or queue shards. Each host is then owned by one worker group, so politeness and per-host rate limiting become local.
- **Dedupe:** a sharded key-value store with unique-insert semantics, or Bloom filters per shard. A Bloom filter can wrongly mark a page as seen, so say you accept that or use a backing store.
- **Delivery:** at-least-once with idempotent processing (the visited check is the idempotency mechanism). Dead-letter queue for poison URLs.
- **Backpressure:** bounded queues and consumer-lag metrics.
- **Observability:** crawl rate, error rate by host, queue depth.

#### B. Rate limiters (including distributed and sliding window)

| Algorithm | Mechanism | Memory | Accuracy | Notes |
| --- | --- | --- | --- | --- |
| Fixed window | Counter per key per window | O(1) | Poor at edges (up to 2x burst at boundary) | Simplest |
| Sliding window log | Timestamps in a sorted set | O(limit) | Exact | Costly for high limits |
| Sliding window counter | Weighted previous + current window | O(1) | Approximate | Good default |
| Token bucket | Tokens refill at rate r, capacity b | O(1) | Allows bursts up to b | Common in APIs |
| Leaky bucket | Queue drains at constant rate | O(queue) | Smooths output | Good for shaping |

**Sliding window counter formula:** `estimate = prev_count x (1 - elapsed_fraction) + current_count`. Reject if `estimate >= limit`.

**Distributed implementation (Redis, atomic via Lua):**

```lua
-- sliding-window log: KEYS[1]=key; ARGV: now_ms, window_ms, limit, unique_member
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1] - ARGV[2])
if redis.call('ZCARD', KEYS[1]) < tonumber(ARGV[3]) then
  redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
  redis.call('PEXPIRE', KEYS[1], ARGV[2])
  return 1
end
return 0
```

**Trade-offs interviewers want:**

- **Centralised store:** accurate, but adds a hop, a hot key and a dependency. Mitigate with sharded keys per client and replicas.
- **Local limiters with periodic sync:** resilient and fast but approximate; a client can exceed the global limit by roughly node-count x local allowance. Often acceptable.
- **Fail open vs fail closed** when the limiter is down. A fraud or login endpoint may fail closed; general traffic usually fails open with a local fallback.
- Clock skew (use the store's clock or a single time source), and whether limits are per user, IP, API key or host.
- Rejection behaviour: return 429 with `Retry-After`. Clients should back off with jitter.

#### C. Top-N / top-K over a stream (the "Spotify" problem)

**Exact approach (small cardinality):** hash map of counts plus a min-heap of size k. Heap updates are O(log k).

**Approximate (high cardinality):**

- **Count-Min Sketch:** width w and depth d give error about epsilon x N with probability at least 1 - delta, where w = ceil(e / epsilon) and d = ceil(ln(1 / delta)). It never underestimates. Pair it with a heap of candidates.
- **Space-Saving (Metwally et al.):** keeps k counters, replacing the minimum, with deterministic error bounds. Often the better answer for pure top-k.

**Time windows ("top tracks in the last hour"):** time-bucketed counters (per minute) summed over the window, or exponential decay. Tumbling vs sliding windows trades accuracy against state.

**Distributed pipeline:**

1. Events go to Kafka, **partitioned by item key**, so each item's counts live on one partition and local top-k is correct.
2. Each partition computes local top-k per window.
3. A merge step takes the top-k of the union. This is exact **only** if keys are partitioned. If one key's events are spread across partitions, sum counts first.
4. Store results in a fast read store and cache them.

**Trade-offs and edge cases:** exact vs approximate (accuracy against memory), hot keys, late and out-of-order events (watermarks and allowed lateness), ties, replays (idempotent aggregation), freshness vs cost, and a batch reconciliation layer for correctness.

#### D. Distributed locks

**Why you may not need one:** prefer designs that avoid locks. Partition so one worker owns each entity, use idempotency, or use a conditional write (compare-and-set).

**If you do need one:**

- **Redis `SET key value NX PX ttl`** with a unique token, released by check-and-delete in Lua. Simple and fast, but not safe on its own for correctness, because a paused client can outlive its lease.
- **Lease plus fencing token:** the lock service issues a monotonically increasing token, and the resource rejects writes with an older token. This is the key concept that makes lease-based locks safe.
- **etcd or ZooKeeper (consensus-based):** stronger guarantees via Raft or Zab, with higher latency.
- **Database locks** (row lock, advisory lock): simple if you already depend on the database.

**Trade-offs:** safety vs liveness, lease TTL (too short loses the lock mid-work, too long delays recovery), clock assumptions, single point of failure, and the multi-node "Redlock" debate (say it relies on timing assumptions).

#### E. Outbox and saga

**Problem:** you update the database and publish an event. A crash between the two leaves the data and the world inconsistent (the dual-write problem).

**Transactional outbox:**

1. In one local transaction, write the business row and an `outbox` row.
2. A relay (polling publisher or change-data-capture) publishes outbox rows to the broker and marks them sent.
3. Delivery is **at-least-once**, so consumers must be **idempotent** (dedupe by event ID).

**Trade-offs:** polling latency and load vs CDC complexity, ordering (per-aggregate ordering via key), outbox table growth (cleanup), and duplicates.

**Saga:** a multi-step business transaction across services using **compensating actions** instead of a distributed lock or two-phase commit.

- **Orchestration** (a coordinator drives the steps): clearer flow and easier to debug, but a central component.
- **Choreography** (services react to events): decoupled, but harder to follow and test.
- Compensations must be idempotent and retryable. Some steps cannot be undone (a sent email), so order steps carefully (pivot step, retriable steps after).

#### F. Idempotency (appears in nearly every payment design)

- The client sends a unique **idempotency key**. The server stores `(key -> request hash, status, response)` with a **unique constraint**.
- States: *in-progress*, *completed*, *failed*. A concurrent duplicate sees *in-progress* and waits or returns 409. A completed duplicate returns the stored response.
- Check the request hash so the same key cannot be reused with a different payload.
- Set a TTL on keys, and make the key store and the side effect atomic (same transaction), or make the side effect itself idempotent.
- **Consumer side:** dedupe by message ID or use natural idempotency (upserts keyed on business ID).

#### G. Ledger and concurrent balance updates

- **Why it fails:** "read balance, compute in the app, write back" gives a **lost update** when two requests interleave.
- **Fixes:** an atomic conditional update (`UPDATE ... SET balance = balance - x WHERE balance >= x`), optimistic locking with a version column and retry, `SELECT ... FOR UPDATE`, higher isolation with retry, or **single-writer-per-account** via partitioning.
- **Ledger model:** immutable, append-only **double-entry** entries (debits equal credits), money as integers in minor units (never floats), balances derived or snapshotted, and idempotent posting with a unique transaction ID.
- **Cross-account transfers:** two-entry posts in one atomic unit if co-located, otherwise a saga with compensations and a reconciliation job to detect drift.
- **Monzo specifically:** I could not verify how Monzo builds its ledger internally. Their blog confirms Cassandra as the main store and describes avoiding counters for the crowdfunding limit. In the interview, reason from first principles and state your assumptions.

#### H. "Notify me when a payment arrives": where can it fail?

- The payment service commits the payment, and an outbox row emits `PaymentReceived`.
- A notification consumer dedupes by event ID, loads preferences and device tokens, applies quiet hours, and calls the push provider with retries and a DLQ.
- **Failure modes:** provider outage (retry with backoff plus circuit breaker), duplicate delivery, out-of-order events, stale device tokens, hot partition from a burst, and a notification arriving before the app shows the transaction (consider read-after-write).
- **Principle:** notifications are non-critical and must never block or fail the payment path.

#### I. Short-answer fundamentals

- **Deadlock:** needs all four Coffman conditions (mutual exclusion, hold-and-wait, no pre-emption, circular wait). Break one: consistent lock ordering, lock timeouts, try-lock with backoff, or avoid nested locks.
- **Thread vs process:** a process has its own address space and resources; threads share a process's memory. Threads are cheaper to create and switch but can race. Processes give isolation.
- **Thread pool sizing:** too small leaves I/O latency unused; too large causes context-switch overhead, memory pressure and downstream overload. Rule of thumb: CPU-bound is about the core count, I/O-bound is about cores x (1 + wait/compute). Then measure.
- **How the internet works:** DNS resolution, TCP handshake, TLS handshake, HTTP request, load balancer, server, response, plus caching and CDNs. Go one level deeper where you are strong.

## 4. Study syllabus and roadmap

### 4.1 Distributed systems (highest priority)

Monzo explicitly recommends *Designing Data-Intensive Applications*, especially Part II.

| Topic | What to master |
| --- | --- |
| CAP and PACELC | The real meaning (a partition forces a choice of C or A); the latency vs consistency trade-off when there is no partition |
| Consistency models | Linearizable, sequential, causal, read-your-writes, monotonic reads, eventual |
| Replication | Leader/follower, multi-leader, leaderless quorums (R + W > N), replication lag and its anomalies |
| Partitioning | Hash vs range, consistent hashing, hot keys, rebalancing |
| Transactions and isolation | Dirty read, non-repeatable read, phantom, **lost update**, **write skew**; read committed, snapshot, serializable |
| Consensus | Raft and Paxos conceptually, leader election, why it matters for locks and coordination |
| Time | Clock skew, Lamport and vector clocks, why wall clocks are unsafe for ordering |
| Delivery semantics | At-most-once, at-least-once, "exactly-once" as idempotent at-least-once |
| Failure handling | Timeouts, retries with backoff and jitter, circuit breaker, bulkhead, load shedding, backpressure, graceful degradation |
| Patterns | Idempotency, outbox, saga, CQRS, event sourcing, change-data-capture |

### 4.2 Concurrency

- Race conditions, memory visibility, mutex vs channels, atomics.
- Go: goroutines, buffered and unbuffered channels, `sync.Mutex/RWMutex/WaitGroup`, `context` cancellation, `errgroup`, worker pools, the race detector, goroutine leaks.
- Java (if that is your language): executors, `CompletableFuture`, concurrent collections, thread-pool tuning.
- Deadlock, livelock, starvation, producer/consumer, backpressure.

### 4.3 Databases and messaging

- **SQL:** indexes, query plans, transaction isolation in practice, optimistic vs pessimistic locking, constraints as correctness tools.
- **Cassandra** (Monzo's main store): partition and clustering keys, query-first data modelling, tunable consistency (ONE, QUORUM, LOCAL\_QUORUM), the write path (commit log, memtable, SSTable), tombstones, why counters and read-before-write are risky, lightweight transactions and their cost.
- **Kafka:** topics, partitions, key-based ordering, consumer groups, offsets and commits, rebalances, retention and compaction, delivery semantics, DLQ patterns, lag.
- **Caching:** cache-aside, write-through, invalidation, stampede protection.

### 4.3b Architecture, operations and security

- **Microservices:** boundaries, RPC, versioning and backwards compatibility, shared libraries. Reporting puts Monzo at roughly 1,600 services, primarily Go on Cassandra, with static analysis checking error handling.
- **Kubernetes and Docker basics:** pods, deployments, probes, resource limits, rolling releases.
- **Observability:** metrics, logs, traces, SLIs/SLOs, percentiles, alert quality, runbooks.
- **Safe delivery:** small frequent changes, feature flags, canaries, rollback.
- **Security:** authentication vs authorisation, secrets, injection, rate limiting and abuse design. Monzo's principle "design systems to prevent misuse" applies.
- **Testing:** unit, integration, contract and property-based tests, plus testing concurrency.

### 4.4 Banking domain (a differentiator, not a requirement)

- Double-entry bookkeeping and why ledgers are append-only.
- Payment lifecycle: authorisation, clearing and settlement; reconciliation.
- UK rails at a conceptual level (Faster Payments, Bacs, CHAPS) and card schemes (the framework mentions Mastercard as a domain).
- Regulation in outline: FCA/PRA oversight, auditability, fraud and financial-crime controls.

### 4.5 Monzo's engineering principles (read these)

The March 2025 principles are worth knowing, because interviewers use them as a lens on your trade-offs. In short: make changes small and often; unblock others whenever you can; leave things better than you found them; optimise for impact-to-effort ratio; take ownership of problems; consider the reversibility of decisions; design systems to prevent misuse; think global first; build extensible, not flexible, systems; treat tests as the foundations of the product; do not optimise prematurely; technical debt is a useful tool when managed; build systems to be read and debugged by humans; pay attention to unexpected system behaviour.

**Use them in design interviews.** For example: "I would start with a simple single-writer-per-account design and only add sharding once data shows a bottleneck", or "I would make this decision reversible by putting the idempotency key store behind an interface".

### 4.6 Six-week timeline

| Weeks | Focus |
| --- | --- |
| 1 | Project briefs, STARR stories, DDIA Part II (replication, partitioning, transactions) |
| 2 | Concurrency refresher in your language; build the crawler with tests and a README; rehearse its review questions aloud |
| 3 | Rate limiter, idempotency store and outbox relay as small coded exercises; interface-plus-tests pairing practice, timed at 45 min |
| 4 | System design: 6–8 problems (notification, ledger, rate limiter, top-k, payments, webhook delivery, job scheduler, idempotent API) |
| 5 | Mock interviews (design and behavioural), Cassandra and Kafka depth, read Monzo's blog and framework |
| 6 | Polish, questions for them, tech setup check |

## 5. Behavioural and cultural alignment

### 5.1 Monzo's actual values

Monzo's job listings state values along these lines: be hard on problems, not people; think customer first and grow Monzo safely; default to transparency; and think big, start small, own it. Monzo recruiters describe defaulting to transparency as particularly important in how they hire. Combine these with the engineering principles and the Progression Framework when building your stories.

| Area | Values and principles it maps to | What they look for |
| --- | --- | --- |
| Communication | Default to transparency; build systems for humans | Written proposals, early sharing, explaining risk to non-engineers |
| Learning | Be hard on problems, not people; leave things better | Honest "I don't know", learning from incidents, feedback both ways |
| Teamwork | Be hard on problems, not people; unblock others | Disagreeing well, mentoring, reviews, sharing credit |
| Delivering projects | Think big, start small, own it; small changes often; impact-to-effort | Incremental delivery, stakeholder alignment, pragmatic trade-offs |

**Rules from Monzo's blog:** frame your own contribution, not your team's. Saying "I don't know" is fine if you reason from first principles. Distinguish what you led from what you joined.

### 5.2 Question bank

*Indicative: one prep site's list, not Monzo-confirmed.*

- Walk me through a recent complex project. What decisions did you make and why?
- Tell me about improving the reliability or performance of a system.
- Describe a technical disagreement. What did you do?
- Tell me about delivering under competing priorities.
- How do you learn something new or tackle an unfamiliar problem?
- Explain a complex technical concept to a non-technical audience.
- Tell me about a mistake you made in production and what changed afterwards.
- How have you kept stakeholders aligned on a long project?
- When did you level up another engineer?

### 5.3 Three sample STARR story structures

These are templates with placeholders. Monzo says not to use invented examples, so replace every detail with a real experience of yours. Monzo suggests the STARR format (Situation, Task, Action, Result, Reflection).

**Story 1: ownership and "be hard on problems, not people" (a production incident)**

- **Situation:** \[Your service\] began producing \[customer-visible problem\] after \[change\]. \[Scale and impact in numbers.\]
- **Task:** You were the on-call or owner and needed to stop the harm and find the root cause.
- **Action:** Mitigate first (roll back or flag off), communicate status to affected teams on a clear cadence, then diagnose using \[metrics and logs\], find the root cause (for example a retry path without idempotency under broker rebalance), fix it, and add a regression test and an alert. Run a blameless review focused on the system, not the person.
- **Result:** \[Customer impact ended in X minutes; the failure class was prevented; one concrete follow-up shipped.\]
- **Reflection:** What you would detect sooner next time (principle: pay attention to unexpected behaviour).

**Story 2: disagreement and collaboration (be hard on problems, not people)**

- **Situation:** You and \[peer or lead\] disagreed on \[approach A vs B\] for \[system\].
- **Task:** Reach a good decision without damaging the working relationship.
- **Action:** Write down each option's trade-offs, agree the criteria (reversibility, operational cost, delivery time), propose a small experiment or timeboxed spike, and invite someone neutral to review the data. If you lost the argument, commit and support the outcome.
- **Result:** \[The decision, the measured outcome and what you learned. Be honest if the other option was right.\]
- **Reflection:** How the team's decision-making process changed afterwards.

**Story 3: think big, start small, default to transparency (a large, ambiguous project)**

- **Situation:** \[A large goal, such as migrating a critical data path or building a new capability\], with unclear requirements and several stakeholders (for example product, compliance, another squad).
- **Task:** Break it into deliverable increments and keep everyone aligned.
- **Action:** Write a proposal and share it widely for feedback early (transparency). Define a thin vertical slice that could ship in weeks. Roll out behind a flag with metrics and a rollback plan. Publish progress notes. State clearly which parts you led personally and which you contributed to.
- **Result:** \[Incremental milestones delivered, risk found early, quantified impact.\]
- **Reflection:** What you would scope differently, and how you brought others along.

**Delivery tips:** keep each answer to 2–3 minutes and let them probe; name stakeholders explicitly and say how you kept them aligned; keep trade-offs and numbers in every story; prepare more stories than you need so you do not repeat one across the behavioural and technical calls.

## Cautions and gaps

- **Verbatim questions are rare.** Candidates paraphrase, and some third-party sites mix sources. Items 16–19 in the question table are lower confidence.
- **Monzo's process evolves.** Their blog says it was accurate when published. Confirm details with your recruiter.
- **Reviews are mixed.** Some candidates describe a clear, well-structured process; others report inconsistent interviewers or little feedback. Prepare for variance and ask your recruiter for feedback if you are rejected.
- **Unverified claims not to rely on:** the "Be Proud, Help People, Dream Big" values, and "Ruby" as a pair-coding language. Monzo's own post lists Go, Python and Java.
