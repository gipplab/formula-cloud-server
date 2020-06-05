import module namespace moi = "http://www.moi.org" at "libs/xquery/moiModule.xqm";

declare variable $docid external;
declare variable $minDocFreq := 1;
declare variable $docs := /harvest/expr[@data-post-id=$docid];
if ($docs/*) then
  moi:extractTerms($docs, $minDocFreq)
