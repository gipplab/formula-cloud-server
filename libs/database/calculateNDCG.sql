SELECT AVG(DCG/IDCG) as nDCG
FROM (
         SELECT tid
              , sum((pow(2, relevance) - 1) / log(2, rang + 1)) as DCG
              , sum((pow(2, topRelevance) - 1) / log(2, rang + 1)) as IDCG
         FROM math_arqmath_inter_results
         GROUP BY tid
     ) s1