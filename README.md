# Assignment 4: Text Analysis

## 1. Overview

Java implementations of LCS, Levenshtein Edit Distance, Naive matching, KMP, Rabin-Karp and Kasai's LCP algorithm.

## 2. Implementation and Assumptions

Both DP algorithms fill a two-dimensional table for string prefixes:

- LCS: `L(i,0)=L(0,j)=0`. Equal final characters give `L(i-1,j-1)+1`; otherwise take `max(L(i-1,j),L(i,j-1))`. Equal ends extend a smaller-prefix solution; unequal ends require omitting one end.
- Edit Distance: `D(i,0)=i`, `D(0,j)=j`; `D(i,j)=min(D(i-1,j)+1,D(i,j-1)+1,D(i-1,j-1)+cost)`. Cost is 0 for equal characters, otherwise 1. These choices cover deletion, insertion and substitution; taking their minimum is optimal.

Matching is case-sensitive, zero-based and includes overlaps. Characters/indices use Java UTF-16 units. An empty pattern matches all boundaries `0..n`; a longer pattern has no matches.

KMP's LPS table preserves reusable prefixes without moving backwards in the text. Rabin-Karp uses base 256, modulus 1000000007 and character verification after every hash hit, so collisions cannot create false matches.

Kasai receives a valid lexicographically sorted suffix-array permutation. `LCP[0]=0`; `LCP[i]` compares suffixes `SA[i-1]` and `SA[i]`. Reusing the previous common length minus one avoids restarting comparisons. Simple suffix-index sorting prepares inputs outside timing.

## 3. Complexity Analysis

DP lengths are m,n; matching uses text length n, pattern length m and `w=n-m+1`; Kasai uses length s. Bounds below assume nonempty DP inputs and `1<=m<=n`. Space excludes inputs and the matching output of O(z) indices.

|Algorithm|Main work / justification|Preprocessing|Best / worst core time|Auxiliary space|
|---|---|---|---|---|
|LCS|L recurrence, every cell once|Included|Both Theta(mn)|O(mn)|
|Edit Distance|D recurrence, every cell once|Included|Both Theta(mn)|O(mn)|
|Naive|Compare each alignment; early mismatch or m comparisons|O(1)|Theta(w) / O(wm)|O(1)|
|KMP|LPS fallbacks; text position never decreases|Theta(m)|Both Theta(n)|O(m)|
|Rabin-Karp|Constant-time rolling updates; verify hash hits|Theta(m)|Theta(w) / O(wm)|O(1)|
|Kasai|Rank array and amortized prefix reuse|Included|Both Theta(s)|O(s)|

RK search costs `O(w+(z+c)m)`, where z is actual matches and c is spurious hash hits. Few collisions give expected `O(w+zm)`, not a worst-case guarantee. Matching totals also include preprocessing. Empty-pattern output takes O(n) time/space; longer patterns and empty DP base cases return in O(1).

## 4. Testing

All 15 test groups passed. Required empty, identical, different, insertion/deletion/substitution, boundary, overlapping, absent and longer-pattern cases are covered. Every matching case checks all three algorithms.

References: 250 exhaustive LCS cases, 150 recursive Edit Distance cases, 600 `String.indexOf` cases and 200 adjacent-suffix LCP cases. Tests also cover an actual RK collision and linear KMP/Kasai comparison bounds.

Manual example: `banana` suffixes are `a, ana, anana, banana, na, nana`; `SA=[5,3,1,0,4,2]`, `LCP=[0,1,3,0,0,2]`.

## 5. Experimental Setup

- Alphabet: `abcd`; DP lengths: 100, 500, 1000; matching text lengths: 10000, 50000, 100000; Kasai lengths: 1000, 5000, 10000.
- Seed: `20261104+n`, with another 100000 for matching and 200000 for Kasai. DP algorithms share each pair. Matchers share each text/pattern: 32 characters, present substring or rejection-sampled absent pattern.
- OpenJDK 25.0.1, Windows 11; five warm-ups per case, then five timed calls. `System.nanoTime()` arithmetic means include algorithm preprocessing, allocation and counters, but exclude generation, suffix sorting, verification and I/O.
- Counters record DP cell updates, preprocessing/search character comparisons and RK initial/rolling hash updates; power setup is not counted. Kasai comparisons use the search column.

From the project folder, with JDK 17+:

```text
javac --release 17 -d out -sourcepath src src/Main.java
java -cp out Main demo
java -cp out Main test
java -cp out Main experiment
```

In IDEA, choose JDK 17+ and mark `src` as Sources Root. Experiments regenerate the tables/plots; timings vary.

## 6. Results

Mean times below are microseconds (`average_ns/1000`). [135 measured runs](results/tables/runs.csv); [27 summaries and operation counts](results/tables/summary.csv).

|Each string length|LCS time|Edit Distance time|Cells per algorithm|
|---:|---:|---:|---:|
|100|353.260|468.360|10000|
|500|1513.260|1320.520|250000|
|1000|4609.960|5110.140|1000000|

|Text length|Pattern|Naive|KMP|Rabin-Karp|
|---:|---|---:|---:|---:|
|10000|Present|235.720|399.820|952.280|
|10000|Absent|203.100|177.380|193.400|
|50000|Present|370.620|465.200|620.720|
|50000|Absent|389.860|307.320|913.520|
|100000|Present|667.120|676.580|905.360|
|100000|Absent|546.420|670.200|930.380|

|Kasai length|Time|Character comparisons|
|---:|---:|---:|
|1000|173.860|1990|
|5000|239.900|9990|
|10000|265.580|19989|

![DP time versus equal string length](results/plots/dp_time_vs_size.png)

![Matching time versus text length, present and absent patterns](results/plots/matching_time_vs_length.png)

## 7. Discussion

DP cell counts grow quadratically; Kasai comparisons grow approximately linearly. Random matching usually fails early, so Naive avoids its repetitive-input worst case. At n=100000, present-pattern search comparisons were 133028/124988/32 for Naive/KMP/RK; RK also performed 100032 hash updates. Fewer comparisons did not mean less time: hashing has additional arithmetic costs. Fixed m=32 does not demonstrate worst-case growth. JVM compilation, allocation and scheduling explain timing variation, including non-monotonic samples; three sizes do not prove Big-O.

## 8. Conclusion

DP solves prefix subproblems with quadratic storage. Naive is simple, KMP guarantees linear matching, RK trades comparisons for hashing, and Kasai builds LCP linearly when given a suffix array.
