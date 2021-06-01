SELECT tt.tid as qID, pid as aID, tt.text as q, mwts.math_body as a, relevance as rel
from qrel join math_wbs_entity_map on math_external_id = pid
join math_wbs_text_store mwts on math_wbs_entity_map.math_local_qid = mwts.math_local_qid
join topic_text tt on qrel.tid = tt.tid
where math_external_id_type =1;