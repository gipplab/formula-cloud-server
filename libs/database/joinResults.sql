SELECT
    tid,
    pid,
    relevance,
    score,
    pos,
    ROW_NUMBER() OVER(PARTITION BY tid ORDER BY pos) as rang,
    null as topRelevance
FROM qrel rel,
     math_arqmath_moi_results results
WHERE results.topic_id = rel.tid
  AND results.formula_id = rel.pid
ORDER BY tid, pos;