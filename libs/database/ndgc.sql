SELECT formula_id
     , relevance
     , (relevance = 1)                                as POSEQONE
     , sum((pow(2, relevance) - 1) / log(2, pos + 1)) as DCG
     , sum((pow(2, 3) - 1) / log(2, 1 + 1)) as IDCG
FROM wiki_arqmath20.qrel rel,
     wiki_arqmath20.math_arqmath_moi_results results
WHERE results.topic_id = rel.tid
  AND results.formula_id = rel.pid
GROUP BY formula_id