SELECT t.math_local_qid, t.math_body
FROM wiki_arqmath20.math_wbs_text_store t
INTO OUTFILE '/mnt/share/data/arqmath/data.csv'
    FIELDS TERMINATED BY ','
    ENCLOSED BY '"'
    LINES TERMINATED BY '\n'